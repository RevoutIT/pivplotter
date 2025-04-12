package de.revout.pi.vplotter.model;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.converter.SVGToPolygonConverter;
import de.revout.pi.vplotter.converter.Section;
import de.revout.pi.vplotter.imagefilter.EdgeDetectionFilter;
import de.revout.pi.vplotter.imagefilter.Filter;
import de.revout.pi.vplotter.imagefilter.GrayscaleFilter;
import de.revout.pi.vplotter.imagefilter.RotateFilter;
import de.revout.pi.vplotter.setting.Setting;

public class Model {

    // Model-Daten
    private List<String> svgData;
    private List<Section> sectionList;
    private static Model current;
    private boolean sameFile;
    private SVGToPolygonConverter svgToPolygonConverter;
    private BufferedImage image;
    private BufferedImage originalImage;
    private BufferedImage svgImage;
    private List<Filter> filters;
    private List<Filter> usedFilters;
    private List<Update> updateList; 

    // Privater Konstruktor – Singleton-Pattern
    private Model() {
        updateList = new ArrayList<>();
        svgData = Collections.emptyList();
        sameFile = true;
        filters = new ArrayList<>();
        usedFilters = new ArrayList<>();
        // Standardfilter werden hinzugefügt
        filters.add(new RotateFilter());
        filters.add(new GrayscaleFilter());
        filters.add(new EdgeDetectionFilter());
        svgToPolygonConverter = new SVGToPolygonConverter();
        sectionList = new ArrayList<>();
    }

    public static Model getCurrent() {
        if (current == null) {
            current = new Model();
        }
        return current;
    }

    // Update-Verwaltung
    public void register(Update update) {
        updateList.add(update);
    }

    private void update() {
        for (Update update : updateList) {
            update.update();
        }
    }

    // --- Plotter-Utility-Methoden ---

    /**
     * Berechnet die euklidische Distanz zwischen zwei Punkten.
     *
     * @param startPoint der Startpunkt
     * @param endPoint   der Endpunkt
     * @return die Länge zwischen den Punkten
     */
    public double getLength(Pair startPoint, Pair endPoint) {
        double dx = startPoint.getX() - endPoint.getX();
        double dy = startPoint.getY() - endPoint.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Sucht in der Liste der Sections diejenige, deren Startpunkt der 
     * nächsten Verbindung zum letzten Punkt (lastPoint) hat.
     *
     * @param lastPoint der zuletzt benutzte Punkt
     * @param sections  Liste verfügbarer Sections; darf nicht leer sein
     * @return Die Section mit der kürzesten Verbindung
     * @throws IllegalArgumentException falls die Liste null oder leer ist
     */
    public Section getNextSection(Pair lastPoint, List<Section> sections) {
        if (sections == null || sections.isEmpty()) {
            throw new IllegalArgumentException("Die Liste der Sections darf nicht null oder leer sein.");
        }
        return sections.stream()
                       .min(Comparator.comparingDouble(s -> getLength(lastPoint, s.getStartPoint())))
                       .orElseThrow(() -> new IllegalStateException("Keine Section gefunden."));
    }


    /**
     * Erstellt aus den geladenen SVG-Daten eine geordnete Liste von Sections.
     * Dazu werden die Segmente, die mit "M" beginnen, als Startpunkte interpretiert
     * und die folgenden Linien (beginnend mit anderen Buchstaben) der Section zugeordnet.
     *
     * @return Die geordnete Liste der Sections.
     */
    public List<Section> createSectionList() {
        // Zwischenspeicher für die unverknüpften Sections
        List<Section> tempList = new ArrayList<>();
        Section currentSection = null;

        // Erzeuge Sections basierend auf den SVG-Daten
        for (String line : svgData) {
            if (line.startsWith("M")) {
                // Wenn bereits eine Section aufgebaut wurde, zur Liste hinzufügen
                if (currentSection != null) {
                    tempList.add(currentSection);
                }
                // Koordinaten parsen: entferne den führenden Buchstaben und splitte anhand beliebiger Leerzeichen
                double[] coords = Arrays.stream(line.substring(1).trim().split("\\s+"))
                                        .mapToDouble(Double::parseDouble)
                                        .toArray();
                // Letzte beiden Werte als Startkoordinaten verwenden
                currentSection = new Section(coords[coords.length - 2], coords[coords.length - 1]);
            } else if (currentSection != null) {
                // Entferne das führende Zeichen und füge die restlichen Daten zur aktuellen Section hinzu
                currentSection.addLineData(line.substring(1));
            }
        }
        // Letzte Section, falls vorhanden, hinzufügen
        if (currentSection != null) {
            tempList.add(currentSection);
        }

        // Nun wird eine geordnete Liste erstellt, basierend auf dem Konzept der nächsten Section mit der kürzesten Verbindung
        List<Section> result = new ArrayList<>();
        if (!tempList.isEmpty()) {
            // Starte mit der ersten Section und entferne sie aus der temporären Liste
            currentSection = tempList.remove(0);
            result.add(currentSection);

            // Füge solange Sections hinzu, bis alle geordnet sind
            while (!tempList.isEmpty()) {
                Section nextSection = getNextSection(currentSection.getLastPoint(), tempList);
                tempList.remove(nextSection);
                result.add(nextSection);
                currentSection = nextSection;
            }
        }
        return result;
    }

    /**
     * Speichert das aktuelle Bild als temporäres BMP.
     *
     * @return Der absolute Pfad zur erzeugten BMP-Datei oder null im Fehlerfall.
     */
    public String saveBMPTemp() {
        try {
            // Temporäre Datei mit vorgegebenem Präfix und Suffix erzeugen
            Path tempFile = Files.createTempFile("Buff_TO_BMP_", ".bmp");
            BufferedImage currentImage = this.getImage();
            if (currentImage != null) {
                // Speichere das Bild im BMP-Format in die temporäre Datei
                ImageIO.write(currentImage, "bmp", tempFile.toFile());
                // Zurückgeben des absoluten Pfads zur erzeugten Datei
                return tempFile.toAbsolutePath().toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Erzeugt aus einer BMP-Datei mithilfe von Potrace eine SVG-Datei und lädt diese anschließend.
     *
     * @param potracePath Der Pfad zur Potrace-Anwendung.
     * @param bmpPath     Der Pfad zur BMP-Datei.
     * @return true, wenn die SVG-Daten erfolgreich generiert wurden, ansonsten false.
     * @throws Exception falls Potrace nicht gefunden wird oder ein anderer Fehler auftritt.
     */
    public boolean generateSVGFromBMP(String potracePath, String bmpPath) throws Exception {
        if (!bmpPath.isEmpty() && bmpPath.endsWith(".bmp")) {
            if (svgData.isEmpty() || !sameFile) {
                Path tempSvg = Files.createTempFile("BMP_TO_SVG_", ".svg");
                Process process = Runtime.getRuntime().exec(new String[] { 
                        potracePath, "-o", tempSvg.toFile().getAbsolutePath(), 
                        "--flat", "-b", "svg", bmpPath 
                });
                process.waitFor();
                return generateDataFromSVG(tempSvg.toString());
            }
            return false;
        }
        return false;
    }

    /**
     * Lädt SVG-Daten aus einer SVG-Datei, erstellt ein leeres BufferedImage und setzt den sameFile-Status auf true.
     *
     * @param svgFilePath Der Pfad zur SVG-Datei.
     * @return true, wenn die Daten erfolgreich geladen wurden, ansonsten false.
     */
    public boolean generateDataFromSVG(String svgFilePath) {
        if (!svgFilePath.isEmpty() && svgFilePath.endsWith(".svg")) {
            Path path = Paths.get(svgFilePath);
            try {
                if (Files.size(path) == 0) {
                    return false;
                }
                svgData = svgToPolygonConverter.convertSVG(path);
                svgImage = new BufferedImage((int) svgToPolygonConverter.getWidth(),
                        (int) svgToPolygonConverter.getHeight(), BufferedImage.TYPE_INT_ARGB);
                sameFile = true;
                return true;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return false;
    }

    // --- Getter und Setter ---

    public List<String> getSvgData() {
        return svgData;
    }

    public void setSvgData(List<String> svgData) {
        this.svgData = svgData;
    }

    public List<Section> getSectionList() {
        return sectionList;
    }

    public void setSectionList(List<Section> sectionList) {
        this.sectionList = sectionList;
    }

    public boolean isSameFile() {
        return sameFile;
    }

    public void setSameFile(boolean sameFile) {
        this.sameFile = sameFile;
    }

    public SVGToPolygonConverter getSvgToPolygonConverter() {
        return svgToPolygonConverter;
    }

    public void setSvgToPolygonConverter(SVGToPolygonConverter svgToPolygonConverter) {
        this.svgToPolygonConverter = svgToPolygonConverter;
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
    }

    /**
     * Prüft, ob das aktuelle Bild in Graustufen vorliegt.
     *
     * @return true, wenn das Bild grauwertig ist, ansonsten false.
     */
    public boolean checkIfGrey() {
        BufferedImage img = getImage();
        if (img == null) return false;
        for (int x = 0; x < img.getWidth(); x++) {
            for (int y = 0; y < img.getHeight(); y++) {
                int rgb = img.getRGB(x, y);
                int red = (rgb >> 16) & 0xff;
                int green = (rgb >> 8) & 0xff;
                int blue = rgb & 0xff;
                if (!(red == green && green == blue)) {
                    return false;
                }
            }
        }
        return true;
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public BufferedImage getSvgImage() {
        return svgImage;
    }

    public void setSvgImage(BufferedImage svgImage) {
        this.svgImage = svgImage;
    }

    // --- Filter-Methoden ---

    /**
     * Aktualisiert den Wert einer Filter-Einstellung, wendet die Änderung an und führt ein Update aus.
     *
     * @param filter       Der betroffene Filter.
     * @param setting      Die betroffene Einstellung.
     * @param settingValue Der neue Einstellungswert.
     */
    public void editFilterSetting(Filter filter, Setting setting, String settingValue) {
        filter.getSetting(setting).setValue(settingValue);
        updateImage();
        update();
    }

    public void addFilter(Filter filter) {
        usedFilters.add(filter);
        updateImage();
        update();
    }

    public void removeFilter(Filter filter) {
        usedFilters.remove(filter);
        updateImage();
        update();
    }

    public void removeAllFilter() {
        usedFilters.clear();
        updateImage();
        update();
    }

    /**
     * Wendet alle verwendeten Filter auf das Originalbild an und aktualisiert so das angezeigte Bild.
     */
    public void updateImage() {
        image = originalImage;
        if (usedFilters != null && !usedFilters.isEmpty()) {
            for (Filter filter : usedFilters) {
                image = filter.filter(image);
            }
        }
    }

    public List<Filter> getUsedFilter() {
        return usedFilters;
    }

    /**
     * Setzt den Zustand für einen neuen Prozess zurück.
     */
    public void newProcess() {
        usedFilters.clear();
        sectionList.clear();
        svgData.clear();
        image = null;
        originalImage = null;
        svgImage = null;
        update();
    }
}
