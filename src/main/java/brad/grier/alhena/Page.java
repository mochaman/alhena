package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.io.File;
import java.security.cert.X509Certificate;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

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
        scrollPane = new JScrollPane(textPane);
        scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE);
        String ss = DB.getPref("scrollspeed", null);

        if (ss != null) {
            int scrollSpeed = Integer.parseInt(ss);

            scrollPane.getVerticalScrollBar().setUnitIncrement(scrollSpeed);
        }
        add(scrollPane, BorderLayout.CENTER);

    }
    
    private String editedText;
    public void setEditedText(String editedText){
        this.editedText = editedText;
    }

    public String getEditedText(){
        return editedText;
    }

    public void setSpartan(boolean isSpartan){
        this.isSpartan = isSpartan;
    }

    public boolean isSpartan(){
        return isSpartan;
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
    public void setTitanEdited(boolean te){
        titanEdited = te;
    }
    public boolean getTitanEdited(){
        return titanEdited;
    }

    private String titanToken;
    public void setTitanToken(String token){
        titanToken = token;
    }

    public String getTitanToken(){
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
            frame.setTmpStatus(elapsed + " ms");
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

}
