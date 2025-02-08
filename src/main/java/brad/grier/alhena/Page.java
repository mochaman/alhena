package brad.grier.alhena;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A class that encapsulates the JTextPane and JScrollPane used in the main view and tabs
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
    private Runnable completed;

    private Page() {

    }

    public Page(Page rootPage, GeminiFrame frame, String url, int themeId) {

        // sometimes we know enough to make a page root at construction, sometimes not
        this.rootPage = rootPage;
        this.frame = frame;
        this.themeId = themeId;
        if(rootPage == ROOT_PAGE){
            arrayIndex = 0;
        }
        setLayout(new BorderLayout());
        textPane = new GeminiTextPane(frame, url);

        add(new JScrollPane(textPane), BorderLayout.CENTER);

    }

    public void runOnLoad(Runnable r){
        completed = r;
    }

    public void loading(){
        if(completed != null){
            completed.run();
        }
    }

    public boolean isRoot(){
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

    public int getArrayIndex(){
        if(isRoot()){
            return arrayIndex;
        }else{
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

}
