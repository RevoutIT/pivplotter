package de.revout.pi.vplotter.view;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;

import javax.swing.JPanel;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.main.Driver;
import de.revout.pi.vplotter.main.DriverMoveObserverIf;

public class LivePlotterView extends JPanel implements DriverMoveObserverIf {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private int lastX;
	private int lastY;
	private boolean clear;
	ArrayList<double[]> pairList;
	private double plotterWidth = 1;
	private double plotterHeight = 1;
	int x = 2;

	public LivePlotterView() {
		super();
		setBackground(MainView.COLOR3);
		Driver.getCurrent().addDriverMoveObserverIf(this);
		lastX = 0;
		lastY = 0;
		pairList = new ArrayList<>();
		clear = true;
	}

	@Override
	public void currentMove(int paramState, Pair paramToPoint, int paramStepCount, int paramActualStep) {
		if (clear) {
			synchronized (pairList) {
				pairList.clear();
				clear = false;
				plotterWidth = Driver.getCurrent().getPlotterSide()[0];
				plotterHeight = Driver.getCurrent().getPlotterSide()[1];
			}
		}
		double[] point = new double[3];
		point[0] = paramState;
		point[1] = paramToPoint.getX() - Driver.getCurrent().getNullPoint().getX();
		point[2] = paramToPoint.getY() - Driver.getCurrent().getNullPoint().getY();
		pairList.add(point);
		repaint();
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);

		synchronized (pairList) {
			double scaleX = plotterWidth / (g.getClipBounds().getWidth()-7);
			double scaleY =  plotterHeight / (g.getClipBounds().getHeight()-7);
			double scale = Math.max(scaleX,scaleY);
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, (int)(plotterWidth/scale)+x*2, (int)(plotterHeight/scale)+x*2);
			g.setColor(Color.BLACK);

			
			for (int i = 0; i < pairList.size(); i++) {
				double[] point = pairList.get(i);
				if(point!=null) {
					if (1 == point[0]) {
						g.drawLine(lastX, lastY, (int) (point[1] / scale)+x, (int) (point[2] / scale)+x);
					}
	
					lastX = (int) (point[1] / scale)+x;
					lastY = (int) (point[2] / scale)+x;
				}
			}

		}
	}

	@Override
	public void finish() {
		clear = true;
	}

	@Override
	public void init() {
		pairList.clear();
		repaint();
	}

}
