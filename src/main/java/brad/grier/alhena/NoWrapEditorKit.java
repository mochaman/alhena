
package brad.grier.alhena;

import javax.swing.text.Element;
import javax.swing.text.ParagraphView;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

public class NoWrapEditorKit extends StyledEditorKit {

    @Override
    public ViewFactory getViewFactory() {

        final ViewFactory defaultFactory = super.getViewFactory();

        return (Element elem) -> {
            View v = defaultFactory.create(elem);
            if (v instanceof ParagraphView) {
                return new NoWrapParagraphView(elem);
            }
            return v;
        };
    }

    static class NoWrapParagraphView extends ParagraphView {

        public NoWrapParagraphView(Element element) {
            super(element);
        }

        @Override
        public void layout(int width, int height) {
            super.layout(Integer.MAX_VALUE, height);
        }

        @Override
        public float getMinimumSpan(int axis) {
            return getPreferredSpan(axis);
        }
    }
}
