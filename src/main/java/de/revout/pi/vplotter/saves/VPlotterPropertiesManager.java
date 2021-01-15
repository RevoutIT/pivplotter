package de.revout.pi.vplotter.saves;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class VPlotterPropertiesManager {

	private static VPlotterPropertiesManager current;
	private Properties properties;
	
	public static enum KEY {
		MINX,MINY,PLOTTERWIDTH,PLOTTERHEIGHT,SPACEBETWEEN,SECTORLENGTH,STARTPOINTX,STARTPOINTY,STEPSPERMM,PAPERHEIGHT,PAPERWIDTH,STEPPAUSEDRAW,STEPPAUSESLACK,DRAWPAUSE
	}
	
	private VPlotterPropertiesManager() {
		init();
	}

	public static VPlotterPropertiesManager getCurrent() {
		
		if (current == null) {
			return current = new VPlotterPropertiesManager();
		}
		return current;
	}

	private void init() {
		properties = new Properties();
	}
	
	private void save(File paramFile, Properties paramProperties) throws IOException {
		try (FileOutputStream output = new FileOutputStream(paramFile.toPath().toFile())) {
		if(paramProperties==null) {
			properties.storeToXML(output, null);
		}else {
			paramProperties.storeToXML(output, null);
		}
		}catch (Exception e) {
			e.printStackTrace();
		}
	}

	public String getValue(KEY paramKey) {
		return properties.getProperty(paramKey.name().toLowerCase());
	}

	public void setValue(KEY paramKey, String paramValue, Properties paramProperty){
		if (paramValue!=null) {
			if(paramProperty == null) {
				properties.setProperty(paramKey.name().toLowerCase(), paramValue);
			}else {
				paramProperty.setProperty(paramKey.name().toLowerCase(), paramValue);
			}
		}
	}
	
	public void load(File paramFile) {
		try (FileInputStream is = new FileInputStream(paramFile.toPath().toFile())) {
			properties.clear();
			properties.loadFromXML(is);
			SettingsManager.getCurrent().setValue(de.revout.pi.vplotter.saves.SettingsManager.KEY.CONFIG, paramFile.getAbsolutePath());
			SettingsManager.getCurrent().save();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}
	
	public void newConfig(File paramFile) {
		Properties newProperties = new Properties();
		try {
			setValue(KEY.MINX, "", newProperties);
			setValue(KEY.MINY, "", newProperties);
			setValue(KEY.PLOTTERWIDTH, "", newProperties);
			setValue(KEY.PLOTTERHEIGHT, "", newProperties);
			setValue(KEY.SPACEBETWEEN, "", newProperties);
			setValue(KEY.SECTORLENGTH, "", newProperties);
			setValue(KEY.STEPSPERMM, "", newProperties);
			setValue(KEY.DRAWPAUSE,"" , newProperties);
			setValue(KEY.STARTPOINTX, "", newProperties);
			setValue(KEY.STARTPOINTY, "", newProperties);
			setValue(KEY.STEPPAUSEDRAW,"" , newProperties);
			setValue(KEY.STEPPAUSESLACK,"", newProperties);
			save(paramFile, newProperties);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
