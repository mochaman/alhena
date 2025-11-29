package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.AudioPlayerComponent;

public class AudioPlayer extends JPanel implements MediaComponent {

    private final AudioPlayerComponent mediaPlayerComponent;
    private boolean paused;
    private volatile boolean ended;
    private JButton pauseButton;
    private boolean stopped;
    private StreamSession session;

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
        JLabel timeLabel = new JLabel(" ");
        timeLabel.setPreferredSize(new Dimension(100, timeLabel.getPreferredSize().height));
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
        controlsPane.setOpaque(false);
        pauseButton = new JButton("⏸");
        pauseButton.setFont(buttonFont);
        controlsPane.add(pauseButton);
        controlsPane.add(timeLabel);
        add(controlsPane, BorderLayout.CENTER);

        pauseButton.addActionListener(al -> {
            paused = !paused;
            if (paused) {
                pauseButton.setText("▶");
                if (session != null) {
                    session.pause();
                }
            } else {
                pauseButton.setText("⏸");
                pauseExempt = true;
                Alhena.pauseMedia();
                pauseExempt = false;
                if (session != null) {
                    session.resume();
                }
            }

            mediaPlayerComponent.mediaPlayer().submit(() -> {
                if (ended) {
                    ended = false;
                    paused = false;
                    pauseButton.setText("⏸");
                    mediaPlayerComponent.mediaPlayer().media().play(mrl);
                } else {

                    mediaPlayerComponent.mediaPlayer().controls().pause(); // pause() will toggle play/pause

                }
            });

        });

    }

    private boolean pauseExempt;

    // called to pause players when new player link is opened
    // call from event dispatch thread
    @Override
    public void pause() {
        if (!paused && !pauseExempt) {
            paused = true;
            pauseButton.setText("▶");
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
        });
    }

}
