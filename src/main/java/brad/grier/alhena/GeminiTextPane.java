package brad.grier.alhena;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.InputEvent;
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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
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

import brad.grier.alhena.GeminiFrame.ClosableTabPanel;
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
    private final String monospacedFamily;
    private final GeminiFrame f;
    // use StringBuilder instead of StringBuffer as only updated in EventDispatch at creation
    private StringBuilder pageBuffer;
    private String docURL;
    private ClickableRange saveRange;
    private String bufferedLine = null;
    private Color hoverColor, linkColor;
    private SimpleAttributeSet hoverStyle, normalStyle, visitedStyle;
    // doubtful there will actually be multi-threaded access but better safe than sorry
    //private static final ConcurrentHashMap<FontInfo, Integer> sizeMap = new ConcurrentHashMap<>();
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
    private final int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private Point mousePoint;
    private static final List<String> dropExtensions;
    private boolean imageOnly;
    private JScrollPane scrollPane;
    private static final int INITIAL_SCROLL_SPEED = 1;
    private static final int MAX_SCROLL_SPEED = 50;
    private static final int INITIAL_DELAY = 100;
    private static final int MIN_DELAY = 30;
    private static final int EDGE_MARGIN = 50;

    private Timer pressTimer;
    private int scrollDirection = 0;
    private int holdTime = 0;
    private final Page page;
    private int pfModeStart;
    private AttributeSet pAttributes;
    private static final ConcurrentHashMap<String, Point> emojiSheetMap = new ConcurrentHashMap<>();
    private static BufferedImage sheetImage = null;

    static {
        dropExtensions = new ArrayList<>(Alhena.fileExtensions);
        dropExtensions.add(".png");
        dropExtensions.add(".jpg");

        String emojiPref = DB.getPref("emoji", null);
        if (emojiPref == null || emojiPref.equals("google")) { // first time or the default set from jar
            setSheetImage(Util.loadImage(GeminiFrame.emojiNameMap.get("google")));
            DB.insertPref("emoji", "google");  // only really need to this for null
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

        }
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

        // Monospaced doesn't align on Windows, Source Code Pro doesn't align on Mac/Linux!
        monospacedFamily = SystemInfo.isWindows ? "Source Code Pro" : "Monospaced";

        Insets insets = getMargin();
        setMargin(new Insets(35, insets.left, insets.bottom, insets.right));

        setEditable(false);
        setCaret(new DefaultCaret() {
            @Override
            public void paint(Graphics g) {
                // do nothing to prevent caret from being painted
            }
        });
        boolean smoothPref = DB.getPref("smoothscrolling", "true").equals("true");
        if (smoothPref) {
            setupAdaptiveScrolling();
        }
        addMouseMotionListener(new MouseAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (scrollPane == null) {
                        scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, GeminiTextPane.this);
                    }
                    checkScroll(e, scrollPane);
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {

                mousePoint = e.getPoint();
                int pos = viewToModel2D(e.getPoint());
                boolean entered = false;

                for (ClickableRange range : clickableRegions) {
                    if (pos >= range.start && pos < range.end) {

                        Rectangle rect = getCharacterBounds(GeminiTextPane.this, range.start, range.end);
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

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && currentCursor != Cursor.HAND_CURSOR) {
                    if (scrollPane == null) {
                        scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, GeminiTextPane.this);
                    }

                    checkScroll(e, scrollPane);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                stopScrolling();
            }

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
                        boolean matches = dropExtensions.stream().anyMatch(lcName::endsWith);
                        if (matches) {

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

    }

    public static void setSheetImage(BufferedImage sheet) {
        sheetImage = sheet;
    }

    private void showPopup(MouseEvent e) {
        if (doc != null) {
            int pos = viewToModel2D(e.getPoint());
            boolean linkClicked = false;
            if (pos >= 0 && pos < doc.getLength()) {
                for (ClickableRange range : clickableRegions) {
                    if (pos >= range.start && pos < range.end) {
                        Rectangle rect = getCharacterBounds(GeminiTextPane.this, range.start, range.end);
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

                                if (Alhena.browsingSupported && range.url.startsWith("http")) {
                                    String useB = DB.getPref("browser", null);
                                    boolean useBrowser = useB == null ? true : useB.equals("true");
                                    //boolean useBrowser = DB.getPref("browser", "false").equals("true");
                                    // show the opposite of the setting - this then becomes the default
                                    String label = useBrowser ? "Alhena" : "Browser";
                                    JMenuItem httpMenuItem = new JMenuItem("Open In " + label);
                                    httpMenuItem.addActionListener(al -> {
                                        if (!useBrowser) {
                                            try {
                                                Desktop.getDesktop().browse(new URI(range.url));
                                            } catch (Exception ex) {
                                                ex.printStackTrace();
                                            }
                                        } else {
                                            f.fetchURL(range.url);
                                        }
                                        DB.insertPref("browser", String.valueOf(!useBrowser));
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
                                    removeImageAtIndex(range);
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
                    String menuText = plainTextMode ? "View As GemText" : "View As Plain Text";
                    JMenuItem ptMenuItem = new JMenuItem(menuText);
                    ptMenuItem.setEnabled(!imageOnly);
                    ptMenuItem.addActionListener(al -> {
                        plainTextMode = !plainTextMode;
                        f.toggleView(GeminiTextPane.this, plainTextMode);
                    });

                    popupMenu.add(ptMenuItem);

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

                    popupMenu.add(saveItem);

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
                            //f.importPem(getURI(), null);
                        });
                        popupMenu.add(certItem);
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
            if (e.getKeyCode() == KeyEvent.VK_I) {
                if (e.getID() == KeyEvent.KEY_PRESSED) {
                    long now = System.currentTimeMillis();
                    int x = mousePoint.x;
                    int y = mousePoint.y;
                    MouseEvent pressEvent = new MouseEvent(this, MouseEvent.MOUSE_PRESSED, now,
                            InputEvent.BUTTON3_DOWN_MASK, x, y, 1, false, MouseEvent.BUTTON3);
                    showPopup(pressEvent);

                }

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

    private void removeImageAtIndex(ClickableRange rg) {

        int index = rg.imageIndex;
        try {
            Element element = doc.getCharacterElement(index + 2);
            AttributeSet attrs = element.getAttributes();

            if (StyleConstants.getIcon(attrs) != null) {
                doc.remove(index + 2, 1); // Remove the image
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

    public void insertImage(byte[] imageBytes) {

        int width = (int) (f.getWidth() * .85);
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

    public boolean find(String word) {
        boolean found = false;
        try {
            requestFocus();
            int startIdx = word.equals(lastSearch) ? lastSearchIdx : 0;
            lastSearch = word;

            String text = doc.getText(0, doc.getLength());

            int pos = text.indexOf(word, startIdx);
            if (pos >= 0) {
                // select the word
                setCaretPosition(pos);
                moveCaretPosition(pos + word.length());
                lastSearchIdx = pos + word.length();
                // scroll to the position
                Rectangle viewRect = modelToView2D(pos).getBounds();
                scrollRectToVisible(viewRect);
                found = true;
            } else {

                f.setStatus("'" + word + "' Not Found");

            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return found;
    }

    public void end() {
        if (bufferedLine != null) {
            String lrl = bufferedLine;
            bufferedLine = null;
            processLine(lrl, false);
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
                            ClosableTabPanel ctp = (ClosableTabPanel) tabbedPane.getTabComponentAt(i);
                            ctp.setTitle(title);
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

        scanForAnsi();
        bStyle = null;
        defPP = null;
        page.doneLoading();

        page.setBusy(false);
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
                                    Object res = Util.confirmDialog(f, "ANSI", "This page uses ANSI escape sequences to style text.\nDo you want to render the page?", JOptionPane.YES_NO_OPTION, null);
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
    private boolean hasAnsi;

    private void handleAnsi(String line, Style style) {
        if (factory == null) {
            ParserFactoryConfig config = new ParserFactoryConfig();
            config.setEnvironment(Environment._8_BIT);
            config.setFunctionTypes(List.of(ControlFunctionType.C0_SET, ControlFunctionType.C1_SET));
            ParserFactoryService factoryService = new ParserFactoryProvider();
            factory = factoryService.createFactory(config);
        }
        if (bStyle == null) {
            bStyle = new SimpleAttributeSet();

            // probably not needed anymore
            bStyle.addAttribute(StyleConstants.FontFamily, monospacedFamily);
            bStyle.addAttribute(StyleConstants.FontSize, GeminiFrame.fontSize);

        }

        var parser = factory.createParser(line);
        Fragment fragment = null;
        while ((fragment = parser.parse()) != null) {
            if (fragment.getType() == FragmentType.TEXT) {
                TextFragment textFragment = (TextFragment) fragment;

                convert(textFragment.getText(), style);
            }

        }
    }
    SimpleAttributeSet bStyle;

    private void convert(String txt, Style style) {

        if (!txt.startsWith("[")) {

            try {
                doc.insertString(doc.getLength(), txt, bStyle);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        } else {
            int mIdx = txt.indexOf('m');
            String line = txt.substring(mIdx + 1);

            String ansi = txt.substring(0, mIdx + 1);

            String[] tokens = ansi.split(";");
            switch (tokens[0]) {
                case "[38" -> {
                    // foreground color
                    if (tokens[1].equals("5")) {

                        if (tokens.length == 4) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                            ansiFG(c);

                        } else if (tokens.length == 3) {

                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2].substring(0, tokens[2].indexOf('m'))));
                            ansiFG(c);

                        }

                    }
                }
                case "[30m" ->
                    ansiFG(AnsiColor.BLACK);
                case "[31m" ->
                    ansiFG(AnsiColor.RED);
                case "[32m" ->
                    ansiFG(AnsiColor.GREEN);
                case "[33m" ->
                    ansiFG(AnsiColor.YELLOW);
                case "[34m" ->
                    ansiFG(AnsiColor.BLUE);
                case "[35m" ->
                    ansiFG(AnsiColor.MAGENTA);
                case "[36m" ->
                    ansiFG(AnsiColor.CYAN);
                case "[37m" ->
                    ansiFG(AnsiColor.WHITE);
                case "[40m" ->
                    ansiBG(Color.BLACK);
                case "[41m" ->
                    ansiBG(AnsiColor.RED);
                case "[42m" ->
                    ansiBG(AnsiColor.GREEN);
                case "[43m" ->
                    ansiBG(AnsiColor.YELLOW);
                case "[44m" ->
                    ansiBG(AnsiColor.BLUE);
                case "[45m" ->
                    ansiBG(AnsiColor.MAGENTA);
                case "[46m" ->
                    ansiBG(AnsiColor.CYAN);
                case "[47m" ->
                    ansiBG(AnsiColor.WHITE);
                case "[48" -> {
                    // foreground color
                    if (tokens[1].equals("5")) {

                        if (tokens.length == 4) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                            ansiBG(c);

                        } else if (tokens.length == 3) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2].substring(0, tokens[2].indexOf('m'))));
                            ansiBG(c);

                        }

                    }
                }
                case "[90m" ->
                    ansiFG(AnsiColor.BRIGHT_BLACK);
                case "[91m" ->
                    ansiFG(AnsiColor.BRIGHT_RED);
                case "[92m" ->
                    ansiFG(AnsiColor.BRIGHT_GREEN);
                case "[93m" ->
                    ansiFG(AnsiColor.BRIGHT_YELLOW);
                case "[94m" ->
                    ansiFG(AnsiColor.BRIGHT_BLUE);
                case "[95m" ->
                    ansiFG(AnsiColor.BRIGHT_MAGENTA);
                case "[96m" ->
                    ansiFG(AnsiColor.BRIGHT_CYAN);
                case "[97m" ->
                    ansiFG(AnsiColor.BRIGHT_WHITE);
                case "[100m" ->
                    ansiBG(AnsiColor.BRIGHT_BLACK);
                case "[101m" ->
                    ansiBG(AnsiColor.BRIGHT_RED);
                case "[102m" ->
                    ansiBG(AnsiColor.BRIGHT_GREEN);
                case "[103m" ->
                    ansiBG(AnsiColor.BRIGHT_YELLOW);
                case "[104m" ->
                    ansiBG(AnsiColor.BRIGHT_BLUE);
                case "[105m" ->
                    ansiBG(AnsiColor.BRIGHT_MAGENTA);
                case "[106m" ->
                    ansiBG(AnsiColor.BRIGHT_CYAN);
                case "[107m" ->
                    ansiBG(AnsiColor.BRIGHT_WHITE);
                case "[0m" -> {
                    bStyle = new SimpleAttributeSet();
                    bStyle.addAttribute(StyleConstants.FontFamily, monospacedFamily);
                    bStyle.addAttribute(StyleConstants.FontSize, GeminiFrame.fontSize);

                }
                case "[1m" -> {
                    StyleConstants.setBold(bStyle, true);
                }
                default ->
                    System.out.println("unknown: " + txt);
            }

            try {

                // DO NOT CALL the insertString method in this class!!!!!
                doc.insertString(doc.getLength(), line, bStyle);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void ansiFG(Color c) {
        StyleConstants.setForeground(bStyle, AnsiColor.adjustColor(c, UIManager.getBoolean("laf.dark"), .2d, .8d, .15d));
    }

    private void ansiBG(Color c) {
        StyleConstants.setBackground(bStyle, AnsiColor.adjustColor(c, UIManager.getBoolean("laf.dark"), .2d, .8d, .15d));
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

        //f.setStatus(" ");
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
        if (defPP == null) { // create and save the default paragraph attributes
            defPP = new SimpleAttributeSet();
            StyleConstants.setLeftIndent(defPP, 50); // Set left indentation in points
            StyleConstants.setRightIndent(defPP, 50); // Optional: Adjust the right margin
            StyleConstants.setLineSpacing(defPP, 0.3f); // Optional: Set line spacing if needed
            StyleConstants.setForeground(defPP, getForeground());
        }
        doc.setParagraphAttributes(0, doc.getLength(), defPP, false);

        addPage(geminiDoc);

    }

    private SimpleAttributeSet defPP;

    public void addPage(String geminiDoc) {
        pageBuffer.append(geminiDoc);

        if (bufferedLine != null) {
            geminiDoc = bufferedLine + geminiDoc;
            bufferedLine = null;
        }
        if (geminiDoc.endsWith("\n")) {

            geminiDoc.lines().forEach(line -> {
                processLine(line, false); // no way to know if a line is the last line
            });
        } else {
            int lastNl = geminiDoc.lastIndexOf("\n");
            if (lastNl == -1) {
                // no newlines at all save
                bufferedLine = geminiDoc;
            } else {
                bufferedLine = geminiDoc.substring(lastNl + 1);
                geminiDoc.substring(0, lastNl + 1).lines().forEach(line -> {
                    processLine(line, false); // no way to know if a line is the last line
                });
            }

        }

        if (page != null) {
            page.loading();
        }

    }

    private String emojiProportional;
    private float pfAdjust = 0.0f;

    private void buildStyles() {
        emojiProportional = SystemInfo.isMacOS ? "SansSerif" : "Noto Emoji";
        boolean isDark = UIManager.getBoolean("laf.dark");

        linkColor = UIManager.getColor("Component.linkColor");
        hoverColor = linkColor.brighter();

        Style pfStyle = doc.addStyle("```", null);
        StyleConstants.setFontFamily(pfStyle, monospacedFamily);
        StyleConstants.setFontSize(pfStyle, GeminiFrame.fontSize);
        StyleConstants.setBold(pfStyle, false);
        StyleConstants.setItalic(pfStyle, false);
        StyleConstants.setUnderline(pfStyle, false);

        // linespacing on non-windows platforms for pre-formatted text creates
        // horizontal lines in ascii block text (for example)
        if (!SystemInfo.isWindows) {
            Font pfFont = new Font(monospacedFamily, Font.PLAIN, GeminiFrame.fontSize);
            FontMetrics fm = getFontMetrics(pfFont);
            int ascent = fm.getAscent();
            int descent = fm.getDescent();
            int leading = fm.getLeading();
            pfAdjust = -((float) (descent + leading - (float) (descent / 1.95f)) / (float) ascent);
        }

        Color foreground = UIManager.getColor("TextField.foreground");

        Style h1Style = doc.addStyle("###", null);
        StyleConstants.setFontFamily(h1Style, GeminiFrame.proportionalFamily);
        StyleConstants.setFontSize(h1Style, GeminiFrame.fontSize + 3); // 18
        StyleConstants.setBold(h1Style, true);
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

        Style linkStyle = doc.addStyle("=>", h1Style);
        StyleConstants.setFontSize(linkStyle, GeminiFrame.fontSize);
        StyleConstants.setForeground(linkStyle, linkColor);
        StyleConstants.setBold(linkStyle, true);

        Style clickedStyle = doc.addStyle("visited", linkStyle);
        StyleConstants.setForeground(clickedStyle, isDark ? linkColor.darker() : linkColor.brighter());

        Style quoteStyle = doc.addStyle(">", h1Style);
        StyleConstants.setFontSize(quoteStyle, GeminiFrame.fontSize);
        StyleConstants.setItalic(quoteStyle, true);
        StyleConstants.setBold(quoteStyle, false);
        StyleConstants.setForeground(quoteStyle, foreground);

        Style textStyle = doc.addStyle("text", h1Style);
        StyleConstants.setFontSize(textStyle, GeminiFrame.fontSize);
        StyleConstants.setBold(textStyle, false);
        StyleConstants.setForeground(textStyle, foreground);

        Style listStyle = doc.addStyle("*", textStyle);

        hoverStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(hoverStyle, hoverColor);

        normalStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(normalStyle, linkColor);

        visitedStyle = new SimpleAttributeSet();
        StyleConstants.setForeground(visitedStyle, isDark ? linkColor.darker() : linkColor.brighter());

    }

    // Segoe UI is nice on Windows
    private void processLine(String line, boolean lastLine) {

        if (line.startsWith("```") && !plainTextMode) {
            preformattedMode = !preformattedMode;

            if (preformattedMode) {
                pfModeStart = doc.getLength();
                if (pfModeStart == 0) {
                    pAttributes = defPP;
                } else {
                    pAttributes = getParagraphAttributes();
                }

            } else {
                // executed at end block
                SimpleAttributeSet indentAttributes = new SimpleAttributeSet();

                StyleConstants.setLeftIndent(indentAttributes, 50); // Set left indentation in points
                StyleConstants.setRightIndent(indentAttributes, 50); // Optional: Adjust the right margin
                StyleConstants.setLineSpacing(indentAttributes, pfAdjust); // Optional: Set line spacing if needed
                Color pfColor = UIManager.getBoolean("laf.dark") ? AnsiColor.blend(linkColor, Color.WHITE, .1f) : AnsiColor.blend(linkColor, Color.BLACK, .1f);
                StyleConstants.setForeground(indentAttributes, pfColor);
                doc.setParagraphAttributes(pfModeStart, doc.getLength(), indentAttributes, true);

                doc.setParagraphAttributes(doc.getLength(), doc.getLength(), pAttributes, true);
            }
            addStyledText(lastLine, "\n", "```", null);
        } else if (preformattedMode) {
            if ((currentMode == BOOKMARK_MODE || currentMode == CERT_MODE) && line.startsWith("=>")) {
                line = "=> " + line.substring(line.indexOf(":") + 1);
            }
            addStyledText(lastLine, line, "```", null);
        } else if (line.startsWith("###")) {
            addStyledText(lastLine, line.substring(3).trim(), "###", null);
        } else if (line.startsWith("##")) {
            addStyledText(lastLine, line.substring(2).trim(), "##", null);
        } else if (line.startsWith("#")) {
            addStyledText(lastLine, line.substring(1).trim(), "#", null);
        } else if (line.startsWith("=>")) {
            String ll = line.substring(2).trim();

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

            ClickableRange cr = addStyledText(lastLine, label.isEmpty() ? url : label, linkStyle,
                    () -> {
                        String useB = DB.getPref("browser", null);
                        boolean useBrowser = useB == null ? true : useB.equals("true");
                        if (finalUrl.startsWith("https") && Alhena.browsingSupported && useBrowser) {
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
                            f.fetchURL(finalUrl);
                        }

                    });
            cr.url = url;
            cr.directive = directive[0];

        } else if (line.startsWith(">")) {
            addStyledText(lastLine, line.substring(1).trim(), ">", null);
        } else if (line.startsWith("* ")) {
            addStyledText(lastLine, "🔹 " + line.substring(1).trim(), "*", null);

        } else {
            addStyledText(lastLine, line, "text", null);
        }
    }

    private ClickableRange addStyledText(boolean lastLine, String text, String styleName, Runnable action) {

        Style style = doc.getStyle(styleName);

        ClickableRange cr = null;

        int start = doc.getLength();
        boolean monospace = styleName.equals("```");
        int heightOffset = monospace ? 4 : 0;
        if (!(hasAnsi && monospace) && EmojiManager.containsAnyEmoji(text)) {

            String fontFamily = StyleConstants.getFontFamily(style);
            int fontSize = StyleConstants.getFontSize(style);

            SimpleAttributeSet emojiStyle = new SimpleAttributeSet(style);

            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(text);

            IndexedEmoji emoji;
            // can't iterate by code point without preprocessing first to get png name
            for (int i = 0; i < text.length(); i++) {
                //char c = text.charAt(i);

                if ((emoji = isEmoji(emojis, i)) != null) {
                    int codePoint = text.codePointAt(i);
                    if (sheetImage != null) {

                        String key = getEmojiHex(emoji);

                        Point p = emojiSheetMap.get(key);
                        ImageIcon icon = null;
                        int imgSize = fontSize + 4;
                        if (p != null) {
                            icon = extractSprite(p.x, p.y, 64, imgSize, imgSize - heightOffset);
                        } else {
                            int dashIdx = key.indexOf('-');
                            if (dashIdx != -1) {
                                p = emojiSheetMap.get(key.substring(0, dashIdx));
                                if (p != null) {
                                    icon = extractSprite(p.x, p.y, 64, imgSize, imgSize - heightOffset);
                                }
                            }
                        }
                        if (icon == null) {

                            char[] chars = Character.toChars(codePoint);

                            //i += Character.charCount(codePoint) - 1;
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

                        char[] chars = Character.toChars(codePoint);

                        i++;

                        StyleConstants.setFontFamily(style, monospace ? monospacedFamily : emojiProportional);

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
        if (!lastLine) {
            try {
                doc.insertString(doc.getLength(), "\n", style);
            } catch (BadLocationException ex) {
            }
        }
        setCaretPosition(caretPosition); // prevent scrolling as content added

        return cr;
    }

    private static IndexedEmoji isEmoji(List<IndexedEmoji> emojiList, int idx) {
        for (IndexedEmoji emo : emojiList) {
            if (emo.getCharIndex() == idx) {
                return emo;
            }
        }
        return null;
    }

    private static String getEmojiHex(IndexedEmoji emo) {

        String code = emo.getEmoji().getHtmlHexadecimalCode().replace("&#x", "").replace(";", "-");
        //String code = emo.getEmoji().getUnicodeText().substring(2).replace("\\u", "-");
        //System.out.println(emo.getEmoji().getHtmlHexadecimalCode() + " " + emo.getEmoji().getUnicodeText());
        return code.substring(0, code.length() - 1);

    }

    private void insertString(int length, String txt, Style style) {
        try {
            if (hasAnsi && preformattedMode) {
                handleAnsi(txt, style);
            } else {
                doc.insertString(length, txt, style);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private static class ClickableRange {

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

    private Rectangle getCharacterBounds(JTextPane textPane, int start, int end) {
        try {
            Rectangle startRect = textPane.modelToView2D(start).getBounds();
            Rectangle endRect = textPane.modelToView2D(end).getBounds();

            if (startRect.y == endRect.y) { // Same line
                return new Rectangle(startRect.x, startRect.y, endRect.x - startRect.x, startRect.height);
            } else {
                // handle multi-line text
                Rectangle combinedRect = new Rectangle(startRect);

                int currentPos = start;
                while (currentPos < end) {
                    Rectangle currentRect = textPane.modelToView2D(currentPos).getBounds();

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

    private void checkScroll(MouseEvent e, JScrollPane scrollPane) {
        Point point = e.getPoint();
        Rectangle viewRect = scrollPane.getViewport().getViewRect();

        if (point.y <= viewRect.y + EDGE_MARGIN) {
            startScrolling(scrollPane, -1); // Scroll up
        } else if (point.y >= viewRect.y + viewRect.height - EDGE_MARGIN) {
            startScrolling(scrollPane, 1); // Scroll down
        } else {
            stopScrolling();
        }
    }

    private void startScrolling(JScrollPane scrollPane, int direction) {
        if (pressTimer != null && pressTimer.isRunning() && scrollDirection == direction) {
            return;
        }
        stopScrolling(); // Ensure only one timer is running

        scrollDirection = direction;
        holdTime = 0; // Reset hold time

        pressTimer = new Timer(INITIAL_DELAY, e -> {
            holdTime++; // Increment hold time

            // Dynamically adjust speed based on hold time
            int scrollSpeed = Math.min(INITIAL_SCROLL_SPEED + holdTime, MAX_SCROLL_SPEED);
            int newDelay = Math.max(INITIAL_DELAY - (holdTime * 5), MIN_DELAY); // Reduce delay over time

            // Apply new speed and delay
            JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
            int newValue = verticalBar.getValue() + (scrollSpeed * direction);
            verticalBar.setValue(newValue);
            // Reset caret position to avoid selection
            setCaretPosition(getCaretPosition());
            pressTimer.setDelay(newDelay); // Update timer delay
        });
        pressTimer.start();
    }

    private void stopScrolling() {
        if (pressTimer != null) {
            pressTimer.stop();
            pressTimer = null;
        }
        scrollDirection = 0;
        holdTime = 0; // Reset hold time
    }

    private static ImageIcon extractSprite(int sheetX, int sheetY, int sheetSize, int width, int height) {

        int x = (sheetX * (sheetSize + 2)) + 1;
        int y = (sheetY * (sheetSize + 2)) + 1;

        BufferedImage bi = sheetImage.getSubimage(x, y, sheetSize, sheetSize);
        Image scaledImg = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);

        return new ImageIcon(scaledImg);
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
                    //System.out.println(scrollAdjust);
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
        if (scrollPane == null) {
            scrollPane = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, GeminiTextPane.this);
        }

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

}
