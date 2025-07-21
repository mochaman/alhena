package brad.grier.alhena;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.UIManager;
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

    public PreformattedTextPane(Color bgColor) {
        setMargin(new Insets(2, 0, 2, 0));
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
        StyleConstants.setFontSize(pfStyle, GeminiFrame.monoFontSize);
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

    private void addStyledText(String text, String styleName) {

        Style style = doc.getStyle(styleName);
        boolean ansi = text.indexOf(27) >= 0;

        int start = doc.getLength();

        if (!(ansi && styleName.equals("```")) && EmojiManager.containsAnyEmoji(text)) {

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

                            insertString(doc.getLength(), new String(chars), style, ansi);

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

                        insertString(doc.getLength(), new String(chars), style, ansi);
                    }
                } else {

                    StyleConstants.setFontFamily(style, fontFamily);

                    insertString(doc.getLength(), String.valueOf(text.charAt(i)), style, ansi);

                }
            }
            StyleConstants.setFontFamily(style, fontFamily);
        } else {
            insertString(start, text, style, ansi);
        }

        int caretPosition = getCaretPosition();
        // if (!lastLine) {
        try {
            doc.insertString(doc.getLength(), "\n", style);
        } catch (BadLocationException ex) {
        }

        setCaretPosition(caretPosition); // prevent scrolling as content added

    }

    private void insertString(int length, String txt, Style style, boolean ansi) {
        try {
            if (ansi) {
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

    private void convert(String txt) {

        if (!txt.startsWith("[")) {
            if (foregroundHandling) {
                StyleConstants.setForeground(bStyle, getForeground());
            }
            try {
                doc.insertString(doc.getLength(), txt, bStyle);
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        } else {
            int mIdx = txt.indexOf('m');
            String line = txt.substring(mIdx + 1);

            String ansi = txt.substring(0, mIdx + 1);

            String[] tokens = ansi.split(";");
            switch (tokens[0]) {
                case "[38" -> {
                    // foreground color
                    if (tokens[1].equals("5")) {

                        if (tokens.length == 4) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                            ansiFG(c);

                        } else if (tokens.length == 3) {

                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2].substring(0, tokens[2].indexOf('m'))));
                            ansiFG(c);

                        }

                    }
                }
                case "[30m" ->
                    ansiFG(AnsiColor.BLACK);
                case "[31m" ->
                    ansiFG(AnsiColor.RED);
                case "[32m" ->
                    ansiFG(AnsiColor.GREEN);
                case "[33m" ->
                    ansiFG(AnsiColor.YELLOW);
                case "[34m" ->
                    ansiFG(AnsiColor.BLUE);
                case "[35m" ->
                    ansiFG(AnsiColor.MAGENTA);
                case "[36m" ->
                    ansiFG(AnsiColor.CYAN);
                case "[37m" ->
                    ansiFG(AnsiColor.WHITE);
                case "[40m" ->
                    ansiBG(Color.BLACK);
                case "[41m" ->
                    ansiBG(AnsiColor.RED);
                case "[42m" ->
                    ansiBG(AnsiColor.GREEN);
                case "[43m" ->
                    ansiBG(AnsiColor.YELLOW);
                case "[44m" ->
                    ansiBG(AnsiColor.BLUE);
                case "[45m" ->
                    ansiBG(AnsiColor.MAGENTA);
                case "[46m" ->
                    ansiBG(AnsiColor.CYAN);
                case "[47m" ->
                    ansiBG(AnsiColor.WHITE);
                case "[48" -> {
                    // foreground color
                    if (tokens[1].equals("5")) {

                        if (tokens.length == 4) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2]));
                            ansiBG(c);

                        } else if (tokens.length == 3) {
                            Color c = AnsiColor.ansiToColor(Integer.parseInt(tokens[2].substring(0, tokens[2].indexOf('m'))));
                            ansiBG(c);

                        }

                    }
                }
                case "[90m" ->
                    ansiFG(AnsiColor.BRIGHT_BLACK);
                case "[91m" ->
                    ansiFG(AnsiColor.BRIGHT_RED);
                case "[92m" ->
                    ansiFG(AnsiColor.BRIGHT_GREEN);
                case "[93m" ->
                    ansiFG(AnsiColor.BRIGHT_YELLOW);
                case "[94m" ->
                    ansiFG(AnsiColor.BRIGHT_BLUE);
                case "[95m" ->
                    ansiFG(AnsiColor.BRIGHT_MAGENTA);
                case "[96m" ->
                    ansiFG(AnsiColor.BRIGHT_CYAN);
                case "[97m" ->
                    ansiFG(AnsiColor.BRIGHT_WHITE);
                case "[100m" ->
                    ansiBG(AnsiColor.BRIGHT_BLACK);
                case "[101m" ->
                    ansiBG(AnsiColor.BRIGHT_RED);
                case "[102m" ->
                    ansiBG(AnsiColor.BRIGHT_GREEN);
                case "[103m" ->
                    ansiBG(AnsiColor.BRIGHT_YELLOW);
                case "[104m" ->
                    ansiBG(AnsiColor.BRIGHT_BLUE);
                case "[105m" ->
                    ansiBG(AnsiColor.BRIGHT_MAGENTA);
                case "[106m" ->
                    ansiBG(AnsiColor.BRIGHT_CYAN);
                case "[107m" ->
                    ansiBG(AnsiColor.BRIGHT_WHITE);
                case "[0m" -> {

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
                case "[1m" -> {
                    StyleConstants.setBold(bStyle, true);
                }
                //default ->
                //System.out.println("unknown: " + txt);
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
        StyleConstants.setForeground(bStyle, AnsiColor.adjustColor(c, UIManager.getBoolean("laf.dark"), .2d, .8d, .15d));
        foregroundHandling = false;
    }

    private void ansiBG(Color c) {
        StyleConstants.setBackground(bStyle, AnsiColor.adjustColor(c, UIManager.getBoolean("laf.dark"), .2d, .8d, .15d));
    }

}
