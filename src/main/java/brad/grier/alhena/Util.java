package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Constructor;
import java.net.FileNameMap;
import java.net.IDN;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BooleanSupplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSlider;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.CMSEnvelopedDataParser;
import org.bouncycastle.cms.CMSEnvelopedDataStreamGenerator;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.drjekyll.fontchooser.FontChooser;

import com.bric.colorpicker.ColorPicker;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;

import brad.grier.alhena.DB.ClientCertInfo;
import brad.grier.alhena.DB.PageStyleInfo;
import io.vertx.core.json.JsonObject;

/**
 * Static utility methods
 *
 * @author Brad Grier
 */
public class Util {

    public static void setupTheme(String themeClassName) {

        Class<?> themeClass;
        try {
            themeClass = Class.forName(themeClassName);
            FlatLaf theme = (FlatLaf) themeClass.getDeclaredConstructor().newInstance();
            FlatLaf.setup(theme);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public static void infoDialog(Component c, String title, String msg) {
        infoDialog(c, title, msg, JOptionPane.INFORMATION_MESSAGE);
    }

    public static void infoDialog(Component c, String title, String msg, int msgType) {

        JOptionPane optionPane = new JOptionPane(msg, msgType);
        JDialog dialog = optionPane.createDialog(c, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);

        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }
        dialog.setVisible(true);

    }

    private static void dressDialog(JDialog dialog) {
        if (SystemInfo.isMacFullWindowContentSupported) {
            dialog.getRootPane().putClientProperty("apple.awt.fullWindowContent", true);
            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
            Container content = dialog.getContentPane();
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0)); // 30px top
            wrapper.add(content, BorderLayout.CENTER);

            dialog.setContentPane(wrapper);
        } else {
            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }
    }

    public static void fancyInfoDialog(Component parent, String title, Object[] components) {

        JOptionPane optionPane = new JOptionPane(
                components, // Message
                JOptionPane.INFORMATION_MESSAGE, // Message type
                JOptionPane.DEFAULT_OPTION, // Option type
                null, // Icon (null for default)
                null, // Options (null for default buttons)
                null // Initial value
        );
        JDialog dialog = optionPane.createDialog(parent, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }

        dialog.setVisible(true);

    }

    public static String colorize(String s) {
        StringBuilder sb = new StringBuilder();
        Color c = UIManager.getColor("Component.linkColor");
        sb.append((char) 27).append("[38;2;").append(c.getRed()).append(";").append(c.getGreen()).append(";").append(c.getBlue()).append("m").append(s).append((char) 27).append('\n');
        return sb.toString();
    }

    public static boolean isLight(Color color) {

        double luminance = (0.299 * color.getRed()
                + 0.587 * color.getGreen()
                + 0.114 * color.getBlue()) / 255;
        return luminance > 0.5; // threshold, tweak if needed
    }

    public static void showAbout(Component c) {
        PreformattedTextPane ptp = new PreformattedTextPane(14, UIManager.getBoolean("laf.dark"), null);
        EventQueue.invokeLater(() -> ptp.setCaretPosition(0));
        ptp.addText(colorize(GeminiFrame.getArt()));

        Object[] comps = {Alhena.PROG_NAME + " " + Alhena.VERSION, "© 2025 Brad Grier", ptp};
        Util.fancyInfoDialog(c, I18n.t("aboutLabel"), comps);
    }

    public static Object confirmDialog(Component parent, String title, String question, int optionType, Object[] options, Integer msgType) {

        //JOptionPane optionPane = new JOptionPane(question, JOptionPane.QUESTION_MESSAGE, msgType);
        JOptionPane optionPane = new JOptionPane(
                question, // Message
                msgType == null ? JOptionPane.QUESTION_MESSAGE : msgType,
                // JOptionPane.QUESTION_MESSAGE, // Message type
                options != null ? JOptionPane.DEFAULT_OPTION : optionType,
                null, // Icon (null for default)
                options, // Options (null for default buttons)
                options != null ? options[0] : null // Initial value
        );
        JDialog dialog = optionPane.createDialog(parent, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }

        dialog.setVisible(true);

        return optionPane.getValue();

    }

    public static String inputDialog(Frame c, String title, String msg, boolean pswd) {
        return inputDialog(c, title, msg, pswd, null, null);
    }

    public static Object inputDialog2(Component c, String title, Object[] components, Object[] options, boolean resizable) {
        return inputDialog2(c, title, components, options, resizable, null);
    }

    public static Object inputDialog2(Component c, String title, Object[] components, Object[] options, boolean resizable, BooleanSupplier[] suppliers) {

        JOptionPane optionPane = new JOptionPane(
                components, // Message
                JOptionPane.QUESTION_MESSAGE, // Message type
                JOptionPane.OK_CANCEL_OPTION, // Option type
                null, // Icon (null for default)
                options, // Options (null for default buttons)
                options != null ? options[0] : null // Initial value
        );

        optionPane.setWantsInput(false);

        JDialog dialog = optionPane.createDialog(c, title);
        dialog.setResizable(resizable);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }

        dialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                focusText(dialog);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {

            }

        });
        if (suppliers != null) {
            for (int i = 0; i < suppliers.length; i++) {
                if (options[i] instanceof JButton button) {
                    int j = i; // annoying
                    button.addActionListener(al -> {
                        if (suppliers[j].getAsBoolean()) {
                            dialog.dispose();
                        }

                    });
                }
            }
        }
        dialog.setVisible(true);

        // return null for cancel as a shortcut
        if (options == null && optionPane.getValue() instanceof Integer val) {
            if (val == JOptionPane.CANCEL_OPTION || val == JOptionPane.CLOSED_OPTION) {
                return null;
            }
        }
        return optionPane.getValue();

    }

    private static void focusText(Container c) {
        for (Object obj : c.getComponents()) {
            if (obj instanceof JTextComponent jtf) {

                jtf.requestFocusInWindow();
                break;
            } else if (obj instanceof Container) {
                focusText((Container) obj);
            }
        }
    }

    public static String inputDialog(Frame c, String title, String msg, boolean pswd, String inputFieldText, JComponent addComp) {
        JTextArea textArea = new JTextArea(1, 35);

        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText(I18n.t("inputTip"));
        //Dimension ps = textArea.getPreferredSize();

        JTextComponent textField = pswd ? new JPasswordField() : textArea;
        textField.addMouseListener(new ContextMenuMouseListener());

        if (inputFieldText != null) {
            textField.setText(inputFieldText);
        }

        Object[] message = {
            msg, textField, addComp
        };
        JOptionPane optionPane = new JOptionPane(
                message, // Message
                JOptionPane.QUESTION_MESSAGE, // Message type
                JOptionPane.OK_CANCEL_OPTION, // Option type
                null, // Icon (null for default)
                null, // Options (null for default buttons)
                null // Initial value
        );

        optionPane.setWantsInput(false);

        JDialog dialog = optionPane.createDialog(c, title);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        textArea.getDocument().addDocumentListener(new DocumentListener() {

            private void updateSize() {
                EventQueue.invokeLater(() -> {
                    dialog.pack();
                });

            }

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateSize();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateSize();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateSize();
            }
        });

        KeyStroke shiftEnter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK);
        textArea.getInputMap().put(shiftEnter, "insert-newline");
        textArea.getActionMap().put("insert-newline", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                textArea.append("\n");
                dialog.pack();
            }
        });

        // Define Enter to trigger the OK button
        KeyStroke enter = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0);
        textArea.getInputMap().put(enter, "submit-dialog");
        textArea.getActionMap().put("submit-dialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                optionPane.setValue(JOptionPane.OK_OPTION);
                dialog.dispose();  // Close the dialog (same as pressing OK)

            }
        });

        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }

        dialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                textField.requestFocusInWindow();

            }

            @Override
            public void windowLostFocus(WindowEvent e) {

            }

        });
        dialog.setVisible(true);

        if (optionPane.getValue() instanceof Integer result) {
            if (result == JOptionPane.OK_OPTION) {
                return pswd ? new String(((JPasswordField) textField).getPassword()) : textField.getText();
            }
        }
        return null;

    }

    public static void loadFont(String path) {

        try (InputStream is = Util.class.getClassLoader().getResourceAsStream(path)) {
            Font customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(16f);

            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(customFont);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static ImageIcon createImageIcon(Object obj, String path,
            String description) {
        java.net.URL imgURL = obj.getClass().getResource(path);
        if (imgURL != null) {
            return new ImageIcon(imgURL, description);
        } else {
            return null;
        }
    }

    public static Font getFont(Component f, Font font, boolean showSlider, boolean showReset) {
        FontChooser fontChooser = new FontChooser(font);
        fontChooser.setPreferredSize(new Dimension(700, 350));
        Object[] message;
        JSlider slider = null;
        if (showSlider) {
            slider = new JSlider(JSlider.HORIZONTAL, 5, 35, GeminiFrame.monoFontSize);
            slider.setMajorTickSpacing(5);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);
            Object[] msg = {
                fontChooser,
                new JLabel(" "),
                new JLabel(I18n.t("fontDialogPFLabel")),
                slider
            };
            message = msg;
        } else {
            Object[] msg = {
                fontChooser
            };
            message = msg;
        }
        Object[] options;
        if (showReset) {
            Object[] opt = {I18n.t("okLabel"), I18n.t("cancelLabel"), I18n.t("resetLabel")};
            options = opt;
        } else {
            Object[] opt = {I18n.t("okLabel"), I18n.t("cancelLabel")};
            options = opt;
        }
        //Object[] options = {I18n.t("okLabel"), I18n.t("cancelLabel"), I18n.t("resetLabel")};
        JOptionPane optionPane = new JOptionPane(
                message, // Message
                JOptionPane.PLAIN_MESSAGE, // Message type
                JOptionPane.DEFAULT_OPTION, // Option type
                null, // Icon (null for default)
                options, // Options (null for default buttons)
                options[0] // Initial value
        );

        optionPane.setWantsInput(false);
        JDialog dialog = optionPane.createDialog(f, I18n.t("fontDialogTitle"));
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }
        dialog.setVisible(true);
        Object selectedValue = optionPane.getValue();
        if (selectedValue instanceof String val) {
            if (val.equals(I18n.t("okLabel"))) {
                if (slider != null) {
                    GeminiFrame.monoFontSize = slider.getValue();
                }
                return fontChooser.getSelectedFont();
            } else if (val.equals(I18n.t("resetLabel"))) {
                GeminiFrame.monoFontSize = GeminiFrame.DEFAULT_FONT_SIZE;
                return new Font("SansSerif", Font.PLAIN, GeminiFrame.DEFAULT_FONT_SIZE);
            }
        }
        return null;

    }

    public static Color getColor(Component f, Color color) {
        //FontChooser fontChooser = new FontChooser(font);
        ColorPicker cp = new ColorPicker(true, false);
        cp.setPreferredSize(new Dimension(700, 350));
        cp.setColor(color);
        Object[] msg = {
            cp
        };

        Object[] options;

        Object[] opt = {I18n.t("okLabel"), I18n.t("cancelLabel")};
        options = opt;

        //Object[] options = {I18n.t("okLabel"), I18n.t("cancelLabel"), I18n.t("resetLabel")};
        JOptionPane optionPane = new JOptionPane(
                msg, // Message
                JOptionPane.PLAIN_MESSAGE, // Message type
                JOptionPane.DEFAULT_OPTION, // Option type
                null, // Icon (null for default)
                options, // Options (null for default buttons)
                options[0] // Initial value
        );

        optionPane.setWantsInput(false);
        JDialog dialog = optionPane.createDialog(f, I18n.t("pageBgColorDialog"));
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }
        dialog.setVisible(true);
        Object selectedValue = optionPane.getValue();
        if (selectedValue instanceof String val) {
            if (val.equals(I18n.t("okLabel"))) {

                return cp.getColor();
            }
        }
        return null;

    }

    public static File getFile(GeminiFrame f, String fileName, boolean isOpenMode, String title, FileNameExtensionFilter filter) {
        JDialog dialog = new JDialog(f, title);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setModalityType(Dialog.ModalityType.DOCUMENT_MODAL);
        if (SystemInfo.isMacOS) {
            dressDialog(dialog);
        }
        JFileChooser fileChooser = new JFileChooser();
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setApproveButtonText(isOpenMode ? I18n.t("importFileDialog") : I18n.t("saveLabel"));

        if (fileName != null && !fileName.trim().isEmpty()) {

            fileChooser.setSelectedFile(new File(System.getProperty("user.home"), fileName));
        } else {
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        }

        dialog.setLayout(new BorderLayout());
        dialog.add(fileChooser, BorderLayout.CENTER);
        File[] selectedFile = {null};

        fileChooser.addActionListener(event -> {
            if (JFileChooser.APPROVE_SELECTION.equals(event.getActionCommand())) {
                File returnedFile = fileChooser.getSelectedFile();
                if (!isOpenMode && returnedFile.exists()) {
                    Object res = Util.confirmDialog(dialog, I18n.t("fileExistsDialog"), I18n.t("fileExistsDialogMsg"), JOptionPane.YES_NO_OPTION, null, null);
                    if (res instanceof Integer result) {
                        if (result == JOptionPane.YES_OPTION) {
                            selectedFile[0] = returnedFile;
                        }
                    }
                } else {
                    selectedFile[0] = returnedFile;
                }
            }
            dialog.dispose();
        });

        dialog.setSize(600, 400);
        dialog.setLocationRelativeTo(f);
        dialog.setVisible(true);

        return selectedFile[0];
    }

    public static BufferedImage getImage(byte[] imageBytes, int previewWidth, int previewHeight, BufferedImage bi, boolean allowScaleUp) {

        BufferedImage img = null;

        try {
            if (bi == null) {
                ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
                img = ImageIO.read(bis);
            } else {
                img = bi;
            }
            if (img == null) {
                return null;
            }
            if (!allowScaleUp && img.getWidth() < previewWidth) {
                previewWidth = img.getWidth();
            }

            double aspect = (double) img.getWidth() / (double) img.getHeight();

            int scaledWidth = 0;
            int scaledHeight = 0;

            scaledWidth = previewWidth;
            scaledHeight = (int) ((double) previewWidth / aspect);

            if (scaledHeight > previewHeight) {
                scaledHeight = previewHeight;
                scaledWidth = (int) ((double) previewHeight * aspect);
            }

            if (scaledHeight <= 0) {
                scaledHeight = 1;
            }
            if (scaledWidth <= 0) {
                scaledWidth = 1;
            }

            if (img.getWidth() > scaledWidth || img.getHeight() > scaledHeight) {
                img = getScaledInstance(img, scaledWidth, scaledHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true, false);
            } else {
                img = getScaledInstance(img, scaledWidth, scaledHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, false, false);
            }
        } catch (IOException ex) {
            ex.printStackTrace();

        }

        return img;

    }

    /**
     * Convenience method that returns a scaled instance of the provided
     * {@code BufferedImage}.
     *
     * @param img the original image to be scaled
     * @param targetWidth the desired width of the scaled instance, in pixels
     * @param targetHeight the desired height of the scaled instance, in pixels
     * @param hint one of the rendering hints that corresponds to
     * {@code RenderingHints.KEY_INTERPOLATION} (e.g. null null null null null
     * null null null null null null null null null null null null null null
     * null null null null null null null null null null null null null null
     * null {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling
     * technique that provides higher quality than the usual one-step technique
     * (only useful in downscaling cases, where {@code targetWidth} or
     * {@code targetHeight} is smaller than the original dimensions, and
     * generally only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    public static BufferedImage getScaledInstance(BufferedImage img,
            int targetWidth,
            int targetHeight,
            Object hint,
            boolean higherQuality,
            boolean forceARGB) {
        int type = (img.getTransparency() == Transparency.OPAQUE)
                ? forceARGB ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;

        BufferedImage ret = (BufferedImage) img;
        int w, h;
        if (higherQuality) {
            // Use multi-step technique: start with original size, then
            // scale down in multiple passes with drawImage()
            // until the target size is reached
            w = img.getWidth();
            h = img.getHeight();
        } else {
            // Use one-step technique: scale directly from original
            // size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        do {
            if (higherQuality && w > targetWidth) {
                w /= 2;
                if (w < targetWidth) {
                    w = targetWidth;
                }
            }

            if (higherQuality && h > targetHeight) {
                h /= 2;
                if (h < targetHeight) {
                    h = targetHeight;
                }
            }

            BufferedImage tmp = new BufferedImage(w, h, type);
            Graphics2D g2 = tmp.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();

            ret = tmp;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    public static String getHome() {

        String homePage = DB.getPref("home", null);
        if (homePage == null) {
            String homeDir = System.getProperty("alhena.home");
            File file = new File(homeDir + "/default.gmi");
            if (!file.exists()) {
                file = copyFromJar(homeDir);

            }
            if (file != null) {
                URI fileUri = file.toURI();
                return fileUri.toString();
            }

        }
        return homePage;
    }

    public static File copyFromJar(String homeDir) {
        String resourcePath = "default.gmi";
        Path outputPath = Paths.get(homeDir + "/default.gmi");

        try (InputStream inputStream = Alhena.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new FileNotFoundException("Resource not found: " + resourcePath);
            }

            Files.copy(inputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
            return outputPath.toFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String readResourceAsString(String resourcePath) {
        try (InputStream inputStream = Alhena.class.getResourceAsStream(resourcePath)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading resource: " + resourcePath, e);
        }
    }

    public static ImageIcon loadPNGIcon(String filePath, int width, int height) {

        try (InputStream is = GeminiTextPane.class.getResourceAsStream(filePath)) {
            if (is == null) {
                return null;
            }
            Image scaledImg = ImageIO.read(is).getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImg);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static BufferedImage loadImage(String filePath) {

        try (InputStream is = GeminiTextPane.class.getResourceAsStream(filePath)) {
            if (is == null) {
                return null;
            }
            return ImageIO.read(is);
            // Image scaledImg = ImageIO.read(is).getScaledInstance(width, height, Image.SCALE_SMOOTH);
            // return new ImageIcon(scaledImg);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void encryptFile(String inputFile, String outputFile, X509Certificate certificate) throws Exception {

        // Create the generator for encrypted data
        CMSEnvelopedDataStreamGenerator edGen = new CMSEnvelopedDataStreamGenerator();

        // Add the recipient's certificate
        edGen.addRecipientInfoGenerator(
                new JceKeyTransRecipientInfoGenerator(certificate)
                        .setProvider("BC")
        );

        // Open input and output streams
        try (InputStream in = Files.newInputStream(new File(inputFile).toPath()); OutputStream out = Files.newOutputStream(new File(outputFile).toPath()); // Create the encrypted output stream
                 OutputStream encryptedOut = edGen.open(
                        out,
                        new JceCMSContentEncryptorBuilder(CMSAlgorithm.AES256_CBC)
                                .setProvider("BC")
                                .build()
                )) {

            // Stream the data
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                encryptedOut.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void decryptFile(String inputFile, String outputFile, PrivateKey privateKey) throws Exception {
        // Set up the recipient
        JceKeyTransEnvelopedRecipient recipient = (JceKeyTransEnvelopedRecipient) new JceKeyTransEnvelopedRecipient(privateKey)
                .setProvider("BC");

        // Open streams
        try (InputStream in = Files.newInputStream(new File(inputFile).toPath()); OutputStream out = Files.newOutputStream(new File(outputFile).toPath())) {

            // Create the parser
            CMSEnvelopedDataParser parser = new CMSEnvelopedDataParser(in);

            // Get the recipient info
            RecipientInformation recipientInfo = parser.getRecipientInfos()
                    .getRecipients()
                    .iterator()
                    .next();

            // Get the decrypted content stream
            try (InputStream decryptedStream = recipientInfo.getContentStream(recipient).getContentStream()) {
                // Stream the data
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = decryptedStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public static String hashAndSign(File file, PrivateKey privateKey) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (InputStream fis = new FileInputStream(file); DigestInputStream dis = new DigestInputStream(fis, digest)) {
            byte[] buffer = new byte[8192];
            while (dis.read(buffer) != -1) {
                // just reading updates the digest
            }
        }

        byte[] hash = digest.digest(); // get the hash

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(hash);
        return Base64.getEncoder().encodeToString(signer.sign());

    }

    public static void importData(GeminiFrame frame, File f, boolean decrypt, boolean delete) {
        if (decrypt) {

            try {
                File file = File.createTempFile("alhenadb", ".zip");
                ClientCertInfo certInfo = DB.getClientCertInfo(GeminiFrame.SYNC_SERVER);
                PrivateKey pk = Alhena.loadPrivateKey(certInfo.privateKey());
                //X509Certificate cert = (X509Certificate) Alhena.loadCertificate(certInfo.cert());
                decryptFile(f.getAbsolutePath(), file.getAbsolutePath(), pk);
                File origFile = f;
                f = file;
                origFile.delete();
                f.deleteOnExit();
            } catch (Exception ex) {
                Util.infoDialog(frame, I18n.t("errorDecryptingDialog"), I18n.t("errorDecryptingDialogMsg"), JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
                return;
            }

        }
        Object[] options = {I18n.t("mergeLabel"), I18n.t("replaceLabel"), I18n.t("cancelLabel")};
        Object res = confirmDialog(frame, I18n.t("confirmReplaceDialog"), I18n.t("confirmReplaceDialogMsg"), JOptionPane.YES_NO_OPTION, options, JOptionPane.WARNING_MESSAGE);
        if (res instanceof String result) {

            if (result.equals(I18n.t("replaceLabel"))) {
                try {
                    String prevEmo = DB.getPref("emoji", null);

                    if (DB.restoreDB(f) != 0) {
                        infoDialog(frame, I18n.t("versionErrorDialog"), I18n.t("versionErrorDialogMsg"));
                    } else {
                        String postEmo = DB.getPref("emoji", null);
                        if ("facebook".equals(postEmo) || "apple".equals(postEmo) || "twitter".equals(postEmo)) {
                            File sheetFile = new File(System.getProperty("alhena.home") + File.separatorChar + "sheet_" + postEmo + "_64.png");
                            if (!sheetFile.exists()) {
                                // trying to change to an emoji set not installed
                                DB.insertPref("emoji", prevEmo);
                            } else {
                                frame.setEmoji(postEmo);
                            }

                        }
                        frame.resetFont();
                        infoDialog(frame, I18n.t("importSuccessDialog"), I18n.t("importSuccessDialogMsg"));
                        Alhena.clearCnList();
                        Alhena.updateFrames(true, true, true);
                    }
                } catch (Exception ex) {
                    infoDialog(frame, I18n.t("importErrorDialog"), I18n.t("importErrorDialogMsg"), JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            } else if (result.equals(I18n.t("mergeLabel"))) {
                try {
                    DBBackup.init();
                    if (DBBackup.mergeDB(f) != 0) {
                        infoDialog(frame, I18n.t("versionErrorDialog"), I18n.t("versionErrorDialogMsg"));
                    } else {
                        infoDialog(frame, I18n.t("mergeCompleteDialog"), I18n.t("mergeCompleteDialogMsg"));
                        Alhena.updateFrames(true, false, false);
                    }
                } catch (Exception ex) {
                    infoDialog(frame, I18n.t("mergeErrorDialog"), I18n.t("mergeErrorDialogMsg"), JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
        if (delete) {
            f.delete(); // delete immediately rather than defer
        }
    }

    public static ImageIcon recolorIcon(String imagePath, Color themeColor, int width, int height) {

        try (InputStream is = GeminiFrame.class.getResourceAsStream(imagePath)) {
            if (is == null) {
                return null;
            }

            BufferedImage image = ImageIO.read(is);

            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int pixel = image.getRGB(x, y);
                    Color color = new Color(pixel, true); // Preserve alpha

                    // If the pixel is black (or close to black), replace it with the theme color
                    if (color.getRed() == 0 && color.getGreen() == 0 && color.getBlue() == 0) {
                        int newColor = (color.getAlpha() << 24) | (themeColor.getRGB() & 0x00FFFFFF); // Preserve alpha
                        image.setRGB(x, y, newColor);
                    }
                }
            }
            Image scaledImage = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(scaledImage);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static boolean downloadFile(String urlString, File outputFile) {

        URL url;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            return false;
        }
        try (ReadableByteChannel rbc = Channels.newChannel(url.openStream()); FileOutputStream fos = new FileOutputStream(outputFile)) {
            fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        } catch (IOException io) {
            io.printStackTrace();
            return false;
        }

        return true;

    }

    public static String getMimeType(String path) {
        FileNameMap fileNameMap = URLConnection.getFileNameMap();
        String lcPath = path.toLowerCase();
        //logger.info("getting mime type for {}:", path);
        if (lcPath.endsWith(".gmi") || lcPath.endsWith(".gemini")) {
            return "text/gemini";
        }

        String ct = fileNameMap.getContentTypeFor(path);
        return ct == null ? "text/plain" : ct;
    }

    public static record PemData(String cert, String key) {

    }

    public static PemData extractPemParts(String pemFilePath) throws Exception {
        String pemContent = new String(Files.readAllBytes(Paths.get(pemFilePath)));
        String cert = null, key = null;

        // find certificate block
        if (pemContent.contains("-----BEGIN CERTIFICATE-----")) {
            cert = pemContent.substring(
                    pemContent.indexOf("-----BEGIN CERTIFICATE-----"),
                    pemContent.indexOf("-----END CERTIFICATE-----") + "-----END CERTIFICATE-----".length()
            ).trim();
        }

        String[] keyTypes = {
            "PRIVATE KEY", // PKCS#8
            "RSA PRIVATE KEY", // PKCS#1 RSA
            "EC PRIVATE KEY" // EC legacy
        };

        for (String type : keyTypes) {
            String begin = "-----BEGIN " + type + "-----";
            String end = "-----END " + type + "-----";
            int start = pemContent.indexOf(begin);
            int stop = pemContent.indexOf(end);
            if (start != -1 && stop != -1) {
                key = pemContent.substring(start, stop + end.length()).trim();
            }
        }

        return new PemData(cert, key);
    }

    public static boolean isFontAvailable(String fontName) {
        String[] fonts = GraphicsEnvironment
                .getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();

        for (String font : fonts) {
            if (font.equalsIgnoreCase(fontName)) {
                return true;
            }
        }
        return false;
    }

    // get filename - used by github to download from amazon bucket
    public static String extractFilenameFromUrl(String url) {
        Pattern outer = Pattern.compile("response-content-disposition=([^&]+)");
        Matcher outerMatcher = outer.matcher(url);

        if (outerMatcher.find()) {
            String encodedDisposition = outerMatcher.group(1);
            String disposition = URLDecoder.decode(encodedDisposition, StandardCharsets.UTF_8);

            Pattern inner = Pattern.compile("filename=([^;\\s]+)");
            Matcher innerMatcher = inner.matcher(disposition);
            if (innerMatcher.find()) {
                return innerMatcher.group(1);
            }
        }

        return null;
    }

    public static String uEncode(String s) {
        return URLEncoder.encode(s).replace("+", "%20");
    }

    public static String mapTheme(String t) {
        Map<String, String> themeAliases = Map.of(
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatGitHubIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTGitHubIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatLightOwlIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTLightOwlIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatAtomOneLightIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTAtomOneLightIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialLighterIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialLighterIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialOceanicIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialOceanicIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialPalenightIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMaterialPalenightIJTheme",
                "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMoonlightIJTheme", "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMTMoonlightIJTheme"
        );

        return themeAliases.getOrDefault(t, t);

    }

    public static boolean isPrintingAvailable() {
        PrintService[] printServices = PrintServiceLookup.lookupPrintServices(null, null);
        return printServices.length > 0;
    }

    public static int getPort(URI uri) {
        return uri.getPort() == -1 ? 1965 : uri.getPort();
    }

    // get size of the latest sprite sheet version - download new version if size mismatch
    public static int getSetSize(String setName) {
        return switch (setName) {
            case "apple" ->
                19999794;
            case "facebook" ->
                21171378;
            case "twitter" ->
                11283284;
            default ->
                -1;
        };
    }

    // uses processURL logic (minus the sanitize) to create a fully resolved url
    // returns the original link url if resolve fails
    public static String resolveURI(URI prevURI, String oUrl) {
        try {
            String url = oUrl;
            URI checkURI = new URI(url);
            String authority = checkURI.getAuthority();
            String host = authority != null ? authority.split(":")[0] : null;
            if (url.startsWith("//")) {
                url = prevURI.getScheme() + ":" + url;
            } else {

                if (host == null && !"file".equals(checkURI.getScheme())) {

                    if (checkURI.getScheme() == null) {
                        //URI.resolve is removing last segment of path when checkURI is only a query???
                        if (url.startsWith("?")) {
                            // do not preserve prevURI's query if there is one
                            url = prevURI.getScheme() + "://" + prevURI.getHost() + prevURI.getPath() + url;

                        } else {
                            url = prevURI.resolve(checkURI).toString();
                            if (url.endsWith("..")) {
                                url = url.substring(0, url.indexOf(".."));
                            }
                        }
                    } else {
                        //  corner case - no host but there's a scheme - is this legal?
                        // spartan://greatfractal.com/
                        url = prevURI.resolve(URI.create(checkURI.getPath())).toString();
                    }

                }
            }
            if (!url.contains(":/")) {
                return oUrl;
            } else {
                URI origURI = URI.create(url).normalize();
                String origURL = origURI.toString();
                if (origURI.getPath().isEmpty()) {

                    //origURL += "/"; // origURL keeps the emoji
                    url += "/";

                }
                if (origURL.startsWith("file:/")) {
                    return origURL;
                }
                String hostPart = url.split("://")[1].split("/")[0];
                for (char c : hostPart.toCharArray()) { // handle emoji
                    if (c > 127) {
                        String punycodeHost = IDN.toASCII(hostPart, IDN.ALLOW_UNASSIGNED);
                        url = url.replace(hostPart, punycodeHost);
                        break;
                    }
                }
                URI punyURI = URI.create(url).normalize();
                return punyURI.toString();
            }
        } catch (URISyntaxException ex) {
            return oUrl;
        }
    }

    public static void showStyleEditor(GeminiFrame gf, int styleId) {
        try {

            PageStyleInfo psi = DB.getStyle(styleId);
            String jstring = psi.style();
            PageTheme pt;
            PageTheme apt = new PageTheme();
            UIDefaults ui = null;
            boolean[] refreshPage = {false};
            if (jstring != null) {
                refreshPage[0] = true;
                boolean isDark = UIManager.getBoolean("laf.dark");
                if (psi.theme().startsWith("com")) {
                    ui = loadDefaults(psi.theme());
                    pt = GeminiTextPane.getDefaultTheme(ui);
                } else if (psi.theme().equals("LIGHT") && isDark) {
                    ui = loadDefaults("com.formdev.flatlaf.FlatLightLaf");
                    pt = GeminiTextPane.getDefaultTheme(ui);
                } else if (psi.theme().equals("DARK") && !isDark) {
                    ui = loadDefaults("com.formdev.flatlaf.FlatDarkLaf");
                    pt = GeminiTextPane.getDefaultTheme(ui);

                } else {
                    ui = UIManager.getDefaults();
                    pt = GeminiTextPane.getDefaultTheme(ui);
                }
                JsonObject apJo = new JsonObject(jstring);
                pt.fromJson(apJo); // merge in changes
                apt.fromJson(apJo);
            } else {
                pt = GeminiTextPane.getDefaultTheme(UIManager.getDefaults());
            }
            String styleName = "Scope: " + psi.scope() + ", Value: " + psi.scopeValue() + ", Theme: " + psi.theme();
            StylePicker sp = new StylePicker(pt, apt, ui, styleName);
            Object[] cmps = {sp};
            JButton okButton = new JButton(I18n.t("okLabel"));
            JButton delButton = new JButton(I18n.t("deleteLabel"));
            JButton cancelButton = new JButton(I18n.t("cancelLabel"));
            BooleanSupplier okRunnable = () -> {
                try {
                    DB.updateStyle(styleId, sp.getAlteredPageTheme().getJson());
                    Alhena.updateFrames(false, false, false);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                return true;

            };

            BooleanSupplier delRunnable = () -> {
                Object r = Util.confirmDialog(gf, I18n.t("styleDeleteDialog"), I18n.t("styleDeleteDialogTxt"), JOptionPane.YES_NO_OPTION, null, null);
                if (r instanceof Integer rs && rs == JOptionPane.YES_OPTION) {
                    try {
                        DB.deleteStyle(styleId);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    Alhena.updateFrames(false, false, false);
                    if (refreshPage[0]) {
                        EventQueue.invokeLater(() -> gf.refresh());

                    }
                    return true;
                }
                return false;
            };

            BooleanSupplier cancelRunnable = () -> {
                return true;
            };

            Object[] options = {okButton, delButton, cancelButton};
            BooleanSupplier[] suppliers = {okRunnable, delRunnable, cancelRunnable};
            Util.inputDialog2(gf, I18n.t("styleDialog"), cmps, options, false, suppliers);

        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    public static UIDefaults loadDefaults(String themeClassName) {

        try {
            Class<?> clazz = Class.forName(themeClassName);

            // // Ensure it extends FlatLaf
            // if (!FlatLaf.class.isAssignableFrom(clazz)) {
            //     throw new IllegalArgumentException(themeClassName + " is not a FlatLaf theme");
            // }
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            ctor.setAccessible(true);
            FlatLaf laf = (FlatLaf) ctor.newInstance();

            // Get the defaults WITHOUT installing the LAF
            return laf.getDefaults();
        } catch (Exception ex) {
            return UIManager.getDefaults();
        }
    }

    public static void newStyle(GeminiFrame gf) {
        boolean[] refresh = {gf.visiblePage().textPane.currentMode == GeminiTextPane.STYLE_MODE};
        String[] scopeItems = {I18n.t("scope1Label"), I18n.t("scope2Label"), I18n.t("scope3Label"), I18n.t("scope4Label")};
        JComboBox<String> scopeCombo = new JComboBox(scopeItems);
        scopeCombo.setEditable(false);

        String[] themeItems = {I18n.t("styleTheme1"), I18n.t("styleTheme2"), I18n.t("styleTheme3"), I18n.t("styleTheme4")};
        JComboBox<String> themeCombo = new JComboBox(themeItems);

        themeCombo.setEditable(false);
        Object[] comps = {new JLabel(I18n.t("scopeText")),
            new JLabel(" "), new JLabel(I18n.t("styleScopeLabel")), scopeCombo, new JLabel(I18n.t("styleThemeLabel")), themeCombo};
        Object[] opts = {new JButton(I18n.t("okLabel")), new JButton(I18n.t("currentLabel")), new JButton(I18n.t("closeLabel"))};

        BooleanSupplier currentR = () -> {
            Integer styleId = gf.visiblePage().textPane.styleId;
            if (styleId != null) {
                showStyleEditor(gf, styleId);
                return false;
            } else {
                infoDialog(gf, I18n.t("noStyleDialog"), I18n.t("noStyleText"));
                return false;
            }
        };
        BooleanSupplier okR = () -> {
            int idx = scopeCombo.getSelectedIndex();
            String scope = null, scopeValue = null, th = null;
            switch (idx) {
                case 0 ->
                    scope = scopeValue = "GLOBAL";
                case 1 -> {
                    scope = "SCHEME";
                    scopeValue = gf.visiblePage().textPane.getURI().getScheme();
                    if (scopeValue == null) { // when saving style for certs or bookmarks but user picked domain
                        scope = "URL";
                        scopeValue = gf.visiblePage().textPane.getDocURLString();
                    }
                }
                case 2 -> {
                    scope = "DOMAIN";
                    scopeValue = gf.visiblePage().textPane.getURI().getAuthority();
                    if (scopeValue == null) { // when saving style for certs or bookmarks but user picked domain
                        scope = "URL";
                        scopeValue = gf.visiblePage().textPane.getDocURLString();
                    }

                }
                case 3 -> {
                    scope = "URL";
                    scopeValue = gf.visiblePage().textPane.getDocURLString();
                }
                default -> {
                }
            }
            idx = themeCombo.getSelectedIndex();
            switch (idx) {
                case 0 ->
                    th = "ALL";
                case 1 ->
                    th = "LIGHT";
                case 2 ->
                    th = "DARK";
                case 3 ->
                    th = Alhena.theme;
            }

            try {
                //String styleName = "[" + scope + "][" + scopeValue + "][" + th + "]";
                String styleName = "Scope: " + scope + ", Value: " + scopeValue + ", Theme: " + th;
                String jstring = DB.getStyle(scope, scopeValue, th);
                PageTheme pt;
                PageTheme apt = new PageTheme();
                UIDefaults ui = UIManager.getDefaults();
                if (jstring != null) {
                    pt = GeminiTextPane.getDefaultTheme(ui);
                    JsonObject apJo = new JsonObject(jstring);
                    pt.fromJson(apJo); // merge in changes
                    apt.fromJson(apJo);
                } else {
                    pt = GeminiTextPane.getDefaultTheme(ui);
                }
                StylePicker sp = new StylePicker(pt, apt, ui, styleName);
                Object[] cmps = {sp};
                JButton okButton = new JButton(I18n.t("okLabel"));
                JButton delButton = new JButton(I18n.t("deleteLabel"));
                JButton cancelButton = new JButton(I18n.t("cancelLabel"));

                String fScope = scope;
                String fScopeVal = scopeValue;
                String fTheme = th;
                BooleanSupplier okRunnable = () -> {
                    try {
                        DB.insertStyle(fScope, fScopeVal, fTheme, sp.getAlteredPageTheme().getJson(), null);
                        Alhena.updateFrames(false, false, false);
                        if (refresh[0]) {
                            EventQueue.invokeLater(() -> gf.refresh());
                        }
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    return true;

                };

                BooleanSupplier delRunnable = () -> {
                    Object r = confirmDialog(gf, I18n.t("styleDeleteDialog"), I18n.t("styleDeleteDialogTxt"), JOptionPane.YES_NO_OPTION, null, null);
                    if (r instanceof Integer rs && rs == JOptionPane.YES_OPTION) {
                        try {
                            DB.deleteStyle(fScope, fScopeVal, fTheme);
                            Alhena.updateFrames(false, false, false);
                            if (refresh[0]) {
                                EventQueue.invokeLater(() -> gf.refresh());
                            }
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                        }
                        return true;
                    }
                    return false;
                };

                Object[] options = {okButton, delButton, cancelButton};
                BooleanSupplier[] suppliers = {okRunnable, delRunnable, () -> true};

                inputDialog2(gf, I18n.t("styleDialog"), cmps, options, false, suppliers);

            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false;

        };
        BooleanSupplier[] supps = {okR, currentR, () -> true};
        inputDialog2(gf, I18n.t("stylesItem"), comps, opts, false, supps);
    }

    public static String formatElapsed(long millis) {
        long totalSeconds = millis / 1000;
        long seconds = totalSeconds % 60;
        long minutes = (totalSeconds / 60) % 60;
        long hours = totalSeconds / 3600;

        if (hours > 0) {
            return String.format("%d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    public static String bytesDecimal(long bytes) {
        if (bytes < 1000) {
            return bytes + " B";
        }

        final String[] units = {"kB", "MB", "GB", "TB", "PB", "EB"};
        double value = bytes;
        int unitIndex = -1;

        while (value >= 1000 && unitIndex < units.length - 1) {
            value /= 1000;
            unitIndex++;
        }

        return String.format("%.2f %s", value, units[unitIndex]);
    }
}
