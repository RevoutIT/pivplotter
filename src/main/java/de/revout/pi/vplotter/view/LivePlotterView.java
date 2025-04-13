package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import javax.swing.JPanel;
import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.main.DriverMoveObserverIf;

public class LivePlotterView extends JPanel implements DriverMoveObserverIf {

    private static final long serialVersionUID = 1L;
    
    // Offset (Rand) für die Zeichnung
    private final int offset = 2;
    // Konstante für den Abzug von der Clip-Größe (vermeidet "Magic Number")
    private static final int CLIP_MARGIN = 7;
    
    // Wird beim ersten Empfang von Bewegungsdaten verwendet, um die Punktliste neu zu initialisieren
    private boolean shouldClear = true;
    
    // Liste der Punktdaten: jedes Element enthält [0] = state, [1] = x, [2] = y
    private final ArrayList<double[]> pointList = new ArrayList<>();
    
    // Plotter-Abmessungen (werden bei initialem currentMove aus Driver übernommen)
    private double plotterWidth = 1;
    private double plotterHeight = 1;
    
    // Offscreen-BufferedImage als Zeichen-Puffer
    private BufferedImage offscreenImage = null;
    // Index des letzten Punkts, der in das offscreenImage gezeichnet wurde
    private int lastDrawnIndex = -1;
    
    public LivePlotterView() {
        super();
        setBackground(MainView.COLOR3);
        // Registriere diesen Beobachter beim Driver
        Driver.getCurrent().addDriverMoveObserverIf(this);
    }
    
    @Override
    public void currentMove(int state, Pair toPoint, long stepCount, long actualStep) {
        synchronized (pointList) {
            if (shouldClear) {
                pointList.clear();
                shouldClear = false;
                double[] plotterSide = Driver.getCurrent().getPlotterSide();
                plotterWidth = plotterSide[0];
                plotterHeight = plotterSide[1];
                // Bei Reset den offscreenBuffer zurücksetzen
                offscreenImage = null;
                lastDrawnIndex = -1;
            }
            double[] point = new double[3];
            point[0] = state;
            // Transformiere die Koordinaten relativ zum Nullpunkt
            point[1] = toPoint.getX() - Driver.getCurrent().getNullPoint().getX();
            point[2] = toPoint.getY() - Driver.getCurrent().getNullPoint().getY();
            pointList.add(point);
        }
        // Aktualisiere den Offscreen-Buffer inkrementell und repaint() anschließend
        updateOffscreenImage();
        repaint();
    }
    
    /**
     * Initialisiert den Offscreen-Puffer in der aktuellen Größe des Panels und füllt ihn mit weißem Hintergrund.
     */
    private void initOffscreenImage() {
        offscreenImage = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = offscreenImage.getGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, getWidth(), getHeight());
        g.dispose();
        lastDrawnIndex = -1;
    }
    
    /**
     * Aktualisiert den Offscreen-Puffer, indem nur die neuen Punkte (seit dem letzten Update)
     * inkrementell gezeichnet werden.
     */
    private void updateOffscreenImage() {
        synchronized (pointList) {
            // Bei Größenänderung oder falls offscreenImage noch nicht initialisiert ist, neu erstellen
            if (offscreenImage == null 
                    || offscreenImage.getWidth() != getWidth() 
                    || offscreenImage.getHeight() != getHeight()) {
                initOffscreenImage();
                redrawOffscreenImage();
                return;
            }
            // Berechne die Skalierung (wie in der Originalvariante)
            Rectangle clip = new Rectangle(0, 0, getWidth(), getHeight());
            int clipWidth = clip.width - CLIP_MARGIN;
            int clipHeight = clip.height - CLIP_MARGIN;
            double scaleX = plotterWidth / clipWidth;
            double scaleY = plotterHeight / clipHeight;
            double scale = Math.max(scaleX, scaleY);
            
            Graphics g = offscreenImage.getGraphics();
            g.setColor(Color.BLACK);
            int prevX = -1, prevY = -1;
            // Hole den letzten gezeichneten Punkt, falls vorhanden
            if (lastDrawnIndex >= 0 && lastDrawnIndex < pointList.size()) {
                double[] lastPoint = pointList.get(lastDrawnIndex);
                prevX = (int) (lastPoint[1] / scale) + offset;
                prevY = (int) (lastPoint[2] / scale) + offset;
            }
            // Zeichne alle neuen Punkte
            for (int i = lastDrawnIndex + 1; i < pointList.size(); i++) {
                double[] pt = pointList.get(i);
                int currentX = (int) (pt[1] / scale) + offset;
                int currentY = (int) (pt[2] / scale) + offset;
                if (pt[0] == 1 && prevX != -1 && prevY != -1) {
                    g.drawLine(prevX, prevY, currentX, currentY);
                }
                prevX = currentX;
                prevY = currentY;
                lastDrawnIndex = i;
            }
            g.dispose();
        }
    }
    
    /**
     * Redrawt den gesamten Offscreen-Puffer basierend auf allen in pointList gespeicherten Punkten.
     * Wird etwa beim Resize oder Neuladen verwendet.
     */
    private void redrawOffscreenImage() {
        initOffscreenImage();
        int clipWidth = getWidth() - CLIP_MARGIN;
        int clipHeight = getHeight() - CLIP_MARGIN;
        double scaleX = plotterWidth / clipWidth;
        double scaleY = plotterHeight / clipHeight;
        double scale = Math.max(scaleX, scaleY);
        
        Graphics g = offscreenImage.getGraphics();
        g.setColor(Color.BLACK);
        int prevX = -1, prevY = -1;
        for (double[] pt : pointList) {
            int currentX = (int) (pt[1] / scale) + offset;
            int currentY = (int) (pt[2] / scale) + offset;
            if (pt[0] == 1 && prevX != -1 && prevY != -1) {
                g.drawLine(prevX, prevY, currentX, currentY);
            }
            prevX = currentX;
            prevY = currentY;
            lastDrawnIndex++;
        }
        g.dispose();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Stelle sicher, dass der Offscreen-Puffer existiert und der aktuellen Panel-Größe entspricht
        if (offscreenImage == null 
                || offscreenImage.getWidth() != getWidth() 
                || offscreenImage.getHeight() != getHeight()) {
            initOffscreenImage();
            redrawOffscreenImage();
        }
        g.drawImage(offscreenImage, 0, 0, this);
    }
    
    @Override
    public void finish() {
        shouldClear = true;
    }
    
    @Override
    public void init() {
        synchronized (pointList) {
            pointList.clear();
        }
        offscreenImage = null;
        lastDrawnIndex = -1;
        repaint();
    }
}
