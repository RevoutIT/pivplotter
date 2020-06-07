package de.revout.pi.vplotter.saves;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class VPlotterPropertiesManager {

	private static VPlotterPropertiesManager current;
	private Properties properties;
	
	//Default Werte
	private final String DEFAULTMINX = "255"; //$NON-NLS-1$
	private final String DEFAULTMINY = "196"; //$NON-NLS-1$
	private final String DEFAULTPLOTTERWIDTH = "920"; //$NON-NLS-1$
	private final String DEFAULTPLOTTERHEIGHT = "600"; //$NON-NLS-1$
	private final String DEFAULTSPACEBETWEEN = "1451"; //$NON-NLS-1$
	private final String DEFAULTSECTORLENGTH = "0.4"; //$NON-NLS-1$
	private final String DEFAULTWAITTIME = "3000"; //$NON-NLS-1$
	private final String DEFAULTSTARTPOINTX = "732.6"; //$NON-NLS-1$
	private final String DEFAULTSTARTPOINTY = "800.6"; //$NON-NLS-1$
	private final String DEFAULTSTEPSPERMM = "160.4278074866310160427807486631"; //$NON-NLS-1$
	private final String DEFAULTWAITTIMEDRAW = "0.5"; //$NON-NLS-1$
	private final String DEFAULTWAITTIMENODRAW = "0.3"; //$NON-NLS-1$
	
	public static enum KEY {
		MINX,MINY,PLOTTERWIDTH,PLOTTERHEIGHT,SPACEBETWEEN,SECTORLENGTH,WAITTIME,STARTPOINTX,STARTPOINTY,STEPSPERMM,WAITTIMEDRAW,WAITTIMENODRAW,PAPERHEIGHT,PAPERWIDTH
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
			setValue(KEY.MINX, DEFAULTMINX, newProperties);
			setValue(KEY.MINY, DEFAULTMINY, newProperties);
			setValue(KEY.PLOTTERWIDTH, DEFAULTPLOTTERWIDTH, newProperties);
			setValue(KEY.PLOTTERHEIGHT, DEFAULTPLOTTERHEIGHT, newProperties);
			setValue(KEY.SPACEBETWEEN, DEFAULTSPACEBETWEEN, newProperties);
			setValue(KEY.SECTORLENGTH, DEFAULTSECTORLENGTH, newProperties);
			setValue(KEY.STEPSPERMM, DEFAULTSTEPSPERMM, newProperties);
			setValue(KEY.WAITTIME, DEFAULTWAITTIME, newProperties);
			setValue(KEY.STARTPOINTX, DEFAULTSTARTPOINTX, newProperties);
			setValue(KEY.STARTPOINTY, DEFAULTSTARTPOINTY, newProperties);
			setValue(KEY.WAITTIMEDRAW, DEFAULTWAITTIMEDRAW, newProperties);
			setValue(KEY.WAITTIMENODRAW, DEFAULTWAITTIMENODRAW, newProperties);
			save(paramFile, newProperties);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
