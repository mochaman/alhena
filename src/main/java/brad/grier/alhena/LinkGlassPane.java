package brad.grier.alhena;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import brad.grier.alhena.GeminiTextPane.ClickableRange;

/**
 * GlassPane for displaying link shortcuts
 * 
 * @author Brad Grier
 */
public class LinkGlassPane extends JComponent {

    private final GeminiTextPane textPane;
    private final List<ClickableRange> visibleLinks;
    private final Font f;
    private FontMetrics metrics;

    public LinkGlassPane(GeminiTextPane textPane) {
        this.textPane = textPane;
        f = new Font(textPane.pageStyle.getLinkFontFamily(), textPane.pageStyle.getLinkStyle(), textPane.pageStyle.getLinkSize());
        visibleLinks = textPane.getVisibleLinks();

    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (metrics == null) {
            metrics = g.getFontMetrics(f);
        }
        g2.setFont(f);
        int textWidth = metrics.stringWidth("W");
        int max = Math.min(visibleLinks.size(), 35);
        for (int i = 0; i < max; i++) {

            ClickableRange range = visibleLinks.get(i);
            try {
                Rectangle2D startRect = textPane.modelToView2D(range.start);
                Rectangle2D endRect = textPane.modelToView2D(range.end);
                Rectangle2D linkBounds = startRect.createUnion(endRect);

                Point basePoint = new Point((int) linkBounds.getX(), (int) (linkBounds.getY()));
                Point pt = SwingUtilities.convertPoint(textPane, basePoint, this);

                String label;
                if (i < 9) {
                    label = Integer.toString(i + 1); // 1–9
                } else {
                    label = String.valueOf((char) ('A' + (i - 9))); // A, B, C...
                }

                g2.setColor(textPane.pageStyle.getLinkColor());

                g2.drawString(label, (int) pt.x - (textWidth) - 10, (int) pt.y + metrics.getAscent() + 1);

            } catch (BadLocationException e) {
                // ignore
            }
        }
    }

}
