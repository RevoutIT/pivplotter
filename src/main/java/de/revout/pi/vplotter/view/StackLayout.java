package de.revout.pi.vplotter.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

class StackLayout implements LayoutManager {

    /** Standardgröße für einen leeren Container */
    private static final Dimension EMPTY_SIZE = new Dimension(100, 100);
    
    /** Aktuell sichtbare Komponente oder null, wenn keine Komponente sichtbar ist */
    private Component visibleComponent = null;

    /**
     * Zeigt die übergebene Komponente im Container an.
     * Ist der Parameter null, so werden alle enthaltenen Komponenten versteckt.
     *
     * @param comp   die anzuzeigende Komponente
     * @param parent der übergeordnete Container
     */
    public void showComponent(Component comp, Container parent) {
        if (visibleComponent != comp) {
            if (comp != null && !parent.isAncestorOf(comp)) {
                parent.add(comp);
            }
            synchronized (parent.getTreeLock()) {
                if (visibleComponent != null) {
                    visibleComponent.setVisible(false);
                }
                visibleComponent = comp;
                if (comp != null) {
                    comp.setVisible(true);
                }
                // Aktualisiert das Layout des Containers
                parent.validate(); // Alternativ: parent.revalidate();
            }
        }
    }

    /**
     * Liefert die aktuell sichtbare Komponente.
     *
     * @return die sichtbare Komponente oder null
     */
    public Component getVisibleComponent() {
        return visibleComponent;
    }

    @Override
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            comp.setVisible(false);
            // Wenn diese Komponente bereits als sichtbar markiert war, aufheben
            if (comp == visibleComponent) {
                visibleComponent = null;
            }
        }
    }

    @Override
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == visibleComponent) {
                visibleComponent = null;
            }
            // Entfernte Komponente wird wieder sichtbar geschaltet,
            // um Problemen mit versteckten Komponenten vorzubeugen.
            comp.setVisible(true);
        }
    }

    @Override
    public void layoutContainer(Container parent) {
        if (visibleComponent != null) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                int width = parent.getWidth() - insets.left - insets.right;
                int height = parent.getHeight() - insets.top - insets.bottom;
                visibleComponent.setBounds(insets.left, insets.top, width, height);
            }
        }
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
        return (visibleComponent == null) ? EMPTY_SIZE : preferredLayoutSize(parent);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        Dimension currentSize = parent.getSize();
        if (visibleComponent != null) {
            Dimension compPrefSize = visibleComponent.getPreferredSize();
            currentSize.width = Math.max(currentSize.width, compPrefSize.width);
            currentSize.height = Math.max(currentSize.height, compPrefSize.height);
        } else if (currentSize.width == 0 || currentSize.height == 0) {
            return EMPTY_SIZE;
        }
        return currentSize;
    }
}
