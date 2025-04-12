package de.revout.pi.vplotter.view;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

import de.revout.pi.vplotter.model.Model;

public class ImagePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    public ImagePanel() {
        super();
        setBackground(MainView.COLOR3);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Image image = Model.getCurrent().getImage();
        if (image == null) {
            return;
        }
        
        // Dimensionen des Panels und des Bildes abrufen
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = image.getWidth(null);
        int imageHeight = image.getHeight(null);
        
        // Seitenverhältnis des Bildes und des Panels berechnen
        double imageAspect = (double) imageWidth / imageHeight;
        double panelAspect = (double) panelWidth / panelHeight;
        
        // Bestimme die gewünschte Zeichenbreite und -höhe, sodass das Bild passt
        int drawWidth, drawHeight;
        if (panelAspect > imageAspect) {
            // Das Panel ist breiter als das Bild: Höhe als Limit verwenden
            drawHeight = panelHeight;
            drawWidth = (int) (panelHeight * imageAspect);
        } else {
            // Das Panel ist schmaler oder gleich proportioniert: Breite als Limit verwenden
            drawWidth = panelWidth;
            drawHeight = (int) (panelWidth / imageAspect);
        }
        
        // Zentriere das Bild im Panel
        int x = (panelWidth - drawWidth) / 2;
        int y = (panelHeight - drawHeight) / 2;
        
        // Zeichne das Bild skaliert und zentriert
        g.drawImage(image, x, y, drawWidth, drawHeight, this);
    }
}
