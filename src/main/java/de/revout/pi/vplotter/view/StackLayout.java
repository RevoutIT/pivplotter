package de.revout.pi.vplotter.view;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

class StackLayout implements LayoutManager {
    /**
     * Default size when no components are contained
     */
    private static Dimension emptySize = null;
    /**
     * Holds currently visible component or null if no comp is visible
     */
    private Component visibleComp = null;

    /**
     * Set the currently displayed component.  If passed null for the component,
     * all contained components will be made invisible (sliding windows do this)
     */
    public void showComponent(Component c, Container parent) {
        if (visibleComp != c) {
            if (!parent.isAncestorOf(c) && c != null) {
                parent.add(c);
            }
            synchronized (parent.getTreeLock()) {
                if (visibleComp != null) {
                    visibleComp.setVisible(false);
                }
                visibleComp = c;
                if (c != null) {
                    visibleComp.setVisible(true);
                }
                // trigger re-layout
                parent.validate(); //XXX revalidate should work!
            }
        }
    }
    
    /** Allows support for content policies */
    public Component getVisibleComponent() {
        return visibleComp;
    }

    /**
     * ********** Implementation of LayoutManager interface *********
     */

    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            comp.setVisible(false);
            // keep consistency if showComponent was already called on this
            // component before
            if (comp == visibleComp) {
                visibleComp = null;
            }
        }
    }
    
    
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            if (comp == visibleComp) {
                visibleComp = null;
            }
            // kick out removed component as visible, so that others
            // don't have problems with hidden components
            comp.setVisible(true);
        }
    }

    public void layoutContainer(Container parent) {
        if (visibleComp != null) {
            synchronized (parent.getTreeLock()) {
                Insets insets = parent.getInsets();
                visibleComp.setBounds(insets.left, insets.top, parent.getWidth()
                   - (insets.left + insets.right), parent.getHeight()
                   - (insets.top + insets.bottom));
            }
        }
    }

    public Dimension minimumLayoutSize(Container parent) {
        return visibleComp == null ?
                getEmptySize() : preferredLayoutSize(parent);
    }

    public Dimension preferredLayoutSize(Container parent) {
        Dimension d = parent.getSize();
        if (visibleComp != null) {
            Dimension d1 = visibleComp.getPreferredSize();
            d.width = Math.max(d.width, d1.width);
            d.height = Math.max(d.height, d1.height);
        } else {
            if (d.width == 0) {
                return getEmptySize();
            }
        }
        return d;
    }

    /**
     * Specifies default size of empty container
     */
    private static Dimension getEmptySize() {
        if (emptySize == null) {
            emptySize = new Dimension(100, 100);
        }
        return emptySize;
    }

}