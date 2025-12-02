package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

// written by claude
public class BandVisualizer extends Visualizer {

    private static final int NUM_BANDS = 12;
    private static final int BAR_SPACING = 2;
    
    private final float[] bandLevels = new float[NUM_BANDS];
    private final float[] targetLevels = new float[NUM_BANDS];
    private final float[] peakLevels = new float[NUM_BANDS];
    private final int[] peakHoldTime = new int[NUM_BANDS];
    private boolean paused;

    public BandVisualizer() {
        setOpaque(false);
        //setBackground(Color.BLACK);
    }

    @Override
    public void updateSamples(short[] newSamples) {
        if (newSamples == null || newSamples.length == 0) {
            return;
        }

        // Divide samples into frequency bands
        int samplesPerBand = newSamples.length / NUM_BANDS;
        
        for (int band = 0; band < NUM_BANDS; band++) {
            float sum = 0;
            int startIdx = band * samplesPerBand;
            int endIdx = Math.min(startIdx + samplesPerBand, newSamples.length);
            
            // Calculate RMS (root mean square) for this band
            for (int i = startIdx; i < endIdx; i++) {
                float normalized = newSamples[i] / 32768.0f;
                sum += normalized * normalized;
            }
            
            float rms = (float) Math.sqrt(sum / samplesPerBand);
            targetLevels[band] = Math.min(1.0f, rms * 3.0f); // Amplify for visibility
            
            // Update peak hold
            if (targetLevels[band] > peakLevels[band]) {
                peakLevels[band] = targetLevels[band];
                peakHoldTime[band] = 20; // Hold peak for 20 frames
            } else if (peakHoldTime[band] > 0) {
                peakHoldTime[band]--;
            } else {
                peakLevels[band] *= 0.95f; // Slowly decay peak
            }
        }
        
        // Smooth animation: gradually move current levels toward target
        for (int i = 0; i < NUM_BANDS; i++) {
            float diff = targetLevels[i] - bandLevels[i];
            if (Math.abs(diff) < 0.01f) {
                bandLevels[i] = targetLevels[i];
            } else if (targetLevels[i] > bandLevels[i]) {
                bandLevels[i] += diff * 0.4f; // Rise quickly
            } else {
                bandLevels[i] += diff * 0.15f; // Fall slowly
            }
        }
        
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
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        if (paused) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        
        // Calculate bar width
        int totalSpacing = BAR_SPACING * (NUM_BANDS - 1);
        int barWidth = (w - totalSpacing) / NUM_BANDS;
        
        for (int i = 0; i < NUM_BANDS; i++) {
            int x = i * (barWidth + BAR_SPACING);
            int barHeight = (int) (bandLevels[i] * h);
            int y = h - barHeight;
            
            // Create gradient effect based on height
            float hue = 0.55f; // Green-cyan base (Spotify-ish)
            float brightness = 0.5f + (bandLevels[i] * 0.5f);
            Color barColor = Color.getHSBColor(hue, 0.8f, brightness);
            
            // Draw main bar with rounded top
            g2d.setColor(barColor);
            if (barHeight > 2) {
                // Bar body
                g2d.fillRect(x, y + 2, barWidth, barHeight - 2);
                // Rounded top
                g2d.fillRoundRect(x, y, barWidth, 4, 3, 3);
            } else {
                g2d.fillRoundRect(x, y, barWidth, barHeight, 3, 3);
            }
            
            // Draw peak indicator
            if (peakLevels[i] > 0.1f) {
                int peakY = h - (int) (peakLevels[i] * h);
                g2d.setColor(new Color(255, 255, 255, 255));
                g2d.fillRect(x, peakY, barWidth, 2);
            }
        }
    }
}