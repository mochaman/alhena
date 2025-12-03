package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class KaleidoscopePanel extends Visualizer {

    private short[] samples = new short[0];
    private final int mirrors = 8;
    private boolean paused;
    private final float[] normalizedSamples = new float[8192]; // or expected max size

    @Override
    public void updateSamples(short[] s) {
        samples = s;
        repaint();
    }

    @Override
    public void pause() {
        paused = true;
        repaint();
    }

    @Override
    public void resume() {
        paused = false;
    }

    @Override
    // optimized by claude
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        if (paused) {
            return;
        }

        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        int cx = w / 2;
        int cy = h / 2;

        // Optimization 1: Pre-allocate arrays and reuse them (make these class fields)
        // float[] normalizedSamples
        // int[] xPoints, yPoints
        int numSamples = samples.length;

        // Optimization 2: Pre-calculate normalized samples once
        for (int i = 0; i < numSamples; i++) {
            normalizedSamples[i] = samples[i] * (100f / 32768f); // combine operations
        }

        float angleStep = (float) (2 * Math.PI / mirrors);
        float sampleAngleStep = angleStep / numSamples;

        // Optimization 3: Pre-calculate sin/cos for sample positions (LUT approach)
        // Could cache these in a lookup table if mirrors value doesn't change often
        for (int m = 0; m < mirrors; m++) {
            float baseAngle = m * angleStep;

            g.setColor(Color.getHSBColor((float) m / mirrors, 1f, 1f));

            // Optimization 4: Build polygon/path instead of individual lines
            // This reduces draw calls significantly
            int[] xPts = new int[numSamples];
            int[] yPts = new int[numSamples];

            for (int i = 0; i < numSamples; i++) {
                float radius = normalizedSamples[i];
                float angle = baseAngle + (i * sampleAngleStep);

                // Optimization 5: Use direct math instead of intermediate variables
                xPts[i] = cx + (int) (radius * Math.cos(angle));
                yPts[i] = cy + (int) (radius * Math.sin(angle));
            }

            // Draw as polyline - single draw call instead of numSamples draw calls
            g.drawPolyline(xPts, yPts, numSamples);
        }
    }

}
