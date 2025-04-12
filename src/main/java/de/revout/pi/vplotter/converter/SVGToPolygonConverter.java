package de.revout.pi.vplotter.converter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SVGToPolygonConverter {

    // Dimensionen und Transformationen der SVG
    private double width = 0;
    private double height = 0;
    private double translateX = 0;
    private double translateY = 0;
    private double scaleX = 1;
    private double scaleY = 1;
    private double minX = 0;
    private double maxX = 0;
    private double minY = 0;
    private double maxY = 0;

    /**
     * Wandelt das gegebene SVG-Dokument (Pfad) in eine Liste von absoluten
     * Polygon-Pfad-Daten um.
     *
     * @param paramPathToSVG Pfad zur SVG-Datei
     * @return Liste der konvertierten Pfaddaten
     */
    public List<String> convertSVG(Path paramPathToSVG) {
        // Reset der globalen Variablen
        width = 0;
        height = 0;
        translateX = 0;
        translateY = 0;
        scaleX = 1;
        scaleY = 1;
        minX = 0;
        maxX = 0;
        minY = 0;
        maxY = 0;

        try (InputStream fileIS = Files.newInputStream(paramPathToSVG, StandardOpenOption.READ)) {
            // XML-Dokument einlesen
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(fileIS);
            Element root = xmlDocument.getDocumentElement();

            // Lese width und height (ohne Einheiten)
            width = parseDoubleClean(root.getAttribute("width"));
            height = parseDoubleClean(root.getAttribute("height"));

            // Transformationswerte aus dem ersten <g>-Element auslesen (falls vorhanden)
            NodeList gList = root.getElementsByTagName("g");
            if (gList.getLength() > 0) {
                Node gNode = gList.item(0);
                NamedNodeMap attrs = gNode.getAttributes();
                Node transformAttr = attrs.getNamedItem("transform");
                if (transformAttr != null) {
                    String transformStr = transformAttr.getNodeValue();
                    double[] translate = getValuePair(transformStr, "translate", 0);
                    double[] scale = getValuePair(transformStr, "scale", 1);
                    translateX = translate[0];
                    translateY = translate[1];
                    scaleX = scale[0];
                    scaleY = scale[1];
                }
            } else {
                translateX = 0;
                translateY = 0;
                scaleX = 1;
                scaleY = 1;
            }

            // Suche alle <path>-Elemente mittels XPath
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "//path";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            List<String> dataList = new ArrayList<>();
            for (int i = 0; i < nodeList.getLength(); i++) {
                Node node = nodeList.item(i);
                String pathData = node.getAttributes().getNamedItem("d").getNodeValue();
                dataList.addAll(getDataFromPath(pathData));
            }

            // Umrechnung in absolute Koordinaten
            List<String> absolutList = convertToAbsolute(dataList, width, height);
            // Korrigiere Werte aus negativen Bereichen (falls erforderlich)
            return adjustForNegativeCoordinates(absolutList);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }

    /**
     * Entfernt alle Buchstaben aus einem Zahlen-String und liefert diesen als double.
     */
    private double parseDoubleClean(String value) {
        return Double.parseDouble(value.replaceAll("\\p{Alpha}", ""));
    }

    /**
     * Extrahiert aus einem Transformations-String (z. B. "translate(10,20) scale(2)")
     * ein Zahlenpaar für den angegebenen Schlüssel.
     *
     * @param paramString Der Transformations-String
     * @param paramKey    Der zu suchende Schlüssel, z. B. "translate" oder "scale"
     * @param paramDefault Standardwert für beide Werte, falls nicht gefunden
     * @return Ein Array mit zwei double-Werten
     */
    private double[] getValuePair(String paramString, String paramKey, double paramDefault) {
        double[] result = new double[] { paramDefault, paramDefault };
        int startIndex = paramString.indexOf(paramKey);
        if (startIndex > -1) {
            int endIndex = paramString.indexOf(")", startIndex);
            if (endIndex > startIndex) {
                String content = paramString.substring(startIndex + paramKey.length() + 1, endIndex);
                // Splitte anhand von Komma und/oder Leerzeichen und filtere leere Strings
                String[] parts = content.split("[,\\s]+");
                try {
                    result = Arrays.stream(parts).mapToDouble(Double::parseDouble).toArray();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /**
     * Wandelt die gesammelten Pfaddaten aus relativen SVG-Koordinaten in absolute Koordinaten um.
     */
    private List<String> convertToAbsolute(List<String> dataList, double svgWidth, double svgHeight) {
        List<String> absolutList = new ArrayList<>();
        double beginX = 0, beginY = 0, lastMoveX = 0, lastMoveY = 0;
        boolean endWithZ;
        BezierCurve bezierCurve = new BezierCurve(20);

        for (String rowData : dataList) {
            rowData = rowData.trim();
            int length = rowData.length();
            endWithZ = false;
            if (rowData.endsWith("z") || rowData.endsWith("Z")) {
                endWithZ = true;
                length--; // Ignoriere das letzte Zeichen
            }
            String command = rowData.substring(0, 1);
            String newLine = command.toUpperCase();
            // Für manche Befehle wird die Abfolge zu "L" geändert
            if (command.matches("[cCvVHh]")) {
                newLine = "L";
            }
            rowData = rowData.substring(1, length).replaceAll(",", " ");
            double[] values = Arrays.stream(rowData.split("\\s+"))
                                    .mapToDouble(Double::parseDouble)
                                    .toArray();
            double x = beginX, y = beginY;
            for (int i = 0; i < values.length; i += 2) {
                switch (command) {
                    case "M":
                        x = values[i];
                        y = values[i + 1];
                        beginX = x;
                        beginY = y;
                        lastMoveX = x;
                        lastMoveY = y;
                        newLine += " " + convertX(svgWidth, x, translateX, scaleX)
                                + " " + convertY(svgHeight, y, translateY, scaleY);
                        newLine = newLine.trim();
                        absolutList.add(newLine);
                        command = "L"; // Folgebefehle als "L"
                        newLine = "";
                        break;
                    case "m":
                        x = beginX + values[i];
                        y = beginY + values[i + 1];
                        beginX = x;
                        beginY = y;
                        lastMoveX = x;
                        lastMoveY = y;
                        newLine += " " + convertX(svgWidth, x, translateX, scaleX)
                                + " " + convertY(svgHeight, y, translateY, scaleY);
                        newLine = newLine.trim();
                        absolutList.add(newLine);
                        command = "l"; // Folgebefehle als relativ
                        newLine = "";
                        break;
                    case "L":
                    case "l":
                        // Bei L oder l werden direkte Koordinaten übernommen; bei "l" werden diese relativ addiert.
                        if ("L".equals(command)) {
                            x = values[i];
                            y = values[i + 1];
                        } else { // "l"
                            x = beginX + values[i];
                            y = beginY + values[i + 1];
                        }
                        beginX = x;
                        beginY = y;
                        if (newLine.isEmpty()) {
                            newLine = "L";
                        }
                        newLine += " " + convertX(svgWidth, x, translateX, scaleX)
                                + " " + convertY(svgHeight, y, translateY, scaleY);
                        break;
                    case "V":
                    case "v":
                        // Vertikale Linien: nur y wird gesetzt; x bleibt gleich.
                        if ("V".equals(command)) {
                            y = values[i];
                        } else {
                            y = beginY + values[i];
                        }
                        beginY = y;
                        if (newLine.isEmpty()) {
                            newLine = "L";
                        }
                        newLine += " " + convertX(svgWidth, beginX, translateX, scaleX)
                                + " " + convertY(svgHeight, y, translateY, scaleY);
                        break;
                    case "H":
                    case "h":
                        // Horizontale Linien: nur x wird gesetzt; y bleibt konstant.
                        if ("H".equals(command)) {
                            x = values[i];
                        } else {
                            x = beginX + values[i];
                        }
                        beginX = x;
                        if (newLine.isEmpty()) {
                            newLine = "L";
                        }
                        newLine += " " + convertX(svgWidth, x, translateX, scaleX)
                                + " " + convertY(svgHeight, beginY, translateY, scaleY);
                        break;
                    case "C":
                        // Kubische Bézierkurve; nimm 6 Werte (drei Punkte)
                        double x1 = values[i];
                        double y1 = values[i + 1];
                        double x2 = values[i + 2];
                        double y2 = values[i + 3];
                        x = values[i + 4];
                        y = values[i + 5];
                        i += 4; // Überspringe zusätzliche Werte
                        double[] bezPoints = bezierCurve.bezier2D(new double[]{beginX, beginY, x1, y1, x2, y2, x, y});
                        // Beginne ab dem zweiten Punkt (Index 2)
                        for (int j = 2; j < bezPoints.length; j += 2) {
                            newLine += " " + convertX(svgWidth, bezPoints[j], translateX, scaleX)
                                    + " " + convertY(svgHeight, bezPoints[j + 1], translateY, scaleY);
                        }
                        beginX = x;
                        beginY = y;
                        break;
                    case "c":
                        // Relative kubische Bézierkurve
                        double rx1 = beginX + values[i];
                        double ry1 = beginY + values[i + 1];
                        double rx2 = beginX + values[i + 2];
                        double ry2 = beginY + values[i + 3];
                        x = beginX + values[i + 4];
                        y = beginY + values[i + 5];
                        i += 4;
                        double[] bezRelPoints = bezierCurve.bezier2D(new double[]{beginX, beginY, rx1, ry1, rx2, ry2, x, y});
                        for (int j = 2; j < bezRelPoints.length; j += 2) {
                            newLine += " " + convertX(svgWidth, bezRelPoints[j], translateX, scaleX)
                                    + " " + convertY(svgHeight, bezRelPoints[j + 1], translateY, scaleY);
                        }
                        beginX = x;
                        beginY = y;
                        break;
                    default:
                        // Unbekannte Befehle werden ignoriert
                        break;
                }
            }
            newLine = newLine.trim();
            if (!newLine.isEmpty()) {
                absolutList.add(newLine);
            }
            if (endWithZ) {
                absolutList.add("L" + convertX(svgWidth, lastMoveX, translateX, scaleX) + " " +
                                  convertY(svgHeight, lastMoveY, translateY, scaleY));
                beginX = lastMoveX;
                beginY = lastMoveY;
            }
        }

        return absolutList;
    }

    /**
     * Passt die absoluten Pfaddaten an, falls negative Werte vorhanden sind.
     * Es werden Korrekturwerte (xCor, yCor) berechnet und auf alle Koordinaten addiert.
     */
    private List<String> adjustForNegativeCoordinates(List<String> absolutList) {
        // Setze Breiten/Hohen basierend auf bisherigen Min-/Max-Werten
        double xCor = 0, yCor = 0;
        if (minX < 0) {
            xCor = Math.abs(minX) + 1;
        }
        if (minY < 0) {
            yCor = Math.abs(minY) + 1;
        }

        List<String> result = new ArrayList<>();
        for (String row : absolutList) {
            // Der erste Buchstabe ist der Befehlsbuchstabe
            String command = row.substring(0, 1);
            // Parse die restlichen Werte
            double[] values = Arrays.stream(row.substring(1).trim().split("\\s+"))
            		.mapToDouble(Double::parseDouble)
                                    .toArray();
            String newLine = command;
            for (int i = 0; i < values.length; i += 2) {
                newLine += " " + (values[i] + xCor) + " " + (values[i + 1] + yCor);
            }
            result.add(newLine.trim());
        }
        return result;
    }

    /**
     * Extrahiert Pfaddaten aus dem "d"-Attribut eines SVG-<path>-Elements.
     *
     * @param pathData Der Inhalt des d-Attributs
     * @return Liste von Datenzeilen, die den Befehlen entsprechen.
     */
    private List<String> getDataFromPath(String pathData) {
        List<String> result = new ArrayList<>();
        // Statt StringTokenizer wird split genutzt, um Tokens anhand von Leerzeichen zu erhalten.
        String[] tokens = pathData.split("\\s+");
        String dataRow = "";
        boolean commandFlag = false;
        for (String token : tokens) {
            if (token.matches("[MmLlCcVvHh].*")) {
                if (!dataRow.isEmpty()) {
                    result.add(dataRow);
                }
                dataRow = token;
                // Wenn das Token exakt einen Buchstaben enthält, gilt es als reiner Befehl
                commandFlag = token.matches("[MmLlCcVvHh]");
            } else if (token.matches(".*[zZ]")) {
                if (token.matches("[zZ]")) {
                    dataRow += token;
                } else {
                    dataRow += " " + token.replace(',', ' ');
                }
                result.add(dataRow);
                dataRow = "";
                commandFlag = false;
            } else if (token.matches("[\\-0-9].*")) {
                String prefix = commandFlag ? "" : " ";
                dataRow += prefix + token.replace(',', ' ');
                commandFlag = false;
            }
        }
        if (!dataRow.isEmpty()) {
            result.add(dataRow);
        }
        return result;
    }

    private void setMinMaxX(double value) {
        minX = Math.min(minX, value);
        maxX = Math.max(maxX, value);
    }

    private void setMinMaxY(double value) {
        minY = Math.min(minY, value);
        maxY = Math.max(maxY, value);
    }

    private double convertX(double maxValue, double value, double translate, double scale) {
        double result;
        if (scale > 0) {
            result = (value + translate) * scale;
        } else {
            result = maxValue - (value + translate) * -scale;
        }
        setMinMaxX(result);
        return result;
    }

    private double convertY(double maxValue, double value, double translate, double scale) {
        double result;
        if (scale > 0) {
            result = (value + translate) * scale;
        } else {
            result = maxValue - (value + translate) * -scale;
        }
        setMinMaxY(result);
        return result;
    }

    // Zugriffsmethoden für die ermittelten SVG-Dimensionen und Transformationen
    public double getWidth() {
        return width;
    }
    public double getHeight() {
        return height;
    }
    public double getTranslateX() {
        return translateX;
    }
    public double getTranslateY() {
        return translateY;
    }
    public double getScaleX() {
        return scaleX;
    }
    public double getScaleY() {
        return scaleY;
    }
    public double getMaxX() {
        return maxX;
    }
    public double getMaxY() {
        return maxY;
    }
}
