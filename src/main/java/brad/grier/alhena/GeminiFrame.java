package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.security.auth.x500.X500Principal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.util.SystemInfo;

import brad.grier.alhena.DB.Bookmark;
import brad.grier.alhena.DB.ClientCertInfo;
import brad.grier.alhena.DB.DBClientCertInfo;
import brad.grier.alhena.GeminiTextPane.CurrentPage;
import brad.grier.alhena.Util.PemData;

/**
 * Alhena frame
 *
 * @author Brad Grier
 */
public final class GeminiFrame extends JFrame {

    private JComboBox comboBox;
    private final JLabel statusField;
    public JTabbedPane tabbedPane;
    public final JButton backButton;
    public final JButton forwardButton;
    private final JButton favButton;
    public final JButton refreshButton;
    public static int currentThemeId;
    private final HashMap<Page, ArrayList<Page>> pageHistoryMap = new HashMap<>();
    private final List<String> clickedLinks = new ArrayList<>();
    private final JMenuBar menuBar;
    private JMenu bookmarkMenu, settingsMenu;
    private String lastSearch;
    private JLabel titleLabel;
    public static final String HISTORY_LABEL = "History";
    public static final String BOOKMARK_LABEL = "Bookmarks";
    public static final String CERT_LABEL = "Certs";
    public static final String INFO_LABEL = "Info";
    public static final String SERVERS_LABEL = "Servers";
    public static final List<String> CUSTOM_LABELS = List.of(HISTORY_LABEL, BOOKMARK_LABEL, CERT_LABEL, INFO_LABEL, SERVERS_LABEL); // make immutable
    public static String proportionalFamily = "SansSerif";
    public static int fontSize = 15;
    public static int monoFontSize = 15;
    public static boolean ansiAlert;
    private static Font saveFont;
    public final static String SYNC_SERVER = "ultimatumlabs.com:1965/";
    private int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private MenuItem mi;
    private Color saveButtonFG = null;

    private Map<String, ThemeInfo> themes = Map.ofEntries(
            Map.entry("FlatArcIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcIJTheme", "Arc", false)),
            Map.entry("FlatGruvboxDarkHardIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme", "Gruvbox Dark Hard", true)),
            Map.entry("FlatGradiantoNatureGreenIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme", "Gradianto Nature Green", true)),
            Map.entry("FlatDarkFlatIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme", "Dark Flat", true)),
            Map.entry("FlatMaterialOceanicIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme", "Material Oceanic", true)),
            Map.entry("FlatLightOwlIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme", "Light Owl", false)),
            Map.entry("FlatDarculaLaf", new ThemeInfo("com.formdev.flatlaf.FlatDarculaLaf", "Darcula", true)),
            Map.entry("FlatIntelliJLaf", new ThemeInfo("com.formdev.flatlaf.FlatIntelliJLaf", "IntelliJ", false)),
            Map.entry("FlatMacDarkLaf", new ThemeInfo("com.formdev.flatlaf.themes.FlatMacDarkLaf", "MacOS Dark", true)),
            Map.entry("FlatMacLightLaf", new ThemeInfo("com.formdev.flatlaf.themes.FlatMacLightLaf", "MacOS Light", false)),
            Map.entry("FlatMaterialPalenightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme", "Material Palenight", true)),
            Map.entry("FlatDarkPurpleIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme", "Dark Purple", true)),
            Map.entry("FlatMonokaiProIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme", "Monokai Pro", true)),
            Map.entry("FlatGradiantoMidnightBlueIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme", "Gradianto Midnight Blue", true)),
            Map.entry("FlatMoonlightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme", "Moonlight", true)),
            Map.entry("FlatMaterialLighterIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme", "Material Lighter", false)),
            Map.entry("FlatGitHubIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme", "GitHub", false)),
            Map.entry("FlatVuesionIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatVuesionIJTheme", "Vuesion", true)),
            Map.entry("FlatNordIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatNordIJTheme", "Nord", true)),
            Map.entry("FlatHiberbeeDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatHiberbeeDarkIJTheme", "Hiberbee Dark", true)),
            Map.entry("FlatCyanLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme", "Cyan Light", false)),
            Map.entry("FlatLightFlatIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme", "Light Flat", false)),
            Map.entry("FlatLightLaf", new ThemeInfo("com.formdev.flatlaf.FlatLightLaf", "Light", false)),
            Map.entry("FlatHighContrastIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme", "High Contrast", true)),
            Map.entry("FlatCarbonIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme", "Carbon", true)),
            Map.entry("FlatCobalt2IJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme", "Cobalt 2", true)),
            Map.entry("FlatGradiantoDarkFuchsiaIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme", "Gradianto Dark Fuchsia", true)),
            Map.entry("FlatGradiantoDeepOceanIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme", "Gradianto Deep Ocean", true)),
            Map.entry("FlatArcOrangeIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme", "Arc Orange", false)),
            Map.entry("FlatArcDarkOrangeIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme", "Arc Dark Orange", true)),
            Map.entry("FlatDarkLaf", new ThemeInfo("com.formdev.flatlaf.FlatDarkLaf", "Dark", true)),
            Map.entry("FlatSolarizedDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme", "Solarized Dark", true)),
            Map.entry("FlatDraculaIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme", "Dracula", true)),
            Map.entry("FlatXcodeDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme", "Xcode Dark", true)),
            Map.entry("FlatAtomOneLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme", "Atom One Light", false)),
            Map.entry("FlatSpacegrayIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme", "Spacegray", true)),
            Map.entry("FlatSolarizedLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", "Solarized Light", false))
    );

    public void forEachPage(Consumer<Page> c) {
        pageHistoryMap.entrySet().stream().forEach(entry -> {
            entry.getValue().forEach(page -> {
                c.accept(page);
            });

        });
    }

    public boolean isClickedLink(String link) {
        return clickedLinks.contains(link);
    }

    public void addClickedLink(String link) {
        clickedLinks.add(link);
    }

    private Page addPageToHistory(Page rootPage, Page page, boolean pageVisible) {

        if (rootPage == null) {

            page.setRootPage(Page.ROOT_PAGE);
            ArrayList<Page> histList = new ArrayList<>();
            histList.add(page);

            pageHistoryMap.put(page, histList);
        } else {

            page.setRootPage(rootPage);

            ArrayList<Page> histList = pageHistoryMap.get(rootPage);
            if (pageVisible) {
                int histIdx = rootPage.incAndGetArrayIndex(); // should get same result if calling on page.
                if (histIdx < histList.size()) {
                    List<Page> sl = histList.subList(histIdx, histList.size());
                    sl.clear();
                }
            }
            histList.add(page);
        }

        return page;
    }

    private Page newPage(String url, Page rootPage, boolean addToHistory) {
        Page pb;

        if (addToHistory) {
            if (rootPage == null) {

                pb = new Page(Page.ROOT_PAGE, this, url, currentThemeId);
                ArrayList<Page> histList = new ArrayList<>();
                histList.add(pb);

                pageHistoryMap.put(pb, histList);

            } else {

                pb = new Page(rootPage, this, url, currentThemeId);

                ArrayList<Page> histList = pageHistoryMap.get(rootPage);

                int histIdx = rootPage.incAndGetArrayIndex();
                if (histIdx < histList.size()) {
                    List<Page> sl = histList.subList(histIdx, histList.size());
                    sl.clear();
                }

                histList.add(pb);

            }
        } else {
            // doesn't have a root page (but may become one later if page load completes)
            pb = new Page(null, this, url, currentThemeId);
        }

        return pb;
    }

    public Page next(Page rootPage) {

        int histIdx = rootPage.getArrayIndex();
        ArrayList<Page> histList = pageHistoryMap.get(rootPage);
        if (histIdx + 1 < histList.size()) {
            histIdx++;
            rootPage.incAndGetArrayIndex();
            return histList.get(histIdx);

        }
        return null;
    }

    public boolean hasNext(Page rootPage) {
        if (rootPage == null) {
            return false;
        }
        int histIdx = rootPage.getArrayIndex();
        ArrayList<Page> histList = pageHistoryMap.get(rootPage);
        return histIdx + 1 < histList.size();
    }

    public Page prev(Page rootPage) {

        int histIdx = rootPage.getArrayIndex();
        ArrayList<Page> histList = pageHistoryMap.get(rootPage);
        if (histIdx > 0) {
            histIdx--;

            rootPage.decAndGetArrayIndex();
            return histList.get(histIdx);

        }
        return null;
    }

    public boolean hasPrev(Page rootPage) {
        return rootPage == null ? false : rootPage.getArrayIndex() > 0;

    }

    private Page getRootPage(Page pb) {
        // this only makes sense if you realize pb.getRootPage() can be null
        return pb.getRootPage() == Page.ROOT_PAGE ? pb : pb.getRootPage(); // returning null means this page has no history (new window/tab)
    }

    public GeminiFrame(String url, String baseUrl) {
        // Ubuntu seems to use the frame icon for the dock icon, using the 64x64 image improves the
        // resolution of the dock image at some cost to the frame image (bad downscaling)
        String pngName = SystemInfo.isWindows ? "alhena_32x32.png" : "alhena_64x64.png";
        URL iconUrl = Alhena.class.getClassLoader().getResource(pngName);
        Image ii = new ImageIcon(iconUrl).getImage();
        setIconImage(ii);

        if (SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            //getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        proportionalFamily = DB.getPref("fontfamily", "SansSerif");
        String fs = DB.getPref("fontsize", "15");
        fontSize = Integer.parseInt(fs);
        String mfs = DB.getPref("monofontsize", "15");
        monoFontSize = Integer.parseInt(mfs);
        
        String dbFont = DB.getPref("font", "SansSerif");
        saveFont = new Font(dbFont, Font.PLAIN, fontSize);

        // test to see if db font exists - maybe deleted from system or db moved to another os
        // fonts created with invalid names are created anyway with Dialog font family
        if (saveFont.getFamily().equals("Dialog") && !saveFont.getName().equals("Dialog")) {

            saveFont = new Font("SansSerif", Font.PLAIN, 15);
            proportionalFamily = "SansSerif";
            monoFontSize = fontSize = 15;
        }

        boolean addToHistory = url != null || baseUrl != null;
        Page pb = newPage(baseUrl, null, addToHistory);

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {

                Alhena.exit(GeminiFrame.this);

            }
        });

        comboBox = new JComboBox();

        comboBox.setEditable(true);

        comboBox.addItemListener(il -> {
            if (il.getStateChange() == ItemEvent.SELECTED) {

                String cbUrl = comboBox.getSelectedItem().toString();

                Object obj = comboBox.getSelectedItem();
                boolean processed = false;
                if (obj instanceof ComboItem ci) {
                    if (ci.supplier != null) {
                        showCustomPage(ci.url, ci.supplier.get());
                        processed = true;
                    }
                }
                if (!processed) {
                    if (CUSTOM_LABELS.contains(cbUrl) && !cbUrl.equals(INFO_LABEL)) {
                        showCustomPage(cbUrl, null);

                    } else {

                        fetchURL(cbUrl);
                    }
                }
            }
        });

        initComboBox();
        ImageIcon leftIcon = Util.recolorIcon("/left.png", UIManager.getColor("Button.foreground"), 21, 21);
        Font buttonFont = new Font("Noto Emoji Regular", Font.PLAIN, 18);
        backButton = new JButton(leftIcon);
        backButton.setToolTipText("Click to go back");
        ImageIcon rightIcon = Util.recolorIcon("/right.png", UIManager.getColor("Button.foreground"), 21, 21);
        forwardButton = new JButton(rightIcon);
        forwardButton.setToolTipText("Click to go forward");

        backButton.setEnabled(false);
        backButton.addActionListener(al -> {

            //showGlassPane(true);
            Page vPage = visiblePage();
            Page rootPage = getRootPage(vPage);
            if (hasPrev(rootPage)) {
                Page prev = prev(rootPage);
                String cUrl = prev.textPane.getDocURLString();
                updateComboBox(cUrl);
                if (tabbedPane == null) {
                    invalidate();
                    remove(vPage);
                    prev.setVisible(true);
                    add(prev, BorderLayout.CENTER);
                    revalidate();

                } else {
                    int idx = tabbedPane.getSelectedIndex();
                    tabbedPane.setComponentAt(idx, prev);

                }
                if (prev.getThemeId() != currentThemeId) {

                    SwingUtilities.updateComponentTreeUI(prev);
                    prev.setThemeId(currentThemeId);
                    refreshFromCache(prev);

                }
                setTitle(createTitle(cUrl, prev.textPane.getFirstHeading()));
                backButton.setEnabled(hasPrev(rootPage));
                forwardButton.setEnabled(hasNext(rootPage));

            }

            validate();
            repaint();

        });

        //forwardButton.setFont(buttonFont);
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(al -> {
            //showGlassPane(true);
            Page vPage = visiblePage();
            Page groupPane = getRootPage(vPage);
            if (hasNext(groupPane)) {
                Page next = next(groupPane);
                String cUrl = next.textPane.getDocURLString();
                updateComboBox(cUrl);
                if (tabbedPane == null) {
                    invalidate();

                    remove(vPage);
                    next.setVisible(true);
                    add(next, BorderLayout.CENTER);

                    revalidate();
                } else {
                    int idx = tabbedPane.getSelectedIndex();
                    tabbedPane.setComponentAt(idx, next);
                }

                if (next.getThemeId() != currentThemeId) {
                    SwingUtilities.updateComponentTreeUI(next);
                    next.setThemeId(currentThemeId);
                    refreshFromCache(next);

                }
                setTitle(createTitle(cUrl, next.textPane.getFirstHeading()));
                backButton.setEnabled(hasPrev(groupPane));
                forwardButton.setEnabled(hasNext(groupPane));
            }

            validate();
            repaint();

        });

        ImageIcon refreshIcon = Util.recolorIcon("/refresh.png", UIManager.getColor("Button.foreground"), 21, 21);
        refreshButton = new JButton(refreshIcon);
        refreshButton.setToolTipText("Reload this page");
        refreshButton.setEnabled(false);

        Action refreshAction = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refresh();
            }
        };
        refreshButton.addActionListener(refreshAction);
        refreshButton.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_R, mod), "refresh");
        refreshButton.getActionMap().put("refresh", refreshAction);

        JButton homeButton = new JButton("🏠");
        homeButton.setToolTipText("Go to home page");
        homeButton.addActionListener(al -> {

            String homePage = Util.getHome();
            if (GeminiFrame.CUSTOM_LABELS.contains(homePage)) {
                showCustomPage(homePage, null);
            } else {
                fetchURL(homePage);
            }
        });
        homeButton.setFont(buttonFont);

        favButton = new JButton("🔖");
        favButton.setToolTipText("Bookmark this page");
        favButton.addActionListener(al -> {
            bookmarkPage();

        });

        favButton.setFont(buttonFont);

        JPanel navPanel = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new java.awt.Insets(0, 5, 5, 0);
        navPanel.add(backButton, c);
        navPanel.add(forwardButton, c);
        navPanel.add(refreshButton, c);

        GridBagConstraints gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 5, 5, 0);

        navPanel.add(comboBox, gridBagConstraints);

        navPanel.add(homeButton, c);

        GridBagConstraints c1 = new java.awt.GridBagConstraints();
        c1.insets = new java.awt.Insets(0, 5, 5, 5);
        navPanel.add(favButton, c1);
        if (SystemInfo.isMacOS) {
            JPanel macPanel = new JPanel(new GridLayout(2, 1));

            JPanel centerPanel = new JPanel(new FlowLayout());
            titleLabel = new JLabel();
            centerPanel.add(titleLabel);

            macPanel.add(centerPanel);

            macPanel.add(navPanel);
            add(macPanel, BorderLayout.NORTH);

        } else {
            add(navPanel, BorderLayout.NORTH);
        }

        add(pb, BorderLayout.CENTER);
        statusField = new JLabel(" ");
        setTmpStatus(Alhena.WELCOME_MESSAGE);
        statusField.setBorder(new EmptyBorder(5, 5, 5, 5)); // Add padding

        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");

        fileMenu.add(createMenuItem("Open File", KeyStroke.getKeyStroke(KeyEvent.VK_O, mod), () -> {
            openFile(null);
        }));
        fileMenu.add(new JSeparator());

        fileMenu.add(createMenuItem("New Tab", KeyStroke.getKeyStroke(KeyEvent.VK_T, mod), () -> {
            newTab("alhena:art");

        }));

        fileMenu.add(createMenuItem("New Window", KeyStroke.getKeyStroke(KeyEvent.VK_N, mod), () -> {
            String home = Util.getHome();
            Alhena.newWindow(home, home);
        }));

        fileMenu.add(new JSeparator());
        JMenu userMenu = new JMenu("User Data");

        userMenu.add(createMenuItem("Export Data", null, () -> {

            FileNameExtensionFilter filter = new FileNameExtensionFilter("zip files (*.zip)", "zip");
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            String formattedDate = now.format(formatter);
            File f = Util.getFile(GeminiFrame.this, "alhenadb_" + formattedDate + ".zip", false, "Save", filter);
            if (f != null) {

                try {

                    DB.dumpDB(f);
                    Util.infoDialog(GeminiFrame.this, "Complete", "Data successfully exported.");

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }

        }));

        userMenu.add(createMenuItem("Import Data", null, () -> {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("zip files (*.zip)", "zip");
            File f = Util.getFile(GeminiFrame.this, null, true, "Open", filter);
            if (f != null) {
                Util.importData(GeminiFrame.this, f, false);
            }
        }));

        userMenu.add(new JSeparator());

        userMenu.add(createMenuItem("Sync Upload", null, () -> {
            try {
                ClientCertInfo ci = DB.getClientCertInfo(SYNC_SERVER);
                if (ci == null) {
                    Object r = Util.confirmDialog(GeminiFrame.this, "Missing Cert", "You do not have an active certificate for " + SYNC_SERVER + "\nDo you want to create one?", JOptionPane.YES_NO_OPTION, null);
                    if (r instanceof Integer result) {
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                Alhena.createKeyPair(SYNC_SERVER, "sync");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            Util.infoDialog(GeminiFrame.this, "Canceled", "Sync upload canceled.");
                            return;
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            Object r = Util.confirmDialog(GeminiFrame.this, "Sync", "This will overwrite any previously saved information on the server.\nAre you sure you want to upload your encrypted data?", JOptionPane.YES_NO_OPTION, null);
            if (r instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    ClientCertInfo certInfo;
                    try {

                        certInfo = DB.getClientCertInfo(SYNC_SERVER);
                        PrivateKey pk = Alhena.loadPrivateKey(certInfo.privateKey());
                        X509Certificate cert = (X509Certificate) Alhena.loadCertificate(certInfo.cert());
                        File file = File.createTempFile("alhenadb", ".zip");
                        DB.dumpDB(file);
                        File encFile = File.createTempFile("alhenadb", ".enc");

                        Util.encryptFile(file.getAbsolutePath(), encFile.getAbsolutePath(), cert);
                        String hash = Util.hashAndSign(encFile, pk);
                        file.deleteOnExit();
                        encFile.deleteOnExit();

                        String titanUrl = "titan://" + SYNC_SERVER + "/sync;token=alhenasync;mime=application/octet-stream;size=" + encFile.length() + "?hash=" + hash;
                        fetchURL(titanUrl, encFile);

                    } catch (Exception ex) {
                        Util.infoDialog(GeminiFrame.this, "Error", "Sync failed.\n" + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }

        }));

        userMenu.add(createMenuItem("Sync Download", null, () -> {
            try {
                File file = File.createTempFile("alhenadb", ".enc");

                fetchURL("gemini://" + SYNC_SERVER + "/sync/", file);
                file.deleteOnExit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }));

        fileMenu.add(userMenu);

        if (!SystemInfo.isMacOS) {
            fileMenu.add(new JSeparator());
            fileMenu.add(createMenuItem("Quit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, mod), () -> {
                Alhena.exit(GeminiFrame.this);
            }));
        }
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");
        viewMenu.setMnemonic('V');
        
        for (String label : CUSTOM_LABELS) {
            KeyStroke ks = null;
            if (!label.equals(INFO_LABEL)) {
                switch (label) {
                    case HISTORY_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod);
                    case BOOKMARK_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, mod);
                    case CERT_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, (mod | KeyEvent.ALT_DOWN_MASK));
                    case SERVERS_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_S, (mod | KeyEvent.ALT_DOWN_MASK));
                    default -> {
                    }
                }

                viewMenu.add(createMenuItem(label, ks, () -> {

                    showCustomPage(label, null);

                }));
            }

        }

        viewMenu.add(new JSeparator());

        viewMenu.add(createMenuItem("Find", KeyStroke.getKeyStroke(KeyEvent.VK_F, mod), () -> {
            String input = Util.inputDialog(this, "Find In Page", "Enter search term", false, lastSearch, null);
            if (input != null) {
                lastSearch = input;
                if (!visiblePage().textPane.find(input)) {
                    lastSearch = null;
                }
            }
        }));

        menuBar.add(viewMenu);

        addBookmarks();

        addWindowsMenu();

        JMenu aboutMenu = new JMenu("Help");
        aboutMenu.setMnemonic('H');
        if (!SystemInfo.isMacOS) {
            aboutMenu.add(createMenuItem("About", null, () -> {
                Util.showAbout(GeminiFrame.this);

            }));
        }
        aboutMenu.add(createMenuItem("Home", null, () -> {

            String homeDir = System.getProperty("alhena.home");
            File file = Util.copyFromJar(homeDir);
            URI fileUri = file.toURI();

            fetchURL(fileUri.toString());

        }));

        aboutMenu.add(createMenuItem("Changes", null, () -> {
            fetchURL("gemini://ultimatumlabs.com/alhena_changes.gmi");
        }));

        aboutMenu.add(createMenuItem("FAQ", null, () -> {
            fetchURL("gemini://ultimatumlabs.com/alhenafaq.gmi");
        }));

        aboutMenu.add(createMenuItem("Details", null, () -> {
            fetchURL("alhena:info");
        }));

        menuBar.add(aboutMenu);
        setJMenuBar(menuBar);
        add(statusField, BorderLayout.SOUTH);
        setSize(1024, 600);
        setLocationRelativeTo(null);
        setVisible(true);
        if (url != null) {
            init(url, pb);
        }

    }

    private void addWindowsMenu() {

        if (settingsMenu != null) {
            settingsMenu.removeAll();
        } else {
            settingsMenu = new JMenu("Settings");
            settingsMenu.setMnemonic('S');
            menuBar.add(settingsMenu);
        }

        JMenu darkThemeMenu = new JMenu("Dark Themes");

        JMenu lightThemeMenu = new JMenu("Light Themes");
        ButtonGroup themeGroup = new ButtonGroup();
        String theme = UIManager.getLookAndFeel().getClass().toString();
        theme = theme.substring(theme.lastIndexOf('.') + 1);
        String finalTheme = theme;
        themes.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey();
                    ThemeInfo value = entry.getValue();

                    JMenu jm = value.isDark() ? darkThemeMenu : lightThemeMenu;

                    JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(value.label, key.equals(finalTheme));
                    themeGroup.add(themeItem);
                    jm.add(themeItem);
                    themeItem.addActionListener(al -> {
                        //if (!key.equals(currentThemeId))
                        try {

                            DB.insertPref("theme", value.className());

                            Alhena.updateFrames(false, false);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                });
        settingsMenu.add(lightThemeMenu);
        settingsMenu.add(darkThemeMenu);

        settingsMenu.add(createMenuItem("Font", KeyStroke.getKeyStroke(KeyEvent.VK_F, (mod | KeyEvent.ALT_DOWN_MASK)), () -> {
            Font defFont = saveFont != null ? saveFont : new Font("SansSerif", Font.PLAIN, 15);
            Font font = Util.getFont(GeminiFrame.this, defFont);
            if (font != null) {
                saveFont = font;

                proportionalFamily = font.getFamily();
                fontSize = font.getSize();
                Alhena.updateFrames(false, false);

                DB.insertPref("font", font.getName());
                DB.insertPref("fontfamily", proportionalFamily);
                DB.insertPref("fontsize", String.valueOf(fontSize));
                DB.insertPref("monofontsize", String.valueOf(monoFontSize));
            }

        }));
        String emojiPref = DB.getPref("emoji", null);
        emojiPref = emojiPref == null ? "google" : emojiPref;
        boolean macUseNoto = false;
        if (SystemInfo.isMacOS && emojiPref.equals("font")) {
            macUseNoto = DB.getPref("macusenoto", "false").equals("true");
        }

        JMenu emojiMenu = new JMenu("Emoji");
        JRadioButtonMenuItem appleItem = new JRadioButtonMenuItem("Apple", emojiPref.equals("apple"));
        if (appleItem.isSelected()) {
            lastSelectedItem = appleItem;
        }
        appleItem.addActionListener(al -> setEmoji("apple", al, false));
        emojiMenu.add(appleItem);
        JRadioButtonMenuItem fbItem = new JRadioButtonMenuItem("Facebook", emojiPref.equals("facebook"));
        if (fbItem.isSelected()) {
            lastSelectedItem = fbItem;
        }
        fbItem.addActionListener(al -> setEmoji("facebook", al, false));
        emojiMenu.add(fbItem);
        JRadioButtonMenuItem googleItem = new JRadioButtonMenuItem("Google", emojiPref.equals("google"));
        if (googleItem.isSelected()) {
            lastSelectedItem = googleItem;
        }
        googleItem.addActionListener(al -> setEmoji("google", al, false));
        emojiMenu.add(googleItem);
        JRadioButtonMenuItem twitterItem = new JRadioButtonMenuItem("Twitter", emojiPref.equals("twitter"));
        if (twitterItem.isSelected()) {
            lastSelectedItem = twitterItem;
        }
        twitterItem.addActionListener(al -> setEmoji("twitter", al, false));
        emojiMenu.add(twitterItem);
        JRadioButtonMenuItem fontItem = new JRadioButtonMenuItem("Font", emojiPref.equals("font") && !macUseNoto);
        if (fontItem.isSelected()) {
            lastSelectedItem = fontItem;
        }
        fontItem.addActionListener(al -> setEmoji("font", al, false));
        emojiMenu.add(fontItem);

        JRadioButtonMenuItem notoItem = null;
        if (SystemInfo.isMacOS) {
            notoItem = new JRadioButtonMenuItem("Noto", emojiPref.equals("font") && macUseNoto);
            if (notoItem.isSelected()) {
                lastSelectedItem = notoItem;
            }
            notoItem.addActionListener(al -> setEmoji("font", al, true));
            emojiMenu.add(notoItem);
        }

        ButtonGroup group = new ButtonGroup();
        group.add(appleItem);
        group.add(fbItem);
        group.add(googleItem);
        group.add(twitterItem);
        group.add(fontItem);
        if (notoItem != null) {
            group.add(notoItem);
        }

        settingsMenu.add(emojiMenu);
        settingsMenu.add(new JSeparator());
        JCheckBoxMenuItem smoothItem = new JCheckBoxMenuItem("Adaptive Scrolling", DB.getPref("smoothscrolling", "true").equals("true"));
        smoothItem.addItemListener(ae -> {

            boolean smoothScrolling = !DB.getPref("smoothscrolling", "true").equals("true"); // toggle
            forEachPage(page -> {
                if (smoothScrolling) {
                    page.textPane.setupAdaptiveScrolling();
                } else {
                    page.textPane.removeAdaptiveScrolling();
                }

            });
            DB.insertPref("smoothscrolling", String.valueOf(smoothScrolling));
            String txt = smoothScrolling ? "on." : "off.";
            Util.infoDialog(GeminiFrame.this, "Update", "Mouse wheel adaptive scrolling turned " + txt);

        });
        settingsMenu.add(smoothItem);
        settingsMenu.add(new JSeparator());
        JMenuItem proxyItem = new JMenuItem("HTTP Proxy");
        proxyItem.addActionListener(ae -> {

            String proxy = Util.inputDialog(GeminiFrame.this, "HTTP Proxy", "Enter \"host:port\" for HTTP proxy.\nClear to use default HTTP link handling.",
                    false, Alhena.httpProxy == null ? "" : Alhena.httpProxy, null);
            if (proxy != null) {
                if (proxy.isBlank()) {
                    Alhena.httpProxy = null;
                } else {
                    Alhena.httpProxy = proxy;
                }
                DB.insertPref("httpproxy", Alhena.httpProxy);
            }
        });
        settingsMenu.add(proxyItem);

        JMenuItem gopherItem = new JMenuItem("Gopher Proxy");
        gopherItem.addActionListener(ae -> {

            String proxy = Util.inputDialog(GeminiFrame.this, "Gopher Proxy", "Enter \"host:port\" for Gopher proxy.\nClear to use default Gopher link handling.",
                    false, Alhena.gopherProxy == null ? "" : Alhena.gopherProxy, null);
            if (proxy != null) {
                if (proxy.isBlank()) {
                    Alhena.gopherProxy = null;
                } else {
                    Alhena.gopherProxy = proxy;
                }
                DB.insertPref("gopherproxy", Alhena.gopherProxy);
            }
        });
        settingsMenu.add(gopherItem);

    }

    private static JRadioButtonMenuItem lastSelectedItem;

    public void editPage(){
        Page vp = visiblePage();
        URI uri = vp.textPane.getURI();
        if(uri.getScheme() != null && uri.getScheme().equals("gemini")){
            String port = uri.getPort() != -1 ? ":" + uri.getPort() : "";
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            String editUrl = "titan://" + uri.getHost() + port + uri.getPath() + ";edit" + query;
            fetchURL(editUrl);
        }
    }

    public void updateBookmarks() {
        bookmarkMenu.invalidate();
        addBookmarks();
        bookmarkMenu.validate();
    }

    public void updateWindowsMenu() {
        settingsMenu.invalidate();
        addWindowsMenu();
        settingsMenu.validate();
    }

    // change font after restore
    public void resetFont() {
        proportionalFamily = DB.getPref("fontfamily", "SansSerif");
        String fs = DB.getPref("fontsize", "15");
        fontSize = Integer.parseInt(fs);
        String mfs = DB.getPref("monofontsize", "15");
        monoFontSize = Integer.parseInt(mfs);
        String dbFont = DB.getPref("font", "SansSerif");
        saveFont = new Font(dbFont, Font.PLAIN, fontSize);
    }

    public static Map<String, String> emojiNameMap = Map.of("apple", "https://github.com/mochaman/alhena/releases/download/v3.4/sheet_apple_64.png",
            "facebook", "https://github.com/mochaman/alhena/releases/download/v3.4/sheet_facebook_64.png",
            "twitter", "https://github.com/mochaman/alhena/releases/download/v3.4/sheet_twitter_64.png",
            "google", "/sheet_google_64.png");

    // called on DB replace - will not be called for 'google' or 'font'
    // never called for sheets not installed
    public void setEmoji(String setName) {
        try {
            String url = emojiNameMap.get(setName);
            String fn = url.substring(url.lastIndexOf('/'));
            File emojiFile = new File(System.getProperty("alhena.home") + File.separatorChar + fn);
            BufferedImage sheetImage = ImageIO.read(emojiFile);
            GeminiTextPane.setSheetImage(sheetImage);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void setEmoji(String setName, ActionEvent ae, boolean macUseNoto) {
        JRadioButtonMenuItem selected = (JRadioButtonMenuItem) ae.getSource();
        String savedSet = DB.getPref("emoji", null);
        // why not a "noto" value for "emoji" pref? Backwards compatibility with previous versions (also other operating systems on restore)
        boolean savedMacNotoPref = DB.getPref("macusenoto", "false").equals("true");
        if (!setName.equals(savedSet) || macUseNoto != savedMacNotoPref) {
            if (setName.equals("font")) {

                // use font
                GeminiTextPane.setSheetImage(null);
                DB.insertPref("emoji", setName);
                DB.insertPref("macusenoto", String.valueOf(macUseNoto));

                Alhena.updateFrames(false, false);
                lastSelectedItem = selected;
            } else {
                String url = emojiNameMap.get(setName);

                if (setName.equals("google")) {
                    GeminiTextPane.setSheetImage(Util.loadImage(url));
                    DB.insertPref("emoji", "google");
                    Alhena.updateFrames(false, false);
                    lastSelectedItem = selected;
                } else {
                    String fn = url.substring(url.lastIndexOf('/'));
                    File emojiFile = new File(System.getProperty("alhena.home") + File.separatorChar + fn);
                    if (!emojiFile.exists()) {

                        downloadSpriteSheet(url, emojiFile, setName, selected);
                    } else {
                        try {
                            GeminiTextPane.setSheetImage(ImageIO.read(emojiFile));
                            DB.insertPref("emoji", setName);
                            Alhena.updateFrames(false, false);
                            lastSelectedItem = selected;
                        } catch (IOException ex) {
                            lastSelectedItem.setSelected(true);
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    public void downloadSpriteSheet(String u, File outFile, String setName, JRadioButtonMenuItem selected) {
        visiblePage().setBusy(true);

        new Thread(() -> {

            boolean success = Util.downloadFile(u, outFile);
            if (success) {
                try {
                    BufferedImage setImage = ImageIO.read(outFile);
                    EventQueue.invokeLater(() -> {
                        visiblePage().setBusy(false);

                        DB.insertPref("emoji", setName);

                        GeminiTextPane.setSheetImage(setImage);
                        Alhena.updateFrames(false, false);
                        lastSelectedItem = selected;

                    });
                } catch (IOException ex) {
                    lastSelectedItem.setSelected(true);
                    ex.printStackTrace();
                }
            } else {
                lastSelectedItem.setSelected(true);
                EventQueue.invokeLater(() -> {
                    visiblePage().setBusy(false);
                    Util.infoDialog(GeminiFrame.this, "Error", "Error downloading emoji file.", JOptionPane.ERROR_MESSAGE);
                });

            }

        }).start();
    }

    public void initComboBox() {
        JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();

        textField.addMouseListener(new ContextMenuMouseListener());
    }

    public void recolorIcons() {
        Color buttonForeground = UIManager.getColor("Button.foreground");
        if (buttonForeground != saveButtonFG) {
            saveButtonFG = buttonForeground;
            ImageIcon refreshIcon = Util.recolorIcon("/refresh.png", buttonForeground, 21, 21);
            refreshButton.setIcon(refreshIcon);
            ImageIcon leftIcon = Util.recolorIcon("/left.png", buttonForeground, 21, 21);
            backButton.setIcon(leftIcon);
            ImageIcon rightIcon = Util.recolorIcon("/right.png", buttonForeground, 21, 21);
            forwardButton.setIcon(rightIcon);
        }
    }

    public void setMenuItem(MenuItem mi) {
        this.mi = mi;
    }

    public MenuItem getMenuItem() {
        return mi;
    }


    // don't leak this from your constructor says IDE
    private void init(String url, Page page) {
        if (CUSTOM_LABELS.contains(url) && !url.equals(INFO_LABEL)) {
            showCustomPage(url, true, null);
        } else {
            Alhena.processURL(url, page, null, page);
        }
    }

    private void bookmarkPage() {
        if (visiblePage().textPane.getDocMode() == GeminiTextPane.INFO_MODE) {
            Util.infoDialog(this, "Invalid", "This page can't be bookmarked");
            return;
        }
        String subject = visiblePage().textPane.getFirstHeading();
        JTextField labelField = new JTextField();
        labelField.addMouseListener(new ContextMenuMouseListener());
        if (subject != null) {
            labelField.setText(subject);
        }
        JComboBox bmComboBox = new JComboBox();
        List<String> folders;
        try {
            folders = DB.bookmarkFolders();
        } catch (SQLException ex) {
            folders = new ArrayList<String>();
        }

        if (!folders.contains("ROOT")) {
            folders.addFirst("ROOT");
        }
        for (String folder : folders) {
            bmComboBox.addItem(folder);
        }

        bmComboBox.setEditable(true);
        bmComboBox.getEditor().getEditorComponent().addMouseListener(new ContextMenuMouseListener());

        Object[] comps = new Object[4];
        comps[0] = "Label:";
        comps[1] = labelField;
        comps[2] = "Bookmark Folder:";
        comps[3] = bmComboBox;
        String res = Util.inputDialog2(this, "New", comps);
        if (res != null) {

            if (labelField.getText().trim().isEmpty() || ((String) bmComboBox.getSelectedItem()).trim().isEmpty()) {
                Util.infoDialog(this, "Required", "Bookmark label and folder required.");
            } else {
                try {
                    DB.insertBookmark(labelField.getText().trim(), visiblePage().getUrl(), ((String) bmComboBox.getSelectedItem()).trim(), null);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                bookmarkMenu.invalidate();
                addBookmarks();
                bookmarkMenu.validate();
            }
        }
    }

    private void addBookmarks() {
        try {
            List<Bookmark> bookmarks = DB.loadBookmarks();

            if (bookmarkMenu != null) {
                bookmarkMenu.removeAll();
            } else {
                bookmarkMenu = new JMenu("Bookmarks");
                bookmarkMenu.setMnemonic('B');
                menuBar.add(bookmarkMenu);
            }

            bookmarkMenu.add(createMenuItem("Bookmark Page", KeyStroke.getKeyStroke(KeyEvent.VK_D, mod), () -> {
                bookmarkPage();
            }));
            if (!bookmarks.isEmpty()) {
                bookmarkMenu.add(new JSeparator());
            }
            HashMap<String, JMenu> folders = new HashMap<>();
            bookmarks.forEach(bm -> {
                Runnable r;
                if (CUSTOM_LABELS.contains(bm.url())) {

                    r = () -> showCustomPage(bm.url(), null); // DO NOT LET INFO PAGE GET BOOKMARKED!!!
                } else {
                    r = () -> fetchURL(bm.url());
                }
                if (bm.folder().equals("ROOT")) {

                    bookmarkMenu.add(createMenuItem(bm.label(), null, () -> {
                        //fetchURL(bm.url());
                        r.run();
                    }));
                } else {
                    if (!folders.containsKey(bm.folder())) {
                        JMenu newFolder = new JMenu(bm.folder());
                        bookmarkMenu.add(newFolder);
                        folders.put(bm.folder(), newFolder);
                    }
                    folders.get(bm.folder()).add(createMenuItem(bm.label(), null, () -> {
                        //fetchURL(bm.url());
                        r.run();
                    }));
                }
            });

            // }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void fetchURL(String url) {
        fetchURL(url, null);
    }

    public void fetchURL(String url, File dataFile) {
        if (CUSTOM_LABELS.contains(url)) {
            return;
        }
        Page currentPB = visiblePage(); // empty page (new tab/window)
        Page rootPage = getRootPage(currentPB);
        if (rootPage != null) {

            if (tabbedPane == null) {

                Page pb = newPage(currentPB.textPane.getDocURLString(), null, false);
                boolean[] first = {true};
                // runnable executes after first block of data received
                Runnable r = () -> {
                    if (first[0]) {
                        first[0] = false;
                        // check to make sure user hasn't changed things in the interim
                        pb.setDataFile(null);
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb, currentPB == visiblePage());
                        if (currentPB == visiblePage()) {

                            setBusy(false, currentPB);
                            setBusy(true, histPage);
                            invalidate();

                            remove(currentPB);

                            add(histPage, BorderLayout.CENTER);

                            revalidate();
                            refreshNav(histPage);
                        } else {
                            histPage.runWhenDone(() -> currentPB.setBusy(false));
                        }
                        refreshNav(visiblePage());

                    }

                };
                pb.runWhenLoading(r);
                pb.setDataFile(dataFile);
                Alhena.processURL(url, pb, null, currentPB);

            } else {
                int currentTabIdx = tabbedPane.getSelectedIndex();

                Page pb = newPage(currentPB.textPane.getDocURLString(), null, false);

                boolean[] first = {true};
                Runnable r = () -> {
                    if (first[0]) {
                        first[0] = false;
                        pb.setDataFile(null);
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb, currentPB == visiblePage());
                        // check to make sure user hasn't changed things in the interim                        
                        if (tabbedPane == null) {
                            return; // tab removed in the interim
                        }
                        int idx = tabbedPane.getSelectedIndex();

                        if (currentTabIdx == idx) { // make sure we're on the same tab
                            if (currentPB == visiblePage()) {
                                // transfer busyness
                                setBusy(false, currentPB);
                                setBusy(true, histPage);
                                tabbedPane.setComponentAt(idx, histPage);

                                refreshNav(histPage);
                            } else {
                                histPage.runWhenDone(() -> currentPB.setBusy(false));
                            }

                        } else { // think and refactor
                            if (currentPB == visiblePage()) {
                                // transfer busyness
                                setBusy(false, currentPB);
                                setBusy(true, histPage);
                                tabbedPane.setComponentAt(currentTabIdx, histPage);
                            } else {
                                histPage.runWhenDone(() -> currentPB.setBusy(false));
                            }
                        }

                    }
                };
                pb.runWhenLoading(r);
                pb.setDataFile(dataFile);
                Alhena.processURL(url, pb, null, currentPB);
            }
        } else {
            Page nPage = addPageToHistory(null, currentPB, true);
            nPage.setDataFile(dataFile);
            nPage.runWhenLoading(() -> nPage.setDataFile(null));
            Alhena.processURL(url, nPage, null, currentPB);

        }
    }

    public record ThemeInfo(String className, String label, boolean isDark) {

    }

    private void refresh() {
        Page visiblePage = visiblePage();

        visiblePage.textPane.getDocURL().ifPresent(cURL -> {
            visiblePage.textPane.resetLastClicked();
            switch (visiblePage.textPane.getDocMode()) {
                case GeminiTextPane.HISTORY_MODE ->
                    loadHistory(visiblePage.textPane, visiblePage);
                case GeminiTextPane.BOOKMARK_MODE ->
                    loadBookmarks(visiblePage.textPane, visiblePage);
                case GeminiTextPane.CERT_MODE ->
                    loadCerts(visiblePage.textPane, visiblePage);
                case GeminiTextPane.SERVER_MODE ->
                    loadServers(visiblePage.textPane, visiblePage);
                case GeminiTextPane.INFO_MODE -> {
                }
                case GeminiTextPane.DEFAULT_MODE -> {
                    if (!cURL.isEmpty()) {
                        visiblePage.setStart();
                        Alhena.processURL(cURL, visiblePage, null, visiblePage);
                    }
                }
                default -> {
                }

            }

        });
    }

    public void refreshFromCache(Page pb) {
        if (pb.textPane.imageOnly()) {
            return;
        }
        //showGlassPane(true, pb);
        String url = pb.textPane.getDocURLString();
        CurrentPage res = pb.textPane.current();
        streamChunks(res.currentPage(), 100, url, res.pMode());
    }

    public void streamChunks(StringBuilder b, int chunkSize, String url, boolean pfMode) {
        if (b == null || b.isEmpty()) {
            // showGlassPane(false);
            return;
        }
        Page vp = visiblePage();
        setBusy(true, vp);
        new Thread(() -> {
            int idx = 0;
            int endIdx = Math.min(chunkSize + idx, b.length());
            final int fIdx = idx;
            final int fEndIdx = endIdx;

            EventQueue.invokeLater(() -> {

                vp.textPane.updatePage(b.substring(fIdx, fEndIdx), pfMode, url, false);

            });
            idx = endIdx;
            while (endIdx < b.length()) {
                // there's more
                endIdx = Math.min(chunkSize + idx, b.length());
                final int fIdx2 = idx;
                final int fEndIdx2 = endIdx;
                EventQueue.invokeLater(() -> {

                    vp.textPane.addPage(b.substring(fIdx2, fEndIdx2));

                });
                idx = endIdx;
            }

            EventQueue.invokeLater(() -> {
                //Page visiblePane = visiblePage();
                Page groupPane = getRootPage(vp);
                vp.textPane.end();
                backButton.setEnabled(hasPrev(groupPane));
                forwardButton.setEnabled(hasNext(groupPane));
                setBusy(false, vp);
            });

        }).start();
    }

    private void showCustomPage(String label, InfoPageInfo info) {
        showCustomPage(label, false, info);
    }

    private void showCustomPage(String label, boolean inPlace, InfoPageInfo info) { // probably need custom refresh button handling
        if (info != null) {
            label = INFO_LABEL; // hack
        }
        Page currentPB = visiblePage();
        Page rootPage = getRootPage(currentPB);
        if (rootPage != null && !inPlace) {

            if (tabbedPane == null) {

                Page pb = newPage(label, null, false);

                boolean[] first = {true};
                Runnable r = () -> {
                    if (first[0]) {
                        first[0] = false;
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb, currentPB == visiblePage());
                        if (currentPB == visiblePage()) { // in case something has changed in the interim
                            setBusy(false, currentPB);
                            setBusy(true, histPage);
                            invalidate();

                            remove(currentPB);
                            add(histPage, BorderLayout.CENTER);

                            revalidate();
                            refreshNav(histPage);
                        } else {
                            histPage.runWhenDone(() -> currentPB.setBusy(false));
                        }
                        refreshNav(visiblePage());
                    }

                };
                pb.runWhenLoading(r);
                switch (label) {
                    case HISTORY_LABEL ->
                        loadHistory(pb.textPane, pb);
                    case BOOKMARK_LABEL ->
                        loadBookmarks(pb.textPane, pb);
                    case CERT_LABEL ->
                        loadCerts(pb.textPane, pb);
                    case INFO_LABEL ->
                        loadInfo(pb.textPane, info);
                    case SERVERS_LABEL ->
                        loadServers(pb.textPane, pb);
                    default -> {
                    }
                }

            } else {
                int currentTabIdx = tabbedPane.getSelectedIndex();

                Page pb = newPage(label, null, false);
                boolean[] first = {true};
                Runnable r = () -> {
                    if (first[0]) {
                        first[0] = false;
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb, currentPB == visiblePage());
                        if (tabbedPane == null) {
                            return; // tab removed in the interim
                        }
                        int idx = tabbedPane.getSelectedIndex();

                        if (currentTabIdx == idx) {
                            if (currentPB == visiblePage()) {
                                setBusy(false, currentPB);
                                setBusy(true, histPage);

                                tabbedPane.setComponentAt(idx, histPage);
                                refreshNav(histPage);
                            } else {
                                histPage.runWhenDone(() -> currentPB.setBusy(false));
                            }
                        } else { // added in to replicated what is the main tab handling. custom pages load too fast to test???
                            if (currentPB == visiblePage()) {

                                setBusy(false, currentPB);
                                setBusy(true, histPage);
                                tabbedPane.setComponentAt(currentTabIdx, histPage);
                            } else {
                                histPage.runWhenDone(() -> currentPB.setBusy(false));
                            }

                        }

                    }
                };
                pb.runWhenLoading(r);
                switch (label) {
                    case HISTORY_LABEL ->
                        loadHistory(pb.textPane, pb);
                    case BOOKMARK_LABEL ->
                        loadBookmarks(pb.textPane, pb);
                    case CERT_LABEL ->
                        loadCerts(pb.textPane, pb);
                    case INFO_LABEL ->
                        loadInfo(pb.textPane, info);
                    case SERVERS_LABEL ->
                        loadServers(pb.textPane, pb);
                    default -> {
                    }
                }

            }
        } else {
            Page visiblePB = visiblePage(); // empty page (new tab/window)

            Page nPage = !inPlace ? addPageToHistory(null, visiblePB, true) : visiblePB;

            switch (label) {
                case HISTORY_LABEL ->
                    loadHistory(nPage.textPane, null);
                case BOOKMARK_LABEL ->
                    loadBookmarks(nPage.textPane, null);
                case CERT_LABEL ->
                    loadCerts(nPage.textPane, null);
                case INFO_LABEL ->
                    loadInfo(nPage.textPane, info);
                case SERVERS_LABEL ->
                    loadServers(nPage.textPane, null);
                default -> {
                }
            }
            setTitle(label);

        }

    }

    public String createTitle(String url, String firstHeading) {
        String shortTitle = DB.getUrlLabel(url);
        if (shortTitle == null) {

            shortTitle = firstHeading;

        }
        if (shortTitle == null) {
            shortTitle = url;
        }
        return shortTitle;
    }

    @Override
    public void setTitle(String title) {
        if (title.length() > 70) {
            title = title.substring(0, 70) + "...";
        }
        if (SystemInfo.isMacOS) {
            titleLabel.setText(title);
        } else {
            super.setTitle(title);
        }
        if (mi != null) {
            mi.setLabel(title);
        }
        if (tabbedPane != null) {
            int idx = tabbedPane.getSelectedIndex();
            if (idx != -1) {

                tabbedPane.setTitleAt(idx, abbrev(title));
            }
        }
    }

    private void loadBookmarks(GeminiTextPane textPane, Page p) {
        try {
            if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

                tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), BOOKMARK_LABEL);
            }
            List<Bookmark> bookmarks = DB.loadBookmarks();
            if (!bookmarks.isEmpty()) {
                textPane.updatePage("#Bookmarks 🔖\nRight click to edit or delete a bookmark.\n\n", false, BOOKMARK_LABEL, true);

                LinkedHashMap<String, ArrayList<Bookmark>> folders = new LinkedHashMap<>();
                bookmarks.forEach(bm -> {
                    if (!folders.containsKey(bm.folder())) {
                        folders.put(bm.folder(), new ArrayList<>());
                    }
                    folders.get(bm.folder()).add(bm);

                });

                folders.entrySet().stream().forEach(entry -> {
                    if (!entry.getKey().equals("ROOT")) {
                        textPane.addPage("\n##" + entry.getKey() + "\n");
                    } else {
                        textPane.addPage("\n");
                    }
                    entry.getValue().stream().forEach(bm -> {
                        textPane.addPage("=> " + bm.id() + ":" + bm.url() + " " + bm.label() + "\n");
                    });

                });
                textPane.end();

                //updateComboBox(BOOKMARK_LABEL);
            } else {
                textPane.end("#Bookmarks🔖\n", false, BOOKMARK_LABEL, true);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadCerts(GeminiTextPane textPane, Page p) {
        try {
            if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

                tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), CERT_LABEL);
            }
            List<DBClientCertInfo> certs = DB.loadCerts();
            if (!certs.isEmpty()) {
                textPane.updatePage("#Client Certificates 🔑\nClick to toggle active state. Right click to activate, inactivate or delete a cert.\n", false, CERT_LABEL, true);

                LinkedHashMap<Boolean, ArrayList<DBClientCertInfo>> types = new LinkedHashMap<>();
                certs.forEach(c -> {
                    if (!types.containsKey(c.active())) {
                        types.put(c.active(), new ArrayList<>());
                    }
                    types.get(c.active()).add(c);

                });

                Stream.of(true, false) // process in order
                        .filter(types::containsKey) // Ensure key exists
                        .forEach(active -> {
                            String label = active ? "Active" : "Inactive";
                            textPane.addPage("\n## " + label + " Certs\n\n");

                            types.get(active).forEach(cert -> {
                                try {
                                    X509Certificate xc = (X509Certificate) Alhena.loadCertificate(cert.cert());
                                    X500Principal principal = xc.getSubjectX500Principal();
                                    textPane.addPage("=> " + cert.id() + "," + cert.active() + ":" + cert.domain() + " " + cert.domain() + " [" + principal.getName() + "]\n");
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                            });
                        });

                textPane.end();

            } else {
                textPane.end("#Client Certificates 🔑\n", false, CERT_LABEL, true);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadInfo(GeminiTextPane textPane, InfoPageInfo info) {

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), info.title);
        }

        textPane.end(info.content, false, INFO_LABEL, false);
        textPane.setDocURL(info.title);

        // problem: end already sets comboBox
        updateComboBox(new ComboItem(info.title, () -> {
            return info;
        }));
        setTitle(info.title);

    }

    private void loadHistory(GeminiTextPane textPane, Page p) {
        setBusy(true, p);

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), HISTORY_LABEL);
        }

        textPane.updatePage("# History 🏛\n", false, HISTORY_LABEL, true);
        new Thread(() -> {
            int[] count = {0};
            try {
                count[0] = DB.loadHistory(textPane);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            EventQueue.invokeLater(() -> {
                if (count[0] == 0) {
                    textPane.addPage("\n### Nothing to see\n");
                } else {
                    setTmpStatus(count[0] + " links");
                }

                if (p != null) {

                    p.loading();
                }

                setBusy(false, p);
            });

        }).start();
    }

    public void setTmpStatus(String msg) {
        setStatus(msg);
        Timer timer = new Timer(5000, event -> {
            if (statusField.getText().equals(msg)) {
                setStatus(" ");
            }
        });
        timer.setRepeats(false);
        timer.start();
    }

    private void loadServers(GeminiTextPane textPane, Page p) {
        setBusy(true, p);

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), SERVERS_LABEL);
        }

        textPane.updatePage("# Servers 🖥\n", false, SERVERS_LABEL, true);
        new Thread(() -> {
            int[] count = {0};
            try {
                count[0] = DB.loadServers(textPane);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            EventQueue.invokeLater(() -> {
                if (count[0] == 0) {
                    textPane.addPage("\n### Nothing to see\n");
                } else {
                    setTmpStatus(count[0] + " servers");
                }

                if (p != null) {

                    p.loading();
                }

                setBusy(false, p);
            });

        }).start();
    }

    public void deleteBookmark(Component f, String id) {
        int bmId = Integer.parseInt(id);
        try {
            Bookmark bm = DB.getBookmark(bmId);
            Object res = Util.confirmDialog(f, "Delete?", "Are you sure you want to delete this bookmark?\n" + bm.label(), JOptionPane.YES_NO_OPTION, null);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    DB.deleteBookmark(bmId);
                    Alhena.updateFrames(true, false);
                    EventQueue.invokeLater(() -> refresh());

                }

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    private record InfoPageInfo(String title, String content) {

    }

    public void exportCert(int id, GeminiTextPane textPane) {
        try {
            ClientCertInfo certInfo = DB.getClientCertInfo(id);
            String lf = certInfo.cert().endsWith("\n") ? "" : "\n";
            String pem = certInfo.cert() + lf + certInfo.privateKey();

            showCustomPage(INFO_LABEL, new InfoPageInfo(certInfo.domain().substring(0, certInfo.domain().indexOf("/")).replace(':', '-') + "." + certInfo.id() + ".pem", pem));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteCert(int id) {
        try {
            ClientCertInfo certInfo = DB.getClientCertInfo(id);
            X509Certificate xc = (X509Certificate) Alhena.loadCertificate(certInfo.cert());
            X500Principal principal = xc.getSubjectX500Principal();
            Object res = Util.confirmDialog(this, "Confirm", "Are you sure you want to delete this certificate?\n" + certInfo.domain() + " [" + principal.getName() + "]", JOptionPane.YES_NO_OPTION, null);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {

                    DB.deleteClientCert(id);
                    Alhena.closeNetClient(certInfo);

                    refresh();
                    //Util.infoDialog(this, "Delete", "Certificate deleted");

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void toggleCert(int id, boolean active, String url) {
        try {
            URI uri = URI.create(url);
            int port = uri.getPort() == -1 ? 1965 : uri.getPort();
            String prunedUrl = uri.getHost() + ":" + port + uri.getPath();
            DB.toggleCert(id, active, prunedUrl, false);
            ClientCertInfo certInfo = DB.getClientCertInfo(id);
            Alhena.closeNetClient(certInfo);
            //Alhena.removeNetClient(host);
            //Alhena.createNetClient();
            refresh();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteFromHistory(String url, boolean prompt) {
        String match = null;
        if (prompt) {
            match = Util.inputDialog(this, "Delete History", "Enter a string. All links containing the string will be removed from history.", false);
            if (match == null || match.isBlank()) {
                return;

            } else {
                Object res = Util.confirmDialog(this, "Confirm", "Are you sure you want to delete all history containing:\n\"" + match + "\"", JOptionPane.YES_NO_OPTION, null);
                if (res instanceof Integer result) {
                    if (result == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
            }

        }
        try {
            int rowCount = DB.deleteHistory(url, match);
            refresh();
            String verbiage = rowCount == 1 ? "1 link " : rowCount + " links ";
            Util.infoDialog(this, "Update", verbiage + "removed from history");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void clearHistory() {
        try {
            Object res = Util.confirmDialog(this, "Confirm", "Are you sure you want to delete all history?", JOptionPane.YES_NO_OPTION, null);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    int rowCount = DB.deleteHistory();
                    refresh();
                    String verbiage = rowCount == 0 ? "No links " : rowCount == 1 ? "1 link " : rowCount + " links ";
                    Util.infoDialog(this, "Update", verbiage + "removed from history");

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    public void updateBookmark(Component f, String id) {
        int bmId = Integer.parseInt(id);
        try {
            Bookmark bm = DB.getBookmark(bmId);
            JTextField labelField = new JTextField();
            // replace with common clase

            labelField.addMouseListener(new ContextMenuMouseListener());
            labelField.setText(bm.label());
            JComboBox bmComboBox = new JComboBox();

            List<String> folders;
            try {
                folders = DB.bookmarkFolders();
            } catch (SQLException ex) {
                folders = new ArrayList<String>();
            }

            if (!folders.contains("ROOT")) {
                folders.addFirst("ROOT");
            }
            for (String folder : folders) {
                bmComboBox.addItem(folder);
            }
            bmComboBox.setSelectedItem(bm.folder());

            bmComboBox.setEditable(true);

            bmComboBox.getEditor().getEditorComponent().addMouseListener(new ContextMenuMouseListener());

            Object[] comps = new Object[4];
            comps[0] = "Label:";
            comps[1] = labelField;
            comps[2] = "Bookmark Folder:";
            comps[3] = bmComboBox;
            String res = Util.inputDialog2(this, "New", comps);
            if (res != null) {
                if (!labelField.getText().trim().isEmpty() && !((String) bmComboBox.getSelectedItem()).trim().isEmpty()) {
                    DB.updateBookmark(bmId, labelField.getText(), (String) bmComboBox.getSelectedItem());
                }
                refresh();
            }

            Alhena.updateFrames(true, false);
            EventQueue.invokeLater(() -> refresh());
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void createCert(URI uri) {
        int port = uri.getPort() == -1 ? 1965 : uri.getPort();
        String prunedUrl = uri.getHost() + ":" + port + uri.getPath();
        Page page = visiblePage();
        boolean success = Alhena.certRequired(null, uri, page, page.getCert(), null);
        if (success) {
            try {
                ClientCertInfo ci = DB.getClientCertInfo(prunedUrl);
                // if (ci != null) {
                //     DB.toggleCert(ci.id(), false, prunedUrl, false); // set existing for host to inactive

                // }
                Alhena.closeNetClient(DB.getClientCertInfo(prunedUrl)); //lazy
                Util.infoDialog(this, "Added", "New client certificate added for : " + uri + ".");
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            refresh();
        }
    }

    public void importPem(URI uri, File f) {
        String host = uri.getHost();
        if (host == null || !uri.getScheme().equals("gemini")) {
            Util.infoDialog(this, "Invalid", "Invalid scheme. Can't import PEM file.");
            return;
        }
        if (f == null) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PEM files (*.pem)", "pem");
            f = Util.getFile(this, null, true, "Select PEM File", filter);
        }

        if (f != null) {
            try {
                PemData pemData = Util.extractPemParts(f.getAbsolutePath());
                if(pemData.cert() == null || pemData.key() == null){
                    Util.infoDialog(this, "Format", "Not a recognized PEM format");    
                }else{
                    int port = uri.getPort() == -1 ? 1965 : uri.getPort();
                    String prunedUrl = uri.getHost() + ":" + port + uri.getPath();

                    boolean exists = DB.loadCerts().stream().anyMatch(c -> c.cert().equals(pemData.cert()) && c.domain().equals(prunedUrl));
                    if(exists){
                        Util.infoDialog(this, "Duplicate", "This cert already exists for: " + uri, JOptionPane.WARNING_MESSAGE);    
                    }else{
                        ClientCertInfo ci = DB.getClientCertInfo(prunedUrl);
                        if (ci != null) {
                            DB.toggleCert(ci.id(), false, prunedUrl, false); // set existing for host to inactive

                        }

                        DB.insertClientCert(prunedUrl, pemData.cert(), pemData.key(), true, null);
                        Alhena.addCertToTrustStore(uri, visiblePage().getCert());

                        Alhena.closeNetClient(DB.getClientCertInfo(prunedUrl)); //lazy
                        Util.infoDialog(this, "Added", "PEM added for : " + uri);
                        refresh();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public void openFile(File file) {

        if (file == null) {
            String desc = Alhena.fileExtensions.stream().collect(Collectors.joining(", "));
            String[] exts = Alhena.fileExtensions.toArray(new String[0]);
            String[] cleanExtensions = Alhena.fileExtensions.stream()
                    .map(ext -> ext.substring(1))
                    .toArray(String[]::new);

            FileNameExtensionFilter filter = new FileNameExtensionFilter(desc, cleanExtensions);

            file = Util.getFile(this, null, true, "Select File", filter);
        }

        if (file != null && file.exists()) {
            try {
                URI fileUri = file.toURI();
                fetchURL(fileUri.toString());

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public void savePage(GeminiTextPane textPane, StringBuilder sb, int currentMode) {
        String suggestedName;
        if (currentMode == GeminiTextPane.INFO_MODE) {
            suggestedName = ((ComboItem) comboBox.getSelectedItem()).url;
        } else {
            suggestedName = DB.getUrlLabel(textPane.getDocURLString());
            if (suggestedName != null) {
                suggestedName = suggestedName.replace(' ', '_') + ".gmi";
            }
            if (suggestedName == null) {
                suggestedName = textPane.getFirstHeading();
                if (suggestedName != null) {
                    suggestedName = suggestedName.replace(' ', '_') + ".gmi";
                }
            }
            if (suggestedName == null) {
                suggestedName = "geminipage.gmi";
            }
        }
        FileNameExtensionFilter filter = new FileNameExtensionFilter("gemini files (*.gmi, *.pem)", "gmi", "pem");
        File saveFile = Util.getFile(this, suggestedName, false, "Save File", filter);
        if (saveFile != null) {
            setBusy(true, textPane);

            try (BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile))) {

                if (currentMode != GeminiTextPane.BOOKMARK_MODE && currentMode != GeminiTextPane.CERT_MODE) {
                    bw.write(sb.toString());
                } else {
                    sb.toString().lines().forEach(line -> {
                        if (line.startsWith("=>")) {
                            line = "=> " + line.substring(line.indexOf(":") + 1);
                        }
                        try {
                            bw.write(line + "\n");
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    });

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            setBusy(false, textPane);
        }
    }

    public void toggleView(GeminiTextPane textPane, boolean isPlainText) {
        // showGlassPane(true, textPane);
        String url = textPane.getDocURLString();
        CurrentPage res = textPane.current();
        visiblePage().ignoreStart();
        streamChunks(res.currentPage(), 100, url, isPlainText);
    }

    public void viewServerCert(GeminiTextPane textPane, URI uri) {
        String host = uri.getHost();

        Optional<Page> page = Optional.ofNullable(SwingUtilities.getAncestorOfClass(Page.class, textPane))
                .map(component -> (Page) component);
        InfoPageInfo pageInfo = new InfoPageInfo("servercert: " + host, page.get().getCert().toString());
        showCustomPage(INFO_LABEL, pageInfo);

    }

    public void newTab(String url) {

        if (tabbedPane == null) {
            invalidate();
            tabbedPane = new JTabbedPane();

            tabbedPane.putClientProperty(FlatClientProperties.TABBED_PANE_TAB_CLOSABLE, true);

            tabbedPane.putClientProperty("JTabbedPane.tabCloseCallback",
                    (IntConsumer) tabIndex -> {
                        // close tab here
                        if (tabbedPane.getTabCount() == 2) {
                            GeminiFrame.this.invalidate();
                            ChangeListener[] cl = tabbedPane.getChangeListeners();
                            for (ChangeListener ev : cl) {
                                tabbedPane.removeChangeListener(ev);
                                break; // REMOVES tabbedPanes changeListener but not the L&F changeListener - CAN ORDER CHANGE?
                            }

                            Page page = (Page) tabbedPane.getComponentAt(tabIndex);
                            pageHistoryMap.remove(getRootPage(page));
                            tabbedPane.remove(tabIndex);
                            page = (Page) tabbedPane.getSelectedComponent();
                            tabbedPane.remove(page);
                            GeminiFrame.this.remove(tabbedPane);
                            tabbedPane = null;
                            GeminiFrame.this.add(page, BorderLayout.CENTER);
                            String frameTitle = createTitle(page.textPane.getDocURLString(), page.textPane.getFirstHeading());
                            if (frameTitle != null) {
                                setTitle(frameTitle);
                            } else {
                                setTitle("New Tab");
                                selectComboBoxItem("");
                            }
                            GeminiFrame.this.validate();
                        } else {
                            Page page = (Page) tabbedPane.getComponentAt(tabIndex);
                            pageHistoryMap.remove(getRootPage(page));
                            tabbedPane.remove(tabIndex);
                        }
                    });

            Page pb = visiblePage();

            remove(pb);
            String ft = createTitle(pb.textPane.getDocURLString(), pb.textPane.getFirstHeading());

            tabbedPane.addTab(abbrev(ft), pb);

            selectComboBoxItem("");

            tabbedPane.addChangeListener(ce -> {

                Page page = (Page) tabbedPane.getSelectedComponent();

                if (!page.busy() && getGlassPane().isShowing()) {
                    showGlassPane(false);
                } else if (page.busy() && !getGlassPane().isShowing()) {
                    showGlassPane(true);
                }

                GeminiTextPane textPane = page.textPane;

                textPane.getDocURL().ifPresentOrElse(title1 -> {

                    selectComboBoxItem(title1);

                }, () -> {
                    selectComboBoxItem("");
                });
                if (page.getThemeId() != currentThemeId) {
                    refreshFromCache(visiblePage());
                    page.setThemeId(currentThemeId);
                }

                Page rootPage = getRootPage(visiblePage());
                if (rootPage != null) { // NOT 100% ON THIS
                    backButton.setEnabled(hasPrev(rootPage));
                    forwardButton.setEnabled(hasNext(rootPage));
                } else {
                    backButton.setEnabled(false);
                    forwardButton.setEnabled(false);
                }
                if (textPane.getDocURLString() != null) {
                    String frameTitle = createTitle(textPane.getDocURLString(), textPane.getFirstHeading());
                    setTitle(frameTitle);

                } else {
                    setTitle("New Tab");
                }

            });

            add(tabbedPane, BorderLayout.CENTER);

            revalidate();

        }

        Page currentPage = visiblePage();
        String du = currentPage.textPane.getDocURLString();

        Page pb = newPage(du, null, true);
        pb.setThemeId(currentThemeId);

        tabbedPane.addTab("  ", pb);

        tabbedPane.setSelectedComponent(pb);

        Alhena.processURL(url, pb, null, currentPage);
        currentPage.setBusy(false);


    }

    public void refreshNav(Page page) {
        Page vPane = page == null ? visiblePage() : page;
        //Page vPane = visiblePage();
        Page rootPage = getRootPage(vPane);

        backButton.setEnabled(hasPrev(rootPage));
        forwardButton.setEnabled(hasNext(rootPage));
        refreshButton.setEnabled(vPane.textPane.getDocURL().isPresent());

    }

    public void setStatus(String msg) {
        statusField.setText(msg);
    }

    public void shutDown() {
        setVisible(false);
        dispose();
    }

    public HashMap<KeyStroke, Runnable> actionMap = new HashMap<>();

    private JMenuItem createMenuItem(String text, KeyStroke keyStroke, Runnable r) {
        JMenuItem menuItem = new JMenuItem(text);
        actionMap.put(keyStroke, r);
        menuItem.addActionListener(al -> {
            r.run();
        });

        if (keyStroke != null) {
            menuItem.setAccelerator(keyStroke);
        }

        return menuItem;
    }

    // this can probably go in it's current form
    public void setBusy(boolean busy, Component c) {

        Runnable r = () -> {
            Page page;
            if (c instanceof Page) {
                page = (Page) c;
            } else {
                page = (Page) SwingUtilities.getAncestorOfClass(Page.class, c);
            }
            if (page != null) {
                page.setBusy(busy);
            }

        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
    }

    // only call on EDT
    public void showGlassPane(boolean visible) {
        if (!(getGlassPane() instanceof GeminiGlassPane)) {
            setGlassPane(new GeminiGlassPane());
        }
        //validate();
        // get the existing blocking glass pane
        GeminiGlassPane bgp = (GeminiGlassPane) getGlassPane();
        //bgp.setPainter(painter);
        bgp.setVisible(visible);
        if (visible) {
            bgp.start();
        } else {
            bgp.stop();
        }
        bgp.repaint();
    }

    private record ComboItem(String url, Supplier<InfoPageInfo> supplier) {

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof ComboItem ci) {
                return ci.url.equals(url);
            }
            return false;

        }

        @Override
        public int hashCode() {
            return url.hashCode();
        }

        @Override
        public String toString() {
            return url;
        }
    }

    public void updateComboBox(String s) {
        updateComboBox(new ComboItem(s, null));
    }

    private void updateComboBox(ComboItem origURL) {

        EventQueue.invokeLater(() -> {
            ItemListener[] il = comboBox.getItemListeners();

            for (ItemListener i : il) {
                comboBox.removeItemListener(i);
            }

            ComboItem foundItem = null;
            int i;

            for (i = 0; i < comboBox.getItemCount(); i++) {
                if (comboBox.getItemAt(i).equals(origURL)) {
                    foundItem = (ComboItem) comboBox.getItemAt(i);
                    break;
                }
            }

            if (foundItem != null) {
                comboBox.removeItemAt(i);
                comboBox.insertItemAt(foundItem, 0);
                comboBox.setSelectedItem(foundItem);
            } else {
                comboBox.insertItemAt(origURL, 0);
                comboBox.setSelectedItem(origURL);
            }

            for (ItemListener j : il) {
                comboBox.addItemListener(j);
            }
        });
    }

    public void selectComboBoxItem(String s) {
        selectComboBoxItem(new ComboItem(s, null));
    }

    private void selectComboBoxItem(ComboItem item) {
        // only call from event dispatch thread
        ItemListener[] il = comboBox.getItemListeners();
        for (ItemListener i : il) {
            comboBox.removeItemListener(i);
        }
        comboBox.setSelectedItem(item);

        for (ItemListener i : il) {
            comboBox.addItemListener(i);
        }
    }


    public Page visiblePage() {
        Page vPage;
        Component centerComponent = ((BorderLayout) getContentPane().getLayout()).getLayoutComponent(BorderLayout.CENTER);
        if (centerComponent instanceof JTabbedPane) {
            Component selected = tabbedPane.getSelectedComponent();
            vPage = (Page) selected;

        } else {

            vPage = (Page) centerComponent;
        }

        return vPage;

    }

    public final String abbrev(String t) {
        if (t.contains("://")) {
            t = t.substring(t.indexOf("://") + 3);
        } else if (t.contains(":/")) {
            t = t.substring(t.indexOf(":/") + 2); // file:/
        }
        if (t.length() > 30) {
            t = t.substring(0, 30) + "...";
        }
        return t;
    }


    public static String getArt() {

        String doc = Util.readResourceAsString("/art.gmi");
        List<String> blocks = new ArrayList<>();
        Pattern pattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(doc);

        while (matcher.find()) {
            blocks.add(matcher.group(1)); // Trim to remove extra spaces or newlines
        }

        int randomIdx = new Random().nextInt(blocks.size());
        return blocks.get(randomIdx);

    }
}
