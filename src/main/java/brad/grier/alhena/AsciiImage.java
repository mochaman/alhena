package brad.grier.alhena;

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
import java.util.Set;

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
    // special handling for specific chars not in font set
    private static final Set<Character> PAD_EMOJI_SET = Set.of('⚡');

    // for future reference, this class is not thread safe - only call on EDT
    public static BufferedImage renderTextToImage(String text, String fontName, int fontSize, Color bgColor, Color fgColor, boolean override) {
        boolean hasAnsi = false;
        ansiBold = false;
        bgColor = GeminiTextPane.shadePF ? AnsiColor.adjustColor(bgColor, UIManager.getBoolean("laf.dark"), .2d, .8d, .05d) : bgColor;
        ansiBG = bgColor;
        ansiFG = fgColor;
        isDark = UIManager.getBoolean("laf.dark");
        bgColor1 = bgColor;
        fgColor1 = fgColor;
        Font font = new Font(fontName, Font.PLAIN, fontSize);
        String[] lines = text.split("\n");
        int max_chars = 0;
        int idx = 0;
        List<List<PositionColor>> colorList = new ArrayList<>();
        for (String line : lines) {
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

            if (line1.length() > max_chars) {
                max_chars = line1.length();
            }
            lines[idx++] = line1;

        }

        FontMetrics metrics = new Canvas().getFontMetrics(font);
        int lineHeight = metrics.getHeight();
        int width = 0;
        for (String line : lines) {
            int w = metrics.stringWidth(line);
            width = Math.max(width, w);
        }
        int height = lineHeight * lines.length;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setFont(font);

        g.setColor(bgColor);
        g.fillRect(0, 0, width, height);
        g.setColor(fgColor);

        int CELL_HEIGHT = height / lines.length;
        int CELL_WIDTH = width / max_chars;
        String emojiProportional = "Noto Emoji";

        if(!override){
            //emojiProportional = "Noto Emoji";
            if (SystemInfo.isMacOS) {
                boolean macUseNoto = DB.getPref("macusenoto", "false").equals("true");
                if (!macUseNoto) {
                    emojiProportional = "SansSerif";
                }
            }
        }
        Font emojiFont = new Font(emojiProportional, Font.PLAIN, fontSize);
        int baseY = lineHeight;
        int y = 0;
        int lineNum = 0;
        for (String line : lines) {

            List<IndexedEmoji> emojis = EmojiManager.extractEmojisInOrderWithIndex(line);
            IndexedEmoji emoji;
            int pad = 0;

            for (int i = 0; i < line.length(); i++) {
                char c = line.charAt(i);

                int x = i * CELL_WIDTH + pad;
                BufferedImage cellImage;
                int charWidth = metrics.charWidth(c);

                if (charWidth > CELL_WIDTH) { // this test fails on windows (or probably when not in font) for '⚡' (and likely others)

                    cellImage = new BufferedImage(CELL_WIDTH * 2, CELL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cellImage.createGraphics();

                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    g2.setFont(font);

                    int x1 = (CELL_WIDTH * 2 - charWidth) / 2;
                    int y1 = (CELL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

                    List<PositionColor> pcl = colorList.get(lineNum);
                    if (pcl != null) {
                        PositionColor pc = pcl.get(i);
                        g2.setColor(pc.bg);
                        g2.fillRect(0, 0, CELL_WIDTH * 2, CELL_HEIGHT);
                        g2.setColor(pc.fg);
                    } else {
                        g2.setColor(bgColor);
                        g2.fillRect(0, 0, CELL_WIDTH * 2, CELL_HEIGHT);

                        g2.setColor(fgColor);
                    }
                    g2.drawString(String.valueOf(c), x1, y1);

                    g2.dispose();

                    pad += CELL_WIDTH;

                } else if ((emoji = GeminiTextPane.isEmoji(emojis, i)) != null) {

                    cellImage = new BufferedImage(CELL_WIDTH * 2, CELL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cellImage.createGraphics();
                    List<PositionColor> pcl = colorList.get(lineNum);
                    if (pcl != null) {
                        PositionColor pc = pcl.get(i);
                        g2.setColor(pc.bg);
                        g2.fillRect(0, 0, CELL_WIDTH * 2, CELL_HEIGHT);
                        g2.setColor(pc.fg);
                    } else {
                        g2.setColor(bgColor);
                        g2.fillRect(0, 0, CELL_WIDTH * 2, CELL_HEIGHT);
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

                            i = emoji.getEndCharIndex() + 1;
                            int x1 = (CELL_WIDTH * 2 - charWidth) / 2;
                            int y1 = (CELL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

                            g2.drawString(new String(chars), x1, y1);
                            g2.dispose();

                        } else {

                            // single char emoji followed by unneccessary variation selector
                            // example: snowman
                            if (i == emoji.getEndCharIndex() - 1) {

                                i++;
                            } else {

                                i = emoji.getEndCharIndex() - 1;

                            }
                            int x1 = (CELL_WIDTH - charWidth) / 2;

                            g2.drawImage(icon, x1, 0, null);
                            g2.dispose();
                            if (PAD_EMOJI_SET.contains(c)) { // windows or where font doesn't support
                                pad += CELL_WIDTH;
                            }
                            i--;
                        }
                    } else {

                        g2.setFont(emojiFont);

                        int x1 = (CELL_WIDTH - charWidth) / 2;
                        int y1 = (CELL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();
                        char[] chars = Character.toChars(line.codePointAt(i));
                        g2.drawString(new String(chars), x1, y1);
                        g2.dispose();
                        if (PAD_EMOJI_SET.contains(c)) { // windows or where font doesn't support
                            pad += CELL_WIDTH;
                        }
                    }
                    i++;

                } else {

                    cellImage = new BufferedImage(CELL_WIDTH, CELL_HEIGHT, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D g2 = cellImage.createGraphics();

                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                    g2.setFont(font);

                    int x1 = (CELL_WIDTH - charWidth) / 2;
                    int y1 = (CELL_HEIGHT - metrics.getHeight()) / 2 + metrics.getAscent();

                    List<PositionColor> pcl = colorList.get(lineNum);
                    if (pcl != null) {

                        PositionColor pc = pcl.get(i);
                        g2.setColor(pc.bg);
                        g2.fillRect(0, 0, CELL_WIDTH, CELL_HEIGHT);
                        g2.setColor(pc.fg);
                        if (pc.bold) {
                            g2.setFont(font.deriveFont(Font.BOLD));
                        }
                    } else {
                        g2.setColor(bgColor);

                        g2.drawRect(0, 0, CELL_WIDTH, CELL_HEIGHT);
                        g2.setColor(fgColor);
                    }
                    g2.drawString(String.valueOf(c), x1, y1);
                    g2.dispose();
                }

                g.drawImage(cellImage, x, y, null);

            }
            y += baseY;
            lineNum++;
        }

        g.dispose();
        return image;
    }

    private static AnsiInfo handleAnsi(String line) {
        //  ParserFactory factory;
        //if (factory == null) {
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
                System.out.println("here");
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
                            //StyleConstants.setForeground(bStyle, getForeground());
                            ansiFG = fgColor1;
                        } else {
                            foregroundHandling = true;

                        }
                        ansiBG = bgColor1;
                        ansiBold = false;
                        //StyleConstants.setBackground(bStyle, getBackground());
                        //StyleConstants.setBold(bStyle, false);

                    }
                    case "1" -> {
                        ansiBold = true;
                        //StyleConstants.setBold(bStyle, true);
                    }
                    // default ->
                    //     System.out.println("unknown: " + txt);
                }
            }
            if (foregroundHandling && !line.isBlank()) {
                //StyleConstants.setForeground(bStyle, getForeground());
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
