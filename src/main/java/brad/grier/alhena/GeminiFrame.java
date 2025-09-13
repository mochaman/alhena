package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.imageio.ImageIO;
import javax.security.auth.x500.X500Principal;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
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
import javax.swing.JSlider;
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
import io.vertx.core.json.JsonObject;

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
    public static final String HISTORY_LABEL = I18n.t("historyItem");
    public static final String BOOKMARK_LABEL = I18n.t("bookmarksItem");
    public static final String CERT_LABEL = I18n.t("certsItem");
    public static final String INFO_LABEL = I18n.t("infoLabel");
    public static final String SERVERS_LABEL = I18n.t("serversItem");
    public static final List<String> CUSTOM_LABELS = List.of(HISTORY_LABEL, BOOKMARK_LABEL, CERT_LABEL, INFO_LABEL, SERVERS_LABEL); // make immutable
    public static String proportionalFamily = "SansSerif";
    public static final int DEFAULT_FONT_SIZE = 20;
    public static int fontSize = DEFAULT_FONT_SIZE;
    public static int monoFontSize = DEFAULT_FONT_SIZE;
    public static boolean ansiAlert;
    public static Font saveFont;
    public final static String SYNC_SERVER = "ultimatumlabs.com:1965/";
    private int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private MenuItem mi;
    private Color saveButtonFG = null;
    private static JRadioButtonMenuItem lastSelectedItem;
    public static int tabPosition = 0;

    private Map<String, ThemeInfo> themes = Map.ofEntries(
            Map.entry("FlatArcIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcIJTheme", "Arc", false)),
            Map.entry("FlatGruvboxDarkHardIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGruvboxDarkHardIJTheme", "Gruvbox Dark Hard", true)),
            Map.entry("FlatGradiantoNatureGreenIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoNatureGreenIJTheme", "Gradianto Nature Green", true)),
            Map.entry("FlatDarkFlatIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDarkFlatIJTheme", "Dark Flat", true)),
            Map.entry("FlatMTMaterialOceanicIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme", "Material Oceanic", true)),
            Map.entry("FlatMTLightOwlIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTLightOwlIJTheme", "Light Owl", false)),
            Map.entry("FlatDarculaLaf", new ThemeInfo("com.formdev.flatlaf.FlatDarculaLaf", "Darcula", true)),
            Map.entry("FlatIntelliJLaf", new ThemeInfo("com.formdev.flatlaf.FlatIntelliJLaf", "IntelliJ", false)),
            Map.entry("FlatMacDarkLaf", new ThemeInfo("com.formdev.flatlaf.themes.FlatMacDarkLaf", "MacOS Dark", true)),
            Map.entry("FlatMacLightLaf", new ThemeInfo("com.formdev.flatlaf.themes.FlatMacLightLaf", "MacOS Light", false)),
            Map.entry("FlatMTMaterialPalenightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme", "Material Palenight", true)),
            Map.entry("FlatDarkPurpleIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDarkPurpleIJTheme", "Dark Purple", true)),
            Map.entry("FlatMonokaiProIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatMonokaiProIJTheme", "Monokai Pro", true)),
            Map.entry("FlatGradiantoMidnightBlueIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoMidnightBlueIJTheme", "Gradianto Midnight Blue", true)),
            Map.entry("FlatMTMoonlightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMoonlightIJTheme", "Moonlight", true)),
            Map.entry("FlatMTMaterialLighterIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme", "Material Lighter", false)),
            Map.entry("FlatMTGitHubIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme", "GitHub", false)),
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
            Map.entry("FlatMTAtomOneLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneLightIJTheme", "Atom One Light", false)),
            Map.entry("FlatSpacegrayIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme", "Spacegray", true)),
            Map.entry("FlatSolarizedLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", "Solarized Light", false)),
            Map.entry("FlatMTAtomOneDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneDarkIJTheme", "Atom One Dark", true)),
            Map.entry("FlatMTGitHubDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubDarkIJTheme", "GitHub Dark", true)),
            Map.entry("FlatMTMaterialDarkerIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialDarkerIJTheme", "Material Darker", true)),
            Map.entry("FlatMTDraculaIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTDraculaIJTheme", "Dracula Material", true))
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
            if (histList != null) {
                if (pageVisible) {
                    int histIdx = rootPage.incAndGetArrayIndex(); // should get same result if calling on page.
                    if (histIdx < histList.size()) {
                        List<Page> sl = histList.subList(histIdx, histList.size());
                        for (Page p : sl) {

                            p.textPane.closePlayers();
                        }
                        sl.clear();
                    }
                }
                histList.add(page);
            }
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

                    for (Page p : sl) {
                        //System.out.println("removing: " + p);
                        p.textPane.closePlayers();
                    }
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

        // test to see if db font exists - maybe deleted from system or db moved to another os
        // fonts created with invalid names are created anyway with Dialog font family
        if (saveFont.getFamily().equals("Dialog") && !saveFont.getName().equals("Dialog")) {

            saveFont = new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
            proportionalFamily = "SansSerif";
            monoFontSize = fontSize = DEFAULT_FONT_SIZE;
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
        // this allows keyboard traversal with ActionListener
        comboBox.putClientProperty("JComboBox.isTableCellEditor", Boolean.TRUE);

        comboBox.setEditable(true);

        comboBox.addActionListener(al -> {
            if (comboBox.getSelectedItem() == null) {
                return;
            }
            if ("comboBoxChanged".equals(al.getActionCommand())) {
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

                        fetchURL(cbUrl, true);
                    }
                }
            }
        });

        initComboBox();
        ImageIcon leftIcon = Util.recolorIcon("/left.png", UIManager.getColor("Button.foreground"), 21, 21);
        Font buttonFont = new Font("Noto Emoji Regular", Font.PLAIN, 18);
        backButton = new JButton(leftIcon);

        //I18n.t("certsItem")
        backButton.setToolTipText(I18n.t("backButtonTip"));
        ImageIcon rightIcon = Util.recolorIcon("/right.png", UIManager.getColor("Button.foreground"), 21, 21);
        forwardButton = new JButton(rightIcon);
        forwardButton.setToolTipText(I18n.t("forwardButtonTip"));

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
            visiblePage().textPane.requestFocusInWindow();

        });

        forwardButton.setEnabled(false);
        forwardButton.addActionListener(al -> {

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
            visiblePage().textPane.requestFocusInWindow();

        });

        ImageIcon refreshIcon = Util.recolorIcon("/refresh.png", UIManager.getColor("Button.foreground"), 21, 21);
        refreshButton = new JButton(refreshIcon);
        refreshButton.setToolTipText(I18n.t("refreshButtonTip"));
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
        homeButton.setToolTipText(I18n.t("homeButtonTip"));
        homeButton.addActionListener(al -> {

            String homePage = Util.getHome();
            if (GeminiFrame.CUSTOM_LABELS.contains(homePage)) {
                showCustomPage(homePage, null);
            } else {
                fetchURL(homePage, false);
            }
        });
        homeButton.setFont(buttonFont);

        favButton = new JButton("🔖");
        favButton.setToolTipText(I18n.t("bookmarkButtonTip"));
        favButton.addActionListener(al -> {
            bookmarkPage(false);

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
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(1, 6, 7, 4);

        navPanel.add(comboBox, gridBagConstraints);

        Supplier<List<JComponent>> dynamicMenuSupplier = () -> {
            List<JComponent> items = new ArrayList<>();
            try {
                List<Bookmark> mList = DB.loadTopBookmarks();

                mList.stream().forEach(bmark -> {
                    JMenuItem mi = new JMenuItem(bmark.label());
                    mi.addActionListener(e -> {
                        fetchURL(bmark.url(), false);
                    });
                    items.add(mi);
                });
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            return items;
        };
        PopupMenuButton hotButton = new PopupMenuButton("🔥", dynamicMenuSupplier, I18n.t("noHistoryPopupLabel"));
        hotButton.setFont(new Font("Noto Emoji Regular", Font.PLAIN, 18));
        hotButton.setToolTipText(I18n.t("hotButtonTip"));
        hotButton.setFont(buttonFont);

        navPanel.add(hotButton, c);
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
        setTmpStatus(Alhena.welcomeMessage);
        statusField.setBorder(new EmptyBorder(5, 5, 5, 5)); // Add padding

        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu(I18n.t("fileMenu"));

        fileMenu.add(createMenuItem(I18n.t("openFileItem"), KeyStroke.getKeyStroke(KeyEvent.VK_O, mod), () -> {
            openFile(null);
        }));

        fileMenu.add(createMenuItem(I18n.t("printItem"), KeyStroke.getKeyStroke(KeyEvent.VK_P, mod), () -> {
            if (!Util.isPrintingAvailable()) {
                Util.infoDialog(GeminiFrame.this, I18n.t("printDialog"), I18n.t("printDialogMsg"));
                return;
            }
            GeminiTextPane gtp = visiblePage().textPane;

            Page p = new Page(Page.ROOT_PAGE, this, gtp.getDocURLString(), currentThemeId);
            StringBuilder sb = new StringBuilder(gtp.current().currentPage());
            p.textPane.end(sb.toString(), gtp.current().pMode(), gtp.getDocURLString(), false, true);

            PrinterJob job = PrinterJob.getPrinterJob();
            job.setJobName("Alhena"); // this might do something somewhere

            PageFormat pf = job.defaultPage();

            job.setPrintable(new ViewBasedTextPanePrinter(p.textPane, pf), pf);

            if (job.printDialog()) {
                try {
                    job.print();
                } catch (PrinterException ex) {
                    ex.printStackTrace();
                }
            }

        }));
        fileMenu.add(new JSeparator());

        fileMenu.add(createMenuItem(I18n.t("newTabItem"), KeyStroke.getKeyStroke(KeyEvent.VK_T, mod), () -> {
            newTab("alhena:art");

        }));

        fileMenu.add(createMenuItem(I18n.t("newWindowItem"), KeyStroke.getKeyStroke(KeyEvent.VK_N, mod), () -> {
            String home = Util.getHome();
            Alhena.newWindow(home, home);
        }));

        fileMenu.add(new JSeparator());
        JMenu userMenu = new JMenu(I18n.t("userDataItem"));

        userMenu.add(createMenuItem(I18n.t("exportDataItem"), null, () -> {

            FileNameExtensionFilter filter = new FileNameExtensionFilter("zip files (*.zip)", "zip");
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            String formattedDate = now.format(formatter);
            File f = Util.getFile(GeminiFrame.this, "alhenadb_" + formattedDate + ".zip", false, "Save", filter);
            if (f != null) {

                try {

                    DB.dumpDB(f);
                    Util.infoDialog(GeminiFrame.this, I18n.t("exportDialog"), I18n.t("exportDialogMsg"));

                } catch (Exception ex) {
                    ex.printStackTrace();
                }

            }

        }));

        userMenu.add(createMenuItem(I18n.t("importDataItem"), null, () -> {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("zip files (*.zip)", "zip");
            File f = Util.getFile(GeminiFrame.this, null, true, I18n.t("importFileDialog"), filter);
            if (f != null) {
                Util.importData(GeminiFrame.this, f, false);
            }
        }));

        userMenu.add(new JSeparator());

        userMenu.add(createMenuItem(I18n.t("syncUploadItem"), null, () -> {
            try {
                ClientCertInfo ci = DB.getClientCertInfo(SYNC_SERVER);
                if (ci == null) {

                    String message = MessageFormat.format(I18n.t("syncMissingCertDialogMsg"), SYNC_SERVER);
                    Object r = Util.confirmDialog(GeminiFrame.this, I18n.t("syncMissingCertDialog"), message, JOptionPane.YES_NO_OPTION, null, null);
                    if (r instanceof Integer result) {
                        if (result == JOptionPane.YES_OPTION) {
                            try {
                                Alhena.createKeyPair(SYNC_SERVER, "sync");
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else {
                            Util.infoDialog(GeminiFrame.this, I18n.t("syncCanceledDialog"), I18n.t("syncCanceledDialogMsg"));
                            return;
                        }
                    }
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            Object r = Util.confirmDialog(GeminiFrame.this, I18n.t("syncConfirmDialog"), I18n.t("syncConfirmDialogMsg"), JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
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
                        fetchURL(titanUrl, encFile, false);

                    } catch (Exception ex) {
                        Util.infoDialog(GeminiFrame.this, I18n.t("syncFailedDialog"), I18n.t("syncFailedDialogMsg") + "\n" + ex.getMessage());
                        ex.printStackTrace();
                    }
                }
            }

        }));

        userMenu.add(createMenuItem(I18n.t("syncDownloadItem"), null, () -> {
            try {
                File file = File.createTempFile("alhenadb", ".enc");

                fetchURL("gemini://" + SYNC_SERVER + "/sync/", file, false);
                file.deleteOnExit();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }));

        fileMenu.add(userMenu);

        if (!SystemInfo.isMacOS) {
            fileMenu.add(new JSeparator());
            fileMenu.add(createMenuItem(I18n.t("quitItem"), KeyStroke.getKeyStroke(KeyEvent.VK_Q, mod), () -> {
                Alhena.exit(GeminiFrame.this);
            }));
        }
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu(I18n.t("viewMenu"));
        viewMenu.setMnemonic('V');

        for (String label : CUSTOM_LABELS) {
            KeyStroke ks = null;
            if (!label.equals(INFO_LABEL)) {

                if (label.equals(HISTORY_LABEL)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod);
                } else if (label.equals(BOOKMARK_LABEL)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, mod);
                } else if (label.equals(CERT_LABEL)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, (mod | KeyEvent.ALT_DOWN_MASK));
                } else if (label.equals(SERVERS_LABEL)) {
                    ks = KeyStroke.getKeyStroke(KeyEvent.VK_S, (mod | KeyEvent.ALT_DOWN_MASK));
                }

                viewMenu.add(createMenuItem(label, ks, () -> {

                    showCustomPage(label, null);

                }));
            }

        }

        viewMenu.add(new JSeparator());

        viewMenu.add(createMenuItem(I18n.t("findItem"), KeyStroke.getKeyStroke(KeyEvent.VK_F, mod), () -> {

            String input = Util.inputDialog(this, I18n.t("findDialog"), I18n.t("findDialogMsg"), false, "", null);
            if (input != null) {
                visiblePage().textPane.resetSearch();
                lastSearch = input;
                visiblePage().textPane.find(input, false);

            }
        }));

        viewMenu.add(createMenuItem(I18n.t("findAgainItem"), KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), () -> {
            findAgain();
        }));

        menuBar.add(viewMenu);

        addBookmarks();

        addWindowsMenu();

        JMenu aboutMenu = new JMenu(I18n.t("helpMenu"));
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

            fetchURL(fileUri.toString(), false);

        }));

        aboutMenu.add(createMenuItem("Changes", null, () -> {
            fetchURL("gemini://ultimatumlabs.com/alhena_changes.gmi", false);
        }));

        aboutMenu.add(createMenuItem("FAQ", null, () -> {
            fetchURL("gemini://ultimatumlabs.com/alhenafaq.gmi", false);
        }));

        aboutMenu.add(createMenuItem("Details", null, () -> {
            fetchURL("alhena:info", false);
        }));

        aboutMenu.add(createMenuItem("Commands", null, () -> {
            fetchURL("alhena:", false);
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

    public void findAgain() {
        if (lastSearch == null) {
            return;
        }
        visiblePage().textPane.find(lastSearch, false);

    }

    private void addWindowsMenu() {

        if (settingsMenu != null) {
            settingsMenu.removeAll();
        } else {
            settingsMenu = new JMenu(I18n.t("settingsMenu"));
            settingsMenu.setMnemonic('S');
            menuBar.add(settingsMenu);
        }

        JMenu darkThemeMenu = new JMenu(I18n.t("darkThemesItem"));

        JMenu lightThemeMenu = new JMenu(I18n.t("lightThemesItem"));
        ButtonGroup themeGroup = new ButtonGroup();
        String theme = UIManager.getLookAndFeel().getClass().toString();
        theme = theme.substring(theme.lastIndexOf('.') + 1);
        String finalTheme = theme;

        themes.entrySet()
                .stream()
                .sorted(Comparator.comparing(entry -> entry.getValue().label))
                .forEach(entry -> {
                    String key = entry.getKey();
                    ThemeInfo value = entry.getValue();

                    JMenu jm = value.isDark() ? darkThemeMenu : lightThemeMenu;

                    JRadioButtonMenuItem themeItem = new JRadioButtonMenuItem(value.label, key.equals(finalTheme));
                    themeGroup.add(themeItem);
                    jm.add(themeItem);
                    themeItem.addActionListener(al -> {

                        try {

                            DB.insertPref("theme", value.className());
                            Alhena.theme = value.className();
                            Alhena.updateFrames(false, false, true);

                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });

                });
        settingsMenu.add(lightThemeMenu);
        settingsMenu.add(darkThemeMenu);

        settingsMenu.add(createMenuItem(I18n.t("fontItem"), KeyStroke.getKeyStroke(KeyEvent.VK_F, (mod | KeyEvent.ALT_DOWN_MASK)), () -> {
            Font defFont = saveFont != null ? saveFont : new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
            Font font = Util.getFont(GeminiFrame.this, defFont, true, true);
            if (font != null) {
                saveFont = font;

                proportionalFamily = font.getName();
                fontSize = font.getSize();
                Alhena.updateFrames(false, false, false);

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
            macUseNoto = Alhena.macUseNoto;
        }

        JMenu emojiMenu = new JMenu(I18n.t("emojiItem"));
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
        JRadioButtonMenuItem fontItem = new JRadioButtonMenuItem(I18n.t("fontItem"), emojiPref.equals("font") && !macUseNoto);
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

        settingsMenu.add(createMenuItem(I18n.t("layoutItem"), KeyStroke.getKeyStroke(KeyEvent.VK_L, (mod | KeyEvent.ALT_DOWN_MASK)), () -> {
            JSlider slider = new JSlider(50, 100, (int) (GeminiTextPane.contentPercentage * 100f));

            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);

            slider.setPreferredSize(new Dimension(500, slider.getPreferredSize().height));

            JPanel tabPosPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            tabPosPanel.add(new JLabel(I18n.t("tabPosLabel")));
            String[] items = {I18n.t("tabPosTop"), I18n.t("tabPosBottom"), I18n.t("tabPosLeft"), I18n.t("tabPosRight")};
            JComboBox<String> tabPosCombo = new JComboBox<>(items);
            tabPosCombo.setEditable(false);
            tabPosCombo.setSelectedIndex(tabPosition);
            tabPosPanel.add(tabPosCombo);

            JCheckBox lineWrapCB = new JCheckBox(I18n.t("lineWrapCB"));
            lineWrapCB.setSelected(GeminiTextPane.wrapPF);

            JCheckBox imagePFCB = new JCheckBox(I18n.t("pfRenderCB"));
            imagePFCB.setSelected(GeminiTextPane.asciiImage);
            imagePFCB.addActionListener(al -> {
                lineWrapCB.setEnabled(!imagePFCB.isSelected());
            });

            JCheckBox showsbCB = new JCheckBox(I18n.t("scrollBarCB"));
            // showsbCB.setToolTipText("Display scrollbar when using scrollable pre-formatted text.");
            showsbCB.setSelected(GeminiTextPane.showSB);

            JCheckBox shadeCB = new JCheckBox(I18n.t("shadeCB"));
            shadeCB.setSelected(GeminiTextPane.shadePF);

            JCheckBox embedPFCB = new JCheckBox(I18n.t("scrollableCB"));
            // embedPFCB.setToolTipText("If enabled, hold down 's' to horizontally sroll pre-formatted text.\nDisabling (legacy) can affect where gemtext wraps.");
            embedPFCB.setSelected(GeminiTextPane.embedPF);
            embedPFCB.addActionListener(e -> {
                boolean selected = embedPFCB.isSelected();
                showsbCB.setEnabled(selected);
                shadeCB.setEnabled(selected);
            });

            showsbCB.setEnabled(embedPFCB.isSelected());
            shadeCB.setEnabled(embedPFCB.isSelected());

            Object[] comps = {new JLabel(I18n.t("contentWidthLabel")), slider, new JLabel(" "), tabPosPanel, lineWrapCB, imagePFCB, embedPFCB, showsbCB, shadeCB};
            Object res = Util.inputDialog2(GeminiFrame.this, "Layout", comps, null, false);

            if (res != null) {
                tabPosition = tabPosCombo.getSelectedIndex();
                DB.insertPref("tabpos", String.valueOf(tabPosition));
                GeminiTextPane.asciiImage = imagePFCB.isSelected();
                DB.insertPref("asciipf", String.valueOf(GeminiTextPane.asciiImage));
                GeminiTextPane.wrapPF = lineWrapCB.isSelected();
                DB.insertPref("linewrappf", String.valueOf(GeminiTextPane.wrapPF));
                GeminiTextPane.contentPercentage = (float) slider.getValue() / 100f;
                DB.insertPref("contentwidth", String.valueOf(slider.getValue()));
                GeminiTextPane.embedPF = embedPFCB.isSelected();
                DB.insertPref("embedpf", String.valueOf(GeminiTextPane.embedPF));
                GeminiTextPane.showSB = showsbCB.isSelected();
                DB.insertPref("showsb", String.valueOf(GeminiTextPane.showSB));
                GeminiTextPane.shadePF = shadeCB.isSelected();
                DB.insertPref("shadepf", String.valueOf(GeminiTextPane.shadePF));
                Alhena.updateFrames(false, false, false);
            }

        }));

        settingsMenu.add(createMenuItem(I18n.t("stylesItem"), KeyStroke.getKeyStroke(KeyEvent.VK_S, mod), () -> {
            String[] scopeItems = {I18n.t("scope1Label"), I18n.t("scope2Label"), I18n.t("scope3Label")};
            JComboBox<String> scopeCombo = new JComboBox(scopeItems);
            scopeCombo.setEditable(false);

            String[] themeItems = {I18n.t("styleTheme1"), I18n.t("styleTheme2"), I18n.t("styleTheme3"), I18n.t("styleTheme4")};
            JComboBox<String> themeCombo = new JComboBox(themeItems);

            themeCombo.setEditable(false);
            Object[] comps = {new JLabel(I18n.t("scopeText")),
                new JLabel(" "), new JLabel(I18n.t("styleScopeLabel")), scopeCombo, new JLabel(I18n.t("styleThemeLabel")), themeCombo};
            Object[] opts = {I18n.t("okLabel"), I18n.t("currentLabel"), I18n.t("cancelLabel")};
            Object res = Util.inputDialog2(this, I18n.t("stylesItem"), comps, opts, false);

            if (I18n.t("okLabel").equals(res)) {

                int idx = scopeCombo.getSelectedIndex();
                String scope = null, scopeValue = null, th = null;
                switch (idx) {
                    case 0 ->
                        scope = scopeValue = "GLOBAL";
                    case 1 -> {
                        scope = "DOMAIN";
                        scopeValue = visiblePage().textPane.getURI().getAuthority();
                        if (scopeValue == null) { // when saving style for certs or bookmarks but user picked domain
                            scope = "URL";
                            scopeValue = visiblePage().textPane.getDocURLString();
                        }

                    }
                    case 2 -> {
                        scope = "URL";
                        scopeValue = visiblePage().textPane.getDocURLString();
                    }
                    default -> {
                    }
                }
                idx = themeCombo.getSelectedIndex();
                switch (idx) {
                    case 0 ->
                        th = "ALL";
                    case 1 ->
                        th = "LIGHT";
                    case 2 ->
                        th = "DARK";
                    case 3 ->
                        th = Alhena.theme;
                }

                try {
                    String jstring = DB.getStyle(scope, scopeValue, th);
                    PageTheme pt;
                    PageTheme apt = new PageTheme();
                    if (jstring != null) {
                        pt = GeminiTextPane.getDefaultTheme();
                        JsonObject apJo = new JsonObject(jstring);
                        pt.fromJson(apJo); // merge in changes
                        apt.fromJson(apJo);
                    } else {
                        pt = GeminiTextPane.getDefaultTheme();
                    }
                    StylePicker sp = new StylePicker(pt, apt);
                    Object[] cmps = {sp};
                    JButton okButton = new JButton(I18n.t("okLabel"));
                    JButton delButton = new JButton(I18n.t("deleteLabel"));
                    JButton cancelButton = new JButton(I18n.t("cancelLabel"));

                    String fScope = scope;
                    String fScopeVal = scopeValue;
                    String fTheme = th;
                    BooleanSupplier okRunnable = () -> {
                        try {
                            DB.insertStyle(fScope, fScopeVal, fTheme, sp.getAlteredPageTheme().getJson(), null);
                            Alhena.updateFrames(false, false, false);
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        return true;

                    };

                    BooleanSupplier delRunnable = () -> {
                        Object r = Util.confirmDialog(GeminiFrame.this, I18n.t("styleDeleteDialog"), I18n.t("styleDeleteDialogTxt"), JOptionPane.YES_NO_OPTION, null, null);
                        if (r instanceof Integer rs && rs == JOptionPane.YES_OPTION) {
                            try {
                                DB.deleteStyle(fScope, fScopeVal, fTheme);
                                Alhena.updateFrames(false, false, false);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                            return true;
                        }
                        return false;
                    };

                    BooleanSupplier cancelRunnable = () -> {
                        return true;
                    };
                    Object[] options = {okButton, delButton, cancelButton};
                    BooleanSupplier[] suppliers = {okRunnable, delRunnable, cancelRunnable};
                    Util.inputDialog2(GeminiFrame.this, I18n.t("styleDialog"), cmps, options, false, suppliers);

                } catch (SQLException ex) {
                    ex.printStackTrace();
                }

            } else if (I18n.t("currentLabel").equals(res)) {
                Integer styleId = visiblePage().textPane.styleId;
                if (styleId != null) {
                    try {
                        String jstring = DB.getStyle(styleId);
                        PageTheme pt;
                        PageTheme apt = new PageTheme();
                        if (jstring != null) {
                            pt = visiblePage().textPane.getDefaultTheme();
                            JsonObject apJo = new JsonObject(jstring);
                            pt.fromJson(apJo); // merge in changes
                            apt.fromJson(apJo);
                        } else {
                            pt = visiblePage().textPane.getDefaultTheme();
                        }
                        StylePicker sp = new StylePicker(pt, apt);
                        Object[] cmps = {sp};
                        JButton okButton = new JButton(I18n.t("okLabel"));
                        JButton delButton = new JButton(I18n.t("deleteLabel"));
                        JButton cancelButton = new JButton(I18n.t("cancelLabel"));
                        BooleanSupplier okRunnable = () -> {
                            try {
                                DB.updateStyle(styleId, sp.getAlteredPageTheme().getJson());
                                Alhena.updateFrames(false, false, false);
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }
                            return true;

                        };

                        BooleanSupplier delRunnable = () -> {
                            Object r = Util.confirmDialog(GeminiFrame.this, I18n.t("styleDeleteDialog"), I18n.t("styleDeleteDialogTxt"), JOptionPane.YES_NO_OPTION, null, null);
                            if (r instanceof Integer rs && rs == JOptionPane.YES_OPTION) {
                                try {
                                    DB.deleteStyle(styleId);
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                                Alhena.updateFrames(false, false, false);
                                return true;
                            }
                            return false;
                        };

                        BooleanSupplier cancelRunnable = () -> {
                            return true;
                        };

                        Object[] options = {okButton, delButton, cancelButton};
                        BooleanSupplier[] suppliers = {okRunnable, delRunnable, cancelRunnable};
                        Util.inputDialog2(GeminiFrame.this, I18n.t("styleDialog"), cmps, options, false, suppliers);

                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                } else {
                    Util.infoDialog(GeminiFrame.this, I18n.t("noStyleDialog"), I18n.t("noStyleText"));
                }
            }

        }));
        settingsMenu.add(new JSeparator());
        JCheckBoxMenuItem smoothItem = new JCheckBoxMenuItem(I18n.t("smoothScrollingItem"), Alhena.smoothScrolling);
        smoothItem.addItemListener(ae -> {

            boolean smoothScrolling = !Alhena.smoothScrolling; // toggle
            forEachPage(page -> {
                if (smoothScrolling) {
                    page.textPane.setupAdaptiveScrolling();
                } else {
                    page.textPane.removeAdaptiveScrolling();
                }

            });
            DB.insertPref("smoothscrolling", String.valueOf(smoothScrolling));
            Alhena.smoothScrolling = smoothScrolling;

        });
        settingsMenu.add(smoothItem);

        JCheckBoxMenuItem vlcItem = new JCheckBoxMenuItem(I18n.t("vlcItem"), Alhena.allowVLC);
        vlcItem.addItemListener(ae -> {

            Alhena.allowVLC = !Alhena.allowVLC;

            DB.insertPref("allowvlc", String.valueOf(Alhena.allowVLC));

            if (Alhena.allowVLC) {
                Util.infoDialog(GeminiFrame.this, I18n.t("vlcUpdateDialog"), I18n.t("vlcUpdateDialogMsg"));
            }

        });

        JCheckBoxMenuItem favIconItem = new JCheckBoxMenuItem(I18n.t("favIconItem"), Alhena.favIcon);
        favIconItem.addItemListener(ae -> {

            Alhena.favIcon = !Alhena.favIcon;

            DB.insertPref("favicon", String.valueOf(Alhena.favIcon));

            visiblePage().textPane.repaint();

        });

        JCheckBoxMenuItem dataUrlItem = new JCheckBoxMenuItem(I18n.t("openDataUrlsItem"), Alhena.dataUrl);
        dataUrlItem.addItemListener(ae -> {

            Alhena.dataUrl = !Alhena.dataUrl;

            DB.insertPref("dataurl", String.valueOf(Alhena.dataUrl));

            Alhena.updateFrames(false, false, false);

        });

        JCheckBoxMenuItem linkIconItem = new JCheckBoxMenuItem(I18n.t("linkIconsItem"), Alhena.linkIcons);
        linkIconItem.addItemListener(ae -> {

            Alhena.linkIcons = !Alhena.linkIcons;

            DB.insertPref("linkicons", String.valueOf(Alhena.linkIcons));

            Alhena.updateFrames(false, false, false);

        });

        JCheckBoxMenuItem scrollSizeItem = new JCheckBoxMenuItem(I18n.t("bigScrollBarItem"), Alhena.bigScrollBar);
        scrollSizeItem.addItemListener(ae -> {

            Alhena.bigScrollBar = !Alhena.bigScrollBar;

            DB.insertPref("bigscrollbar", String.valueOf(Alhena.bigScrollBar));

            Alhena.updateFrames(false, false, false);

        });

        JCheckBoxMenuItem dragScrollItem = new JCheckBoxMenuItem(I18n.t("dragToScrollItem"), GeminiTextPane.dragToScroll);
        dragScrollItem.addItemListener(ae -> {

            GeminiTextPane.dragToScroll = !GeminiTextPane.dragToScroll;

            DB.insertPref("dragscroll", String.valueOf(GeminiTextPane.dragToScroll));

        });

        settingsMenu.add(vlcItem);
        settingsMenu.add(favIconItem);
        settingsMenu.add(dataUrlItem);
        settingsMenu.add(linkIconItem);
        settingsMenu.add(scrollSizeItem);
        settingsMenu.add(dragScrollItem);

        settingsMenu.add(new JSeparator());
        JMenuItem proxyItem = new JMenuItem(I18n.t("httpProxyItem"));
        proxyItem.addActionListener(ae -> {

            String proxy = Util.inputDialog(GeminiFrame.this, I18n.t("httpProxyDialog"), I18n.t("httpProxyDialogMsg"),
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

        JMenuItem gopherItem = new JMenuItem(I18n.t("gopherProxyItem"));
        gopherItem.addActionListener(ae -> {

            String proxy = Util.inputDialog(GeminiFrame.this, I18n.t("gopherProxyDialog"), I18n.t("gopherProxyDialogMsg"),
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

        settingsMenu.add(new JSeparator());
        JMenuItem searchItem = new JMenuItem(I18n.t("searchUrlItem"));
        searchItem.addActionListener(ae -> {

            String sUrl = Util.inputDialog(GeminiFrame.this, I18n.t("searchUrlDialog"), I18n.t("searchUrlDialogMsg"),
                    false, Alhena.searchUrl == null ? "" : Alhena.searchUrl, null);
            if (sUrl != null) {
                if (sUrl.isBlank()) {
                    Alhena.searchUrl = null;
                } else {
                    Alhena.searchUrl = sUrl;
                }
                DB.insertPref("searchurl", Alhena.searchUrl);
            }
        });
        settingsMenu.add(searchItem);

    }

    public void setTabPos(int pos) {
        if (tabbedPane != null) {
            int newPos = switch (pos) {
                case 0 ->
                    JTabbedPane.TOP;
                case 1 ->
                    JTabbedPane.BOTTOM;
                case 2 ->
                    JTabbedPane.LEFT;
                default ->
                    JTabbedPane.RIGHT;
            };
            tabbedPane.setTabPlacement(newPos);
        }
    }

    public void editPage() {
        Page vp = visiblePage();
        URI uri = vp.textPane.getURI();
        if (uri.getScheme() != null && uri.getScheme().equals("gemini")) {
            String port = uri.getPort() != -1 ? ":" + uri.getPort() : "";
            String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
            String editUrl = "titan://" + uri.getHost() + port + uri.getPath() + ";edit" + query;
            fetchURL(editUrl, false);
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
        String fs = DB.getPref("fontsize", String.valueOf(DEFAULT_FONT_SIZE));
        fontSize = Integer.parseInt(fs);
        String mfs = DB.getPref("monofontsize", String.valueOf(DEFAULT_FONT_SIZE));
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

        boolean savedMacNotoPref = Alhena.macUseNoto;
        if (!setName.equals(savedSet) || macUseNoto != savedMacNotoPref) {

            if (setName.equals("font")) {

                // use font
                GeminiTextPane.setSheetImage(null);
                DB.insertPref("emoji", setName);
                DB.insertPref("macusenoto", String.valueOf(macUseNoto));
                Alhena.macUseNoto = macUseNoto;
                Alhena.updateFrames(false, false, false);
                lastSelectedItem = selected;
            } else if (macUseNoto != savedMacNotoPref && setName.equals(savedSet)) {

                Alhena.updateFrames(false, false, false);
            } else {
                String url = emojiNameMap.get(setName);

                if (setName.equals("google")) {
                    GeminiTextPane.setSheetImage(Util.loadImage(url));
                    DB.insertPref("emoji", "google");
                    Alhena.updateFrames(false, false, false);
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
                            Alhena.updateFrames(false, false, false);
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
                        Alhena.updateFrames(false, false, false);
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
                    Util.infoDialog(GeminiFrame.this, I18n.t("spriteErrorDialog"), I18n.t("spriteErrorDialogMsg"), JOptionPane.ERROR_MESSAGE);
                });

            }

        }).start();
    }

    public void initComboBox() {
        JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();

        textField.addMouseListener(new ContextMenuMouseListener());
        textField.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> {
                    textField.selectAll();
                });
            }
        });
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
            Alhena.processURL(url, page, null, page, false);
        }
    }

    private void bookmarkPage(boolean newBookmark) {

        if (!newBookmark && visiblePage().textPane.getDocMode() == GeminiTextPane.INFO_MODE) {
            Util.infoDialog(this, I18n.t("invalidBookmarkDialog"), I18n.t("invalidBookmarkDialogMsg"));
            return;
        }

        JTextField labelField = new JTextField();
        labelField.addMouseListener(new ContextMenuMouseListener());
        JTextField urlField = new JTextField();
        urlField.addMouseListener(new ContextMenuMouseListener());
        urlField.setPreferredSize(new Dimension(400, urlField.getPreferredSize().height));
        if (!newBookmark) {
            String subject = visiblePage().textPane.getFirstHeading();

            if (subject != null) {
                labelField.setText(subject);
            }
            urlField.setText(visiblePage().getUrl());
            urlField.setCaretPosition(0);
        }

        JComboBox bmComboBox = new JComboBox();
        List<String> folders;
        try {
            folders = DB.bookmarkFolders();
        } catch (SQLException ex) {
            folders = new ArrayList<>();
        }

        if (!folders.contains("ROOT")) {
            folders.addFirst("ROOT");
        }
        for (String folder : folders) {
            bmComboBox.addItem(folder);
        }

        bmComboBox.setEditable(true);
        bmComboBox.getEditor().getEditorComponent().addMouseListener(new ContextMenuMouseListener());

        Object[] comps = new Object[6];
        comps[0] = I18n.t("bookmarkDialogLabel");
        comps[1] = labelField;
        comps[2] = I18n.t("bookmarkDialogUrl");
        comps[3] = urlField;
        comps[4] = I18n.t("bookmarkDialogFolder");
        comps[5] = bmComboBox;
        Object res = Util.inputDialog2(this, I18n.t("bookmarkDialog"), comps, null, false);
        if (res != null) {

            if (labelField.getText().trim().isEmpty() || ((String) bmComboBox.getSelectedItem()).trim().isEmpty() || urlField.getText().trim().isEmpty()) {
                Util.infoDialog(this, I18n.t("bookmarkRequiredDialog"), I18n.t("bookmarkRequiredDialogMsg"));
            } else {
                try {
                    DB.insertBookmark(labelField.getText().trim(), urlField.getText().trim(), ((String) bmComboBox.getSelectedItem()).trim(), null);
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
                bookmarkMenu = new JMenu(I18n.t("bookmarksMenu"));
                bookmarkMenu.setMnemonic('B');
                menuBar.add(bookmarkMenu);
            }
            bookmarkMenu.add(createMenuItem(I18n.t("newBookmarkItem"), KeyStroke.getKeyStroke(KeyEvent.VK_W, mod), () -> {
                bookmarkPage(true);
            }));
            bookmarkMenu.add(createMenuItem(I18n.t("bookmarkPageItem"), KeyStroke.getKeyStroke(KeyEvent.VK_D, mod), () -> {
                bookmarkPage(false);
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
                    r = () -> fetchURL(bm.url(), false);
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

    public void fetchURL(String url, boolean searchInput) {
        fetchURL(url, null, searchInput);
    }

    public void fetchURL(String url, File dataFile, boolean searchInput) {
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

                        if (tabbedPane != null) { // ugh - tab added in interim
                            int idx = tabbedPane.getSelectedIndex();

                            if (tabbedPane.getComponentAt(idx) == currentPB) { // make sure we're on the same tab
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
                                    tabbedPane.setComponentAt(idx, histPage);
                                } else {
                                    histPage.runWhenDone(() -> currentPB.setBusy(false));
                                }
                            }
                            return;
                        }
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
                Alhena.processURL(url, pb, null, currentPB, searchInput);

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
                Alhena.processURL(url, pb, null, currentPB, searchInput);
            }
        } else {
            Page nPage = addPageToHistory(null, currentPB, true);
            nPage.setDataFile(dataFile);
            nPage.runWhenLoading(() -> nPage.setDataFile(null));
            Alhena.processURL(url, nPage, null, currentPB, searchInput);

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
                        Alhena.processURL(cURL, visiblePage, null, visiblePage, false);
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
                pb.ignoreStart();
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
                if (label.equals(HISTORY_LABEL)) {
                    loadHistory(pb.textPane, pb);
                } else if (label.equals(BOOKMARK_LABEL)) {
                    loadBookmarks(pb.textPane, pb);
                } else if (label.equals(CERT_LABEL)) {
                    loadCerts(pb.textPane, pb);
                } else if (label.equals(INFO_LABEL)) {
                    loadInfo(pb.textPane, info);
                } else if (label.equals(SERVERS_LABEL)) {
                    loadServers(pb.textPane, pb);
                }

            } else {
                int currentTabIdx = tabbedPane.getSelectedIndex();

                Page pb = newPage(label, null, false);
                pb.ignoreStart();
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

                if (label.equals(HISTORY_LABEL)) {
                    loadHistory(pb.textPane, pb);
                } else if (label.equals(BOOKMARK_LABEL)) {
                    loadBookmarks(pb.textPane, pb);
                } else if (label.equals(CERT_LABEL)) {
                    loadCerts(pb.textPane, pb);
                } else if (label.equals(INFO_LABEL)) {
                    loadInfo(pb.textPane, info);
                } else if (label.equals(SERVERS_LABEL)) {
                    loadServers(pb.textPane, pb);
                }

            }
        } else {
            Page visiblePB = visiblePage(); // empty page (new tab/window)
            visiblePB.ignoreStart();
            Page nPage = !inPlace ? addPageToHistory(null, visiblePB, true) : visiblePB;

            if (label.equals(HISTORY_LABEL)) {
                loadHistory(nPage.textPane, null);
            } else if (label.equals(BOOKMARK_LABEL)) {
                loadBookmarks(nPage.textPane, null);
            } else if (label.equals(CERT_LABEL)) {
                loadCerts(nPage.textPane, null);
            } else if (label.equals(INFO_LABEL)) {
                loadInfo(nPage.textPane, info);
            } else if (label.equals(SERVERS_LABEL)) {
                loadServers(nPage.textPane, null);
            }
            setTitle(label);

        }

    }

    public String createTitle(String url, String firstHeading) {
        String shortTitle = DB.getUrlLabel(url);
        if (shortTitle == null) {

            shortTitle = firstHeading;

        }
        if (shortTitle == null || shortTitle.isBlank()) {
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
                textPane.updatePage(I18n.t("bookmarksHeading"), false, BOOKMARK_LABEL, true);

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
                textPane.end(I18n.t("emptyBookmarksHeading"), false, BOOKMARK_LABEL, true);
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
                textPane.updatePage(I18n.t("clientCertsHeading"), false, CERT_LABEL, true);

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
                            String label = active ? I18n.t("clientCertsActiveLabel") : I18n.t("clientCertsInactiveLabel");
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
                textPane.end(I18n.t("emptyClientCertsHeading"), false, CERT_LABEL, true);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    private void loadInfo(GeminiTextPane textPane, InfoPageInfo info) {

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), info.title);
        }

        textPane.end(info.content, true, INFO_LABEL, false);
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

        textPane.updatePage(I18n.t("historyHeading"), false, HISTORY_LABEL, true);
        new Thread(() -> {
            int[] count = {0};
            try {
                count[0] = DB.loadHistory(textPane);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            EventQueue.invokeLater(() -> {
                if (count[0] == 0) {
                    textPane.addPage(I18n.t("nothingToSeeLabel"));
                } else {
                    setTmpStatus(count[0] + " links");
                }

                if (p != null) {

                    p.loading();
                }
                textPane.end();
            });

        }).start();
    }

    public void setTmpStatus(String msg) {
        Runnable r = () -> {

            setStatus(msg);
            Timer timer = new Timer(5000, event -> {
                if (statusField.getText().equals(msg)) {
                    setStatus(" ");
                }
            });
            timer.setRepeats(false);
            timer.start();
        };
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(r);
        } else {
            r.run();
        }
    }

    private void loadServers(GeminiTextPane textPane, Page p) {
        setBusy(true, p);

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)

            tabbedPane.setTitleAt(tabbedPane.getSelectedIndex(), SERVERS_LABEL);
        }

        textPane.updatePage(I18n.t("serversHeading"), false, SERVERS_LABEL, true);
        new Thread(() -> {
            int[] count = {0};
            try {
                count[0] = DB.loadServers(textPane);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            EventQueue.invokeLater(() -> {
                if (count[0] == 0) {
                    textPane.addPage(I18n.t("nothingToSeeLabel"));
                } else {
                    setTmpStatus(count[0] + " servers");
                }

                if (p != null) {

                    p.loading();
                }
                textPane.end();
            });

        }).start();
    }

    public void deleteBookmark(Component f, String id) {
        int bmId = Integer.parseInt(id);
        try {
            Bookmark bm = DB.getBookmark(bmId);
            Object res = Util.confirmDialog(f, I18n.t("deleteBookmarkDialog"), I18n.t("deleteBookmarkDialogMsg") + bm.label(), JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    DB.deleteBookmark(bmId);
                    Alhena.updateFrames(true, false, false);
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
            Object res = Util.confirmDialog(this, I18n.t("deleteCertDialog"), I18n.t("deleteCertDialogMsg") + certInfo.domain() + " [" + principal.getName() + "]", JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
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
            match = Util.inputDialog(this, I18n.t("deleteHistoryDialog"), I18n.t("deleteHistoryDialogMsg"), false);
            if (match == null || match.isBlank()) {
                return;

            } else {
                String message = MessageFormat.format(I18n.t("deleteHistoryConfirmDialogMsg"), match);
                Object res = Util.confirmDialog(this, I18n.t("deleteHistoryConfirmDialog"), message, JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
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
            String verbiage = rowCount == 1 ? I18n.t("linkLabel") + " " : " " + rowCount + " " + I18n.t("linksLabel") + " ";
            Util.infoDialog(this, I18n.t("linksDeleteDialog"), verbiage + I18n.t("linksDeleteDialogMsg"));

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void clearHistory() {
        try {
            Object res = Util.confirmDialog(this, I18n.t("clearHistoryDialog"), I18n.t("clearHistoryDialogMsg"), JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    int rowCount = DB.deleteHistory();
                    refresh();
                    String verbiage = rowCount == 0 ? I18n.t("noLinksLabel") + " " : rowCount == 1 ? I18n.t("linkLabel") + " " : rowCount + " " + I18n.t("linksLabel") + " ";
                    Util.infoDialog(this, I18n.t("clearHistoryResultDialog"), verbiage + I18n.t("clearHistoryResultDialogMsg"));

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

            labelField.addMouseListener(new ContextMenuMouseListener());
            labelField.setText(bm.label());

            JTextField urlField = new JTextField();
            urlField.addMouseListener(new ContextMenuMouseListener());
            urlField.setPreferredSize(new Dimension(400, urlField.getPreferredSize().height));
            urlField.setText(bm.url());
            urlField.setCaretPosition(0);

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

            Object[] comps = new Object[6];
            comps[0] = I18n.t("bookmarkDialogLabel");
            comps[1] = labelField;
            comps[2] = I18n.t("bookmarkDialogUrl");
            comps[3] = urlField;
            comps[4] = I18n.t("bookmarkDialogFolder");
            comps[5] = bmComboBox;
            Object res = Util.inputDialog2(this, I18n.t("newLabel"), comps, null, false);
            if (res != null) {
                if (!labelField.getText().trim().isEmpty() && !((String) bmComboBox.getSelectedItem()).trim().isEmpty() && !urlField.getText().trim().isEmpty()) {
                    DB.updateBookmark(bmId, labelField.getText(), (String) bmComboBox.getSelectedItem(), urlField.getText());
                }
                refresh();
                Alhena.updateFrames(true, false, false);
                //EventQueue.invokeLater(() -> refresh());
            }

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

                Alhena.closeNetClient(DB.getClientCertInfo(prunedUrl)); //lazy
                String message = MessageFormat.format(I18n.t("createCertDialogMsg"), uri);
                Util.infoDialog(this, I18n.t("createCertDialog"), message);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            refresh();
        }
    }

    public void importPem(URI uri, File f) {
        String host = uri.getHost();
        if (host == null || !uri.getScheme().equals("gemini")) {
            Util.infoDialog(this, I18n.t("invalidLabel"), I18n.t("invalidSchemeDialogMsg"));
            return;
        }
        if (f == null) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PEM files (*.pem)", "pem");
            f = Util.getFile(this, null, true, I18n.t("selectPEMDialog"), filter);
        }

        if (f != null) {
            try {
                PemData pemData = Util.extractPemParts(f.getAbsolutePath());
                if (pemData.cert() == null || pemData.key() == null) {
                    Util.infoDialog(this, I18n.t("pemFormatDialog"), I18n.t("pemFormatDialogMsg"));
                } else {
                    int port = uri.getPort() == -1 ? 1965 : uri.getPort();
                    String prunedUrl = uri.getHost() + ":" + port + uri.getPath();

                    boolean exists = DB.loadCerts().stream().anyMatch(c -> c.cert().equals(pemData.cert()) && c.domain().equals(prunedUrl));
                    if (exists) {
                        Util.infoDialog(this, I18n.t("duplicatePEMDialog"), I18n.t("duplicatePEMDialogMsg") + " " + uri, JOptionPane.WARNING_MESSAGE);
                    } else {
                        ClientCertInfo ci = DB.getClientCertInfo(prunedUrl);
                        if (ci != null) {
                            DB.toggleCert(ci.id(), false, prunedUrl, false); // set existing for host to inactive

                        }

                        DB.insertClientCert(prunedUrl, pemData.cert(), pemData.key(), true, null);
                        Alhena.addCertToTrustStore(uri, visiblePage().getCert(), false);

                        Alhena.closeNetClient(DB.getClientCertInfo(prunedUrl)); //lazy
                        String message = MessageFormat.format(I18n.t("pemAddedDialogMsg"), uri);
                        Util.infoDialog(this, I18n.t("pemAddedDialog"), message);
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
            file = Util.getFile(this, null, true, I18n.t("selectFileDialog"), null);
        }

        if (file != null && file.exists()) {
            try {
                URI fileUri = file.toURI();
                fetchURL(fileUri.toString(), false);

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
        File saveFile = Util.getFile(this, suggestedName, false, I18n.t("saveFileDialog"), filter);
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
        X509Certificate pageCert = page.get().getCert();
        if (pageCert == null) {
            Util.infoDialog(GeminiFrame.this, I18n.t("noCertDialog"), I18n.t("noCertDialogMsg"));
        } else {
            InfoPageInfo pageInfo = new InfoPageInfo("servercert: " + host, pageCert.toString());
            showCustomPage(INFO_LABEL, pageInfo);
        }

    }

    private void removeRootPageAudioPlayers(Page page) {
        ArrayList<Page> hPageList = pageHistoryMap.get(page);
        for (Page p : hPageList) {
            p.textPane.closePlayers();
        }
        page.textPane.closePlayers();
    }

    public void newTab(String url) {

        if (tabbedPane == null) {
            invalidate();
            tabbedPane = new JTabbedPane();
            int newPos = switch (tabPosition) {
                case 0 ->
                    JTabbedPane.TOP;
                case 1 ->
                    JTabbedPane.BOTTOM;
                case 2 ->
                    JTabbedPane.LEFT;
                default ->
                    JTabbedPane.RIGHT;
            };
            tabbedPane.setTabPlacement(newPos);

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
                            removeRootPageAudioPlayers(getRootPage(page));
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
                                selectComboBoxItem(page.textPane.getDocURLString());
                            } else {
                                setTitle(I18n.t("newTabLabel"));
                                selectComboBoxItem("");
                            }

                            Page rootPage = getRootPage(page);
                            if (rootPage != null) { // NOT 100% ON THIS
                                backButton.setEnabled(hasPrev(rootPage));
                                forwardButton.setEnabled(hasNext(rootPage));
                            } else {
                                backButton.setEnabled(false);
                                forwardButton.setEnabled(false);
                            }
                            GeminiFrame.this.validate();
                        } else {
                            Page page = (Page) tabbedPane.getComponentAt(tabIndex);
                            removeRootPageAudioPlayers(getRootPage(page));
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
                if (page == null) {
                    return;
                }
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

        Alhena.processURL(url, pb, null, currentPage, false);

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
            ActionListener[] il = comboBox.getActionListeners();

            for (ActionListener i : il) {
                comboBox.removeActionListener(i);
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

            for (ActionListener j : il) {
                comboBox.addActionListener(j);
            }
        });
    }

    public void selectComboBoxItem(String s) {
        selectComboBoxItem(new ComboItem(s, null));
    }

    private void selectComboBoxItem(ComboItem item) {
        ActionListener[] il = comboBox.getActionListeners();
        for (ActionListener i : il) {
            comboBox.removeActionListener(i);
        }
        comboBox.setSelectedItem(item);

        for (ActionListener i : il) {
            comboBox.addActionListener(i);
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

    public void focusOnAddressBar() {
        comboBox.requestFocusInWindow();
    }
}
