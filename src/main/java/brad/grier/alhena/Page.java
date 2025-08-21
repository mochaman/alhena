package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.File;
import java.security.cert.X509Certificate;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayer;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.LayerUI;

import com.formdev.flatlaf.util.SystemInfo;

import brad.grier.alhena.Alhena.FavIconInfo;
import net.fellbaum.jemoji.EmojiManager;

/**
 * A class that encapsulates the JTextPane and JScrollPane used in the main view
 * and tabs
 *
 * @author Brad Grier
 */
public class Page extends JPanel {

    public static final Page ROOT_PAGE = new Page();
    public GeminiTextPane textPane;
    private Page rootPage;
    private int themeId;
    private GeminiFrame frame;
    private int arrayIndex = 0; // only tracked in root pages
    private Runnable onLoading;
    private JScrollPane scrollPane;
    private Runnable onDone;
    private File dataFile; // shoehorn in titan upload data for sync
    private long start;
    private long elapsed;
    public int redirectCount;
    private boolean busy = true;
    private boolean isSpartan;
    private boolean isNex;
    private JLabel overlayLabel;
    public static final int ICON_SIZE = 39;
    private JLayer<JComponent> layer;

    private Page() {

    }

    public Page(Page rootPage, GeminiFrame frame, String url, int themeId) {
        start = System.currentTimeMillis();
        // sometimes we know enough to make a page root at construction, sometimes not
        this.rootPage = rootPage;
        this.frame = frame;
        this.themeId = themeId;
        if (rootPage == ROOT_PAGE) {
            arrayIndex = 0;
        }
        setLayout(new BorderLayout());
        textPane = new GeminiTextPane(frame, this, url);
        init();

        if (Alhena.scrollSpeed != null) {
            int scrollSpeed = Integer.parseInt(Alhena.scrollSpeed);

            scrollPane.getVerticalScrollBar().setUnitIncrement(scrollSpeed);
        }
    }

    private void init() {
        scrollPane = new JScrollPane(textPane);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

        overlayLabel = new JLabel("");
        updateFavIconFont();

        overlayLabel.setBounds(50, 10, 50, 50);

        layer = new JLayer<>(scrollPane, new LayerUI<>() {

            @Override
            public void paint(Graphics g, JComponent c) {
                super.paint(g, c);
                if (Alhena.favIcon && GeminiTextPane.indent > ICON_SIZE + 5) {
                    int x = GeminiTextPane.indent / 2 - (ICON_SIZE / 2);
                    int y = scrollPane.getViewport().getExtentSize().height / 2 - ICON_SIZE;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.translate(x, y);
                    overlayLabel.paint(g2);
                    g2.dispose();
                }
            }
        });
        add(layer, BorderLayout.CENTER);
    }

    public final void updateFavIconFont() {
        if (favIconLocked) {
            return;
        }
        String fontFamily = "Noto Emoji";
        if (SystemInfo.isMacOS) {
            if (!Alhena.macUseNoto) {
                fontFamily = "SansSerif";
            }
        }
        overlayLabel.setFont(new Font(fontFamily, Font.PLAIN, ICON_SIZE));
    }

    private String favIconKey;
    private boolean favIconLocked;

    public void setFavIcon(String favIconKey, FavIconInfo favIconInfo) {
        this.favIconKey = favIconKey;
        if (favIconInfo.icon() instanceof ImageIcon imageIcon) {
            overlayLabel.setIcon(imageIcon);
            overlayLabel.setText(null);
        } else {
            if (((String) favIconInfo.icon()).length() < 5) {

                overlayLabel.setForeground(UIManager.getColor("TextField.foreground"));
                String s = (String) favIconInfo.icon();

                boolean isPrintable = overlayLabel.getFont().canDisplay(s.charAt(0));
                if (!EmojiManager.isEmoji(s) && !isPrintable) {
                    favIconLocked = true;
                    overlayLabel.setFont(new Font("SanSerif", Font.PLAIN, ICON_SIZE));
                }

                overlayLabel.setText(s);

                overlayLabel.setIcon(null);
            }
        }
    }

    public String getFavIconKey() {
        return favIconKey;
    }

    private String editedText;

    public void setEditedText(String editedText) {
        this.editedText = editedText;
    }

    public String getEditedText() {
        return editedText;
    }

    public void setSpartan(boolean isSpartan) {
        this.isSpartan = isSpartan;
    }

    public boolean isSpartan() {
        return isSpartan;
    }

    public void setNex(boolean isNex) {
        this.isNex = isNex;
    }

    public boolean isNex() {
        return isNex;
    }

    public void setStart() {
        start = System.currentTimeMillis();
    }

    public void ignoreStart() {
        start = 0;
    }

    public boolean busy() {
        return busy;
    }

    public void setBusy(boolean b) {

        if (isShowing()) {
            if (!b && frame.getGlassPane().isShowing()) {
                frame.showGlassPane(false);
            } else if (b && !frame.getGlassPane().isShowing()) {
                frame.showGlassPane(true);

            }
        }
        this.busy = b;
    }

    private boolean titanEdited;

    public void setTitanEdited(boolean te) {
        titanEdited = te;
    }

    public boolean getTitanEdited() {
        return titanEdited;
    }

    private String titanToken;

    public void setTitanToken(String token) {
        titanToken = token;
    }

    public String getTitanToken() {
        return titanToken;
    }

    public void setDataFile(File file) {
        dataFile = file;
    }

    public File getDataFile() {

        return dataFile;
    }

    public void setScrollIncrement(int inc) {
        scrollPane.getVerticalScrollBar().setUnitIncrement(inc);
    }

    public void resetScrollIncrement() {
        // amazing that you can't just save off the initial scroll increment and reset it
        invalidate();
        remove(layer);
        String overlayTxt = overlayLabel.getText();
        Icon overlayIcon = overlayLabel.getIcon();

        init();
        if (overlayTxt == null) {
            overlayLabel.setIcon(overlayIcon);
        }else{
            overlayLabel.setText(overlayTxt);
        }
        revalidate();
        repaint();
    }

    public void runWhenLoading(Runnable r) {
        onLoading = r;
    }

    public void loading() {
        if (onLoading != null) {
            onLoading.run();
            onLoading = null;
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        JTabbedPane tabbedPane = (JTabbedPane) SwingUtilities.getAncestorOfClass(JTabbedPane.class, this);
        if (tabbedPane != null) {
            if (visible) {
                if (!busy && frame.getGlassPane().isShowing()) {
                    frame.showGlassPane(false);
                } else if (busy && !frame.getGlassPane().isShowing()) {
                    frame.showGlassPane(true);
                }
            }
        }

    }

    @Override
    public void addNotify() {
        super.addNotify();
        textPane.resetLastClicked();
        EventQueue.invokeLater(() -> {
            if (!busy && frame.getGlassPane().isShowing()) {
                frame.showGlassPane(false);
            } else if (busy && !frame.getGlassPane().isShowing()) {
                frame.showGlassPane(true);
            }
        });

    }

    public void runWhenDone(Runnable r) {
        onDone = r;
    }

    public void doneLoading() {
        if (start != 0) {
            elapsed = System.currentTimeMillis() - start;

            if (protocol != null) {
                frame.setTmpStatus(elapsed + " ms " + protocol + " " + cipherSuite);
            } else {
                frame.setTmpStatus(elapsed + " ms");
            }
        }

        if (onDone != null) {
            onDone.run();
            onDone = null;
        }
    }

    public boolean isRoot() {
        return rootPage == ROOT_PAGE;
    }

    public void setRootPage(Page page) {
        if (isRoot() && page != ROOT_PAGE) {
            // already a root page - once a root, always a root
            // a reminder for future me
            throw new IllegalStateException();
        }
        this.rootPage = page;

    }

    public int getArrayIndex() {
        if (isRoot()) {
            return arrayIndex;
        } else {
            return rootPage.getArrayIndex();
        }
    }

    public int decAndGetArrayIndex() {
        if (isRoot()) {
            // this is a root page, use it's index
            arrayIndex--;
            return arrayIndex;
        } else {
            return rootPage.decAndGetArrayIndex();
        }
    }

    public int incAndGetArrayIndex() {
        if (isRoot()) {
            // this is a root page, use it's index
            arrayIndex++;
            return arrayIndex;
        } else {
            return rootPage.incAndGetArrayIndex();
        }
    }

    public GeminiFrame frame() {
        return frame;
    }

    public String getUrl() {
        return textPane.getDocURLString();
    }

    public Page getRootPage() {
        return rootPage;
    }

    public int getThemeId() {
        return themeId;
    }

    public void setThemeId(int themeId) {
        this.themeId = themeId;
    }

    public GeminiTextPane textPane() {
        return textPane;
    }

    // because Page is a key in hashmap
    @Override
    public int hashCode() {
        // Use identity hash code for JTextPane
        // because textPane is mutable
        return System.identityHashCode(textPane);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;

        }
        if (!(obj instanceof Page other)) {
            return false;
        }
        // use textPane for identity
        return this.textPane == other.textPane;
    }

    @Override
    public String toString() {
        return textPane.getDocURLString();
    }

    private X509Certificate cert;

    public void setCert(X509Certificate cert) {
        this.cert = cert;
    }

    public X509Certificate getCert() {
        return cert;
    }

    private String protocol, cipherSuite;

    public void setConnectInfo(String protocol, String cipherSuite) {
        this.protocol = protocol;
        this.cipherSuite = cipherSuite;
    }

    public String getProtocol() {
        return protocol;
    }

    public String getCipherSuite() {
        return cipherSuite;
    }

}
