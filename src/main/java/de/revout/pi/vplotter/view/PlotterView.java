package de.revout.pi.vplotter.view;

import java.awt.Graphics;
import java.awt.Image;

import javax.swing.JPanel;

import de.revout.pi.vplotter.model.Model;

public class PlotterView extends JPanel {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PlotterView() {
		super();
		setBackground(MainView.COLOR3);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	
		Image image = Model.getCurrent().getSvgImage();
		if (image != null) {
			
			double imageProp = (double)image.getWidth(null)/(double)image.getHeight(null);
			
			int newHeight = (int) (((double)getWidth())/imageProp);
			double scall = 1;
			if(getHeight()<newHeight) {
				scall = (double)newHeight/(double)getHeight();
				int xDiff = (getWidth()-(int)(getWidth()/scall))/2;
				g.drawImage(image, xDiff, 0 , (int)(getWidth()/scall)+xDiff, (int)(((double)getWidth()/imageProp)/scall) ,0,0,image.getWidth(null),image.getHeight(null),null );
			}else {
			
			if(imageProp>=1) {
				int yDiff = (getHeight()-(int)((double)getWidth()/imageProp))/2;
				g.drawImage(image, 0, yDiff , getWidth(), (int)((double)getWidth()/imageProp)+yDiff ,0,0,image.getWidth(null),image.getHeight(null),null );
			}else {
				int xDiff = (getWidth()-(int)((double)getHeight()*imageProp))/2;
				g.drawImage(image, xDiff,0, (int)((double)getHeight()*imageProp)+xDiff, getHeight() ,0,0,image.getWidth(null),image.getHeight(null),null );
			}
			}
		
		}	
	}

}
