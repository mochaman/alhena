package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.security.auth.x500.X500Principal;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;

import brad.grier.alhena.DB.Bookmark;
import brad.grier.alhena.DB.ClientCertInfo;
import brad.grier.alhena.DB.DBClientCertInfo;
import brad.grier.alhena.GeminiTextPane.CurrentPage;

/**
 * Alhena frame
 *
 * @author Brad Grier
 */
public final class GeminiFrame extends JFrame {

    private JComboBox comboBox;
    private final JLabel statusField;
    public JTabbedPane tabbedPane;
    private final JButton backButton;
    private final JButton forwardButton;
    private final JButton favButton;
    private final JButton refreshButton;
    public static int currentThemeId;
    private final HashMap<Page, ArrayList<Page>> pageHistoryMap = new HashMap<>();
    private final List<String> clickedLinks = new ArrayList<>();
    private final JMenuBar menuBar;
    private JMenu bookmarkMenu;
    private String lastSearch;
    private JLabel titleLabel;
    public static final String HISTORY_LABEL = "History";
    public static final String BOOKMARK_LABEL = "Bookmarks";
    public static final String CERT_LABEL = "Certs";
    public static final String INFO_LABEL = "Info";
    public static final List<String> CUSTOM_LABELS = List.of(HISTORY_LABEL, BOOKMARK_LABEL, CERT_LABEL, INFO_LABEL); // make immutable
    public String proportionalFamily = "SansSerif";
    public int fontSize = 15;
    private Font saveFont;

    private Map<String, ThemeInfo> themes = Map.ofEntries(
            Map.entry("FlatCyanLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCyanLightIJTheme", false)),
            Map.entry("FlatLightFlatIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatLightFlatIJTheme", false)),
            Map.entry("FlatHighContrastIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatHighContrastIJTheme", true)),
            Map.entry("FlatCarbonIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCarbonIJTheme", true)),
            Map.entry("FlatCobalt2IJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatCobalt2IJTheme", true)),
            Map.entry("FlatGradiantoDarkFuchsiaIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoDarkFuchsiaIJTheme", true)),
            Map.entry("FlatGradiantoDeepOceanIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatGradiantoDeepOceanIJTheme", true)),
            Map.entry("FlatArcOrangeIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcOrangeIJTheme", false)),
            Map.entry("FlatArcDarkOrangeIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatArcDarkOrangeIJTheme", true)),
            Map.entry("FlatLightLaf", new ThemeInfo("com.formdev.flatlaf.FlatLightLaf", false)),
            Map.entry("FlatDarkLaf", new ThemeInfo("com.formdev.flatlaf.FlatDarkLaf", true)),
            Map.entry("FlatSolarizedDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSolarizedDarkIJTheme", true)),
            Map.entry("FlatDraculaIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatDraculaIJTheme", true)),
            Map.entry("FlatXcodeDarkIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatXcodeDarkIJTheme", true)),
            Map.entry("FlatAtomOneLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme", false)),
            Map.entry("FlatSpacegrayIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSpacegrayIJTheme", true)),
            Map.entry("FlatSolarizedLightIJTheme", new ThemeInfo("com.formdev.flatlaf.intellijthemes.FlatSolarizedLightIJTheme", false))
    );

    public boolean isClickedLink(String link) {
        return clickedLinks.contains(link);
    }

    public void addClickedLink(String link) {
        clickedLinks.add(link);
    }

    private Page addPageToHistory(Page rootPage, Page page) {

        if (rootPage == null) {

            page.setRootPage(Page.ROOT_PAGE);
            ArrayList<Page> histList = new ArrayList<>();
            histList.add(page);

            pageHistoryMap.put(page, histList);
        } else {

            page.setRootPage(rootPage);

            ArrayList<Page> histList = pageHistoryMap.get(rootPage);

            int histIdx = rootPage.incAndGetArrayIndex(); // should get same result if calling on page.
            if (histIdx < histList.size()) {
                List<Page> sl = histList.subList(histIdx, histList.size());
                sl.clear();
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

        //return rootPage == null ? false : pageHistIndexMap.get(rootPage).get() > 0;
        return rootPage == null ? false : rootPage.getArrayIndex() > 0;

    }

    private Page getRootPage(Page pb) {
        // this only makes sense if you realize pb.getRootPage() can be null
        return pb.getRootPage() == Page.ROOT_PAGE ? pb : pb.getRootPage(); // returning null means this page has no history (new window/tab)
    }

    public GeminiFrame(String url, String baseUrl, String themeName) {

        URL iconUrl = GeminiClient.class.getClassLoader().getResource("alhena_32x32.png");
        Image ii = new ImageIcon(iconUrl).getImage();
        setIconImage(ii);

        if (SystemInfo.isMacFullWindowContentSupported) {
            getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            //getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        proportionalFamily = DB.getPref("fontfamily");
        proportionalFamily = proportionalFamily == null ? "SansSerif" : proportionalFamily;
        String fs = DB.getPref("fontsize");
        fontSize = fs == null ? 15 : Integer.parseInt(fs);
        String dbFont = DB.getPref("font");
        dbFont = dbFont == null ? "SansSerif" : dbFont;
        saveFont = new Font(dbFont, Font.PLAIN, fontSize);

        // test to see if db font exists - maybe deleted from system or db moved to another os
        // fonts created with invalid names are created anyway with Dialog font family
        if (saveFont.getFamily().equals("Dialog") && !saveFont.getName().equals("Dialog")) {

            saveFont = new Font("SansSerif", Font.PLAIN, 15);
            proportionalFamily = "SansSerif";
            fontSize = 15;
        }

        boolean addToHistory = url != null || baseUrl != null;
        Page pb = newPage(baseUrl, null, addToHistory);

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evt) {

                GeminiClient.exit(GeminiFrame.this);

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

        JTextField textField = (JTextField) comboBox.getEditor().getEditorComponent();
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                SwingUtilities.invokeLater(() -> prefill(textField, e.getKeyChar()));
            }
        });
        textField.addMouseListener(new ContextMenuMouseListener());
        Font buttonFont = new Font("Noto Emoji Regular", Font.PLAIN, 18);
        backButton = new JButton("⬅"); //emoji_u303d.png
        //backButton = new JButton("◀️");
        //backButton = new JButton(Util.loadPNGIcon("/png/emoji_u2b05.png", 18, 18)); //emoji_u303d.png
        backButton.setToolTipText("Click to go back");
        forwardButton = new JButton("➡");
        //forwardButton = new JButton("▶️");
        forwardButton.setToolTipText("Click to go forward");
        backButton.setFont(buttonFont);

        backButton.setEnabled(false);
        backButton.addActionListener(al -> {

            showGlassPane(true);
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

            showGlassPane(false);

        });

        forwardButton.setFont(buttonFont);
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(al -> {
            showGlassPane(true);
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

            showGlassPane(false);

        });

        refreshButton = new JButton("🔄");
        refreshButton.setToolTipText("Reload this page");
        refreshButton.setEnabled(false);
        refreshButton.setFont(buttonFont);
        refreshButton.addActionListener(al -> {

            refresh();

        });

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
        //eastPanel.add(homeButton);
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
        //add(jComboBox1, gridBagConstraints);
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

        //add(pb.scrollPane, BorderLayout.CENTER);
        add(pb, BorderLayout.CENTER);
        statusField = new JLabel(GeminiClient.WELCOME_MESSAGE);

        statusField.setBorder(new EmptyBorder(5, 5, 5, 5)); // Add padding

        menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("File");
        //fileMenu.setBorder(new EmptyBorder(0, 0, 0, 10));
        fileMenu.add(createMenuItem("Open File", null, () -> {
            openFile(null);
        }));
        fileMenu.add(new JSeparator());
        int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
        fileMenu.add(createMenuItem("New Tab", KeyStroke.getKeyStroke(KeyEvent.VK_T, mod), () -> {

            newTab(null);

        }));

        fileMenu.add(createMenuItem("New Window", KeyStroke.getKeyStroke(KeyEvent.VK_N, mod), () -> {
            GeminiClient.newWindow(null, null);
        }));

        if (!SystemInfo.isMacOS) {
            fileMenu.add(new JSeparator());
            fileMenu.add(createMenuItem("Quit", KeyStroke.getKeyStroke(KeyEvent.VK_Q, mod), () -> {
                GeminiClient.exit(GeminiFrame.this);
            }));
        }
        fileMenu.setMnemonic('F');
        menuBar.add(fileMenu);

        JMenu viewMenu = new JMenu("View");

        for (String label : CUSTOM_LABELS) {
            KeyStroke ks = null;
            if (!label.equals(INFO_LABEL)) {
                switch (label) {
                    case HISTORY_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_Y, mod);
                    case BOOKMARK_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_B, mod);
                    case CERT_LABEL ->
                        ks = KeyStroke.getKeyStroke(KeyEvent.VK_C, (mod | KeyEvent.SHIFT_DOWN_MASK));
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
            String input = Util.inputDialog(this, "Find In Page", "Enter search term", false, lastSearch);
            if (input != null) {
                lastSearch = input;
                visiblePage().textPane.find(input);
            }
        }));

        menuBar.add(viewMenu);

        addBookmarks();

        JMenu windowsMenu = new JMenu("Windows");

        JMenu darkThemeMenu = new JMenu("Dark Themes");

        JMenu lightThemeMenu = new JMenu("Light Themes");

        themes.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String key = entry.getKey();
                    ThemeInfo value = entry.getValue();

                    JMenu jm = value.isDark() ? darkThemeMenu : lightThemeMenu;

                    jm.add(createMenuItem(key, null, () -> {
                        if (!key.equals(currentThemeId))
                        try {
                            Class<?> themeClass = Class.forName(value.className());
                            FlatLaf theme = (FlatLaf) themeClass.getDeclaredConstructor().newInstance();
                            FlatLaf.setup(theme);

                            currentThemeId++;
                            GeminiClient.updateFrames();

                            refreshFromCache(visiblePage());
                            visiblePage().setThemeId(currentThemeId);
                            DB.insertPref("theme", value.className());
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }));

                });
        windowsMenu.add(lightThemeMenu);
        windowsMenu.add(darkThemeMenu);

        windowsMenu.add(createMenuItem("Font", KeyStroke.getKeyStroke(KeyEvent.VK_F, (mod | KeyEvent.SHIFT_DOWN_MASK)), () -> {
            Font defFont = saveFont != null ? saveFont : new Font("SansSerif", Font.PLAIN, 15);
            Font font = Util.getFont(GeminiFrame.this, defFont);
            if (font != null) {
                saveFont = font;
                // System.out.println(font.getFontName() + " " + font.getFamily());
                proportionalFamily = font.getFamily();
                fontSize = font.getSize();
                currentThemeId++;
                GeminiClient.updateFrames();
                refreshFromCache(visiblePage());

                visiblePage().setThemeId(currentThemeId);
                DB.insertPref("font", font.getName());
                DB.insertPref("fontfamily", proportionalFamily);
                DB.insertPref("fontsize", String.valueOf(fontSize));
            }

        }));

        menuBar.add(windowsMenu);

        JMenu aboutMenu = new JMenu("Help");
        aboutMenu.setMnemonic('H');
        if (!SystemInfo.isMacOS) {
            aboutMenu.add(createMenuItem("About", null, () -> {
                JOptionPane.showMessageDialog(this, GeminiClient.PROG_NAME + " " + GeminiClient.VERSION + "\nWritten by Brad Grier");

            }));
        }
        aboutMenu.add(createMenuItem("Home", null, () -> {

            String homeDir = System.getProperty("alhena.home");
            File file = Util.copyFromJar(homeDir);
            URI fileUri = file.toURI();

            //GeminiClient.processURL(fileUri.toString(), visiblePage(), null, visiblePage());
            fetchURL(fileUri.toString());

        }));

        aboutMenu.add(createMenuItem("Changes", null, () -> {
            fetchURL("gemini://ultimatumlabs.com/alhena_changes.gmi");
            //GeminiClient.processURL("gemini://ultimatumlabs.com/alhena_changes.gmi", visiblePage(), null, visiblePage());
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

    private void prefill(JTextField textField, char typedChar) {

        String text = textField.getText();

        if (typedChar == KeyEvent.VK_BACK_SPACE) {
            return;
        }

        // If user starts typing "g", prefill with "gemini://"
        if (text.equalsIgnoreCase("g")) {
            textField.setText("gemini://");
            textField.setCaretPosition(9);
        } else if (text.equalsIgnoreCase("h")) {
            textField.setText("https://");
            textField.setCaretPosition(8);
        } else if (text.equalsIgnoreCase("f")) {
            textField.setText("file://");
            textField.setCaretPosition(7);
        }
    }

    // don't leak this from your constructor says IDE
    private void init(String url, Page page) {
        if (CUSTOM_LABELS.contains(url) && !url.equals(INFO_LABEL)) {
            showCustomPage(url, true, null);
        } else {
            GeminiClient.processURL(url, page, null, page);
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
                    DB.insertBookmark(labelField.getText().trim(), visiblePage().getUrl(), ((String) bmComboBox.getSelectedItem()).trim());
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
            //if (!bookmarks.isEmpty()) {
            if (bookmarkMenu != null) {
                bookmarkMenu.removeAll();
            } else {
                bookmarkMenu = new JMenu("Bookmarks");
                menuBar.add(bookmarkMenu);
            }

            bookmarkMenu.add(createMenuItem("Bookmark Page", null, () -> {
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
                        if (currentPB == visiblePage()) {
                            Page histPage = addPageToHistory(getRootPage(currentPB), pb);

                            invalidate();

                            remove(currentPB);

                            add(histPage, BorderLayout.CENTER);

                            revalidate();
                            refreshNav(histPage);
                        }

                    }

                };
                pb.runOnLoad(r);
                GeminiClient.processURL(url, pb, null, currentPB);

            } else {
                int currentTabIdx = tabbedPane.getSelectedIndex();

                Page pb = newPage(currentPB.textPane.getDocURLString(), null, false);

                boolean[] first = {true};
                Runnable r = () -> {
                    if (first[0]) {
                        first[0] = false;
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb);
                        // check to make sure user hasn't changed things in the interim                        

                        int idx = tabbedPane.getSelectedIndex();

                        if (currentTabIdx == idx) { // make sure we're on the same tab
                            tabbedPane.setComponentAt(idx, histPage);

                            refreshNav(histPage);
                        } else {
                            tabbedPane.setComponentAt(currentTabIdx, histPage);

                        }

                    }
                };
                pb.runOnLoad(r);

                GeminiClient.processURL(url, pb, null, currentPB);
            }
        } else {
            Page nPage = addPageToHistory(null, currentPB);
            GeminiClient.processURL(url, nPage, null, currentPB);

        }
    }

    public record ThemeInfo(String className, boolean isDark) {

    }

    private void refresh() {
        visiblePage().textPane.getDocURL().ifPresent(cURL -> {

            switch (visiblePage().textPane.getDocMode()) {
                case GeminiTextPane.HISTORY_MODE ->
                    loadHistory(visiblePage().textPane, visiblePage());
                case GeminiTextPane.BOOKMARK_MODE ->
                    loadBookmarks(visiblePage().textPane, visiblePage());
                case GeminiTextPane.CERT_MODE ->
                    loadCerts(visiblePage().textPane, visiblePage());
                case GeminiTextPane.INFO_MODE -> {
                }
                case GeminiTextPane.DEFAULT_MODE -> {
                    if (!cURL.isEmpty()) {
                        GeminiClient.processURL(cURL, visiblePage(), null, visiblePage());
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
        showGlassPane(true);
        String url = pb.textPane.getDocURLString();
        CurrentPage res = pb.textPane.current();
        streamChunks(res.currentPage(), 100, url, res.pMode());
    }

    public void streamChunks(StringBuilder b, int chunkSize, String url, boolean pfMode) {
        if (b == null || b.isEmpty()) {
            showGlassPane(false);
            return;
        }
        new Thread(() -> {
            int idx = 0;
            int endIdx = Math.min(chunkSize + idx, b.length());
            final int fIdx = idx;
            final int fEndIdx = endIdx;
            EventQueue.invokeLater(() -> {

                visiblePage().textPane.updatePage(b.substring(fIdx, fEndIdx), pfMode, url, false);

            });
            idx = endIdx;
            while (endIdx < b.length()) {
                // there's more
                endIdx = Math.min(chunkSize + idx, b.length());
                final int fIdx2 = idx;
                final int fEndIdx2 = endIdx;
                EventQueue.invokeLater(() -> {

                    visiblePage().textPane.addPage(b.substring(fIdx2, fEndIdx2));

                });
                idx = endIdx;
            }

            EventQueue.invokeLater(() -> {
                Page visiblePane = visiblePage();
                Page groupPane = getRootPage(visiblePane);
                visiblePane.textPane.end();
                backButton.setEnabled(hasPrev(groupPane));
                forwardButton.setEnabled(hasNext(groupPane));
                showGlassPane(false);
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
                        if (currentPB == visiblePage()) { // in case something has changed in the interim
                            Page histPage = addPageToHistory(getRootPage(currentPB), pb);
                            invalidate();

                            remove(currentPB);
                            add(histPage, BorderLayout.CENTER);

                            revalidate();
                            refreshNav(histPage);
                        }

                    }

                };
                pb.runOnLoad(r);
                switch (label) {
                    case HISTORY_LABEL ->
                        loadHistory(pb.textPane, pb);
                    case BOOKMARK_LABEL ->
                        loadBookmarks(pb.textPane, pb);
                    case CERT_LABEL ->
                        loadCerts(pb.textPane, pb);
                    case INFO_LABEL ->
                        loadInfo(pb.textPane, pb, info);
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
                        Page histPage = addPageToHistory(getRootPage(currentPB), pb);
                        int idx = tabbedPane.getSelectedIndex();

                        if (currentTabIdx == idx) {
                            tabbedPane.setComponentAt(idx, histPage);
                            refreshNav(histPage);
                        }

                    }
                };
                pb.runOnLoad(r);
                switch (label) {
                    case HISTORY_LABEL ->
                        loadHistory(pb.textPane, pb);
                    case BOOKMARK_LABEL ->
                        loadBookmarks(pb.textPane, pb);
                    case CERT_LABEL ->
                        loadCerts(pb.textPane, pb);
                    case INFO_LABEL ->
                        loadInfo(pb.textPane, pb, info);
                    default -> {
                    }
                }

            }
        } else {
            Page visiblePB = visiblePage(); // empty page (new tab/window)

            Page nPage = !inPlace ? addPageToHistory(null, visiblePB) : visiblePB;

            switch (label) {
                case HISTORY_LABEL ->
                    loadHistory(nPage.textPane, null);
                case BOOKMARK_LABEL ->
                    loadBookmarks(nPage.textPane, null);
                case CERT_LABEL ->
                    loadCerts(nPage.textPane, null);
                case INFO_LABEL ->
                    loadInfo(nPage.textPane, null, info);
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
        if (tabbedPane != null) {
            int idx = tabbedPane.getSelectedIndex();
            if (idx != -1) {
                ClosableTabPanel ct = (ClosableTabPanel) tabbedPane.getTabComponentAt(idx);
                ct.setTitle(title);
            }
        }
    }

    private void loadBookmarks(GeminiTextPane textPane, Page p) {
        try {
            if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)
                ClosableTabPanel ct = (ClosableTabPanel) tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
                ct.setTitle(BOOKMARK_LABEL);
            }
            List<Bookmark> bookmarks = DB.loadBookmarks();
            if (!bookmarks.isEmpty()) {
                textPane.updatePage("#Bookmarks🔖\nRight click to edit or delete a bookmark.\n\n", false, BOOKMARK_LABEL, true);

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
                ClosableTabPanel ct = (ClosableTabPanel) tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
                ct.setTitle(CERT_LABEL);
            }
            List<DBClientCertInfo> certs = DB.loadCerts();
            if (!certs.isEmpty()) {
                textPane.updatePage("#Client Certificates 🔑\nRight click to activate, inactivate or delete a cert.\n", false, CERT_LABEL, true);

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
                                    X509Certificate xc = (X509Certificate) GeminiClient.loadCertificate(cert.cert());
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

    private void loadInfo(GeminiTextPane textPane, Page p, InfoPageInfo info) {

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)
            ClosableTabPanel ct = (ClosableTabPanel) tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
            ct.setTitle(info.title);
        }

        textPane.end(info.content, true, INFO_LABEL, false);
        textPane.setDocURL(info.title);

        // problem: end already sets comboBoxk
        updateComboBox(new ComboItem(info.title, () -> {
            return info;
        }));
        setTitle(info.title);

    }

    private void loadHistory(GeminiTextPane textPane, Page p) {
        showGlassPane(true);

        if (tabbedPane != null) { // TODO: might not do anything (see runnable that makes visible)
            ClosableTabPanel ct = (ClosableTabPanel) tabbedPane.getTabComponentAt(tabbedPane.getSelectedIndex());
            ct.setTitle(HISTORY_LABEL);
        }
        //setTitle("History");
        textPane.updatePage("# History 📜\n", false, HISTORY_LABEL, true);
        new Thread(() -> {
            int[] count = {0};
            try {
                count[0] = DB.loadHistory(textPane);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            textPane.end();
            EventQueue.invokeLater(() -> {
                if (count[0] == 0) {
                    textPane.addPage("\n### Nothing to see\n");
                } else {
                    //vertx.eventBus().send("status", count[0] + " links");
                    setStatus(count[0] + " links");
                }

                //updateComboBox(HISTORY_LABEL);
                if (p != null) {
                    //r.run();
                    p.loading();
                }

                showGlassPane(false);
            });

        }).start();
    }

    public void deleteBookmark(Component f, String id) {
        int bmId = Integer.parseInt(id);
        try {
            Bookmark bm = DB.getBookmark(bmId);
            Object res = Util.confirmDialog(f, "Delete?", "Are you sure you want to delete this bookmark?\n" + bm.label(), JOptionPane.YES_NO_OPTION);
            if (res instanceof Integer result) {
                if (result == JOptionPane.YES_OPTION) {
                    DB.deleteBookmark(bmId);
                    refresh();
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
            String pem = certInfo.cert() + certInfo.privateKey();
            showCustomPage(INFO_LABEL, new InfoPageInfo(certInfo.domain() + "." + certInfo.id() + ".pem", pem));
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteCert(int id) {
        Object res = Util.confirmDialog(this, "Confirm", "Are you sure you want to delete this certificate?", JOptionPane.YES_NO_OPTION);
        if (res instanceof Integer result) {
            if (result == JOptionPane.YES_OPTION) {
                try {
                    DB.deleteClientCert(id);
                    GeminiClient.createNetClient();
                    refresh();
                    Util.infoDialog(this, "Delete", "Certificate deleted");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }

    }

    public void toggleCert(int id, boolean active, String url) {
        try {
            DB.toggleCert(id, active, url, false);
            GeminiClient.createNetClient();
            refresh();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public void deleteFromHistory(String url) {
        try {
            int rowCount = DB.deleteHistory(url);
            refresh();
            String verbiage = rowCount == 1 ? "1 link " : rowCount + " links ";
            Util.infoDialog(this, "Update", verbiage + "removed from history");

        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void clearHistory() {
        try {
            Object res = Util.confirmDialog(this, "Confirm", "Are you sure you want to delete all history?", JOptionPane.YES_NO_OPTION);
            if (res instanceof Integer result) {
                if (result == JOptionPane.OK_OPTION) {
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
            bookmarkMenu.invalidate();
            addBookmarks();
            bookmarkMenu.validate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

    }

    public void importPem(URI uri, File f) {
        String host = uri.getHost();
        if (host == null || !uri.getScheme().equals("gemini")) {
            Util.infoDialog(this, "Invalid", "Invalid domain. Can't import PEM file.");
            return;
        }
        if (f == null) {
            FileNameExtensionFilter filter = new FileNameExtensionFilter("PEM files (*.pem)", "pem");
            f = Util.getFile(this, null, true, "Select PEM File", filter);
        }

        if (f != null) {
            try {

                String pem = Files.readString(Path.of(f.getAbsolutePath()));
                int idx = pem.indexOf("-----BEGIN PRIVATE KEY");
                if (idx == -1) {
                    Util.infoDialog(this, "Format", "Not a recognized PEM format");
                } else {
                    String cert = pem.substring(0, idx);
                    String key = pem.substring(idx);
                    ClientCertInfo ci = DB.getClientCertInfo(host);
                    if (ci != null) {
                        DB.toggleCert(ci.id(), false, host, false);

                    }

                    DB.insertClientCert(host, cert, key);
                    GeminiClient.addCertToTrustStore(host, visiblePage().getCert());
                    GeminiClient.createNetClient();
                    Util.infoDialog(this, "Added", "PEM added for : " + host);

                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

    public void openFile(File file) {

        if (file == null) {
            String desc = GeminiClient.fileExtensions.stream().collect(Collectors.joining(", "));
            String[] exts = GeminiClient.fileExtensions.toArray(new String[0]);
            String[] cleanExtensions = GeminiClient.fileExtensions.stream()
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

        File saveFile = Util.getFile(this, suggestedName, false, "Save File", null);
        if (saveFile != null) {
            showGlassPane(true);

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

            showGlassPane(false);
        }
    }

    public void toggleView(GeminiTextPane textPane, boolean isPlainText) {
        showGlassPane(true);
        String url = textPane.getDocURLString();
        CurrentPage res = textPane.current();
        streamChunks(res.currentPage(), 100, url, isPlainText);
    }

    public void viewServerCert(GeminiTextPane textPane, URI uri) {
        String host = uri.getHost();

        Optional<Page> page = Optional.ofNullable(SwingUtilities.getAncestorOfClass(Page.class, textPane))
                .map(component -> (Page) component);
        InfoPageInfo pageInfo = new InfoPageInfo("servercert: " + host, page.get().getCert().toString());
        showCustomPage(INFO_LABEL, pageInfo);

        //streamChunks(res.currentPage(), 100, url, true);
    }

    public void newTab(String url) {
        if (tabbedPane == null) {
            invalidate();
            tabbedPane = new JTabbedPane();

            Page pb = visiblePage();
            pb.textPane.getDocURL().ifPresent(docUrl -> {

                remove(pb);
                String frameTitle = createTitle(docUrl, pb.textPane.getFirstHeading());
                addClosableTab(tabbedPane, frameTitle, pb);
            });

            selectComboBoxItem("");

            tabbedPane.addChangeListener(ce -> {

                Page page = (Page) tabbedPane.getSelectedComponent();

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

        if (url == null) {

            Page pb = newPage(null, null, false);
            pb.setThemeId(currentThemeId);

            addClosableTab(tabbedPane, "New Tab", pb);

            tabbedPane.setSelectedComponent(pb);
        } else {
            Page currentPage = visiblePage();
            String du = currentPage.textPane.getDocURLString();

            Page pb = newPage(du, null, true);
            pb.setThemeId(currentThemeId);

            addClosableTab(tabbedPane, "  ", pb);

            tabbedPane.setSelectedComponent(pb);
            // GeminiClient.processURL(url, this, pb.textPane, null, null);
            GeminiClient.processURL(url, pb, null, currentPage);
        }

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

    public void showGlassPane(boolean visible) {

        Runnable r = () -> {
            if (!(getGlassPane() instanceof GeminiGlassPane)) {
                setGlassPane(new GeminiGlassPane());
            }
            validate();
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
        };

        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            EventQueue.invokeLater(r);
        }
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

    private void addClosableTab(JTabbedPane tabbedPane, String title, Component content) {
        // Add the content to the tab
        tabbedPane.add(content);
        ClosableTabPanel tabPanel = new ClosableTabPanel(title);
        tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, tabPanel);

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

    public class ClosableTabPanel extends JPanel {

        private final JLabel tabTitle;

        public ClosableTabPanel(String title) {
            super(new FlowLayout(FlowLayout.LEFT, 5, 0));
            setOpaque(false);
            if (title.startsWith("gemini://") || title.startsWith("file:/") || title.startsWith("http")) {
                title = title.substring(9);
            }
            tabTitle = new JLabel(abbrev(title));
            add(tabTitle);
            JButton closeButton = new JButton("❎");
            closeButton.setFont(new Font("Noto Emoji Regular", Font.BOLD, 8));

            closeButton.setMargin(new Insets(0, 2, 0, 2));
            closeButton.setBorderPainted(false);
            closeButton.setFocusable(false);
            closeButton.addActionListener((ActionEvent e) -> {

                if (tabbedPane.getTabCount() == 1) {
                    GeminiFrame.this.invalidate();
                    ChangeListener[] cl = tabbedPane.getChangeListeners();
                    for (ChangeListener ev : cl) {
                        tabbedPane.removeChangeListener(ev);
                        break; // REMOVES tabbedPanes changeListener but not the L&F changeListener - CAN ORDER CHANGE?
                    }

                    Page page = (Page) tabbedPane.getSelectedComponent();

                    tabbedPane.remove(page);

                    GeminiFrame.this.remove(tabbedPane);
                    tabbedPane = null;

                    GeminiFrame.this.add(page, BorderLayout.CENTER);
                    GeminiFrame.this.validate();

                } else {
                    int index = tabbedPane.indexOfTabComponent(this);
                    Page page = (Page) tabbedPane.getComponentAt(index);

                    pageHistoryMap.remove(page.getRootPage());

                    if (index != -1) {
                        tabbedPane.remove(index);
                    }
                }

            });
            add(closeButton);
        }

        public final String abbrev(String t) {
            if (t.startsWith("gemini://") || t.startsWith("file:/") || t.startsWith("http")) {
                t = t.substring(9);
            }
            if (t.length() > 30) {
                t = t.substring(0, 30) + "...";
            }
            return t;
        }

        public void setTitle(String title) {

            tabTitle.setText(abbrev(title));
        }
    }
    private boolean tabClosing;

}
