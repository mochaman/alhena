package brad.grier.alhena;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.h2.jdbcx.JdbcConnectionPool;

import brad.grier.alhena.PipedEncryption.Encryptor;

/**
 * Database methods.
 *
 * @author Brad Grier
 */
public class DB {

    private static JdbcConnectionPool cp;
    public static String VERSION = "2";

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
        initV1();
        initV2(cp);

    }

    public static JdbcConnectionPool getPool() {
        return cp;
    }

    // THE SQL HERE IS SET IN STONE TO SUPPORT IMPORT/EXPORT - NO CHANGES!!!!
    // ANY TABLE CHANGES NEED TO BE VIA ALTER STATEMENTS IN AN initV2() METHOD, ETC WHICH
    // MUST THEN BE CALLED FROM restoreDB() ONCE ADDED
    private static void initV1() throws Exception {
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

    public static void initV2(JdbcConnectionPool conPool) throws Exception {
        try (Connection con = conPool.getConnection()) {
            String checkSql = "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = 'CLIENTCERTS' AND COLUMN_NAME = 'DOMAIN'";
            try (var stmt = con.createStatement(); ResultSet rs = stmt.executeQuery(checkSql)) {
                if (rs.next() && rs.getInt(1) == 256) {
                    stmt.executeUpdate("ALTER TABLE CLIENTCERTS ALTER COLUMN DOMAIN VARCHAR(1024)");
                    stmt.executeUpdate("UPDATE CLIENTCERTS SET DOMAIN = DOMAIN || ':1965/'");
                    stmt.executeUpdate("CREATE INDEX idx_url ON CLIENTCERTS(DOMAIN)");
                }
            }
        }
    }

    public static boolean tableExists(Connection connection, String tableName) throws Exception {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet resultSet = metaData.getTables(null, null, tableName.toUpperCase(), null)) {
            return resultSet.next();
        }
    }

    public static void runStatement(String sql) throws SQLException {
        runStatement(cp.getConnection(), sql);

    }

    private static void runStatement(Connection connection, String sql) throws SQLException {

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
    public static void insertHistory(String url, Long tStamp) throws SQLException {
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
                long ts = tStamp == null ? System.currentTimeMillis() : tStamp;
                ps.setDate(2, new Date(ts));
                ps.setTimestamp(3, new Timestamp(ts));
                ps.executeUpdate();
            }
        }
    }

    public static int deleteHistory(String url, String clause) throws SQLException {

        try (Connection con = cp.getConnection()) {
            String c = clause == null ? "URL = ?" : "URL LIKE ?";
            try (var ps = con.prepareStatement("DELETE FROM HISTORY WHERE " + c)) {
                ps.setString(1, clause == null ? url : "%" + clause + "%");
                ps.execute();
                return ps.getUpdateCount();

            }
        }
    }

    public static void insertClientCert(String domain, String cert, String key, boolean active, Timestamp ts) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("INSERT INTO CLIENTCERTS (DOMAIN, CERT, PRIVATEKEY, ACTIVE, TIME_STAMP) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, domain);
                ps.setString(2, cert);
                ps.setString(3, key);
                ps.setBoolean(4, active);
                ps.setTimestamp(5, ts == null ? new Timestamp(System.currentTimeMillis()) : ts);
                ps.execute();
            }
        }
    }

    public record ClientCertInfo(int id, String cert, String privateKey, String domain) {

    }

    public record DBClientCertInfo(int id, String domain, String cert, boolean active) {

    }

    public static ClientCertInfo getClientCert(URI uri) throws SQLException {
        int port = uri.getPort();
        port = port == -1 ? 1965 : port;
        String baseDomain = uri.getHost() + ":" + port;
        String prunedUrl = baseDomain + uri.getPath();

        List<ClientCertInfo> certList = new ArrayList<>();

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT ID, DOMAIN, CERT, PRIVATEKEY FROM CLIENTCERTS WHERE DOMAIN LIKE ? AND ACTIVE = TRUE")) {
            ps.setString(1, baseDomain + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    certList.add(new ClientCertInfo(rs.getInt(1), rs.getString(3), rs.getString(4), rs.getString(2)));

                }
            }
        }

        ClientCertInfo bestCCI = null;
        for (ClientCertInfo cci : certList) {
            if (prunedUrl.startsWith(cci.domain)) {
                if (bestCCI == null) {
                    bestCCI = cci;
                } else {
                    if (cci.domain.length() > bestCCI.domain.length()) {
                        bestCCI = cci;
                    }
                }
            }
        }
        return bestCCI;
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

    public static void insertBookmark(String label, String url, String folder, Long ts) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("INSERT INTO BOOKMARKS (LABEL, URL, FOLDER, TIME_STAMP) VALUES (?, ?, ?, ?)")) {
                ps.setString(1, label);
                ps.setString(2, url);
                ps.setString(3, folder);
                ps.setTimestamp(4, ts == null ? new Timestamp(System.currentTimeMillis()) : new Timestamp(ts));
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

    public static List<Bookmark> loadTopBookmarks() throws SQLException {
        ArrayList<Bookmark> bookmarkList = new ArrayList<>();
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("select b.id, b.url, b.label, b.folder, count(h.url) as visit_count from bookmarks b join history h on b.url = h.url group by b.id, b.url, b.label, b.folder order by visit_count desc, b.id ASC limit 20")) {
                while (rs.next()) {
                    bookmarkList.add(new Bookmark(rs.getString(3), rs.getString(2), rs.getString(4), rs.getInt(1)));
                }
            }
        }
        return bookmarkList;
    }

    public static List<DBClientCertInfo> loadCerts() throws SQLException {
        ArrayList<DBClientCertInfo> certList = new ArrayList<>();
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("select id, domain, cert, active from clientcerts order by time_stamp, active asc")) {
                while (rs.next()) {
                    certList.add(new DBClientCertInfo(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4)));
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

    public static String getClientCert(int id) throws SQLException {
        String host = null;
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("SELECT DOMAIN FROM CLIENTCERTS WHERE ID = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    host = rs.getString(1);
                }
            }
        }
        return host;
    }

    public static void deleteClientCert(int id) throws SQLException {
        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("DELETE FROM CLIENTCERTS WHERE ID = ?")) {
            ps.setInt(1, id);
            ps.execute();
        }

    }

    public static void updateBookmark(int id, String label, String folder, String url) throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement("UPDATE BOOKMARKS SET LABEL = ?, FOLDER = ?, URL = ? WHERE ID = ?")) {
            ps.setString(1, label);
            ps.setString(2, folder);
            ps.setString(3, url);
            ps.setInt(4, id);

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

                Locale systemLocale = Locale.getDefault(); // Get system locale
                DateFormat groupFormat = DateFormat.getDateInstance(DateFormat.FULL, systemLocale);
                while (rs.next()) {
                    count++;
                    Timestamp ts = rs.getTimestamp(2);

                    String formattedDate = groupFormat.format(ts);
                    if (saveDate == null || !saveDate.equals(formattedDate)) {
                        EventQueue.invokeLater(() -> {
                            textPane.addPage("\n### " + formattedDate + "\n\n");
                        });

                        saveDate = formattedDate;
                    }
                    String l1 = rs.getString(1);
                    EventQueue.invokeLater(() -> {
                        textPane.addPage("=> " + l1 + "\n");
                    });

                }
            }
        }
        return count;
    }

    public static int loadServers(GeminiTextPane textPane) throws SQLException {
        int count = 0;
        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            try (ResultSet rs = st.executeQuery("SELECT DOMAIN, FINGERPRINT, EXPIRES, TIME_STAMP FROM SERVERCERTS ORDER BY TIME_STAMP ASC")) {
                String saveDate = null;

                Locale systemLocale = Locale.getDefault(); // Get system locale
                DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, systemLocale);
                DateFormat groupFormat = DateFormat.getDateInstance(DateFormat.FULL, systemLocale);
                while (rs.next()) {
                    count++;
                    String domain = rs.getString(1);
                    String fingerprint = rs.getString(2);
                    Timestamp expireTs = rs.getTimestamp(3);
                    Timestamp ts = rs.getTimestamp(4);

                    String saved = groupFormat.format(ts);
                    String expires = dateFormat.format(expireTs);
                    if (saveDate == null || !saveDate.equals(saved)) {
                        EventQueue.invokeLater(() -> {
                            textPane.addPage("\n### " + saved + "\n\n");
                        });

                        saveDate = saved;
                    }

                    EventQueue.invokeLater(() -> {
                        textPane.addPage(domain + " expires " + expires + "\n");
                        textPane.addPage(fingerprint + "\n\n");
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

    public static void upsertCert(String domain, String cert, Timestamp expires, Timestamp ts) throws SQLException {

        String mergeSql = """
            MERGE INTO SERVERCERTS (DOMAIN, FINGERPRINT, EXPIRES, TIME_STAMP)
            KEY(DOMAIN) VALUES
            (?, ?, ?, ?);   
        """;

        try (Connection con = cp.getConnection(); var ps = con.prepareStatement(mergeSql)) {

            ps.setString(1, domain);
            ps.setString(2, cert);
            ps.setTimestamp(3, expires);
            ps.setTimestamp(4, ts == null ? new Timestamp(System.currentTimeMillis()) : ts);
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

    public static String getPref(String key, String defVal) {
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
        return pref == null ? defVal : pref;
    }

    public static HashMap<String, String> getAllPrefs() {
        HashMap<String, String> map = new HashMap<>();

        try {
            try (Connection con = cp.getConnection(); var st = con.createStatement()) {

                try (ResultSet rs = st.executeQuery("SELECT PREFKEY, PREF FROM PREFS")) {

                    while (rs.next()) {
                        map.put(rs.getString(1), rs.getString(2));
                    }
                }
            }

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return map;

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

    public static void dumpDB(X509Certificate cert, File outputFile) throws Exception {
        Encryptor encryptor = new Encryptor(cert, outputFile);
        try (Connection con = cp.getConnection(); var st = con.createStatement();) {
            ResultSet rs = st.executeQuery("SCRIPT");

            while (rs.next()) {
                encryptor.writeString(rs.getString(1));
            }
            encryptor.close();
        }
    }

    public static void dumpDB(File outputFile) throws Exception {
        runStatement("DROP TABLE IF EXISTS CACERTS");
        // create a cacerts table and copy the x509certs from cacerts for each host in clientcerts
        String sql = """
            CREATE TABLE CACERTS (
                DOMAIN VARCHAR(256),
                CERT VARCHAR(8192)
            );
            """;
        runStatement(sql);
        List<DBClientCertInfo> cCerts = loadCerts();
        List<String> domainList = cCerts.stream()
                .map(DBClientCertInfo::domain) // Extract domains
                .distinct() // keep only unique domains
                .toList();

        // extract the certificate from cacerts for each domain
        HashMap<String, X509Certificate> certMap = Alhena.getServerCerts(domainList);

        certMap.entrySet().stream().forEach(es -> {
            try {
                String certPem = "-----BEGIN CERTIFICATE-----\n"
                        + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(es.getValue().getEncoded())
                        + "\n-----END CERTIFICATE-----";

                insertCACert(es.getKey(), certPem);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        try (Connection con = cp.getConnection(); var st = con.createStatement()) {
            //ResultSet rs = st.executeQuery("SCRIPT");
            st.execute("SCRIPT DROP TO '" + outputFile.getAbsolutePath() + "' COMPRESSION ZIP");

        }
        runStatement("DROP TABLE CACERTS");
        addFileToZip(outputFile.getAbsolutePath(), "version.txt", VERSION);

    }

    public static int restoreDB(File inputFile) throws Exception {
        boolean vlc = Alhena.allowVLC;
        int version = 0;
        try (FileSystem zipFs = FileSystems.newFileSystem(inputFile.toPath(), (ClassLoader) null)) {
            version = Integer.parseInt(Files.readString(zipFs.getPath("/version.txt")).trim());
        }

        if (version > Integer.parseInt(VERSION)) {
            // backup is newer than current version of Alhena - no go
            return version;
        }

        try (Connection con = cp.getConnection(); var st = con.createStatement()) {
            //ResultSet rs = st.executeQuery("SCRIPT");
            st.execute("RUNSCRIPT FROM '" + inputFile + "' COMPRESSION ZIP");
        }

        if (tableExists(cp.getConnection(), "CACERTS")) { // for some backward compatibility
            HashMap<String, X509Certificate> certMap = getSavedCerts(cp);
            Alhena.setServerCerts(certMap);
            runStatement("DROP TABLE CACERTS");
        }

        DB.insertPref("allowvlc", String.valueOf(vlc)); //  do not restore vlc setting as vlc may not exist on different machine

        HashMap<String, String> map = DB.getAllPrefs();
        Alhena.httpProxy = map.getOrDefault("httpproxy", null);
        Alhena.gopherProxy = map.getOrDefault("gopherproxy", null);
        Alhena.searchUrl = map.getOrDefault("searchurl", null);
        int contentP = Integer.parseInt(map.getOrDefault("contentwidth", "80"));
        GeminiTextPane.contentPercentage = (float) ((float) contentP / 100f);
        GeminiTextPane.wrapPF = map.getOrDefault("linewrappf", "false").equals("true");
        GeminiTextPane.asciiImage = map.getOrDefault("asciipf", "false").equals("true");
        GeminiTextPane.embedPF = map.getOrDefault("embedpf", "true").equals("true");
        GeminiTextPane.showSB = map.getOrDefault("showsb", "false").equals("true");
        GeminiTextPane.shadePF = map.getOrDefault("shadepf", "false").equals("true");
        GeminiFrame.ansiAlert = map.getOrDefault("ansialert", "false").equals("true");
        Alhena.favIcon = map.getOrDefault("favicon", "false").equals("true");
        Alhena.dataUrl = map.getOrDefault("dataurl", "true").equals("true");
        Alhena.linkIcons = map.getOrDefault("linkicons", "true").equals("true");

        // after DB VERSION 1 of db release, need to call future initV2(), initV3() methods so older database dumps have
        // subsequent database changes
        if (version == 1) {
            initV2(cp);
        }
        return 0;

    }

    public static void addFileToZip(String zipFilePath, String entryNameInZip, String contents) {
        Map<String, String> env = new HashMap<>();
        env.put("create", "false"); // File should exist

        Path path = Paths.get(zipFilePath);
        URI uri = URI.create("jar:" + path.toUri());

        try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
            //Path fileToAdd = Paths.get(fileToAddPath);
            Path targetInZip = fs.getPath(entryNameInZip);
            Files.write(targetInZip, contents.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private static void insertCACert(String domain, String cert) throws SQLException {

        try (Connection con = cp.getConnection()) {
            try (var ps = con.prepareStatement("INSERT INTO CACERTS (DOMAIN, CERT) VALUES (?, ?)")) {
                ps.setString(1, domain);
                ps.setString(2, cert);
                ps.execute();
            }
        }
    }

    public static HashMap<String, X509Certificate> getSavedCerts(JdbcConnectionPool pool) throws Exception {
        HashMap<String, X509Certificate> certMap = new HashMap<>();
        try (Connection con = pool.getConnection(); var st = con.createStatement();) {
            ResultSet rs = st.executeQuery("SELECT DOMAIN, CERT FROM CACERTS");

            while (rs.next()) {
                certMap.put(rs.getString(1), (X509Certificate) Alhena.loadCertificate(rs.getString(2)));

            }

        }
        return certMap;
    }

}
