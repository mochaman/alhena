
package brad.grier.alhena;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class I18n {

    private static ResourceBundle bundle;
    private static Locale forcedLocale;

    static {
        loadBundle();
    }

    public static void setForcedLocale(Locale locale) {
        forcedLocale = locale;
        loadBundle();
    }

    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return "??" + key + "??";
        }
    }

    private static void loadBundle() {
        Locale locale = (forcedLocale != null) ? forcedLocale : Locale.getDefault();
        bundle = ResourceBundle.getBundle("MessagesBundle", locale);
    }
}