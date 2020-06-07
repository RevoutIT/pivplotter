package de.revout.pi.vplotter.imagefilter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import de.revout.pi.vplotter.lang.Dictionary;
import de.revout.pi.vplotter.setting.ComboSetting;
import de.revout.pi.vplotter.setting.Setting;

public class RotateFilter implements Filter {

	private ComboSetting comboSetting;
	private List<Setting> list;
	private static final String NEUNZIG = "90"; //$NON-NLS-1$
	private static final String HUNDERT80 = "180"; //$NON-NLS-1$
	private static final String ZWEIHUNDER70 = "270"; //$NON-NLS-1$
	
	
	public RotateFilter() {
		comboSetting = new ComboSetting(Dictionary.getCurrent().getString("RotateFilter.SettingsName"), NEUNZIG); //$NON-NLS-1$
		comboSetting.getValueList().add(NEUNZIG);
		comboSetting.getValueList().add(HUNDERT80);
		comboSetting.getValueList().add(ZWEIHUNDER70);
		list = new ArrayList<>();
		list.add(comboSetting);
	}

	@Override
	public BufferedImage filter(BufferedImage paramImage) {
		BufferedImage filterimage = paramImage;
		BufferedImage newImage = null;
		int counter = 0;
		switch (comboSetting.getValue()) {
		case (NEUNZIG):
			counter = 1;
			break;
		case (HUNDERT80):
			counter = 2;
			break;
		case (ZWEIHUNDER70):
			counter = 3;
			break;
		}
		for (int i = 0; counter > i;i++) {
			newImage = new BufferedImage(filterimage.getHeight(), filterimage.getWidth(), paramImage.getType());
			for (int x = 0; x < filterimage.getWidth(); x++) {
				for (int y = 0; y < filterimage.getHeight(); y++) {
					newImage.setRGB(y, (filterimage.getWidth() - 1) - x, filterimage.getRGB(x, y));
				}
			}
			filterimage = newImage;
		}
		return newImage;
	}
	@Override
	public List<Setting> getSettings() {
		return list;
	}

	@Override
	public String getName() {
		return Dictionary.getCurrent().getString("RotateFilter.FilterName"); //$NON-NLS-1$
	}
	
	@Override
	public Filter createNewFilter() {
		return new RotateFilter();
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
