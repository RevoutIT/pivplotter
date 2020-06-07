/**
 * 
 */
package de.revout.pi.vplotter.imagefilter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.setting.Setting;
import de.revout.pi.vplotter.setting.SliderSetting;

/**
 * @author User
 *
 */
public class EdgeDetectionFilter implements Filter {

	private SliderSetting sliderSetting;
	private List<Setting> settingsList;
	
	
	public EdgeDetectionFilter() {
		sliderSetting = new SliderSetting(Dictionary.getCurrent().getString("EdgeDetectionFilter.SettingsName"), "50", 0, 255, 1); //$NON-NLS-1$ //$NON-NLS-2$
		settingsList = new ArrayList<>();
		settingsList.add(sliderSetting);
	}
	
	/* (non-Javadoc)
	 * @see de.revout.imageFilter.Filter#filter(java.awt.image.BufferedImage)
	 */
	@Override
	public BufferedImage filter(BufferedImage paramImage) {
		BufferedImage image=null;
		
		try {
			
			int[][] filter1= {
					{-1,0,1},
					{-2,0,2},
					{-1,0,1}
			};
			int[][] filter2= {
					{1,2,1},
					{0,0,0},
					{-1,-2,-1}
			};

			
			image = new BufferedImage(paramImage.getWidth(),paramImage.getHeight(), paramImage.getType());
			
			double[][] lum = luminanz(paramImage);
			
			int width = paramImage.getWidth();
			int height = paramImage.getHeight();
			
			for(int x=1;x<width-1;x++) {
				for(int y=1;y<height-1;y++) {
					int gx=0;
					int gy=0;
					for(int i=-1;i<2;i++) {
						for(int j=-1;j<2;j++) {
							gx += (int)Math.round((lum[x+i][y+j]*filter1[1+i][1+j]));
							gy += (int)Math.round((lum[x+i][y+j]*filter2[1+i][1+j]));
						}
					}
					
					int gr = cut((int)Math.sqrt(gx*gx+gy*gy));
					image.setRGB(x, y, new Color(gr,gr,gr).getRGB());
				}
			}
			
			
		}catch(Exception exc) {
			exc.printStackTrace();
		}
		return image;
	}

	/* (non-Javadoc)
	 * @see de.revout.imageFilter.Filter#getSettings()
	 */
	@Override
	public List<Setting> getSettings() {
		return settingsList;
	}

	/* (non-Javadoc)
	 * @see de.revout.imageFilter.Filter#getName()
	 */
	@Override
	public String getName() {
		return this.getClass().getSimpleName().toUpperCase();
	}

	/* (non-Javadoc)
	 * @see de.revout.imageFilter.Filter#createNewFilter()
	 */
	@Override
	public Filter createNewFilter() {
		return new EdgeDetectionFilter();
	}
	
	private int cut(int paramValue) {
		if(paramValue>Integer.parseInt(sliderSetting.getValue())) {
			return 0;
		}else if(paramValue<Integer.parseInt(sliderSetting.getValue())) {
			return 255; 
		}
		return 255-paramValue;
	}
	
	private double[][] luminanz(BufferedImage paramBufferedImage){
		double[][] result = new double[paramBufferedImage.getWidth()][paramBufferedImage.getHeight()];
		for(int x =0; x<paramBufferedImage.getWidth();x++) {
			for(int y=0;y<paramBufferedImage.getHeight();y++) {
				Color pixel = new Color(paramBufferedImage.getRGB(x, y));
				result[x][y]=0.299*pixel.getRed()+0.587*pixel.getGreen()+0.114*pixel.getBlue();
			}
		}
		return result;
	}

	@Override
	public Setting getSetting(Setting paramSetting) {
		for (Setting setting : settingsList) {
			if (paramSetting==setting) {
				return setting;
			}
		}
		return null;
	}
	

	

}
