package brad.grier.alhena;

import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.HashMap;

import javax.swing.JSplitPane;

public class SplitPanel extends JSplitPane {

    private Page leftPage, rightPage, focusedPage;
    private final GeminiFrame frame;

    public SplitPanel(GeminiFrame frame, int orientation) {
        super(orientation);
        this.frame = frame;
    }

    public Page getFocusedPage() {
        return focusedPage;
    }

    public void setFocusedPage(Page p) {
        focusedPage = p;
    }

    private final HashMap<Page, FocusAdapter> faMap = new HashMap<>();

    private void addPageFocusListener(Page p) {
        FocusAdapter fa = new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                focusedPage = p;
                frame.splitViewChanged(p);
            }
        };
        p.textPane.addFocusListener(fa);
        faMap.put(p, fa);
    }

    public Page getLeftPage(){
        return leftPage;
    }

    public Page getRightPage(){
        return rightPage;
    }

    public void setLeftPage(Page p) {
        super.setLeftComponent(p);

        leftPage = p;
        focusedPage = leftPage;
        addPageFocusListener(leftPage);

    }


    public void setRightPage(Page p) {
        setRightComponent(p);
        rightPage = p;
        focusedPage = rightPage;
        addPageFocusListener(rightPage);

    }

    @Override
    public void remove(Component c) {
        super.remove(c);

        if (c instanceof Page p) {
            p.textPane.removeFocusListener(faMap.remove(p));
        }
    }

    public void replacePage(Page replace, Page with) {
        int loc = getDividerLocation();
        //remove(replace); // this happens anyway
        if (replace == leftPage) {
            setLeftPage(with);
        } else {
            setRightPage(with);
        }
        setDividerLocation(loc);

    }
}
