package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Component;
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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.SystemInfo;

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

        JOptionPane optionPane = new JOptionPane(msg, JOptionPane.INFORMATION_MESSAGE);
        JDialog dialog = optionPane.createDialog(c, title);

        // Enable the transparent title bar on the dialog
        if (SystemInfo.isMacOS) {
            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }
        // Display the dialog
        dialog.setVisible(true);

    }

    public static Object confirmDialog(Component parent, String title, String question, int msgType) {

        JOptionPane optionPane = new JOptionPane(question, JOptionPane.QUESTION_MESSAGE, msgType);
        JDialog dialog = optionPane.createDialog(parent, title);

        if (SystemInfo.isMacOS) {

            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);
        }

        dialog.setVisible(true);

        return optionPane.getValue();

    }

    public static String inputDialog(Frame c, String title, String msg, boolean pswd) {
        return inputDialog(c, title, msg, pswd, null);
    }

    public static String inputDialog2(Component c, String title, Object[] components) {

        JOptionPane optionPane = new JOptionPane(
                components, // Message
                JOptionPane.QUESTION_MESSAGE, // Message type
                JOptionPane.OK_CANCEL_OPTION, // Option type
                null, // Icon (null for default)
                null, // Options (null for default buttons)
                null // Initial value
        );

        optionPane.setWantsInput(false);

        JDialog dialog = optionPane.createDialog(c, title);

        if (SystemInfo.isMacOS) {

            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);

        }
        dialog.addWindowFocusListener(new WindowFocusListener() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                for (Object obj : components) {
                    if (obj instanceof JTextField jtf) {
                        jtf.requestFocusInWindow();
                        break;
                    }
                }
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                
            }

        });
        dialog.setVisible(true);

        if (optionPane.getValue() instanceof Integer result) {
            if (result == JOptionPane.OK_OPTION) {

                return "Ok";
            }
        }
        return null;

    }


    public static String inputDialog(Frame c, String title, String msg, boolean pswd, String inputFieldText) {
        JTextArea textArea = new JTextArea(1, 35);

        textArea.setLineWrap(true); 
        textArea.setWrapStyleWord(true);
        textArea.setToolTipText("shift+return for line break");
        Dimension ps = textArea.getPreferredSize();

        JTextComponent textField = pswd ? new JPasswordField() : textArea;
        textField.addMouseListener(new ContextMenuMouseListener());

        if (inputFieldText != null) {
            textField.setText(inputFieldText);
        }

        Object[] message = {
            msg, textField
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

        textArea.getDocument().addDocumentListener(new DocumentListener() {

            private void updateSize() {
                EventQueue.invokeLater(()->{
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

            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);

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
            System.out.println(customFont.getFamily());
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

    public static File getFile(GeminiFrame f, String fileName, boolean isOpenMode, String title, FileNameExtensionFilter filter) {
        JDialog dialog = new JDialog(f, title, true);
        if (SystemInfo.isMacOS) {

            dialog.getRootPane().putClientProperty("apple.awt.transparentTitleBar", true);
            //dialog.getRootPane().putClientProperty("apple.awt.windowTitleVisible", false);

        }
        JFileChooser fileChooser = new JFileChooser();
        if (filter != null) {
            fileChooser.setFileFilter(filter);
        }
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setApproveButtonText(isOpenMode ? "Open" : "Save");

        if (fileName != null) {

            fileChooser.setSelectedFile(new File(System.getProperty("user.home") + "/downloads", fileName));
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
                    Object res = Util.confirmDialog(dialog, "Exists", "File already exists. Continue?", JOptionPane.YES_NO_OPTION);
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


    public static BufferedImage getImage(byte[] imageBytes, int previewWidth, int previewHeight) {

        BufferedImage img = null;

        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            img = ImageIO.read(bis);
            if (img.getWidth() < previewWidth) {
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
        
        String homePage = DB.getPref("home");
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

        try (InputStream inputStream = GeminiClient.class.getClassLoader().getResourceAsStream(resourcePath)) {
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

}
