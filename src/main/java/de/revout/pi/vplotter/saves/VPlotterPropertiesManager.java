package de.revout.pi.vplotter.saves;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class VPlotterPropertiesManager {

    private final Properties properties;

    public static enum KEY {
        MINX, MINY, PLOTTERWIDTH, PLOTTERHEIGHT, SPACEBETWEEN, SECTORLENGTH, 
        STARTPOINTX, STARTPOINTY, STEPSPERMM, PAPERHEIGHT, PAPERWIDTH, 
        STEPPAUSEDRAW, STEPPAUSESLACK, DRAWPAUSE
    }

    // Private Konstruktion, Singleton über den Holder
    private VPlotterPropertiesManager() {
        properties = new Properties();
    }

    private static class Holder {
        private static final VPlotterPropertiesManager INSTANCE = new VPlotterPropertiesManager();
    }

    public static VPlotterPropertiesManager getCurrent() {
        return Holder.INSTANCE;
    }

    /**
     * Speichert die übergebenen Properties (falls nicht null) oder die internen Properties
     * in die angegebene Datei als XML.
     *
     * @param file        Zieldatei
     * @param propToStore Falls nicht null, wird diese Properties-Instanz gespeichert; andernfalls die internen Properties.
     * @throws IOException bei Fehlern im Dateischreibprozess.
     */
    private void save(File file, Properties propToStore) throws IOException {
        try (FileOutputStream output = new FileOutputStream(file)) {
            if (propToStore == null) {
                properties.storeToXML(output, null);
            } else {
                propToStore.storeToXML(output, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Gibt den Wert zum angegebenen Schlüssel zurück.
     *
     * @param key der Schlüssel.
     * @return Der gespeicherte Wert oder null, wenn nicht vorhanden.
     */
    public synchronized String getValue(KEY key) {
        return properties.getProperty(key.name().toLowerCase());
    }

    /**
     * Setzt den Wert zum angegebenen Schlüssel in den übergebenen Properties (falls nicht null),
     * ansonsten in den internen Properties.
     *
     * @param key         der Schlüssel.
     * @param value       der zu setzende Wert.
     * @param targetProps Ziel-Properties, in die der Wert gesetzt werden soll, oder null für die internen Properties.
     */
    public synchronized void setValue(KEY key, String value, Properties targetProps) {
        if (value != null) {
            if (targetProps == null) {
                properties.setProperty(key.name().toLowerCase(), value);
            } else {
                targetProps.setProperty(key.name().toLowerCase(), value);
            }
        }
    }

    /**
     * Lädt die Eigenschaften aus der angegebenen XML-Datei und aktualisiert gleichzeitig
     * die CONFIG-Einstellung im SettingsManager.
     *
     * @param file Die Konfigurationsdatei.
     */
    public void load(File file) {
        try (FileInputStream is = new FileInputStream(file)) {
            properties.clear();
            properties.loadFromXML(is);
            // Aktualisiere im SettingsManager den CONFIG-Schlüssel mit dem Dateipfad.
            SettingsManager.getCurrent().setValue(SettingsManager.KEY.CONFIG, file.getAbsolutePath());
            SettingsManager.getCurrent().save();
        } catch (Exception exc) {
            exc.printStackTrace();
        }
    }

    /**
     * Erstellt eine neue Konfiguration, indem bestimmte Schlüssel initial auf einen leeren String gesetzt werden.
     * Anschließend wird die Konfiguration in die angegebene Datei gespeichert.
     *
     * @param file Die Datei, in der die neue Konfiguration gespeichert wird.
     */
    public void newConfig(File file) {
        Properties newProperties = new Properties();
        // Liste der Schlüssel, die initialisiert werden sollen
        KEY[] keysToInitialize = {
            KEY.MINX, KEY.MINY, KEY.PLOTTERWIDTH, KEY.PLOTTERHEIGHT, 
            KEY.SPACEBETWEEN, KEY.SECTORLENGTH, KEY.STEPSPERMM, 
            KEY.DRAWPAUSE, KEY.STARTPOINTX, KEY.STARTPOINTY, 
            KEY.STEPPAUSEDRAW, KEY.STEPPAUSESLACK
        };
        for (KEY key : keysToInitialize) {
            setValue(key, "", newProperties);
        }
        try {
            save(file, newProperties);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
