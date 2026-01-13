package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;

import javax.swing.JTextPane;
import javax.swing.text.View;

public class ViewBasedTextPanePrinter implements Printable {

    private final View rootView;
    private final int pageWidth;
    public static final int MONOSPACED_SIZE = 12;
    private final GeminiTextPane srcTextPane;

    public ViewBasedTextPanePrinter(GeminiTextPane source, PageFormat pageFormat) {

        // TODO: Not sure this is needed anymore since using offscreen GeminiTextPane
        JTextPane dummyPane = new JTextPane();
        dummyPane.setEditorKit(source.getEditorKit());
        dummyPane.setDocument(source.getDocument());
        dummyPane.setSize((int) pageFormat.getImageableWidth(), Integer.MAX_VALUE);
        dummyPane.setEditable(false);
        dummyPane.setCaretPosition(0);
        srcTextPane = source;
        dummyPane.validate();

        rootView = dummyPane.getUI().getRootView(dummyPane);
        pageWidth = (int) pageFormat.getImageableWidth();
        rootView.setSize(pageWidth, Integer.MAX_VALUE);
    }

    @Override
    public int print(Graphics g, PageFormat pageFormat, int pageIndex) throws PrinterException {
        Graphics2D g2 = (Graphics2D) g;
        g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
        if (!Alhena.forceWhite) {

            // Color c = g2.getColor();
            // Page.paintGradient(g, srcTextPane, 0, 0, g.getClipBounds().width, g.getClipBounds().height);
            // g2.setColor(c);

            // still can have issues with gradient printing on linux cups
            Color c = g2.getColor();
            Rectangle clip = g2.getClipBounds();

            // save state
            Shape oldClip = g2.getClip();

            g2.setClip(null);

            int x = clip != null ? clip.x : (int) pageFormat.getImageableX();
            int y = clip != null ? clip.y : (int) pageFormat.getImageableY();

            int w = (int) Math.ceil(pageFormat.getImageableWidth());
            int h = (int) Math.ceil(pageFormat.getImageableHeight());

            Page.paintGradient(g2, srcTextPane, x, y, w, h);

            g2.setClip(oldClip);
            g2.setColor(c);

        }

        int pageHeight = (int) pageFormat.getImageableHeight();
        int pageCounter = 0;
        int yOffset = 0;
        int pageStartY = 0;

        View contentView = rootView.getView(0);
        int n = contentView.getViewCount();

        for (int i = 0; i < n; i++) {
            View child = contentView.getView(i);
            Shape alloc = getAllocation(contentView, i, yOffset);

            if (alloc == null) {
                yOffset += child.getPreferredSpan(View.Y_AXIS);
                continue;
            }

            Rectangle bounds = alloc.getBounds();
            int viewBottom = yOffset + bounds.height;

            // check if this view would extend beyond the current page
            if (viewBottom > pageStartY + pageHeight) {
                // we need a page break before this view
                if (pageCounter == pageIndex) {
                    // we've finished the requested page
                    return PAGE_EXISTS;
                }

                // move to next page
                pageCounter++;
                pageStartY = yOffset; // New page starts at this view's position
            }

            // if this is the requested page, paint the view
            if (pageCounter == pageIndex) {
                // translate so that pageStartY becomes y=0 on the printed page
                g2.translate(0, -pageStartY);

                // set clipping to prevent content from bleeding beyond page boundaries
                Shape oldClip = g2.getClip();
                g2.clipRect(0, pageStartY, pageWidth, pageHeight);

                child.paint(g2, alloc);

                // restore clip and translation
                g2.setClip(oldClip);
                g2.translate(0, pageStartY);
            }

            yOffset += bounds.height;
        }

        // check if the requested page exists
        if (pageCounter >= pageIndex) {
            return PAGE_EXISTS;
        }

        return NO_SUCH_PAGE;
    }

    private Shape getAllocation(View parent, int index, int yOffset) {
        View v = parent.getView(index);
        float height = v.getPreferredSpan(View.Y_AXIS);
        return new Rectangle(0, yOffset, pageWidth, (int) height);
    }

}
