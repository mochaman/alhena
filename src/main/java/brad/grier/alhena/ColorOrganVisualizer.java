package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;

// written(?) by Claude. Prompt from memories of a 1970s light display
public class ColorOrganVisualizer extends Visualizer {

    private static final int NUM_DIAMONDS = 5;
    private short[] samples = new short[0];
    private boolean paused;
    
    // Frequency band energies (bass, low-mid, mid, high-mid, treble)
    private float[] bandEnergy = new float[NUM_DIAMONDS];
    private float[] smoothEnergy = new float[NUM_DIAMONDS];
    
    // Classic 70s color palette
    private static final Color[] DIAMOND_COLORS = {
        new Color(255, 0, 0),      // Red - Bass
        new Color(255, 165, 0),    // Orange - Low-mid
        new Color(255, 255, 0),    // Yellow - Mid
        new Color(0, 255, 0),      // Green - High-mid
        new Color(0, 150, 255)     // Blue - Treble
    };

    public ColorOrganVisualizer() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    @Override
    public void updateSamples(short[] newSamples) {
        samples = newSamples;
        
        if (samples.length == 0) {
            return;
        }
        
        // Divide samples into frequency bands
        int samplesPerBand = samples.length / NUM_DIAMONDS;
        
        for (int band = 0; band < NUM_DIAMONDS; band++) {
            int startIdx = band * samplesPerBand;
            int endIdx = Math.min(startIdx + samplesPerBand, samples.length);
            
            // Calculate RMS energy for this band
            float sum = 0;
            for (int i = startIdx; i < endIdx; i++) {
                float normalized = samples[i] / 32768.0f;
                sum += normalized * normalized;
            }
            
            bandEnergy[band] = (float) Math.sqrt(sum / samplesPerBand) * 5.0f; // Amplify!
            
            // Smooth the response (like analog circuitry would) - faster attack
            float diff = bandEnergy[band] - smoothEnergy[band];
            if (diff > 0) {
                smoothEnergy[band] += diff * 0.6f; // Quick rise
            } else {
                smoothEnergy[band] += diff * 0.2f; // Slower decay
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
    protected synchronized void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        
        if (paused) {
            return;
        }

        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();
        
        // Calculate diamond size and spacing
        int diamondSize = Math.min(w, h) / (NUM_DIAMONDS + 1);
        int spacing = (w - (NUM_DIAMONDS * diamondSize)) / (NUM_DIAMONDS + 1);
        
        // Draw each diamond
        for (int i = 0; i < NUM_DIAMONDS; i++) {
            int x = spacing + i * (diamondSize + spacing);
            int y = (h - diamondSize) / 2;
            
            drawDiamond(g, x, y, diamondSize, i);
        }
        
        // Draw textured overlay to simulate rough plastic
        drawTextureOverlay(g, w, h);
    }

    private void drawDiamond(Graphics2D g, int x, int y, int size, int bandIndex) {
        int cx = x + size / 2;
        int cy = y + size / 2;
        
        // Energy determines brightness
        float energy = smoothEnergy[bandIndex];
        float intensity = Math.min(1.0f, energy * 1.5f); // Better response range
        
        // Create the diamond polygon
        Polygon diamond = new Polygon();
        diamond.addPoint(cx, cy - size / 2);           // Top
        diamond.addPoint(cx + size / 2, cy);           // Right
        diamond.addPoint(cx, cy + size / 2);           // Bottom
        diamond.addPoint(cx - size / 2, cy);           // Left
        
        Color baseColor = DIAMOND_COLORS[bandIndex];
        
        // Draw glow effect (multiple layers for that 70s bulb diffusion)
        for (int layer = 3; layer > 0; layer--) {
            float layerAlpha = intensity * (layer / 3.0f) * 0.7f; // Brighter glow
            int layerSize = size + (layer * 10);
            
            int r = (int) (baseColor.getRed() * layerAlpha);
            int gr = (int) (baseColor.getGreen() * layerAlpha);
            int b = (int) (baseColor.getBlue() * layerAlpha);
            
            g.setColor(new Color(r, gr, b));
            
            Polygon glowDiamond = new Polygon();
            int glowOffset = layerSize / 2;
            glowDiamond.addPoint(cx, cy - glowOffset);
            glowDiamond.addPoint(cx + glowOffset, cy);
            glowDiamond.addPoint(cx, cy + glowOffset);
            glowDiamond.addPoint(cx - glowOffset, cy);
            
            g.fillPolygon(glowDiamond);
        }
        
        // Draw main diamond with full brightness
        int r = Math.min(255, (int) (baseColor.getRed() * intensity * 1.2f));
        int gr = Math.min(255, (int) (baseColor.getGreen() * intensity * 1.2f));
        int b = Math.min(255, (int) (baseColor.getBlue() * intensity * 1.2f));
        g.setColor(new Color(r, gr, b));
        g.fillPolygon(diamond);
        
        // Add subtle facets for that textured plastic look
        if (intensity > 0.1f) {
            g.setColor(new Color(255, 255, 255, (int) (80 * intensity)));
            Polygon highlight = new Polygon();
            highlight.addPoint(cx, cy - size / 2);
            highlight.addPoint(cx + size / 4, cy - size / 4);
            highlight.addPoint(cx, cy);
            highlight.addPoint(cx - size / 4, cy - size / 4);
            g.fillPolygon(highlight);
        }
        
        // Dark outline for definition
        g.setColor(new Color(40, 40, 40));
        g.drawPolygon(diamond);
    }

    private void drawTextureOverlay(Graphics2D g, int w, int h) {
        // Simulate the rough plastic texture with random noise
        AffineTransform oldTransform = g.getTransform();
        
        for (int i = 0; i < 200; i++) {
            int x = (int) (Math.random() * w);
            int y = (int) (Math.random() * h);
            int size = (int) (Math.random() * 3) + 1;
            
            g.setColor(new Color(255, 255, 255, (int) (Math.random() * 15)));
            g.fillOval(x, y, size, size);
        }
        
        g.setTransform(oldTransform);
    }
}