package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.BorderFactory;
import javax.swing.InputMap;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.fife.ui.rsyntaxtextarea.AbstractTokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxScheme;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.TokenTypes;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.fife.ui.rtextarea.SearchContext;
import org.fife.ui.rtextarea.SearchEngine;

import com.formdev.flatlaf.util.SystemInfo;

/**
 * Panel with tabs for a text editor and file chooser. Used for Spartan and
 * Titan uploads.
 *
 * @author Brad Grier
 */
public class TextEditor extends JPanel implements ActionListener {

    private RSyntaxTextArea advTextArea = null;
    //private JTextPane textArea = null;
    private final JFileChooser fileChooser;
    private final JTabbedPane tabbedPane;
    private final JTextField tokenField;
    private String searchField = "";
    private JCheckBoxMenuItem regexCB;
    private JCheckBoxMenuItem matchCaseCB;
    private int mod = SystemInfo.isMacOS ? KeyEvent.META_DOWN_MASK : KeyEvent.CTRL_DOWN_MASK;
    private JMenuItem findItem;
    JMenuItem findNextItem;

    public TextEditor(String text, boolean token) {
        setLayout(new BorderLayout(0, 10));
        tabbedPane = new JTabbedPane();
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        advTextArea = new RSyntaxTextArea(20, 60);
        EventQueue.invokeLater(() -> {
            Window w = SwingUtilities.getWindowAncestor(TextEditor.this);
            if (w instanceof JDialog dialog) {
                JRootPane rootPane = dialog.getRootPane();

                InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap am = rootPane.getActionMap();

                KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F,
                        Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx());

                im.put(ks, "find");
                am.put("find", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        //forward = true;
                        findItem.doClick(0);
                    }
                });

                KeyStroke findNextKS = KeyStroke.getKeyStroke("F3");
                im.put(findNextKS, "findnext");
                am.put("findnext", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        forward = true;
                        findNextItem.doClick(0);

                    }
                });
                KeyStroke.getKeyStroke("shift F3");
                KeyStroke findPrevKS = KeyStroke.getKeyStroke("shift F3");
                im.put(findPrevKS, "findprev");
                am.put("findprev", new AbstractAction() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        forward = false;
                        findNextItem.doClick(0);
                    }
                });
            }
        });

        advTextArea.requestFocusInWindow();
        advTextArea.setLineWrap(true);

        advTextArea.setFont(new Font(GeminiFrame.proportionalFamily, Font.PLAIN, 18));
        applyFlatLafColors(advTextArea);
        AbstractTokenMakerFactory atmf = (AbstractTokenMakerFactory) TokenMakerFactory.getDefaultInstance();
        atmf.putMapping("text/gemini", "brad.grier.alhena.GemtextTokenMaker");
        advTextArea.setSyntaxEditingStyle("text/gemini");

        advTextArea.setHighlightCurrentLine(false);
        advTextArea.setCodeFoldingEnabled(false);

        RTextScrollPane sp = new RTextScrollPane(advTextArea);

        sp.setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));
        //tabbedPane.addTab("Text", sp);
        editorPanel.add(sp, BorderLayout.CENTER);
        advTextArea.setText(text);
        EventQueue.invokeLater(() -> {
            sp.getVerticalScrollBar().setValue(0);
            advTextArea.setCaretPosition(0);
            advTextArea.requestFocusInWindow();
        });

        fileChooser = new JFileChooser();
        fileChooser.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0),
                BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UIManager.getColor("ComboBox.buttonSeparatorColor")), BorderFactory.createEmptyBorder(10, 10, 10, 10))));

        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        fileChooser.setControlButtonsAreShown(false);

        JPanel tokenPanel = new JPanel();
        tokenPanel.setLayout(new BorderLayout(5, 0));
        tokenPanel.add(new JLabel(I18n.t("titanTokenLabel")), BorderLayout.WEST);
        tokenPanel.add(tokenField = new JTextField(), BorderLayout.CENTER);
        tokenField.addMouseListener(new ContextMenuMouseListener());
        add(tokenPanel, BorderLayout.SOUTH);
        tokenPanel.setVisible(token);

        setPreferredSize(new Dimension(800, 440));

        List<JComponent> items = new ArrayList<>();
        findItem = new JMenuItem(I18n.t("titanFind"));
        KeyStroke ks = KeyStroke.getKeyStroke(KeyEvent.VK_F, mod);
        findItem.setAccelerator(ks);
        findNextItem = new JMenuItem(I18n.t("titanFindNext"));
        findItem.addActionListener(al -> {
            GeminiFrame gf = (GeminiFrame) SwingUtilities.getAncestorOfClass(GeminiFrame.class, TextEditor.this);
            String input = Util.inputDialog(gf, I18n.t("titanSearchDialog"), I18n.t("titanSearchDialogMsg"), false, "", null);
            if (input != null) {
                searchField = input;
                forward = true;
                findNextItem.doClick(0);
            }
        });

        findNextItem.addActionListener(TextEditor.this);
        findNextItem.setAccelerator(KeyStroke.getKeyStroke("F3"));
        JMenuItem findPrevItem = new JMenuItem(I18n.t("titanFindPrevious"));
        findPrevItem.setAccelerator(KeyStroke.getKeyStroke("shift F3"));
        findPrevItem.addActionListener(al -> {
            forward = false;
            findNextItem.doClick(0);
        });
        regexCB = new JCheckBoxMenuItem(I18n.t("titanRegex"));
        matchCaseCB = new JCheckBoxMenuItem(I18n.t("titanMatchCase"));
        Supplier<List<JComponent>> supplier = () -> {

            if (items.isEmpty()) {

                items.add(findItem);

                items.add(findNextItem);

                items.add(findPrevItem);
                items.add(new JSeparator());

                items.add(regexCB);

                items.add(matchCaseCB);
            }

            return items;
        };

        JPanel tb = new JPanel(new BorderLayout(0, 0));
        PopupMenuButton pmb = new PopupMenuButton("🔎", supplier, "");
        pmb.setFont(new Font("Noto Emoji Regular", Font.PLAIN, 18));
        tb.add(new JLabel(I18n.t("titanEditorMsg")), BorderLayout.WEST);
        tb.add(pmb, BorderLayout.EAST);
        editorPanel.add(tb, BorderLayout.NORTH);

        tabbedPane.addTab(I18n.t("titanTextTab"), editorPanel);
        tabbedPane.addTab(I18n.t("titanFileTab"), fileChooser);
        add(tabbedPane, BorderLayout.CENTER);

        // Can't get this to work with custom token maker at this time
        // File zip = new File("/Users/brad/.alhena/english_dic.zip");
        // System.out.println(zip.exists());
        // SpellingParser parser;
        // try {
        //     parser = SpellingParser.createEnglishSpellingParser(zip, false, false);
        //     advTextArea.addParser(parser);
        // } catch (IOException ex) {
        //     ex.printStackTrace();
        // }
    }

    public Object getResult() {
        int idx = tabbedPane.getSelectedIndex();
        if (idx == 0) {
            return advTextArea.getText();
        } else {
            return fileChooser.getSelectedFile();
        }
    }

    public String getTokenParam() {
        String token = tokenField.getText().isBlank() ? null : tokenField.getText();
        if (token != null) {
            return ";token=" + Util.uEncode(token);
        } else {
            return "";
        }
    }

    public final void applyFlatLafColors(RSyntaxTextArea textArea) {
        SyntaxScheme scheme = textArea.getSyntaxScheme();

        Color bg = UIManager.getColor("TextArea.background");
        Color fg = UIManager.getColor("TextArea.foreground");
        Color selBg = UIManager.getColor("TextArea.selectionBackground");
        Color selFg = UIManager.getColor("TextArea.selectionForeground");

        textArea.setBackground(bg);
        textArea.setForeground(fg);
        textArea.setCaretColor(fg);
        textArea.setSelectionColor(selBg);
        textArea.setSelectedTextColor(selFg);

        boolean isDark = UIManager.getBoolean("laf.dark");
        Color linkColor = UIManager.getColor("Component.linkColor");
        scheme.getStyle(TokenTypes.RESERVED_WORD).foreground = AnsiColor.adjustColor(isDark ? linkColor.brighter() : linkColor.darker(), isDark, .1, .9, .2);

        scheme.getStyle(TokenTypes.FUNCTION).foreground = linkColor;

        Color pfText = AnsiColor.adjustColor(isDark ? Color.GRAY.brighter() : Color.GRAY.darker(), isDark, .1, .9, .2);
        Font monoF = new Font(GeminiTextPane.monospacedFamily, Font.PLAIN, 16);
        scheme.getStyle(TokenTypes.COMMENT_MULTILINE).foreground = pfText;
        scheme.getStyle(TokenTypes.COMMENT_MULTILINE).font = monoF;
        scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).foreground = pfText; // contents
        scheme.getStyle(TokenTypes.LITERAL_STRING_DOUBLE_QUOTE).font = monoF;

        scheme.getStyle(TokenTypes.OPERATOR).foreground = fg;

        scheme.getStyle(TokenTypes.OPERATOR).font = new Font(GeminiFrame.proportionalFamily, Font.BOLD, 16);

        textArea.repaint();
    }

    private boolean forward;

    @Override
    public void actionPerformed(ActionEvent e) {

        SearchContext context = new SearchContext();
        String text = searchField;
        if (text.length() == 0) {
            SearchEngine.markAll(advTextArea, new SearchContext(""));
            advTextArea.setCaretPosition(0);
            return;
        }
        context.setSearchFor(text);
        context.setMatchCase(matchCaseCB.isSelected());
        context.setRegularExpression(regexCB.isSelected());
        context.setSearchForward(forward);
        context.setWholeWord(false);

        boolean found = SearchEngine.find(advTextArea, context).wasFound();
        if (!found) {
            Util.infoDialog(this, I18n.t("titanNotFoundDialog"), I18n.t("titanNotFoundDialogMsg"));

            advTextArea.select(0, 0);
        }

    }

}
