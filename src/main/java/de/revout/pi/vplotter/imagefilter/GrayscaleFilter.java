package de.revout.pi.vplotter.imagefilter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.setting.ComboSetting;
import de.revout.pi.vplotter.setting.Setting;
import de.revout.pi.vplotter.setting.SliderSetting;

public class GrayscaleFilter implements Filter {
	private ComboSetting comboSetting;
	private SliderSetting sliderSetting;
	private List<Setting> list;
	private static final String NORMAL = "Normal"; //$NON-NLS-1$
	private static final String GREY_IS_R = "Grey is R"; //$NON-NLS-1$
	private static final String GREY_IS_G = "Grey is G"; //$NON-NLS-1$
	private static final String GREY_IS_B = "Grey is B"; //$NON-NLS-1$
	private static final String GREY_IS_R_G_B_3 = "Grey is (R+G+B)/3"; //$NON-NLS-1$

	public GrayscaleFilter() {

		comboSetting = new ComboSetting(Dictionary.getCurrent().getString("GrayscaleFilter.SettingsName"), NORMAL); //$NON-NLS-1$
		comboSetting.getValueList().add(NORMAL);
		comboSetting.getValueList().add(GREY_IS_R);
		comboSetting.getValueList().add(GREY_IS_G);
		comboSetting.getValueList().add(GREY_IS_B);
		comboSetting.getValueList().add(GREY_IS_R_G_B_3);
		list = new ArrayList<>();
		list.add(comboSetting);
		
		sliderSetting = new SliderSetting(Dictionary.getCurrent().getString("GrayscaleFilter.Cut"), "0", 0, 255, 1); //$NON-NLS-1$ //$NON-NLS-2$
		list.add(sliderSetting);
	}

	@Override
	public BufferedImage filter(BufferedImage paramImage) {
		BufferedImage newImage = null;
		switch (comboSetting.getValue()) {
		case (NORMAL):
			newImage = new BufferedImage(paramImage.getWidth(), paramImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < paramImage.getWidth(); x++) {
				for (int y = 0; y < paramImage.getHeight(); y++) {
					int rgb = paramImage.getRGB(x, y);
					int alpha = (rgb >> 24) & 0xff;
					int red = (rgb >> 16) & 0xff;
					int green = (rgb >> 8) & 0xff;
					int blue = rgb & 0xff;
					int avg = (int) ((0.21*red) + (0.72*green) + (0.07*blue));
					int grey = (alpha << 24) | (avg << 16) | (avg << 8) | avg;
					newImage.setRGB(x, y, grey);
				}
			}
			break;
		case (GREY_IS_R):
			newImage = generateGreyWith1BaseColor(paramImage, GREY_IS_R);
			break;
		case (GREY_IS_G):
			newImage = generateGreyWith1BaseColor(paramImage, GREY_IS_G);
			break;
		case (GREY_IS_B):
			newImage = generateGreyWith1BaseColor(paramImage, GREY_IS_B);
			break;
		case (GREY_IS_R_G_B_3):
			newImage = new BufferedImage(paramImage.getWidth(), paramImage.getHeight(), BufferedImage.TYPE_INT_RGB);
			for (int x = 0; x < paramImage.getWidth(); x++) {
				for (int y = 0; y < paramImage.getHeight(); y++) {
					int rgb = paramImage.getRGB(x, y);
					int alpha = (rgb >> 24) & 0xff;
					int red = (rgb >> 16) & 0xff;
					int green = (rgb >> 8) & 0xff;
					int blue = rgb & 0xff;
					int avg = (red + green + blue) / 3;
					int grey = (alpha << 24) | (avg << 16) | (avg << 8) | avg;
					newImage.setRGB(x, y, grey);
				}
			}
			break;

		}
		return newImage;
	}

	private BufferedImage generateGreyWith1BaseColor(BufferedImage paramImage, String paramColor) {
		BufferedImage newImage;
		newImage = new BufferedImage(paramImage.getWidth(), paramImage.getHeight(), BufferedImage.TYPE_INT_RGB);
		for (int x = 0; x < paramImage.getWidth(); x++) {
			for (int y = 0; y < paramImage.getHeight(); y++) {
				int rgb = paramImage.getRGB(x, y);
				int red = (rgb >> 16) & 0xff;
				int green = (rgb >> 8) & 0xff;
				int blue = rgb & 0xff;
				int color = 0;
				switch (paramColor) {
				case (GREY_IS_R):
					color = red;
					break;
				case (GREY_IS_G):
					color = green;
					break;
				case (GREY_IS_B):
					color = blue;
					break;
				}
				
				if(color<Integer.parseInt(sliderSetting.getValue())) {
					color = 0;
				}
				newImage.setRGB(x, y, new Color(color, color, color).getRGB());
			}

		}
		return newImage;
	}

	@Override
	public List<Setting> getSettings() {
		return list;
	}

	@Override
	public String getName() {
		return Dictionary.getCurrent().getString("GrayscaleFilter.FilterName"); //$NON-NLS-1$
	}
	
	@Override
	public Filter createNewFilter() {
		return new GrayscaleFilter();
	}

	@Override
	public Setting getSetting(Setting paramSetting) {
		for (Setting setting : list) {
			if (paramSetting==setting) {
				return setting;
			}
		}
		return null;
	}

	

	

}
