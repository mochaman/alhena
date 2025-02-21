package brad.grier.alhena;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.IDN;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.util.SystemInfo;

import brad.grier.alhena.DB.CertInfo;
import brad.grier.alhena.DB.ClientCertInfo;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.IdentityCipherSuiteFilter;
import io.netty.handler.ssl.JdkSslContext;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.JdkSSLEngineOptions;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetClientOptions;
import io.vertx.core.net.NetSocket;
import io.vertx.core.spi.tls.SslContextFactory;
import io.vertx.core.streams.Pump;

/**
 * Static main class to manage frame creation and connectivity
 *
 */
public class Alhena {

    private static Vertx vertx;
    private static NetClient client;
    private static HttpClient httpClient;
    private static String netClientDomainCert;

    private final static List<GeminiFrame> frameList = new ArrayList<>();
    public final static String PROG_NAME = "Alhena";
    public final static String WELCOME_MESSAGE = "Welcome To " + PROG_NAME;
    public final static String VERSION = "2.7";
    private static volatile boolean interrupted;
    private static int redirectCount;
    public static final List<String> fileExtensions = List.of(".txt", ".gemini", ".gmi", ".log", ".html", ".pem", ".csv", ".png", ".jpg", ".jpeg");
    public static final List<String> imageExtensions = List.of(".png", ".jpg", ".jpeg");
    public static boolean browsingSupported, mailSupported;

    public static void main(String[] args) throws Exception {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher((KeyEvent e) -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_ESCAPE) {

                Component source = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
                GeminiFrame gf = (GeminiFrame) SwingUtilities.getAncestorOfClass(GeminiFrame.class, source);
                if (gf != null && gf.getGlassPane().isShowing()) {

                    interrupted = true;
                    // closing and recreating the client doesn't work for ending connection handshake
                    // close vertx and recreate
                    vertx.close();
                    VertxOptions options = new VertxOptions().setBlockedThreadCheckInterval(Integer.MAX_VALUE);
                    vertx = Vertx.vertx(options);
                    client = null;
                    createNetClient();

                    return true; // consume
                }
                return false;
            }

            Component source = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            GeminiFrame gf = (GeminiFrame) SwingUtilities.getAncestorOfClass(GeminiFrame.class, source);

            // textpane eats keys
            if (gf != null && gf.visiblePage().textPane.hasFocus()) {
                KeyStroke keyStroke = KeyStroke.getKeyStrokeForEvent(e);
                Runnable r = gf.actionMap.get(keyStroke);
                if (r != null) {
                    r.run();
                    return true;
                }
            }
            return false; // allow event to be processed
        });

        // look for file and not directory in case user created alhena directory for install purposes
        boolean legacyHomeExists = new File(System.getProperty("user.home") + "/alhena/cacerts").exists();

        String alhenaHome = System.getenv("ALHENA_HOME"); // the directory to store cacerts, db, etc
        if (alhenaHome != null) {
            System.setProperty("alhena.home", alhenaHome);
        } else {
            // use .alehnahome from here on out but preserve the the non-hidden home
            // for legacy users
            String sep = legacyHomeExists ? "/" : "/.";
            alhenaHome = System.getProperty("user.home") + sep + "alhena";
            System.setProperty("alhena.home", alhenaHome);
        }
        Files.createDirectories(Paths.get(alhenaHome));
        if (!new File(alhenaHome + "/cacerts").exists()) {

            // first time
            // java will not negotiate a ssl handshake between a client cert and a server with
            // a self-signed certificate unless the server's certificate is in java's trustore
            // it will work with ca signed server certs but most Gemini servers use self-signed certs
            // ...and no - making a separate truststore with the server certs will not work
            // so...
            // copy Java's original cacerts file and use that to start storing server certs
            // we need to specify this is the new trustore for the app with javax.net.ssl.trustStore
            Path source = Paths.get(System.getProperty("java.home") + "/lib/security/cacerts");
            Path destination = Paths.get(alhenaHome + "/cacerts");

            try {
                Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        System.setProperty("javax.net.ssl.trustStore", alhenaHome + "/cacerts"); // current dir
        System.setProperty("javax.net.ssl.trustStorePassword", "changeit");
        if (SystemInfo.isMacOS) {
            System.setProperty("apple.awt.application.appearance", "system");
            System.setProperty("apple.awt.application.name", PROG_NAME);
            System.setProperty("apple.laf.useScreenMenuBar", "true");

        } else if (SystemInfo.isLinux) {
            // enable custom window decorations
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);
        }

        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Feature.ICON_IMAGE)) {
                URL iconUrl = Alhena.class.getClassLoader().getResource("alhena_256x256.png");
                taskbar.setIconImage(new ImageIcon(iconUrl).getImage());
            }
            if (taskbar.isSupported(Feature.MENU)) {

                taskbar.setMenu(createPopupMenu());
            }

        }
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.APP_ABOUT)) {
                desktop.setAboutHandler(e -> {
                    Component c = frameList.size() == 1 ? frameList.get(0) : null;
                    Util.infoDialog(c, "About", PROG_NAME + " " + VERSION + "\nWritten by Brad Grier");
                });
            }
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
                browsingSupported = true;
            }
            if (desktop.isSupported(Desktop.Action.MAIL)) {
                mailSupported = true;
            }

        }

        // load monospaced font for windows known to correctly align ascii art
        if (SystemInfo.isWindows) {
            Util.loadFont("SourceCodePro-Regular.ttf");
        }
        // load a comprehensive emoji font
        // this is used by default except on macintosh which can display color emojis
        Util.loadFont("NotoEmoji-Regular.ttf");

        // initialize the database
        DB.init();

        EventQueue.invokeLater(() -> {

            String theme = DB.getPref("theme", null);
            if (theme != null) {
                Util.setupTheme(theme);
            } else {
                FlatLightLaf.setup();
                theme = "com.formdev.flatlaf.FlatLightLaf";
                DB.insertPref("theme", theme);
            }

            String u = Util.getHome();
            newWindow(u, u);

        });

        // turn off blocked thread warnings - this is not a server
        VertxOptions options = new VertxOptions().setBlockedThreadCheckInterval(Integer.MAX_VALUE);
        vertx = Vertx.vertx(options);

        createNetClient(null, null, null, null);

    }

    private static PopupMenu createPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();

        MenuItem frameItem = new MenuItem("New Window");
        frameItem.addActionListener(al -> {
            String home = Util.getHome();
            Alhena.newWindow(home, home);
        });
        int idx = 1;
        for (GeminiFrame gf : frameList) {
            MenuItem mi = gf.getMenuItem();
            if (mi == null) {
                mi = new MenuItem("Window: " + idx++);
                gf.setMenuItem(mi);
                mi.addActionListener(al -> {
                    gf.toFront();
                });
            }

            popupMenu.add(mi);
        }

        popupMenu.add(frameItem);
        return popupMenu;
    }

    public static void newWindow(String url, String baseUrl) {
        String theme = DB.getPref("theme", null);
        frameList.add(new GeminiFrame(url, baseUrl, theme));
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Feature.MENU)) {

                taskbar.setMenu(createPopupMenu());
            }
        }
    }

    public static void updateFrames(boolean updateBookmarks) {

        try {
            Class<?> themeClass = Class.forName(DB.getPref("theme", null));
            FlatLaf theme = (FlatLaf) themeClass.getDeclaredConstructor().newInstance();
            FlatLaf.setup(theme);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        GeminiFrame.currentThemeId++;
        for (GeminiFrame jf : frameList) {
            if (updateBookmarks) {
                jf.updateBookmarks();
            }

            jf.refreshFromCache(jf.visiblePage());
            jf.visiblePage().setThemeId(GeminiFrame.currentThemeId);
            SwingUtilities.updateComponentTreeUI(jf);
        }
    }

    public static void exit(GeminiFrame gf) {
        if (frameList.size() == 1) {

            System.exit(0);
        } else {
            gf.shutDown();
            frameList.remove(gf);
            if (Taskbar.isTaskbarSupported()) {
                Taskbar taskbar = Taskbar.getTaskbar();
                if (taskbar.isSupported(Feature.MENU)) {

                    taskbar.setMenu(createPopupMenu());
                }
            }
        }

    }

    public static void processURL(String url, Page p, String redirectUrl, Page cPage) {
        // this method needs to be refactored
        url = url.trim();

        if (url.startsWith("alhena:")) {
            processCommand(url, p);
            return;
        }
        if (url.contains("://")) {
            if (!url.startsWith("gemini://") && !url.startsWith("file://") && !url.startsWith("https://")) {
                p.textPane.end("## Bad scheme\n", false, url, true);
                return;

            }
            if (url.length() == url.indexOf("//") + 2) {
                return;
            }
        }
        try {
            if (url.startsWith("file:/")) {
                p.frame().showGlassPane(true);
                URL fileUrl;
                try {

                    fileUrl = new URL(url);
                    File file = new File(fileUrl.toURI());
                    if (file.exists()) {

                        boolean matches = fileExtensions.stream().anyMatch(url.toLowerCase()::endsWith);
                        if (matches) {
                            //boolean[] first = {true};
                            String fUrl = url;
                            boolean pformatted = !(url.endsWith(".gmi") || url.endsWith(".gemini"));
                            p.textPane.updatePage("", pformatted, fUrl, true);
                            boolean isImage = imageExtensions.stream().anyMatch(url.toLowerCase()::endsWith);

                            Buffer imageBuffer = Buffer.buffer();

                            vertx.fileSystem().open(file.getAbsolutePath(), new OpenOptions().setRead(true), result -> {
                                if (result.succeeded()) {
                                    AsyncFile asyncFile = result.result();

                                    // read the file in chunks
                                    asyncFile.handler(buffer -> {
                                        if (isImage) {
                                            imageBuffer.appendBuffer(buffer);
                                        } else {
                                            bg(() -> {
                                                p.textPane.addPage(buffer.toString());
                                            });
                                        }

                                    });

                                    // process the content when reading is done
                                    asyncFile.endHandler(v -> {
                                        if (isImage) {
                                            bg(() -> {
                                                p.textPane.end(" ", false, fUrl, true);
                                                p.textPane.insertImage(imageBuffer.getBytes());
                                                p.frame().showGlassPane(false);
                                            });

                                        } else {
                                            bg(() -> {
                                                p.textPane.end();
                                                p.frame().showGlassPane(false);
                                            });
                                        }

                                        asyncFile.close();

                                    });

                                    asyncFile.exceptionHandler(throwable -> {

                                        bg(() -> {
                                            p.textPane.end("## Error reading file\n", false, fUrl, true);
                                            p.frame().showGlassPane(false);
                                        });
                                    });
                                } else {

                                    bg(() -> {
                                        p.textPane.end("## Error opening file\n", false, fUrl, true);
                                        p.frame().showGlassPane(false);
                                    });
                                }
                            });

                        } else {
                            p.textPane.end("## Invalid file type\n", false, url, true);
                            p.frame().showGlassPane(false);
                        }
                    } else {
                        p.textPane.end("## File not found\n", false, url, true);
                        p.frame().showGlassPane(false);
                    }
                } catch (Exception ex) {
                    p.textPane.end("## Error reading file\n" + ex.getMessage() + "\n", false, url, true);
                    ex.printStackTrace();
                    p.frame().showGlassPane(false);
                }

                return;
            }

            URI prevURI = redirectUrl == null ? p.textPane.getURI() : new URI(redirectUrl);

            if (url.startsWith("https://") || (!url.startsWith("gemini://") && prevURI.getScheme().equals("https"))) {

                if (!url.startsWith("https")) {
                    url = prevURI.resolve(url).toString();
                }

                if (httpClient == null) {
                    HttpClientOptions options = new HttpClientOptions().
                            setSsl(true).
                            setTrustAll(true)
                            .setLogActivity(true);
                    httpClient = vertx.createHttpClient(options);
                }
                p.frame().showGlassPane(true);
                String finalURL = url;
                URI finalURI = new URI(finalURL);

                httpClient.request(HttpMethod.GET, 443, finalURI.getHost(), finalURI.getPath()).onComplete(ar -> {
                    HttpClientRequest req = ar.result();
                    req.send().onComplete(ar2 -> {
                        if (ar2.succeeded()) {
                            HttpClientResponse resp = ar2.result();

                            String contentType = resp.getHeader("Content-Type");
                            if (contentType != null && contentType.startsWith("text/html")) {
                                resp.body().onSuccess(buffer -> {
                                    bg(() -> {
                                        p.textPane.end(convertHtmlToGemtext(buffer.toString(), finalURL), false, finalURL, true);
                                        p.frame().showGlassPane(false);
                                    });
                                    req.end();
                                }).onFailure(ex -> {
                                    ex.printStackTrace();
                                    bg(() -> {
                                        p.textPane.end("error getting web page\n", true, finalURL, true);
                                        p.frame().showGlassPane(false);
                                    });
                                    req.end();
                                });

                            } else {
                                try {
                                    // download!
                                    File[] file = new File[1];
                                    resp.pause();
                                    EventQueue.invokeAndWait(() -> {
                                        String fileName = finalURL.substring(finalURL.lastIndexOf("/") + 1);
                                        file[0] = Util.getFile(p.frame(), fileName, false, "Save File", null);

                                    });
                                    if (file[0] != null) {
                                        vertx.fileSystem().open(file[0].getAbsolutePath(), new OpenOptions().setCreate(true).setTruncateExisting(true), fileResult -> {
                                            if (fileResult.succeeded()) {
                                                AsyncFile af = fileResult.result();
                                                resp.resume();

                                                Pump pump = Pump.pump(resp, af);
                                                pump.start();

                                                resp.endHandler(eh -> {
                                                    af.close();

                                                    req.end();
                                                    bg(() -> {
                                                        Util.infoDialog(p.frame(), "Complete", file[0].getName() + " downloaded");
                                                        p.frame().showGlassPane(false);
                                                    });
                                                });

                                            }
                                        });
                                    } else {
                                        req.end();
                                        bg(() -> {

                                            p.frame().showGlassPane(false);
                                        });
                                    }

                                } catch (InterruptedException ex) {
                                } catch (InvocationTargetException ex) {
                                }
                            }

                        } else {
                            ar2.cause().printStackTrace();
                            bg(() -> {
                                p.textPane.end("broke\n", true, finalURL, true);
                                p.frame().showGlassPane(false);
                            });

                        }
                    });

                });

                return;

            }

            if (!url.startsWith("gemini://")) {
                if (url.startsWith("//")) {
                    url = "gemini:" + url;
                } else if (url.startsWith("/")) {
                    String port = prevURI.getPort() != -1 ? ":" + prevURI.getPort() : "";
                    url = "gemini://" + prevURI.getHost() + port + url;
                } else {
                    if (url.startsWith("?")) {
                        String port = prevURI.getPort() != -1 ? ":" + prevURI.getPort() : "";
                        url = "gemini://" + prevURI.getHost() + port + prevURI.getPath() + "?" + url.substring(1);

                    } else {
                        url = prevURI.resolve(url).toString();
                    }

                }

            }
            URI origURI = new URI(url).normalize();
            String origURL = origURI.toString();
            if (origURI.getPath().isEmpty()) {

                origURL = origURL + "/";
            }

            String hostPart = url.split("://")[1].split("/")[0];

            for (char c : hostPart.toCharArray()) { // handle emoji
                if (c > 127) {
                    String punycodeHost = IDN.toASCII(hostPart, IDN.ALLOW_UNASSIGNED);
                    url = url.replace(hostPart, punycodeHost);
                    break;
                }
            }

            URI punyURI = new URI(url).normalize();

            // switching domains - stop using domain cert
            if (netClientDomainCert != null && !netClientDomainCert.equals(punyURI.getHost())) {
                createNetClient(punyURI, p, origURL, cPage);
            } else {
                fetch(client, punyURI, p, origURL, cPage);
            }
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

    }

    private static void fetch(NetClient client, URI uri, Page p, String origURL, Page cPage) {
        p.frame().showGlassPane(true);

        String host = uri.getHost();

        // TODO: make sure there's no fragment - I think
        int[] port = {uri.getPort()};

        if (port[0] == -1) {
            port[0] = 1965;
        }
        client.connect(port[0], host, connection -> {
            if (connection.succeeded()) {
                boolean[] proceed = {true};
                X509Certificate[] cert = new X509Certificate[1];
                try {
                    List<Certificate> certList = connection.result().peerCertificates();
                    cert[0] = (X509Certificate) certList.get(0);
                    p.setCert(cert[0]);
                    String res = verifyFingerPrint(host + ":" + port[0], cert[0]);

                    if (res != null) {
                        try {
                            // this blocks vertx event loop
                            EventQueue.invokeAndWait(() -> {
                                Object diagRes = Util.confirmDialog(p.frame(), "Certificate Issue", res + "\nDo you want to continue?", JOptionPane.YES_NO_OPTION);
                                if (diagRes instanceof Integer result) {
                                    proceed[0] = result == JOptionPane.YES_OPTION;
                                } else {
                                    proceed[0] = false;
                                }

                            });
                        } catch (InterruptedException | InvocationTargetException ex) {
                        }
                        if (!proceed[0]) {

                            connection.result().close();
                            p.frame().showGlassPane(false);
                            return;
                        } else {
                            saveCert(host + ":" + port[0], (X509Certificate) certList.get(0));
                        }
                    }
                } catch (SSLPeerUnverifiedException ex) {
                    ex.printStackTrace(); // TODO: WHAT TO DO HERE
                }
                System.out.println("connected: " + host);
                // wrap for the lambda
                boolean[] firstBuffer = {true};
                // if it turns out this is an image request we need to track where it starts
                int[] imageStartIdx = {-1};

                String urlText = uri.toString();

                connection.result().write(urlText + "\r\n");
                Buffer saveBuffer = Buffer.buffer();

                boolean[] error = {false};
                // Handle the response
                connection.result().handler(buffer -> {

                    // since we're asynchronous, we have to make sure we have the entire header before
                    // we proceed
                    if (firstBuffer[0]) {

                        saveBuffer.appendBuffer(buffer);
                        char respCode = (char) saveBuffer.getByte(0);

                        boolean firstLine = false;
                        int i;
                        for (i = 0; i < saveBuffer.length(); i++) {
                            if (((char) saveBuffer.getByte(i)) == '\n') {
                                //end of first line
                                firstLine = true;
                                break;
                            }
                        }
                        if (!firstLine) {
                            return;
                        }
                        firstBuffer[0] = false;

                        switch (respCode) {
                            case '1' -> {
                                redirectCount = 0;

                                String reqMsg = saveBuffer.getString(3, i - 1);
                                char respType = (char) saveBuffer.getByte(1);
                                bg(() -> {
                                    if (respType != '1') {

                                        String input = Util.inputDialog(p.frame(), "Server Request", reqMsg, false);
                                        if (input != null) {

                                            String questionMark = uri.toString().endsWith("?") ? "" : "?";
                                            processURL(uri + questionMark + URLEncoder.encode(input).replace("+", "%20"), p, null, cPage);

                                        }

                                    } else {

                                        String input = Util.inputDialog(p.frame(), "Server Request", reqMsg, true);
                                        if (input != null) {

                                            String questionMark = uri.toString().endsWith("?") ? "" : "?";
                                            processURL(uri + questionMark + URLEncoder.encode(input).replace("+", "%20"), p, null, cPage);

                                        }

                                    }
                                });
                            }
                            case '2' -> {
                                redirectCount = 0;
                                // have to assume there's at least one byte
                                String mime = saveBuffer.getString(3, i - 1);
                                if (mime.isBlank()) {
                                    mime = "text/gemini"; // apparently mime type is optional in type 20 -  NOT ANYMORE
                                }

                                final String chunk = saveBuffer.getString(i + 1, saveBuffer.length(), "UTF-8");
                                if (mime.startsWith("text/gemini")) {

                                    bg(() -> {
                                        p.textPane.updatePage(chunk, false, origURL, true);
                                    });
                                } else if (mime.startsWith("text/")) {
                                    bg(() -> {
                                        p.textPane.updatePage(chunk, true, origURL, true);
                                    });
                                } else if (mime.startsWith("image/")) {
                                    imageStartIdx[0] = i + 1;
                                    try {
                                        DB.insertHistory(origURL);
                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }

                                } else {
                                    File[] file = new File[1];

                                    try {
                                        //error[0] = true;
                                        connection.result().pause();
                                        EventQueue.invokeAndWait(() -> {
                                            String fileName = origURL.substring(origURL.lastIndexOf("/") + 1);
                                            file[0] = Util.getFile(p.frame(), fileName, false, "Save File", null);

                                        });
                                    } catch (Exception ex) {
                                        ex.printStackTrace();
                                    }

                                    if (file[0] == null) {
                                        // canceled
                                        //textPane.end("# Download canceled", false, origURL, true, r);
                                        connection.result().close();
                                        p.frame().showGlassPane(false);
                                        return;
                                    }

                                    streamToFile(connection.result(), file[0], saveBuffer.slice(i + 1, saveBuffer.length()), p, origURL);
                                    return;
                                }
                            }

                            case '3' -> {
                                if (redirectCount++ == 6) {
                                    redirectCount = 0;
                                    connection.result().close();
                                    bg(() -> {
                                        p.textPane.updatePage("## Too many redirects", false, origURL, true);
                                        p.frame().showGlassPane(false);
                                    });
                                    return;
                                }
                                String redirectURI = saveBuffer.getString(3, i - 1).trim();
                                redirectCount++;
                                //vertx.eventBus().send("fetch", redirectURI);
                                processURL(redirectURI, p, origURL, cPage);
                            }
                            case '4', '5' -> {
                                redirectCount = 0;
                                String errorMsg = saveBuffer.getString(0, i - 1).trim();
                                //char respType = (char) saveBuffer.getByte(1);
                                bg(() -> {
                                    p.textPane.updatePage("## Server Response: " + errorMsg, false, origURL, true);
                                });
                            }
                            case '6' -> {
                                redirectCount = 0;
                                char respType = (char) saveBuffer.getByte(1);
                                if (respType == '0') { // 60 cert required
                                    String msg = saveBuffer.getString(3, i - 1).trim();
                                    certRequired(msg, uri.toString(), host, p, cert[0], cPage);
                                } else if (respType == 1 || respType == 2) {
                                    String errorMsg = saveBuffer.getString(0, i - 1).trim();

                                    bg(() -> {
                                        p.textPane.updatePage("## Server Response: " + errorMsg, false, origURL, true);
                                    });
                                }
                            }
                            default -> {
                                redirectCount = 0;
                                connection.result().close();
                                bg(() -> {
                                    p.textPane.updatePage("## Invalid response", false, origURL, true);
                                    p.frame().showGlassPane(false);
                                });
                                return;

                            }
                        }

                    } else {
                        redirectCount = 0;
                        if (!error[0]) {
                            if (imageStartIdx[0] != -1) {
                                saveBuffer.appendBuffer(buffer);
                            } else {

                                bg(() -> {
                                    //System.out.println("buffer: " + buffer.length());
                                    p.textPane.addPage(buffer.toString());
                                });
                            }

                        }
                    }

                });

                connection.result().closeHandler(v -> {
                    redirectCount = 0;
                    System.out.println("connection closed");
                    if (imageStartIdx[0] != -1) {

                        bg(() -> {
                            // insert into existing page and not new page created for the call to processUrl
                            // get rid of this - maybe put spawning page in page ref

                            //GeminiTextPane tPane = p.frame().visiblePage().textPane;
                            GeminiTextPane tPane = cPage.textPane;

                            if (tPane.awatingImage()) {
                                tPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));

                            } else {
                                p.textPane.end(" ", false, origURL, true);
                                p.textPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));
                            }
                            p.frame().showGlassPane(false);
                        });
                    } else {
                        bg(() -> {
                            p.frame().showGlassPane(false);
                            p.textPane.end();

                        });
                    }

                });
            } else {
                redirectCount = 0;
                if (interrupted) {
                    interrupted = false;
                    p.frame().showGlassPane(false);
                } else {
                    bg(() -> {
                        p.textPane.end(connection.cause().toString() + "\n", true, origURL, true);
                        p.frame().showGlassPane(false);
                    });
                    connection.cause().printStackTrace();
                    System.out.println("Failed to connect: " + connection.cause().getMessage());
                }
            }
        });
    }

    private static String verifyFingerPrint(String host, X509Certificate cert) {

        try {
            //Object[] certInfo = DB.getCert(host);
            CertInfo certInfo = DB.getServerCert(host);
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException ex) {

                if (certInfo.lastModified() != null) {
                    java.sql.Timestamp certExpires = new java.sql.Timestamp(cert.getNotAfter().getTime());
                    if (certInfo.lastModified().before(certExpires)) {
                        return "Server certificate has expired.";
                    }
                } else {
                    return "Server certificate has expired.";
                }
            } catch (CertificateNotYetValidException cex) {
                return "Server certificate is not yet valid.";
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] certBytes = cert.getEncoded();
            byte[] hash = md.digest(certBytes);

            // Convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }

            String fp = hexString.substring(0, hexString.length());

            String dbFingerprint = certInfo.fingerPrint();

            java.sql.Timestamp expires = certInfo.expires();

            if (dbFingerprint == null) {

                java.sql.Timestamp ts = new java.sql.Timestamp(cert.getNotAfter().getTime());
                DB.upsertCert(host, fp, ts);   // TOFU  
            } else if (!fp.equals(dbFingerprint) && expires.before(new Date())) {

                return "Server certificate has changed without expiring.";
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return "Unable to validate certificate.";
        }

        return null;
    }

    private static void saveCert(String host, X509Certificate cert) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] certBytes = cert.getEncoded();
            byte[] hash = md.digest(certBytes);

            // convert to hex
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02X", b));
            }

            String fp = hexString.substring(0, hexString.length());
            java.sql.Timestamp ts = new java.sql.Timestamp(cert.getNotAfter().getTime());
            DB.upsertCert(host, fp, ts);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void streamToFile(NetSocket socket, File outFile, Buffer buffer, Page p, String url) {
        vertx.fileSystem().open(outFile.getAbsolutePath(), new OpenOptions().setWrite(true).setCreate(true), fileRes -> {
            if (fileRes.succeeded()) {
                AsyncFile file = fileRes.result();

                file.write(buffer);

                socket.handler(null);
                socket.resume();
                // Use Pump to stream to file
                Pump pump = Pump.pump(socket, file);
                pump.start();

                // Close file when socket closes
                socket.closeHandler(v -> {
                    file.close();
                    bg(() -> {
                        try {
                            DB.insertHistory(url);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        p.frame().showGlassPane(false);
                        Util.infoDialog(p.frame(), "Success", outFile.getName() + " saved");
                    });

                });

            } else {
                p.textPane.end("# Failed to open file: " + outFile, false, url, true);
                p.frame().showGlassPane(false);
                fileRes.cause().printStackTrace();

            }
        });
    }

    private static void bg(Runnable r) {
        EventQueue.invokeLater(() -> {
            r.run();
        });
    }

    public static void createNetClient() {
        if (client != null) {
            client.close();
        }
        NetClientOptions options = new NetClientOptions()
                .setConnectTimeout(15000)
                .setSsl(true) // Gemini uses TLS   
                .setTrustAll(true)
                .setHostnameVerificationAlgorithm("HTTPS");
        client = vertx.createNetClient(options);
    }

    private static void createNetClient(URI uri, Page p, String origURL, Page cPage) {
        createNetClient();
        if (uri != null) {
            fetch(client, uri, p, origURL, cPage);
        }

    }

    private static void createNetClientWithCert(String host, String cn) {

        NetClientOptions options = new NetClientOptions()
                .setSsl(true) // Gemini uses TLS
                .setTrustAll(true) // For testing, bypass certificate checks
                .setHostnameVerificationAlgorithm("HTTPS")
                .setSslEngineOptions(new JdkSSLEngineOptions() {
                    @Override
                    public SslContextFactory sslContextFactory() {
                        return () -> {
                            try {
                                return new JdkSslContext(
                                        createSSLContext(host, cn),
                                        true,
                                        null,
                                        IdentityCipherSuiteFilter.INSTANCE,
                                        ApplicationProtocolConfig.DISABLED,
                                        io.netty.handler.ssl.ClientAuth.NONE,
                                        null,
                                        false);
                            } catch (Exception ex) {

                                ex.printStackTrace();
                                return null;
                            }
                        };
                    }
                });
        client = vertx.createNetClient(options);

        netClientDomainCert = host;

    }

    private static void certRequired(String msg, String reqURL, String host, Page p, X509Certificate cert, Page cPage) {

        ClientCertInfo certInfo = null;
        try {
            certInfo = DB.getClientCertInfo(host);
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        if (certInfo != null) {
            createNetClientWithCert(host, null);

            processURL(reqURL, p, null, cPage);
        } else {

            bg(() -> {

                String cn = Util.inputDialog(
                        p.frame(),
                        "Certificate", "Server: '" + msg + "'\nDo you want to create a new client certificate for " + host + "?\n\nCertificate Name:",
                        false,
                        PROG_NAME);
                if (cn != null) {
                    String cnString = cn.isEmpty() ? PROG_NAME : cn;
                    addCertToTrustStore(host, cert);
                    client.close().onSuccess(s -> {
                        createNetClientWithCert(host, cnString);
                        //vertx.eventBus().send("fetch", reqURL);
                        processURL(reqURL, p, null, cPage);

                    });
                }
            });
        }
    }

    public static void addCertToTrustStore(String host, X509Certificate cert) {
        // add server certs for sites that require a client certificate

        String cacertsPath = System.getProperty("alhena.home") + "/cacerts"; // Default cacerts path
        String cacertsPassword = "changeit"; // Default cacerts password
        File f = new File(cacertsPath);
        if (f.exists()) {
            try {

                // load the cacerts keystore
                KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream is = new FileInputStream(cacertsPath)) {
                    keyStore.load(is, cacertsPassword.toCharArray());
                }

                if (keyStore.containsAlias(host)) {
                    keyStore.deleteEntry(host);
                }

                // add the certificate to the keystore
                keyStore.setCertificateEntry(host, cert);

                try (FileOutputStream os = new FileOutputStream(cacertsPath)) {
                    keyStore.store(os, cacertsPassword.toCharArray());
                }
            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static SSLContext createSSLContext(String host, String cn) throws Exception {
        // register Bouncy Castle as a security provider
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate cert;
        PrivateKey privateKey;

        ClientCertInfo certInfo = DB.getClientCertInfo(host);
        if (certInfo != null) {
            cert = (X509Certificate) loadCertificate(certInfo.cert());

            privateKey = loadPrivateKey(certInfo.privateKey());
        } else {
            // generate a key pair
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(2048);
            KeyPair keyPair = keyGen.generateKeyPair();
            privateKey = keyPair.getPrivate();
            // create a self-signed certificate
            cert = generateSelfSignedCertificate(keyPair, cn);

            String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                    + "\n-----END PRIVATE KEY-----";

            String certPem = "-----BEGIN CERTIFICATE-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded())
                    + "\n-----END CERTIFICATE-----";

            DB.insertClientCert(host, certPem, privateKeyPem);

        }

        // create a KeyStore and load the certificate and private key
        KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(null, null); // initialize an empty KeyStore
        keyStore.setKeyEntry("client-cert", privateKey, "password".toCharArray(),
                new java.security.cert.Certificate[]{cert});

        // create a KeyManagerFactory and initialize it with the KeyStore
        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(keyStore, "password".toCharArray());

        // create and initialize the SSLContext
        SSLContext sslContext = SSLContext.getInstance("TLS");

        sslContext.init(kmf.getKeyManagers(), null, new SecureRandom());
        return sslContext;
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String cn) throws Exception {

        //X500Name subject = new X500Name("CN=Jeremy,O=UltimatumLabs,L=Elkhorn,C=US");
        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + ((365L * 24 * 60 * 60 * 1000) * 5)); // 5 year validity

        // build the certificate
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        // sign the certificate
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);

        // convert to X509Certificate
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public static void savePrivateKey(PrivateKey privateKey, String filePath) throws Exception {
        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(privateKeyPem);
        }
        System.out.println("Private key saved to " + filePath);
    }

    public static void saveCertificate(Certificate certificate, String filePath) throws Exception {
        String certPem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certificate.getEncoded())
                + "\n-----END CERTIFICATE-----";
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(certPem);
        }
        System.out.println("Certificate saved to " + filePath);
    }

    public static PrivateKey loadPrivateKey(String keyString) throws Exception {

        String key = keyString
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s+", "");
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);

    }

    public static Certificate loadCertificate(String cert) throws Exception {

        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        InputStream is = new ByteArrayInputStream(cert.getBytes());
        return cf.generateCertificate(is);
    }

    // this routine is pretty basic right now - some opportunity here
    private static String convertHtmlToGemtext(String html, String host) {
        StringBuilder gemtext = new StringBuilder();

        Document doc = Jsoup.parse(html);

        for (Element element : doc.body().children()) {
            gemtext.append(processElement(element, host)).append("\n");
        }

        return gemtext.toString();
    }

    private static String processElement(Element element, String host) {
        String tagName = element.tagName();
        return switch (tagName) {

            case "div" -> {
                StringBuilder gt = new StringBuilder();
                element.children().stream().forEach(child -> {
                    gt.append(processElement(child, host)).append("\n");
                });
                yield gt.toString();
            }
            case "h1", "h2", "h3" ->
                "#".repeat(tagName.charAt(1) - '0') + " " + element.text();
            case "p" ->
                element.text();
            case "a" -> {
                String href = element.attr("href");
                String origHref = href;
                if (href.startsWith("/")) {
                    href = host + href;
                }
                String elemText = element.text();
                if (elemText == null || elemText.isEmpty()) {
                    elemText = origHref;
                }
                yield "=> " + href + " " + elemText;
            }
            case "ul", "ol" ->
                processList(element);
            case "li" ->
                "* " + element.text();
            default ->
                element.text();
        };
    }

    private static String processList(Element element) {
        StringBuilder list = new StringBuilder();
        for (Element li : element.children()) {
            list.append(processElement(li, null)).append("\n");
        }
        return list.toString();
    }

    private static void processCommand(String url, Page p) {
        boolean plainText = false;
        String[] cmd = url.substring(url.indexOf(':') + 1).split("=");
        String message = "## Unknown command\n";
        if (cmd.length == 1) {
            if (cmd[0].equals("pngemoji")) {
                message = "# pngemoji\n###Set emoji rendering type: pngemoji=true\ntrue required for color emojis on non-Mac platforms.";
            } else if (cmd[0].equals("scrollspeed")) {
                message = "# scrollspeed\n###Set the mouse wheel speed: scrollspeed=10\nscrollspeed=default resets. Negative numbers reverse scroll direction.";

            } else if (cmd[0].equals("info")) {
                plainText = true;
                message = getAlhenaInfo().toString();
            }

        } else if (cmd.length == 2) {
            if (cmd[0].equals("scrollspeed")) {
                try {
                    if (cmd[1].equals("default")) {
                        DB.insertPref("scrollspeed", null);
                        message = "## scrollspeed reset\n";
                    } else {
                        int val = Integer.parseInt(cmd[1]);
                        DB.insertPref("scrollspeed", cmd[1]);
                        for (GeminiFrame gf : frameList) {
                            gf.setScrollIncrement(val);
                        }
                        message = "## " + cmd[0] + " set to " + cmd[1] + "\n";
                    }

                } catch (NumberFormatException ex) {
                    message = "## Value must be a number\n";
                }

            } else if (cmd[0].equals("pngemoji")) {
                if (!cmd[1].toLowerCase().equals("true") && !cmd[1].toLowerCase().equals("false")) {
                    message = "## must be true or false\n";
                } else {
                    DB.insertPref("pngemoji", cmd[1].toLowerCase());
                    EventQueue.invokeLater(() -> {
                        updateFrames(false);
                    });

                    message = "## " + cmd[0] + " set to " + cmd[1].toLowerCase() + "\n";
                }
            }
        }
        p.textPane.end(message, plainText, url, true);

    }

    public static StringBuilder getAlhenaInfo() {
        StringBuilder sb = new StringBuilder();

        sb.append(PROG_NAME + " " + VERSION + "\n\n");
        sb.append("Java version: ").append(System.getProperty("java.version")).append("\n");
        sb.append("Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("OS name: ").append(System.getProperty("os.name")).append("\n");
        sb.append("Architecture: ").append(System.getProperty("os.arch")).append("\n");
        sb.append("OS version: ").append(System.getProperty("os.version")).append("\n\n");

        Runtime runtime = Runtime.getRuntime();

        long maxMemory = runtime.maxMemory();
        long totalMemory = runtime.totalMemory();
        long allocatedMemory = totalMemory - runtime.freeMemory();
        long freeMemory = runtime.freeMemory();
        sb.append("Max memory: " + maxMemory / (1024 * 1024) + " MB\n");
        sb.append("Total memory: " + totalMemory / (1024 * 1024) + " MB\n");
        sb.append("Allocated memory: " + allocatedMemory / (1024 * 1024) + " MB\n");
        sb.append("Free memory: " + freeMemory / (1024 * 1024) + " MB\n\n");

        sb.append("Documents: \n");
        for (GeminiFrame gf : frameList) {
            gf.getInfo(sb);
        }

        return sb;
    }

}
