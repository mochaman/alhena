
package brad.grier.alhena;

import java.awt.EventQueue;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;

/**
 * Database methods.
 * 
 * @author Brad Grier
 */
public class DB {

    private static JdbcConnectionPool cp;

    public static record CertInfo(String fingerPrint, Timestamp expires, Timestamp lastModified) {

    }

    public static void init() throws Exception {

        String homeDir = System.getProperty("alhena.home");
        if (System.getenv("ALHENAPSWD") != null) {
            cp = JdbcConnectionPool.create("jdbc:h2:" + homeDir + "/alhena;CIPHER=AES", "sa", "sa " + System.getenv("ALHENAPSWD"));
        } else { 
            char[] pswd = {'i', 'a', 'm', 't', 'h', 'e', 'w', 'a', 'l', 'n', 'u', 't', '!'};
            cp = JdbcConnectionPool.create("jdbc:h2:" + homeDir + "/alhena;CIPHER=AES", "sa", "sa " + new String(pswd));
        }

        try (Connection connection = cp.getConnection()) {
            if (!tableExists(connection, "HISTORY")) {
                String sql = """
                    CREATE TABLE HISTORY (
                        ID INT PRIMARY KEY AUTO_INCREMENT,
                        URL VARCHAR(1024),
                        TIME_STAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        TIME_STAMP_DATE DATE AS (CAST(TIME_STAMP AS DATE))
                );
                 """;
                runStatement(connection, sql);
                runStatement(connection, "ALTER TABLE HISTORY ADD CONSTRAINT UNIQUE_URL_PER_DAY UNIQUE (URL, TIME_STAMP_DATE)");
 
            }
            if (!tableExists(connection, "BOOKMARKS")) {
                String sql = """
                    CREATE TABLE BOOKMARKS (
                        ID INT PRIMARY KEY AUTO_INCREMENT,
                        LABEL VARCHAR(256),
                        URL VARCHAR(1024),
                        FOLDER VARCHAR(256),
                        TIME_STAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
                 """;
                runStatement(connection, sql);
                runStatement(connection, "CREATE INDEX idx_favurl ON BOOKMARKS (URL);");
            }
            if (!tableExists(connection, "SERVERCERTS")) {
                String sql = """
                    CREATE TABLE SERVERCERTS (
                        ID INT AUTO_INCREMENT,
                        DOMAIN VARCHAR(256) PRIMARY KEY,
                        FINGERPRINT VARCHAR(64),
                        EXPIRES TIMESTAMP,
                        TIME_STAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
                 """;
                runStatement(connection, sql);
                runStatement(connection, "CREATE INDEX idx_domain ON SERVERCERTS (DOMAIN);");

            }

            if (!tableExists(connection, "CLIENTCERTS")) {
                String sql = """
                    CREATE TABLE CLIENTCERTS (
                        ID INT AUTO_INCREMENT PRIMARY KEY,
                        DOMAIN VARCHAR(256),
                        CERT VARCHAR(8192),
                        PRIVATEKEY VARCHAR(8192),
                        ACTIVE BOOLEAN,
                        TIME_STAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
                 """;
                runStatement(connection, sql);

            }

            if (!tableExists(connection, "PREFS")) {
                String sql = """
                    CREATE TABLE PREFS (
                        ID INT AUTO_INCREMENT,
                        PREFKEY VARCHAR(256) PRIMARY KEY,
                        PREF VARCHAR(2048),
                        TIME_STAMP TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
                 """;
                runStatement(connection, sql);
                runStatement(connection, "CREATE INDEX idx_prefs ON PREFS (PREFKEY);");
            }

        }
    }

    private static boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return resultSet.next();
        }
    }

    private static void runStatement(Connection connection, String sql) throws Exception {

        try (var statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    public static int deleteHistory() throws SQLException {

        try (var statement = cp.getConnection().createStatement()) {
            statement.execute("DELETE FROM HISTORY");
            return statement.getUpdateCount();
        }
    }

    // idea here is that history is saved on a daily basis with duplicate entries for any given day
    // rising to the top
    public static void insertHistory(String url) throws SQLException {
        String mergeSql = """
            MERGE INTO history AS h
            USING (SELECT CAST(? AS VARCHAR) AS url, CAST(? AS DATE) AS time_stamp_date, CAST(? AS TIMESTAMP) AS time_stamp) AS src
            ON h.url = src.url AND CAST(h.time_stamp AS DATE) = src.time_stamp_date
            WHEN MATCHED THEN
                UPDATE SET time_stamp = src.time_stamp
            WHEN NOT MATCHED THEN
                INSERT (url, time_stamp) VALUES (src.url, src.time_stamp);
            """;
        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement(mergeSql)) {
                ps.setString(1, url);
                long ts = System.currentTimeMillis();
                ps.setDate(2, new Date(ts));
                ps.setTimestamp(3, new Timestamp(ts));
                ps.execute();
            }
        }
    }

    public static int deleteHistory(String url) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("DELETE FROM HISTORY WHERE URL = ?")) {
                ps.setString(1, url);
                ps.execute();
                return ps.getUpdateCount();

            }
        }
    }

    public static void insertClientCert(String domain, String cert, String key) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("INSERT INTO CLIENTCERTS (DOMAIN, CERT, PRIVATEKEY, ACTIVE) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, domain);
                ps.setString(2, cert);
                ps.setString(3, key);
                ps.setBoolean(4, true);
                ps.execute();
            }
        }
    }

    public record ClientCertInfo(int id, String cert, String privateKey, String domain) {

    }

    public static ClientCertInfo getClientCertInfo(String domain) throws SQLException {

        ClientCertInfo certInfo = null;
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT ID, CERT, PRIVATEKEY FROM CLIENTCERTS WHERE DOMAIN = ? AND ACTIVE = TRUE")) {
            ps.setString(1, domain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    //cert = rs.getString(1);
                    certInfo = new ClientCertInfo(rs.getInt(1), rs.getString(2), rs.getString(3), domain);
                }
            }
        }
        return certInfo;
    }

    public static ClientCertInfo getClientCertInfo(int id) throws SQLException {

        ClientCertInfo certInfo = null;
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT ID, CERT, PRIVATEKEY, DOMAIN FROM CLIENTCERTS WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    //cert = rs.getString(1);
                    certInfo = new ClientCertInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4));
                }
            }
        }
        return certInfo;
    }

    public static void insertBookmark(String label, String url, String folder) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("INSERT INTO BOOKMARKS (LABEL, URL, FOLDER) VALUES (?, ?, ?)")) {
                ps.setString(1, label);
                ps.setString(2, url);
                ps.setString(3, folder);
                ps.execute();
            }
        }
    }

    public record Bookmark(String label, String url, String folder, int id) {

    }

    public static List<Bookmark> loadBookmarks() throws SQLException {
        ArrayList<Bookmark> bookmarkList = new ArrayList<>();
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT LABEL, URL, FOLDER, ID FROM BOOKMARKS ORDER BY TIME_STAMP ASC")) {
                while (rs.next()) {
                    bookmarkList.add(new Bookmark(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
                }
            }
        }
        return bookmarkList;
    }

    public record DBClientCertInfo(int id, String domain, String cert, boolean active) {

    }

    public static List<DBClientCertInfo> loadCerts() throws SQLException {
        ArrayList<DBClientCertInfo> certList = new ArrayList<>();
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("select id, domain, cert, active from clientcerts order by time_stamp, active asc")) {
                while (rs.next()) {
                    certList.add(new DBClientCertInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
                    //bookmarkList.add(new Bookmark(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
                }
            }
        }
        return certList;
    }

    public static void toggleCert(int id, boolean state, String domain, boolean always) throws SQLException {
        if (state == false || always) {
            try (Connection con = cp.getConnection(); var ps = con.prepareStatement("UPDATE CLIENTCERTS SET ACTIVE = ? WHERE ID = ?")) {
                ps.setBoolean(1, state);
                ps.setInt(2, id);
                ps.execute();
            }

        } else {
            ClientCertInfo certInfo = getClientCertInfo(domain);
            if (certInfo != null) {
                // need to deactivate
                toggleCert(certInfo.id, false, domain, true);
            }
            toggleCert(id, state, domain, true);
        }
    }

    public static Bookmark getBookmark(int id) throws SQLException {

        Bookmark bm = null;
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT LABEL, URL, FOLDER FROM BOOKMARKS WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    bm = new Bookmark(rs.getString(1), rs.getString(2), rs.getString(3), id);
                }
            }
        }
        return bm;
    }

    public static void deleteBookmark(int id) throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("DELETE FROM BOOKMARKS WHERE ID = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }

    }

    public static void deleteClientCert(int id) throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("DELETE FROM CLIENTCERTS WHERE ID = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }

    }

    public static void updateBookmark(int id, String label, String folder) throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("UPDATE BOOKMARKS SET LABEL = ?, FOLDER = ? WHERE ID = ?")) {
            ps.setString(1, label);
            ps.setString(2, folder);
            ps.setInt(3, id);
            ps.execute();
        }

    }

    public static List<String> bookmarkFolders() throws SQLException {

        ArrayList<String> fl = new ArrayList<>();

        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("select folder from ( select folder, min(time_stamp) as first_time_stamp from bookmarks group by folder) subquery order by first_time_stamp asc")) {
                while (rs.next()) {
                    fl.add(rs.getString(1));
                }
            }
        }
        return fl;
    }

    public static int loadHistory(GeminiTextPane textPane) throws SQLException {
        int count = 0;
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT URL, TIME_STAMP FROM HISTORY ORDER BY TIME_STAMP DESC")) {
                String saveDate = null;

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMMM dd, yyyy");
                while (rs.next()) {
                    count++;
                    Timestamp ts = rs.getTimestamp(2);
                    // convert to LocalDateTime
                    LocalDateTime localDateTime = ts.toLocalDateTime();

                    // format the LocalDateTime
                    String formattedDate = localDateTime.format(formatter);
                    if (saveDate == null || !saveDate.equals(formattedDate)) {
                        EventQueue.invokeLater(() -> {
                            textPane.addPage("\n### " + formattedDate + "\n\n", null);
                        });

                        saveDate = formattedDate;
                    }
                    String l1 = rs.getString(1);
                    EventQueue.invokeLater(() -> {
                        textPane.addPage("=> " + l1 + "\n", null);
                    });

                }
            }
        }
        return count;
    }

    public static CertInfo getServerCert(String domain) throws SQLException {

        CertInfo cr = null;
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT FINGERPRINT, EXPIRES, TIME_STAMP FROM SERVERCERTS WHERE DOMAIN = ?")) {
            ps.setString(1, domain);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cr = new CertInfo(rs.getString(1), rs.getTimestamp(2), rs.getTimestamp(3));
                }
            }

        }
        return cr == null ? new CertInfo(null, null, null) : cr;
    }

    public static void upsertCert(String domain, String cert, Timestamp expires) throws SQLException {

        String mergeSql = """
            MERGE INTO SERVERCERTS (DOMAIN, FINGERPRINT, EXPIRES, TIME_STAMP)
            KEY(DOMAIN) VALUES
            (?, ?, ?, ?);   
        """;

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement(mergeSql)) {

            ps.setString(1, domain);
            ps.setString(2, cert);
            ps.setTimestamp(3, expires);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();

        }
    }

    public static void insertPref(String key, String pref) {
        try {
            String mergeSql = """
                MERGE INTO PREFS (PREFKEY, PREF)
                KEY(PREFKEY) VALUES
                (?, ?);   
            """;
            try (Connection con = cp.getConnection()) {
                try (var ps = con.prepareStatement(mergeSql)) {
                    ps.setString(1, key);
                    ps.setString(2, pref);
                    ps.execute();
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static String getPref(String key) {
        String pref = null;
        try {
            try (Connection con = cp.getConnection()) {
                try (var ps = con.prepareStatement("SELECT PREF FROM PREFS WHERE PREFKEY = ?")) {
                    ps.setString(1, key);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            pref = rs.getString(1);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return pref;
    }

    public static String getUrlLabel(String url) {
        String label = null;
        try {
            try (Connection con = cp.getConnection()) {
                try (var ps = con.prepareStatement("SELECT LABEL FROM BOOKMARKS WHERE URL = ?")) {
                    ps.setString(1, url);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            label = rs.getString(1);
                        }
                    }
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return label;
    }

}
