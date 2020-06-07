package de.revout.pi.vplotter.imagefilter;

import java.awt.image.BufferedImage;
import java.util.List;

import de.revout.pi.vplotter.setting.Setting;

public interface Filter {
	public BufferedImage filter(BufferedImage paramImage);
	public Setting getSetting(Setting paramSetting);
	public List<Setting> getSettings();
	public String getName();
	public Filter createNewFilter();
}
