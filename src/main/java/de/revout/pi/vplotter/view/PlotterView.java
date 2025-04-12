package de.revout.pi.vplotter.view;

import java.awt.Graphics;
import java.awt.Image;
import javax.swing.JPanel;
import de.revout.pi.vplotter.model.Model;

public class PlotterView extends JPanel {

    private static final long serialVersionUID = 1L;

    public PlotterView() {
        super();
        setBackground(MainView.COLOR3);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        Image svgImage = Model.getCurrent().getSvgImage();
        if (svgImage == null) {
            return;
        }
        
        // Abmessungen des Panels und des Bildes abrufen
        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = svgImage.getWidth(null);
        int imageHeight = svgImage.getHeight(null);
        
        // Errechne das Seitenverhältnis des Bildes
        double imageAspect = (double) imageWidth / imageHeight;
        
        // Bestimme die Zielabmessungen, sodass das Bild vollständig in das Panel passt
        int drawWidth, drawHeight;
        if ((double)panelWidth / panelHeight > imageAspect) {
            // Höhe ist das begrenzende Element
            drawHeight = panelHeight;
            drawWidth = (int) (panelHeight * imageAspect);
        } else {
            // Breite ist das begrenzende Element
            drawWidth = panelWidth;
            drawHeight = (int) (panelWidth / imageAspect);
        }
        
        // Berechne den Offset zur Zentrierung des Bildes im Panel
        int xOffset = (panelWidth - drawWidth) / 2;
        int yOffset = (panelHeight - drawHeight) / 2;
        
        // Zeichne das Bild skaliert und zentriert
        g.drawImage(svgImage, xOffset, yOffset, drawWidth, drawHeight, this);
    }
}
