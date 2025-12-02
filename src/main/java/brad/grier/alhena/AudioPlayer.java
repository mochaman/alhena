package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import com.sun.jna.Pointer;

import uk.co.caprica.vlcj.player.base.AudioApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.base.callback.AudioCallbackAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class AudioPlayer extends JPanel implements MediaComponent {

    private final AudioPlayerComponent mediaPlayerComponent;
    private boolean paused;
    private volatile boolean ended;
    private JButton pauseButton;
    private boolean stopped;
    private StreamSession session;
    private SourceDataLine line;
    private Visualizer visualizerPanel;

    public AudioPlayer(StreamSession session) {
        this.session = session;
        setLayout(new BorderLayout());
        setOpaque(false);

        Font buttonFont = new Font("Noto Emoji Regular", Font.PLAIN, 18);
        AtomicBoolean suppressEvents = new AtomicBoolean(false);
        final JSlider slider = new JSlider(0, 100, 0);
        mediaPlayerComponent = new AudioPlayerComponent();

        if (session == null) {

            add(slider, BorderLayout.NORTH);

            slider.addChangeListener(cl -> {
                if (suppressEvents.get()) {
                    return;
                }
                if (!slider.getValueIsAdjusting()) { // slider released
                    pauseExempt = true;
                    Alhena.pauseMedia();
                    pauseExempt = false;
                    float value = (float) slider.getValue() / 100;
                    stopped = false;
                    paused = false;
                    pauseButton.setText("⏸");

                    mediaPlayerComponent.mediaPlayer().submit(() -> {

                        if (ended) {
                            ended = false;
                            mediaPlayerComponent.mediaPlayer().media().play(mrl);
                            mediaPlayerComponent.mediaPlayer().controls().setPosition(value);
                        } else {
                            mediaPlayerComponent.mediaPlayer().controls().setPosition(value);
                            mediaPlayerComponent.mediaPlayer().controls().pause();
                        }
                    });
                } else {

                    if (!stopped) {
                        stopped = true;
                        if (!paused) {
                            paused = true;

                            pauseButton.setText("▶");
                            mediaPlayerComponent.mediaPlayer().submit(() -> {

                                mediaPlayerComponent.mediaPlayer().controls().pause();
                            });
                        }
                    }

                }
            });
        }
        JLabel timeLabel = new JLabel(" ", SwingConstants.CENTER);
        timeLabel.setPreferredSize(new Dimension(50, timeLabel.getPreferredSize().height));
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

            @Override
            public void finished(MediaPlayer mediaPlayer) {

                paused = false;
                stopped = false;
                ended = true;
                EventQueue.invokeLater(() -> {
                    pauseButton.setText("▶");
                    if (session != null) {
                        pauseButton.setEnabled(false);
                    }
                    suppressEvents.set(true);
                    slider.setValue(0);
                    suppressEvents.set(false);
                });
            }

            @Override
            public void timeChanged(MediaPlayer mediaPlayer, long newTime) {
                EventQueue.invokeLater(() -> {
                    timeLabel.setText(Util.formatElapsed(newTime));
                });
            }

            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {

                super.positionChanged(mediaPlayer, newPosition);
                EventQueue.invokeLater(() -> {
                    suppressEvents.set(true);
                    if (session == null) {
                        slider.setValue((int) (newPosition * 100));
                    }
                    suppressEvents.set(false);
                });
                //System.out.println(newPosition);
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                EventQueue.invokeLater(() -> {
                    Util.infoDialog(AudioPlayer.this, I18n.t("playErrorDialog"), I18n.t("playErrorDialogMsg"), JOptionPane.ERROR_MESSAGE);
                });
            }
        });

        JPanel controlsPane = new JPanel();
        //controlsPane.setOpaque(false);
        pauseButton = new JButton("⏸");
        pauseButton.setFont(buttonFont);
        controlsPane.add(pauseButton);
        controlsPane.add(timeLabel);

        add(controlsPane, BorderLayout.CENTER);
        //Visualizer visualizerPanel = null;
        String vis = Alhena.audioVisualizer;
        switch (vis) {
            case "Waveform" ->
                visualizerPanel = new WaveformPanel();
            case "Oscilloscope" ->
                visualizerPanel = new OscilloscopePanel();
            case "Kaleidoscope" ->
                visualizerPanel = new KaleidoscopePanel();
            case "Bands" ->
                visualizerPanel = new BandVisualizer();
            default -> {
            }
        }

        if (visualizerPanel != null) {
            Visualizer finalV = visualizerPanel;
            try {
                AudioFormat format = new AudioFormat(
                        44100, // sample rate
                        16, // sample size in bits
                        2, // channels (stereo)
                        true, // signed
                        false // little-endian
                );

                line = AudioSystem.getSourceDataLine(format);
                line.open(format);
                line.start();
                AudioApi audio = mediaPlayerComponent.mediaPlayer().audio();
                audio.callback("S16N", 44100, 2,
                        new AudioCallbackAdapter() {

                    @Override
                    public void play(MediaPlayer mp, Pointer buffer, int sampleCount, long pts) {
                        // For stereo, total samples = sampleCount * 2
                        int channels = 2;
                        int byteCount = sampleCount * channels * 2; // 2 bytes per sample
                        ByteBuffer bb = buffer.getByteBuffer(0, byteCount).order(ByteOrder.LITTLE_ENDIAN);

                        byte[] audioBytes = new byte[byteCount];
                        bb.get(audioBytes);

                        // write to JavaSound
                        line.write(audioBytes, 0, audioBytes.length);
                        long now = System.currentTimeMillis();
                        if (now - lastUpdate > 60) {
                            lastUpdate = now;
                            // optional: extract samples for waveform (use only one channel or mix)
                            short[] pcm = new short[sampleCount];
                            ByteBuffer bb2 = ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN);
                            for (int i = 0; i < sampleCount; i++) {
                                short left = bb2.getShort();
                                short right = bb2.getShort();
                                pcm[i] = (short) ((left + right) / 2); // mix to mono for waveform
                            }
                            finalV.updateSamples(pcm);
                        }
                    }
                }
                );

                JPanel wrapper = new JPanel();
                wrapper.setOpaque(false);
                wrapper.setLayout(new FlowLayout());
                wrapper.add(visualizerPanel);
                visualizerPanel.setPreferredSize(new Dimension(150, 50)); // height limited
                controlsPane.add(wrapper, BorderLayout.SOUTH);
                //add(wavePanel, BorderLayout.SOUTH);
            } catch (Exception ex) {

            }
        }
        pauseButton.addActionListener(al -> {
            paused = !paused;
            if (paused) {
                pauseButton.setText("▶");
                if (session != null) {
                    session.pause();
                }
                if (visualizerPanel != null) {
                    visualizerPanel.pause();
                }
            } else {
                pauseButton.setText("⏸");
                pauseExempt = true;
                Alhena.pauseMedia();
                pauseExempt = false;
                if (session != null) {
                    session.resume();
                }
                if (visualizerPanel != null) {
                    visualizerPanel.resume();
                }
            }

            mediaPlayerComponent.mediaPlayer().submit(() -> {
                if (ended) {
                    ended = false;

                    EventQueue.invokeLater(() -> {
                        paused = false;
                        pauseButton.setText("⏸");
                    });

                    mediaPlayerComponent.mediaPlayer().media().play(mrl);
                } else {

                    mediaPlayerComponent.mediaPlayer().controls().pause(); // pause() will toggle play/pause

                }
            });

        });

    }

    private boolean pauseExempt;
    private long lastUpdate;

    // called to pause players when new player link is opened
    // call from event dispatch thread
    @Override
    public void pause() {
        if (!paused && !pauseExempt) {
            paused = true;
            pauseButton.setText("▶");
            if (visualizerPanel != null) {
                visualizerPanel.pause();
            }
            mediaPlayerComponent.mediaPlayer().submit(() -> {
                if (session != null) {
                    session.pause();
                }
                mediaPlayerComponent.mediaPlayer().controls().pause(); // pause() will toggle play/pause

            });

        }
    }

    private String mrl;

    @Override
    public void start(String mrl) {

        this.mrl = mrl;
        mediaPlayerComponent.mediaPlayer().submit(() -> {
            mediaPlayerComponent.mediaPlayer().media().play(mrl);
        });

    }

    @Override
    public void dispose() {
        System.out.println("audio player dispose");

        mediaPlayerComponent.mediaPlayer().submit(() -> {
            mediaPlayerComponent.mediaPlayer().controls().stop();
            mediaPlayerComponent.mediaPlayer().release();
            if (line != null) {
                line.stop();
                line.drain();
                line.close();

            }
            if (visualizerPanel != null) {
                EventQueue.invokeLater(() -> {
                    visualizerPanel.dispose();
                });

            }
        });
    }

}
