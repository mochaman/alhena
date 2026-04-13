package brad.grier.alhena;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyBoundsAdapter;
import java.awt.event.HierarchyEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.Highlighter;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.formdev.flatlaf.util.SystemInfo;
import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.LoaderContext;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.github.weisj.jsvg.view.FloatSize;
import com.techsenger.ansi4j.core.api.Environment;
import com.techsenger.ansi4j.core.api.Fragment;
import com.techsenger.ansi4j.core.api.FragmentType;
import com.techsenger.ansi4j.core.api.ParserFactory;
import com.techsenger.ansi4j.core.api.TextFragment;
import com.techsenger.ansi4j.core.api.iso6429.ControlFunctionType;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryConfig;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryService;
import com.techsenger.ansi4j.core.impl.ParserFactoryProvider;

import brad.grier.alhena.DB.PageStyleInfo;
import brad.grier.alhena.DB.StyleInfo;
import io.vertx.core.http.impl.MimeMapping;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.IndexedEmoji;

/**
 *
 * @author Brad
 */
public class GeminiTextPane extends JTextPane {

    private StyledDocument doc;
    public List<ClickableRange> clickableRegions = new ArrayList<>();
    private int currentCursor = Cursor.DEFAULT_CURSOR;
    private boolean preformattedMode;
    public boolean originalPfMode;
    private String currentStatus = Alhena.welcomeMessage;
    public static String monospacedFamily;
    private GeminiFrame f;
    // use StringBuilder instead of StringBuffer as only updated in EventDispatch at creation
    private StringBuilder pageBuffer;
    private String docURL;
    private ClickableRange saveRange;
    private String bufferedLine = null;
    private Color hoverColor, linkColor;
    private SimpleAttributeSet hoverStyle, normalStyle, visitedStyle;

    public final static int DEFAULT_MODE = 0;
    public final static int BOOKMARK_MODE = 1;
    public final static int HISTORY_MODE = 2;
    public final static int CERT_MODE = 3;
    public final static int INFO_MODE = 4;
    public final static int SERVER_MODE = 5;
    public final static int STYLE_MODE = 6;
    public final static int FEED_MODE = 7;
    public final static int SUBSCRIPTION_MODE = 8;
    public int currentMode = DEFAULT_MODE;

    private String firstHeading;
    private ClickableRange lastClicked;
    private boolean plainTextMode;
    private String lastSearch;
    private int lastSearchIdx;
    int lastSearchDoc = -1;
    private final int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private boolean imageOnly;
    private JScrollPane scrollPane;

    private final Page page;

    public static final HashMap<String, Point> emojiSheetMap = new HashMap<>();
    public static BufferedImage sheetImage = null;
    public int indent;
    public static float contentPercentage;
    public static boolean wrapPF;
    public static boolean asciiImage;
    public static boolean embedPF;
    public static boolean showSB;
    public static boolean shadePF;
    private PreformattedTextPane ptp;
    private StringBuilder asciiSB;
    private Point lastScreen;
    public long pressTime;
    public boolean dragging;
    public static boolean dragToScroll;
    private int printWidth;
    private ArrayList<ClickableRange> openQueue;
    private LinkedHashMap<String, Integer> headingMap = new LinkedHashMap<>();
    private static final Set<Integer> DIRECTIVE_MODES = Set.of(BOOKMARK_MODE, CERT_MODE, STYLE_MODE, FEED_MODE, SUBSCRIPTION_MODE);

    private GeminiFrame f() {
        GeminiFrame frame = (GeminiFrame) SwingUtilities.getWindowAncestor(this);
        if (frame != null) {
            return frame;
        }
        return f;
    }

    public void setFrame(GeminiFrame gf) {
        f = gf;
    }

    public static void setup() {
        String userDefined = System.getenv("ALHENA_MONOFONT");
        if (SystemInfo.isWindows) {

            monospacedFamily = userDefined == null || userDefined.isBlank() ? "Source Code Pro" : userDefined;

        } else if (SystemInfo.isMacOS) {
            // PT Mono - some horizontal lines but works
            // Menlo - taller horizontal lines
            // Courier New - works!
            // FreeMono - no horizontal lines but squished a bit
            // Liberation Mono - works on pi but lines
            String uff = userDefined == null ? "" : userDefined;
            List<String> goodFonts = List.of(uff, "Courier New", "Andale Mono", "PT Mono", "Monospaced");
            for (String ff : goodFonts) {
                if (Util.isFontAvailable(ff)) {
                    monospacedFamily = ff;
                    break;
                }
            }

        } else { // linux, bsd and whatnot
            monospacedFamily = userDefined == null || userDefined.isBlank() ? "Monospaced" : userDefined;
        }

        String emojiPref = DB.getPref("emoji", null);
        if (emojiPref == null || emojiPref.equals("google")) { // first time or the default set from jar
            setSheetImage(Util.loadImage(GeminiFrame.emojiNameMap.get("google")));
            DB.insertPref("emoji", "google");  // only really need to this for null

        } else if (!emojiPref.equals("font")) {
            String url = GeminiFrame.emojiNameMap.get(emojiPref);
            String fn = url.substring(url.lastIndexOf('/') + 1);
            File emojiFile = new File(Alhena.alhenaHome + File.separatorChar + fn);

            // if the file exists, make sure it's the right version
            if (emojiFile.exists() && Util.getSetSize(emojiPref) == emojiFile.length()) {
                try {
                    setSheetImage(ImageIO.read(emojiFile));
                } catch (IOException ex) {
                    ex.printStackTrace();
                    // reset to google
                    setSheetImage(Util.loadImage(GeminiFrame.emojiNameMap.get("google")));
                    DB.insertPref("emoji", "google");
                }

            } else {
                // reset to google
                setSheetImage(Util.loadImage(GeminiFrame.emojiNameMap.get("google")));
                DB.insertPref("emoji", "google");
            }

        }
    }

    private static void readEmojiJson() {
        try (InputStream is = GeminiTextPane.class.getResourceAsStream("/emoji.json")) {
            if (is == null) {
                throw new RuntimeException("Resource not found: /emoji.json");
            }

            // Read the InputStream into a String
            String content = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Convert to JsonObject
            JsonArray emojiArray = new JsonArray(content);
            for (int i = 0; i < emojiArray.size(); i++) {
                JsonObject jo = emojiArray.getJsonObject(i);

                emojiSheetMap.put(jo.getString("unified"), new Point(jo.getInteger("sheet_x"), jo.getInteger("sheet_y")));
                String nonQualified = jo.getString("non_qualified");
                if (nonQualified != null) {
                    emojiSheetMap.put(nonQualified, new Point(jo.getInteger("sheet_x"), jo.getInteger("sheet_y")));
                }
                JsonObject skinVariations = jo.getJsonObject("skin_variations");
                if (skinVariations != null) {
                    skinVariations.forEach(entry -> {
                        JsonObject variation = (JsonObject) entry.getValue();
                        emojiSheetMap.put(variation.getString("unified"), new Point(variation.getInteger("sheet_x"), variation.getInteger("sheet_y")));
                    });

                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // IBM Plex Mono works for proper alignment too
    public GeminiTextPane(GeminiFrame f, Page page, String url) {
        this.f = f;
        this.page = page;
        docURL = url;
        setFocusTraversalPolicy(
                new ClassFocusTraversalPolicy(PreformattedTextPane.class)
        );

        Insets insets = getMargin();
        setMargin(new Insets(35, insets.left, insets.bottom, insets.right));

        setEditorKit(new GeminiEditorKit());

        setEditable(false);
        DefaultCaret newCaret = new DefaultCaret() {
            @Override
            public void paint(Graphics g) {
                // do nothing to prevent caret from being painted
            }
        };
        setCaret(newCaret);

        EventQueue.invokeLater(() -> {
            scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, GeminiTextPane.this);
        });

        // take over up and down arrow scrolling
        InputMap inputMap = getInputMap(JComponent.WHEN_FOCUSED);
        inputMap.put(KeyStroke.getKeyStroke("UP"), "scrollUpNow");
        getActionMap().put("scrollUpNow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {

                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getValue() - bar.getUnitIncrement(-1));
            }
        });

        inputMap.put(KeyStroke.getKeyStroke("DOWN"), "scrollDownNow");
        getActionMap().put("scrollDownNow", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JScrollBar bar = scrollPane.getVerticalScrollBar();
                bar.setValue(bar.getValue() + bar.getUnitIncrement(1));
            }
        });
        if (!SystemInfo.isMacOS) {
            inputMap.put(KeyStroke.getKeyStroke("HOME"), "docHome");
            getActionMap().put("docHome", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JScrollBar bar = scrollPane.getVerticalScrollBar();
                    bar.setValue(bar.getMinimum());
                }
            });
            inputMap.put(KeyStroke.getKeyStroke("END"), "docEnd");
            getActionMap().put("docEnd", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    JScrollBar bar = scrollPane.getVerticalScrollBar();
                    bar.setValue(bar.getMaximum());
                }
            });
        }

        if (Alhena.smoothScrolling) {
            setupAdaptiveScrolling();
        }
        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {

                int pos = viewToModel2D(e.getPoint());
                boolean entered = false;

                for (ClickableRange range : clickableRegions) {
                    if (pos >= range.start && pos < range.end) {

                        Rectangle rect = getCharacterBounds(range.start, range.end);
                        if (rect != null && rect.contains(e.getPoint())) {
                            entered = true;

                            if (!range.displayString().equals(currentStatus)) {
                                // +1 needed since using png for emoji

                                SimpleAttributeSet sa = new SimpleAttributeSet();
                                StyleConstants.setForeground(sa, hoverColor);

                                f().setStatus(range.displayString());
                                currentStatus = range.displayString();

                                doc.setCharacterAttributes(range.start, range.end - range.start, sa, false);

                                if (saveRange != null) {

                                    setLinkStyle();

                                }
                                saveRange = range;

                            }

                            break;
                        }
                    }
                }

                // fix this
                if (!entered && !" ".equals(currentStatus) && currentStatus != null && !currentStatus.equals(Alhena.welcomeMessage)) {

                    f().setStatus(" ");

                    if (saveRange != null) {

                        setLinkStyle();
                        saveRange = null;
                    }
                    currentStatus = " ";

                }

                if (entered && currentCursor != Cursor.HAND_CURSOR) {

                    currentCursor = Cursor.HAND_CURSOR;
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                } else if (!entered && currentCursor != Cursor.DEFAULT_CURSOR) {
                    currentCursor = Cursor.DEFAULT_CURSOR;
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                }

            }
        });

        addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {

                showPopup(e);

            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (dragToScroll) {
                    lastScreen = e.getLocationOnScreen();
                    pressTime = System.currentTimeMillis();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragToScroll) {
                    lastScreen = null;
                    pressTime = 0;
                    dragging = false;
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                // duplicate of !entered logic above
                // links could remain highlighted if located near top or bottom of JTextPane and mouse exits
                f().setStatus(" ");

                if (saveRange != null) {
                    setLinkStyle();
                    saveRange = null;
                }
                currentStatus = " ";

            }
        });

        setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent evt) {
                try {
                    evt.acceptDrop(DnDConstants.ACTION_COPY);
                    List<File> droppedFiles = (List<File>) evt.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);

                    for (File file : droppedFiles) {
                        String lcName = file.getName().toLowerCase();
                        boolean matches = Alhena.fileExtensions.stream().anyMatch(lcName::endsWith);
                        String mimeExt = MimeMapping.getMimeTypeForFilename(lcName);
                        boolean vlcType = lcName.endsWith(".opus") || (mimeExt != null && (mimeExt.startsWith("audio") || mimeExt.startsWith("video")));
                        if (matches || vlcType) {

                            if (lcName.endsWith(".pem")) {
                                f().importPem(new URI(getDocURLString()), file);
                            } else {
                                f().openFile(file);
                            }
                        }

                        break;

                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        // componentResized doesn't fire after going fullscreen (mac), showing and then hiding an inline image and then exiting fullscreen
        // hierarchylistener does fire but more often so only do this on mac
        boolean useHierarchyListener = SystemInfo.isMacOS && SystemInfo.isMacFullWindowContentSupported;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                applyCenteredParagraphStyle();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (!useHierarchyListener) {
                    resized();
                }
            }
        });
        if (useHierarchyListener) {
            addHierarchyBoundsListener(new HierarchyBoundsAdapter() {
                @Override
                public void ancestorResized(HierarchyEvent e) {
                    resized();
                }
            });
        }

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                int keyCode = ke.getKeyCode();
                if ((keyCode == KeyEvent.VK_F || keyCode == KeyEvent.VK_G) && lgp == null) {

                    lgp = new LinkGlassPane(GeminiTextPane.this, true, keyCode == KeyEvent.VK_G);
                    f().setGlassPane(lgp);
                    lgp.setVisible(true);

                    ke.consume();
                    return;
                }
                if (lgp != null) {

                    int index = -1;

                    if (keyCode >= KeyEvent.VK_1 && keyCode <= KeyEvent.VK_9) {  // 1-9 → indices 0-8
                        index = keyCode - KeyEvent.VK_1;
                    } // A-Z → indices 9-34
                    else if (keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
                        index = 9 + (keyCode - KeyEvent.VK_A);
                    }

                    if (index >= 0 && lgp != null) {

                        lgp.setVisible(false);

                        if (lgp.isRightClick()) {
                            rightClickVisibleLink(index);
                        } else {
                            clickVisibleLink(index);
                        }
                        lgp = null;

                    }
                }
            }
        }
        );
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                resetLGP();
            }
        });
    }

    public LinkGlassPane lgp;

    public void resetLGP() {
        if (lgp != null) {
            lgp.setVisible(false);
            lgp = null;
        }
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        resetLGP();
    }

    private void resized() {
        if (inserting) {
            inserting = false;
            return;
        }
        if (doc != null) {
            applyCenteredParagraphStyle();
        }
        savedContentWidth = contentWidth;
        if (ptpList != null) {
            for (PreformattedTextPane ptp : ptpList) {
                JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, ptp);
                sp.setMaximumSize(new Dimension((int) contentWidth, Integer.MAX_VALUE));
                ptp.revalidate();
                ptp.repaint();
            }
        }
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (dragToScroll && e.getID() == MouseEvent.MOUSE_DRAGGED && (dragging || System.currentTimeMillis() - pressTime < 500)) {
            if (lastScreen == null) {
                return;
            }
            dragging = true;
            Point current = e.getLocationOnScreen();
            int dx = current.x - lastScreen.x;  // flipped
            int dy = current.y - lastScreen.y;  // flipped

            JScrollBar hbar = scrollPane.getHorizontalScrollBar();
            JScrollBar vbar = scrollPane.getVerticalScrollBar();

            hbar.setValue(hbar.getValue() - dx);
            vbar.setValue(vbar.getValue() - dy);

            lastScreen = current;

        } else {
            super.processMouseMotionEvent(e);
            pressTime = 0;
        }
    }

    private boolean inserting;

    public static void setSheetImage(BufferedImage sheet) {
        if (sheet != null && emojiSheetMap.isEmpty()) {
            readEmojiJson();
        }

        // IF YOU WANT SPRITE EMOJIS TO BE GRAYSCALE
        // BufferedImage gray = new BufferedImage(
        //         sheet.getWidth(),
        //         sheet.getHeight(),
        //         BufferedImage.TYPE_INT_ARGB
        // );
        // ColorConvertOp op = new ColorConvertOp(
        //         ColorSpace.getInstance(ColorSpace.CS_GRAY), null
        // );
        // op.filter(sheet, gray);
        // sheetImage = gray;
        sheetImage = sheet;
    }

    private void showPopup(MouseEvent e) {
        requestFocusInWindow();
        if (doc != null) {
            int pos = viewToModel2D(e.getPoint());
            boolean linkClicked = false;
            if (pos >= 0 && pos < doc.getLength()) {
                for (ClickableRange range : clickableRegions) {
                    if (pos >= range.start && pos < range.end) {
                        Rectangle rect = getCharacterBounds(range.start, range.end);
                        if (rect != null && rect.contains(e.getPoint())) {
                            linkClicked = true;
                            if (SwingUtilities.isRightMouseButton(e) || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {

                                JPopupMenu popupMenu = new JPopupMenu();

                                String selectedText = getSelectedText();
                                if (selectedText != null && !selectedText.isEmpty()) {
                                    JMenuItem copyItem = new JMenuItem(I18n.t("copyPopupItem"));

                                    copyItem.addActionListener(ev -> {
                                        copyText(selectedText);
                                    });
                                    popupMenu.add(copyItem);

                                }
                                String resolvedURI;
                                if (range.url.startsWith("alhena:")) {
                                    resolvedURI = range.url;
                                } else {
                                    resolvedURI = Util.resolveURI(getURI(), range.url);
                                }
                                if (currentMode != STYLE_MODE) {
                                    JMenuItem copyLinkItem = new JMenuItem(I18n.t("copyLinkPopupItem"));

                                    copyLinkItem.addActionListener(ev -> {
                                        copyText(resolvedURI);
                                    });
                                    popupMenu.add(copyLinkItem);

                                    popupMenu.add(new JSeparator());
                                    JMenuItem newTabItem = new JMenuItem(I18n.t("newTabPopupItem"));

                                    newTabItem.addActionListener(ev -> {
                                        // open in new tab with gemtext converter regardless
                                        boolean saveSetting = Alhena.useBrowser;
                                        Alhena.useBrowser = false;
                                        clicked(range);

                                        f().newTab(range.url, null, null, range.label == null ? null : range.label);
                                        Alhena.useBrowser = saveSetting;
                                    });
                                    newTabItem.setEnabled(!range.dataUrl);
                                    popupMenu.add(newTabItem);

                                    if (SwingUtilities.getAncestorOfClass(SplitPanel.class, GeminiTextPane.this) == null) {
                                        JMenuItem splitRightItem = new JMenuItem(I18n.t("splitRightItem"));

                                        splitRightItem.addActionListener(ev -> {
                                            // open in new tab with gemtext converter regardless
                                            boolean saveSetting = Alhena.useBrowser;
                                            Alhena.useBrowser = false;
                                            clicked(range);

                                            f().splitView(range.url, null, JSplitPane.HORIZONTAL_SPLIT, range.label);
                                            Alhena.useBrowser = saveSetting;
                                        });
                                        splitRightItem.setEnabled(!range.dataUrl);
                                        popupMenu.add(splitRightItem);

                                        JMenuItem splitBottomItem = new JMenuItem(I18n.t("splitBottomItem"));

                                        splitBottomItem.addActionListener(ev -> {
                                            boolean saveSetting = Alhena.useBrowser;
                                            Alhena.useBrowser = false;
                                            clicked(range);

                                            f().splitView(range.url, null, JSplitPane.VERTICAL_SPLIT, range.label);
                                            Alhena.useBrowser = saveSetting;
                                        });
                                        splitBottomItem.setEnabled(!range.dataUrl);
                                        popupMenu.add(splitBottomItem);
                                    } else {
                                        JMenuItem oppositeItem = new JMenuItem(I18n.t("openOppItem"));

                                        oppositeItem.addActionListener(ev -> {
                                            boolean saveSetting = Alhena.useBrowser;
                                            Alhena.useBrowser = false;
                                            f().splitView(range.url, null, JSplitPane.VERTICAL_SPLIT, range.label);
                                            Alhena.useBrowser = saveSetting;
                                        });
                                        oppositeItem.setEnabled(!range.dataUrl);
                                        popupMenu.add(oppositeItem);
                                    }

                                    JMenuItem menuItem2 = new JMenuItem(I18n.t("newWindowPopupItem"));
                                    menuItem2.setEnabled(!range.dataUrl);
                                    menuItem2.addActionListener(ev -> {
                                        // open in new tab with gemtext converter regardless
                                        boolean saveSetting = Alhena.useBrowser;
                                        Alhena.useBrowser = false;
                                        Alhena.newWindow(range.url, docURL, null, null, range.label);
                                        Alhena.useBrowser = saveSetting;

                                    });
                                    popupMenu.add(menuItem2);

                                }

                                if (Alhena.browsingSupported && resolvedURI.toLowerCase().startsWith("http")) {
                                    JMenuItem httpMenuItem = new JMenuItem(I18n.t("browserPopupItem"));
                                    httpMenuItem.addActionListener(al -> {

                                        boolean saveBrowser = Alhena.useBrowser;
                                        Alhena.useBrowser = true;
                                        f().fetchURL(resolvedURI, false, null);
                                        Alhena.useBrowser = saveBrowser;
                                    });

                                    popupMenu.add(httpMenuItem);
                                }

                                switch (currentMode) {
                                    case CERT_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        String[] tokens = range.directive.split(",", 3);
                                        int id = Integer.parseInt(tokens[0]);
                                        boolean active = tokens[1].equals("true");
                                        JMenuItem exportItem = new JMenuItem(I18n.t("exportPopupItem"));
                                        exportItem.addActionListener(al -> {
                                            f().exportCert(id, GeminiTextPane.this);
                                        });
                                        popupMenu.add(exportItem);
                                        String command = active ? I18n.t("deactivatePopupItem") : I18n.t("activatePopupItem");
                                        JMenuItem actionItem = new JMenuItem(command);
                                        actionItem.addActionListener(al -> {

                                            f().toggleCert(id, !active, range.url);
                                        });
                                        popupMenu.add(actionItem);
                                        JMenuItem delItem = new JMenuItem(I18n.t("deletePopupItem"));
                                        delItem.addActionListener(al -> {
                                            f().deleteCert(id);
                                        });
                                        popupMenu.add(delItem);
                                    }
                                    case STYLE_MODE -> {
                                        JMenuItem jsonItem = new JMenuItem(I18n.t("styleJsonItem"));
                                        jsonItem.addActionListener(al -> {
                                            try {
                                                PageStyleInfo psi = DB.getStyle(Integer.parseInt(range.directive));
                                                JTextArea jta = new JTextArea(new JsonObject(psi.style()).encodePrettily());
                                                jta.setEditable(false);
                                                jta.setLineWrap(true);
                                                jta.setWrapStyleWord(true);

                                                JScrollPane jsp = new JScrollPane(jta);
                                                jsp.setPreferredSize(new Dimension(700, 400));
                                                Object[] comps = {jsp};
                                                Util.fancyInfoDialog(f(), I18n.t("styleJsonDialog"), comps);

                                            } catch (SQLException ex) {
                                                ex.printStackTrace();
                                            }

                                        });
                                        popupMenu.add(jsonItem);
                                        JMenuItem delStyleItem = new JMenuItem(I18n.t("deleteStyleItem"));
                                        delStyleItem.addActionListener(al -> {
                                            Object r = Util.confirmDialog(f(), I18n.t("styleDeleteDialog"), I18n.t("styleDeleteDialogTxt"), JOptionPane.YES_NO_OPTION, null, null);
                                            if (r instanceof Integer rs && rs == JOptionPane.YES_OPTION) {
                                                try {
                                                    DB.deleteStyle(Integer.parseInt(range.directive));
                                                } catch (SQLException ex) {
                                                    ex.printStackTrace();
                                                }
                                                Alhena.updateFrames(false, false, false, false);
                                                EventQueue.invokeLater(() -> f().refresh());
                                            }

                                        });
                                        popupMenu.add(delStyleItem);

                                    }
                                    case HISTORY_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem forgetItem = new JMenuItem(I18n.t("forgetLinkItem"));
                                        forgetItem.addActionListener(al -> {
                                            f().deleteFromHistory(range.url, false);
                                        });

                                        popupMenu.add(forgetItem);
                                        JMenuItem clearItem = new JMenuItem(I18n.t("deleteHistoryItem"));

                                        clearItem.addActionListener(al -> {
                                            f().clearHistory();
                                        });
                                        popupMenu.add(clearItem);
                                    }
                                    case BOOKMARK_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem editItem = new JMenuItem(I18n.t("editBookmarkItem"));
                                        editItem.addActionListener(ev -> {
                                            f().updateBookmark(f(), range.directive);

                                        });
                                        popupMenu.add(editItem);
                                        JMenuItem deleteItem = new JMenuItem(I18n.t("deleteBookmarkItem"));
                                        deleteItem.addActionListener(ev -> {
                                            f().deleteBookmark(f(), range.directive);

                                        });
                                        popupMenu.add(deleteItem);
                                    }
                                    case FEED_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem openFeedItem = new JMenuItem("Open Feed Page");
                                        openFeedItem.addActionListener(ev -> {
                                            String token = range.directive.split(",", 4)[1];
                                            String pUrl = DB.getFeedUrl(Integer.parseInt(token));
                                            if (pUrl != null) {
                                                f().fetchURL(pUrl, false, null);
                                            }

                                        });
                                        popupMenu.add(openFeedItem);
                                        boolean read = Integer.parseInt(range.directive.split(",", 4)[2]) == 1;
                                        JMenuItem markReadItem = new JMenuItem(read ? "Mark As Unread" : "Mark As Read");
                                        markReadItem.addActionListener(ev -> {
                                            DB.markUrlRead(range.url, !read);
                                            f().refresh(false);

                                        });
                                        popupMenu.add(markReadItem);

                                        JMenuItem markBelowItem = new JMenuItem("Mark Below As Read");
                                        markBelowItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 2)[0]);

                                            DB.markOlderFeedRecords(id);
                                            f().refresh(false);

                                        });
                                        popupMenu.add(markBelowItem);

                                        JMenuItem unsubscribeItem = new JMenuItem("Unsubscribe");
                                        unsubscribeItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 3)[1]);
                                            String s = DB.getFeedUrl(id);
                                            Object r = Util.confirmDialog(f(), "Confirm", "Are you sure you want to unsubscribe from\n" + s, JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
                                            if (r instanceof Integer result) {
                                                if (result == JOptionPane.YES_OPTION) {

                                                    DB.unsubscribe(id);
                                                    f().refresh(false);
                                                }
                                            }

                                        });
                                        popupMenu.add(unsubscribeItem);

                                    }
                                    case SUBSCRIPTION_MODE -> {
                                        popupMenu.add(new JSeparator());

                                        JMenuItem editItem = new JMenuItem("Edit Label");
                                        editItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 2)[0]);
                                            String origLabel = DB.getSubLabel(id);
                                            String label = Util.inputDialog(f(), "Edit", "Edit label for subscription",
                                                    false, origLabel, null);
                                            if (label != null && !label.isBlank()) {
                                                if (label.length() > 128) {
                                                    label = label.substring(0, 125) + "...";
                                                }
                                                DB.updateSubLabel(id, label);
                                                f().refresh();
                                            }
                                        });
                                        popupMenu.add(editItem);

                                        JMenuItem markReadItem = new JMenuItem("Mark As Read");
                                        markReadItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 2)[0]);

                                            DB.markSubscriptionRead(id, true);

                                            Util.infoDialog(f(), "Info", "Subscription marked as read.\n" + range.url);

                                        });
                                        popupMenu.add(markReadItem);

                                        JMenuItem markUnreadItem = new JMenuItem("Mark As Unread");
                                        markUnreadItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 2)[0]);
                                            DB.markSubscriptionRead(id, false);

                                            Util.infoDialog(f(), "Info", "Subscription marked as unread.\n" + range.url);

                                        });
                                        popupMenu.add(markUnreadItem);

                                        JMenuItem unsubscribeItem = new JMenuItem("Unsubscribe");
                                        unsubscribeItem.addActionListener(ev -> {
                                            int id = Integer.parseInt(range.directive.split(",", 2)[0]);
                                            String s = DB.getFeedUrl(id);
                                            Object r = Util.confirmDialog(f(), "Confirm", "Are you sure you want to unsubscribe from\n" + s, JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
                                            if (r instanceof Integer result) {
                                                if (result == JOptionPane.YES_OPTION) {

                                                    DB.unsubscribe(id);
                                                    f().refresh();
                                                }
                                            }

                                        });
                                        popupMenu.add(unsubscribeItem);
                                    }
                                    default -> {
                                    }
                                }

                                popupMenu.show(e.getComponent(), e.getX(), e.getY());

                            } else if (SwingUtilities.isLeftMouseButton(e)) {

                                if (range.imageIndex != -1) {
                                    removeItemAtIndex(range);
                                    range.imageIndex = -1;

                                } else {
                                    lastClicked = range;
                                    if (!range.dataUrl) {
                                        clicked(range);
                                        range.action.run();
                                    } else {
                                        dataURL(range.url, false);

                                    }

                                }
                            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                                if (!range.dataUrl) {
                                    boolean saveSetting = Alhena.useBrowser;
                                    Alhena.useBrowser = false;
                                    f().newTab(range.url, null, null, range.label);
                                    Alhena.useBrowser = saveSetting;
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (!linkClicked) {
                if (SwingUtilities.isRightMouseButton(e) || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {

                    JPopupMenu popupMenu = new JPopupMenu();

                    String selectedText = getSelectedText();
                    if (selectedText != null && !selectedText.isEmpty()) {
                        JMenuItem copyItem = new JMenuItem(I18n.t("copyPopupItem"));

                        copyItem.addActionListener(ev -> {
                            copyText(selectedText);
                        });
                        popupMenu.add(copyItem);
                        popupMenu.add(new JSeparator());

                    }

                    JMenuItem homePageMenuItem = new JMenuItem(I18n.t("homePageItem"));
                    homePageMenuItem.addActionListener(al -> {
                        DB.insertPref("home", docURL);
                    });

                    popupMenu.add(homePageMenuItem);
                    homePageMenuItem.setEnabled(currentMode != INFO_MODE);
                    if (!page.isNex() && !page.isGopher()) {
                        String menuText = plainTextMode ? I18n.t("gemTextItem") : I18n.t("plainTextItem");
                        JMenuItem ptMenuItem = new JMenuItem(menuText);
                        ptMenuItem.setEnabled(!imageOnly);
                        ptMenuItem.addActionListener(al -> {
                            plainTextMode = !plainTextMode;
                            f().toggleView(GeminiTextPane.this, plainTextMode);
                        });

                        popupMenu.add(ptMenuItem);
                    }

                    URI uri = getURI();
                    if (!imageOnly && uri != null && "gemini".equals(uri.getScheme())) {
                        JMenuItem crtMenuItem = new JMenuItem(I18n.t("viewCertItem"));
                        crtMenuItem.addActionListener(al -> {

                            f().viewServerCert(GeminiTextPane.this, getURI());
                        });

                        popupMenu.add(crtMenuItem);
                    }

                    if (currentMode == DEFAULT_MODE && Alhena.socksFilter && uri != null && !"alhena".equals(uri.getScheme())) {
                        JCheckBoxMenuItem socksItem = new JCheckBoxMenuItem(I18n.t("socksPopupItem"));

                        boolean socksDomain;
                        try {
                            socksDomain = DB.socksDomainExists(uri.getHost());
                            socksItem.setSelected(socksDomain);
                        } catch (SQLException ex) {
                            socksItem.setSelected(false); // in case
                            ex.printStackTrace();
                        }

                        socksItem.addActionListener(al -> {
                            if (socksItem.isSelected()) {
                                try {
                                    DB.insertSocksDomain(uri.getHost());
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }
                            } else {
                                try {
                                    DB.deleteSocksDomain(uri.getHost());
                                } catch (SQLException ex) {
                                    ex.printStackTrace();
                                }

                            }
                            Alhena.closeNetClientByDomain(uri.getHost());

                        });

                        popupMenu.add(socksItem);
                    }

                    popupMenu.add(new JSeparator());
                    JMenuItem saveItem = new JMenuItem(I18n.t("savePageItem"));
                    saveItem.setEnabled(!imageOnly);
                    saveItem.addActionListener(al -> {
                        f().savePage(GeminiTextPane.this, pageBuffer, currentMode);
                    });

                    popupMenu.add(saveItem);

                    if (docURL.startsWith("gemini://")) {
                        JMenuItem subscribeItem = new JMenuItem("Subscribe");
                        subscribeItem.setEnabled(!imageOnly);
                        subscribeItem.addActionListener(al -> {
                            try {
                                if (DB.isSubscribed(docURL)) {
                                    Util.infoDialog(f, "Already Subscribed", "Already subscribed to " + docURL);
                                } else {

                                    String suggestedName = getFirstHeading();
                                    if (suggestedName == null) {
                                        suggestedName = URI.create(docURL).getHost();
                                    }
                                    int type = DB.insertSubscribed(docURL, pageBuffer.toString(), suggestedName);
                                    String tString = type == 1 ? "YYYY-MM-DD Links" : "Headings";
                                    Util.infoDialog(f, "Subscribed", "Subscribed to " + docURL + "\nSubscription type: " + tString);
                                }
                            } catch (SQLException ex) {
                                ex.printStackTrace();
                            }

                        });

                        popupMenu.add(subscribeItem);
                    }

                    // TODO: When is URI actually null? Investigate.
                    boolean showGeminiItems = !imageOnly && currentMode == DEFAULT_MODE && uri != null && uri.getHost() != null && "gemini".equals(uri.getScheme());

                    if (showGeminiItems) {
                        JMenuItem titanItem = new JMenuItem(I18n.t("titanItem"));
                        titanItem.setEnabled(!imageOnly && currentMode == DEFAULT_MODE && uri != null && "gemini".equals(uri.getScheme()));
                        titanItem.addActionListener(al -> {

                            f().editPage();
                        });
                        popupMenu.add(titanItem);
                        JMenuItem pemItem = new JMenuItem(I18n.t("importPEMPopup"));
                        pemItem.setEnabled(!imageOnly);
                        pemItem.addActionListener(al -> {
                            f().importPem(getURI(), null);
                        });
                        popupMenu.add(pemItem);

                        JMenuItem certItem = new JMenuItem(I18n.t("newClientCertPopup"));
                        certItem.setEnabled(!imageOnly);
                        certItem.addActionListener(al -> {
                            f().createCert(getURI());
                        });
                        popupMenu.add(certItem);
                    }

                    switch (currentMode) {
                        case STYLE_MODE -> {
                            popupMenu.add(new JSeparator());
                            JMenuItem newStyleItem = new JMenuItem(I18n.t("stylesItem"));
                            newStyleItem.addActionListener(al -> {
                                Util.newStyle(f());
                            });
                            popupMenu.add(newStyleItem);
                        }
                        case HISTORY_MODE -> {
                            popupMenu.add(new JSeparator());
                            JMenuItem whereItem = new JMenuItem(I18n.t("forgetLinksPopup"));
                            whereItem.addActionListener(al -> {
                                f().deleteFromHistory(null, true);
                            });
                            popupMenu.add(whereItem);
                            JMenuItem clearItem = new JMenuItem(I18n.t("deleteHistoryPopup"));
                            clearItem.addActionListener(al -> {
                                f().clearHistory();
                            });
                            popupMenu.add(clearItem);
                        }
                        case FEED_MODE -> {
                            popupMenu.add(new JSeparator());
                            JMenuItem allReadItem = new JMenuItem("Mark All Read");
                            allReadItem.addActionListener(al -> {
                                DB.markAllFeeds(true);
                                f().refresh(false);
                            });
                            popupMenu.add(allReadItem);
                            JMenuItem allUnreadItem = new JMenuItem("Mark All Unread");
                            allUnreadItem.addActionListener(al -> {
                                DB.markAllFeeds(false);
                                f().refresh(false);
                            });
                            popupMenu.add(allUnreadItem);
                            JMenuItem refreshItem = new JMenuItem("Refresh All Feeds");
                            refreshItem.addActionListener(al -> {

                                f().setBusy(true, page);
                                Thread.ofVirtual().start(() -> {
                                    // new Thread(() -> {
                                    try {
                                        DB.updateFeeds(true);
                                        EventQueue.invokeLater(() -> {
                                            f().refresh(false);
                                            f().setBusy(false, page);
                                        });
                                    } catch (SQLException ex) {
                                        ex.printStackTrace();
                                    }

                                });

                            });
                            popupMenu.add(refreshItem);
                            popupMenu.add(new JSeparator());
                            JMenuItem allFeedsItem = new JMenuItem("Show All Feeds");
                            allFeedsItem.addActionListener(al -> {
                                f().loadAllFeeds = true;
                                f().refresh(false);

                            });
                            popupMenu.add(allFeedsItem);
                        }
                        case SUBSCRIPTION_MODE -> {
                            popupMenu.add(new JSeparator());
                            JMenuItem allFeedsItem = new JMenuItem("Unsubscribe All");
                            allFeedsItem.addActionListener(al -> {
                                Object r = Util.confirmDialog(f(), "Confirm", "Are you sure you want to unsubscribe from all feeds?", JOptionPane.YES_NO_OPTION, null, JOptionPane.WARNING_MESSAGE);
                                if (r instanceof Integer result) {
                                    if (result == JOptionPane.YES_OPTION) {

                                        DB.unsubscribeAll();
                                        f().refresh();
                                    }
                                }

                            });
                            popupMenu.add(allFeedsItem);
                        }
                        default -> {
                        }
                    }
                    if (SwingUtilities.getAncestorOfClass(SplitPanel.class, GeminiTextPane.this) != null) {
                        popupMenu.add(new JSeparator());
                        JMenuItem splitItem = new JMenuItem(I18n.t("closeSplitItem"));
                        splitItem.addActionListener(al -> {
                            f().removeSplitView();
                        });
                        popupMenu.add(splitItem);
                    }
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

    }

    private void setLinkStyle() {
        SimpleAttributeSet sas = (currentMode != FEED_MODE && f().isClickedLink(saveRange.url)) || feedVisitedList.contains(saveRange.displayString()) ? visitedStyle : normalStyle;
        doc.setCharacterAttributes(saveRange.start, saveRange.end - saveRange.start, sas, false);
    }

    private void clicked(ClickableRange range) {
        doc.setCharacterAttributes(range.start, range.end - range.start, visitedStyle, false);
        if (currentMode == FEED_MODE) {
            feedVisitedList.add(range.displayString());
        }
    }

    @Override
    public void processKeyEvent(KeyEvent e) {

        if ((e.getModifiersEx() & mod) != 0) {
            int kc = e.getKeyCode();
            if ((kc == KeyEvent.VK_I || kc == KeyEvent.VK_U) && e.getID() == KeyEvent.KEY_PRESSED) {
                Point mousePoint = MouseInfo.getPointerInfo().getLocation();
                SwingUtilities.convertPointFromScreen(mousePoint, GeminiTextPane.this);

                long now = System.currentTimeMillis();
                int x = mousePoint.x;
                int y = mousePoint.y;
                MouseEvent pressEvent = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, now,
                        InputEvent.BUTTON3_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON3);
                showPopup(pressEvent);

            }
        } else {
            super.processKeyEvent(e);
        }
    }

    public URI getURI() {

        return getDocURL().map(u -> {
            try {
                return new URI(u);
            } catch (URISyntaxException ex) {
                return null;
            }
        }).orElse(null);

    }

    public int getDocMode() {
        return currentMode;
    }

    private void removeItemAtIndex(ClickableRange rg) {
        if (rg != null) {
            rg.openImage = false;
        }
        int index = rg == null ? -1 : rg.imageIndex;
        try {
            Element element = doc.getCharacterElement(index + 1);
            AttributeSet attrs = element.getAttributes();
            Component jc = StyleConstants.getComponent(attrs);

            if (StyleConstants.getIcon(attrs) != null || jc != null) {
                if (jc != null) {
                    MediaComponent removedAp = null;
                    for (MediaComponent ap : playerList) {
                        if (ap == jc) {
                            ap.dispose();
                            removedAp = ap;
                            break;
                        }
                    }
                    if (removedAp != null) {
                        playerList.remove(removedAp);
                    }
                }
                if (rg != null) {
                    doc.remove(index + 1, 2);
                } else {
                    doc.remove(0, 1);
                }
                if (rg != null) {
                    boolean start = false;
                    for (ClickableRange range : clickableRegions) {
                        if (range == rg) {
                            start = true;
                            continue;
                        }
                        if (start) {
                            range.start = range.start - 2;
                            range.end = range.end - 2;
                            if (range.imageIndex != -1) {
                                range.imageIndex = range.imageIndex - 2;
                            }
                        }
                    }
                    headingMap.replaceAll((key, value) -> value > rg.start ? value - 2 : value);
                }
            }

            if (rg != null && rg.dataUrl) {
                EventQueue.invokeLater(() -> {
                    updateUI();
                    //SwingUtilities.updateComponentTreeUI(GeminiTextPane.this);
                });
            }

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private static void copyText(String text) {

        if (text != null && !text.isEmpty()) {
            StringSelection stringSelection = new StringSelection(text);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
        }
    }

    public boolean awatingImage() {
        return lastClicked != null;
    }

    public void resetLastClicked() {
        lastClicked = null;
    }

    public boolean imageOnly() {
        return imageOnly;
    }

    private final ArrayList<MediaComponent> playerList = new ArrayList<>();
    private final ArrayList<ClickableRange> playerLinkList = new ArrayList<>();

    public void closePlayers() {
        for (MediaComponent ap : playerList) {
            ap.dispose();
        }
        playerList.clear();
        playerLinkList.clear();
    }

    // called when tab is closed - do this so players are not only disposed but hidden (in case tab is restored)
    public void closePlayerLinks() {
        playerLinkList.stream().forEach(cr -> {

            removeItemAtIndex(cr);
            if (cr != null) {
                cr.imageIndex = -1;
            }

        });
        playerList.clear();
        playerLinkList.clear();

    }

    public void pausePlayers() {
        for (MediaComponent mc : playerList) {
            mc.pause();
        }
    }

    public boolean closed;
    public boolean gopherHtml;

    // should only use this when inserting at the end since no code to offset links that follow
    private void insertComp(Component c, int pos) {
        SimpleAttributeSet apStyle = new SimpleAttributeSet();
        StyleConstants.setComponent(apStyle, c);

        try {
            doc.insertString(pos, " ", apStyle);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // Needed if user clicks a link, cancels that connection with ESC but 
    // then subsequently opens a bookmark for a media-only resource.
    // Without setting lastClicked to null, the media player opens as an embedded link.
    public void clearLastClicked() {
        lastClicked = null;
    }

    private int insertComp(Component c) {
        SimpleAttributeSet apStyle = new SimpleAttributeSet();
        StyleConstants.setComponent(apStyle, c);
        int res = -1;
        try {
            if (lastClicked == null) {

                doc.insertString(0, " ", apStyle);
                res = 0;
                imageOnly = true;
                if (c instanceof MediaComponent) {
                    playerLinkList.add(null);
                }
            } else {
                if (c instanceof MediaComponent) {
                    playerLinkList.add(lastClicked);
                }
                doc.insertString(lastClicked.end + 1, " ", apStyle);
                res = lastClicked.end + 1;
                doc.insertString(lastClicked.end + 2, "\n", null); //???

                lastClicked.imageIndex = lastClicked.end;
                boolean start = false;
                for (ClickableRange range : clickableRegions) {
                    if (range == lastClicked) {
                        start = true;
                        continue;
                    }
                    if (start) {
                        range.start = range.start + 2;
                        range.end = range.end + 2;
                        if (range.imageIndex != -1) {
                            range.imageIndex = range.imageIndex + 2;
                        }
                    }
                }
                headingMap.replaceAll((key, value) -> value > lastClicked.start ? value + 2 : value);
                lastClicked = null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return res;

    }

    public void insertMediaPlayer(String path, String mime, StreamSession session) {
        if (closed) { // tab closed while media downloading

            return;
        }
        inserting = true;
        Alhena.pauseMedia();

        MediaComponent ap;
        if (!Alhena.allowVLC && Alhena.playerCommand != null) {
            ap = new ExternalPlayer(f());
        } else {
            ap = mime.startsWith("audio") ? new AudioPlayer(session, path != null && session != null) : new VideoPlayer(session);
        }

        playerList.add(ap);
        insertComp((Component) ap);
        ap.start(path == null ? "http://localhost:" + Alhena.streamingPort + "/stream" : path);
        f().setBusy(false, page);

    }

    public static boolean isGif(byte[] data) {
        if (data.length < 6) {
            return false;
        }
        String sig = new String(data, 0, 6, java.nio.charset.StandardCharsets.US_ASCII);
        return "GIF87a".equals(sig) || "GIF89a".equals(sig);
    }

    public void insertImage(byte[] imageBytes, boolean curPos, boolean isSVG) {

        inserting = true;
        // if restoring from json saved state, use the saved width because actual width can't be computed
        // this is necessary for data urls when set to auto open
        float useContentWidth = savedContentWidth != null && savedContentWidth > 0.0 ? savedContentWidth : contentWidth;
        // 50 pixel fudge factor. Unable to land on a programmatic width insets plus scrollbar width, etc
        // that doesn't cause the horizontal scrollbar to appear
        int width = (int) useContentWidth - 50;
        BufferedImage image;
        ImageIcon icon = null;
        if (isSVG) {
            SVGLoader loader = new SVGLoader();
            try (ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes)) {
                LoaderContext ctx = LoaderContext.builder().build();

                SVGDocument svgDoc = loader.load(bais, null, ctx);
                if (svgDoc != null) {
                    FloatSize size = svgDoc.size();
                    BufferedImage svgImage = new BufferedImage((int) size.width, (int) size.height, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g = svgImage.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                    svgDoc.render(null, g);
                    g.dispose();
                    image = Util.getImage(null, width, width * 2, svgImage, false);
                    // TODO: needed (maybe)
                    if (image == null) {
                        f().setBusy(false, page);
                        return;
                    }
                    icon = new ImageIcon(image);
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (isGif(imageBytes) && !printing) {
            icon = new ImageIcon(imageBytes);
        } else {
            int scaleWidth = printing ? printWidth : width;
            image = Util.getImage(imageBytes, scaleWidth, scaleWidth * 2, null, false);

            if (image == null) {
                f().setBusy(false, page);
                return;
            }
            icon = new ImageIcon(image);
        }

        SimpleAttributeSet emojiStyle = new SimpleAttributeSet();
        StyleConstants.setIcon(emojiStyle, icon);
        try {
            if (curPos) {
                doc.insertString(doc.getLength(), " ", emojiStyle);
                doc.insertString(doc.getLength(), "\n", null);
            } else if (lastClicked == null) {
                doc.insertString(0, " ", emojiStyle);
                imageOnly = true;
                if (printing) {
                    f().printIt(this);
                }
            } else {

                doc.insertString(lastClicked.end + 1, " ", emojiStyle);
                doc.insertString(lastClicked.end + 2, "\n", null); //???

                lastClicked.imageIndex = lastClicked.end;
                boolean start = false;
                for (ClickableRange range : clickableRegions) {
                    if (range == lastClicked) {
                        start = true;
                        continue;
                    }
                    if (start) {
                        range.start = range.start + 2;
                        range.end = range.end + 2;
                        if (range.imageIndex != -1) {
                            range.imageIndex = range.imageIndex + 2;
                        }
                    }
                }
                headingMap.replaceAll((key, value) -> value > lastClicked.start ? value + 2 : value);

                lastClicked.openImage = true;
                lastClicked = null;
                if (printing) {
                    if (openQueue != null && openQueue.isEmpty()) {
                        f().printIt(this);
                    }
                    fetchLink(); // when printing, fetch the next open image

                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        f().setBusy(false, page);

    }

    public Float savedContentWidth;

    public String getFirstHeading() {
        return firstHeading;
    }

    private String createHeading() {
        if (pageBuffer == null) {
            return null;
        }

        int hIdx = pageBuffer.indexOf("#");
        if (hIdx != -1) {
            int idx = pageBuffer.indexOf("\n");
            if (idx > hIdx) {
                String heading = pageBuffer.substring(hIdx, pageBuffer.indexOf("\n", hIdx));
                // remove emojis using a regular expression
                heading = heading.replaceAll("[^\\p{L}\\p{N}\\p{P}\\p{Z}]", "");

                if (heading.startsWith("###")) {
                    return heading.substring(3).trim();
                } else if (heading.startsWith("##")) {
                    return heading.substring(2).trim();
                } else {
                    return heading.substring(1).trim();
                }
            }
        }
        return null;
    }

    public void resetSearch() {
        lastSearch = null;
        lastSearchIdx = 0;
        lastSearchDoc = -1;
    }

    public void find(String word, boolean recurse) {
        if (word.isBlank()) {
            getHighlighter().removeAllHighlights();
            for (PreformattedTextPane p : ptpList) {
                Highlighter highlighter = p.getHighlighter();
                highlighter.removeAllHighlights();
            }
            return;
        }
        boolean found = false;
        try {
            highlight(this, word.toLowerCase());
            for (int i = 0; i < ptpList.size(); i++) {
                highlight(ptpList.get(i), word.toLowerCase());
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
        try {
            int pos = -1;
            int startIdx = word.equals(lastSearch) ? lastSearchIdx : 0;
            if (lastSearchDoc == -1) {

                lastSearch = word;

                String text = doc.getText(0, doc.getLength()).toLowerCase();

                pos = text.indexOf(word.toLowerCase(), startIdx);
            }
            if (pos >= 0) {
                int finalPos = pos;

                requestFocusInWindow();
                setCaretPosition(finalPos);
                moveCaretPosition(finalPos + word.length());
                lastSearchIdx = finalPos + word.length();

                // scroll to the position
                Rectangle viewRect = modelToView2D(pos).getBounds();
                scrollRectToVisible(viewRect);
                found = true;
            } else {
                if (embedPF && !printing) {

                    int start = lastSearchDoc;
                    if (lastSearchDoc == -1) {
                        startIdx = 0;
                        start = 0;
                    }

                    for (int i = start; i < ptpList.size(); i++) {
                        PreformattedTextPane textPane = ptpList.get(i);
                        //String content = textPane.getText();
                        String content = textPane.getDocument().getText(0, textPane.getDocument().getLength());
                        int foundIndex = content.toLowerCase().indexOf(word.toLowerCase(), startIdx);

                        if (foundIndex != -1) {
                            setCaretPosition(getCaretPosition());
                            lastSearchDoc = i;
                            lastSearchIdx = foundIndex + word.length();
                            //textPane.requestFocus();
                            //textPane.setFocusable(true);
                            textPane.requestFocusInWindow();
                            textPane.setCaretPosition(foundIndex);
                            textPane.moveCaretPosition(foundIndex + word.length());

                            scrollToText(textPane, foundIndex);

                            found = true;
                            break;
                        }
                        startIdx = 0;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        if (!found && !recurse) {
            lastSearchDoc = -1;
            lastSearchIdx = 0;
            find(word, true);
        }
    }

    public void scrollToHeading(String heading) {
        int pos = headingMap.get(heading);

        setCaretPosition(pos);
        EventQueue.invokeLater(() -> {
            try {
                Rectangle caretRect = modelToView2D(pos).getBounds();
                Container parent = getParent();
                if (parent instanceof JViewport viewport) {

                    int targetY = Math.max(0, caretRect.y);
                    viewport.setViewPosition(new Point(0, targetY));
                }
            } catch (BadLocationException e) {

            }
        });
    }

    public static void highlight(JTextPane textPane, String pattern) throws BadLocationException {
        Highlighter highlighter = textPane.getHighlighter();
        highlighter.removeAllHighlights();

        String text = textPane.getDocument().getText(0, textPane.getDocument().getLength()).toLowerCase();
        int pos = 0;
        while ((pos = text.indexOf(pattern, pos)) >= 0) {
            highlighter.addHighlight(pos, pos + pattern.length(),
                    new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW));
            pos += pattern.length();
        }
    }

    public void scrollToText(JTextPane embeddedTextPane, int foundPosition) {
        try {

            Rectangle textRect = embeddedTextPane.modelToView(foundPosition);

            Rectangle scrollRect = SwingUtilities.convertRectangle(
                    embeddedTextPane,
                    textRect,
                    scrollPane.getViewport().getView()
            );

            scrollRect.grow(20, 20);

            EventQueue.invokeLater(() -> { // need this so document scrolls in all situations
                scrollRectToVisible(scrollRect);
            });

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    public void end() {

        if (page.getTitanEdited() && !docURL.endsWith(";edit")) { // docURL test in case user cancels
            docURL += ";edit";
        }

        if (bufferedLine != null) {
            String lrl = bufferedLine;
            bufferedLine = null;
            processLine(lrl);
        }
        // hack to fix inline image positioning when an image link is the very last line in the document
        if (doc != null) {
            processLine(" \n");
        }
        JTabbedPane tabbedPane = page.frame().tabbedPane;
        firstHeading = createHeading();
        String title = f().createTitle(docURL, firstHeading);
        if (title != null) {
            if (tabbedPane != null) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {

                    if (tabbedPane.getComponentAt(i) == page) {

                        if (page == tabbedPane.getSelectedComponent()) {
                            f().updateComboBox(docURL);
                            f().setTitle(title);
                        } else {

                            tabbedPane.setTitleAt(i, title);
                        }
                        break;
                    }
                }
            } else if (page.isVisible()) {

                if (currentMode != INFO_MODE) {
                    f().updateComboBox(docURL);
                    f().setTitle(title);
                }
            }
        }
        if (pageBuffer != null) {
            pageBuffer.trimToSize();
        }
        if (!printing && (plainTextMode || !embedPF)) {
            scanForAnsi();
        }
        bStyle = null;
        foregroundHandling = false;
        //defPP = null;
        page.doneLoading();

        page.setBusy(false);
        requestFocusInWindow();
    }

    private void scanForAnsi() {

        StringBuilder localBuilder = pageBuffer;
        if (!hasAnsi && localBuilder != null) {
            Thread.ofVirtual().start(() -> {

                for (int i = 0; i < localBuilder.length(); i++) {
                    if (localBuilder.charAt(i) == 27) {
                        EventQueue.invokeLater(() -> {
                            if (GeminiTextPane.this.isShowing()) {
                                Runnable r = () -> {
                                    hasAnsi = true;
                                    f().refreshFromCache(page);
                                };
                                if (GeminiFrame.ansiAlert) {
                                    Object res = Util.confirmDialog(f(), I18n.t("ansiDialog"), I18n.t("ansiDialogMsg"), JOptionPane.YES_NO_OPTION, null, null);
                                    if (res instanceof Integer result) {
                                        if (result == JOptionPane.YES_OPTION) {
                                            r.run();
                                        }
                                    }
                                } else {
                                    r.run();
                                }
                            }
                        });
                        break;
                    }
                }

            });
        }
    }

    // for one and done messages
    public void end(String geminiDoc, boolean pfMode, String docURL, boolean newRequest) {
        if (printing) {
            return;
        }
        end(geminiDoc, pfMode, docURL, newRequest, false);
    }

    private boolean printing;

    public void end(String geminiDoc, boolean pfMode, String docURL, boolean newRequest, boolean printing) {
        this.printing = printing;
        if (printing) {
            //setBackground(Color.WHITE);
            hasAnsi = true;
        }
        updatePage(geminiDoc, pfMode, docURL, newRequest);

        end();
    }
    private ParserFactory factory;
    public boolean hasAnsi;

    private void handleAnsi(String line) {
        if (factory == null) {
            ParserFactoryConfig config = new ParserFactoryConfig();
            config.setEnvironment(Environment._8_BIT);
            config.setFunctionTypes(List.of(ControlFunctionType.C0_SET, ControlFunctionType.C1_SET));
            ParserFactoryService factoryService = new ParserFactoryProvider();
            factory = factoryService.createFactory(config);
        }

        if (bStyle == null) {
            // copy the preformat style
            bStyle = new SimpleAttributeSet(doc.getStyle("```"));
            if (Util.isLight(getBackground())) {
                ansiFG(Color.BLACK);
            } else {
                ansiFG(Color.WHITE);  // "default foreground color"
            }
        }

        var parser = factory.createParser(line);
        Fragment fragment;
        while ((fragment = parser.parse()) != null) {
            if (fragment.getType() == FragmentType.TEXT) {
                TextFragment textFragment = (TextFragment) fragment;
                convert(textFragment.getText());
            }
        }

    }

    private SimpleAttributeSet bStyle;
    private boolean foregroundHandling;

    private void plainText(String txt) {
        if (foregroundHandling) {
            StyleConstants.setForeground(bStyle, getForeground());
        }
        try {
            doc.insertString(doc.getLength(), txt, bStyle);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void convert(String txt) {

        if (!txt.startsWith("[")) {
            plainText(txt);

        } else {
            int mIdx = txt.indexOf('m');
            if (mIdx == -1) {
                plainText(txt);
                return;
            }
            String line = txt.substring(mIdx + 1);

            String ansi = txt.substring(1, mIdx);

            String[] tokens = ansi.split(";");
            outer:
            for (String token : tokens) {
                switch (token) {
                    case "38" -> {
                        // foreground color
                        if (tokens[1].equals("5")) {

                            if (tokens.length == 4) { // no idea why originally put this here - corner case example?
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiFG(c);

                            } else if (tokens.length == 3) {

                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiFG(c);

                            }

                        } else if (tokens[1].equals("2")) {
                            int r = Integer.parseInt(tokens[2]);
                            int g = Integer.parseInt(tokens[3]);
                            int b = Integer.parseInt(tokens[4]);
                            ansiFG(new Color(r, g, b));

                        }
                        break outer;
                    }
                    case "30" ->
                        ansiFG(AnsiColor.BLACK);
                    case "31" ->
                        ansiFG(AnsiColor.RED);
                    case "32" ->
                        ansiFG(AnsiColor.GREEN);
                    case "33" ->
                        ansiFG(AnsiColor.YELLOW);
                    case "34" ->
                        ansiFG(AnsiColor.BLUE);
                    case "35" ->
                        ansiFG(AnsiColor.MAGENTA);
                    case "36" ->
                        ansiFG(AnsiColor.CYAN);
                    case "37" ->
                        ansiFG(AnsiColor.WHITE);
                    case "39" ->
                        ansiFG(AnsiColor.WHITE); // "Default" fg color
                    case "40" ->
                        ansiBG(Color.BLACK);
                    case "41" ->
                        ansiBG(AnsiColor.RED);
                    case "42" ->
                        ansiBG(AnsiColor.GREEN);
                    case "43" ->
                        ansiBG(AnsiColor.YELLOW);
                    case "44" ->
                        ansiBG(AnsiColor.BLUE);
                    case "45" ->
                        ansiBG(AnsiColor.MAGENTA);
                    case "46" ->
                        ansiBG(AnsiColor.CYAN);
                    case "47" ->
                        ansiBG(AnsiColor.WHITE);
                    case "48" -> {
                        // foreground color
                        if (tokens[1].equals("5")) {

                            if (tokens.length == 4) { // why is this here?
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiBG(c);

                            } else if (tokens.length == 3) {
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiBG(c);

                            }
                        } else if (tokens[1].equals("2")) {
                            int r = Integer.parseInt(tokens[2]);
                            int g = Integer.parseInt(tokens[3]);
                            int b = Integer.parseInt(tokens[4]);
                            ansiBG(new Color(r, g, b));
                        }
                        break outer;
                    }
                    case "49" ->
                        ansiBG(Color.BLACK); //"default" background
                    case "90" ->
                        ansiFG(AnsiColor.BRIGHT_BLACK);
                    case "91" ->
                        ansiFG(AnsiColor.BRIGHT_RED);
                    case "92" ->
                        ansiFG(AnsiColor.BRIGHT_GREEN);
                    case "93" ->
                        ansiFG(AnsiColor.BRIGHT_YELLOW);
                    case "94" ->
                        ansiFG(AnsiColor.BRIGHT_BLUE);
                    case "95" ->
                        ansiFG(AnsiColor.BRIGHT_MAGENTA);
                    case "96" ->
                        ansiFG(AnsiColor.BRIGHT_CYAN);
                    case "97" ->
                        ansiFG(AnsiColor.BRIGHT_WHITE);
                    case "100" ->
                        ansiBG(AnsiColor.BRIGHT_BLACK);
                    case "101" ->
                        ansiBG(AnsiColor.BRIGHT_RED);
                    case "102" ->
                        ansiBG(AnsiColor.BRIGHT_GREEN);
                    case "103" ->
                        ansiBG(AnsiColor.BRIGHT_YELLOW);
                    case "104" ->
                        ansiBG(AnsiColor.BRIGHT_BLUE);
                    case "105" ->
                        ansiBG(AnsiColor.BRIGHT_MAGENTA);
                    case "106" ->
                        ansiBG(AnsiColor.BRIGHT_CYAN);
                    case "107" ->
                        ansiBG(AnsiColor.BRIGHT_WHITE);
                    case "0" -> {

                        // setting the forground on blank strings causes a layout change that breaks
                        // the no line wrap on preformatted text
                        if (!line.isBlank()) {
                            StyleConstants.setForeground(bStyle, getForeground());
                        } else {
                            foregroundHandling = true;

                        }

                        StyleConstants.setBackground(bStyle, new Color(0, 0, 0, 0));
                        StyleConstants.setBold(bStyle, false);

                    }
                    case "1" -> {
                        StyleConstants.setBold(bStyle, true);
                    }
                    case "2" -> {
                        // not really faint - could lighten or darken depending on theme but then would have to track for reset w/22
                        StyleConstants.setBold(bStyle, false);
                    }
                    case "22" -> { //normal intensity
                        StyleConstants.setBold(bStyle, false);
                    }
                    // default ->
                    //     System.out.println("unknown: " + txt);
                }
            }

            try {

                if (foregroundHandling && !line.isBlank()) {
                    StyleConstants.setForeground(bStyle, getForeground());
                }

                doc.insertString(doc.getLength(), line, bStyle);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void ansiFG(Color c) {
        StyleConstants.setForeground(bStyle, AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d));
        foregroundHandling = false;
    }

    private void ansiBG(Color c) {
        StyleConstants.setBackground(bStyle, AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d));
    }

    private String preRedirectUrl;

    public void setPreRedirectUrl(String url) {
        preRedirectUrl = url;
    }

    public void updatePage(String geminiDoc, boolean pfMode, String docURL, boolean newRequest) {

        if (page.isGopherTLS()) {
            docURL = docURL.replace("gopher:/", "gophers:/");
        }
        if (!docURL.equals(this.docURL) && newRequest) {

            if (currentMode == DEFAULT_MODE)
            try {

                if (preRedirectUrl != null) {
                    // this is a kludge to mark redirected gemlog feeds as read!
                    DB.markUrlRead(preRedirectUrl, true);
                    preRedirectUrl = null;
                }

                DB.insertHistory(docURL, null);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
        if (docURL.equals(GeminiFrame.SUBSCRIPTION_LABEL)) {
            currentMode = SUBSCRIPTION_MODE;
        } else if (docURL.equals(GeminiFrame.FEEDS_LABEL)) {
            currentMode = FEED_MODE;
        } else if (docURL.equals(GeminiFrame.HISTORY_LABEL)) {
            currentMode = HISTORY_MODE;
        } else if (docURL.equals(GeminiFrame.BOOKMARK_LABEL)) {
            currentMode = BOOKMARK_MODE;
        } else if (docURL.equals(GeminiFrame.CERT_LABEL)) {
            currentMode = CERT_MODE;
        } else if (docURL.equals(GeminiFrame.INFO_LABEL)) {
            currentMode = INFO_MODE;
        } else if (docURL.equals(GeminiFrame.SERVERS_LABEL)) {
            currentMode = SERVER_MODE;
        } else if (docURL.equals(GeminiFrame.STYLES_LABEL)) {
            currentMode = STYLE_MODE;
        }

        this.docURL = docURL;
        GeminiFrame gf = f();
        if (gf != null) {
            gf.refreshNav(null);
        }

        pageBuffer = new StringBuilder();

        bufferedLine = null; // probably not necessary here

        originalPfMode = preformattedMode = pfMode;
        plainTextMode = pfMode;
        // map to track clickable regions and their actions
        clickableRegions.clear();
        headingMap.clear();
        feedVisitedList.clear();
        saveRange = null;
        currentCursor = Cursor.DEFAULT_CURSOR;
        currentStatus = null;
        doc = new DefaultStyledDocument();

        setStyledDocument(doc);
        buildStyles();
        ptpList = new ArrayList<>();
        //defPP = new SimpleAttributeSet();

        applyCenteredParagraphStyle();
        addPage(geminiDoc);

    }

    private static int totalWidth;

    private void applyCenteredParagraphStyle() {

        JViewport viewport = (JViewport) getParent();

        int width = viewport.getWidth();

        if (width > 0) {
            totalWidth = width;
        }
        if (totalWidth <= 0) {
            return;
        }

        float cp = printing ? 1.0f : pageStyle.getContentPercentage();
        contentWidth = totalWidth * cp;
        indent = (int) ((totalWidth - contentWidth) / 2f);

        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setLeftIndent(attrs, indent);
        StyleConstants.setRightIndent(attrs, indent);
        StyleConstants.setForeground(attrs, getForeground());

        doc.setParagraphAttributes(0, doc.getLength(), attrs, false);
    }

    private float contentWidth;

    public float getContentWidth() {
        return contentWidth;
    }

    public void addPage(String geminiDoc) {

        if (pageBuffer == null) {
            return;
        }
        pageBuffer.append(geminiDoc);

        if (bufferedLine != null) {
            geminiDoc = bufferedLine + geminiDoc;
            bufferedLine = null;
        }
        if (geminiDoc.endsWith("\n")) {

            geminiDoc.lines().forEach(line -> processLine(line)); // no way to know if a line is the last line

        } else {
            int lastNl = geminiDoc.lastIndexOf("\n");
            if (lastNl == -1) {
                // no newlines at all save
                bufferedLine = geminiDoc;
            } else {
                bufferedLine = geminiDoc.substring(lastNl + 1);
                geminiDoc.substring(0, lastNl + 1).lines().forEach(line -> {
                    processLine(line); // no way to know if a line is the last line
                });
            }

        }

        if (page != null) {
            page.loading();
        }

    }

    private String emojiProportional;
    private int customFontSize;
    private boolean isDark;

    // override for custom screens - used by embedded PreformattedTextPane
    public void setCustomFontSize(int fs) {
        customFontSize = fs;
    }

    public static PageTheme getDefaultTheme() {
        return getDefaultTheme(UIManager.getDefaults());
    }

    public static PageTheme getDefaultTheme(UIDefaults ui) {
        PageTheme pageTheme = new PageTheme();
        Color bg = ui.getColor("TextPane.inactiveBackground");
        pageTheme.setPageBackground(bg);
        pageTheme.setContentPercentage(contentPercentage);
        boolean isDark = !Util.isLight(bg);
        Color lc = ui.getColor("Component.linkColor");
        pageTheme.setLinkColor(lc);
        pageTheme.setSpinnerColor(lc);
        pageTheme.setLinkStyle(Font.PLAIN);
        pageTheme.setVisitedLinkColor(isDark ? lc.darker() : lc.brighter());
        pageTheme.setHoverColor(pageTheme.getLinkColor().brighter());
        pageTheme.setMonoFontSize(GeminiFrame.monoFontSize);
        pageTheme.setMonoFontFamily(monospacedFamily);
        pageTheme.setMonoFontColor(ui.getColor("TextPane.foreground"));
        pageTheme.setTextForeground(ui.getColor("TextPane.foreground"));
        pageTheme.setFontStyle(Font.PLAIN);
        pageTheme.setQuoteForeground(pageTheme.getTextForeground());
        pageTheme.setQuoteStyle(Font.ITALIC);
        pageTheme.setFontSize(GeminiFrame.fontSize);
        pageTheme.setHeader1Color(AnsiColor.adjustColor(isDark ? lc.brighter() : lc.darker(), isDark, .1, .9, .2));
        pageTheme.setHeader1Style(Font.PLAIN);
        pageTheme.setHeader2Color(pageTheme.getHeader1Color());
        pageTheme.setHeader2Style(Font.PLAIN);
        pageTheme.setHeader3Color(pageTheme.getHeader1Color());
        pageTheme.setHeader3Style(Font.PLAIN);
        pageTheme.setHeader1Size(pageTheme.getFontSize() + 3);
        pageTheme.setHeader2Size(pageTheme.getFontSize() + 9);
        pageTheme.setHeader3Size(pageTheme.getFontSize() + 17);
        pageTheme.setLinkSize(pageTheme.getFontSize());
        pageTheme.setQuoteSize(pageTheme.getFontSize());
        pageTheme.setFontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setHeader1FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setHeader2FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setHeader3FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setLinkFontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setQuoteFontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setListFont(GeminiFrame.proportionalFamily);
        pageTheme.setLinkUnderline(false);
        pageTheme.setQuoteUnderline(false);
        pageTheme.setFontUnderline(false);
        pageTheme.setHeader1Underline(false);
        pageTheme.setHeader2Underline(false);
        pageTheme.setHeader3Underline(false);
        pageTheme.setListStyle(Font.PLAIN);
        pageTheme.setListColor(ui.getColor("TextPane.foreground"));
        pageTheme.setListUnderline(false);
        pageTheme.setListFontSize(GeminiFrame.fontSize);
        pageTheme.setGradientBG(Alhena.gradientBG);
        // pageTheme.setGradient1Color((Color) null);
        // pageTheme.setGradient2Color((Color) null);
        return pageTheme;
    }

    public Integer styleId;

    public PageTheme getPageStyle() {
        String dbTheme = Alhena.theme;

        StyleInfo dbStyle = null;
        try {
            URI u = getURI();
            if (u != null) {
                dbStyle = DB.getStyle(docURL, u.getAuthority(), u.getScheme(), dbTheme, !UIManager.getBoolean("laf.dark"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        JsonObject customTheme;
        if (dbStyle != null) {
            customTheme = new JsonObject(dbStyle.style());
            styleId = dbStyle.id();
        } else {
            customTheme = new JsonObject();
            styleId = null;
        }
        PageTheme pageTheme = getDefaultTheme(UIManager.getDefaults());
        pageTheme.fromJson(customTheme);

        return pageTheme;

    }

    protected PageTheme pageStyle;

    private void buildStyles() {
        pageStyle = getPageStyle();
        isDark = !Util.isLight(pageStyle.getPageBackground());
        // set to transparent because Dracula theme overrides gradient paint
        setBackground(new Color(0, 0, 0, 0));

        setForeground(pageStyle.getMonoFontColor());

        for (MediaComponent ap : playerList) {
            ap.dispose();
        }
        playerList.clear();
        playerLinkList.clear();

        emojiProportional = "Noto Emoji";
        if (SystemInfo.isMacOS) {
            if (!Alhena.macUseNoto) {
                emojiProportional = "SansSerif";
            }
        }

        linkColor = pageStyle.getLinkColor();
        hoverColor = pageStyle.getHoverColor();

        int gfMonoFontSize = ViewBasedTextPanePrinter.getMonospacedPrintSize(printing, pageStyle.getMonoFontSize());
        Style pfStyle = doc.addStyle("```", null);
        StyleConstants.setFontFamily(pfStyle, pageStyle.getMonoFontFamily());
        StyleConstants.setFontSize(pfStyle, gfMonoFontSize);
        StyleConstants.setForeground(pfStyle, pageStyle.getMonoFontColor());

        Style h1Style = doc.addStyle("###", null);
        StyleConstants.setFontFamily(h1Style, pageStyle.getHeader1FontFamily());
        StyleConstants.setFontSize(h1Style, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE + 3 : pageStyle.getHeader1Size());
        StyleConstants.setBold(h1Style, (pageStyle.getHeader1Style() & Font.BOLD) != 0);
        StyleConstants.setItalic(h1Style, (pageStyle.getHeader1Style() & Font.ITALIC) != 0);
        StyleConstants.setUnderline(h1Style, pageStyle.getHeader1Underline());
        StyleConstants.setForeground(h1Style, pageStyle.getHeader1Color());

        Style h2Style = doc.addStyle("##", h1Style);
        StyleConstants.setUnderline(h2Style, pageStyle.getHeader2Underline());
        StyleConstants.setFontFamily(h2Style, pageStyle.getHeader2FontFamily());
        StyleConstants.setForeground(h2Style, pageStyle.getHeader2Color());
        StyleConstants.setBold(h2Style, (pageStyle.getHeader2Style() & Font.BOLD) != 0);
        StyleConstants.setItalic(h2Style, (pageStyle.getHeader2Style() & Font.ITALIC) != 0);
        StyleConstants.setFontSize(h2Style, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE + 9 : pageStyle.getHeader2Size()); // 24

        Style h3Style = doc.addStyle("#", h1Style);
        StyleConstants.setUnderline(h3Style, pageStyle.getHeader3Underline());
        StyleConstants.setForeground(h3Style, pageStyle.getHeader3Color());
        StyleConstants.setFontFamily(h3Style, pageStyle.getHeader3FontFamily());
        StyleConstants.setBold(h3Style, (pageStyle.getHeader3Style() & Font.BOLD) != 0);
        StyleConstants.setItalic(h3Style, (pageStyle.getHeader3Style() & Font.ITALIC) != 0);
        StyleConstants.setFontSize(h3Style, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE + 17 : pageStyle.getHeader3Size()); // 32

        Style linkStyle;
        if (page.isNex() || page.isGopher()) {
            linkStyle = doc.addStyle("=>", h1Style);
            StyleConstants.setFontFamily(linkStyle, monospacedFamily);
            StyleConstants.setFontSize(linkStyle, gfMonoFontSize);
        } else {
            linkStyle = doc.addStyle("=>", h1Style);
            StyleConstants.setUnderline(linkStyle, pageStyle.getLinkUnderline());
            StyleConstants.setFontFamily(linkStyle, pageStyle.getLinkFontFamily());
            StyleConstants.setBold(linkStyle, (pageStyle.getLinkStyle() & Font.BOLD) != 0);
            StyleConstants.setItalic(linkStyle, (pageStyle.getLinkStyle() & Font.ITALIC) != 0);
            StyleConstants.setFontSize(linkStyle, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE : pageStyle.getLinkSize());

        }
        StyleConstants.setForeground(linkStyle, linkColor);

        Style clickedStyle = doc.addStyle("visited", linkStyle);
        Color visitColor = pageStyle.getVisitedLinkColor();
        StyleConstants.setForeground(clickedStyle, visitColor);

        Style quoteStyle = doc.addStyle(">", h1Style);
        StyleConstants.setUnderline(quoteStyle, pageStyle.getQuoteUnderline());
        StyleConstants.setFontFamily(quoteStyle, pageStyle.getQuoteFontFamily());
        StyleConstants.setFontSize(quoteStyle, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE : pageStyle.getQuoteSize());
        StyleConstants.setBold(quoteStyle, (pageStyle.getQuoteStyle() & Font.BOLD) != 0);
        StyleConstants.setItalic(quoteStyle, (pageStyle.getQuoteStyle() & Font.ITALIC) != 0);
        // StyleConstants.setItalic(quoteStyle, true);
        StyleConstants.setForeground(quoteStyle, pageStyle.getQuoteForeground());
        //TODO: ADJUST TEXT AND LIST SIZES FOR PRINTING?
        Style textStyle = doc.addStyle("text", h1Style);
        StyleConstants.setUnderline(textStyle, pageStyle.getFontUnderline());
        StyleConstants.setFontFamily(textStyle, pageStyle.getFontFamily());
        StyleConstants.setFontSize(textStyle, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE : pageStyle.getFontSize());
        StyleConstants.setBold(textStyle, (pageStyle.getFontStyle() & Font.BOLD) != 0);
        StyleConstants.setItalic(textStyle, (pageStyle.getFontStyle() & Font.ITALIC) != 0);
        //StyleConstants.setBold(textStyle, false);
        StyleConstants.setForeground(textStyle, pageStyle.getTextForeground());

        Style listStyle = doc.addStyle("*", textStyle);
        StyleConstants.setUnderline(listStyle, pageStyle.getListUnderline());
        StyleConstants.setFontFamily(listStyle, pageStyle.getListFont());
        StyleConstants.setFontSize(listStyle, printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE : pageStyle.getListFontSize());
        StyleConstants.setBold(listStyle, (pageStyle.getListStyle() & Font.BOLD) != 0);
        StyleConstants.setItalic(listStyle, (pageStyle.getListStyle() & Font.ITALIC) != 0);
        StyleConstants.setForeground(listStyle, pageStyle.getListColor());
        //doc.addStyle("*", textStyle);

        hoverStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(hoverStyle, hoverColor);

        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, linkColor);

        visitedStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(visitedStyle, visitColor);
        Color liColor = pageStyle.getPageBackground();
        if (Alhena.linkIcons && (!liColor.equals(linkIconBG) || dataIcon == null)) {
            linkIconBG = liColor;
            rebuildLinkIcons(pageStyle.getFontSize(), liColor, linkColor);
        }
    }

    // get background color for printing
    public Color getPageBackground() {
        return pageStyle.getPageBackground();
    }

    private static Color linkIconBG;

    // only call on EDT
    private static void rebuildLinkIcons(int gfFontSize, Color bgColor, Color linkColor) {
        String iconFont = "Noto Emoji";
        dataIcon = getLinkIcon("📎", iconFont, gfFontSize, linkColor);
        mailIcon = getLinkIcon("✉️", iconFont, gfFontSize, linkColor);
        geminiIcon = getLinkIcon("♊️", iconFont, gfFontSize, linkColor);
        otherIcon = getLinkIcon("🌐", iconFont, gfFontSize, linkColor);
        titanIcon = getLinkIcon("✏️", iconFont, gfFontSize, linkColor);
        picIcon = getLinkIcon("📸", iconFont, gfFontSize, linkColor);
        videoIcon = getLinkIcon("🎥", iconFont, gfFontSize, linkColor);
        audioIcon = getLinkIcon("🎧", iconFont, gfFontSize, linkColor);
        gopherIcon = getLinkIcon("🐹", iconFont, gfFontSize, linkColor);
        spartanIcon = getLinkIcon("💪", iconFont, gfFontSize, linkColor);
        nexIcon = getLinkIcon("🚄", iconFont, gfFontSize, linkColor);
        fileIcon = getLinkIcon("💾", iconFont, gfFontSize, linkColor);

    }

    public static void clearLinkIcons() {
        dataIcon = mailIcon = geminiIcon = titanIcon = null;
    }

    private boolean checkScrollingNeeded(JScrollPane sp) {
        JViewport viewport = sp.getViewport();
        Dimension viewSize = viewport.getViewSize();

        return viewSize.getWidth() > contentWidth;
    }

    private void processLine(String line) {

        if (page.isGopher() || (page.isNex() && docURL.endsWith("/"))) {

            if (line.startsWith("=>")) {
                String ll = line.substring(2).trim();

                int i;
                for (i = 0; i < ll.length(); i++) {
                    if (Character.isWhitespace(ll.charAt(i))) {
                        break;
                    }
                }
                String url = ll.substring(0, i);

                String finalUrl = url;
                String label = ll.substring(i).trim();

                String linkStyle = f().isClickedLink(url) ? "visited" : "=>";

                ClickableRange cr = addStyledText(label.isEmpty() ? url : label, linkStyle,
                        () -> {

                            if (Alhena.httpProxy == null && finalUrl.startsWith("http") && Alhena.browsingSupported && Alhena.useBrowser) {
                                try {
                                    Desktop.getDesktop().browse(new URI(finalUrl));
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            } else if (finalUrl.startsWith("mailto:") && Alhena.mailSupported) {
                                try {
                                    Desktop.getDesktop().mail(new URI(finalUrl));
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }

                            } else {
                                f().addClickedLink(finalUrl);
                                f().fetchURL(finalUrl, false, null);
                            }

                        });
                cr.url = url;

            } else {
                addStyledText(line, "```", null);
            }
            return;
        }

        if (line.startsWith("```") && !plainTextMode) {
            preformattedMode = !preformattedMode;
            if (!preformattedMode) {
                foregroundHandling = false;
                bStyle = null;
                if (asciiSB != null && !asciiSB.isEmpty()) {
                    asciiSB.deleteCharAt(asciiSB.length() - 1);

                    BufferedImage bi = AsciiImage.renderTextToImage(shadePF, asciiSB.toString(), pageStyle.getMonoFontFamily(), pageStyle.getMonoFontSize(), pageStyle.getMonoFontColor(), pageStyle.getPageBackground(), false);
                    ImageIcon icon = new ImageIcon(bi);
                    if (ptp == null) {
                        insertComp(new JLabel(icon), doc.getLength());
                    } else {
                        // insert JLabel into ptp
                        ptp.insertComp(new JLabel(icon));
                        ptp.scrollLeft();
                        ptp = null;
                    }
                    asciiSB = null;
                }
                if (ptp != null) {

                    ptp.end();
                    // removeLastChar causes a rendering error on large pf blocks (notably ascii art)
                    //ptp.removeLastChar();
                    ptp.scrollLeft();
                    ptp = null;
                } else {
                    // use else clause to compensate for spacing when ptp.removeLastChar() commented out above
                    addStyledText("\n", "```", null);
                }

            } else { // preformatted mode

                if (asciiImage && !printing) {
                    if (!embedPF) {
                        addStyledText("", "```", null);
                    }
                    asciiSB = new StringBuilder();
                }
                if (embedPF && !printing) {

                    addStyledText("\n", "```", null);
                    ptp = createTextComponent(true);

                } else {
                    addStyledText("\n", "```", null);
                }
            }

        } else if (preformattedMode) {
            // huh? Refresh yourself
            if (DIRECTIVE_MODES.contains(currentMode) && line.startsWith("=>")) {
                line = "=> " + line.substring(line.indexOf(":") + 1);
            }
            if (ptp != null) {
                if (asciiSB != null) {
                    asciiSB.append(line).append('\n');
                } else {
                    ptp.addText(line + "\n");
                }
            } else {
                if (asciiSB != null) {
                    asciiSB.append(line).append('\n');
                } else {
                    addStyledText(line, "```", null);
                }
            }
        } else if (line.startsWith("###")) {
            int hstart = doc.getLength();
            String hl = line.substring(3).trim();
            addStyledText(hl, "###", null);
            headingMap.put(hl, hstart);
        } else if (line.startsWith("##")) {
            int hstart = doc.getLength();
            String hl = line.substring(2).trim();
            addStyledText(hl, "##", null);
            headingMap.put(hl, hstart);
        } else if (line.startsWith("#")) {
            int hstart = doc.getLength();
            String hl = line.substring(1).trim();
            addStyledText(hl, "#", null);
            headingMap.put(hl, hstart);
        } else if (line.startsWith("=>") || (page.isSpartan() && line.startsWith("=: "))) {
            String ll = line.substring(2).trim();
            boolean spartanLink = line.startsWith("=: ");
            int i;
            for (i = 0; i < ll.length(); i++) {
                if (Character.isWhitespace(ll.charAt(i))) {
                    break;
                }
            }
            String url = ll.substring(0, i);
            String[] directive = {null};
            if (DIRECTIVE_MODES.contains(currentMode)) {
                int cIdx = url.indexOf(":");
                directive[0] = url.substring(0, cIdx);
                url = url.substring(cIdx + 1);
                if (currentMode == CERT_MODE) {
                    url = "gemini://" + url;
                }
            }
            boolean dataUrl = url.startsWith("data:");

            String finalUrl = url;
            String label;
            String sfx = "";

            if (Alhena.linkIcons && currentMode != CERT_MODE && currentMode != STYLE_MODE) {

                boolean isImage = Alhena.imageExtensions.stream().anyMatch(url.toLowerCase()::endsWith);
                boolean isVideo = false;
                boolean isAudio = false;
                if (!isImage) {
                    if (url.toLowerCase().endsWith(".opus")) {
                        isAudio = true;
                    } else {
                        String mimeExt = MimeMapping.getMimeTypeForFilename(finalUrl);

                        if (mimeExt != null) {
                            isVideo = mimeExt.startsWith("video");
                            isAudio = mimeExt.startsWith("audio");

                        }
                    }
                }

                if (isImage) {
                    sfx = "🌠";
                } else if (isVideo) {
                    sfx = "🎥";
                } else if (isAudio) {
                    sfx = "🎧";
                } else if (finalUrl.indexOf("://") == -1) {
                    if (finalUrl.startsWith("data")) {
                        sfx = "📎";
                    } else if (finalUrl.startsWith("mailto")) {
                        sfx = "✉️";
                    } else {
                        if (docURL.startsWith("spartan")) {
                            sfx = "💪";
                        } else if (docURL.startsWith("gemini")) {
                            sfx = "🔗";
                        } else if (docURL.startsWith("gopher")) {
                            sfx = "🐭";
                        } else if (docURL.startsWith("nex")) {
                            sfx = "🚄";
                        } else if (docURL.startsWith("file")) {
                            sfx = "💾";
                        } else {
                            sfx = "🌐";
                        }
                        //sfx = !docURL.startsWith("gemini") ? "🌐" : "🔗";
                    }
                } else {
                    if (finalUrl.startsWith("titan")) {
                        sfx = "✏️";
                    } else if (finalUrl.startsWith("spartan")) {
                        sfx = "💪";
                    } else if (finalUrl.startsWith("gemini")) {
                        sfx = "🔗";
                    } else if (finalUrl.startsWith("gopher")) {
                        sfx = "🐭";
                    } else if (finalUrl.startsWith("nex")) {
                        sfx = "🚄";
                    } else if (finalUrl.startsWith("file")) {
                        sfx = "💾";
                    } else {
                        sfx = "🌐";
                    }
                    //sfx = !finalUrl.startsWith("gemini") ? "🌐" : "🔗";

                }
            }
            label = ll.substring(i).trim();

            String feedAppend = "";
            if (currentMode == FEED_MODE) {

                int visited = Integer.parseInt(directive[0].split(",", 4)[2]);
                if (visited == 1) {

                    if (directive[0].endsWith("#")) {
                        feedAppend = "#" + label;
                        feedVisitedList.add(url + feedAppend);

                    } else {
                        feedVisitedList.add(url);
                    }
                }
            }
            String linkStyle = (currentMode != FEED_MODE && f().isClickedLink(url)) || feedVisitedList.contains(url + feedAppend) ? "visited" : "=>";

            ClickableRange cr = addStyledText(label.isEmpty() ? sfx + url.replace("/", "/\u200B") : sfx + label, linkStyle,
                    () -> {

                        if (spartanLink) {

                            TextEditor textEditor = new TextEditor("", false, GeminiTextPane.this);
                            Object[] comps = new Object[1];
                            comps[0] = textEditor;
                            Object res = Util.inputDialog2(f(), "Edit", comps, null, true);
                            if (res != null) {
                                Object result = textEditor.getResult();
                                if (result instanceof String string) {
                                    if (!string.isBlank()) {
                                        f().addClickedLink(finalUrl);
                                        f().fetchURL(finalUrl + "?" + Util.uEncode(string), false, null);
                                    }
                                } else {
                                    f().addClickedLink(finalUrl);
                                    f().fetchURL(finalUrl, (File) result, false, null, null);
                                }
                            }

                        } else if (Alhena.httpProxy == null && finalUrl.startsWith("http") && Alhena.browsingSupported && Alhena.useBrowser) {
                            try {
                                Desktop.getDesktop().browse(new URI(finalUrl));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        } else if (finalUrl.startsWith("mailto:") && Alhena.mailSupported) {
                            try {
                                Desktop.getDesktop().mail(new URI(finalUrl));
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }

                        } else if (currentMode == CERT_MODE) {

                            int id = Integer.parseInt(directive[0].substring(0, directive[0].indexOf(",")));
                            boolean active = directive[0].substring(directive[0].indexOf(",") + 1).equals("true");
                            f().toggleCert(id, !active, finalUrl);
                        } else if (currentMode == STYLE_MODE) {
                            Util.showStyleEditor(f(), Integer.parseInt(directive[0]));
                        } else if (gopherHtml) {
                            String resolvedURI = Util.resolveURI(getURI(), finalUrl);
                            f().addClickedLink(finalUrl);
                            char gType = getGopherType(finalUrl);

                            f().fetchURL(resolvedURI.replace("/h/", "/" + gType + "/"), false, null);
                        } else {
                            f().addClickedLink(finalUrl);
                            f().fetchURL(finalUrl, false, currentMode == FEED_MODE && directive[0].endsWith("#") ? label : null);
                        }

                    });
            if (Alhena.dataUrl && dataUrl) { // auto view
                dataURL(url, true);
                cr.imageIndex = cr.end;
            }
            cr.dataUrl = dataUrl;
            cr.url = url;
            cr.directive = directive[0];

            if (cr.directive != null) {
                if (cr.directive.endsWith(">")) {
                    cr.label = null;
                }
            }

        } else if (line.startsWith(">")) {
            addStyledText(line.substring(1).trim(), ">", null);
        } else if (line.startsWith("* ")) {
            addStyledText("• " + line.substring(1).trim(), "*", null);

        } else {
            addStyledText(line, "text", null);
        }
    }

    private HashSet<String> feedVisitedList = new HashSet<>();

    private void dataURL(String url, boolean curPos) {
        int scIndex = url.indexOf(";");
        byte[] byteData = null;
        String mime;
        String charset = null;
        if (scIndex != -1) {
            String[] parts = url.split(";");
            //String mime = url.substring(5, scIndex);
            mime = parts[0].substring(5);
            int cIdx = url.indexOf(",");
            String data = "";

            if (parts.length == 2) {
                String encoding = url.substring(scIndex + 1, cIdx);
                if (encoding.equals("base64")) {
                    data = "base64," + url.substring(cIdx + 1);
                } else {
                    if (encoding.toLowerCase().startsWith("charset")) {
                        charset = encoding;
                    }
                    data = url.substring(cIdx + 1);
                }

            } else if (parts.length == 3) {
                charset = parts[1];
                data = parts[2];
            }

            if (data.startsWith("base64,")) {
                byteData = Base64.getDecoder().decode(data.substring(7));
            } else {
                try {
                    String cs = charset == null ? "UTF-8" : charset.substring(charset.indexOf('=') + 1);
                    byteData = data.getBytes(cs);
                } catch (UnsupportedEncodingException ex) {
                }

            }
        } else {
            int cIdx = url.indexOf(",");
            mime = url.substring(5, cIdx);
            try {
                byteData = url.substring(cIdx + 1).getBytes("UTF-8");
            } catch (UnsupportedEncodingException ex) {
            }

        }
        String cs = charset == null ? "UTF-8" : charset.substring(charset.indexOf('=') + 1);
        if (mime.startsWith("audio") || mime.startsWith("video")) {
            if (!Alhena.allowVLC) {
                if (!curPos) {
                    Util.infoDialog(f(), I18n.t("vlcRequiredDialog"), I18n.t("vlcRequiredDialogMsg"));
                }
                return;
            }
            Alhena.pauseMedia();
            File af;
            try {
                af = File.createTempFile("alhena", "media");
                af.deleteOnExit();
                Files.write(af.toPath(), byteData);  // overwrite or create

                MediaComponent ap = mime.startsWith("audio") ? new AudioPlayer(null, false) : new VideoPlayer(null);

                playerList.add(ap);

                if (curPos) {
                    insertComp((Component) ap, doc.getLength());
                    addStyledText("", "```", null);
                } else {
                    insertComp((Component) ap);
                }

                ap.start(af.getAbsolutePath());
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else if (mime.startsWith("image")) {
            insertImage(byteData, curPos, mime.contains("svg+xml"));
        } else if (mime.startsWith("text")) {
            String s;
            try {
                s = URLDecoder.decode(new String(byteData, cs), cs);

                PreformattedTextPane ptpText = createTextComponent(curPos);
                if (asciiImage && !printing) {
                    BufferedImage bi = AsciiImage.renderTextToImage(shadePF, s, pageStyle.getMonoFontFamily(), pageStyle.getMonoFontSize(), pageStyle.getMonoFontColor(), pageStyle.getPageBackground(), false);
                    ImageIcon icon = new ImageIcon(bi);
                    ptpText.insertComp(new JLabel(icon));
                    ptpText.scrollLeft();
                } else {
                    ptpText.addText(s + "\n");
                    ptpText.end();
                    ptpText.removeLastChar();
                    ptpText.scrollLeft();
                }
                ptpList.add(ptpText);
                EventQueue.invokeLater(() -> {
                    updateUI();
                    //SwingUtilities.updateComponentTreeUI(GeminiTextPane.this);
                });

            } catch (UnsupportedEncodingException ex) {
                ex.printStackTrace();
            }

        }
        EventQueue.invokeLater(() -> {
            updateUI();
            //SwingUtilities.updateComponentTreeUI(GeminiTextPane.this);
        });

    }

    private char getGopherType(String name) {
        String mimeFromExt = MimeMapping.getMimeTypeForFilename(name);

        if (mimeFromExt != null) {
            if (mimeFromExt.startsWith("audio") || mimeFromExt.startsWith("video")) {
                return '9';
            }
            if (Alhena.imageExtensions.stream().anyMatch(ext -> name.toLowerCase().endsWith(ext))) {
                return 'I';
            }
        }
        return '0'; // assume text otherwise
    }

    void makeTransparent(Component c) {
        if (c instanceof JComponent jc) {
            jc.setOpaque(false);
        }
        if (c instanceof Container container) {
            for (Component child : container.getComponents()) {
                makeTransparent(child);
            }
        }
    }

    private PreformattedTextPane createTextComponent(boolean curPos) {

        //Color background = shadePF ? AnsiColor.adjustColor(getBackground(), isDark, .2d, .8d, .05d) : getBackground();
        PreformattedTextPane pfTextPane = new PreformattedTextPane(customFontSize == 0 ? null : customFontSize, isDark, GeminiTextPane.this);

        JScrollPane sp = new JScrollPane(pfTextPane);
        makeTransparent(sp);
        pfTextPane.setFocusTraversalKeysEnabled(false);
        EventQueue.invokeLater(() -> pfTextPane.setCaretPosition(0));

        pfTextPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_TAB) {

                    JButton fButton = f().backButton.isEnabled() ? f().backButton : f().forwardButton.isEnabled() ? f().forwardButton : f().refreshButton;

                    fButton.requestFocusInWindow();
                    pfTextPane.setCaretPosition(pfTextPane.getCaretPosition());
                    pfTextPane.setFocusTraversalKeysEnabled(false);
                }
                GeminiTextPane.this.dispatchEvent(ke);

            }

            @Override
            public void keyReleased(KeyEvent ke) {
                GeminiTextPane.this.dispatchEvent(ke);
            }

            @Override
            public void keyTyped(KeyEvent ke) {
                GeminiTextPane.this.dispatchEvent(ke);
            }

        });

        pfTextPane.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (dragToScroll) {
                    lastScreen = e.getLocationOnScreen();
                    pressTime = System.currentTimeMillis();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (dragToScroll) {
                    lastScreen = null;
                    pressTime = 0;
                    dragging = false;
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (checkScrollingNeeded(sp)) {
                    f().setTmpStatus(I18n.t("holdToScrollLabel"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                f().setTmpStatus(" ");
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                GeminiTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(pfTextPane, e, GeminiTextPane.this));
            }
        });

        pfTextPane.addMouseWheelListener(e -> {
            if (!Alhena.sDown) {
                GeminiTextPane.this.dispatchEvent(SwingUtilities.convertMouseEvent(pfTextPane, e, GeminiTextPane.this));

            } else {
                sp.dispatchEvent(SwingUtilities.convertMouseEvent(pfTextPane, e, sp));
            }

        });
        sp.addMouseWheelListener(e -> {
            if (!Alhena.sDown) {
                scrollPane.dispatchEvent(SwingUtilities.convertMouseEvent(sp, e, scrollPane));
                //e.consume();
            }

        });
        sp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

        if (!showSB) {
            sp.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 0));
        }

        sp.setBorder(null);
        sp.setMaximumSize(new Dimension((int) contentWidth, Integer.MAX_VALUE));
        if (curPos) {
            insertComp(sp, doc.getLength());
            addStyledText("", "```", null);
        } else {
            sp.setFocusable(false);
            insertComp(sp);

        }
        ptpList.add(pfTextPane);
        return pfTextPane;
    }
    private List<PreformattedTextPane> ptpList;
    private SimpleAttributeSet emojiStyle1 = new SimpleAttributeSet();

    private ClickableRange addStyledText(String text, String styleName, Runnable action) {

        Style style = doc.getStyle(styleName);

        ClickableRange cr = null;

        int start = doc.getLength();
        boolean pfText = styleName.equals("```");

        if (!(hasAnsi && pfText) && EmojiManager.containsAnyEmoji(text)) {

            String fontFamily = StyleConstants.getFontFamily(style);
            int fontSize = StyleConstants.getFontSize(style);
            int imgSize = fontSize + (pfText ? 4 : 0);

            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(text);

            IndexedEmoji emoji;
            // can't iterate by code point without preprocessing first to get name
            for (int i = 0; i < text.length(); i++) {

                if ((emoji = isEmoji(emojis, i)) != null) {
                    //emojiIdx++;
                    if (action != null && Alhena.linkIcons && i == 0) {
                        //ImageIcon icon = new ImageIcon(image);
                        String em = emoji.getEmoji().getEmoji();
                        ImageIcon icon;
                        icon = switch (em) {
                            case "🌐" ->
                                otherIcon;
                            case "📎" ->
                                dataIcon;
                            case "✉️" ->
                                mailIcon;
                            case "✏️" ->
                                titanIcon;
                            case "🌠" ->
                                picIcon;
                            case "🎥" ->
                                videoIcon;
                            case "🎧" ->
                                audioIcon;
                            case "🐭" ->
                                gopherIcon;
                            case "💪" ->
                                spartanIcon;
                            case "🚄" ->
                                nexIcon;
                            case "💾" ->
                                fileIcon;
                            default ->
                                geminiIcon;
                        };

                        StyleConstants.setIcon(emojiStyle1, icon);
                        try {
                            doc.insertString(doc.getLength(), " ", emojiStyle1); // Use emoji style
                        } catch (BadLocationException ex) {
                        }
                        if (emojis.size() == 1) {
                            insertString(doc.getLength(), text.substring(2), style);
                            break;
                        }

                        i++;
                    } else if (sheetImage != null) {

                        String key = getEmojiHex(emoji);

                        Point p = emojiSheetMap.get(key);
                        ImageIcon icon = null;

                        if (p != null) {
                            icon = extractSprite(p.x, p.y, 64, imgSize, imgSize, fontSize);
                        } else {
                            int dashIdx = key.indexOf('-');
                            if (dashIdx != -1) {

                                p = emojiSheetMap.get(key.substring(0, dashIdx));
                                if (p != null) {
                                    icon = extractSprite(p.x, p.y, 64, imgSize, imgSize, fontSize);
                                }
                            }
                        }
                        if (icon == null) {
                            // copyright symbol for example
                            char[] chars = Character.toChars(text.codePointAt(i));

                            int eci = emoji.getEndCharIndex();
                            int emojiSize = eci - emoji.getCharIndex();

                            // advance past emoji
                            i += (emojiSize - 1);

                            // check for variation selector
                            if (eci < text.length()) {
                                int nextCodePoint = text.codePointAt(eci);
                                if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                    i += Character.charCount(nextCodePoint); // usually 1
                                }
                            }

                            insertString(doc.getLength(), new String(chars), style);

                        } else {
                            int opto = 0;
                            int eci = emoji.getEndCharIndex();
                            int emojiSize = eci - emoji.getCharIndex();

                            // advance past emoji
                            i += (emojiSize - 1);

                            // check for variation selector
                            if (eci < text.length()) {
                                int nextCodePoint = text.codePointAt(eci);
                                if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                    i += Character.charCount(nextCodePoint); // usually 1
                                    opto = 1;
                                }
                            }

                            SimpleAttributeSet emojiStyle = new SimpleAttributeSet(style);
                            StyleConstants.setIcon(emojiStyle, icon);
                            try {
                                doc.insertString(doc.getLength(), " ", emojiStyle); // Use emoji style
                            } catch (BadLocationException ex) {
                            }
                            if (emojis.size() == 1 && eci < text.length()) {
                                insertString(doc.getLength(), text.substring(eci + opto), style);
                                break;
                            }

                        }
                    } else {
                        StyleConstants.setFontFamily(style, emojiProportional);
                        insertString(doc.getLength(), unescapeUnicode(emoji.getEmoji().getUnicode()), style);

                        int eci = emoji.getEndCharIndex();
                        int emojiSize = eci - emoji.getCharIndex();

                        // advance past emoji
                        //i += emojiSize;
                        i += (emojiSize - 1);

                        // check for variation selector
                        if (eci < text.length()) {
                            int nextCodePoint = text.codePointAt(eci);
                            if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                i += Character.charCount(nextCodePoint); // usually 1
                                eci += Character.charCount(nextCodePoint);
                                if (eci < text.length()) { // optomize common scenario
                                    if (emojis.indexOf(emoji) == emojis.size() - 1) { // this is last emoji

                                        StyleConstants.setFontFamily(style, fontFamily);
                                        insertString(doc.getLength(), text.substring(eci), style);
                                        break;
                                    }
                                }
                            }
                        }
                    }
                } else {
                    IndexedEmoji nextEmoji = null;
                    for (IndexedEmoji em : emojis) {
                        if (em.getCharIndex() > i) {
                            nextEmoji = em;
                            break;
                        }
                    }
                    StyleConstants.setFontFamily(style, fontFamily);
                    if (nextEmoji != null) {
                        insertString(doc.getLength(), text.substring(i, nextEmoji.getCharIndex()), style);
                        i = nextEmoji.getCharIndex() - 1;
                    } else {
                        insertString(doc.getLength(), text.substring(i), style);
                        break;
                    }

                }
            }
            StyleConstants.setFontFamily(style, fontFamily);
        } else {

            insertString(start, text, style);
        }

        if (action != null) {
            int end = doc.getLength();

            // add range to clickable ranges
            cr = new ClickableRange(start, end, action);
            if (currentMode == FEED_MODE) {
                cr.label = text;
            }

            clickableRegions.add(cr);
        }
        int caretPosition = getCaretPosition();
        try {
            doc.insertString(doc.getLength(), "\n", style);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }

        setCaretPosition(caretPosition); // prevent scrolling as content added

        return cr;
    }

    private final static StringBuilder sb = new StringBuilder();

    public static String unescapeUnicode(String input) { // not thread safe only use on EDT
        sb.setLength(0);
        for (int i = 0; i < input.length();) {
            char c = input.charAt(i);
            if (c == '\\' && i + 5 < input.length() && input.charAt(i + 1) == 'u') {
                // parse 4 hex digits after \\u
                int code = Integer.parseInt(input.substring(i + 2, i + 6), 16);
                sb.append((char) code);
                i += 6;
            } else {
                sb.append(c);
                i++;
            }
        }
        return sb.toString();
    }

    public static IndexedEmoji isEmoji(List<IndexedEmoji> emojiList, int idx) {
        for (IndexedEmoji emo : emojiList) {
            if (emo.getCharIndex() == idx) {
                return emo;
            }
        }
        return null;
    }

    public static String getEmojiHex(IndexedEmoji emo) {

        String code = emo.getEmoji().getHtmlHexadecimalCode().replace("&#x", "").replace(";", "-");
        return code.substring(0, code.length() - 1);

    }

    private void insertString(int length, String txt, AttributeSet style) {
        try {
            if (hasAnsi && preformattedMode && txt.indexOf(27) >= 0) {
                handleAnsi(txt);
            } else {
                doc.insertString(length, txt, style);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public void scrollLeft() {
        EventQueue.invokeLater(() -> {
            JScrollPane jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            jsp.getHorizontalScrollBar().setValue(0);
        });
    }

    // create a list of image links that are currently open and displayed
    public void queueLink(int linkNum) {
        if (openQueue == null) {
            openQueue = new ArrayList<>();
        }
        int i = 0;
        for (ClickableRange range : clickableRegions) {
            if (i == linkNum) {
                openQueue.add(range);
                break;
            }
            i++;
        }
    }

    public void fetchLink() {
        if (openQueue != null && !openQueue.isEmpty()) {
            lastClicked = openQueue.remove(0);
            Alhena.processURL(lastClicked.url, page, null, page, false);
        }
    }

    public void setPrintWidth(int pw) {
        printWidth = pw;
    }

    public static class ClickableRange {

        int start;
        int end;
        Runnable action;
        String url;
        String directive;
        int imageIndex = -1;
        boolean dataUrl;
        boolean openImage;
        String label; // use for scroll to header feed

        ClickableRange(int start, int end, Runnable action) {
            this.start = start;
            this.end = end;
            this.action = action;

        }

        public String displayString() {
            if (label != null) {
                return url + "#" + label;
            }
            return url;
        }
    }

    public Rectangle getCharacterBounds(int start, int end) {
        try {
            Rectangle startRect = modelToView2D(start).getBounds();
            Rectangle endRect = modelToView2D(end).getBounds();

            if (startRect.y == endRect.y) { // Same line
                return new Rectangle(startRect.x, startRect.y, endRect.x - startRect.x, startRect.height);
            } else {
                // handle multi-line text
                Rectangle combinedRect = new Rectangle(startRect);

                int currentPos = start;
                while (currentPos < end) {
                    Rectangle currentRect = modelToView2D(currentPos).getBounds();

                    // extend the combined rectangle to include the current rectangle
                    combinedRect.add(currentRect);

                    // move to the next character
                    currentPos++;
                }

                return combinedRect;
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Optional<String> getDocURL() {
        return Optional.ofNullable(docURL);
    }

    // for new tabs/windows only!
    public void setDocURL(String du) {
        docURL = du;
    }

    // for new tabs only!
    public String getDocURLString() {
        return docURL;
    }

    public record CurrentPage(StringBuilder currentPage, boolean pMode) {

    }

    public CurrentPage current() {
        return new CurrentPage(pageBuffer, originalPfMode);
    }

    public static ImageIcon extractSprite(int sheetX, int sheetY, int sheetSize, int width, int height, int fontSize) {

        int x = (sheetX * (sheetSize + 2)) + 1;
        int y = (sheetY * (sheetSize + 2)) + 1;

        BufferedImage bi = sheetImage.getSubimage(x, y, sheetSize, sheetSize);
        //Image scaledImg = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        Image scaledImg = Util.getImage(null, width, height, bi, true);

        return new BaselineShiftedIcon(scaledImg, fontSize / 10);
    }

    public static Image extractSpriteImage(int sheetX, int sheetY, int sheetSize, int width, int height, int fontSize) {

        int x = (sheetX * (sheetSize + 2)) + 1;
        int y = (sheetY * (sheetSize + 2)) + 1;

        BufferedImage bi = sheetImage.getSubimage(x, y, sheetSize, sheetSize);
        Image scaledImg = Util.getImage(null, width, height, bi, true);
        //Image scaledImg = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return scaledImg;
    }

    private static final double FRICTION = 0.93; // friction coefficient (lower = more friction) .90
    private static final int SCROLL_INTERVAL = 15; // update interval in milliseconds
    private static final double SCROLL_MULTIPLIER = 0.4; // adjust scrolling sensitivity
    //private static final int SCROLL_AMOUNT = 1; // Get rid of this when uniform behavior

    // scrolling state variables
    private double momentumY = 0;
    private boolean isScrolling = false;
    private Timer scrollTimer2;
    private long lastScrollTime;
    private double lastScrollAmount = 0;
    private int scrollAdjust = 1;

    public void removeAdaptiveScrolling() {
        MouseWheelListener[] listeners = getMouseWheelListeners();
        for (MouseWheelListener mwl : listeners) {
            removeMouseWheelListener(mwl);
        }

    }

    public final void setupAdaptiveScrolling() {
        double osScrollFactor = SystemInfo.isMacOS ? 1.0 : Double.parseDouble(System.getProperty("scrollfactor", "0.5"));
        // create a timer for smooth animation
        scrollTimer2 = new Timer(SCROLL_INTERVAL, e -> updateScrollPosition());

        // add mouse wheel listener with smooth scrolling
        addMouseWheelListener(new MouseWheelListener() {
            boolean first = true;
            long time = 0;
            long elapsed;

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (first) {
                    first = false;
                    time = System.currentTimeMillis();
                } else {
                    if (!SystemInfo.isMacOS) {
                        int cmp = SystemInfo.isMacOS ? 10 : 20;
                        if (scrollAdjust < cmp || !isScrolling) {
                            elapsed = System.currentTimeMillis() - time;
                            if (elapsed < 200) {
                                scrollAdjust++;
                            } else {
                                scrollAdjust = 1;
                            }
                            if (scrollAdjust > 6) {
                                scrollAdjust = (int) (scrollAdjust * 1.5);
                            }
                            time = System.currentTimeMillis();
                        }
                    }
                }

                // get scroll amount with multiplier for sensitivity
                double scrollAmount = e.getPreciseWheelRotation() * SCROLL_MULTIPLIER * osScrollFactor;

                // detect direction change
                if (Math.signum(scrollAmount) != Math.signum(momentumY)) {
                    momentumY = 0;
                    lastScrollAmount = 0;
                }

                // update momentum based on current scroll
                momentumY = scrollAmount * 8; // Initial velocity
                // record time for velocity calculations
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastScrollTime < 200) {
                    // if scrolling quickly, add some of the previous momentum
                    momentumY = momentumY * 0.7 + lastScrollAmount * 0.3;
                }
                lastScrollTime = currentTime;
                lastScrollAmount = momentumY;
                // handle immediate scroll
                smoothScroll(scrollAmount);
                // start momentum if not already scrolling
                if (!isScrolling) {
                    isScrolling = true;
                    scrollTimer2.start();
                }
            }
        });
    }

    private void smoothScroll(double amount) {

        JScrollBar verticalScrollBar = scrollPane.getVerticalScrollBar();
        int currentValue = verticalScrollBar.getValue();
        int newValue = amount > 0 ? (int) Math.ceil(currentValue + amount * scrollAdjust) : (int) (currentValue + amount * scrollAdjust);

        if (amount < 0) {
            newValue = Math.max(0, Math.min(newValue, verticalScrollBar.getMaximum() - verticalScrollBar.getVisibleAmount()));
        }

        verticalScrollBar.setValue(newValue);

    }

    private void updateScrollPosition() {
        // apply friction to slow down momentum
        momentumY *= FRICTION;
        // apply the momentum
        smoothScroll(momentumY);
        // stop when momentum becomes very small
        if (Math.abs(momentumY) < 0.1) {
            momentumY = 0;
            isScrolling = false;
            scrollTimer2.stop();
        }
    }

    public List<ClickableRange> getVisibleLinks() {
        List<ClickableRange> visibleList = new ArrayList<>();
        for (ClickableRange cr : clickableRegions) {
            if (isClickableRangeVisible(cr)) {
                visibleList.add(cr);
            }
        }
        return visibleList;
    }

    public void rightClickVisibleLink(int linkIdx) {
        int idx = 0;
        for (ClickableRange cr : clickableRegions) {
            if (isClickableRangeVisible(cr)) {

                if (linkIdx == idx) {
                    long now = System.currentTimeMillis();
                    Rectangle cb = getCharacterBounds(cr.start, cr.end);
                    Rectangle newRect = SwingUtilities.convertRectangle(this, cb, this);

                    MouseEvent pressEvent = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, now,
                            InputEvent.BUTTON3_DOWN_MASK, newRect.x, newRect.y, 1, false, MouseEvent.BUTTON3);
                    showPopup(pressEvent);
                    break;
                } else {
                    idx++;
                }
            }
        }
    }

    public void clickVisibleLink(int linkIdx) {
        int idx = 0;
        for (ClickableRange cr : clickableRegions) {
            if (isClickableRangeVisible(cr)) {

                if (linkIdx == idx) {

                    if (cr.imageIndex != -1) {
                        setCaretPosition(cr.start);
                        removeItemAtIndex(cr);
                        cr.imageIndex = -1;

                    } else {
                        lastClicked = cr;
                        if (!cr.dataUrl) {

                            clicked(cr);
                            cr.action.run();

                        }
                    }

                    break;
                } else {
                    idx++;
                }
            }
        }
    }

    private boolean isClickableRangeVisible(ClickableRange range) {

        Rectangle cb = getCharacterBounds(range.start, range.end);
        Rectangle newRect = SwingUtilities.convertRectangle(this, cb, this);
        JViewport viewport = (JViewport) getParent();
        Rectangle viewRect = viewport.getViewRect();

        return viewRect.contains(newRect.getBounds());

    }

    public static Object getFavIcon(String s) {
        List<IndexedEmoji> emojis;
        try {
            emojis = EmojiManager.extractEmojisInOrderWithIndex(s);
        } catch (Exception ex) {
            return s;
        }
        IndexedEmoji emoji;
        ImageIcon icon = null;
        if ((emoji = isEmoji(emojis, 0)) != null) {
            if (sheetImage != null) {

                String key = getEmojiHex(emoji);

                Point p = emojiSheetMap.get(key);

                int imgSize = Page.ICON_SIZE + 4;
                if (p != null) {
                    icon = extractSprite(p.x, p.y, 64, imgSize, imgSize, Page.ICON_SIZE);
                } else {
                    int dashIdx = key.indexOf('-');
                    if (dashIdx != -1) {

                        p = emojiSheetMap.get(key.substring(0, dashIdx));
                        if (p != null) {
                            icon = extractSprite(p.x, p.y, 64, imgSize, imgSize, Page.ICON_SIZE);
                        }
                    }
                }

            }
        }
        if (icon == null) {
            return s;
        } else {

            return icon;
        }
    }

    private static ImageIcon dataIcon, mailIcon, geminiIcon, otherIcon, titanIcon, picIcon, videoIcon, audioIcon, gopherIcon, spartanIcon, nexIcon, fileIcon;

    private static ImageIcon getLinkIcon(String txt, String fontName, int fontSize, Color fgColor) {
        BufferedImage bi = AsciiImage.renderTextToImage(shadePF, txt, fontName, fontSize, fgColor, null, true);
        // this could be optimized as AsciiImage already created font and metrics
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        return new BaselineShiftedIcon(bi, metrics.getDescent() / 2);

    }

    public List<String> getHeadings() {
        ArrayList<String> hl = new ArrayList<>();
        headingMap.keySet().forEach(key -> {
            hl.add(key);
        });
        return hl;
    }

}
