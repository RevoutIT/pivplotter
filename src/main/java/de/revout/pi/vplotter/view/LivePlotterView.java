package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JPanel;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.main.DriverMoveObserverIf;

public class LivePlotterView extends JPanel implements DriverMoveObserverIf {

    private static final long serialVersionUID = 1L;
    
    // Offset für den Zeichenbereich (Rand)
    private final int offset = 2;
    
    // Wird beim ersten Empfang von Bewegungsdaten verwendet, um die Liste zu leeren
    private boolean shouldClear = true;
    
    // Liste der zu zeichnenden Punkte. Jedes Element enthält:
    // [0]: state, [1]: x-Koordinate (bezogen auf den Nullpunkt), [2]: y-Koordinate (bezogen auf den Nullpunkt)
    private final ArrayList<double[]> pointList = new ArrayList<>();
    
    // Abmessungen des Plotters (werden beim ersten Aufruf von currentMove aktualisiert)
    private double plotterWidth = 1;
    private double plotterHeight = 1;

    public LivePlotterView() {
        super();
        setBackground(MainView.COLOR3);
        Driver.getCurrent().addDriverMoveObserverIf(this);
    }

    @Override
    public void currentMove(int state, Pair toPoint, int stepCount, int actualStep) {
        synchronized (pointList) {
            if (shouldClear) {
                pointList.clear();
                shouldClear = false;
                double[] plotterSide = Driver.getCurrent().getPlotterSide();
                plotterWidth = plotterSide[0];
                plotterHeight = plotterSide[1];
            }
            // Korrigiere die Koordinaten anhand des Nullpunkts des Plotters
            double[] point = new double[3];
            point[0] = state;
            point[1] = toPoint.getX() - Driver.getCurrent().getNullPoint().getX();
            point[2] = toPoint.getY() - Driver.getCurrent().getNullPoint().getY();
            pointList.add(point);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        synchronized (pointList) {
            int clipWidth = g.getClipBounds().width - 7;
            int clipHeight = g.getClipBounds().height - 7;
            double scaleX = plotterWidth / clipWidth;
            double scaleY = plotterHeight / clipHeight;
            // Wähle den maximalen Skalierungsfaktor, um das komplette Plotter-Layout abzubilden
            double scale = Math.max(scaleX, scaleY);

            int drawWidth = (int) (plotterWidth / scale) + offset * 2;
            int drawHeight = (int) (plotterHeight / scale) + offset * 2;
            
            // Fülle den Hintergrund weiß
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, drawWidth, drawHeight);
            g.setColor(Color.BLACK);

            // Zeichne Linien, die die Punkte verbinden, sofern der state (point[0]) 1 ist.
            int prevX = -1, prevY = -1;
            for (double[] point : pointList) {
                int currentX = (int) (point[1] / scale) + offset;
                int currentY = (int) (point[2] / scale) + offset;
                if (point[0] == 1 && prevX != -1 && prevY != -1) {
                    g.drawLine(prevX, prevY, currentX, currentY);
                }
                // Aktualisiere die vorherigen Koordinaten für den nächsten Durchlauf
                prevX = currentX;
                prevY = currentY;
            }
        }
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
        repaint();
    }
}
