package brad.grier.alhena;

import java.awt.EventQueue;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

public class PopupMenuButton extends JButton {

    private MouseAdapter ma;
    private final Supplier<List<JMenuItem>> menuItemSupplier;
    private final String emptyMessage;

    public PopupMenuButton(String text, Supplier<List<JMenuItem>> menuItemSupplier, String emptyMessage) {
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
                    JPopupMenu popupMenu = new JPopupMenu();
                    PopupMenuListener pml = new PopupMenuListener() {
                        @Override
                        public void popupMenuWillBecomeVisible(PopupMenuEvent pme) {

                            List<JMenuItem> mList = menuItemSupplier.get();
                            if (mList.isEmpty()) {
                                popupMenu.add(new JMenuItem(emptyMessage));
                            }
                            for (JMenuItem item : mList) {
                                popupMenu.add(item);
                            }

                        }

                        @Override
                        public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {

                            EventQueue.invokeLater(() -> {
                                addMouseListener(ma);
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
        addActionListener(e -> {

            JPopupMenu popupMenu = new JPopupMenu();
            List<JMenuItem> mList = menuItemSupplier.get();
            if (mList.isEmpty()) {
                popupMenu.add(new JMenuItem(emptyMessage));
            }
            for (JMenuItem item : mList) {
                popupMenu.add(item);
            }
            popupMenu.show(PopupMenuButton.this, 0, getHeight());

        });
    }
}
