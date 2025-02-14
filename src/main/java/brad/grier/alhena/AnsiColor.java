package brad.grier.alhena;

import java.awt.Color;

/**
 * Ansi color support
 *
 * @author Brad Grier
 */
public class AnsiColor {

    public static final Color BLACK = Color.BLACK;
    public static final Color RED = new Color(205, 0, 0);
    public static final Color GREEN = new Color(0, 205, 0);
    public static final Color YELLOW = new Color(205, 205, 0);
    public static final Color BLUE = new Color(0, 0, 238);
    public static final Color MAGENTA = new Color(205, 0, 205);
    public static final Color CYAN = new Color(0, 205, 205);
    public static final Color WHITE = new Color(229, 229, 229);

    public static final Color BRIGHT_BLACK = new Color(127, 127, 127);
    public static final Color BRIGHT_RED = new Color(255, 0, 0);
    public static final Color BRIGHT_GREEN = new Color(0, 255, 0);
    public static final Color BRIGHT_YELLOW = new Color(255, 255, 0);
    public static final Color BRIGHT_BLUE = new Color(92, 92, 255);
    public static final Color BRIGHT_MAGENTA = new Color(255, 0, 255);
    public static final Color BRIGHT_CYAN = new Color(0, 255, 255);
    public static final Color BRIGHT_WHITE = new Color(255, 255, 255);

    public static Color ansiToColor(int ansiCode) {
        if (ansiCode >= 0 && ansiCode <= 15) {
            // basic
            return basicAnsiToColor(ansiCode);
        } else if (ansiCode >= 16 && ansiCode <= 231) {
            // rgb
            return rgbAnsiToColor(ansiCode);
        } else if (ansiCode >= 232 && ansiCode <= 255) {
            // grayscale
            return grayscaleAnsiToColor(ansiCode);
        } else {
            throw new IllegalArgumentException("Invalid ANSI color code: " + ansiCode);
        }
    }

    private static Color basicAnsiToColor(int code) {
        switch (code) {
            case 0 -> {
                return BLACK;   // Black
            }
            case 1 -> {
                return RED; // Red
            }
            case 2 -> {
                return GREEN; // Green
            }
            case 3 -> {
                return YELLOW; // Yellow
            }
            case 4 -> {
                return BLUE; // Blue
            }
            case 5 -> {
                return MAGENTA; // Magenta
            }
            case 6 -> {
                return CYAN; // Cyan
            }
            case 7 -> {
                return WHITE; // White
            }
            case 8 -> {
                return BRIGHT_BLACK; // Gray
            }
            case 9 -> {
                return BRIGHT_RED;   // Light Red
            }
            case 10 -> {
                return BRIGHT_GREEN;   // Light Green
            }
            case 11 -> {
                return BRIGHT_YELLOW; // Light Yellow
            }
            case 12 -> {
                return BRIGHT_BLUE; // Light Blue
            }
            case 13 -> {
                return BRIGHT_MAGENTA; // Light Magenta
            }
            case 14 -> {
                return BRIGHT_CYAN; // Light Cyan
            }
            case 15 -> {
                return BRIGHT_WHITE; // Light White
            }
            default -> throw new IllegalArgumentException("Unexpected value: " + code);
        }
    }

    private static Color rgbAnsiToColor(int code) {
        // 6x6x6 RGB color cube: values range from 16 to 231
        int r = ((code - 16) / 36) * 51;
        int g = ((code - 16) % 36) / 6 * 51;
        int b = ((code - 16) % 6) * 51;
        return new Color(r, g, b);
    }

    private static Color grayscaleAnsiToColor(int code) {
        // Grayscale colors range from 232 to 255
        int gray = (code - 232) * 10 + 8; // from 8 to 255
        return new Color(gray, gray, gray);
    }

    public static Color adjustColor(Color color, boolean isDarkTheme) {

        double luminance = getLuminance(color);

        double minLuminance = isDarkTheme ? 0.2 : 0.2; // Thresholds for adjustment
        double maxLuminance = isDarkTheme ? 0.8 : 0.8; // was 0.9

        if (luminance < minLuminance) {
            // too dark on dark theme
            return blend(color, Color.WHITE, 0.15);
            //return color.brighter();
        } else if (luminance > maxLuminance) {
            // too light on light theme
            return blend(color, Color.BLACK, 0.15);
            //return color.darker();
        }
        return color; // no change
    }

    private static double getLuminance(Color color) {
        return (0.299 * color.getRed() + 0.587 * color.getGreen() + 0.114 * color.getBlue()) / 255.0;
    }

    public static Color blend(Color c1, Color c2, double ratio) {
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        return new Color(r, g, b);
    }

}
