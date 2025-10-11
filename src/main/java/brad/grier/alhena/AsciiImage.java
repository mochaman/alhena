package brad.grier.alhena;

import java.awt.AlphaComposite;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

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

public class AsciiImage {

    private static Color bgColor1, fgColor1;
    private static Color ansiFG;
    private static Color ansiBG;
    private static boolean ansiBold;

    private static boolean isDark;

    // for future reference, this class is not thread safe - only call on EDT
    public static BufferedImage renderTextToImage(boolean shade, String text, String fontName, int fontSize, Color fgColor, Color bgColor, boolean override) {

        boolean hasAnsi = false;
        ansiBold = false;

        ansiBG = new Color(0, 0, 0, 0);

        ansiFG = fgColor;
        isDark = UIManager.getBoolean("laf.dark");
        bgColor1 = ansiBG;
        fgColor1 = fgColor;
        //ansiFG(Color.WHITE);  // "default foreground color" crossword site
        if (bgColor != null && Util.isLight(bgColor)) {
            ansiFG(Color.BLACK);
        } else {
            ansiFG(Color.WHITE);  // "default foreground color"

        }
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        String[] lines = text.split("\n", -1);

        int idx = 0;
        List<List<PositionColor>> colorList = new ArrayList<>();
        for (String line : lines) {
            if (line.isBlank()) {
                line = " ";
            }
            if (!hasAnsi) {
                hasAnsi = line.indexOf(27) >= 0;
            }

            String line1;
            if (hasAnsi) {

                AnsiInfo ai = handleAnsi(line);
                line1 = ai.line;
                colorList.add(ai.pcList);
            } else {
                line1 = line;
                colorList.add(null);
            }

            lines[idx++] = line1;

        }

        String emojiProportional = "Noto Emoji";

        if (!override) {
            //emojiProportional = "Noto Emoji";
            if (SystemInfo.isMacOS) {
                if (!Alhena.macUseNoto) {
                    emojiProportional = "SansSerif";
                }
            }
        }
        FontMetrics metrics = new Canvas().getFontMetrics(font);
        Font emojiFont = new Font(emojiProportional, Font.PLAIN, fontSize);
        FontMetrics emojiMetrics = new Canvas().getFontMetrics(emojiFont);
        int lineHeight = metrics.getHeight();
        int width = 0;

        int maxChars = 0;
        for (String line : lines) {
            int w = metrics.stringWidth(line);
            width = Math.max(width, w);
            maxChars = Math.max(maxChars, line.codePointCount(0, line.length()));
        }

        maxChars = override ? 1 : maxChars;
        int height = lineHeight * lines.length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);

        g.setComposite(AlphaComposite.SrcOver);
        g.setFont(font);

        g.setColor(fgColor);

        int cellHeight = height / lines.length;
        int cellWidth = override ? emojiMetrics.stringWidth(text) : width / maxChars;

        int baseY = lineHeight;
        int y = 0;
        int lineNum = 0;
        for (String line : lines) {
            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(line);
            IndexedEmoji emoji;

            int x = 0;

            for (int i = 0; i < line.length(); i++) {
                int pad = 0;
                char c = line.charAt(i);

                BufferedImage cellImage;

                // font.canDisplay test means use the font's version of this character instead of the emoji version
                // often this fixes assumptions made by page designers about character width (weather, etc)
                if ((emoji = GeminiTextPane.isEmoji(emojis, i)) != null && !font.canDisplay(c)) {

                    int charWidth = emojiMetrics.stringWidth(emoji.getEmoji().getEmoji());

                    int superFudge = charWidth <= cellWidth ? cellWidth : cellWidth * 2;
                    cellImage = new BufferedImage(superFudge, cellHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cellImage.createGraphics();
                    List<PositionColor> pcl = colorList.get(lineNum);
                    if (pcl != null) {
                        PositionColor pc = pcl.get(i);
                        g2.setColor(pc.bg);
                        g2.fillRect(0, 0, superFudge, cellHeight);
                        g2.setColor(pc.fg);
                    } else {
                        //g2.setColor(bgColor);
                        //g2.fillRect(0, 0, superFudge, cellHeight);
                        g2.setColor(fgColor);
                    }

                    if (GeminiTextPane.sheetImage != null && !override) {

                        String key = GeminiTextPane.getEmojiHex(emoji);

                        Point p = GeminiTextPane.emojiSheetMap.get(key);
                        Image icon = null;
                        int imgSize = fontSize;
                        if (p != null) {
                            icon = GeminiTextPane.extractSpriteImage(p.x, p.y, 64, imgSize, imgSize, fontSize);
                        } else {
                            int dashIdx = key.indexOf('-');
                            if (dashIdx != -1) {

                                p = GeminiTextPane.emojiSheetMap.get(key.substring(0, dashIdx));
                                if (p != null) {
                                    icon = GeminiTextPane.extractSpriteImage(p.x, p.y, 64, imgSize, imgSize, fontSize);
                                }
                            }
                        }
                        if (icon == null) {
                            char[] chars = Character.toChars(text.codePointAt(i));

                            int eci = emoji.getEndCharIndex();
                            int emojiSize = eci - emoji.getCharIndex();

                            // advance past emoji
                            i += (emojiSize - 1);

                            // check for variation selector
                            if (eci < text.length()) {
                                int nextCodePoint = text.codePointAt(eci);
                                if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                    i += Character.charCount(nextCodePoint); // usually 1
                                }
                            }

                            int x1 = (cellWidth * 2 - charWidth) / 2;
                            int y1 = (cellHeight - metrics.getHeight()) / 2 + metrics.getAscent();

                            g2.drawString(new String(chars), x1, y1);
                            g2.dispose();

                        } else {

                            int eci = emoji.getEndCharIndex();
                            int emojiSize = eci - emoji.getCharIndex();

                            // advance past emoji
                            i += (emojiSize - 1);

                            // check for variation selector
                            if (eci < text.length()) {
                                int nextCodePoint = text.codePointAt(eci);
                                if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                    i += Character.charCount(nextCodePoint); // usually 1
                                }
                            }
                            int x1 = (superFudge - charWidth) / 2;

                            if (charWidth > cellWidth) {
                                pad += cellWidth;
                            }
                            g2.drawImage(icon, x1, 0, null);
                            g2.dispose();

                            pad += cellWidth;
                            i--;
                        }
                    } else {

                        g2.setFont(emojiFont);
                        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        int x1 = (cellWidth - charWidth) / 2;
                        int y1 = (cellHeight - metrics.getHeight()) / 2 + metrics.getAscent();
                        String em = emoji.getEmoji().getEmoji();

                        g2.drawString(em, x1, y1);
                        g2.dispose();

                        int eci = emoji.getEndCharIndex();
                        int emojiSize = eci - emoji.getCharIndex();

                        //i += emojiSize;
                        i += (emojiSize - 1);

                        // check for variation selector
                        if (eci < text.length()) {
                            int nextCodePoint = text.codePointAt(eci);
                            if (nextCodePoint == 0xFE0E || nextCodePoint == 0xFE0F) {
                                i += Character.charCount(nextCodePoint); // usually 1
                            }
                        }

                        if (charWidth > cellWidth) {
                            pad += cellWidth;
                        }
                        pad += cellWidth;

                        i--;
                    }

                    i++;

                } else {
                    int charWidth = metrics.charWidth(c);
                    // +1 when width calculation right on the line (UK forecast site)
                    int superFudge = charWidth <= (cellWidth + 1) ? cellWidth : cellWidth * 2;

                    cellImage = new BufferedImage(superFudge, cellHeight, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cellImage.createGraphics();

                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    g2.setFont(font);

                    int x1 = (superFudge - charWidth) / 2;
                    int y1 = (cellHeight - metrics.getHeight()) / 2 + metrics.getAscent();

                    List<PositionColor> pcl = colorList.get(lineNum);
                    if (pcl != null) {

                        PositionColor pc = pcl.get(i);
                        g2.setColor(pc.bg);
                        g2.fillRect(0, 0, superFudge, cellHeight);
                        g2.setColor(pc.fg);
                        if (pc.bold) {
                            g2.setFont(font.deriveFont(Font.BOLD));
                        }
                    } else {
                        // g2.setColor(bgColor);

                        // g2.drawRect(0, 0, superFudge, cellHeight);
                        g2.setColor(fgColor);
                    }

                    Font saveFont = font;
                    if (!saveFont.canDisplay(line.codePointAt(i))) {
                        // on windows, source code pro might not support symbols - fallback in a safe way
                        saveFont = new Font("SansSerif", Font.PLAIN, fontSize);
                    }
                    if (font.canDisplay(c)) {

                        g2.drawString(String.valueOf(c), x1, y1);
                    } else if (saveFont.canDisplay(line.codePointAt(i))) {
                        g2.setFont(saveFont);
                        int cp = line.codePointAt(i);
                        g2.drawString(new String(Character.toChars(cp)), x1, y1);
                        i += Character.charCount(cp) - 1;
                        g2.setFont(font);
                    }
                    g2.dispose();
                    if (charWidth > (cellWidth + 1)) { // +1 when the width calculation is right on the line - uk forecast pages
                        pad += cellWidth;
                    }
                    pad += cellWidth;

                }

                g.drawImage(cellImage, x, y, null);
                x += pad;

            }
            y += baseY;
            lineNum++;
        }

        g.dispose();
        return image;
    }

    private static AnsiInfo handleAnsi(String line) {

        ParserFactoryConfig config = new ParserFactoryConfig();
        config.setEnvironment(Environment._8_BIT);
        config.setFunctionTypes(List.of(ControlFunctionType.C0_SET, ControlFunctionType.C1_SET));
        ParserFactoryService factoryService = new ParserFactoryProvider();
        ParserFactory factory = factoryService.createFactory(config);
        //}

        StringBuilder sb = new StringBuilder();
        List<PositionColor> pcList = new ArrayList<>();
        var parser = factory.createParser(line);
        Fragment fragment = null;
        int fromIdx = 0;
        while ((fragment = parser.parse()) != null) {
            if (fragment.getType() == FragmentType.TEXT) {

                TextFragment textFragment = (TextFragment) fragment;
                String ln = convert(textFragment.getText());

                sb.append(ln);
                int idx = sb.indexOf(ln, fromIdx);
                fromIdx = idx;

                PositionColor pc = new PositionColor(ansiBG, ansiFG, ansiBold);
                for (int i = 0; i < ln.length(); i++) {
                    pcList.add(pc);
                }
            }
        }

        return new AnsiInfo(sb.toString(), pcList);

    }

    private static record AnsiInfo(String line, List<PositionColor> pcList) {

    }

    private static record PositionColor(Color bg, Color fg, boolean bold) {

    }

    private static boolean foregroundHandling;

    private static String convert(String txt) {

        if (!txt.startsWith("[")) {
            if (foregroundHandling) {
                ansiFG = fgColor1;
            }
            return txt;

        } else {
            int mIdx = txt.indexOf('m');
            if (mIdx == -1) {
                if (foregroundHandling) {
                    ansiFG = fgColor1;
                }
                return txt;
            }
            String line = txt.substring(mIdx + 1);

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
                    case "39" ->
                        ansiFG(AnsiColor.WHITE); // "Default" fg color
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
                        // background color
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
                    case "49" ->
                        ansiBG(Color.BLACK); //"default" background
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
                            ansiFG = fgColor1;
                        } else {
                            foregroundHandling = true;

                        }
                        ansiBG = bgColor1;
                        ansiBold = false;

                    }
                    case "1" -> {
                        ansiBold = true;
                    }
                    case "2" -> {
                        // not really faint - could lighten or darken depending on theme but then would have to track for reset w/22
                        ansiBold = false;
                    }
                    case "22" -> { //normal intensity
                        ansiBold = false;
                    }

                }
            }
            if (foregroundHandling && !line.isBlank()) {
                ansiFG = fgColor1;
            }
            return line;

        }

    }

    private static void ansiFG(Color c) {
        ansiFG = AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d);
        foregroundHandling = false;
    }

    private static void ansiBG(Color c) {
        ansiBG = AnsiColor.adjustColor(c, isDark, .2d, .8d, .15d);
    }

}
