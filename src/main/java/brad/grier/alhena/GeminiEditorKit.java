package brad.grier.alhena;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class GeminiEditorKit extends StyledEditorKit {

    private final ViewFactory defaultFactory = new GeminiViewFactory();

    @Override
    public ViewFactory getViewFactory() {
        return defaultFactory;
    }

    static class GeminiViewFactory implements ViewFactory {

        @Override
        public View create(Element elem) {

            String kind = elem.getName();
            if (kind != null) {
                switch (kind) {
                    case AbstractDocument.ContentElementName -> {
                        return new GeminiLabelView(elem);
                    }
                    case AbstractDocument.ParagraphElementName -> {
                        return new ParagraphView(elem);
                    }
                    case AbstractDocument.SectionElementName -> {
                        return new BoxView(elem, View.Y_AXIS);
                    }
                    case StyleConstants.ComponentElementName -> {
                        return new ComponentView(elem);
                    }
                    case StyleConstants.IconElementName -> {
                        return new IconView(elem);
                    }
                }
            }
            return new LabelView(elem);
        }
    }

    // view handles line wrapping — or not — based on the style
    static class GeminiLabelView extends LabelView {

        public GeminiLabelView(Element elem) {
            super(elem);
        }

        @Override
        public float getMinimumSpan(int axis) {
            AttributeSet attrs = getAttributes();
            if (axis == View.X_AXIS && !GeminiTextPane.wrapPF && isPreformatted(attrs)) {
                return super.getPreferredSpan(axis);
            }
            return super.getMinimumSpan(axis); // allow wrap
        }

        private boolean isPreformatted(AttributeSet attrs) {
            Object name = attrs.getAttribute(StyleConstants.NameAttribute);

            return "```".equals(name);
        }
    }
}
