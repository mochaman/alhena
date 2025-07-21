

package brad.grier.alhena;

import java.awt.Component;
import java.awt.Container;
import java.awt.DefaultFocusTraversalPolicy;

public class ClassFocusTraversalPolicy extends DefaultFocusTraversalPolicy {
    private final Class<? extends Component> classToSkip;

    public ClassFocusTraversalPolicy(Class<? extends Component> classToSkip) {
        this.classToSkip = classToSkip;
    }

    @Override
    public Component getComponentAfter(Container focusCycleRoot, Component aComponent) {
        Component next = super.getComponentAfter(focusCycleRoot, aComponent);
        while (isSkipClass(next)) {
            next = super.getComponentAfter(focusCycleRoot, next);
        }
        return next;
    }

    @Override
    public Component getComponentBefore(Container focusCycleRoot, Component aComponent) {
        Component prev = super.getComponentBefore(focusCycleRoot, aComponent);
        while (isSkipClass(prev)) {
            prev = super.getComponentBefore(focusCycleRoot, prev);
        }
        return prev;
    }

    @Override
    public Component getDefaultComponent(Container focusCycleRoot) {
        Component def = super.getDefaultComponent(focusCycleRoot);
        return isSkipClass(def) ? getComponentAfter(focusCycleRoot, def) : def;
    }

    @Override
    public boolean accept(Component c) {
        return super.accept(c) && !isSkipClass(c);
    }

    private boolean isSkipClass(Component c) {
        return classToSkip.isInstance(c);
    }
}
