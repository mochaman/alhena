package brad.grier.alhena;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Component;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
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
    private List<ClickableRange> clickableRegions = new ArrayList<>();
    private int currentCursor = Cursor.DEFAULT_CURSOR;
    private boolean preformattedMode;
    private String currentStatus = Alhena.welcomeMessage;
    public static String monospacedFamily;
    private final GeminiFrame f;
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

    static {
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
            readEmojiJson();
        } else if (!emojiPref.equals("font")) {
            String url = GeminiFrame.emojiNameMap.get(emojiPref);
            String fn = url.substring(url.lastIndexOf('/') + 1);
            File emojiFile = new File(System.getProperty("alhena.home") + File.separatorChar + fn);
            if (emojiFile.exists()) {
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
            readEmojiJson();
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

                            if (!range.url.equals(currentStatus)) {
                                // +1 needed since using png for emoji

                                SimpleAttributeSet sa = new SimpleAttributeSet();
                                StyleConstants.setForeground(sa, hoverColor);

                                f.setStatus(range.url);
                                currentStatus = range.url;

                                doc.setCharacterAttributes(range.start, range.end - range.start, sa, false);
                                if (saveRange != null) {

                                    SimpleAttributeSet sas = f.isClickedLink(saveRange.url) ? visitedStyle : normalStyle;
                                    doc.setCharacterAttributes(saveRange.start, saveRange.end - saveRange.start, sas, false);

                                }
                                saveRange = range;

                            }

                            break;
                        }
                    }
                }

                // fix this
                if (!entered && !" ".equals(currentStatus) && currentStatus != null && !currentStatus.equals(Alhena.welcomeMessage)) {

                    f.setStatus(" ");

                    if (saveRange != null) {
                        SimpleAttributeSet sas = f.isClickedLink(saveRange.url) ? visitedStyle : normalStyle;

                        doc.setCharacterAttributes(saveRange.start, saveRange.end - saveRange.start, sas, false);
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
                        boolean vlcType = url.toLowerCase().endsWith(".opus") || (mimeExt != null && (mimeExt.startsWith("audio") || mimeExt.startsWith("video")));
                        if (matches || vlcType) {

                            if (lcName.endsWith(".pem")) {
                                f.importPem(new URI(getDocURLString()), file);
                            } else {
                                f.openFile(file);
                            }
                        }

                        break;

                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                applyCenteredParagraphStyle();
            }

            @Override
            public void componentResized(ComponentEvent e) {
                if (inserting) {
                    inserting = false;
                    return;
                }
                if (doc != null) {
                    applyCenteredParagraphStyle();
                }
                if (ptpList != null) {
                    for (PreformattedTextPane ptp : ptpList) {
                        JScrollPane sp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, ptp);
                        sp.setMaximumSize(new Dimension((int) contentWidth, Integer.MAX_VALUE));
                        ptp.revalidate();
                        ptp.repaint();
                    }
                }
            }
        });

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
        sheetImage = sheet;
    }

    private void showPopup(MouseEvent e) {
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
                                JMenuItem copyLinkItem = new JMenuItem(I18n.t("copyLinkPopupItem"));

                                copyLinkItem.addActionListener(ev -> {
                                    copyText(range.url);
                                });
                                popupMenu.add(copyLinkItem);

                                popupMenu.add(new JSeparator());
                                JMenuItem menuItem1 = new JMenuItem(I18n.t("newTabPopupItem"));

                                menuItem1.addActionListener(ev -> {
                                    f.newTab(range.url);
                                });
                                menuItem1.setEnabled(!range.dataUrl);
                                popupMenu.add(menuItem1);
                                JMenuItem menuItem2 = new JMenuItem(I18n.t("newWindowPopupItem"));
                                menuItem2.setEnabled(!range.dataUrl);
                                menuItem2.addActionListener(ev -> {
                                    Alhena.newWindow(range.url, docURL);

                                });
                                popupMenu.add(menuItem2);

                                if (Alhena.httpProxy == null && Alhena.browsingSupported && range.url.startsWith("http")) {
                                    String useB = DB.getPref("browser", null);
                                    boolean useBrowser = useB == null ? true : useB.equals("true");

                                    // show the opposite of the setting - this then becomes the default
                                    if (useBrowser) {
                                        // do not allow open in new window or tab if using system browser
                                        menuItem1.setEnabled(false);
                                        menuItem2.setEnabled(false);
                                    }
                                    String label = useBrowser ? "Alhena" : I18n.t("browserLabel");
                                    String message = MessageFormat.format(I18n.t("httpOpenItem"), label);
                                    JMenuItem httpMenuItem = new JMenuItem(message);
                                    httpMenuItem.addActionListener(al -> {

                                        DB.insertPref("browser", String.valueOf(!useBrowser));
                                        f.fetchURL(range.url, false);
                                    });

                                    popupMenu.add(httpMenuItem);
                                }

                                switch (currentMode) {
                                    case CERT_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        int id = Integer.parseInt(range.directive.substring(0, range.directive.indexOf(",")));
                                        boolean active = range.directive.substring(range.directive.indexOf(",") + 1).equals("true");
                                        JMenuItem exportItem = new JMenuItem(I18n.t("exportPopupItem"));
                                        exportItem.addActionListener(al -> {
                                            f.exportCert(id, GeminiTextPane.this);
                                        });
                                        popupMenu.add(exportItem);
                                        String command = active ? I18n.t("deactivatePopupItem") : I18n.t("activatePopupItem");
                                        JMenuItem actionItem = new JMenuItem(command);
                                        actionItem.addActionListener(al -> {

                                            f.toggleCert(id, !active, range.url);
                                        });
                                        popupMenu.add(actionItem);
                                        JMenuItem delItem = new JMenuItem(I18n.t("deletePopupItem"));
                                        delItem.addActionListener(al -> {
                                            f.deleteCert(id);
                                        });
                                        popupMenu.add(delItem);
                                    }
                                    case HISTORY_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem forgetItem = new JMenuItem(I18n.t("forgetLinkItem"));
                                        forgetItem.addActionListener(al -> {
                                            f.deleteFromHistory(range.url, false);
                                        });

                                        popupMenu.add(forgetItem);
                                        JMenuItem clearItem = new JMenuItem(I18n.t("deleteHistoryItem"));

                                        clearItem.addActionListener(al -> {
                                            f.clearHistory();
                                        });
                                        popupMenu.add(clearItem);
                                    }
                                    case BOOKMARK_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem editItem = new JMenuItem(I18n.t("editBookmarkItem"));
                                        editItem.addActionListener(ev -> {
                                            f.updateBookmark(f, range.directive);

                                        });
                                        popupMenu.add(editItem);
                                        JMenuItem deleteItem = new JMenuItem(I18n.t("deleteBookmarkItem"));
                                        deleteItem.addActionListener(ev -> {
                                            f.deleteBookmark(f, range.directive);

                                        });
                                        popupMenu.add(deleteItem);
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
                                        range.action.run();
                                    } else {
                                        dataURL(range.url, false);

                                    }

                                }
                            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                                if (!range.dataUrl) {
                                    f.newTab(range.url);
                                }
                            }
                            break;
                        }
                    }
                }
            }
            if (!linkClicked) {
                if (SwingUtilities.isRightMouseButton(e) || (e.getButton() == MouseEvent.BUTTON1 && e.isControlDown())) {
                    if (currentMode == SERVER_MODE) {
                        return;
                    }
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
                    if (!page.isNex()) {
                        String menuText = plainTextMode ? I18n.t("gemTextItem") : I18n.t("plainTextItem");
                        JMenuItem ptMenuItem = new JMenuItem(menuText);
                        ptMenuItem.setEnabled(!imageOnly);
                        ptMenuItem.addActionListener(al -> {
                            plainTextMode = !plainTextMode;
                            f.toggleView(GeminiTextPane.this, plainTextMode);
                        });

                        popupMenu.add(ptMenuItem);
                    }

                    JMenuItem crtMenuItem = new JMenuItem(I18n.t("viewCertItem"));
                    URI uri = getURI();
                    crtMenuItem.setEnabled(!imageOnly && currentMode == DEFAULT_MODE && uri != null && uri.getScheme().equals("gemini"));
                    crtMenuItem.addActionListener(al -> {

                        f.viewServerCert(GeminiTextPane.this, getURI());
                    });

                    popupMenu.add(crtMenuItem);

                    popupMenu.add(new JSeparator());
                    JMenuItem saveItem = new JMenuItem(I18n.t("savePageItem"));
                    saveItem.setEnabled(!imageOnly);
                    saveItem.addActionListener(al -> {
                        f.savePage(GeminiTextPane.this, pageBuffer, currentMode);
                    });

                    JMenuItem titanItem = new JMenuItem(I18n.t("titanItem"));
                    titanItem.setEnabled(!imageOnly);
                    titanItem.addActionListener(al -> {

                        f.editPage();
                    });

                    popupMenu.add(saveItem);
                    popupMenu.add(titanItem);

                    if (currentMode == DEFAULT_MODE) {
                        JMenuItem pemItem = new JMenuItem(I18n.t("importPEMPopup"));
                        pemItem.setEnabled(!imageOnly);
                        pemItem.addActionListener(al -> {
                            f.importPem(getURI(), null);
                        });
                        popupMenu.add(pemItem);

                        JMenuItem certItem = new JMenuItem(I18n.t("newClientCertPopup"));
                        certItem.setEnabled(!imageOnly);
                        certItem.addActionListener(al -> {
                            f.createCert(getURI());
                        });
                        popupMenu.add(certItem);
                        boolean disable = uri.getHost() == null || !uri.getScheme().equals("gemini");
                        if (disable) {
                            pemItem.setEnabled(false);
                            certItem.setEnabled(false);
                        }
                    }

                    if (currentMode == HISTORY_MODE) {
                        popupMenu.add(new JSeparator());
                        JMenuItem whereItem = new JMenuItem(I18n.t("forgetLinksPopup"));
                        whereItem.addActionListener(al -> {
                            f.deleteFromHistory(null, true);
                        });

                        popupMenu.add(whereItem);
                        JMenuItem clearItem = new JMenuItem(I18n.t("deleteHistoryPopup"));

                        clearItem.addActionListener(al -> {
                            f.clearHistory();
                        });
                        popupMenu.add(clearItem);
                    }

                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        }

    }

    @Override
    public void processKeyEvent(KeyEvent e) {

        if ((e.getModifiersEx() & mod) != 0) {
            if (e.getKeyCode() == KeyEvent.VK_I && e.getID() == KeyEvent.KEY_PRESSED) {
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

        int index = rg.imageIndex;
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

                doc.remove(index + 1, 2);

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
            }

            if (rg.dataUrl) {
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

    public void closePlayers() {
        for (MediaComponent ap : playerList) {
            ap.dispose();
        }
        playerList.clear();
    }

    public void pausePlayers() {
        for (MediaComponent mc : playerList) {
            mc.pause();
        }
    }

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

    private void insertComp(Component c) {
        SimpleAttributeSet apStyle = new SimpleAttributeSet();
        StyleConstants.setComponent(apStyle, c);

        try {
            if (lastClicked == null) {

                doc.insertString(0, " ", apStyle);
                imageOnly = true;
            } else {
                doc.insertString(lastClicked.end + 1, " ", apStyle);
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
                lastClicked = null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public void insertMediaPlayer(String path, String mime) {
        inserting = true;
        Alhena.pauseMedia();
        MediaComponent ap = mime.startsWith("audio") ? new AudioPlayer() : new VideoPlayer();

        playerList.add(ap);

        insertComp((Component) ap);
        ap.start(path);
        f.setBusy(false, page);

    }

    public void insertImage(byte[] imageBytes, boolean curPos, boolean isSVG) {
        inserting = true;
        // 50 pixel fudge factor. Unable to land on a programmatic width insets plus scrollbar width, etc
        // that doesn't cause the horizontal scrollbar to appear
        int width = (int) contentWidth - 50;
        BufferedImage image = null;
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
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        } else {
            image = Util.getImage(imageBytes, width, width * 2, null, false);
        }

        // TODO: needed (maybe)
        if (image == null) {
            //System.out.println("here");
            f.setBusy(false, page);
            return;
        }

        ImageIcon icon = new ImageIcon(image);

        SimpleAttributeSet emojiStyle = new SimpleAttributeSet();
        StyleConstants.setIcon(emojiStyle, icon);

        try {
            if (curPos) {
                doc.insertString(doc.getLength(), " ", emojiStyle);
                doc.insertString(doc.getLength(), "\n", null);
            } else if (lastClicked == null) {
                doc.insertString(0, " ", emojiStyle);
                imageOnly = true;
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
                lastClicked = null;
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        f.setBusy(false, page);

    }

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

        JTabbedPane tabbedPane = page.frame().tabbedPane;
        firstHeading = createHeading();
        String title = f.createTitle(docURL, firstHeading);
        if (title != null) {
            if (tabbedPane != null) {
                for (int i = 0; i < tabbedPane.getTabCount(); i++) {

                    if (tabbedPane.getComponentAt(i) == page) {

                        if (page == tabbedPane.getSelectedComponent()) {
                            f.updateComboBox(docURL);
                            f.setTitle(title);
                        } else {

                            tabbedPane.setTitleAt(i, title);
                        }
                        break;
                    }
                }
            } else if (page.isVisible()) {

                if (currentMode != INFO_MODE) {
                    f.updateComboBox(docURL);
                    f.setTitle(title);
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
            new Thread(() -> {

                for (int i = 0; i < localBuilder.length(); i++) {
                    if (localBuilder.charAt(i) == 27) {
                        EventQueue.invokeLater(() -> {
                            if (GeminiTextPane.this.isShowing()) {
                                Runnable r = () -> {
                                    hasAnsi = true;
                                    f.refreshFromCache(page);
                                };
                                if (GeminiFrame.ansiAlert) {
                                    Object res = Util.confirmDialog(f, I18n.t("ansiDialog"), I18n.t("ansiDialogMsg"), JOptionPane.YES_NO_OPTION, null, null);
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

            }).start();
        }
    }

    // for one and done messages
    public void end(String geminiDoc, boolean pfMode, String docURL, boolean newRequest) {
        end(geminiDoc, pfMode, docURL, newRequest, false);
    }

    private boolean printing;

    public void end(String geminiDoc, boolean pfMode, String docURL, boolean newRequest, boolean printing) {
        this.printing = printing;
        if (printing) {
            setBackground(Color.WHITE);
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
            ansiFG(Color.WHITE);  // "default foreground color"
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

                        StyleConstants.setBackground(bStyle, getBackground());
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

    public void updatePage(String geminiDoc, boolean pfMode, String docURL, boolean newRequest) {
        if (!docURL.equals(this.docURL) && newRequest) {

            if (currentMode == DEFAULT_MODE)
            try {
                DB.insertHistory(docURL, null);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        if (docURL.equals(GeminiFrame.HISTORY_LABEL)) {
            currentMode = HISTORY_MODE;
        } else if (docURL.equals(GeminiFrame.BOOKMARK_LABEL)) {
            currentMode = BOOKMARK_MODE;
        } else if (docURL.equals(GeminiFrame.CERT_LABEL)) {
            currentMode = CERT_MODE;
        } else if (docURL.equals(GeminiFrame.INFO_LABEL)) {
            currentMode = INFO_MODE;
        } else if (docURL.equals(GeminiFrame.SERVERS_LABEL)) {
            currentMode = SERVER_MODE;
        }

        this.docURL = docURL;
        f.refreshNav(null);

        pageBuffer = new StringBuilder();

        bufferedLine = null; // probably not necessary here

        preformattedMode = pfMode;
        plainTextMode = pfMode;
        // map to track clickable regions and their actions
        clickableRegions.clear();
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
        PageTheme pageTheme = new PageTheme();
        Color bg = UIManager.getColor("TextPane.inactiveBackground");
        pageTheme.setPageBackground(bg);
        pageTheme.setContentPercentage(contentPercentage);
        boolean isDark = !Util.isLight(bg);
        Color lc = UIManager.getColor("Component.linkColor");
        pageTheme.setLinkColor(lc);
        pageTheme.setLinkStyle(Font.PLAIN);
        pageTheme.setVisitedLinkColor(isDark ? lc.darker() : lc.brighter());
        pageTheme.setHoverColor(pageTheme.getLinkColor().brighter());
        pageTheme.setMonoFontSize(GeminiFrame.monoFontSize);
        pageTheme.setMonoFontFamily(monospacedFamily);
        pageTheme.setMonoFontColor(UIManager.getColor("TextPane.foreground"));
        pageTheme.setTextForeground(UIManager.getColor("TextPane.foreground"));
        pageTheme.setFontStyle(Font.PLAIN);
        pageTheme.setQuoteForeground(pageTheme.getTextForeground());
        pageTheme.setQuoteStyle(Font.ITALIC);
        pageTheme.setFontSize(GeminiFrame.fontSize);
        pageTheme.setFontFamily(GeminiFrame.proportionalFamily);
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
        pageTheme.setHeader1FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setHeader2FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setHeader3FontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setLinkFontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setQuoteFontFamily(GeminiFrame.proportionalFamily);
        pageTheme.setLinkUnderline(false);
        pageTheme.setQuoteUnderline(false);
        pageTheme.setFontUnderline(false);
        pageTheme.setHeader1Underline(false);
        pageTheme.setHeader2Underline(false);
        pageTheme.setHeader3Underline(false);
        pageTheme.setListFont(GeminiFrame.proportionalFamily);
        pageTheme.setListStyle(Font.PLAIN);
        pageTheme.setListColor(UIManager.getColor("TextPane.foreground"));
        pageTheme.setListUnderline(false);
        pageTheme.setListFontSize(GeminiFrame.fontSize);

        return pageTheme;
    }

    public Integer styleId;

    public PageTheme getPageStyle() {
        String dbTheme = Alhena.theme;

        StyleInfo dbStyle = null;
        try {
            URI u = getURI();
            if (u != null) {
                dbStyle = DB.getStyle(docURL, u.getAuthority(), dbTheme, !UIManager.getBoolean("laf.dark"));
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
        PageTheme pageTheme = getDefaultTheme();
        pageTheme.fromJson(customTheme);

        return pageTheme;

    }

    protected PageTheme pageStyle;

    private void buildStyles() {
        pageStyle = getPageStyle();
        isDark = !Util.isLight(pageStyle.getPageBackground());

        setBackground(pageStyle.getPageBackground());
        setForeground(pageStyle.getMonoFontColor());

        for (MediaComponent ap : playerList) {
            ap.dispose();
        }
        playerList.clear();

        emojiProportional = "Noto Emoji";
        if (SystemInfo.isMacOS) {
            if (!Alhena.macUseNoto) {
                emojiProportional = "SansSerif";
            }
        }

        linkColor = pageStyle.getLinkColor();
        hoverColor = pageStyle.getHoverColor();

        int gfMonoFontSize = printing ? ViewBasedTextPanePrinter.MONOSPACED_SIZE : pageStyle.getMonoFontSize();
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
        if (page.isNex()) {
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

    private static Color linkIconBG;

    // only call on EDT
    private static void rebuildLinkIcons(int gfFontSize, Color bgColor, Color linkColor) {
        dataIcon = getLinkIcon("📎", "Noto Emoji", gfFontSize, bgColor, linkColor);
        mailIcon = getLinkIcon("✉️", "Noto Emoji", gfFontSize, bgColor, linkColor);
        geminiIcon = getLinkIcon("♊️", "Noto Emoji", gfFontSize, bgColor, linkColor);
        otherIcon = getLinkIcon("🌐", "Noto Emoji", gfFontSize, bgColor, linkColor);
        titanIcon = getLinkIcon("✏️", "Noto Emoji", gfFontSize, bgColor, linkColor);
        picIcon = getLinkIcon("📸", "Noto Emoji", gfFontSize, bgColor, linkColor);
        mediaIcon = getLinkIcon("🎥", "Noto Emoji", gfFontSize, bgColor, linkColor);
        gopherIcon = getLinkIcon("🐹", "Noto Emoji", gfFontSize, bgColor, linkColor);
        spartanIcon = getLinkIcon("💪", "Noto Emoji", gfFontSize, bgColor, linkColor);

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

        if (page.isNex() && docURL.endsWith("/")) {

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

                String linkStyle = f.isClickedLink(url) ? "visited" : "=>";

                ClickableRange cr = addStyledText(label.isEmpty() ? url : label, linkStyle,
                        () -> {
                            String useB = DB.getPref("browser", null);
                            boolean useBrowser = useB == null ? true : useB.equals("true");
                            if (Alhena.httpProxy == null && finalUrl.startsWith("https") && Alhena.browsingSupported && useBrowser) {
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
                                f.addClickedLink(finalUrl);
                                f.fetchURL(finalUrl, false);
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

                    BufferedImage bi = AsciiImage.renderTextToImage(asciiSB.toString(), pageStyle.getMonoFontFamily(), pageStyle.getMonoFontSize(), getBackground(), pageStyle.getMonoFontColor(), false);
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
                    ptp.removeLastChar();
                    ptp.scrollLeft();
                    ptp = null;
                }
                addStyledText("\n", "```", null);
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
            if ((currentMode == BOOKMARK_MODE || currentMode == CERT_MODE) && line.startsWith("=>")) {
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
            addStyledText(line.substring(3).trim(), "###", null);
        } else if (line.startsWith("##")) {
            addStyledText(line.substring(2).trim(), "##", null);
        } else if (line.startsWith("#")) {
            addStyledText(line.substring(1).trim(), "#", null);
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
            if (currentMode == BOOKMARK_MODE || currentMode == CERT_MODE) {
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

            if (Alhena.linkIcons) {

                boolean isImage = Alhena.imageExtensions.stream().anyMatch(url.toLowerCase()::endsWith);
                boolean isMedia = false;
                if (!isImage) {
                    String mimeExt = MimeMapping.getMimeTypeForFilename(finalUrl);
                    isMedia = (url.toLowerCase().endsWith(".opus") || (mimeExt != null && (mimeExt.startsWith("audio") || mimeExt.startsWith("video"))));
                }

                if (isImage) {
                    sfx = "🌠";
                } else if (isMedia) {
                    sfx = "🎥";

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
                    } else {
                        sfx = "🌐";
                    }
                    //sfx = !finalUrl.startsWith("gemini") ? "🌐" : "🔗";

                }
            }
            label = ll.substring(i).trim();
            String linkStyle = f.isClickedLink(url) ? "visited" : "=>";

            ClickableRange cr = addStyledText(label.isEmpty() ? sfx + url.replace("/", "/\u200B") : sfx + label, linkStyle,
                    () -> {

                        String useB = DB.getPref("browser", null);
                        boolean useBrowser = useB == null ? true : useB.equals("true");
                        if (spartanLink) {

                            TextEditor textEditor = new TextEditor("", false, GeminiTextPane.this);
                            Object[] comps = new Object[1];
                            comps[0] = textEditor;
                            Object res = Util.inputDialog2(f, "Edit", comps, null, true);
                            if (res != null) {
                                Object result = textEditor.getResult();
                                if (result instanceof String string) {
                                    if (!string.isBlank()) {
                                        f.addClickedLink(finalUrl);
                                        f.fetchURL(finalUrl + "?" + Util.uEncode(string), false);
                                    }
                                } else {
                                    f.addClickedLink(finalUrl);
                                    f.fetchURL(finalUrl, (File) result, false);
                                }
                            }

                        } else if (Alhena.httpProxy == null && finalUrl.startsWith("https") && Alhena.browsingSupported && useBrowser) {
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
                            f.toggleCert(id, !active, finalUrl);

                        } else {
                            f.addClickedLink(finalUrl);
                            f.fetchURL(finalUrl, false);
                        }

                    });
            if (Alhena.dataUrl && dataUrl) { // auto view
                dataURL(url, true);
                cr.imageIndex = cr.end;
            }
            cr.dataUrl = dataUrl;
            cr.url = url;
            cr.directive = directive[0];

        } else if (line.startsWith(">")) {
            addStyledText(line.substring(1).trim(), ">", null);
        } else if (line.startsWith("* ")) {
            addStyledText("• " + line.substring(1).trim(), "*", null);

        } else {
            addStyledText(line, "text", null);
        }
    }

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
                    Util.infoDialog(f, I18n.t("vlcRequiredDialog"), I18n.t("vlcRequiredDialogMsg"));
                }
                return;
            }
            Alhena.pauseMedia();
            File af;
            try {
                af = File.createTempFile("alhena", "media");
                af.deleteOnExit();
                Files.write(af.toPath(), byteData);  // overwrite or create

                MediaComponent ap = mime.startsWith("audio") ? new AudioPlayer() : new VideoPlayer();

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
                    BufferedImage bi = AsciiImage.renderTextToImage(s, pageStyle.getMonoFontFamily(), pageStyle.getMonoFontSize(), getBackground(), pageStyle.getMonoFontColor(), false);
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

    private PreformattedTextPane createTextComponent(boolean curPos) {

        Color background = shadePF ? AnsiColor.adjustColor(getBackground(), isDark, .2d, .8d, .05d) : getBackground();
        PreformattedTextPane pfTextPane = new PreformattedTextPane(background, customFontSize == 0 ? null : customFontSize, isDark, GeminiTextPane.this);

        JScrollPane sp = new JScrollPane(pfTextPane);
        pfTextPane.setFocusTraversalKeysEnabled(false);
        EventQueue.invokeLater(() -> pfTextPane.setCaretPosition(0));

        pfTextPane.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent ke) {
                if (ke.getKeyCode() == KeyEvent.VK_TAB) {

                    JButton fButton = f.backButton.isEnabled() ? f.backButton : f.forwardButton.isEnabled() ? f.forwardButton : f.refreshButton;

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
                    f.setTmpStatus(I18n.t("holdToScrollLabel"));
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                f.setTmpStatus(" ");
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
            int cpCount = text.codePointCount(0, text.length());
            int nudge = 0;
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
                                mediaIcon;
                            case "🐭" ->
                                gopherIcon;
                            case "💪" ->
                                spartanIcon;
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
                        nudge = 1;
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

                            char[] chars = Character.toChars(text.codePointAt(i));

                            i = emoji.getEndCharIndex() + 1;

                            insertString(doc.getLength(), new String(chars), style);

                        } else {
                            int eci = emoji.getEndCharIndex();

                            // single char emoji followed by unneccessary variation selector
                            // example: snowman
                            int opto = 0;

                            int emojiSize = eci - emoji.getCharIndex();

                            i += (emojiSize - 1);
                            int charPointOfNextChar = emoji.getCodePointIndex() + 1;

                            if (emojiSize == 1 && charPointOfNextChar < cpCount && isEmojiVariationSelector(text.codePointAt(charPointOfNextChar + nudge))) {
                                i++; // skip any variation selector
                                opto = 1;
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

                        i += (emojiSize - 1);
                        int charPointOfNextChar = emoji.getCodePointIndex() + 1;

                        if (emojiSize == 1 && charPointOfNextChar < cpCount && isEmojiVariationSelector(text.codePointAt(charPointOfNextChar))) {
                            i++; // skip any variation selector
                            eci++;
                        }

                        if (eci < text.length()) { // optomize common scenario
                            if (emojis.indexOf(emoji) == emojis.size() - 1) { // this is last emoji
                                StyleConstants.setFontFamily(style, fontFamily);
                                insertString(doc.getLength(), text.substring(eci), style);
                                break;
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

    public static boolean isEmojiVariationSelector(int codePoint) {
        return codePoint == 0xFE0E || codePoint == 0xFE0F;
    }

    private final static StringBuilder sb = new StringBuilder();

    private static String unescapeUnicode(String input) { // not thread safe only use on EDT
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

    public static class ClickableRange {

        int start;
        int end;
        Runnable action;
        String url;
        String directive;
        int imageIndex = -1;
        boolean dataUrl;

        ClickableRange(int start, int end, Runnable action) {
            this.start = start;
            this.end = end;
            this.action = action;

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
        return new CurrentPage(pageBuffer, preformattedMode);
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
            //System.out.println(cr.url);
            if (isClickableRangeVisible(cr)) {
                visibleList.add(cr);
            }
        }
        return visibleList;
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

    private static ImageIcon dataIcon, mailIcon, geminiIcon, otherIcon, titanIcon, picIcon, mediaIcon, gopherIcon, spartanIcon;

    private static ImageIcon getLinkIcon(String txt, String fontName, int fontSize, Color bgColor, Color fgColor) {
        BufferedImage bi = AsciiImage.renderTextToImage(txt, fontName, fontSize, bgColor, fgColor, true);
        // this could be optimized as AsciiImage already created font and metrics
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        return new BaselineShiftedIcon(bi, metrics.getDescent() / 2);

    }

}
