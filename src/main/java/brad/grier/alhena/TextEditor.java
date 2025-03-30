package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;

import com.formdev.flatlaf.util.SystemInfo;

/**
 *  Panel with tabs for a text editor and file chooser. Used for Spartan and Titan uploads.
 * 
 * @author Brad Grier
 */
public class TextEditor extends JPanel {

    private final JTextPane textArea;
    private final JFileChooser fileChooser;
    private final JTabbedPane tabbedPane;

    public TextEditor(String text) {
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();
        textArea = new JTextPane();
        String fontName = SystemInfo.isWindows ? "Source Code Pro" : "Monospaced";
        Font font = new Font(fontName, Font.PLAIN, 14);
        textArea.setFont(font);

        tabbedPane.addTab("Text", new JScrollPane(textArea));

        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setControlButtonsAreShown(false);

        tabbedPane.addTab("File", fileChooser);
        add(tabbedPane, BorderLayout.CENTER);
        textArea.setText(text);
        setPreferredSize(new Dimension(800, 400));

    }

    public Object getResult(){
        int idx = tabbedPane.getSelectedIndex();
        if(idx == 0){
            return textArea.getText();
        }else{
            return fileChooser.getSelectedFile();
        }
    }

}
