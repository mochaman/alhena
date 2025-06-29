package brad.grier.alhena;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.KeyboardFocusManager;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Taskbar;
import java.awt.Taskbar.Feature;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigInteger;
import java.net.IDN;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
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
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
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
import io.vertx.core.http.impl.MimeMapping;
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
    private static HttpClient httpClient;

    private final static List<GeminiFrame> frameList = new ArrayList<>();
    public final static String PROG_NAME = "Alhena";
    public final static String WELCOME_MESSAGE = "Welcome To " + PROG_NAME;
    public final static String VERSION = "5.1.5";
    private static volatile boolean interrupted;
    // TODO: combine the other lists to arrive at this
    public static final List<String> fileExtensions = List.of(".txt", ".gemini", ".gmi", ".log", ".html", ".pem", ".csv", ".png", ".jpg", ".jpeg", ".mp4", ".mp3", ".ogg", ".opus", ".mov", ".webp", ".flac", ".xml", ".wav", ".json");
    public static final List<String> imageExtensions = List.of(".png", ".jpg", ".jpeg", ".webp");
    public static final List<String> mediaExtensions = List.of(".mp4", ".mp3", ".ogg", ".opus", ".mov", ".flac", ".wav");
    public static final List<String> videoExtensions = List.of(".mp4", ".mov");
    public static final List<String> txtExtensions = List.of(".txt", ".gemini", ".gmi", ".log", ".html", ".csv", ".xml", ".json");
    public static boolean browsingSupported, mailSupported;
    private static final Map<ClientCertInfo, NetClient> certMap = Collections.synchronizedMap(new HashMap<>());
    private static String theme;
    public static String httpProxy;
    public static String gopherProxy;
    private static NetClient spartanClient = null;
    private static final int MOD = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private static final int MODIFIER = (InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK);
    private static final ArrayList<String> cnConfirmedList = new ArrayList<>();
    private static boolean keyDown;
    private static LinkGlassPane lgp;
    public static boolean allowVLC;
    private static final List<String> allowedSchemes = List.of(
            "gemini://", "file://", "spartan://", "nex://",
            "https://", "http://", "titan://"
    );

    public static void main(String[] args) throws Exception {
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher((KeyEvent e) -> {
            Component source = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            GeminiFrame gf = (GeminiFrame) SwingUtilities.getAncestorOfClass(GeminiFrame.class, source);
            if (gf == null) {
                return false;
            }
            if (e.getID() == KeyEvent.KEY_RELEASED) {
                lgp = null;
                keyDown = false;
                if (gf.getGlassPane() instanceof LinkGlassPane) {
                    gf.getGlassPane().setVisible(false);
                }
                return false;
            } else if (e.getID() == KeyEvent.KEY_PRESSED) {

                KeyStroke ks = KeyStroke.getKeyStrokeForEvent(e);

                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && !keyDown) {
                    keyDown = true;

                    if (gf.visiblePage().busy()) {
                        gf.visiblePage().setBusy(false);
                        gf.showGlassPane(false);
                        interrupted = true;
                        // closing and recreating the client doesn't work for ending connection handshake
                        // close vertx and recreate
                        httpClient = null;
                        spartanClient = null;
                        vertx.close();
                        VertxOptions options = new VertxOptions().setBlockedThreadCheckInterval(Integer.MAX_VALUE);
                        vertx = Vertx.vertx(options);

                        // reset all connections in map
                        certMap.clear();
                        e.consume();
                        return true; // consume
                    }
                    return false;
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_UP, (MOD | KeyEvent.SHIFT_DOWN_MASK)))) {
                    // go to root
                    URI uri = gf.visiblePage().textPane().getURI();
                    if (uri.getHost() != null && uri.getScheme() != null) {
                        URI rootURI = URI.create(uri.getScheme() + "://" + uri.getHost());
                        gf.fetchURL(rootURI.toString());
                    }
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_E, (MOD | KeyEvent.ALT_DOWN_MASK)))) {
                    // titan edit
                    gf.editPage();
                    e.consume();
                    return true;

                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_C, MOD))) {

                    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                    String select = gf.visiblePage().textPane.getSelectedText();
                    if (select != null) {
                        StringSelection selectedText = new StringSelection(select);
                        clipboard.setContents(selectedText, selectedText);
                        e.consume();
                        return true;
                    }
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0))) {
                    gf.findAgain();
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_L, MOD))) {
                    gf.focusOnAddressBar();
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_OPEN_BRACKET, MOD))) {
                    gf.backButton.doClick();
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_CLOSE_BRACKET, MOD))) {
                    gf.forwardButton.doClick();
                } else if (ks.equals(KeyStroke.getKeyStroke(KeyEvent.VK_R, MOD))) {
                    gf.refreshButton.doClick();

                } else {

                    if ((e.getModifiersEx() & MODIFIER) == MODIFIER) {

                        if (!keyDown) {
                            int keyCode = e.getKeyCode();
                            int index = -1;

                            if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {  // 1-9 → indices 0-8
                                index = keyCode - KeyEvent.VK_1;
                            } // A-Z → indices 9-34
                            else if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
                                index = 9 + (keyCode - KeyEvent.VK_A);
                            }

                            if (index >= 0 && lgp != null) {

                                keyDown = true;

                                lgp.setVisible(false);

                                //lgp = null;
                                gf.visiblePage().textPane.clickVisibleLink(index);
                                e.consume();
                                return true;

                            }
                        }

                        if (lgp == null) {
                            lgp = new LinkGlassPane(gf.visiblePage().textPane);
                            gf.setGlassPane(lgp);
                            lgp.setVisible(true);
                            gf.repaint();
                        }

                    } else if (gf.visiblePage().textPane.hasFocus()) {

                        Runnable r = gf.actionMap.get(ks);
                        if (r != null) {
                            r.run();
                            e.consume();
                            return true;
                        }
                    }

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
                    Util.showAbout(c);
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
        allowVLC = DB.getPref("allowvlc", "false").equals("true");

        httpProxy = DB.getPref("httpproxy", null);
        gopherProxy = DB.getPref("gopherproxy", null);

        EventQueue.invokeLater(() -> {
            int contentP = Integer.parseInt(DB.getPref("contentwidth", "80"));
            GeminiTextPane.contentPercentage = (float) ((float) contentP / 100f);
            GeminiTextPane.wrapPF = DB.getPref("linewrappf", "false").equals("true");

            theme = DB.getPref("theme", null);
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
    }

    private static PopupMenu createPopupMenu() {
        PopupMenu popupMenu = new PopupMenu();

        MenuItem frameItem = new MenuItem("New Window");
        frameItem.addActionListener(al -> {
            String home = Util.getHome();
            newWindow(home, home);
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
        frameList.add(new GeminiFrame(url, baseUrl));
        GeminiFrame.ansiAlert = DB.getPref("ansialert", "false").equals("true");
        if (Taskbar.isTaskbarSupported()) {
            Taskbar taskbar = Taskbar.getTaskbar();
            if (taskbar.isSupported(Feature.MENU)) {

                taskbar.setMenu(createPopupMenu());
            }
        }
    }

    public static void updateFrames(boolean updateBookmarks, boolean updateWindowsMenu) {
        String dbTheme = DB.getPref("theme", null);
        if (!theme.equals(dbTheme)) {
            theme = dbTheme;
            try {
                Class<?> themeClass = Class.forName(dbTheme);
                FlatLaf lafTheme = (FlatLaf) themeClass.getDeclaredConstructor().newInstance();
                FlatLaf.setup(lafTheme);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        GeminiFrame.currentThemeId++; // rename - encompasses more than theme...font, etc
        for (GeminiFrame jf : frameList) {
            if (updateBookmarks) {
                jf.updateBookmarks();
            }
            if (updateWindowsMenu) {
                jf.updateWindowsMenu();

                boolean smoothScroll = DB.getPref("smoothscrolling", "true").equals("true");
                jf.forEachPage(page -> {
                    if (smoothScroll) {
                        page.textPane.setupAdaptiveScrolling();
                    } else {
                        page.textPane.removeAdaptiveScrolling();
                    }
                });

            }
            jf.recolorIcons();

            jf.forEachPage(page -> {
                page.ignoreStart();
            });
            jf.visiblePage().setThemeId(GeminiFrame.currentThemeId);
            jf.refreshFromCache(jf.visiblePage());

            SwingUtilities.updateComponentTreeUI(jf);
            jf.initComboBox(); // combo box loses key listener & mouse listener when theme changes
        }
    }

    public static void exit(GeminiFrame gf) {
        for (GeminiFrame jf : frameList) {
            jf.forEachPage(page -> {
                page.textPane().closePlayers();
            });
        }
        if (frameList.size() == 1) {
            gf.setVisible(false); // closes faster for the naysayers
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

    // only call from EDT
    public static void processURL(String url, Page p, String redirectUrl, Page cPage) {

        if (!EventQueue.isDispatchThread()) {

            String finalUrl = url;
            bg(() -> {
                processURL(finalUrl, p, redirectUrl, cPage);
            });
            return;
        }
        // this method needs to be refactored
        url = url.trim();

        if (url.startsWith("alhena:")) {
            processCommand(url, p);
            return;
        }
        int idx = url.indexOf("://");
        if (idx != -1) {
            String lcScheme = url.substring(0, idx).toLowerCase();
            url = lcScheme + url.substring(idx);
            boolean isGopher = gopherProxy != null && url.startsWith("gopher");
            if (allowedSchemes.stream().noneMatch(url::startsWith) && !isGopher) {
                p.textPane.end("## Bad scheme\n", false, url, true);
                return;

            }
            if (url.length() == url.indexOf("//") + 2) {
                return;
            }
        }

        URI prevURI = redirectUrl == null ? p.textPane.getURI() : URI.create(redirectUrl);

        URI checkURI = URI.create(url);

        // because getHost() can return null for hosts with emoji
        String authority = checkURI.getAuthority();
        String host = authority != null ? authority.split(":")[0] : null;

        if (prevURI != null) {
            if (url.startsWith("//")) {
                url = prevURI.getScheme() + ":" + url;
            } else {

                if (host == null && !"file".equals(checkURI.getScheme())) {

                    if (checkURI.getScheme() == null) {
                        //URI.resolve is removing last segment of path when checkURI is only a query???
                        if (url.startsWith("?")) {
                            // do not preserve prevURI's query if there is one
                            url = prevURI.getScheme() + "://" + prevURI.getHost() + prevURI.getPath() + url;

                        } else {
                            url = prevURI.resolve(checkURI).toString();
                        }
                    } else {
                        //  corner case - no host but there's a scheme - is this legal?
                        // spartan://greatfractal.com/
                        url = prevURI.resolve(URI.create(checkURI.getPath())).toString();
                    }

                }
            }
        }

        if (!url.contains(":/")) {
            p.textPane.end("Invalid address: " + url + "\n", true, url, true);
            return;
        }

        URI origURI = URI.create(url).normalize();
        String origURL = origURI.toString();
        if (origURI.getPath().isEmpty()) {

            origURL += "/"; // origURL keeps the emoji
            url += "/";
        }

        if (origURL.startsWith("file:/")) {
            handleFile(origURL, p, cPage);
            return;
        }

        // handle those hosts with emoji (shrimp and whatnot)
        String hostPart = url.split("://")[1].split("/")[0];
        for (char c : hostPart.toCharArray()) { // handle emoji
            if (c > 127) {
                String punycodeHost = IDN.toASCII(hostPart, IDN.ALLOW_UNASSIGNED);
                url = url.replace(hostPart, punycodeHost);
                break;
            }
        }
        URI punyURI = URI.create(url).normalize();

        if (httpProxy == null && !url.startsWith("file:/") && (url.startsWith("https://")
                || ((!url.startsWith("gemini://") && !url.startsWith("spartan://") && !url.startsWith("nex://")) && (prevURI != null && "https".equalsIgnoreCase(prevURI.getScheme()))))) {
            handleHttp(punyURI.toString(), prevURI, p, cPage);
            return;
        }

        if (punyURI.getScheme().equals("titan") && !punyURI.getPath().endsWith(";edit")) {
            String port = punyURI.getPort() != -1 ? ":" + punyURI.getPort() : "";
            String query = punyURI.getRawQuery() == null ? "" : "?" + punyURI.getRawQuery();
            if (!p.getTitanEdited() && p.getDataFile() == null) {
                File titanFile = null;
                String titanText = null;
                String token;
                TextEditor textEditor = new TextEditor("", true);
                Object[] comps = new Object[1];
                comps[0] = textEditor;
                Object res = Util.inputDialog2(p.frame(), "Edit", comps, null);
                if (res == null) {

                    return;
                } else {
                    Object rsp = textEditor.getResult();
                    p.setTitanEdited(true);
                    if (rsp instanceof String string) {
                        titanText = string;
                    } else {
                        titanFile = (File) rsp;
                    }
                    token = textEditor.getTokenParam();

                }

                if (titanFile != null) {
                    String mimeType = Util.getMimeType(titanFile.getAbsolutePath());
                    String titanUrl = "titan://" + punyURI.getHost() + port + punyURI.getPath() + token + ";size=" + titanFile.length() + ";mime=" + mimeType + query;
                    p.setDataFile(titanFile);

                    punyURI = URI.create(titanUrl);

                } else if (titanText != null && !titanText.isBlank()) {
                    String mimeType = "text/gemini";
                    String titanUrl = "titan://" + punyURI.getHost() + port + punyURI.getPath() + token + ";size=" + titanText.getBytes().length + ";mime=" + mimeType + query;

                    p.setEditedText(titanText);
                    punyURI = URI.create(titanUrl);

                } else {
                    String mimeType = "text/gemini"; // doesn't matter here? this is a zero length request which should be delete on server
                    String titanUrl = "titan://" + punyURI.getHost() + port + punyURI.getPath() + token + ";size=0;mime=" + mimeType + query;
                    p.setEditedText("");
                    punyURI = URI.create(titanUrl);

                }
            } else if (p.getTitanEdited()) {
                String token = p.getTitanToken();
                if (p.getEditedText() != null) {
                    String text = p.getEditedText();
                    String mimeType = "text/gemini";

                    String titanUrl = "titan://" + punyURI.getHost() + port + punyURI.getPath() + token + ";size=" + text.getBytes().length + ";mime=" + mimeType + query;

                    punyURI = URI.create(titanUrl);
                } else {
                    // ;edit but sending a file
                    String mimeType = Util.getMimeType(p.getDataFile().getAbsolutePath());

                    String titanUrl = "titan://" + punyURI.getHost() + port + punyURI.getPath() + token + ";size=" + p.getDataFile().length() + ";mime=" + mimeType + query;
                    punyURI = URI.create(titanUrl);
                }
            }
        }
        String proxyURL = null;
        if ((httpProxy != null && punyURI.getScheme().startsWith("http"))) {

            proxyURL = punyURI.toString();
            punyURI = URI.create("gemini://" + httpProxy);

        } else if (gopherProxy != null && punyURI.getScheme().equals("gopher")) {
            proxyURL = punyURI.toString();
            punyURI = URI.create("gemini://" + gopherProxy);
        }

        switch (punyURI.getScheme()) {
            case "gemini", "titan" ->
                gemini(getNetClient(punyURI), punyURI, p, origURL, cPage, proxyURL);
            case "spartan" ->
                spartan(punyURI, p, origURL, cPage);
            default ->
                nex(punyURI, p, origURL, cPage);
        }

    }

    private static boolean isAscii(Buffer buffer) {

        if (buffer.length() == 0) {
            return false;
        }
        for (int i = 0; i < buffer.length(); i++) {
            if ((buffer.getByte(i) & 0xFF) > 127) {
                return false;
            }
        }
        return true;
    }

    private static void spartan(URI uri, Page p, String origURL, Page cPage) {
        if (p.redirectCount == 0) {
            p.frame().setBusy(true, cPage);
        }
        p.setSpartan(true);
        if (spartanClient == null) {
            NetClientOptions options = new NetClientOptions()
                    .setConnectTimeout(60000)
                    .setSsl(false).setHostnameVerificationAlgorithm("");
            spartanClient = vertx.createNetClient(options);

        }
        String host = uri.getHost();

        int[] port = {uri.getPort()};

        if (port[0] == -1) {
            port[0] = 300;
        }
        spartanClient.connect(port[0], host, connection -> {
            if (connection.succeeded()) {

                System.out.println("connected: " + host);
                // wrap for the lambda
                boolean[] firstBuffer = {true};
                // if it turns out this is an image request we need to track where it starts
                int[] imageStartIdx = {-1};

                File uploadFile = p.getDataFile();
                String path = uri.getPath();
                if (uploadFile == null) {
                    String query = uri.getQuery();
                    int length = query == null ? 0 : query.length();

                    String urlText = uri.getHost() + " " + (path.isEmpty() ? "/" : path) + " " + length + "\r\n";
                    if (query != null) {
                        urlText += query;
                    }
                    connection.result().write(urlText);
                } else {
                    String urlText = uri.getHost() + " " + (path.isEmpty() ? "/" : path) + " " + uploadFile.length() + "\r\n";
                    connection.result().write(urlText);

                    String fp = p.getDataFile().getAbsolutePath();
                    p.setDataFile(null);
                    streamToSocket(fp, connection.result(), p, origURL);

                }

                Buffer saveBuffer = Buffer.buffer();

                boolean[] error = {false};
                int[] lineEnd = {0};
                Buffer[] charIncompleteBuffer = {Buffer.buffer()};
                // Handle the response
                connection.result().handler(buffer -> {

                    // we have to make sure we have the entire header before we proceed
                    if (firstBuffer[0]) {

                        saveBuffer.appendBuffer(buffer);
                        char respCode = (char) saveBuffer.getByte(0);

                        if (lineEnd[0] == 0) {
                            int i;
                            for (i = 0; i < saveBuffer.length(); i++) {
                                if (((char) saveBuffer.getByte(i)) == '\n') {
                                    //end of first line
                                    break;
                                }
                            }
                            lineEnd[0] = i;
                        }
                        // get enough of the response to tell if the payload is text
                        // this is for a popular spartan server that sends the wrong mime type for text files
                        // (probably based on their extension rather than content)
                        // Since it often only sends the first response line in the first buffer, we need to wait for more
                        if (lineEnd[0] == saveBuffer.length() - 1) {
                            return;
                        }
                        int i = lineEnd[0];

                        firstBuffer[0] = false;

                        switch (respCode) {

                            case '2' -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }

                                // have to assume there's at least one byte
                                String mime = saveBuffer.getString(2, i - 1);

                                if (mime.isBlank() || "application/octet-stream".equals(mime)) {
                                    String mimeFromExt = MimeMapping.getMimeTypeForFilename(uri.getPath());
                                    
                                    mime = mimeFromExt == null ? "text/gemini" : mimeFromExt;
                                }

                                if (mime.startsWith("text/gemini")) {

                                    BufferSplit split = splitBuffer(saveBuffer.slice(i + 1, saveBuffer.length()));
                                    bg(() -> {
                                        charIncompleteBuffer[0].appendBuffer(split.complete);

                                        p.textPane.updatePage(charIncompleteBuffer[0].toString(), false, origURL, true);
                                        charIncompleteBuffer[0] = Buffer.buffer(split.incomplete.getBytes());

                                    });
                                } else if (mime.startsWith("text/") || isAscii(saveBuffer.slice(i + 1, saveBuffer.length()))) {
                                    final String chunk = saveBuffer.getString(i + 1, saveBuffer.length(), "UTF-8");
                                    bg(() -> {
                                        p.textPane.updatePage(chunk, true, origURL, true);
                                    });
                                } else if (mime.startsWith("image/")) {
                                    imageStartIdx[0] = i + 1;
                                    try {
                                        DB.insertHistory(origURL, null);
                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }
                                } else if (allowVLC && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                                    try {
                                        connection.result().pause();
                                        connection.result().handler(null);
                                        try {
                                            DB.insertHistory(origURL, null);
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        }
                                        File af = File.createTempFile("alhena", "media");
                                        af.deleteOnExit();
                                        String finalMime = mime;
                                        Runnable r = () -> {
                                            p.frame().showGlassPane(false);
                                            GeminiTextPane tPane = cPage.textPane;
                                            if (tPane.awatingImage()) {
                                                tPane.insertMediaPlayer(af.getAbsolutePath(), finalMime);
                                            } else {
                                                p.textPane.end(" ", false, origURL, true);
                                                p.textPane.insertMediaPlayer(af.getAbsolutePath(), finalMime);
                                            }
                                        };
                                        streamToFile(connection.result(), af, saveBuffer.slice(i + 1, saveBuffer.length()), p, origURL, r);
                                        return;
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }

                                } else {
                                    File[] file = new File[1];
                                    connection.result().pause();
                                    connection.result().handler(null);

                                    if (p.getDataFile() == null) {

                                        try {

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
                                    } else {
                                        file[0] = p.getDataFile();
                                    }

                                    streamToFile(connection.result(), file[0], saveBuffer.slice(i + 1, saveBuffer.length()), p, origURL, null);

                                }
                            }

                            case '3' -> {
                                if (p.redirectCount++ == 6) {
                                    p.redirectCount = 0;
                                    connection.result().close();
                                    bg(() -> {
                                        p.textPane.end("## Too many redirects", false, origURL, true);

                                    });
                                    return;
                                }
                                String redirectPath = saveBuffer.getString(2, i - 1).trim();
                                p.redirectCount++;
                                String prt = port[0] == 300 ? "" : (":" + port[0]);
                                // can redirect return full path?
                                String redirect = "spartan://" + uri.getHost() + prt + redirectPath;
                                processURL(redirect, p, origURL, cPage);
                            }
                            case '4', '5' -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                String errorMsg = saveBuffer.getString(0, i - 1).trim();

                                bg(() -> {
                                    p.textPane.end("## Server Response: " + errorMsg, false, origURL, true);
                                });
                            }

                            default -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                connection.result().close();
                                bg(() -> {
                                    p.textPane.end("## Invalid response", false, origURL, true);

                                });
                                return;

                            }
                        }

                    } else {
                        //p.redirectCount = 0;
                        if (!error[0]) {
                            if (imageStartIdx[0] != -1) {
                                saveBuffer.appendBuffer(buffer);
                            } else {

                                bg(() -> {
                                    BufferSplit split = splitBuffer(buffer);
                                    charIncompleteBuffer[0].appendBuffer(split.complete);
                                    p.textPane.addPage(charIncompleteBuffer[0].toString());
                                    charIncompleteBuffer[0] = Buffer.buffer(split.incomplete.getBytes());

                                });
                            }

                        }
                    }

                });

                connection.result().closeHandler(v -> {
                    if (p.redirectCount > 0) {
                        p.redirectCount--;
                    }
                    System.out.println("connection closed");
                    if (imageStartIdx[0] != -1) {

                        bg(() -> {
                            // insert into existing page and not new page created for the call to processUrl
                            // get rid of this - maybe put spawning page in page ref

                            GeminiTextPane tPane = cPage.textPane;

                            if (tPane.awatingImage()) {
                                tPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));

                            } else {
                                p.textPane.end(" ", false, origURL, true);
                                p.textPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));
                            }
                        });
                    } else {
                        if (p.redirectCount == 0) {
                            bg(() -> {
                                p.textPane.end();

                            });
                        }
                    }

                });
            } else {
                p.redirectCount = 0;
                if (interrupted) {
                    interrupted = false;
                    p.frame().setBusy(false, cPage);
                } else {
                    bg(() -> {
                        p.textPane.end(new Date() + "\n" + connection.cause().toString() + "\n", true, origURL, true);
                    });
                    //connection.cause().printStackTrace();
                    System.out.println("Failed to connect: " + connection.cause().getMessage());
                }
            }
        });

    }

    private static void nex(URI uri, Page p, String origURL, Page cPage) {

        p.frame().setBusy(true, cPage);

        p.setNex(true);
        if (spartanClient == null) {
            NetClientOptions options = new NetClientOptions()
                    .setConnectTimeout(60000)
                    .setSsl(false).setHostnameVerificationAlgorithm("");
            spartanClient = vertx.createNetClient(options);

        }
        String host = uri.getHost();

        int[] port = {uri.getPort()};

        if (port[0] == -1) {
            port[0] = 1900;
        }
        spartanClient.connect(port[0], host, connection -> {
            if (connection.succeeded()) {

                System.out.println("connected: " + host);
                // wrap for the lambda
                boolean[] firstBuffer = {true};
                // if it turns out this is an image request we need to track where it starts
                int[] imageStartIdx = {-1};

                String path = uri.getPath();

                if (imageExtensions.stream().anyMatch(ext -> origURL.endsWith(ext))) {
                    imageStartIdx[0] = 0;
                }
                // boolean isText = txtExtensions.stream().anyMatch(ext -> ext.equalsIgnoreCase(extension[0]));
                boolean isText = txtExtensions.stream().anyMatch(ext -> origURL.endsWith(ext));

                connection.result().write(path.equals("/") ? "\n" : path + "\n");

                Buffer saveBuffer = Buffer.buffer();

                // Handle the response
                connection.result().handler(buffer -> {

                    if (imageStartIdx[0] != -1) {
                        try {
                            DB.insertHistory(origURL, null);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        saveBuffer.appendBuffer(buffer);
                    } else if (isText || path.lastIndexOf('.') == -1) {
                        if (firstBuffer[0]) {
                            firstBuffer[0] = false;
                            p.textPane.updatePage(buffer.toString(), true, origURL, true);
                        } else {
                            bg(() -> {
                                p.textPane.addPage(buffer.toString());
                            });
                        }
                    } else {
                        File[] file = new File[1];
                        connection.result().pause();
                        connection.result().handler(null);

                        if (p.getDataFile() == null) {

                            try {

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
                        }

                        streamToFile(connection.result(), file[0], buffer, p, origURL, null);
                    }

                });

                connection.result().closeHandler(v -> {

                    System.out.println("connection closed");
                    if (imageStartIdx[0] != -1) {

                        bg(() -> {
                            // insert into existing page and not new page created for the call to processUrl
                            // get rid of this - maybe put spawning page in page ref

                            GeminiTextPane tPane = cPage.textPane;

                            if (tPane.awatingImage()) {
                                tPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));

                            } else {
                                p.textPane.end(" ", false, origURL, true);
                                p.textPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));
                            }
                        });
                    } else {
                        if (p.redirectCount == 0) {
                            bg(() -> {
                                p.textPane.end();

                            });
                        }
                    }

                });
            } else {
                //p.redirectCount = 0;
                if (interrupted) {
                    interrupted = false;
                    p.frame().setBusy(false, cPage);
                } else {
                    bg(() -> {
                        p.textPane.end(new Date() + "\n" + connection.cause().toString() + "\n", true, origURL, true);
                    });
                    //connection.cause().printStackTrace();
                    System.out.println("Failed to connect: " + connection.cause().getMessage());
                }
            }
        });

    }

    private static void streamToSocket(String path, NetSocket socket, Page p, String origURL) {
        vertx.fileSystem().open(path, new OpenOptions().setRead(true), fileResult -> {
            if (fileResult.succeeded()) {
                AsyncFile asyncFile = fileResult.result();
                Pump pump = Pump.pump(asyncFile, socket);
                pump.start();

                // Once the file is fully read, wait for the server response
                asyncFile.endHandler(v -> {
                    asyncFile.close();
                });

                asyncFile.exceptionHandler(err -> {

                    err.printStackTrace();
                    asyncFile.close();
                    socket.close();
                    bg(() -> {
                        p.textPane.end("## Error sending file", false, origURL, true);
                    });

                });
            } else {
                fileResult.cause().printStackTrace();
                socket.close();
                bg(() -> {
                    p.textPane.end("## Error opening file", false, origURL, true);
                });
                //System.err.println("Failed to open file: " + fileResult.cause().getMessage());
            }
        });
    }

    private static void gemini(NetClient client, URI uri, Page p, String origURL, Page cPage, String proxyURL) {
        if (p.redirectCount == 0) {
            p.frame().setBusy(true, cPage);
        }

        String host = uri.getHost();

        // TODO: make sure there's no fragment - I think
        int[] port = {uri.getPort()};

        if (port[0] == -1) {
            port[0] = 1965;
        }
        client.connect(port[0], host, connection -> {
            if (connection.succeeded()) {
                SSLSession sslSession = connection.result().sslSession();
                p.setConnectInfo(sslSession.getProtocol(), sslSession.getCipherSuite());

                boolean[] titanEdit = {uri.getScheme().equals("titan") && uri.getPath().endsWith(";edit")};
                StringBuilder titanSB = new StringBuilder();
                boolean[] proceed = {true};
                X509Certificate[] cert = new X509Certificate[1];
                try {
                    List<Certificate> certList = connection.result().peerCertificates();
                    cert[0] = (X509Certificate) certList.get(0);
                    p.setCert(cert[0]);
                    CertTest res = verifyFingerPrint(host + ":" + port[0], cert[0]);

                    if (res.msg != null || res.host != null) {
                        try {
                            String resMsg = res.msg == null ? "" : res.msg + "\n";
                            String resHost = res.host == null ? "" : "The server certificate is from the wrong domain: " + res.cn + "\n";
                            // this blocks vertx event loop
                            EventQueue.invokeAndWait(() -> {
                                Object diagRes = Util.confirmDialog(p.frame(), "Certificate Issue", resMsg + resHost + "Do you want to continue?", JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
                                if (diagRes instanceof Integer result) {
                                    proceed[0] = result == JOptionPane.YES_OPTION;
                                } else {
                                    proceed[0] = false;
                                }

                            });
                        } catch (InterruptedException | InvocationTargetException ex) {
                            proceed[0] = false;
                        }
                        if (!proceed[0]) {

                            connection.result().close();
                            bg(() -> {
                                p.frame().showGlassPane(false);
                                p.frame().setBusy(false, cPage);
                                p.setBusy(false);
                            });

                            return;
                        } else {
                            cnConfirmedList.add(res.host);
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

                String urlText = proxyURL == null ? uri.toString() : proxyURL;
                connection.result().write(urlText + "\r\n");

                if (uri.getScheme().equals("titan") && !uri.getPath().endsWith(";edit")) {
                    p.setTitanToken(null);
                    if (p.getEditedText() != null) {
                        String txt = p.getEditedText();
                        p.setEditedText(null);
                        if (!txt.isEmpty()) {

                            connection.result().write(txt).onFailure(ex -> {
                                connection.result().close();
                                bg(() -> {
                                    p.textPane.end("Error sending text", false, origURL, true);
                                });
                                return;
                            });
                        }
                    } else if (p.getDataFile() != null) {
                        String fp = p.getDataFile().getAbsolutePath();
                        p.setDataFile(null);
                        streamToSocket(fp, connection.result(), p, origURL);

                    } else {
                        connection.result().close();
                        bg(() -> {
                            p.textPane.end("## Nothing to send", false, origURL, true);
                        });

                        return;
                    }
                }
                Buffer saveBuffer = Buffer.buffer();

                boolean[] error = {false};
                int[] hLength = {0};
                Buffer[] charIncompleteBuffer = {Buffer.buffer()};
                // Handle the response
                connection.result().handler(buffer -> {

                    // we have to make sure we have the entire header before we proceed
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
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                p.frame().setBusy(false, cPage);
                                String reqMsg = i == 3 ? "" : saveBuffer.getString(3, i - 1);
                                char respType = (char) saveBuffer.getByte(1);
                                bg(() -> {

                                    String input = Util.inputDialog(p.frame(), "Server Request", reqMsg, respType == '1');

                                    if (input != null) {
                                        String nUrl = uri.toString();
                                        int idx = nUrl.indexOf('?');
                                        if (idx != -1) {
                                            nUrl = nUrl.substring(0, idx);
                                        }

                                        p.setStart();
                                        processURL(nUrl + "?" + URLEncoder.encode(input).replace("+", "%20"), p, null, cPage);

                                    } else {
                                        cPage.textPane.resetLastClicked();
                                    }
                                });
                            }
                            case '2' -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                // have to assume there's at least one byte
                                String mime = saveBuffer.getString(3, i - 1);
                                if (mime.isBlank() || "application/octet-stream".equals(mime)) {
                                    String mimeFromExt = MimeMapping.getMimeTypeForFilename(uri.getPath());
                                    // once upon a time mime type was optional - no longer the case (officially)
                                    mime = mimeFromExt == null ? "text/gemini" : mimeFromExt;
                                }


                                if (mime.startsWith("text/gemini")) {
                                    if (titanEdit[0]) {
                                        titanSB.append(saveBuffer.getString(i + 1, saveBuffer.length(), "UTF-8"));
                                    } else {
                                        BufferSplit split = splitBuffer(saveBuffer.slice(i + 1, saveBuffer.length()));
                                        bg(() -> {

                                            charIncompleteBuffer[0].appendBuffer(split.complete);

                                            p.textPane.updatePage(charIncompleteBuffer[0].toString(), false, origURL, true);
                                            charIncompleteBuffer[0] = Buffer.buffer(split.incomplete.getBytes());

                                        });
                                    }
                                } else if (mime.startsWith("text/")) {
                                    final String chunk = saveBuffer.getString(i + 1, saveBuffer.length(), "UTF-8");
                                    if (titanEdit[0]) {
                                        titanSB.append(chunk);
                                    } else {
                                        bg(() -> {
                                            p.textPane.updatePage(chunk, true, origURL, true);
                                        });
                                    }
                                } else if (mime.startsWith("image/")) {
                                    imageStartIdx[0] = i + 1;
                                    hLength[0] = i + 1;
                                    try {
                                        DB.insertHistory(origURL, null);
                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }
                                } else if (allowVLC && (mime.startsWith("audio/") || mime.startsWith("video/"))) {
                                    try {
                                        connection.result().pause();
                                        connection.result().handler(null);
                                        try {
                                            DB.insertHistory(origURL, null);
                                        } catch (SQLException ex) {
                                            ex.printStackTrace();
                                        }
                                        File af = File.createTempFile("alhena", "media");
                                        af.deleteOnExit();
                                        String finalMime = mime;
                                        Runnable r = () -> {
                                            p.frame().showGlassPane(false);
                                            GeminiTextPane tPane = cPage.textPane;
                                            if (tPane.awatingImage()) {
                                                tPane.insertMediaPlayer(af.getAbsolutePath(), finalMime);
                                            } else {
                                                p.textPane.end(" ", false, origURL, true);
                                                p.textPane.insertMediaPlayer(af.getAbsolutePath(), finalMime);
                                            }
                                        };
                                        streamToFile(connection.result(), af, saveBuffer.slice(i + 1, saveBuffer.length()), p, origURL, r);
                                        return;
                                    } catch (IOException ex) {
                                        ex.printStackTrace();
                                    }
                                } else {
                                    File[] file = new File[1];
                                    connection.result().handler(null);
                                    connection.result().pause();
                                    if (p.getDataFile() == null) {

                                        try {

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
                                    } else {
                                        file[0] = p.getDataFile();
                                    }

                                    streamToFile(connection.result(), file[0], saveBuffer.slice(i + 1, saveBuffer.length()), p, origURL, null);
                                    return;
                                }
                            }

                            case '3' -> {
                                if (p.redirectCount++ == 6) {
                                    p.redirectCount = 0;
                                    connection.result().close();
                                    bg(() -> {
                                        p.textPane.end("## Too many redirects", false, origURL, true);

                                    });
                                    return;
                                }
                                p.setTitanEdited(false);
                                String redirectURI = saveBuffer.getString(3, i - 1).trim();
                                p.redirectCount++;
                                processURL(redirectURI, p, origURL, cPage);
                            }
                            case '4', '5' -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }

                                char respType = (char) saveBuffer.getByte(1);
                                if (!(titanEdit[0] && respCode == '5' && respType == '1')) {

                                    String errorMsg = saveBuffer.getString(0, i - 1).trim();

                                    titanEdit[0] = false;

                                    bg(() -> {
                                        p.textPane.end("## Server Response: " + errorMsg, false, origURL, true);
                                    });
                                }

                            }
                            case '6' -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                char respType = (char) saveBuffer.getByte(1);
                                if (respType == '0') { // 60 cert required
                                    //SSLSession sslSession = connection.result().sslSession();
                                    if ("TLSv1.2".equals(sslSession.getProtocol())) {
                                        try {
                                            EventQueue.invokeAndWait(() -> {

                                                Object res = Util.confirmDialog(p.frame(), "Warning", "A TLSv1.2 server is requesting a client certificate.\nDo you want to continue?", JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
                                                if (res instanceof Integer result) {
                                                    proceed[0] = result == JOptionPane.YES_OPTION;
                                                } else {
                                                    proceed[0] = false;
                                                }

                                            });
                                        } catch (InterruptedException | InvocationTargetException ex) {
                                            proceed[0] = false;

                                        }
                                    }
                                    if (!proceed[0]) {

                                        connection.result().close();
                                        bg(() -> {
                                            p.frame().showGlassPane(false);
                                            p.frame().setBusy(false, cPage);

                                        });

                                        return;
                                    }
                                    titanEdit[0] = false;
                                    p.frame().setBusy(false, cPage);
                                    String msg = saveBuffer.getString(3, i - 1).trim();
                                    certRequired(msg, uri, p, cert[0], cPage);
                                } else if (respType == '1' || respType == '2') {
                                    String errorMsg = saveBuffer.getString(0, i - 1).trim();

                                    bg(() -> {
                                        p.textPane.end("## Server Response: " + errorMsg, false, origURL, true);
                                    });
                                }
                            }
                            default -> {
                                if (p.redirectCount > 0) {
                                    p.redirectCount--;
                                }
                                connection.result().close();
                                bg(() -> {
                                    p.textPane.end("## Invalid response", false, origURL, true);

                                });
                                return;

                            }
                        }

                    } else {
                        //p.redirectCount = 0;
                        if (!error[0]) {
                            if (imageStartIdx[0] != -1) {

                                saveBuffer.appendBuffer(buffer);
                                cPage.frame().setTmpStatus((saveBuffer.length() - hLength[0]) + " bytes");
                            } else {
                                if (titanEdit[0]) {
                                    p.frame().setBusy(false, cPage);
                                    titanSB.append(buffer.toString());

                                } else {
                                    bg(() -> {
                                        BufferSplit split = splitBuffer(buffer);
                                        charIncompleteBuffer[0].appendBuffer(split.complete);
                                        p.textPane.addPage(charIncompleteBuffer[0].toString());
                                        charIncompleteBuffer[0] = Buffer.buffer(split.incomplete.getBytes());

                                    });
                                }
                            }

                        }
                    }

                });

                connection.result().closeHandler(v -> {
                    if (p.redirectCount > 0) {
                        p.redirectCount--;
                    }
                    System.out.println("connection closed");
                    if (imageStartIdx[0] != -1) {

                        bg(() -> {

                            GeminiTextPane tPane = cPage.textPane;

                            if (tPane.awatingImage()) {
                                tPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));

                            } else {
                                p.textPane.end(" ", false, origURL, true);
                                p.textPane.insertImage(saveBuffer.getBytes(imageStartIdx[0], saveBuffer.length()));
                            }
                        });
                    } else {
                        if (p.redirectCount == 0) {

                            bg(() -> {
                                if (titanEdit[0]) {

                                    TextEditor textEditor = new TextEditor(titanSB.toString(), true);
                                    Object[] comps = new Object[1];
                                    comps[0] = textEditor;
                                    Object res = Util.inputDialog2(p.frame(), "Edit", comps, null);

                                    if (res != null) {
                                        Object rsp = textEditor.getResult();
                                        p.setTitanEdited(true);
                                        String uriString = uri.toString();
                                        if (rsp instanceof String string) {
                                            if (!string.isBlank()) {
                                                p.setEditedText(string);
                                            } else {
                                                p.setEditedText("");
                                            }
                                        } else {
                                            p.setDataFile((File) rsp);
                                        }
                                        p.setTitanToken(textEditor.getTokenParam());
                                        processURL(uriString.substring(0, uriString.indexOf(";edit")), p, origURL, cPage);

                                    } else {
                                        p.frame().setBusy(false, cPage);
                                    }

                                } else {
                                    p.textPane.end();
                                }

                            });

                        }
                    }

                });
            } else {
                p.redirectCount = 0;
                if (interrupted) {
                    interrupted = false;
                    p.frame().setBusy(false, cPage);
                } else {
                    bg(() -> {
                        p.textPane.end(new Date() + "\n" + connection.cause().toString() + "\n", true, origURL, true);
                        String cause = causedByCertificateParsingException(connection.cause());
                        if (cause != null) {
                            Util.infoDialog(p.frame(), "Certificate Error", cause, JOptionPane.ERROR_MESSAGE);
                        }
                    });
                    //connection.cause().printStackTrace();
                    System.out.println("Failed to connect: " + connection.cause().getMessage());
                }
            }
        });
    }

    public static String causedByCertificateParsingException(Throwable throwable) {
        while (throwable != null) {
            if (throwable instanceof CertificateParsingException) {
                return throwable.getLocalizedMessage();
            }
            throwable = throwable.getCause();
        }
        return null;
    }

    private static record CertTest(String msg, String host, String cn) {

    }

    public static boolean matchesDomain(String host, String domain) {
        if (domain.startsWith("*.") && host.endsWith(domain.substring(1))) {
            return true; // Wildcard match
        }
        return host.equalsIgnoreCase(domain);
    }

    private static CertTest verifyFingerPrint(String host, X509Certificate cert) {
        String badHost = null;
        String cn = null;
        try {

            if (!cnConfirmedList.contains(host)) {

                cn = cert.getSubjectX500Principal().getName().replaceAll(".*CN=([^,]+).*", "$1");

                String h = host.substring(0, host.indexOf(':'));
                if (!matchesDomain(h, cn)) {

                    Collection<List<?>> sanList = cert.getSubjectAlternativeNames();
                    if (sanList != null) {

                        boolean matched = false;
                        for (List<?> san : sanList) {
                            if ((int) san.get(0) == 2) { // 2 = DNSName
                                String sanDomain = (String) san.get(1);

                                if (h.equalsIgnoreCase(sanDomain) || (sanDomain.startsWith("*.") && h.endsWith(sanDomain.substring(1)))) {
                                    matched = true;
                                    break;
                                }
                            }
                        }

                        if (!matched) {
                            badHost = host;
                        }

                    } else {
                        badHost = host;
                    }
                }
                if (badHost == null) {
                    cnConfirmedList.add(host);
                }

            }

            CertInfo certInfo = DB.getServerCert(host);
            try {
                cert.checkValidity();
            } catch (CertificateExpiredException ex) {

                if (certInfo.lastModified() != null) {
                    java.sql.Timestamp certExpires = new java.sql.Timestamp(cert.getNotAfter().getTime());
                    if (certInfo.lastModified().before(certExpires)) {
                        return new CertTest("Server certificate has expired.", badHost, cn);
                    }
                } else {
                    return new CertTest("Server certificate has expired.", badHost, cn);
                }
            } catch (CertificateNotYetValidException cex) {
                return new CertTest("Server certificate is not yet valid.", badHost, cn);
            }

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            byte[] certBytes = cert.getEncoded();
            byte[] hash = md.digest(certBytes);

            // Convert to hex
            StringBuilder hexSB = new StringBuilder();
            for (byte b : hash) {
                hexSB.append(String.format("%02X", b));
            }

            String fp = hexSB.substring(0, hexSB.length());

            String dbFingerprint = certInfo.fingerPrint();

            java.sql.Timestamp expires = certInfo.expires();

            if (dbFingerprint == null) {

                java.sql.Timestamp ts = new java.sql.Timestamp(cert.getNotAfter().getTime());
                DB.upsertCert(host, fp, ts, null);   // TOFU  
            } else if (!fp.equals(dbFingerprint)) {
                if (expires.after(new Date())) {
                    return new CertTest("Server certificate has changed without expiring.\n\n" + host + "\nSaved Expiration: "
                            + expires + "\nNew Expiration: " + new java.sql.Timestamp(cert.getNotAfter().getTime()) + "\n", badHost, cn);
                } else {
                    // update record
                    saveCert(host, cert);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            return new CertTest("Unable to validate certificate.", badHost, cn);
        }

        return new CertTest(null, badHost, cn);
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
            DB.upsertCert(host, fp, ts, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void streamToFile(NetSocket socket, File outFile, Buffer buffer, Page p, String url, Runnable runOnDone) {
        vertx.fileSystem().open(outFile.getAbsolutePath(), new OpenOptions().setWrite(true).setCreate(true).setTruncateExisting(true), fileRes -> {
            if (fileRes.succeeded()) {
                AsyncFile file = fileRes.result();

                file.write(buffer);

                boolean[] done = {false};
                Runnable r = () -> {
                    if (!done[0]) {
                        System.out.println("connection closed");
                        done[0] = true;

                        file.close();
                        bg(() -> {
                            if (runOnDone == null) {
                                try {
                                    DB.insertHistory(url, null);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                                p.frame().showGlassPane(false);
                                if (p.getDataFile() == null) {
                                    Util.infoDialog(p.frame(), "Success", outFile.getName() + " saved");
                                } else {
                                    // restore downloaded file
                                    Util.importData(p.frame(), outFile, true);
                                }
                            } else {
                                runOnDone.run();
                            }
                        });
                    }
                };

                socket.endHandler(v -> {
                    r.run();
                });
                // Close file when socket closes
                socket.closeHandler(v -> {

                    r.run();

                });
                // socket.handler(null);
                socket.resume();
                // Use Pump to stream to file
                Pump pump = Pump.pump(socket, file);
                pump.start();

            } else {
                bg(() -> {
                    p.textPane.end("# Failed to open file: " + outFile, false, url, true);
                });

                fileRes.cause().printStackTrace();

            }
        });
    }

    private static void bg(Runnable r) {
        EventQueue.invokeLater(() -> {
            r.run();
        });
    }

    public static void closeNetClient(ClientCertInfo cci) {
        NetClient nc = certMap.get(cci);
        if (nc != null) {
            nc.close();
            certMap.remove(cci);
        }
    }

    public static NetClient getNetClient(URI uri) {
        NetClient res = null;
        try {
            ClientCertInfo cci = DB.getClientCert(uri);
            if (cci == null) {
                if (!certMap.containsKey(null)) { // default connection

                    NetClientOptions options = new NetClientOptions()
                            .setConnectTimeout(60000)
                            .setSslHandshakeTimeout(30)
                            .setSsl(true) // Gemini uses TLS   
                            .setTrustAll(true)
                            .setHostnameVerificationAlgorithm("");
                    certMap.put(null, vertx.createNetClient(options));
                }
                res = certMap.get(null); // default shared NetClient for connections without client certs
            } else {
                if (!certMap.containsKey(cci)) {
                    NetClientOptions options = new NetClientOptions()
                            .setSsl(true) // gemini uses TLS
                            .setTrustAll(true) // gemini self-signed certs
                            .setSslHandshakeTimeout(30)
                            .setHostnameVerificationAlgorithm("")
                            .setSslEngineOptions(new JdkSSLEngineOptions() {
                                @Override
                                public SslContextFactory sslContextFactory() {
                                    return () -> {
                                        try {
                                            return new JdkSslContext(
                                                    createSSLContext(cci, uri),
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
                    certMap.put(cci, vertx.createNetClient(options));
                    //NetClient ccNetClient = vertx.createNetClient(options);
                }
                res = certMap.get(cci); // NetClient with cert
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return res;
    }

    private static void createClientCert(URI uri, String cn) {
        try {
            Security.addProvider(new BouncyCastleProvider());
            X509Certificate cert;
            PrivateKey privateKey;
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
            int port = uri.getPort() == -1 ? 1965 : uri.getPort();
            String url = uri.getHost() + ":" + port + uri.getPath();
            ClientCertInfo existingCert = DB.getClientCertInfo(url);
            if (existingCert != null) {
                DB.toggleCert(existingCert.id(), false, url, false);
            }
            //DB.toggleCert(ci.id(), false, prunedUrl, false);
            DB.insertClientCert(url, certPem, privateKeyPem, true, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static boolean certRequired(String msg, URI uri, Page p, X509Certificate cert, Page cPage) {

        String reqURL = uri.toString();
        String serverMsg = msg == null ? "" : "Server: '" + msg + "'\n";

        BooleanSupplier bs = () -> {
            JTextField cnField = new JTextField(PROG_NAME);

            cnField.addMouseListener(new ContextMenuMouseListener());
            JRadioButton dcButton = new JRadioButton("Domain Certificate");
            dcButton.setSelected(true);
            JRadioButton pcButton = new JRadioButton("Page Certificate");
            ButtonGroup bg = new ButtonGroup();
            bg.add(dcButton);
            bg.add(pcButton);

            Object[] comps = new Object[5];
            comps[0] = serverMsg + "Do you want to create a new client certificate for " + uri + "?\n\nCertificate Name:";
            comps[1] = cnField;
            comps[2] = new JLabel(" ");
            comps[3] = dcButton;
            comps[4] = pcButton;
            Object[] options = {"OK", "Cancel"};
            if (msg != null) {
                Object[] newOptions = {"OK", "Cancel", "Import PEM"};
                options = newOptions;
            }

            Object cn = Util.inputDialog2(p.frame(), "New Client Certificate", comps, options);

            if ("OK".equals(cn)) {
                String cnString = cnField.getText();
                cnString = cnString.isEmpty() ? PROG_NAME : cnString;
                addCertToTrustStore(uri, cert);

                if (dcButton.isSelected()) {
                    String port = uri.getPort() == -1 ? ":1965" : ":" + uri.getPort();
                    URI newURI = URI.create(uri.getScheme() + "://" + uri.getHost() + port + "/");

                    createClientCert(newURI, cnString);

                } else {
                    createClientCert(uri, cnString);

                }
                if (msg != null) { // from type 60
                    processURL(reqURL, p, null, cPage);

                    p.frame().setBusy(false, cPage);

                }
                return true;

            } else if ("Import PEM".equals(cn)) { // never happen when msg == null
                if (dcButton.isSelected()) {
                    String port = uri.getPort() == -1 ? ":1965" : ":" + uri.getPort();
                    URI newURI = URI.create(uri.getScheme() + "://" + uri.getHost() + port + "/");
                    p.frame().importPem(newURI, null);
                    //createClientCert(newURI, cnString);

                } else {
                    //createClientCert(uri, cnString);
                    p.frame().importPem(uri, null);

                }
                processURL(reqURL, p, null, cPage);
                // if opening page in a new tab, need to set busy false on original page
                if (p.getParent() instanceof JTabbedPane) {

                    cPage.setBusy(false);
                }
                //p.frame().importPem(uri, null);
            }
            return false;

        };

        if (EventQueue.isDispatchThread()) {
            return bs.getAsBoolean();

        } else {
            bg(() -> {
                bs.getAsBoolean();
            });
        }
        return false; // value doesn't matter when called from type 60 (sent to bg())
    }

    public static void addCertToTrustStore(URI uri, X509Certificate cert) {
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

                if (certExists(keyStore, calculateFingerprint(cert)) != null) {
                    return;
                }

                int port = uri.getPort() == -1 ? 1965 : uri.getPort();
                String newAlias = uri.getHost() + "." + port;

                // add the certificate to the keystore
                keyStore.setCertificateEntry(newAlias, cert);

                try (FileOutputStream os = new FileOutputStream(cacertsPath)) {
                    keyStore.store(os, cacertsPassword.toCharArray());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static String calculateFingerprint(X509Certificate cert) throws NoSuchAlgorithmException, CertificateEncodingException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(cert.getEncoded());
        StringBuilder sb = new StringBuilder();
        for (byte b : digest) {
            sb.append(String.format("%02X:", b));
        }
        return sb.substring(0, sb.length()); // Remove trailing colon
        //return sb.toString();

    }

    public static HashMap<String, X509Certificate> getServerCerts(List<String> hostList) {
        // add server certs for sites that require a client certificate
        HashMap<String, X509Certificate> cMap = new HashMap<>();
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

                for (String host : hostList) {
                    URI uri = URI.create("gemini://" + host);
                    host = uri.getHost();
                    int port = uri.getPort();
                    port = port == -1 ? 1965 : port;

                    X509Certificate cert = (X509Certificate) keyStore.getCertificate(host);
                    if (cert == null) {
                        // try with port 
                        cert = (X509Certificate) keyStore.getCertificate(host + "." + port);
                    }
                    if (cert != null /* && certExists(keyStore, calculateFingerprint(cert)) != null*/) {
                        // TODO: make sure host in correct format BELIEVE OKAY
                        cMap.put(host + ":" + port, cert);
                    }
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return cMap;
    }

    // used when restoring database
    public static void setServerCerts(HashMap<String, X509Certificate> certMap) {

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

                certMap.entrySet().stream().forEach(es -> {
                    try {

                        if (certExists(keyStore, calculateFingerprint(es.getValue())) == null) {
                            // add the certificate to the keystore
                            String alias = es.getKey().replace(':', '.');
                            keyStore.setCertificateEntry(alias, es.getValue());
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                });

                try (FileOutputStream os = new FileOutputStream(cacertsPath)) {
                    keyStore.store(os, cacertsPassword.toCharArray());
                }

            } catch (KeyStoreException | NoSuchAlgorithmException | CertificateException | IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String certExists(KeyStore keystore, String certFingerprint) throws Exception {
        for (String alias : java.util.Collections.list(keystore.aliases())) {
            if (keystore.isCertificateEntry(alias)) {
                Certificate cert = keystore.getCertificate(alias);
                if (cert instanceof X509Certificate x509Cert) {
                    String fingerprint = calculateFingerprint(x509Cert);
                    if (fingerprint.equalsIgnoreCase(certFingerprint)) {
                        ;
                        return alias;
                    }
                }
            }
        }
        return null;
    }

    private static SSLContext createSSLContext(ClientCertInfo certInfo, URI uri) throws Exception {
        // register Bouncy Castle as a security provider
        Security.addProvider(new BouncyCastleProvider());
        X509Certificate cert = (X509Certificate) loadCertificate(certInfo.cert());
        PrivateKey privateKey = loadPrivateKey(certInfo.privateKey());

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

    // only used by sync server upload - consolidate at some point
    public static void createKeyPair(String host, String cn) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        // generate a key pair
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        // create a self-signed certificate
        X509Certificate cert = generateSelfSignedCertificate(keyPair, cn);

        String privateKeyPem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(privateKey.getEncoded())
                + "\n-----END PRIVATE KEY-----";

        String certPem = "-----BEGIN CERTIFICATE-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(cert.getEncoded())
                + "\n-----END CERTIFICATE-----";

        DB.insertClientCert(host, certPem, privateKeyPem, true, null);
    }

    private static X509Certificate generateSelfSignedCertificate(KeyPair keyPair, String cn) throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        //X500Name subject = new X500Name("CN=Jeremy,O=UltimatumLabs,L=Elkhorn,C=US");
        X500Name subject = new X500Name("CN=" + cn);
        BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
        Date notBefore = new Date();
        Date notAfter = new Date(notBefore.getTime() + ((365L * 24 * 60 * 60 * 1000) * 10)); // 10 year validity

        // build the certificate
        JcaX509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                subject, serial, notBefore, notAfter, subject, keyPair.getPublic());

        // sign the certificate
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        X509CertificateHolder certHolder = certBuilder.build(signer);

        // convert to X509Certificate
        return new JcaX509CertificateConverter().setProvider("BC").getCertificate(certHolder);
    }

    public static PrivateKey loadPrivateKey(String pemString) throws Exception {
        try (PEMParser parser = new PEMParser(new StringReader(pemString))) {
            Object obj = parser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");

            if (obj instanceof PEMKeyPair keyPair) {
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo()); // PKCS#1
            } else if (obj instanceof PrivateKeyInfo privKeyInfo) {
                return converter.getPrivateKey(privKeyInfo); // PKCS#8
            } else {
                throw new IllegalArgumentException("Unsupported key format: " + (obj != null ? obj.getClass() : "null"));
            }
        }
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
        if (element.hasAttr("hidden")) {
            return "";
        }

        String tagName = element.tagName();

        return switch (tagName) {

            case "div" -> {
                StringBuilder gt = new StringBuilder();
                element.children().stream().forEach(child -> {
                    String line = processElement(child, host).trim();
                    if (!line.isBlank()) {

                        gt.append(line).append("\n\n");
                    }
                });
                yield gt.toString();
            }
            case "h1", "h2", "h3" -> {

                yield "#".repeat(tagName.charAt(1) - '0') + " " + element.text();
            }
            case "img" -> {
                String src = element.attr("src");
                String alt = element.attr("alt");
                yield "=> " + src + " " + alt;

            }
            case "p" -> {
                StringBuilder sb = new StringBuilder();

                boolean match = processBlock(sb, element, host, element.text());
                if (!match) {
                    sb.insert(0, element.text() + "\n");
                }
                yield sb.toString();

            }
            case "pre" -> {

                StringBuilder sb = new StringBuilder();
                sb.append("```\n");
                sb.append(element.text() + "\n");
                sb.append("```\n");
                yield sb.toString();

            }
            case "a" -> {
                String href = element.attr("href");
                String origHref = href;
                if (href.startsWith("#")) {
                    yield "";
                }
                if (href.startsWith("/") && host != null) {
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
            case "li" -> {
                if (element.ownText().isBlank()) {
                    StringBuilder sb = new StringBuilder();

                    element.children().stream().forEach(child -> {
                        sb.append(processElement(child, host)).append("\n");
                    });
                    yield sb.toString();
                } else {
                    yield "* " + element.text();
                }

            }

            default -> {
                if (element.ownText().isBlank()) {

                    StringBuilder sb = new StringBuilder();
                    if (tagName.equals("tr")) {
                        sb.append("-".repeat(100) + "\n");
                    }

                    element.children().stream().forEach(child -> {
                        String line = processElement(child, host);

                        if (!line.isBlank() && !child.hasAttr("hidden")) {
                            sb.append(line.trim()).append("\n\n");
                        }
                    });
                    yield sb.toString();
                } else {

                    yield element.text();

                }

            }

        };
    }

    private static boolean processBlock(StringBuilder sb, Element element, String host, String text) {
        boolean[] ret = {false};
        element.children().stream().forEach(child -> {
            String line = processElement(child, host);
            if (!line.isBlank()) { // weak sauce. avoid double-output of text if this element's contents match the parent's text (single <a> in <p>)
                if (line.endsWith(text)) {
                    ret[0] = true;
                }
                sb.append(line).append("\n");
            }
        });
        return ret[0];
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
        String message = "# Commands:\n\n* ansialert\n* scrollspeed\n* info\n* art\n\nType 'alhena:[command]' for details.\n";
        if (cmd.length == 1) {

            if (cmd[0].equals("ansialert")) {
                message = "# ansialert\n###Display alert when ANSI color codes detected\nalhena:ansialert=true";
            } else if (cmd[0].equals("scrollspeed")) {
                message = "# scrollspeed\n###Set the mouse wheel speed\nalhena:scrollspeed=10\nalhena:scrollspeed=default resets. Negative numbers reverse scroll direction.";

            } else if (cmd[0].equals("info")) {
                plainText = true;
                message = getAlhenaInfo().toString();
            } else if (cmd[0].equals("art")) {
                message = "```\n" + GeminiFrame.getArt() + "```\n";
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
                            gf.forEachPage(page -> {
                                page.setScrollIncrement(val);
                            });
                            //gf.setScrollIncrement(val);
                        }
                        message = "## " + cmd[0] + " set to " + cmd[1] + "\n";
                    }

                } catch (NumberFormatException ex) {
                    message = "## Value must be a number\n";
                }

            } else if (cmd[0].equals("ansialert")) {

                if (cmd[1].equals("true") || cmd[1].equals("false")) {
                    DB.insertPref("ansialert", cmd[1]);
                    GeminiFrame.ansiAlert = cmd[1].equals("true");
                    message = "## ansialert set to " + cmd[1] + "\n";
                } else {
                    message = "## Value must be 'true' or 'false'\n";
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

            gf.forEachPage(page -> {
                StringBuilder sbdoc = page.textPane.current().currentPage();
                if (sbdoc != null) {

                    sb.append(page.textPane.getDocURLString()).append(": ").append(sbdoc.length()).append(" bytes").append("\n");
                }
            });
        }

        return sb;
    }

    // call on EDT only
    public static void pauseMedia() {
        for (GeminiFrame gf : frameList) {
            gf.forEachPage(page -> {
                page.textPane.pausePlayers();
            });
        }
    }

    // only call from EDT
    private static void handleFile(String url, Page p, Page cPage) {
        URL fileUrl;
        try {

            fileUrl = new URL(url);

            File file = new File(fileUrl.toURI());
            if (file.exists()) {

                boolean matches = fileExtensions.stream().anyMatch(url.toLowerCase()::endsWith);
                if (matches) {

                    String fUrl = url;
                    p.frame().setBusy(true, cPage);
                    boolean pformatted = !(url.endsWith(".gmi") || url.endsWith(".gemini"));
                    if (!cPage.textPane.awatingImage()) {
                        p.textPane.updatePage("", pformatted, fUrl, true);
                    }
                    boolean isImage = imageExtensions.stream().anyMatch(url.toLowerCase()::endsWith);
                    boolean isMedia = isImage == false ? mediaExtensions.stream().anyMatch(url.toLowerCase()::endsWith) : false;

                    if (isMedia) {
                        String type = videoExtensions.stream().anyMatch(url.toLowerCase()::endsWith) ? "video" : "audio";
                        if (cPage.textPane.awatingImage()) {
                            cPage.textPane.insertMediaPlayer(file.getAbsolutePath(), type);
                        } else {

                            p.textPane.end(" ", false, fUrl, true);
                            p.textPane.insertMediaPlayer(file.getAbsolutePath(), type);
                        }

                    } else {

                        Buffer imageBuffer = Buffer.buffer();

                        vertx.fileSystem().open(file.getAbsolutePath(), new OpenOptions().setRead(true), result -> {
                            if (result.succeeded()) {
                                AsyncFile asyncFile = result.result();
                                Buffer[] charIncompleteBuffer = {Buffer.buffer()};
                                // read the file in chunks
                                asyncFile.handler(buffer -> {
                                    if (isImage) {
                                        imageBuffer.appendBuffer(buffer);
                                    } else {
                                        bg(() -> {
                                            BufferSplit split = splitBuffer(buffer);
                                            charIncompleteBuffer[0].appendBuffer(split.complete);
                                            p.textPane.addPage(charIncompleteBuffer[0].toString());
                                            charIncompleteBuffer[0] = Buffer.buffer(split.incomplete.getBytes());

                                        });
                                    }

                                });

                                // process the content when reading is done
                                asyncFile.endHandler(v -> {
                                    if (isImage) {
                                        bg(() -> {

                                            GeminiTextPane tPane = cPage.textPane;

                                            if (tPane.awatingImage()) {
                                                tPane.insertImage(imageBuffer.getBytes());

                                            } else {
                                                p.textPane.end(" ", false, fUrl, true);
                                                p.textPane.insertImage(imageBuffer.getBytes());
                                            }
                                        });

                                    } else {
                                        bg(() -> {
                                            p.textPane.end();
                                        });
                                    }

                                    asyncFile.close();

                                });

                                asyncFile.exceptionHandler(throwable -> {

                                    bg(() -> {
                                        p.textPane.end("## Error reading file\n", false, fUrl, true);
                                    });
                                });
                            } else {

                                bg(() -> {
                                    p.textPane.end("## Error opening file\n", false, fUrl, true);
                                });
                            }
                        });
                    }
                } else {
                    p.textPane.end("## Invalid file type\n", false, url, true);
                }
            } else {
                p.textPane.end("## File not found\n", false, url, true);
            }
        } catch (Exception ex) {
            p.textPane.end("## Error reading file\n" + ex.getMessage() + "\n", false, url, true);
            ex.printStackTrace();

        }
    }

    // only call on EDT
    private static void handleHttp(String url, URI prevURI, Page p, Page cPage) {
        String useB = DB.getPref("browser", null);
        boolean useBrowser = useB == null ? true : useB.equals("true");
        if (browsingSupported && useBrowser) {
            try {
                Desktop.getDesktop().browse(new URI(url));
            } catch (Exception ex) {

                ex.printStackTrace();
            }
            p.frame().updateComboBox(prevURI.toString());
            return;
        }

        if (httpClient == null) {
            HttpClientOptions options = new HttpClientOptions().
                    setSsl(true).
                    setTrustAll(true)
                    .setLogActivity(true);
            httpClient = vertx.createHttpClient(options);
        }
        p.frame().setBusy(true, cPage);
        String finalURL = url;

        URI finalURI = URI.create(finalURL);
        httpClient.request(HttpMethod.GET, 443, finalURI.getHost(), finalURI.getPath()).onComplete(ar -> {
            HttpClientRequest req = ar.result();
            req.send().onComplete(ar2 -> {
                if (ar2.succeeded()) {
                    HttpClientResponse resp = ar2.result();
                    String contentType = resp.getHeader("Content-Type");

                    if (contentType != null && contentType.startsWith("text/")) {
                        resp.body().onSuccess(buffer -> {
                            bg(() -> {
                                if (contentType.startsWith("text/html")) {
                                    p.textPane.end(convertHtmlToGemtext(buffer.toString(), finalURI.getScheme() + "://" + finalURI.getAuthority()), false, finalURL, true);
                                } else {
                                    p.textPane.end(buffer.toString(), false, finalURL, true);
                                }
                                cPage.setBusy(false);
                                //p.frame().showGlassPane(false);
                            });
                            req.end();
                        }).onFailure(ex -> {
                            ex.printStackTrace();
                            bg(() -> {
                                p.textPane.end("error getting web page\n", true, finalURL, true);
                            });
                            req.end();
                        });
                    } else if (contentType != null && contentType.startsWith("image/")) {
                        File file;
                        resp.pause();
                        try {
                            file = File.createTempFile("alhena", "media");
                            file.deleteOnExit();
                        } catch (IOException ex) {
                            ex.printStackTrace();
                            bg(() -> {
                                p.frame().showGlassPane(false);
                            });
                            return;
                        }
                        vertx.fileSystem().open(file.getAbsolutePath(), new OpenOptions().setCreate(true).setTruncateExisting(true), fileResult -> {
                            if (fileResult.succeeded()) {
                                AsyncFile af = fileResult.result();
                                resp.resume();
                                Pump pump = Pump.pump(resp, af);
                                pump.start();
                                resp.endHandler(eh -> {
                                    af.close();
                                    req.end();
                                    bg(() -> {

                                        GeminiTextPane tPane = cPage.textPane;

                                        try {
                                            byte[] data = Files.readAllBytes(file.toPath());
                                            file.delete();
                                            if (tPane.awatingImage()) {
                                                tPane.insertImage(data);

                                            } else {
                                                p.textPane.end(" ", false, finalURL, true);
                                                p.textPane.insertImage(data);
                                            }
                                        } catch (IOException ex) {
                                            ex.printStackTrace();
                                        }

                                        p.frame().showGlassPane(false);
                                    });
                                });
                            }
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
                        } catch (InterruptedException | InvocationTargetException ex) {
                            ex.printStackTrace();
                        }
                    }
                } else {
                    ar2.cause().printStackTrace();
                    bg(() -> {
                        p.textPane.end("broke\n", true, finalURL, true);
                    });
                }
            });
        });
    }

    public static void clearCnList() {
        // called when use complete replaces a db - saved server certs may be different
        cnConfirmedList.clear();
    }

    private static int findLastValidUTF8Position(Buffer buffer) {
        if (buffer == null || buffer.length() == 0) {
            return 0;
        }

        int length = buffer.length();

        // check from the end backwards to find any incomplete UTF-8 sequence
        for (int i = length - 1; i >= Math.max(0, length - 4); i--) {
            int b = buffer.getByte(i) & 0xFF;

            // check if this could be the start of a UTF-8 character
            if (isUTF8Start(b)) {
                int expectedLength = getUTF8CharLength(b);
                int remainingBytes = length - i;

                if (remainingBytes >= expectedLength) {
                    // complete character found - validate it
                    if (isValidUTF8Sequence(buffer, i, expectedLength)) {
                        return i + expectedLength;
                    }
                } else {
                    // incomplete character - return position before it
                    return i;
                }
            }
        }

        return length; // all chars complete
    }

    // check if a byte is the start of a UTF-8 character
    private static boolean isUTF8Start(int b) {
        return (b & 0x80) == 0
                || // 0xxxxxxx (1-byte)
                (b & 0xE0) == 0xC0
                || // 110xxxxx (2-byte start)
                (b & 0xF0) == 0xE0
                || // 1110xxxx (3-byte start)
                (b & 0xF8) == 0xF0;     // 11110xxx (4-byte start)
    }

    // get the expected length of a UTF-8 character from its first byte
    private static int getUTF8CharLength(int firstByte) {
        if ((firstByte & 0x80) == 0) {
            return 1;      // 0xxxxxxx

        }
        if ((firstByte & 0xE0) == 0xC0) {
            return 2;   // 110xxxxx

        }
        if ((firstByte & 0xF0) == 0xE0) {
            return 3;   // 1110xxxx

        }
        if ((firstByte & 0xF8) == 0xF0) {
            return 4;   // 11110xxx

        }
        return 1; // invalid, treat as single byte
    }

    // validates a UTF-8 sequence starting at the given position
    private static boolean isValidUTF8Sequence(Buffer buffer, int start, int length) {
        if (start + length > buffer.length()) {
            return false;
        }

        // check first byte
        int firstByte = buffer.getByte(start) & 0xFF;

        // check continuation bytes
        for (int i = 1; i < length; i++) {
            int b = buffer.getByte(start + i) & 0xFF;
            if ((b & 0xC0) != 0x80) { // Should be 10xxxxxx
                return false;
            }
        }

        // additional validation for overlong sequences and invalid ranges
        switch (length) {
            case 2 -> {
                int codePoint = ((firstByte & 0x1F) << 6) | (buffer.getByte(start + 1) & 0x3F);
                return codePoint >= 0x80;
            }
            case 3 -> {
                int codePoint = ((firstByte & 0x0F) << 12)
                        | ((buffer.getByte(start + 1) & 0x3F) << 6)
                        | (buffer.getByte(start + 2) & 0x3F);
                return codePoint >= 0x800 && (codePoint < 0xD800 || codePoint > 0xDFFF);
            }
            case 4 -> {
                int codePoint = ((firstByte & 0x07) << 18)
                        | ((buffer.getByte(start + 1) & 0x3F) << 12)
                        | ((buffer.getByte(start + 2) & 0x3F) << 6)
                        | (buffer.getByte(start + 3) & 0x3F);
                return codePoint >= 0x10000 && codePoint <= 0x10FFFF;
            }
            default -> {
            }
        }

        return true;
    }

    // splits a buffer into complete UTF-8 characters and incomplete bytes
    public static BufferSplit splitBuffer(Buffer buffer) {
        int validPosition = findLastValidUTF8Position(buffer);

        Buffer complete = buffer.slice(0, validPosition);
        Buffer incomplete = validPosition < buffer.length()
                ? buffer.slice(validPosition, buffer.length())
                : Buffer.buffer();

        return new BufferSplit(complete, incomplete);
    }

    public static class BufferSplit {

        public final Buffer complete;
        public final Buffer incomplete;

        public BufferSplit(Buffer complete, Buffer incomplete) {
            this.complete = complete;
            this.incomplete = incomplete;
        }
    }

}
