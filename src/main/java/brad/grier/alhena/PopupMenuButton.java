package brad.grier.alhena;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class PopupMenuButton extends JButton {

    private MouseAdapter ma;
    private ActionListener al;
    private final Supplier<List<JComponent>> menuItemSupplier;
    private final String emptyMessage;

    public PopupMenuButton(String text, Supplier<List<JComponent>> menuItemSupplier, String emptyMessage) {
        super(text);
        this.menuItemSupplier = menuItemSupplier;
        this.emptyMessage = emptyMessage;
        initializeButton();

    }

    private void initializeButton() {

        ma = new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {

                    removeActionListener(al);

                    JPopupMenu popupMenu = new JPopupMenu();
                    PopupMenuListener pml = new PopupMenuListener() {
                        @Override
                        public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {

                            List<JComponent> mList = menuItemSupplier.get();
                            if (mList.isEmpty()) {
                                popupMenu.add(new JMenuItem(emptyMessage));
                            }
                            for (JComponent item : mList) {
                                popupMenu.add(item);
                            }

                        }

                        @Override
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

                            EventQueue.invokeLater(() -> {
                                addMouseListener(ma);
                                addActionListener(al);
                            });
                        }

                        @Override
                        public void popupMenuCanceled(PopupMenuEvent e) {

                        }
                    };
                    popupMenu.addPopupMenuListener(pml);
                    popupMenu.show(PopupMenuButton.this, 0, getHeight());
                    removeMouseListener(ma);
                }
            }
        };
        // mouse listener instead of action listener to avoid double-triggering
        addMouseListener(ma);

        al = (ActionEvent e) -> {
            boolean isKeyboardTriggered = (e.getModifiers() & ActionEvent.MOUSE_EVENT_MASK) == 0;
            if (!isKeyboardTriggered) {
                return;
            }

            removeMouseListener(ma);
            JPopupMenu popupMenu1 = new JPopupMenu();

            PopupMenuListener pml = new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {
                    List<JComponent> mList = menuItemSupplier.get();
                    if (mList.isEmpty()) {
                        popupMenu1.add(new JMenuItem(emptyMessage));
                    }
                    for (JComponent item : mList) {
                        popupMenu1.add(item);
                    }
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

                    EventQueue.invokeLater(() -> {

                        addMouseListener(ma);
                        addActionListener(al);
                    });

                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    
                }
            };
            popupMenu1.addPopupMenuListener(pml);
            popupMenu1.show(PopupMenuButton.this, 0, getHeight());
            removeActionListener(al);
        };

        addActionListener(al);
    }
}
