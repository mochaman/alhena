package brad.grier.alhena;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

import com.formdev.flatlaf.util.SystemInfo;
import com.techsenger.ansi4j.core.api.Environment;
import com.techsenger.ansi4j.core.api.Fragment;
import com.techsenger.ansi4j.core.api.FragmentType;
import com.techsenger.ansi4j.core.api.ParserFactory;
import com.techsenger.ansi4j.core.api.TextFragment;
import com.techsenger.ansi4j.core.api.iso6429.ControlFunctionType;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryConfig;
import com.techsenger.ansi4j.core.api.spi.ParserFactoryService;
import com.techsenger.ansi4j.core.impl.ParserFactoryProvider;

import net.fellbaum.jemoji.EmojiManager;
import net.fellbaum.jemoji.IndexedEmoji;

public class PreformattedTextPane extends JTextPane {

    private String emojiProportional;
    private StyledDocument doc;
    private String bufferedLine = null;
    private int fontSize;
    private boolean isDark;

    public PreformattedTextPane(Color bgColor, Integer fontSize, boolean isDark) {
        if (fontSize != null) {
            this.fontSize = fontSize;
        }
        this.isDark = isDark;
        setMargin(new Insets(0, 0, 0, 0));
        setEditable(false);
        setBackground(bgColor);
        DefaultCaret newCaret = new DefaultCaret() {
            @Override
            public void paint(Graphics g) {
                // do nothing to prevent caret from being painted
            }
        };
        setCaret(newCaret);

        setEditorKit(new GeminiEditorKit());
        init(bgColor);

    }

    private void buildStyle(Color bgColor) {
        emojiProportional = "Noto Emoji";
        if (SystemInfo.isMacOS) {
            boolean macUseNoto = DB.getPref("macusenoto", "false").equals("true");
            if (!macUseNoto) {
                emojiProportional = "SansSerif";
            }
        }
        Style pfStyle = doc.addStyle("```", null);
        StyleConstants.setFontFamily(pfStyle, GeminiTextPane.monospacedFamily);
        StyleConstants.setBackground(pfStyle, bgColor);
        StyleConstants.setFontSize(pfStyle, fontSize != 0 ? fontSize : GeminiFrame.monoFontSize);
        StyleConstants.setBold(pfStyle, false);
        StyleConstants.setItalic(pfStyle, false);
        StyleConstants.setUnderline(pfStyle, false);
    }

    public final void init(Color bgColor) {
        doc = new DefaultStyledDocument();
        setStyledDocument(doc);
        buildStyle(bgColor);
    }

    public void end() {
        if (bufferedLine != null) {
            String lrl = bufferedLine;
            bufferedLine = null;
            addStyledText(lrl, "```");
        }
    }

    public void removeLastChar() {
        int length = doc.getLength();
        if (length > 0) {
            try {
                doc.remove(length - 1, 1);  // remove the last character
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

    }

    public void addText(String geminiDoc) {

        if (bufferedLine != null) {
            geminiDoc = bufferedLine + geminiDoc;
            bufferedLine = null;
        }
        if (geminiDoc.endsWith("\n")) {

            //geminiDoc.lines().forEach(line -> processLine(line)); // no way to know if a line is the last line
            geminiDoc.lines().forEach(line -> addStyledText(line, "```"));

        } else {
            int lastNl = geminiDoc.lastIndexOf("\n");
            if (lastNl == -1) {
                // no newlines at all save
                bufferedLine = geminiDoc;
            } else {
                bufferedLine = geminiDoc.substring(lastNl + 1);
                geminiDoc.substring(0, lastNl + 1).lines().forEach(line -> {
                    //processLine(line); // no way to know if a line is the last line
                    addStyledText(line, "```");
                });
            }
        }
    }

    private boolean hasAnsi;

    private void addStyledText(String text, String styleName) {

        Style style = doc.getStyle(styleName);
        if (!hasAnsi) {
            hasAnsi = text.indexOf(27) >= 0;
        }

        int start = doc.getLength();

        if (!(hasAnsi && styleName.equals("```")) && EmojiManager.containsAnyEmoji(text)) {

            String fontFamily = StyleConstants.getFontFamily(style);
            int fontSize = StyleConstants.getFontSize(style);

            SimpleAttributeSet emojiStyle = new SimpleAttributeSet(style);

            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(text);

            IndexedEmoji emoji;
            // can't iterate by code point without preprocessing first to get name
            for (int i = 0; i < text.length(); i++) {

                if ((emoji = GeminiTextPane.isEmoji(emojis, i)) != null) {

                    if (GeminiTextPane.sheetImage != null) {

                        String key = GeminiTextPane.getEmojiHex(emoji);

                        Point p = GeminiTextPane.emojiSheetMap.get(key);
                        ImageIcon icon = null;
                        int imgSize = fontSize + 4;
                        if (p != null) {
                            icon = GeminiTextPane.extractSprite(p.x, p.y, 64, imgSize, imgSize, fontSize);
                        } else {
                            int dashIdx = key.indexOf('-');
                            if (dashIdx != -1) {

                                p = GeminiTextPane.emojiSheetMap.get(key.substring(0, dashIdx));
                                if (p != null) {
                                    icon = GeminiTextPane.extractSprite(p.x, p.y, 64, imgSize, imgSize, fontSize);
                                }
                            }
                        }
                        if (icon == null) {

                            char[] chars = Character.toChars(text.codePointAt(i));

                            i = emoji.getEndCharIndex() + 1;

                            insertString(doc.getLength(), new String(chars), style);

                        } else {
                            // single char emoji followed by unneccessary variation selector
                            // example: snowman
                            if (i == emoji.getEndCharIndex() - 1) {
                                i++;
                            } else {

                                i = emoji.getEndCharIndex() - 1;

                            }

                            StyleConstants.setIcon(emojiStyle, icon);
                            try {
                                doc.insertString(doc.getLength(), " ", emojiStyle); // Use emoji style
                            } catch (BadLocationException ex) {
                            }

                        }
                    } else {

                        char[] chars = Character.toChars(text.codePointAt(i));

                        i++;

                        StyleConstants.setFontFamily(style, emojiProportional);

                        insertString(doc.getLength(), new String(chars), style);
                    }
                } else {

                    StyleConstants.setFontFamily(style, fontFamily);

                    insertString(doc.getLength(), String.valueOf(text.charAt(i)), style);

                }
            }
            StyleConstants.setFontFamily(style, fontFamily);
        } else {
            insertString(start, text, style);
        }

        int caretPosition = getCaretPosition();
        // if (!lastLine) {
        try {
            doc.insertString(doc.getLength(), "\n", style);
        } catch (BadLocationException ex) {
        }

        setCaretPosition(caretPosition); // prevent scrolling as content added

    }

    private void insertString(int length, String txt, Style style) {
        try {
            if (hasAnsi) {
                handleAnsi(txt);
            } else {
                doc.insertString(length, txt, style);
            }
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }
    private ParserFactory factory;
    private SimpleAttributeSet bStyle;

    private void handleAnsi(String line) {

        if (factory == null) {
            ParserFactoryConfig config = new ParserFactoryConfig();
            config.setEnvironment(Environment._8_BIT);
            config.setFunctionTypes(List.of(ControlFunctionType.C0_SET, ControlFunctionType.C1_SET));
            ParserFactoryService factoryService = new ParserFactoryProvider();
            factory = factoryService.createFactory(config);
        }

        if (bStyle == null) {
            // copy the preformat style
            bStyle = new SimpleAttributeSet(doc.getStyle("```"));
        }

        var parser = factory.createParser(line);
        Fragment fragment = null;
        while ((fragment = parser.parse()) != null) {
            if (fragment.getType() == FragmentType.TEXT) {
                TextFragment textFragment = (TextFragment) fragment;
                convert(textFragment.getText());
            }
        }

    }
    private boolean foregroundHandling;

    public void insertComp(Component c) {
        SimpleAttributeSet apStyle = new SimpleAttributeSet();
        StyleConstants.setComponent(apStyle, c);

        try {
            doc.insertString(0, " ", apStyle);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
        EventQueue.invokeLater(() -> {
            JScrollPane jsp = (JScrollPane) SwingUtilities.getAncestorOfClass(JScrollPane.class, this);
            jsp.getHorizontalScrollBar().setValue(0);
        });

    }

    private void plainText(String txt) {
        if (foregroundHandling) {
            StyleConstants.setForeground(bStyle, getForeground());
        }
        try {
            doc.insertString(doc.getLength(), txt, bStyle);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void convert(String txt) {

        if (!txt.startsWith("[")) {
            plainText(txt);
        } else {
            int mIdx = txt.indexOf('m');
            if(mIdx == -1){
                plainText(txt);
                return;
            }

            String line = txt.substring(mIdx + 1);

            //String ansi = txt.substring(0, mIdx + 1);
            String ansi = txt.substring(1, mIdx);

            String[] tokens = ansi.split(";");
            outer:
            for (String token : tokens) {
                switch (token) {
                    case "38" -> {
                        // foreground color
                        if (tokens[1].equals("5")) {

                            if (tokens.length == 4) { // no idea why originally put this here - corner case example?
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiFG(c);

                            } else if (tokens.length == 3) {

                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiFG(c);

                            }

                        } else if (tokens[1].equals("2")) {
                            int r = Integer.parseInt(tokens[2]);
                            int g = Integer.parseInt(tokens[3]);
                            int b = Integer.parseInt(tokens[4]);
                            ansiFG(new Color(r, g, b));

                        }
                        break outer;
                    }
                    case "30" ->
                        ansiFG(AnsiColor.BLACK);
                    case "31" ->
                        ansiFG(AnsiColor.RED);
                    case "32" ->
                        ansiFG(AnsiColor.GREEN);
                    case "33" ->
                        ansiFG(AnsiColor.YELLOW);
                    case "34" ->
                        ansiFG(AnsiColor.BLUE);
                    case "35" ->
                        ansiFG(AnsiColor.MAGENTA);
                    case "36" ->
                        ansiFG(AnsiColor.CYAN);
                    case "37" ->
                        ansiFG(AnsiColor.WHITE);
                    case "40" ->
                        ansiBG(Color.BLACK);
                    case "41" ->
                        ansiBG(AnsiColor.RED);
                    case "42" ->
                        ansiBG(AnsiColor.GREEN);
                    case "43" ->
                        ansiBG(AnsiColor.YELLOW);
                    case "44" ->
                        ansiBG(AnsiColor.BLUE);
                    case "45" ->
                        ansiBG(AnsiColor.MAGENTA);
                    case "46" ->
                        ansiBG(AnsiColor.CYAN);
                    case "47" ->
                        ansiBG(AnsiColor.WHITE);
                    case "48" -> {
                        // foreground color
                        if (tokens[1].equals("5")) {

                            if (tokens.length == 4) { // why is this here?
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiBG(c);

                            } else if (tokens.length == 3) {
                                Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                                ansiBG(c);

                            }
                        } else if (tokens[1].equals("2")) {
                            int r = Integer.parseInt(tokens[2]);
                            int g = Integer.parseInt(tokens[3]);
                            int b = Integer.parseInt(tokens[4]);
                            ansiBG(new Color(r, g, b));
                        }
                        break outer;
                    }
                    case "90" ->
                        ansiFG(AnsiColor.BRIGHT_BLACK);
                    case "91" ->
                        ansiFG(AnsiColor.BRIGHT_RED);
                    case "92" ->
                        ansiFG(AnsiColor.BRIGHT_GREEN);
                    case "93" ->
                        ansiFG(AnsiColor.BRIGHT_YELLOW);
                    case "94" ->
                        ansiFG(AnsiColor.BRIGHT_BLUE);
                    case "95" ->
                        ansiFG(AnsiColor.BRIGHT_MAGENTA);
                    case "96" ->
                        ansiFG(AnsiColor.BRIGHT_CYAN);
                    case "97" ->
                        ansiFG(AnsiColor.BRIGHT_WHITE);
                    case "100" ->
                        ansiBG(AnsiColor.BRIGHT_BLACK);
                    case "101" ->
                        ansiBG(AnsiColor.BRIGHT_RED);
                    case "102" ->
                        ansiBG(AnsiColor.BRIGHT_GREEN);
                    case "103" ->
                        ansiBG(AnsiColor.BRIGHT_YELLOW);
                    case "104" ->
                        ansiBG(AnsiColor.BRIGHT_BLUE);
                    case "105" ->
                        ansiBG(AnsiColor.BRIGHT_MAGENTA);
                    case "106" ->
                        ansiBG(AnsiColor.BRIGHT_CYAN);
                    case "107" ->
                        ansiBG(AnsiColor.BRIGHT_WHITE);
                    case "0" -> {

                        // setting the forground on blank strings causes a layout change that breaks
                        // the no line wrap on preformatted text
                        if (!line.isBlank()) {
                            StyleConstants.setForeground(bStyle, getForeground());
                        } else {
                            foregroundHandling = true;

                        }

                        StyleConstants.setBackground(bStyle, getBackground());
                        StyleConstants.setBold(bStyle, false);

                    }
                    case "1" -> {
                        StyleConstants.setBold(bStyle, true);
                    }
                    // default ->
                    //     System.out.println("unknown: " + txt);
                }
            }

            try {

                if (foregroundHandling && !line.isBlank()) {
                    StyleConstants.setForeground(bStyle, getForeground());
                }

                doc.insertString(doc.getLength(), line, bStyle);

            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }

    }

    private void ansiFG(Color c) {
        StyleConstants.setForeground(bStyle, AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d));
        foregroundHandling = false;
    }

    private void ansiBG(Color c) {
        StyleConstants.setBackground(bStyle, AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d));
    }

}
