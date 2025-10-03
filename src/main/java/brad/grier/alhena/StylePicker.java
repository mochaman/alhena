package brad.grier.alhena;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.font.TextAttribute;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.UIDefaults;

import io.vertx.core.json.JsonObject;

public class StylePicker extends JPanel {

    private JLabel h3Label, h2Label, h1Label, linkLabel, quoteLabel, textLabel, hoverLabel, monoFontLabel, visitedLabel, listLabel;
    private JPanel textPanel;

    private String selectedLine = "#";
    private PageTheme alteredPageTheme;
    private JCheckBoxMenuItem applyAllCB;
    private PageTheme saveAlteredPageTheme;
    private PageTheme pageTheme;

    public StylePicker(PageTheme pTheme, PageTheme apTheme, UIDefaults ui) {

        super(new BorderLayout(0, 0));
        alteredPageTheme = apTheme;
        pageTheme = pTheme;
        // pageThemeBackup = new PageTheme();
        // pageThemeBackup.fromJson(new JsonObject(pTheme.getJson()));
        List<JComponent> mItems = new ArrayList<>();
        JMenuItem pageColorItem = new JMenuItem(I18n.t("pageBackgroundItem"));
        pageColorItem.addActionListener(al -> {

            Color chosenColor = Util.getColor(StylePicker.this, pageTheme.getPageBackground());
            if (chosenColor != null) {
                textPanel.setBackground(chosenColor);
                pageTheme.setPageBackground(chosenColor);
                alteredPageTheme.setPageBackground(chosenColor);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.setPageBackground(chosenColor);
                }
            }
        });
        JMenuItem resetPageBGItem = new JMenuItem(I18n.t("backgroundResetLabel"));
        resetPageBGItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            textPanel.setBackground(defaultTheme.getPageBackground());
            pageTheme.setPageBackground(defaultTheme.getPageBackground());
            alteredPageTheme.clearPageBackground();
            if (saveAlteredPageTheme != null) {
                saveAlteredPageTheme.clearPageBackground();
            }

        });


        JMenuItem spinnerColorItem = new JMenuItem("Spinner Color");
        spinnerColorItem.addActionListener(al -> {

            Color chosenColor = Util.getColor(StylePicker.this, pageTheme.getSpinnerColor());
            if (chosenColor != null) {
                //textPanel.setBackground(chosenColor);
                pageTheme.setSpinnerColor(chosenColor);
                alteredPageTheme.setSpinnerColor(chosenColor);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.setSpinnerColor(chosenColor);
                }
            }
        });
        JMenuItem spinnerResetItem = new JMenuItem("Reset Spinner Color");
        spinnerResetItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            //textPanel.setBackground(defaultTheme.getPageBackground());
            pageTheme.setSpinnerColor(defaultTheme.getSpinnerColor());
            alteredPageTheme.clearSpinnerColor();
            if (saveAlteredPageTheme != null) {
                saveAlteredPageTheme.clearSpinnerColor();
            }

        });

        applyAllCB = new JCheckBoxMenuItem(I18n.t("applyAllCBItem"));
        applyAllCB.addItemListener(il -> {
            if (il.getStateChange() == ItemEvent.SELECTED) {
                Util.infoDialog(StylePicker.this, I18n.t("attrDialog"), I18n.t("attrDialogTxt"));
                saveAlteredPageTheme = new PageTheme();
                saveAlteredPageTheme.fromJson(new JsonObject(alteredPageTheme.getJson()));
                alteredPageTheme = pageTheme;
            } else {
                Util.infoDialog(StylePicker.this, I18n.t("attrDialog"), I18n.t("changedAttrDialogTxt"));
                alteredPageTheme = saveAlteredPageTheme;

            }
        });

        JMenuItem resetItem = new JMenuItem(I18n.t("resetAttrItem"));

        resetItem.addActionListener(al -> {
            pageTheme = GeminiTextPane.getDefaultTheme(ui);
            alteredPageTheme = new PageTheme();
            saveAlteredPageTheme = null;
            setPanelAttributes();
            Util.infoDialog(StylePicker.this, I18n.t("resetAttrDialog"), I18n.t("resetAttrDialogTxt"));

        });

        JMenuItem widthItem = new JMenuItem(I18n.t("styleWidthDialog"));
        widthItem.addActionListener(al ->{

            JSlider slider = new JSlider(50, 100, (int)(pageTheme.getContentPercentage() * 100));
            slider.setMajorTickSpacing(10);
            slider.setMinorTickSpacing(5);
            slider.setPaintTicks(true);
            slider.setPaintLabels(true);

            slider.setPreferredSize(new Dimension(500, slider.getPreferredSize().height));
            Object[] comps = {new JLabel(I18n.t("contentWidthLabel") + " " + I18n.t("styleWidthLabel")), slider};
            Object res = Util.inputDialog2(StylePicker.this, "Content Width", comps, null, false);
            if (res != null) {
                float p = (float) slider.getValue() / 100f;
                pageTheme.setContentPercentage(p);
                alteredPageTheme.setContentPercentage(p);
                if (saveAlteredPageTheme != null) { 
                    saveAlteredPageTheme.setContentPercentage(p);
                }
            }
        });
        JMenuItem resetWidthItem = new JMenuItem(I18n.t("widthResetDialog"));
        resetWidthItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            //textPanel.setBackground(defaultTheme.getPageBackground());
            pageTheme.setContentPercentage(defaultTheme.getContentPercentage());
            alteredPageTheme.clearContentWidth();
            if (saveAlteredPageTheme != null) {
                saveAlteredPageTheme.clearContentWidth();
            }

        });
        Supplier<List<JComponent>> supplier = () -> {

            if (mItems.isEmpty()) {
                mItems.add(pageColorItem);
                mItems.add(resetPageBGItem);
                mItems.add(spinnerColorItem);
                mItems.add(spinnerResetItem);
                mItems.add(widthItem);
                mItems.add(resetWidthItem);
                mItems.add(applyAllCB);
                mItems.add(resetItem);
            }

            return mItems;
        };

        JPanel tb = new JPanel(new BorderLayout(0, 0));
        PopupMenuButton pmb = new PopupMenuButton("⚙️", supplier, "");
        pmb.setFont(new Font("Noto Emoji Regular", Font.PLAIN, 18));
        tb.add(new JLabel(I18n.t("styleEditorTxt")), BorderLayout.WEST);
        tb.add(pmb, BorderLayout.EAST);
        add(tb, BorderLayout.NORTH);

        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));

        contentPanel.add(Box.createVerticalStrut(5));
        String[] items = {"#", "##", "###", "=>", "=> Hover", "=> Visited", "*", ">", "Text", "PF Text"};
        JPanel linePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        linePanel.add(new JLabel(I18n.t("lineLabel")));
        JComboBox<String> lineCombo = new JComboBox<>(items);
        lineCombo.addActionListener(al -> {
            selectedLine = (String) lineCombo.getSelectedItem();
        });

        lineCombo.setEditable(false);
        linePanel.add(lineCombo);
        contentPanel.add(linePanel);
        contentPanel.add(Box.createVerticalStrut(5));
        //saveFont = new Font(pageTheme.getHeader3FontFamily(), Font.PLAIN, pageTheme.getHeader3Size());
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        JButton fontButton = new JButton("Font");
        fontButton.addActionListener(al -> {
            Font f = switch (selectedLine) {
                case "#" ->
                    new Font(pageTheme.getHeader3FontFamily(),pageTheme.getHeader3Style(), pageTheme.getHeader3Size());
                case "##" ->
                    new Font(pageTheme.getHeader2FontFamily(), pageTheme.getHeader2Style(), pageTheme.getHeader2Size());
                case "###" ->
                    new Font(pageTheme.getHeader1FontFamily(), pageTheme.getHeader1Style(), pageTheme.getHeader1Size());
                case "=>" ->
                    new Font(pageTheme.getLinkFontFamily(), pageTheme.getLinkStyle(), pageTheme.getLinkSize());
                case "=> Hover" ->
                    new Font(pageTheme.getLinkFontFamily(), pageTheme.getLinkStyle(), pageTheme.getLinkSize());
                case "=> Visited" ->
                    new Font(pageTheme.getLinkFontFamily(), pageTheme.getLinkStyle(), pageTheme.getLinkSize());
                case ">" ->
                    new Font(pageTheme.getQuoteFontFamily(), pageTheme.getQuoteStyle(), pageTheme.getQuoteSize());
                case "Text" ->
                    new Font(pageTheme.getFontFamily(), pageTheme.getFontStyle(), pageTheme.getFontSize());
                case "PF Text" ->
                    new Font(pageTheme.getMonoFontFamily(), Font.PLAIN, pageTheme.getMonoFontSize());
                case "*" ->
                     new Font(pageTheme.getListFont(), pageTheme.getListStyle(), pageTheme.getListFontSize());
                default ->
                    null;

            };
            Font chosenFont = Util.getFont(StylePicker.this, f, false, false);
            if (chosenFont != null) {

                switch (selectedLine) {
                    case "#" -> {
                        pageTheme.setHeader3FontFamily(chosenFont.getFontName());
                        pageTheme.setHeader3Size(chosenFont.getSize());
                        alteredPageTheme.setHeader3FontFamily(chosenFont.getFontName());
                        alteredPageTheme.setHeader3Size(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader3FontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setHeader3Size(chosenFont.getSize());
                        }
                        h3Label.setFont(chosenFont);
                    }
                    case "##" -> {
                        pageTheme.setHeader2FontFamily(chosenFont.getFontName());
                        pageTheme.setHeader2Size(chosenFont.getSize());
                        alteredPageTheme.setHeader2FontFamily(chosenFont.getFontName());
                        alteredPageTheme.setHeader2Size(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader2FontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setHeader2Size(chosenFont.getSize());
                        }
                        h2Label.setFont(chosenFont);
                    }
                    case "###" -> {
                        pageTheme.setHeader1FontFamily(chosenFont.getFontName());
                        pageTheme.setHeader1Size(chosenFont.getSize());
                        alteredPageTheme.setHeader1FontFamily(chosenFont.getFontName());
                        alteredPageTheme.setHeader1Size(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader1FontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setHeader1Size(chosenFont.getSize());
                        }
                        h1Label.setFont(chosenFont);
                    }
                    case "=>", "=> Hover", "=> Visited" -> {
                        pageTheme.setLinkFontFamily(chosenFont.getFontName());
                        pageTheme.setLinkSize(chosenFont.getSize());
                        alteredPageTheme.setLinkFontFamily(chosenFont.getFontName());
                        alteredPageTheme.setLinkSize(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setLinkFontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setLinkSize(chosenFont.getSize());
                        }
                        linkLabel.setFont(chosenFont);
                        hoverLabel.setFont(chosenFont);
                        visitedLabel.setFont(chosenFont);
                    }
                    case ">" -> {
                        pageTheme.setQuoteFontFamily(chosenFont.getFontName());
                        pageTheme.setQuoteSize(chosenFont.getSize());
                        alteredPageTheme.setQuoteFontFamily(chosenFont.getFontName());
                        alteredPageTheme.setQuoteSize(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setQuoteFontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setQuoteSize(chosenFont.getSize());
                        }
                        quoteLabel.setFont(chosenFont);
                    }
                    case "Text" -> {
                        pageTheme.setFontFamily(chosenFont.getFontName());
                        pageTheme.setFontSize(chosenFont.getSize());
                        alteredPageTheme.setFontFamily(chosenFont.getFontName());
                        alteredPageTheme.setFontSize(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setFontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setFontSize(chosenFont.getSize());
                        }
                        textLabel.setFont(chosenFont);
                    }
                    case "PF Text" -> {
                        pageTheme.setMonoFontFamily(chosenFont.getFontName());
                        pageTheme.setMonoFontSize(chosenFont.getSize());
                        alteredPageTheme.setMonoFontFamily(chosenFont.getFontName());
                        alteredPageTheme.setMonoFontSize(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setMonoFontFamily(chosenFont.getFontName());
                            saveAlteredPageTheme.setMonoFontSize(chosenFont.getSize());
                        }
                        monoFontLabel.setFont(chosenFont);
                    }
                    case "*" -> {
                        pageTheme.setListFont(chosenFont.getFontName());
                        pageTheme.setListFontSize(chosenFont.getSize());
                        alteredPageTheme.setListFont(chosenFont.getFontName());
                        alteredPageTheme.setListFontSize(chosenFont.getSize());
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setListFont(chosenFont.getFontName());
                            saveAlteredPageTheme.setListFontSize(chosenFont.getSize());
                        }
                        listLabel.setFont(chosenFont);
                    }
                    default -> {
                    }
                }

            }
        });
        buttonPanel.add(fontButton);
        JButton colorButton = new JButton(I18n.t("colorButton"));
        //saveColor = pageTheme.getHeader3Color();

        colorButton.addActionListener(al -> {
            Color c = switch (selectedLine) {
                case "#" ->
                    pageTheme.getHeader3Color();
                case "##" ->
                    pageTheme.getHeader2Color();
                case "###" ->
                    pageTheme.getHeader1Color();
                case "=>" ->
                    pageTheme.getLinkColor();
                case "=> Hover" ->
                    pageTheme.getHoverColor();
                case "=> Visited" ->
                    pageTheme.getVisitedLinkColor();
                case ">" ->
                    pageTheme.getQuoteForeground();
                case "Text" ->
                    pageTheme.getTextForeground();
                case "PF Text" ->
                    pageTheme.getMonoFontColor();
                case "*" ->
                    pageTheme.getListColor();
                default ->
                    null;

            };

            Color chosenColor = Util.getColor(StylePicker.this, c);
            if (chosenColor == null) {
                return;
            }

            switch (selectedLine) {
                case "#" -> {
                    pageTheme.setHeader3Color(chosenColor);
                    alteredPageTheme.setHeader3Color(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setHeader3Color(chosenColor);
                    }
                    h3Label.setForeground(chosenColor);
                }
                case "##" -> {
                    pageTheme.setHeader2Color(chosenColor);
                    alteredPageTheme.setHeader2Color(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setHeader2Color(chosenColor);
                    }
                    h2Label.setForeground(chosenColor);
                }
                case "###" -> {
                    pageTheme.setHeader1Color(chosenColor);
                    alteredPageTheme.setHeader1Color(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setHeader1Color(chosenColor);
                    }
                    h1Label.setForeground(chosenColor);
                }
                case "=>" -> {
                    pageTheme.setLinkColor(chosenColor);
                    alteredPageTheme.setLinkColor(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setLinkColor(chosenColor);
                    }
                    linkLabel.setForeground(chosenColor);
                }
                case "=> Hover" -> {
                    pageTheme.setHoverColor(chosenColor);
                    alteredPageTheme.setHoverColor(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setHoverColor(chosenColor);
                    }
                    hoverLabel.setForeground(chosenColor);
                }
                case "=> Visited" -> {
                    pageTheme.setVisitedLinkColor(chosenColor);
                    alteredPageTheme.setVisitedLinkColor(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setVisitedLinkColor(chosenColor);
                    }
                    visitedLabel.setForeground(chosenColor);
                }
                case ">" -> {
                    pageTheme.setQuoteForeground(chosenColor);
                    alteredPageTheme.setQuoteForeground(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setQuoteForeground(chosenColor);
                    }
                    quoteLabel.setForeground(chosenColor);
                }
                case "Text" -> {
                    pageTheme.setTextForeground(chosenColor);
                    alteredPageTheme.setTextForeground(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setTextForeground(chosenColor);
                    }
                    textLabel.setForeground(chosenColor);
                }
                case "PF Text" -> {
                    pageTheme.setMonoFontColor(chosenColor);
                    alteredPageTheme.setMonoFontColor(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setMonoFontColor(chosenColor);
                    }
                    monoFontLabel.setForeground(chosenColor);
                }
                case "*" -> {
                    pageTheme.setListColor(chosenColor);
                    alteredPageTheme.setListColor(chosenColor);
                    if (saveAlteredPageTheme != null) {
                        saveAlteredPageTheme.setListColor(chosenColor);
                    }
                    listLabel.setForeground(chosenColor);
                }
                default -> {
                }
            }
        });
        buttonPanel.add(colorButton);
        JButton styleButton = new JButton(I18n.t("styleButton"));
        styleButton.addActionListener(al -> {
            Integer st = switch (selectedLine) {
                case "#" ->
                    pageTheme.getHeader3Style();
                case "##" ->
                    pageTheme.getHeader2Style();
                case "###" ->
                    pageTheme.getHeader1Style();
                case "=>" ->
                    pageTheme.getLinkStyle();
                case "=> Hover" ->
                    pageTheme.getLinkStyle();
                case "=> Visited" ->
                    pageTheme.getLinkStyle();
                case ">" ->
                    pageTheme.getQuoteStyle();
                case "Text" ->
                    pageTheme.getFontStyle();
                case "*" ->
                    pageTheme.getListStyle();
                case "PF Text" ->
                    null;
                //pageTheme.getMonoFontColor();
                default ->
                    null;

            };
            Boolean ul = switch (selectedLine) {
                case "#" ->
                    pageTheme.getHeader3Underline();
                case "##" ->
                    pageTheme.getHeader2Underline();
                case "###" ->
                    pageTheme.getHeader1Underline();
                case "=>" ->
                    pageTheme.getLinkUnderline();
                case "=> Hover" ->
                    pageTheme.getLinkUnderline();
                case "=> Visited" ->
                    pageTheme.getLinkUnderline();
                case ">" ->
                    pageTheme.getQuoteUnderline();
                case "Text" ->
                    pageTheme.getFontUnderline();
                case "*" ->
                    pageTheme.getListUnderline();
                case "PF Text" ->
                    null;
                //pageTheme.getMonoFontColor();
                default ->
                    null;

            };
            if (st == null) {
                Util.infoDialog(StylePicker.this, I18n.t("unsupportedDialog"), I18n.t("unsupportedText"), JOptionPane.WARNING_MESSAGE);
                return;
            }
            JCheckBox italicCB = new JCheckBox(I18n.t("italicStyle"));
            italicCB.setSelected((st & Font.ITALIC) != 0);
            JCheckBox boldCB = new JCheckBox(I18n.t("boldStyle"));
            boldCB.setSelected((st & Font.BOLD) != 0);
            JCheckBox underlineCB = new JCheckBox(I18n.t("underlineStyle"));
            underlineCB.setSelected(ul);
            Object[] comps = new Object[3];

            comps[0] = boldCB;
            comps[1] = italicCB;
            comps[2] = underlineCB;

            Object res = Util.inputDialog2(this, I18n.t("fontStyleDialog"), comps, null, false);
            if (res != null) {
                boolean isBold = boldCB.isSelected();
                boolean isItalic = italicCB.isSelected();
                boolean underline = underlineCB.isSelected();
                int style = Font.PLAIN;  // start plain

                if (isBold) {
                    style |= Font.BOLD;
                }
                if (isItalic) {
                    style |= Font.ITALIC;
                }
                switch (selectedLine) {
                    case "#" -> {
                        pageTheme.setHeader3Style(style);
                        pageTheme.setHeader3Underline(underline);
                        alteredPageTheme.setHeader3Style(style);
                        alteredPageTheme.setHeader3Underline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader3Style(style);
                            saveAlteredPageTheme.setHeader3Underline(underline);
                        }
                        Font f = new Font(pageTheme.getHeader3FontFamily(), style, pageTheme.getHeader3Size());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        h3Label.setFont(f);

                    }
                    case "##" -> {
                        pageTheme.setHeader2Style(style);
                        pageTheme.setHeader2Underline(underline);
                        alteredPageTheme.setHeader2Style(style);
                        alteredPageTheme.setHeader2Underline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader2Style(style);
                            saveAlteredPageTheme.setHeader2Underline(underline);
                        }
                        Font f = new Font(pageTheme.getHeader2FontFamily(), style, pageTheme.getHeader2Size());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        h2Label.setFont(f);
                    }
                    case "###" -> {
                        pageTheme.setHeader1Style(style);
                        pageTheme.setHeader1Underline(underline);
                        alteredPageTheme.setHeader1Style(style);
                        alteredPageTheme.setHeader1Underline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setHeader1Style(style);
                            saveAlteredPageTheme.setHeader1Underline(underline);
                        }
                        Font f = new Font(pageTheme.getHeader1FontFamily(), style, pageTheme.getHeader1Size());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        h1Label.setFont(f);
                        //h1Label.setFont(new Font(pageTheme.getHeader1FontFamily(), style, pageTheme.getHeader1Size()));
                    }
                    case "=>", "=> Hover", "=> Visited" -> {
                        pageTheme.setLinkStyle(style);
                        pageTheme.setLinkUnderline(underline);
                        alteredPageTheme.setLinkStyle(style);
                        alteredPageTheme.setLinkUnderline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setLinkStyle(style);
                            saveAlteredPageTheme.setLinkUnderline(underline);
                        }
                        Font f = new Font(pageTheme.getLinkFontFamily(), style, pageTheme.getLinkSize());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        linkLabel.setFont(f);
                        hoverLabel.setFont(f);
                        visitedLabel.setFont(f);
                    }
                    case ">" -> {
                        pageTheme.setQuoteStyle(style);
                        pageTheme.setQuoteUnderline(underline);

                        alteredPageTheme.setQuoteStyle(style);
                        alteredPageTheme.setQuoteUnderline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setQuoteStyle(style);
                            saveAlteredPageTheme.setQuoteUnderline(underline);
                        }
                        Font f = new Font(pageTheme.getQuoteFontFamily(), style, pageTheme.getQuoteSize());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        quoteLabel.setFont(f);
                        //quoteLabel.setFont(new Font(pageTheme.getQuoteFontFamily(), style, pageTheme.getQuoteSize()));
                    }
                    case "Text" -> {
                        pageTheme.setFontStyle(style);
                        pageTheme.setFontUnderline(underline);
                        alteredPageTheme.setFontStyle(style);
                        alteredPageTheme.setFontUnderline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setFontStyle(style);
                            saveAlteredPageTheme.setFontUnderline(underline);
                        }
                        Font f = new Font(pageTheme.getFontFamily(), style, pageTheme.getFontSize());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        textLabel.setFont(f);
                    }
                    case "*" -> {
                        pageTheme.setListStyle(style);
                        pageTheme.setListUnderline(underline);
                        alteredPageTheme.setListStyle(style);
                        alteredPageTheme.setListUnderline(underline);
                        if (saveAlteredPageTheme != null) {
                            saveAlteredPageTheme.setListStyle(style);
                            saveAlteredPageTheme.setListUnderline(underline);
                        }
                        Font f = new Font(pageTheme.getFontFamily(), style, pageTheme.getFontSize());
                        if (underline) {
                            f = underlineFont(f);
                        }
                        listLabel.setFont(f);
                    }
                    case "PF Text" -> {

                    }
                    default -> {
                    }
                }
            }
        });
        buttonPanel.add(styleButton);

        JMenuItem resetFItem = new JMenuItem(I18n.t("fontNameLabel"));
        Map<String, BiConsumer<PageTheme, PageTheme>> nameResetters = new HashMap<>();
        nameResetters.put("#", (page, def) -> page.setHeader3FontFamily(def.getHeader3FontFamily()));
        nameResetters.put("##", (page, def) -> page.setHeader2FontFamily(def.getHeader2FontFamily()));
        nameResetters.put("###", (page, def) -> page.setHeader1FontFamily(def.getHeader1FontFamily()));
        nameResetters.put("=>", (page, def) -> page.setLinkFontFamily(def.getLinkFontFamily()));
        nameResetters.put("=> Hover", (page, def) -> page.setLinkFontFamily(def.getLinkFontFamily()));
        nameResetters.put("=> Visited", (page, def) -> page.setLinkFontFamily(def.getLinkFontFamily()));
        nameResetters.put(">", (page, def) -> page.setQuoteFontFamily(def.getQuoteFontFamily()));
        nameResetters.put("Text", (page, def) -> page.setFontFamily(def.getFontFamily()));
        nameResetters.put("*", (page, def) -> page.setListFont(def.getListFont()));
        nameResetters.put("PF Text", (page, def) -> page.setMonoFontFamily(def.getMonoFontFamily()));
        resetFItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            BiConsumer<PageTheme, PageTheme> resetter = nameResetters.get(selectedLine);
            if (resetter != null) {
                resetter.accept(pageTheme, defaultTheme);

                alteredPageTheme.clearFont(selectedLine);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.clearFont(selectedLine);
                }
                setPanelAttributes();
            }
            //Util.infoDialog(StylePicker.this, I18n.t("resetAttrDialog"), I18n.t("resetAttrDialogTxt"));

        });

        JMenuItem resetFSItem = new JMenuItem(I18n.t("fontSizeLabel"));
        Map<String, BiConsumer<PageTheme, PageTheme>> sizeResetters = new HashMap<>();
        sizeResetters.put("#", (page, def) -> page.setHeader3Size(def.getHeader3Size()));
        sizeResetters.put("##", (page, def) -> page.setHeader2Size(def.getHeader2Size()));
        sizeResetters.put("###", (page, def) -> page.setHeader1Size(def.getHeader1Size()));
        sizeResetters.put("=>", (page, def) -> page.setLinkSize(def.getLinkSize()));
        sizeResetters.put("=> Hover", (page, def) -> page.setLinkSize(def.getLinkSize()));
        sizeResetters.put("=> Visited", (page, def) -> page.setLinkSize(def.getLinkSize()));
        sizeResetters.put(">", (page, def) -> page.setQuoteSize(def.getQuoteSize()));
        sizeResetters.put("*", (page, def) -> page.setListFontSize(def.getListFontSize()));
        sizeResetters.put("Text", (page, def) -> page.setFontSize(def.getFontSize()));
        sizeResetters.put("PF Text", (page, def) -> page.setMonoFontSize(def.getMonoFontSize()));
        resetFSItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            BiConsumer<PageTheme, PageTheme> resetter = sizeResetters.get(selectedLine);
            if (resetter != null) {
                resetter.accept(pageTheme, defaultTheme);

                alteredPageTheme.clearFontSize(selectedLine);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.clearFontSize(selectedLine);
                }
                setPanelAttributes();
            }
            //Util.infoDialog(StylePicker.this, I18n.t("resetAttrDialog"), I18n.t("resetAttrDialogTxt"));

        });

        JMenuItem resetFCItem = new JMenuItem(I18n.t("fontColorLabel"));
        Map<String, BiConsumer<PageTheme, PageTheme>> colorResetters = new HashMap<>();
        colorResetters.put("#", (page, def) -> page.setHeader3Color(def.getHeader3Color()));
        colorResetters.put("##", (page, def) -> page.setHeader2Color(def.getHeader2Color()));
        colorResetters.put("###", (page, def) -> page.setHeader1Color(def.getHeader1Color()));
        colorResetters.put("=>", (page, def) -> page.setLinkColor(def.getLinkColor()));
        colorResetters.put("=> Hover", (page, def) -> page.setHoverColor(def.getHoverColor()));
        colorResetters.put("=> Visited", (page, def) -> page.setVisitedLinkColor(def.getVisitedLinkColor()));
        colorResetters.put(">", (page, def) -> page.setQuoteForeground(def.getQuoteForeground()));
        colorResetters.put("*", (page, def) -> page.setListColor(def.getListColor()));
        colorResetters.put("Text", (page, def) -> page.setTextForeground(def.getTextForeground()));
        colorResetters.put("PF Text", (page, def) -> page.setMonoFontColor(def.getMonoFontColor()));
        resetFCItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            BiConsumer<PageTheme, PageTheme> resetter = colorResetters.get(selectedLine);
            if (resetter != null) {
                resetter.accept(pageTheme, defaultTheme);

                alteredPageTheme.clearColor(selectedLine);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.clearColor(selectedLine);
                }
                setPanelAttributes();
            }
            //Util.infoDialog(StylePicker.this, I18n.t("resetAttrDialog"), I18n.t("resetAttrDialogTxt"));

        });

        JMenuItem resetStyleItem = new JMenuItem(I18n.t("fontStyleLabel"));
        Map<String, BiConsumer<PageTheme, PageTheme>> styleResetters = new HashMap<>();
        styleResetters.put("#", (page, def) -> {
            page.setHeader3Style(def.getHeader3Style());
            page.setHeader3Underline(def.getHeader3Underline());
        });
        styleResetters.put("##", (page, def) -> {
            page.setHeader2Style(def.getHeader2Style());
            page.setHeader2Underline(def.getHeader2Underline());
        });
        styleResetters.put("###", (page, def) -> {
            page.setHeader1Style(def.getHeader1Style());
            page.setHeader1Underline(def.getHeader1Underline());
        });
        styleResetters.put("=>", (page, def) -> {
            page.setLinkColor(def.getLinkColor());
            page.setLinkUnderline(def.getLinkUnderline());
        });
        styleResetters.put("=> Hover", (page, def) -> {
            page.setLinkColor(def.getLinkColor());
            page.setLinkUnderline(def.getLinkUnderline());
        });
        styleResetters.put("=> Visited", (page, def) -> {
            page.setLinkColor(def.getLinkColor());
            page.setLinkUnderline(def.getLinkUnderline());
        });
        styleResetters.put(">", (page, def) -> {
            page.setQuoteStyle(def.getQuoteStyle());
            page.setQuoteUnderline(def.getQuoteUnderline());
        });
        styleResetters.put("*", (page, def) -> {
            page.setListStyle(def.getListStyle());
            page.setListUnderline(def.getListUnderline());
        });
        styleResetters.put("Text", (page, def) -> {
            page.setFontStyle(def.getFontStyle());
            page.setFontUnderline(def.getFontUnderline());
        });
        // styleResetters.put("PF Text", (page, def) -> {
        //     page.setMonoFontColor(def.getMonoFontColor());
        // });
        resetStyleItem.addActionListener(al -> {
            PageTheme defaultTheme = GeminiTextPane.getDefaultTheme(ui);
            BiConsumer<PageTheme, PageTheme> resetter = styleResetters.get(selectedLine);
            if (resetter != null) {
                resetter.accept(pageTheme, defaultTheme);

                alteredPageTheme.clearFontStyle(selectedLine);
                if (saveAlteredPageTheme != null) {
                    saveAlteredPageTheme.clearFontStyle(selectedLine);
                }
                setPanelAttributes();
            }
            //Util.infoDialog(StylePicker.this, I18n.t("resetAttrDialog"), I18n.t("resetAttrDialogTxt"));

        });

        List<JComponent> resetItems = new ArrayList<>();
        Supplier<List<JComponent>> resetSupplier = () -> {

            if (resetItems.isEmpty()) {
                resetItems.add(resetFItem);
                resetItems.add(resetFSItem);
                resetItems.add(resetFCItem);
                resetItems.add(resetStyleItem);

            }

            return resetItems;
        };
        PopupMenuButton resetButton = new PopupMenuButton("Reset", resetSupplier, "");
        buttonPanel.add(resetButton);
        textPanel = new JPanel();
        textPanel.setPreferredSize(new Dimension(800, 390));
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(true);

        h3Label = new JLabel(I18n.t("h3Label"));
        textPanel.add(wrap(h3Label));
        h2Label = new JLabel(I18n.t("h2Label"));
        textPanel.add(wrap(h2Label));
        h1Label = new JLabel(I18n.t("h1Label"));
        textPanel.add(wrap(h1Label));

        linkLabel = new JLabel(I18n.t("linkLabel"));

        textPanel.add(wrap(linkLabel));

        hoverLabel = new JLabel(I18n.t("linkHoverLabel"));
        textPanel.add(wrap(hoverLabel));

        visitedLabel = new JLabel(I18n.t("linkVisitedLabel"));

        textPanel.add(wrap(visitedLabel));

        listLabel = new JLabel("• " + I18n.t("listLabel"));
        textPanel.add(wrap(listLabel));

        quoteLabel = new JLabel(I18n.t("quoteLabel"));
        textPanel.add(wrap(quoteLabel));

        textLabel = new JLabel(I18n.t("styleTextLabel"));
        textPanel.add(wrap(textLabel));

        monoFontLabel = new JLabel(I18n.t("pfTextLabel"));
        setPanelAttributes();
        textPanel.add(wrap(monoFontLabel));

        linePanel.add(buttonPanel);

        contentPanel.add(Box.createVerticalStrut(10));
        textPanel.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        contentPanel.add(new JScrollPane(textPanel));

        add(contentPanel, BorderLayout.CENTER);

    }

    private Font underlineFont(Font f) {
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) f.getAttributes();
        attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON);
        return f.deriveFont(attributes);
    }

    private void setPanelAttributes() {
        textPanel.setBackground(pageTheme.getPageBackground());
        Font f = new Font(pageTheme.getHeader3FontFamily(), pageTheme.getHeader3Style(), pageTheme.getHeader3Size());
        if (pageTheme.getHeader3Underline()) {
            f = underlineFont(f);
        }
        h3Label.setFont(f);

        h3Label.setForeground(pageTheme.getHeader3Color());
        f = new Font(pageTheme.getHeader2FontFamily(), pageTheme.getHeader2Style(), pageTheme.getHeader2Size());
        if (pageTheme.getHeader2Underline()) {
            f = underlineFont(f);
        }
        h2Label.setFont(f);

        h2Label.setForeground(pageTheme.getHeader2Color());
        f = new Font(pageTheme.getHeader1FontFamily(), pageTheme.getHeader1Style(), pageTheme.getHeader1Size());
        if (pageTheme.getHeader1Underline()) {
            f = underlineFont(f);
        }
        h1Label.setFont(f);

        h1Label.setForeground(pageTheme.getHeader1Color());
        f = new Font(pageTheme.getLinkFontFamily(), pageTheme.getLinkStyle(), pageTheme.getLinkSize());
        if (pageTheme.getLinkUnderline()) {
            f = underlineFont(f);
        }
        linkLabel.setFont(f);

        linkLabel.setForeground(pageTheme.getLinkColor());

        hoverLabel.setFont(f);
        hoverLabel.setForeground(pageTheme.getHoverColor());

        visitedLabel.setFont(f);
        visitedLabel.setForeground(pageTheme.getVisitedLinkColor());
        f = new Font(pageTheme.getQuoteFontFamily(), pageTheme.getQuoteStyle(), pageTheme.getQuoteSize());
        if (pageTheme.getQuoteUnderline()) {
            f = underlineFont(f);
        }
        quoteLabel.setFont(f);

        quoteLabel.setForeground(pageTheme.getQuoteForeground());
        f = new Font(pageTheme.getFontFamily(), pageTheme.getFontStyle(), pageTheme.getFontSize());
        if (pageTheme.getFontUnderline()) {
            f = underlineFont(f);
        }
        f = new Font(pageTheme.getListFont(), pageTheme.getListStyle(), pageTheme.getListFontSize());
        if (pageTheme.getListUnderline()) {
            f = underlineFont(f);
        }
        listLabel.setFont(f);

        listLabel.setForeground(pageTheme.getListColor());

        f = new Font(pageTheme.getFontFamily(), pageTheme.getFontStyle(), pageTheme.getFontSize());
        if (pageTheme.getFontUnderline()) {
            f = underlineFont(f);
        }

        textLabel.setFont(f);

        textLabel.setForeground(pageTheme.getTextForeground());
        monoFontLabel.setFont(new Font(pageTheme.getMonoFontFamily(), Font.PLAIN, pageTheme.getMonoFontSize()));
        monoFontLabel.setForeground(pageTheme.getMonoFontColor());

    }

    private JPanel wrap(JLabel l) {
        JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        jp.setOpaque(false);
        jp.add(l);
        return jp;
    }

    public PageTheme getAlteredPageTheme() {
        return alteredPageTheme;
    }

}
