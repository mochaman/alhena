package brad.grier.alhena;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


// written by claude
public class OscilloscopePanel extends Visualizer {

    private short[] samples = new short[0];
    private boolean paused;
    private BufferedImage trailBuffer;
    private int scanLinePos = 0;
    
    // Classic phosphor green
    private static final Color PHOSPHOR_GREEN = new Color(57, 255, 20);
    private static final Color GRID_COLOR = new Color(57, 255, 20, 30);
    private static final Color SCAN_LINE_COLOR = new Color(57, 255, 20, 80);

    public OscilloscopePanel() {
        setOpaque(true);
        setBackground(Color.BLACK);
    }

    @Override
    public void updateSamples(short[] newSamples) {
        samples = newSamples;
        scanLinePos = (scanLinePos + 2) % getHeight();
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
        
        int w = getWidth();
        int h = getHeight();
        
        // Initialize trail buffer if needed
        if (trailBuffer == null || trailBuffer.getWidth() != w || trailBuffer.getHeight() != h) {
            trailBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        }
        
        if (paused) {
            return;
        }
        
        if (samples.length == 0) {
            return;
        }

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw persistent trail buffer with fade effect
        Graphics2D bufferG2d = trailBuffer.createGraphics();
        bufferG2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
        bufferG2d.setColor(Color.BLACK);
        bufferG2d.fillRect(0, 0, w, h);
        bufferG2d.dispose();

        // Draw grid
        drawGrid(g2d, w, h);

        // Draw the waveform onto trail buffer
        Graphics2D trailG2d = trailBuffer.createGraphics();
        trailG2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        drawWaveform(trailG2d, w, h);
        trailG2d.dispose();

        // Composite trail buffer onto main canvas
        g2d.drawImage(trailBuffer, 0, 0, null);

        // Draw scan line effect
        g2d.setColor(SCAN_LINE_COLOR);
        g2d.fillRect(0, scanLinePos, w, 2);

        // Draw glowing border/bezel
        g2d.setColor(new Color(57, 255, 20, 40));
        g2d.drawRect(2, 2, w - 5, h - 5);
        g2d.drawRect(1, 1, w - 3, h - 3);
    }

    private void drawGrid(Graphics2D g2d, int w, int h) {
        g2d.setColor(GRID_COLOR);
        
        // Vertical lines
        int gridSpacing = 20;
        for (int x = 0; x < w; x += gridSpacing) {
            g2d.drawLine(x, 0, x, h);
        }
        
        // Horizontal lines
        for (int y = 0; y < h; y += gridSpacing) {
            g2d.drawLine(0, y, w, y);
        }
        
        // Center lines (brighter)
        g2d.setColor(new Color(57, 255, 20, 60));
        g2d.drawLine(w / 2, 0, w / 2, h);
        g2d.drawLine(0, h / 2, w, h / 2);
    }

    private void drawWaveform(Graphics2D g2d, int w, int h) {
        int mid = h / 2;
        int step = Math.max(1, samples.length / w);
        
        // Draw the main waveform with glow
        for (int pass = 0; pass < 3; pass++) {
            float alpha = pass == 0 ? 0.3f : (pass == 1 ? 0.6f : 1.0f);
            int thickness = pass == 0 ? 5 : (pass == 1 ? 3 : 1);
            
            g2d.setColor(new Color(
                PHOSPHOR_GREEN.getRed(),
                PHOSPHOR_GREEN.getGreen(),
                PHOSPHOR_GREEN.getBlue(),
                (int) (255 * alpha)
            ));
            g2d.setStroke(new java.awt.BasicStroke(thickness, 
                java.awt.BasicStroke.CAP_ROUND, 
                java.awt.BasicStroke.JOIN_ROUND));
            
            int prevX = 0;
            int prevY = mid;
            
            for (int i = 0; i < samples.length; i += step) {
                int x = (i / step);
                
                // Normalize sample to screen coordinates
                float normalized = samples[i] / 32768.0f;
                int y = mid - (int) (normalized * (h / 2) * 0.8f);
                
                if (i > 0) {
                    g2d.drawLine(prevX, prevY, x, y);
                }
                
                prevX = x;
                prevY = y;
                
                if (x >= w) {
                    break;
                }
            }
        }
    }
}