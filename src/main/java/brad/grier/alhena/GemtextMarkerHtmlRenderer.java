package brad.grier.alhena;
import java.util.HashSet;
import java.util.Set;

import org.commonmark.node.Code;
import org.commonmark.node.Emphasis;
import org.commonmark.node.Node;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlWriter;

// the idea here is to convert markdown to html while using the gemtext markers supported in Alhena
public class GemtextMarkerHtmlRenderer implements NodeRenderer {
    private final HtmlNodeRendererContext context;
    private final HtmlWriter htmlWriter;

    public GemtextMarkerHtmlRenderer(HtmlNodeRendererContext context) {
        this.context = context;
        this.htmlWriter = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes() {
        Set<Class<? extends Node>> types = new HashSet<>();
        types.add(StrongEmphasis.class); // Markdown **bold**
        types.add(Emphasis.class);       // Markdown *italic* or _italic_
        types.add(Code.class);           // Markdown `inline code`
        return types;
    }

    @Override
    public void render(Node node) {
        if (node instanceof StrongEmphasis) {
            htmlWriter.raw("*");
            renderChildren(node);
            htmlWriter.raw("*");
            
        } else if (node instanceof Emphasis) {
            // MATH PROTECTION GUARD
            if (isMathExpression((Emphasis) node)) {
                // restore the original asterisks as literal characters 
                // so they are not eaten or turned into underscores!
                htmlWriter.raw("*");
                renderChildren(node);
                htmlWriter.raw("*");
            } else {
                // process normally as standard italic text
                htmlWriter.raw("_");
                renderChildren(node);
                htmlWriter.raw("_");
            }
            
        } else if (node instanceof Code) {
            htmlWriter.raw("`");
            htmlWriter.text(((Code) node).getLiteral());
            htmlWriter.raw("`");
        }
    }

    // identifies if Commonmark split a math expression (like 2*2*5) into an emphasis block.
    private boolean isMathExpression(Emphasis emphasisNode) {
        Node previous = emphasisNode.getPrevious();
        Node next = emphasisNode.getNext();

        if (previous instanceof Text && next instanceof Text) {
            String prevText = ((Text) previous).getLiteral();
            String nextText = ((Text) next).getLiteral();

            if (!prevText.isEmpty() && Character.isDigit(prevText.charAt(prevText.length() - 1)) &&
                !nextText.isEmpty() && Character.isDigit(nextText.charAt(0))) {
                return true; 
            }
        }
        return false;
    }

    private void renderChildren(Node parent) {
        Node node = parent.getFirstChild();
        while (node != null) {
            Node next = node.getNext();
            context.render(node);
            node = next;
        }
    }
}
