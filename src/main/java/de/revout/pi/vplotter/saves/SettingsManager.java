package de.revout.pi.vplotter.saves;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

public class SettingsManager {

	private static final String SETTINGS_PROPERTY = "./conf/programm.settings"; //$NON-NLS-1$
	private static SettingsManager current;
	private Properties properties;

	public static enum KEY {
		FILEPATH, POTRACE, SIMULATION, CONFIG, LAST_USED
	}

	private SettingsManager() {
		init();
	}

	public static SettingsManager getCurrent() {
		if (current == null) {
			return current = new SettingsManager();
		}
		return current;
	}

	private void init() {
		properties = new Properties();
		if (new File(SETTINGS_PROPERTY).exists()) {
			try (FileInputStream is = new FileInputStream(Paths.get(SETTINGS_PROPERTY).toFile())) {
				properties.loadFromXML(is);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

	}

	public synchronized void save() throws IOException {
		
		File file = new File(SETTINGS_PROPERTY);
		File toFile = new File(SETTINGS_PROPERTY + "_"); //$NON-NLS-1$
		file.renameTo(toFile);
		try (FileOutputStream output = new FileOutputStream(Paths.get(SETTINGS_PROPERTY).toFile())) {
			properties.storeToXML(output, null);
			toFile.delete();
		} catch (Exception e) {
			toFile.renameTo(file);
			e.printStackTrace();
		}
	}

	public String getValue(KEY paramKey) {
		return properties.getProperty(paramKey.name().toLowerCase());
	}

	public void setValue(KEY paramKey, String paramValue) throws IOException {
		if (paramValue != null && !paramValue.equals(getValue(paramKey))) {
			properties.setProperty(paramKey.name().toLowerCase(), paramValue);
		}
	}

}
