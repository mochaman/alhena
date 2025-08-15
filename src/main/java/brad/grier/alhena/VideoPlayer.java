package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.component.CallbackMediaPlayerComponent;
import uk.co.caprica.vlcj.player.component.callback.ScaledCallbackImagePainter;

public class VideoPlayer extends JPanel implements MediaComponent {

    private final CallbackMediaPlayerComponent mediaPlayerComponent;
    private boolean paused;
    private volatile boolean ended;
    private JButton pauseButton;
    private boolean stopped;
    private GeminiTextPane gtp;

    public VideoPlayer() {
        setLayout(new BorderLayout());
        final JSlider slider = new JSlider(0, 100, 0);

        Font buttonFont = new Font("Noto Emoji Regular", Font.PLAIN, 18);
        AtomicBoolean suppressEvents = new AtomicBoolean(false);

        mediaPlayerComponent = new CallbackMediaPlayerComponent() {
            @Override
            public void positionChanged(MediaPlayer mediaPlayer, float newPosition) {

                super.positionChanged(mediaPlayer, newPosition);
                EventQueue.invokeLater(() -> {
                    suppressEvents.set(true);
                    slider.setValue((int) (newPosition * 100));
                    suppressEvents.set(false);
                });

            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                super.mouseWheelMoved(e);
                EventQueue.invokeLater(() -> {
                    if (gtp == null) {
                        gtp = (GeminiTextPane) SwingUtilities.getAncestorOfClass(GeminiTextPane.class, this);
                    }
                    gtp.dispatchEvent(SwingUtilities.convertMouseEvent(mediaPlayerComponent, e, gtp));
                });

            }

        };
        mediaPlayerComponent.setImagePainter(new ScaledCallbackImagePainter());

        add(mediaPlayerComponent, BorderLayout.CENTER);
        mediaPlayerComponent.mediaPlayer().events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
            @Override
            public void finished(MediaPlayer mediaPlayer) {
                paused = false;
                stopped = false;
                ended = true;
                EventQueue.invokeLater(() -> {
                    pauseButton.setText("▶");

                    suppressEvents.set(true);
                    slider.setValue(0);
                    suppressEvents.set(false);
                });
            }

            @Override
            public void error(MediaPlayer mediaPlayer) {
                EventQueue.invokeLater(() -> {
                    Util.infoDialog(VideoPlayer.this, I18n.t("playErrorDialog"), I18n.t("playErrorDialogMsg"), JOptionPane.ERROR_MESSAGE);
                });
            }
        });

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
                //mediaPlayerComponent.mediaPlayer().controls().setPosition(value);
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
                //System.out.println("stopped: " + stopped + " paused: " + paused);
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

        JPanel southPanel = new JPanel(new BorderLayout());
        southPanel.add(slider, BorderLayout.CENTER);
        JPanel controlsPane = new JPanel();

        pauseButton = new JButton("⏸");
        pauseButton.setFont(buttonFont);
        controlsPane.add(pauseButton);

        // JButton rewindButton = new JButton("Rewind");
        // controlsPane.add(rewindButton);
        // JButton skipButton = new JButton("Skip");
        // controlsPane.add(skipButton);
        southPanel.add(controlsPane, BorderLayout.SOUTH);
        add(southPanel, BorderLayout.SOUTH);
        //contentPane.add(controlsPane, BorderLayout.SOUTH);

        pauseButton.addActionListener(al -> {
            paused = !paused;
            if (paused) {
                pauseButton.setText("▶");
            } else {
                pauseButton.setText("⏸");
                pauseExempt = true;
                Alhena.pauseMedia();
                pauseExempt = false;
            }

            mediaPlayerComponent.mediaPlayer().submit(() -> {
                if (ended) {
                    ended = false;
                    paused = false;
                    pauseButton.setText("⏸");
                    mediaPlayerComponent.mediaPlayer().media().play(mrl);
                } else {
                    mediaPlayerComponent.mediaPlayer().controls().pause();
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
        System.out.println("video player dispose");
        mediaPlayerComponent.mediaPlayer().submit(() -> {
            mediaPlayerComponent.mediaPlayer().controls().stop();
            mediaPlayerComponent.mediaPlayer().release();
        });
    }

    private Dimension pSize;
    @Override
    public Dimension getPreferredSize(){

        if(pSize == null){
            pSize = super.getPreferredSize();
        }
        return pSize;   
    }

}
