package brad.grier.alhena;

import java.io.File;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;

import org.h2.jdbcx.JdbcConnectionPool;

import brad.grier.alhena.DB.Bookmark;
import brad.grier.alhena.DB.ClientCertInfo;
import brad.grier.alhena.DB.DBClientCertInfo;

/**
 * Create a second db for merge
 * 
 * @author Brad Grier
 */
public class DBBackup {

    private static JdbcConnectionPool cp;

    public static void init() throws Exception {

        String homeDir = System.getProperty("alhena.home");
        File prevDB = new File(homeDir + "/alhena_restore.mv.db");
        if (prevDB.exists()) {
            prevDB.delete();
        }
        char[] pswd = {'i', 'a', 'm', 't', 'h', 'e', 'w', 'a', 'l', 'n', 'u', 't', '!'};
        cp = JdbcConnectionPool.create("jdbc:h2:" + homeDir + "/alhena_restore;CIPHER=AES", "sa", "sa " + new String(pswd));

    }

    public static int mergeDB(File inputFile) throws Exception {
        int version = 0;
        try (FileSystem zipFs = FileSystems.newFileSystem(inputFile.toPath(), (ClassLoader) null)) {
            version = Integer.parseInt(Files.readString(zipFs.getPath("/version.txt")).trim());
        }

        try (Connection con = cp.getConnection(); var st = con.createStatement()) {

            st.execute("RUNSCRIPT FROM '" + inputFile + "' COMPRESSION ZIP");
        }

        if (version <= Integer.parseInt(DB.VERSION)) {
            mergeHistory();
            mergeBookmarks();
            mergeServerCerts();
            mergeClientCerts();

            if (DB.tableExists(cp.getConnection(), "CACERTS")) { // for some backward compatibility
                HashMap<String, X509Certificate> certMap = DB.getSavedCerts(cp);
                Alhena.setServerCerts(certMap);
                // DB.runStatement("DROP TABLE CACERTS");
            }

        }
        cp.dispose();
        String homeDir = System.getProperty("alhena.home");
        new File(homeDir + "/alhena_restore.mv.db").delete();
        return version <= Integer.parseInt(DB.VERSION) ? 0 : version; // backwards but...

    }

    public static void mergeHistory() throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.createStatement()) {

            try (ResultSet rs = ps.executeQuery("SELECT URL, TIME_STAMP FROM HISTORY")) {
                while (rs.next()) {
                    DB.insertHistory(rs.getString(1), rs.getTimestamp(2).getTime());
                }
            }
        }

    }

    public static void mergeBookmarks() throws SQLException {
        List<Bookmark> bList = DB.loadBookmarks();
        try (Connection con = cp.getConnection(); var ps = con.createStatement()) {

            try (ResultSet rs = ps.executeQuery("SELECT LABEL, URL, FOLDER, TIME_STAMP FROM BOOKMARKS")) {
                while (rs.next()) {
                    try {
                        Bookmark bmark = new Bookmark(rs.getString(1), rs.getString(2), rs.getString(3), 0);
                        Timestamp ts = rs.getTimestamp(4);
                        boolean exists = bList.stream()
                                .anyMatch(b -> b.label().equals(bmark.label()) && b.url().equals(bmark.url()) && b.folder().equals(bmark.folder()));
                        if (!exists) {
                            DB.insertBookmark(bmark.label(), bmark.url(), bmark.folder(), ts.getTime());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                }
            }
        }
    }

    public static void mergeServerCerts() throws SQLException {

        try (Connection con = cp.getConnection(); var ps = con.createStatement()) {

            try (ResultSet rs = ps.executeQuery("SELECT DOMAIN, FINGERPRINT, EXPIRES, TIME_STAMP FROM SERVERCERTS")) {
                while (rs.next()) {
                    String domain = rs.getString(1);

                    // only insert if domain not present in current db
                    if (DB.getServerCert(domain) == null) {
                        String fingerPrint = rs.getString(2);
                        Timestamp expires = rs.getTimestamp(3);
                        Timestamp ts = rs.getTimestamp(4);
                        DB.upsertCert(domain, fingerPrint, expires, ts);
                    }
                }
            }
        }

    }

    public static void mergeClientCerts() throws SQLException {
        List<DBClientCertInfo> certs = DB.loadCerts();
        try (Connection con = cp.getConnection(); var ps = con.createStatement()) {

            try (ResultSet rs = ps.executeQuery("SELECT DOMAIN, CERT, PRIVATEKEY, ACTIVE, TIME_STAMP FROM CLIENTCERTS")) {
                while (rs.next()) {
                    String domain = rs.getString(1);
                    String cert = rs.getString(2);
                    String pkey = rs.getString(3);
                    boolean active = rs.getBoolean(4);
                    Timestamp ts = rs.getTimestamp(5);
                    // check if cert domain combo already there
                    boolean exists = certs.stream().anyMatch(c -> c.cert().equals(cert) && c.domain().equals(domain));
                    if (!exists) {
                        ClientCertInfo ci = DB.getClientCertInfo(domain); // only returns active
                        if (ci != null && active) {
                            active = false; // already an active cert for this domain, insert as inactive

                        }
                        DB.insertClientCert(domain, cert, pkey, active, ts);
                    }

                }
            }
        }

    }

}
