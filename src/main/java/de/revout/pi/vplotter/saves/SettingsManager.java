package de.revout.pi.vplotter.saves;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

public class SettingsManager {

    private static final String SETTINGS_PROPERTY = "./conf/programm.settings";
    private final Properties properties;

    public static enum KEY {
        FILEPATH, POTRACE, SIMULATION, CONFIG, LAST_USED
    }

    // Singleton-Instanz über ein Holder-Konstrukt (thread-safe)
    private static class Holder {
        private static final SettingsManager INSTANCE = new SettingsManager();
    }

    public static SettingsManager getCurrent() {
        return Holder.INSTANCE;
    }

    private SettingsManager() {
        properties = new Properties();
        loadProperties();
    }

    /**
     * Lädt die Properties aus der XML-Datei, falls vorhanden.
     */
    private void loadProperties() {
        Path settingsPath = Paths.get(SETTINGS_PROPERTY);
        if (Files.exists(settingsPath)) {
            try (FileInputStream is = new FileInputStream(settingsPath.toFile())) {
                properties.loadFromXML(is);
            } catch (IOException exc) {
                exc.printStackTrace();
            }
        }
    }

    /**
     * Speichert die aktuellen Einstellungen in die XML-Datei.
     * Dabei wird zuerst ein Backup der bestehenden Datei erstellt.
     *
     * @throws IOException falls ein Fehler beim Speichern auftritt.
     */
    public synchronized void save() throws IOException {
        Path settingsPath = Paths.get(SETTINGS_PROPERTY);
        Path backupPath = Files.createTempFile("backup", "");
        
        // Erstelle zunächst ein Backup der bestehenden Datei
        if (Files.exists(settingsPath)) {
            Files.move(settingsPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        try (OutputStream output = new FileOutputStream(settingsPath.toFile())) {
            properties.storeToXML(output, null);
            // Lösche das Backup, wenn das Speichern erfolgreich war
            Files.deleteIfExists(backupPath);
        } catch (IOException e) {
            // Bei einem Fehler wird das Backup zurückverschoben
            Files.move(backupPath, settingsPath, StandardCopyOption.REPLACE_EXISTING);
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Liefert den in den Properties gespeicherten Wert für den angegebenen Schlüssel.
     *
     * @param key Der aufzurufende Schlüssel.
     * @return Der Wert als String oder null, falls nicht gesetzt.
     */
    public synchronized String getValue(KEY key) {
        return properties.getProperty(key.name().toLowerCase());
    }

    /**
     * Setzt den Wert zu einem bestimmten Schlüssel, sofern der neue Wert nicht null ist und sich
     * vom aktuellen Wert unterscheidet.
     *
     * @param key   Der Schlüssel.
     * @param value Der neue Wert.
     * @throws IOException Im Falle eines Fehlers beim Speichern (kann aber hier optional sein).
     */
    public synchronized void setValue(KEY key, String value) throws IOException {
        if (value != null && !value.equals(getValue(key))) {
            properties.setProperty(key.name().toLowerCase(), value);
        }
    }
}
