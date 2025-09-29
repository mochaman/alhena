package brad.grier.alhena;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.Timer;
import javax.swing.UIManager;

public class GeminiGlassPane extends JComponent implements ActionListener {

    private boolean mIsRunning;
    private boolean mIsFadingOut;
    private Timer mTimer;
    private int mAngle;
    private int mFadeCount;
    private final int mFadeLimit = 15;
    private final GeminiFrame gf;

    public GeminiGlassPane(GeminiFrame gf) {
        super();
        //setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        setOpaque(false);
        this.gf = gf;
    }

    @Override
    public void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        if (!mIsRunning) {
            return;
        }

        Graphics2D g2 = (Graphics2D) g.create();

        float fade = (float) mFadeCount / (float) mFadeLimit;

        // Paint the wait indicator.
        int s = Math.min(w, h) / 30; // was 5
        int cx = w / 2;
        int cy = h / 2;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(
                new BasicStroke(s / 4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setPaint(spinnerColor);
        g2.rotate(Math.PI * mAngle / 180, cx, cy);
        for (int i = 0; i < 12; i++) {
            float scale = (11.0f - (float) i) / 11.0f;
            g2.drawLine(cx + s, cy, cx + s * 2, cy);
            g2.rotate(-Math.PI / 6, cx, cy);
            g2.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, scale * fade));
        }

        g2.dispose();
    }
    private Color spinnerColor;

    public void start() {
        //spinnerColor = UIManager.getBoolean("laf.dark") ? Color.WHITE : Color.BLACK;
        if (gf.visiblePage().textPane.pageStyle != null) {
            spinnerColor = gf.visiblePage().textPane.pageStyle.getSpinnerColor();
        } else {
            spinnerColor = UIManager.getColor("Component.linkColor");
        }
        if (mIsRunning) {
            if (mIsFadingOut) {
                mIsFadingOut = false;
            }
            return;
        }

        // Run a thread for animation.
        mIsRunning = true;
        mIsFadingOut = false;
        mFadeCount = 0;
        int fps = 30;
        int tick = 1000 / fps;
        mTimer = new Timer(tick, this);
        mTimer.start();
    }

    public void stop() {
        mIsFadingOut = true;
    }

    public boolean isRunning() { // call only on edt
        if (mTimer != null) {
            return mTimer.isRunning();
        }
        return false;
    }

    @Override
    public void setVisible(boolean b) {
        if (!b && isRunning()) {
            mFadeCount = 0;
            mIsRunning = false;
            mTimer.stop();
        }
        super.setVisible(b);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (mIsRunning) {
            //firePropertyChange("tick", 0, 1);
            repaint();
            mAngle += 3;
            if (mAngle >= 360) {
                mAngle = 0;
            }
            if (mIsFadingOut) {
                if (--mFadeCount == 0) {
                    mIsRunning = false;
                    mTimer.stop();
                }
            } else if (mFadeCount < mFadeLimit) {
                mFadeCount++;
            }
        }
    }

}
