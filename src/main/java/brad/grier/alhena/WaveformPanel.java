package brad.grier.alhena;

import java.awt.Graphics;

public class WaveformPanel extends Visualizer {

    private short[] samples = new short[0];
    private boolean paused;

    public WaveformPanel() {
        setOpaque(false);
    }

    @Override
    public void updateSamples(short[] newSamples) {

        samples = newSamples;
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
    protected synchronized void paintComponent(Graphics g) {
        super.paintComponent(g);
        if(paused){
            return;
        }
        if (samples.length == 0) {
            return;
        }

        int w = getWidth();
        int h = getHeight();
        int mid = h / 2;

        int step = Math.max(1, samples.length / w);
        int x = 0;

        for (int i = 0; i < samples.length; i += step) {

            // Normalize (-32768..32767) to panel height
            int y = (int) ((samples[i] / 32768.0) * (h / 2));

            g.drawLine(x, mid - y, x, mid + y);

            if (++x >= w) {
                break;
            }
        }
    }
}
