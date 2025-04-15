package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.io.File;
import java.net.URLEncoder;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.UIManager;

import com.formdev.flatlaf.util.SystemInfo;

/**
 * Panel with tabs for a text editor and file chooser. Used for Spartan and
 * Titan uploads.
 *
 * @author Brad Grier
 */
public class TextEditor extends JPanel {

    private final JTextPane textArea;
    private final JFileChooser fileChooser;
    private final JTabbedPane tabbedPane;
    private final JTextField tokenField;

    public TextEditor(String text, boolean token) {
        setLayout(new BorderLayout(0, 10));
        tabbedPane = new JTabbedPane();
        textArea = new JTextPane();
        textArea.requestFocusInWindow();

        String fontName = SystemInfo.isWindows ? "Source Code Pro" : "Monospaced";
        Font font = new Font(fontName, Font.PLAIN, 14);
        textArea.setFont(font);
        textArea.addMouseListener(new ContextMenuMouseListener());
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
        tabbedPane.addTab("Text", scrollPane);

        fileChooser = new JFileChooser();
        fileChooser.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("ComboBox.buttonSeparatorColor")), BorderFactory.createEmptyBorder(10, 10, 10, 10))));

        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setControlButtonsAreShown(false);

        tabbedPane.addTab("File", fileChooser);
        add(tabbedPane, BorderLayout.CENTER);

        JPanel tokenPanel = new JPanel();
        tokenPanel.setLayout(new BorderLayout(5, 0));
        tokenPanel.add(new JLabel("Token:"), BorderLayout.WEST);
        tokenPanel.add(tokenField = new JTextField(), BorderLayout.CENTER);
        tokenField.addMouseListener(new ContextMenuMouseListener());
        add(tokenPanel, BorderLayout.SOUTH);
        tokenPanel.setVisible(token);
        textArea.setText(text);
        setPreferredSize(new Dimension(800, 400));
        EventQueue.invokeLater(() -> scrollPane.getVerticalScrollBar().setValue(0));

    }

    public Object getResult() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx == 0) {
            return textArea.getText();
        } else {
            return fileChooser.getSelectedFile();
        }
    }

    public String getTokenParam() {
        String token = tokenField.getText().isBlank() ? null : tokenField.getText();
        if (token != null) {
            return ";token=" + URLEncoder.encode(token).replace("+", "%20");
        } else {
            return "";
        }
    }

}
