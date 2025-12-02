
package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class KaleidoscopePanel extends Visualizer {

    private short[] samples = new short[0];
    private final int mirrors = 8;
    private boolean paused;

    @Override
    public void updateSamples(short[] s) {
        samples = s;
        repaint();
    }

    @Override
    public void pause(){
        paused = true;
        repaint();
    }

    @Override
    public void resume(){
        paused = false;
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if(paused){
            return;
        }
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        float[] pts = new float[samples.length];
        for (int i = 0; i < samples.length; i++) {
            pts[i] = samples[i] / 32768f;
        }

        float angleStep = (float) (2 * Math.PI / mirrors);

        for (int m = 0; m < mirrors; m++) {
            double baseAngle = m * angleStep;

            g.setColor(Color.getHSBColor((float) m / mirrors, 1f, 1f));

            for (int i = 0; i < pts.length - 1; i++) {
                float r1 = pts[i] * 100;
                float r2 = pts[i + 1] * 100;

                double a1 = baseAngle + (i / (float) pts.length) * angleStep;
                double a2 = baseAngle + ((i + 1) / (float) pts.length) * angleStep;

                int x1 = cx + (int) (r1 * Math.cos(a1));
                int y1 = cy + (int) (r1 * Math.sin(a1));
                int x2 = cx + (int) (r2 * Math.cos(a2));
                int y2 = cy + (int) (r2 * Math.sin(a2));

                g.drawLine(x1, y1, x2, y2);
            }
        }
    }
}
