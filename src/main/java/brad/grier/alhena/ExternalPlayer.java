package brad.grier.alhena;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 *
 * @author brad
 */
public class ExternalPlayer extends JPanel implements MediaComponent {

    private Process process;
    private final GeminiFrame frame;
    private final PacketDotsIndicator packets;

    public ExternalPlayer(GeminiFrame frame) {
        this.frame = frame;
        setLayout(new FlowLayout());
        packets = new PacketDotsIndicator();
        add(packets);

        packets.start();
    }

    @Override
    public void start(String mrl) {
        //String cmd = "/usr/local/bin/ffplay -loglevel quiet %1";
        // String cmd = "/usr/local/bin/mpv --no-video %1"
        //String cmd = "/Applications/VLC.app/Contents/MacOS/VLC %1"
        String[] args = parseCommand(Alhena.playerCommand, mrl);
        frame.setTmpStatus("Opening: " + args[0]);
        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                    args
            );

            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            try {
                process = pb.start();
            
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            process.onExit().thenAccept(proc ->{
                packets.stop();
            });
            
        }).start(); // overkill - needed to get opening message to the status bar on the EDT right away
    }

    @Override
    public void dispose() {
        process.destroy();
        packets.stop();
    }

    @Override
    public void pause() {
        // no op
    }

    public static String[] parseCommand(String command, String url) {
        List<String> args = new ArrayList<>();

        StringBuilder current = new StringBuilder();
        boolean inDoubleQuotes = false;
        boolean inSingleQuotes = false;

        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);

            if (c == '"' && !inSingleQuotes) {
                inDoubleQuotes = !inDoubleQuotes;
                continue;
            }
            if (c == '\'' && !inDoubleQuotes) {
                inSingleQuotes = !inSingleQuotes;
                continue;
            }

            if (Character.isWhitespace(c) && !inDoubleQuotes && !inSingleQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }

        if (current.length() > 0) {
            args.add(current.toString());
        }

        // Substitute %1
        for (int i = 0; i < args.size(); i++) {
            args.set(i, args.get(i).replace("%1", url));
        }

        return args.toArray(new String[0]);
    }

    public class PacketDotsIndicator extends JComponent {

        private static final int FPS = 30;
        private static final int MAX_DOTS = 8;

        private final List<Dot> dots = new ArrayList<>();
        private final Random rnd = new Random();
        private final Timer timer;

        public PacketDotsIndicator() {
            setPreferredSize(new Dimension(100, 24));
            setOpaque(false);

            timer = new Timer(1000 / FPS, e -> tick());
        }

        public void start() {
            if (!timer.isRunning()) {
                timer.start();
            }
        }

        public void stop() {
            timer.stop();
            dots.clear();
            repaint();
        }

        private void tick() {
            if (dots.size() < MAX_DOTS && rnd.nextFloat() < 0.25f) {
                dots.add(new Dot());
            }

            Iterator<Dot> it = dots.iterator();
            while (it.hasNext()) {
                Dot d = it.next();
                d.x += d.speed;
                d.alpha -= 0.015f;

                if (d.x > getWidth() || d.alpha <= 0) {
                    it.remove();
                }
            }

            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);

                int h = getHeight();
                Color base = UIManager.getColor("Label.foreground");

                for (Dot d : dots) {
                    g2.setComposite(AlphaComposite.SrcOver.derive(d.alpha));
                    g2.setColor(base);
                    g2.fillOval((int) d.x, d.y, d.size, d.size);
                }
            } finally {
                g2.dispose();
            }
        }

        private class Dot {

            float x = 0;
            int y;
            int size;
            float speed;
            float alpha = 1f;

            Dot() {
                int h = getHeight();
                size = 3 + rnd.nextInt(3);
                y = (h - size) / 2 + rnd.nextInt(3) - 1;
                speed = 1.2f + rnd.nextFloat() * 1.8f;
            }
        }
    }

}
