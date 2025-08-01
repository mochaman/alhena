package brad.grier.alhena;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
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
import com.techsenger.ansi4j.core.api.Environment;
import com.techsenger.ansi4j.core.api.Fragment;
import com.techsenger.ansi4j.core.api.FragmentType;
import com.techsenger.ansi4j.core.api.ParserFactory;
import com.techsenger.ansi4j.core.api.TextFragment;
import com.techsenger.ansi4j.core.api.iso6429.ControlFunctionType;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryConfig;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryService;
import com.techsenger.ansi4j.core.impl.ParserFactoryProvider;

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
    private String currentStatus = Alhena.WELCOME_MESSAGE;
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

    // REMOVE AUTOSCROLL - INTERFERING WITH TEXT SELECTION
    // private static final int INITIAL_SCROLL_SPEED = 1;
    // private static final int MAX_SCROLL_SPEED = 50;
    // private static final int INITIAL_DELAY = 100;
    // private static final int MIN_DELAY = 30;
    // private static final int EDGE_MARGIN = 50;
    // private Timer pressTimer;
    // private int scrollDirection = 0;
    // private int holdTime = 0;
    private final Page page;

    //private static final ConcurrentHashMap<String, Point> emojiSheetMap = new ConcurrentHashMap<>();
    public static final HashMap<String, Point> emojiSheetMap = new HashMap<>();
    public static BufferedImage sheetImage = null;
    public static int indent;
    public static float contentPercentage = .80f;
    public static boolean wrapPF;
    public static boolean asciiImage;
    public static boolean embedPF;
    public static boolean showSB;
    public static boolean shadePF;
    private PreformattedTextPane ptp;
    private StringBuilder asciiSB;

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

        boolean smoothPref = DB.getPref("smoothscrolling", "true").equals("true");
        if (smoothPref) {
            setupAdaptiveScrolling();
        }
        addMouseMotionListener(new MouseAdapter() {
            // remove autoscroll - interfering with ctrl+c
            // @Override
            // public void mouseDragged(MouseEvent e) {
            //     if (SwingUtilities.isLeftMouseButton(e)) {

            //         checkScroll(e, scrollPane);
            //     }
            // }
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
                                AttributeSet originalStyle = doc.getCharacterElement(range.start + 1).getAttributes();
                                Color hoverC = StyleConstants.getForeground(originalStyle).brighter();
                                SimpleAttributeSet sa = new SimpleAttributeSet();
                                StyleConstants.setForeground(sa, hoverC);

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
                if (!entered && !" ".equals(currentStatus) && currentStatus != null && !currentStatus.equals(Alhena.WELCOME_MESSAGE)) {

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
            // remove autoscroll which is interfering with text selection
            // @Override
            // public void mousePressed(MouseEvent e) {
            //     if (SwingUtilities.isLeftMouseButton(e) && currentCursor != Cursor.HAND_CURSOR) {

            //         checkScroll(e, scrollPane);
            //     }
            // }
            // @Override
            // public void mouseReleased(MouseEvent e) {
            //     stopScrolling();
            // }
            @Override
            public void mouseClicked(MouseEvent e) {

                showPopup(e);

            }
        });

        setDropTarget(new DropTarget() {
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
                                    JMenuItem copyItem = new JMenuItem("Copy");

                                    copyItem.addActionListener(ev -> {
                                        copyText(selectedText);
                                    });
                                    popupMenu.add(copyItem);

                                }
                                JMenuItem copyLinkItem = new JMenuItem("Copy Link");

                                copyLinkItem.addActionListener(ev -> {
                                    copyText(range.url);
                                });
                                popupMenu.add(copyLinkItem);

                                popupMenu.add(new JSeparator());
                                JMenuItem menuItem1 = new JMenuItem("Open in New Tab");

                                menuItem1.addActionListener(ev -> {
                                    f.newTab(range.url);
                                });
                                popupMenu.add(menuItem1);
                                JMenuItem menuItem2 = new JMenuItem("Open in New Window");

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
                                    String label = useBrowser ? "Alhena" : "Browser";
                                    JMenuItem httpMenuItem = new JMenuItem("Open In " + label);
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
                                        JMenuItem exportItem = new JMenuItem("Export");
                                        exportItem.addActionListener(al -> {
                                            f.exportCert(id, GeminiTextPane.this);
                                        });
                                        popupMenu.add(exportItem);
                                        String command = active ? "Deactivate" : "Activate";
                                        JMenuItem actionItem = new JMenuItem(command);
                                        actionItem.addActionListener(al -> {

                                            f.toggleCert(id, !active, range.url);
                                        });
                                        popupMenu.add(actionItem);
                                        JMenuItem delItem = new JMenuItem("Delete");
                                        delItem.addActionListener(al -> {
                                            f.deleteCert(id);
                                        });
                                        popupMenu.add(delItem);
                                    }
                                    case HISTORY_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem forgetItem = new JMenuItem("Forget Link");
                                        forgetItem.addActionListener(al -> {
                                            f.deleteFromHistory(range.url, false);
                                        });

                                        popupMenu.add(forgetItem);
                                        JMenuItem clearItem = new JMenuItem("Delete History");

                                        clearItem.addActionListener(al -> {
                                            f.clearHistory();
                                        });
                                        popupMenu.add(clearItem);
                                    }
                                    case BOOKMARK_MODE -> {
                                        popupMenu.add(new JSeparator());
                                        JMenuItem editItem = new JMenuItem("Edit Bookmark");
                                        editItem.addActionListener(ev -> {
                                            f.updateBookmark(f, range.directive);

                                        });
                                        popupMenu.add(editItem);
                                        JMenuItem deleteItem = new JMenuItem("Delete Bookmark");
                                        deleteItem.addActionListener(ev -> {
                                            f.deleteBookmark(f, range.directive);

                                        });
                                        popupMenu.add(deleteItem);
                                    }
                                    default -> {
                                    }
                                }

                                popupMenu.show(e.getComponent(), e.getX(), e.getY());

                            } else {

                                if (range.imageIndex != -1) {
                                    removeItemAtIndex(range);
                                    range.imageIndex = -1;

                                } else {
                                    lastClicked = range;
                                    range.action.run();
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
                        JMenuItem copyItem = new JMenuItem("Copy");

                        copyItem.addActionListener(ev -> {
                            copyText(selectedText);
                        });
                        popupMenu.add(copyItem);
                        popupMenu.add(new JSeparator());

                    }

                    JMenuItem homePageMenuItem = new JMenuItem("Set As Home Page");
                    homePageMenuItem.addActionListener(al -> {
                        DB.insertPref("home", docURL);
                    });

                    popupMenu.add(homePageMenuItem);
                    homePageMenuItem.setEnabled(currentMode != INFO_MODE);
                    if (!page.isNex()) {
                        String menuText = plainTextMode ? "View As GemText" : "View As Plain Text";
                        JMenuItem ptMenuItem = new JMenuItem(menuText);
                        ptMenuItem.setEnabled(!imageOnly);
                        ptMenuItem.addActionListener(al -> {
                            plainTextMode = !plainTextMode;
                            f.toggleView(GeminiTextPane.this, plainTextMode);
                        });

                        popupMenu.add(ptMenuItem);
                    }

                    JMenuItem crtMenuItem = new JMenuItem("View Server Cert");
                    URI uri = getURI();
                    crtMenuItem.setEnabled(!imageOnly && currentMode == DEFAULT_MODE && uri != null && uri.getScheme().equals("gemini"));
                    crtMenuItem.addActionListener(al -> {

                        f.viewServerCert(GeminiTextPane.this, getURI());
                    });

                    popupMenu.add(crtMenuItem);

                    popupMenu.add(new JSeparator());
                    JMenuItem saveItem = new JMenuItem("Save Page");
                    saveItem.setEnabled(!imageOnly);
                    saveItem.addActionListener(al -> {
                        f.savePage(GeminiTextPane.this, pageBuffer, currentMode);
                    });

                    JMenuItem titanItem = new JMenuItem("Titan Editor");
                    titanItem.setEnabled(!imageOnly);
                    titanItem.addActionListener(al -> {

                        f.editPage();
                    });

                    popupMenu.add(saveItem);
                    popupMenu.add(titanItem);

                    if (currentMode == DEFAULT_MODE) {
                        JMenuItem pemItem = new JMenuItem("Import PEM");
                        pemItem.setEnabled(!imageOnly);
                        pemItem.addActionListener(al -> {
                            f.importPem(getURI(), null);
                        });
                        popupMenu.add(pemItem);

                        JMenuItem certItem = new JMenuItem("New Client Certificate");
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
                        JMenuItem whereItem = new JMenuItem("Forget Links Containing");
                        whereItem.addActionListener(al -> {
                            f.deleteFromHistory(null, true);
                        });

                        popupMenu.add(whereItem);
                        JMenuItem clearItem = new JMenuItem("Delete History");

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
            Element element = doc.getCharacterElement(index + 2);
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
                doc.remove(index + 2, 1); // Remove the image/component
                doc.remove(index + 1, 2);
                boolean start = false;
                for (ClickableRange range : clickableRegions) {
                    if (range == rg) {
                        start = true;
                        continue;
                    }
                    if (start) {
                        range.start = range.start - 3;
                        range.end = range.end - 3;
                        if (range.imageIndex != -1) {
                            range.imageIndex = range.imageIndex - 3;
                        }
                    }
                }
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

                doc.insertString(lastClicked.end + 1, "\n\n", null);
                doc.insertString(lastClicked.end + 2, " ", apStyle);
                lastClicked.imageIndex = lastClicked.end;
                boolean start = false;
                for (ClickableRange range : clickableRegions) {
                    if (range == lastClicked) {
                        start = true;
                        continue;
                    }
                    if (start) {
                        range.start = range.start + 3;
                        range.end = range.end + 3;
                        if (range.imageIndex != -1) {
                            range.imageIndex = range.imageIndex + 3;
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

    public void insertImage(byte[] imageBytes) {
        inserting = true;

        // 50 pixel fudge factor. Unable to land on a programmatic width insets plus scrollbar width, etc
        // that doesn't cause the horizontal scrollbar to appear
        int width = (int) contentWidth - 50;

        BufferedImage image = Util.getImage(imageBytes, width, width * 2);

        ImageIcon icon = new ImageIcon(image);

        SimpleAttributeSet emojiStyle = new SimpleAttributeSet();
        StyleConstants.setIcon(emojiStyle, icon);

        try {
            if (lastClicked == null) {
                doc.insertString(0, " ", emojiStyle);
                imageOnly = true;
            } else {

                doc.insertString(lastClicked.end + 1, "\n\n", null);
                doc.insertString(lastClicked.end + 2, " ", emojiStyle);
                lastClicked.imageIndex = lastClicked.end;
                boolean start = false;
                for (ClickableRange range : clickableRegions) {
                    if (range == lastClicked) {
                        start = true;
                        continue;
                    }
                    if (start) {
                        range.start = range.start + 3;
                        range.end = range.end + 3;
                        if (range.imageIndex != -1) {
                            range.imageIndex = range.imageIndex + 3;
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

    public void find(String word) {
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
                if (embedPF) {

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
        if (!found) {
            lastSearchDoc = -1;
            lastSearchIdx = 0;
            find(word);
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
        if (plainTextMode || !embedPF) {
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
                                    Object res = Util.confirmDialog(f, "ANSI", "This page uses ANSI escape sequences to style text.\nDo you want to render the page?", JOptionPane.YES_NO_OPTION, null, null);
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
        }

        var parser = factory.createParser(line);
        Fragment fragment = null;
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
            if(mIdx == -1){
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

        switch (docURL) {
            case GeminiFrame.HISTORY_LABEL ->
                currentMode = HISTORY_MODE;
            case GeminiFrame.BOOKMARK_LABEL ->
                currentMode = BOOKMARK_MODE;
            case GeminiFrame.CERT_LABEL ->
                currentMode = CERT_MODE;
            case GeminiFrame.INFO_LABEL ->
                currentMode = INFO_MODE;
            case GeminiFrame.SERVERS_LABEL ->
                currentMode = SERVER_MODE;
            default -> {
            }
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

        contentWidth = totalWidth * contentPercentage;
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

    private void buildStyles() {

        for (MediaComponent ap : playerList) {
            ap.dispose();
        }
        playerList.clear();

        emojiProportional = "Noto Emoji";
        if (SystemInfo.isMacOS) {
            boolean macUseNoto = DB.getPref("macusenoto", "false").equals("true");
            if (!macUseNoto) {
                emojiProportional = "SansSerif";
            }
        }

        isDark = UIManager.getBoolean("laf.dark");

        linkColor = UIManager.getColor("Component.linkColor");
        hoverColor = linkColor.brighter();

        Style pfStyle = doc.addStyle("```", null);
        StyleConstants.setFontFamily(pfStyle, monospacedFamily);
        StyleConstants.setFontSize(pfStyle, GeminiFrame.monoFontSize);
        StyleConstants.setBold(pfStyle, false);
        StyleConstants.setItalic(pfStyle, false);
        StyleConstants.setUnderline(pfStyle, false);

        Color foreground = UIManager.getColor("TextField.foreground");

        Style h1Style = doc.addStyle("###", null);
        StyleConstants.setFontFamily(h1Style, GeminiFrame.proportionalFamily);
        StyleConstants.setFontSize(h1Style, GeminiFrame.fontSize + 3); // 18

        StyleConstants.setBold(h1Style, false);
        StyleConstants.setItalic(h1Style, false);
        StyleConstants.setUnderline(h1Style, false);
        //Color hColor = UIManager.getColor("TextField.selectionBackground");
        Color hColor = AnsiColor.adjustColor(isDark ? linkColor.brighter() : linkColor.darker(), isDark, .1, .9, .2);
        //hColor = isDark ? hColor.brighter() : hColor.darker();
        StyleConstants.setForeground(h1Style, hColor);

        Style h2Style = doc.addStyle("##", h1Style);
        StyleConstants.setFontSize(h2Style, GeminiFrame.fontSize + 9); // 24

        Style h3Style = doc.addStyle("#", h1Style);
        StyleConstants.setFontSize(h3Style, GeminiFrame.fontSize + 17); // 32

        Style linkStyle;
        if (page.isNex()) {
            linkStyle = doc.addStyle("=>", h1Style);
            StyleConstants.setFontFamily(linkStyle, monospacedFamily);
            StyleConstants.setFontSize(linkStyle, GeminiFrame.monoFontSize);
        } else {
            linkStyle = doc.addStyle("=>", h1Style);
            StyleConstants.setFontSize(linkStyle, GeminiFrame.fontSize);

        }
        StyleConstants.setForeground(linkStyle, linkColor);
        //StyleConstants.setBold(linkStyle, true);

        Style clickedStyle = doc.addStyle("visited", linkStyle);
        StyleConstants.setForeground(clickedStyle, isDark ? linkColor.darker() : linkColor.brighter());

        Style quoteStyle = doc.addStyle(">", h1Style);
        StyleConstants.setFontSize(quoteStyle, GeminiFrame.fontSize);
        StyleConstants.setItalic(quoteStyle, true);
        //StyleConstants.setBold(quoteStyle, false);
        StyleConstants.setForeground(quoteStyle, foreground);

        Style textStyle = doc.addStyle("text", h1Style);
        StyleConstants.setFontSize(textStyle, GeminiFrame.fontSize);
        //StyleConstants.setBold(textStyle, false);
        StyleConstants.setForeground(textStyle, foreground);

        Style listStyle = doc.addStyle("*", textStyle);

        hoverStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(hoverStyle, hoverColor);

        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, linkColor);

        visitedStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(visitedStyle, isDark ? linkColor.darker() : linkColor.brighter());

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
                if (asciiSB != null) {
                    asciiSB.deleteCharAt(asciiSB.length() - 1);
                    
                    BufferedImage bi = AsciiImage.renderTextToImage(asciiSB.toString(), monospacedFamily, GeminiFrame.monoFontSize, getBackground(), getForeground());
                    ImageIcon icon = new ImageIcon(bi);
                    if (ptp == null) {
                        insertComp(new JLabel(icon), doc.getLength());
                    } else {
                        // insert JLabel into ptp
                        ptp.insertComp(new JLabel(icon));
                        ptp = null;
                    }
                    asciiSB = null;
                }
                if (ptp != null) {

                    ptp.end();
                    ptp.removeLastChar();
                    ptp = null;
                }
                addStyledText("\n", "```", null);
            } else { // preformatted mode

                if (asciiImage) {
                    if (!embedPF) {
                        addStyledText("", "```", null);
                    }
                    asciiSB = new StringBuilder();
                }
                if (embedPF) {

                    addStyledText("\n", "```", null);
                    ptp = (PreformattedTextPane) createTextComponent();

                }else{
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
        } else if (line.startsWith("=>") || (line.startsWith("=: ") && page.isSpartan())) {
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
            String finalUrl = url;
            String label = ll.substring(i).trim();

            String linkStyle = f.isClickedLink(url) ? "visited" : "=>";

            ClickableRange cr = addStyledText(label.isEmpty() ? url.replace("/", "/\u200B") : label, linkStyle,
                    () -> {
                        String useB = DB.getPref("browser", null);
                        boolean useBrowser = useB == null ? true : useB.equals("true");
                        if (spartanLink) {

                            TextEditor textEditor = new TextEditor("", false);
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

    private Component createTextComponent() {

        Color background = shadePF ? AnsiColor.adjustColor(getBackground(), UIManager.getBoolean("laf.dark"), .2d, .8d, .05d) : getBackground();
        PreformattedTextPane pfTextPane = new PreformattedTextPane(background, customFontSize == 0 ? null : customFontSize, isDark);

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
            public void mouseEntered(MouseEvent e) {
                if (checkScrollingNeeded(sp)) {
                    f.setTmpStatus("Hold 's' key to scroll");
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
        insertComp(sp, doc.getLength());
        addStyledText("", "```", null);
        ptpList.add(pfTextPane);
        return pfTextPane;
    }
    private List<PreformattedTextPane> ptpList;

    private ClickableRange addStyledText(String text, String styleName, Runnable action) {

        Style style = doc.getStyle(styleName);

        ClickableRange cr = null;

        int start = doc.getLength();

        if (!(hasAnsi && styleName.equals("```")) && EmojiManager.containsAnyEmoji(text)) {

            String fontFamily = StyleConstants.getFontFamily(style);
            int fontSize = StyleConstants.getFontSize(style);

            SimpleAttributeSet emojiStyle = new SimpleAttributeSet(style);

            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(text);

            IndexedEmoji emoji;
            // can't iterate by code point without preprocessing first to get name
            for (int i = 0; i < text.length(); i++) {

                if ((emoji = isEmoji(emojis, i)) != null) {

                    if (sheetImage != null) {

                        String key = getEmojiHex(emoji);

                        Point p = emojiSheetMap.get(key);
                        ImageIcon icon = null;
                        int imgSize = fontSize + 4;
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
                            // single char emoji followed by unneccessary variation selector
                            // example: snowman
                            if (i == emoji.getEndCharIndex() - 1) {
                                i++;
                            } else {

                                i = emoji.getEndCharIndex() - 1;

                            }

                            StyleConstants.setIcon(emojiStyle, icon);
                            try {
                                doc.insertString(doc.getLength(), " ", emojiStyle); // Use emoji style
                            } catch (BadLocationException ex) {
                            }

                        }
                    } else {

                        char[] chars = Character.toChars(text.codePointAt(i));

                        i++;

                        StyleConstants.setFontFamily(style, emojiProportional);

                        insertString(doc.getLength(), new String(chars), style);
                    }
                } else {

                    StyleConstants.setFontFamily(style, fontFamily);

                    insertString(doc.getLength(), String.valueOf(text.charAt(i)), style);

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

    private void insertString(int length, String txt, Style style) {
        try {
            if (hasAnsi && preformattedMode) {
                handleAnsi(txt);
            } else {
                doc.insertString(length, txt, style);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    public static class ClickableRange {

        int start;
        int end;
        Runnable action;
        String url;
        String directive;
        int imageIndex = -1;

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

// AUTOSCROLL - INTERFERING WITH TEXT SELECTION
    // private void checkScroll(MouseEvent e, JScrollPane scrollPane) {
    //     Point point = e.getPoint();
    //     Rectangle viewRect = scrollPane.getViewport().getViewRect();
    //     if (point.y <= viewRect.y + EDGE_MARGIN) {
    //         startScrolling(scrollPane, -1); // Scroll up
    //     } else if (point.y >= viewRect.y + viewRect.height - EDGE_MARGIN) {
    //         startScrolling(scrollPane, 1); // Scroll down
    //     } else {
    //         stopScrolling();
    //     }
    // }
    // private void startScrolling(JScrollPane scrollPane, int direction) {
    //     if (pressTimer != null && pressTimer.isRunning() && scrollDirection == direction) {
    //         return;
    //     }
    //     stopScrolling(); // Ensure only one timer is running
    //     scrollDirection = direction;
    //     holdTime = 0; // Reset hold time
    //     pressTimer = new Timer(INITIAL_DELAY, e -> {
    //         holdTime++; // Increment hold time
    //         // Dynamically adjust speed based on hold time
    //         int scrollSpeed = Math.min(INITIAL_SCROLL_SPEED + holdTime, MAX_SCROLL_SPEED);
    //         int newDelay = Math.max(INITIAL_DELAY - (holdTime * 5), MIN_DELAY); // Reduce delay over time
    //         // Apply new speed and delay
    //         JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
    //         int newValue = verticalBar.getValue() + (scrollSpeed * direction);
    //         verticalBar.setValue(newValue);
    //         // Reset caret position to avoid selection
    //         setCaretPosition(getCaretPosition());
    //         pressTimer.setDelay(newDelay); // Update timer delay
    //     });
    //     pressTimer.start();
    // }
    // private void stopScrolling() {
    //     if (pressTimer != null) {
    //         pressTimer.stop();
    //         pressTimer = null;
    //     }
    //     scrollDirection = 0;
    //     holdTime = 0; // Reset hold time
    // }
    public static ImageIcon extractSprite(int sheetX, int sheetY, int sheetSize, int width, int height, int fontSize) {

        int x = (sheetX * (sheetSize + 2)) + 1;
        int y = (sheetY * (sheetSize + 2)) + 1;

        BufferedImage bi = sheetImage.getSubimage(x, y, sheetSize, sheetSize);
        Image scaledImg = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return new BaselineShiftedIcon(scaledImg, fontSize / 10);
    }

    public static Image extractSpriteImage(int sheetX, int sheetY, int sheetSize, int width, int height, int fontSize) {

        int x = (sheetX * (sheetSize + 2)) + 1;
        int y = (sheetY * (sheetSize + 2)) + 1;

        BufferedImage bi = sheetImage.getSubimage(x, y, sheetSize, sheetSize);
        Image scaledImg = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);

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
                        cr.action.run();
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

}
