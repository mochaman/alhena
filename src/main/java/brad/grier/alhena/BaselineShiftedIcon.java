package brad.grier.alhena;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;

import javax.swing.ImageIcon;

public class BaselineShiftedIcon extends ImageIcon {

    private final int shiftDown;
    private final int iWidth, iHeight;

    public BaselineShiftedIcon(Image img, int shiftDown){
        super(img);
        
        iWidth = img.getWidth(null);
        iHeight = img.getHeight(null);
        this.shiftDown = shiftDown;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        super.paintIcon(c, g, x, y + shiftDown);
    }

    @Override
    public int getIconWidth() {
        return iWidth;
    }

    @Override
    public int getIconHeight() {
        return iHeight - shiftDown;

    }
}
