package brad.grier.alhena;

import java.awt.Color;

import io.vertx.core.json.JsonObject;

public class PageTheme {

    private Color linkColor;
    private Integer linkStyle;
    private Boolean linkUnderline;
    private Color hoverColor;
    private Integer monoFontSize;
    private String monoFontFamily;
    private Color monoFontColor;
    private Integer fontSize;
    private Integer fontStyle;
    private Boolean fontUnderline;
    private String fontFamily;
    private Color header1Color;
    private Boolean header1Underline;
    private Integer header1Style;
    private Color header2Color;
    private Boolean header2Underline;
    private Integer header2Style;
    private Color header3Color;
    private Boolean header3Underline;
    private Integer header3Style;
    private Integer header1Size;
    private Integer header2Size;
    private Integer header3Size;
    private Integer linkSize;
    private Color visitedLinkColor;
    private Color quoteForeground;
    private Boolean quoteUnderline;
    private Integer quoteSize;
    private Integer quoteStyle;
    private Color textForeground;
    private Color pageBackground;
    private String header1FontFamily;
    private String header2FontFamily;
    private String header3FontFamily;
    private String linkFontFamily;
    private String quoteFontFamily;
    private String listFont;
    private Integer listFontSize;
    private Color listColor;
    private Integer listStyle;
    private Boolean listUnderline;
    private Float contentPercentage;
    private Color spinnerColor;
    private Boolean gradientBG;
    private Color gradient1Color;
    private Color gradient2Color;

    public Color getLinkColor() {
        return linkColor;
    }

    public void setLinkColor(Color linkColor) {
        this.linkColor = linkColor;
    }

    public void setLinkColor(Integer color) {
        if (color != null) {
            this.linkColor = new Color(color);
        }
    }

    public Color getHoverColor() {
        return hoverColor;
    }

    public void setHoverColor(Color hoverColor) {
        this.hoverColor = hoverColor;
    }

    public void setHoverColor(Integer color) {
        if (color != null) {
            this.hoverColor = new Color(color);
        }
    }

    public Integer getMonoFontSize() {
        return monoFontSize;
    }

    public void setMonoFontSize(Integer size) {
        if (size != null) {
            this.monoFontSize = size;
        }
    }

    public String getMonoFontFamily() {
        return monoFontFamily;
    }

    public void setMonoFontFamily(String monoFontFamily) {
        if (monoFontFamily != null) {
            this.monoFontFamily = monoFontFamily;
        }
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        if (fontSize != null) {
            this.fontSize = fontSize;
        }
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        if (fontFamily != null) {
            this.fontFamily = fontFamily;
        }
    }

    public Color getHeader1Color() {
        return header1Color;
    }

    public void setHeader1Color(Color header1Color) {
        this.header1Color = header1Color;
    }

    public void setHeader1Color(Integer header1Color) {
        if (header1Color != null) {
            this.header1Color = new Color(header1Color);
        }
    }

    public Color getHeader2Color() {
        return header2Color;
    }

    public void setHeader2Color(Color header2Color) {
        this.header2Color = header2Color;
    }

    public void setHeader2Color(Integer header2Color) {
        if (header2Color != null) {
            this.header2Color = new Color(header2Color);
        }
    }

    public Color getHeader3Color() {
        return header3Color;
    }

    public void setHeader3Color(Color header3Color) {
        this.header3Color = header3Color;
    }

    public void setHeader3Color(Integer header3Color) {
        if (header3Color != null) {
            this.header3Color = new Color(header3Color);
        }
    }

    public Integer getHeader1Size() {
        return header1Size;
    }

    public void setHeader1Size(Integer header1Size) {
        if (header1Size != null) {
            this.header1Size = header1Size;
        }
    }

    public Integer getHeader2Size() {
        return header2Size;
    }

    public void setHeader2Size(Integer header2Size) {
        if (header2Size != null) {
            this.header2Size = header2Size;
        }
    }

    public Integer getHeader3Size() {
        return header3Size;
    }

    public void setHeader3Size(Integer header3Size) {
        if (header3Size != null) {
            this.header3Size = header3Size;
        }
    }

    public Integer getLinkSize() {
        return linkSize;
    }

    public void setLinkSize(Integer linkSize) {
        if (linkSize != null) {
            this.linkSize = linkSize;
        }
    }

    public Color getVisitedLinkColor() {
        return visitedLinkColor;
    }

    public void setVisitedLinkColor(Color visitedLinkColor) {
        this.visitedLinkColor = visitedLinkColor;
    }

    public void setVisitedLinkColor(Integer visitedLinkColor) {
        if (visitedLinkColor != null) {
            this.visitedLinkColor = new Color(visitedLinkColor);
        }
    }

    public Color getQuoteForeground() {
        return quoteForeground;
    }

    public void setQuoteForeground(Color quoteForeground) {
        this.quoteForeground = quoteForeground;
    }

    public void setQuoteForeground(Integer quoteForeground) {
        if (quoteForeground != null) {
            this.quoteForeground = new Color(quoteForeground);
        }
    }

    public Color getTextForeground() {
        return textForeground;
    }

    public void setTextForeground(Color textForeground) {
        this.textForeground = textForeground;
    }

    public void setTextForeground(Integer textForeground) {
        if (textForeground != null) {
            this.textForeground = new Color(textForeground);
        }
    }

    public Integer getQuoteSize() {
        return quoteSize;
    }

    public void setQuoteSize(Integer quoteSize) {
        if (quoteSize != null) {
            this.quoteSize = quoteSize;
        }
    }

    public Color getPageBackground() {
        return pageBackground;
    }

    public void setPageBackground(Color pageBackground) {
        this.pageBackground = pageBackground;
    }

    public void setPageBackground(Integer pageBackground) {
        if (pageBackground != null) {
            this.pageBackground = new Color(pageBackground);
        }
    }

    public Color getSpinnerColor() {
        return spinnerColor;
    }

    public void setSpinnerColor(Color spinnerColor) {
        this.spinnerColor = spinnerColor;
    }

    public void setSpinnerColor(Integer spinnerColor) {
        if (spinnerColor != null) {
            this.spinnerColor = new Color(spinnerColor);
        }
    }

    public Color getGradient1Color() {
        return gradient1Color;
    }

    public void setGradient1Color(Color gradient1Color) {
        this.gradient1Color = gradient1Color;
    }

    public void setGradient1Color(Integer gradient1Color) {
        if (gradient1Color != null) {
            this.gradient1Color = new Color(gradient1Color);
        }
    }

    public Color getGradient2Color() {
        return gradient2Color;
    }

    public void setGradient2Color(Color gradient2Color) {
        this.gradient2Color = gradient2Color;
    }

    public void setGradient2Color(Integer gradient2Color) {
        if (gradient2Color != null) {
            this.gradient2Color = new Color(gradient2Color);
        }
    }

    public String getHeader1FontFamily() {
        return header1FontFamily;
    }

    public void setHeader1FontFamily(String header1FontFamily) {
        if (header1FontFamily != null) {
            this.header1FontFamily = header1FontFamily;
        }
    }

    public String getHeader2FontFamily() {
        return header2FontFamily;
    }

    public void setHeader2FontFamily(String header2FontFamily) {
        if (header2FontFamily != null) {
            this.header2FontFamily = header2FontFamily;
        }
    }

    public String getHeader3FontFamily() {
        return header3FontFamily;
    }

    public void setHeader3FontFamily(String header3FontFamily) {
        if (header3FontFamily != null) {
            this.header3FontFamily = header3FontFamily;
        }
    }

    public String getLinkFontFamily() {
        return linkFontFamily;
    }

    public void setLinkFontFamily(String linkFontFamily) {
        if (linkFontFamily != null) {
            this.linkFontFamily = linkFontFamily;
        }
    }

    public String getQuoteFontFamily() {
        return quoteFontFamily;
    }

    public void setQuoteFontFamily(String quoteFontFamily) {
        if (quoteFontFamily != null) {
            this.quoteFontFamily = quoteFontFamily;
        }
    }

    public void fromJson(JsonObject jo) {
        setLinkColor(jo.getInteger("linkColor"));
        setLinkStyle(jo.getInteger("linkStyle"));
        setVisitedLinkColor(jo.getInteger("visitedLinkColor"));
        setHoverColor(jo.getInteger("hoverColor"));
        setMonoFontSize(jo.getInteger("monoFontSize"));
        setMonoFontFamily(jo.getString("monoFontFamily"));
        setMonoFontColor(jo.getInteger("monoFontColor"));
        setTextForeground(jo.getInteger("textForeground"));
        setQuoteForeground(jo.getInteger("quoteForeground"));
        setQuoteStyle(jo.getInteger("quoteStyle"));
        setFontSize(jo.getInteger("fontSize"));
        setFontStyle(jo.getInteger("fontStyle"));
        setFontFamily(jo.getString("fontFamily"));
        setHeader1Color(jo.getInteger("header1Color"));
        setHeader1Style(jo.getInteger("header1Style"));
        setHeader2Color(jo.getInteger("header2Color"));
        setHeader2Style(jo.getInteger("header2Style"));
        setHeader3Color(jo.getInteger("header3Color"));
        setHeader3Style(jo.getInteger("header3Style"));
        setHeader1Size(jo.getInteger("header1Size"));
        setHeader2Size(jo.getInteger("header2Size"));
        setHeader3Size(jo.getInteger("header3Size"));
        setLinkSize(jo.getInteger("linkSize"));
        setQuoteSize(jo.getInteger("quoteSize"));
        setPageBackground(jo.getInteger("pageBackground"));
        setSpinnerColor(jo.getInteger("spinnerColor"));
        setContentPercentage(jo.getFloat("contentPercentage"));
        setHeader1FontFamily(jo.getString("header1FontFamily"));
        setHeader2FontFamily(jo.getString("header2FontFamily"));
        setHeader3FontFamily(jo.getString("header3FontFamily"));
        setLinkFontFamily(jo.getString("linkFontFamily"));
        setQuoteFontFamily(jo.getString("quoteFontFamily"));
        setLinkUnderline(jo.getBoolean("linkUnderline"));
        setQuoteUnderline(jo.getBoolean("quoteUnderline"));
        setFontUnderline(jo.getBoolean("fontUnderline"));
        setHeader1Underline(jo.getBoolean("header1Underline"));
        setHeader2Underline(jo.getBoolean("header2Underline"));
        setHeader3Underline(jo.getBoolean("header3Underline"));
        setListFont(jo.getString("listFont"));
        setListFontSize(jo.getInteger("listFontSize"));
        setListColor(jo.getInteger("listColor"));
        setListStyle(jo.getInteger("listStyle"));
        setListUnderline(jo.getBoolean("listUnderline"));
        setGradientBG(jo.getBoolean("gradientBG"));
        setGradient1Color(jo.getInteger("gradient1Color"));
        setGradient2Color(jo.getInteger("gradient2Color"));

    }

    public String getJson() {
        JsonObject styleJson = new JsonObject();
        if (linkColor != null) {
            styleJson.put("linkColor", linkColor.getRGB());
        }
        if (linkStyle != null) {
            styleJson.put("linkStyle", linkStyle);
        }
        if (visitedLinkColor != null) {
            styleJson.put("visitedLinkColor", visitedLinkColor.getRGB());
        }

        if (hoverColor != null) {
            styleJson.put("hoverColor", hoverColor.getRGB());
        }

        if (monoFontSize != null) {
            styleJson.put("monoFontSize", monoFontSize);
        }

        if (monoFontFamily != null) {
            styleJson.put("monoFontFamily", monoFontFamily);
        }
        if (monoFontColor != null) {
            styleJson.put("monoFontColor", monoFontColor.getRGB());
        }

        if (textForeground != null) {
            styleJson.put("textForeground", textForeground.getRGB());
        }
        if (fontStyle != null) {
            styleJson.put("fontStyle", fontStyle);
        }

        if (quoteForeground != null) {
            styleJson.put("quoteForeground", quoteForeground.getRGB());
        }

        if (quoteStyle != null) {
            styleJson.put("quoteStyle", quoteStyle);
        }

        if (fontSize != null) {
            styleJson.put("fontSize", fontSize);
        }

        if (fontFamily != null) {
            styleJson.put("fontFamily", fontFamily);
        }

        if (header1Color != null) {
            styleJson.put("header1Color", header1Color.getRGB());
        }

        if (header2Color != null) {
            styleJson.put("header2Color", header2Color.getRGB());
        }

        if (header3Color != null) {
            styleJson.put("header3Color", header3Color.getRGB());
        }

        if (header1Size != null) {
            styleJson.put("header1Size", header1Size);
        }
        if (header1Style != null) {
            styleJson.put("header1Style", header1Style);
        }

        if (header2Size != null) {
            styleJson.put("header2Size", header2Size);
        }
        if (header2Style != null) {
            styleJson.put("header2Style", header2Style);
        }

        if (header3Size != null) {
            styleJson.put("header3Size", header3Size);
        }
        if (header3Style != null) {
            styleJson.put("header3Style", header3Style);
        }

        if (linkSize != null) {
            styleJson.put("linkSize", linkSize);
        }

        if (quoteSize != null) {
            styleJson.put("quoteSize", quoteSize);
        }

        if (pageBackground != null) {
            styleJson.put("pageBackground", pageBackground.getRGB());
        }

        if (spinnerColor != null) {
            styleJson.put("spinnerColor", spinnerColor.getRGB());
        }

        if (gradient1Color != null) {
            styleJson.put("gradient1Color", gradient1Color.getRGB());
        }

        if (gradient2Color != null) {
            styleJson.put("gradient2Color", gradient2Color.getRGB());
        }

        if (contentPercentage != null) {
            styleJson.put("contentPercentage", contentPercentage);
        }

        if (header1FontFamily != null) {
            styleJson.put("header1FontFamily", header1FontFamily);
        }

        if (header2FontFamily != null) {
            styleJson.put("header2FontFamily", header2FontFamily);
        }

        if (header3FontFamily != null) {
            styleJson.put("header3FontFamily", header3FontFamily);
        }

        if (linkFontFamily != null) {
            styleJson.put("linkFontFamily", linkFontFamily);
        }

        if (quoteFontFamily != null) {
            styleJson.put("quoteFontFamily", quoteFontFamily);
        }

        if (linkUnderline != null) {
            styleJson.put("linkUnderline", linkUnderline);
        }
        if (quoteUnderline != null) {
            styleJson.put("quoteUnderline", quoteUnderline);
        }
        if (fontUnderline != null) {
            styleJson.put("fontUnderline", fontUnderline);
        }
        if (header1Underline != null) {
            styleJson.put("header1Underline", header1Underline);
        }
        if (header2Underline != null) {
            styleJson.put("header2Underline", header2Underline);
        }
        if (header3Underline != null) {
            styleJson.put("header3Underline", header3Underline);
        }

        if (listFont != null) {
            styleJson.put("listFont", listFont);
        }
        if (listFontSize != null) {
            styleJson.put("listFontSize", listFontSize);
        }
        if (listColor != null) {
            styleJson.put("listColor", listColor.getRGB());
        }
        if (listStyle != null) {
            styleJson.put("listStyle", listStyle);
        }
        if (listUnderline != null) {
            styleJson.put("listUnderline", listUnderline);
        }

        if (gradientBG != null) {
            styleJson.put("gradientBG", gradientBG);
        }

        return styleJson.encode();

    }

    public Color getMonoFontColor() {
        return monoFontColor;
    }

    public void setMonoFontColor(Color monoFontColor) {
        this.monoFontColor = monoFontColor;
    }

    public void clearPageBackground() {
        pageBackground = null;
    }

    public void clearGradientSettings() {
        gradient1Color = null;
        gradient2Color = null;
        gradientBG = null;
    }

    public void clearSpinnerColor() {
        spinnerColor = null;
    }

    public void clearContentWidth() {
        contentPercentage = null;
    }

    public void setMonoFontColor(Integer monoFontColor) {
        if (monoFontColor != null) {
            this.monoFontColor = new Color(monoFontColor);
        }
    }

    public Integer getLinkStyle() {
        return linkStyle;
    }

    public void setLinkStyle(Integer linkStyle) {
        if (linkStyle != null) {
            this.linkStyle = linkStyle;
        }
    }

    public Integer getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(Integer fontStyle) {
        if (fontStyle != null) {
            this.fontStyle = fontStyle;
        }
    }

    public Integer getHeader1Style() {
        return header1Style;
    }

    public void setHeader1Style(Integer header1Style) {
        if (header1Style != null) {
            this.header1Style = header1Style;
        }
    }

    public Integer getHeader2Style() {
        return header2Style;
    }

    public void setHeader2Style(Integer header2Style) {
        if (header2Style != null) {
            this.header2Style = header2Style;
        }
    }

    public Integer getHeader3Style() {
        return header3Style;
    }

    public void setHeader3Style(Integer header3Style) {
        if (header3Style != null) {
            this.header3Style = header3Style;
        }
    }

    public Integer getQuoteStyle() {
        return quoteStyle;
    }

    public void setQuoteStyle(Integer quoteStyle) {
        if (quoteStyle != null) {
            this.quoteStyle = quoteStyle;
        }
    }

    public Boolean getLinkUnderline() {
        return linkUnderline;
    }

    public void setLinkUnderline(Boolean linkUnderline) {
        if (linkUnderline != null) {
            this.linkUnderline = linkUnderline;
        }
    }

    public Boolean getFontUnderline() {
        return fontUnderline;
    }

    public void setFontUnderline(Boolean fontUnderline) {
        if (fontUnderline != null) {
            this.fontUnderline = fontUnderline;
        }
    }

    public Boolean getHeader1Underline() {
        return header1Underline;
    }

    public void setHeader1Underline(Boolean header1Underline) {
        if (header1Underline != null) {
            this.header1Underline = header1Underline;
        }
    }

    public Boolean getHeader2Underline() {
        return header2Underline;
    }

    public void setHeader2Underline(Boolean header2Underline) {
        if (header2Underline != null) {
            this.header2Underline = header2Underline;
        }
    }

    public Boolean getHeader3Underline() {
        return header3Underline;
    }

    public void setHeader3Underline(Boolean header3Underline) {
        if (header3Underline != null) {
            this.header3Underline = header3Underline;
        }
    }

    public Boolean getQuoteUnderline() {
        return quoteUnderline;
    }

    public void setQuoteUnderline(Boolean quoteUnderline) {
        if (quoteUnderline != null) {
            this.quoteUnderline = quoteUnderline;
        }
    }

    public void clearColor(String lineType) {
        switch (lineType) {
            case "#" ->
                header3Color = null;
            case "##" ->
                header2Color = null;
            case "###" ->
                header1Color = null;
            case "=>" ->
                linkColor = null;
            case "=> Hover" ->
                hoverColor = null;
            case "=> Visited" ->
                visitedLinkColor = null;
            case ">" ->
                quoteForeground = null;
            case "Text" ->
                textForeground = null;
            case "PF Text" ->
                monoFontColor = null;
            case "*" ->
                listColor = null;
            default ->
                throw new IllegalArgumentException("Unknown type: " + lineType);
        }
    }

    public void clearFont(String lineType) {
        switch (lineType) {
            case "#" ->
                header3FontFamily = null;
            case "##" ->
                header2FontFamily = null;
            case "###" ->
                header1FontFamily = null;
            case "=>" ->
                linkFontFamily = null;
            case "=> Hover" ->
                linkFontFamily = null;
            case "=> Visited" ->
                linkFontFamily = null;
            case ">" ->
                quoteFontFamily = null;
            case "Text" ->
                fontFamily = null;
            case "PF Text" ->
                monoFontFamily = null;
            case "*" ->
                listFont = null;
            default ->
                throw new IllegalArgumentException("Unknown type: " + lineType);
        }
    }

    public void clearFontSize(String lineType) {
        switch (lineType) {
            case "#" ->
                header3Size = null;
            case "##" ->
                header2Size = null;
            case "###" ->
                header1Size = null;
            case "=>" ->
                linkSize = null;
            case "=> Hover" ->
                linkSize = null;
            case "=> Visited" ->
                linkSize = null;
            case ">" ->
                quoteSize = null;
            case "Text" ->
                fontSize = null;
            case "PF Text" ->
                monoFontSize = null;
            case "*" ->
                listFontSize = null;
            default ->
                throw new IllegalArgumentException("Unknown type: " + lineType);
        }
    }

    public void clearFontStyle(String lineType) {
        switch (lineType) {
            case "#" -> {
                header3Style = null;
                header3Underline = null;
            }
            case "##" -> {
                header2Style = null;
                header2Underline = null;
            }
            case "###" -> {
                header1Style = null;
                header1Underline = null;
            }
            case "=>" -> {
                linkStyle = null;
                linkUnderline = null;
            }
            case "=> Hover" -> {
                linkStyle = null;
                linkUnderline = null;
            }
            case "=> Visited" -> {
                linkStyle = null;
                linkUnderline = null;
            }
            case ">" -> {
                quoteStyle = null;
                quoteUnderline = null;
            }
            case "Text" -> {
                fontStyle = null;
                fontUnderline = null;
            }
            case "*" -> {
                listStyle = null;
                listUnderline = null;
            }
            // case "PF Text" ->
            //     mono = null;
            default ->
                throw new IllegalArgumentException("Unknown type: " + lineType);
        }
    }

    public String getListFont() {
        return listFont;
    }

    public void setListFont(String listFont) {
        if (listFont != null) {
            this.listFont = listFont;
        }
    }

    public Integer getListFontSize() {
        return listFontSize;
    }

    public void setListFontSize(Integer listFontSize) {
        if (listFontSize != null) {
            this.listFontSize = listFontSize;
        }
    }

    public Color getListColor() {
        return listColor;
    }

    public void setListColor(Color listColor) {
        this.listColor = listColor;
    }

    public void setListColor(Integer listColor) {
        if (listColor != null) {
            this.listColor = new Color(listColor);
        }
    }

    public Integer getListStyle() {
        return listStyle;
    }

    public void setListStyle(Integer listStyle) {
        if (listStyle != null) {
            this.listStyle = listStyle;
        }
    }

    public Boolean getListUnderline() {
        return listUnderline;
    }

    public void setListUnderline(Boolean listUnderline) {
        if (listUnderline != null) {
            this.listUnderline = listUnderline;
        }
    }

    public Boolean getGradientBG() {
        return gradientBG;
    }

    public void setGradientBG(Boolean gradientBG) {
        if (gradientBG != null) {
            this.gradientBG = gradientBG;
        }
    }

    public Float getContentPercentage() {
        return contentPercentage;
    }

    public void setContentPercentage(Float contentPercentage) {
        if (contentPercentage != null) {
            this.contentPercentage = contentPercentage;
        }
    }
}
