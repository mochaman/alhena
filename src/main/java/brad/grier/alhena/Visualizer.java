package brad.grier.alhena;

import javax.swing.JPanel;

public abstract class Visualizer extends JPanel {


    public abstract void updateSamples(short[] data);

    public void pause() {
    }

    public void resume() {
    }

    public void dispose(){

    }

}
