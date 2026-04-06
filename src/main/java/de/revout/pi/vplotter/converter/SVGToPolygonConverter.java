package de.revout.pi.vplotter.converter;

import java.awt.geom.AffineTransform;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Konvertiert SVG-<path>-Elemente in absolute Polygon-/Liniensegmente.
 *
 * <p>Unterstützte SVG-Pfadbefehle:
 * M, m, Z, z, L, l, H, h, V, v, C, c, S, s, Q, q, T, t, A, a
 *
 * <p>Unterstützte Transformationen:
 * matrix, translate, scale, rotate, skewX, skewY
 *
 * <p>Die Ausgabe enthält Zeilen im Format:
 * M x y
 * L x y x y ...
 *
 * <p>Jede Koordinate ist bereits in das endgültige Benutzerkoordinatensystem
 * transformiert, einschließlich root-viewBox, preserveAspectRatio sowie
 * vererbter transform-Attribute auf svg/g/path.
 */
public final class SVGToPolygonConverter {

    private static final Pattern LENGTH_NUMBER_PATTERN =
            Pattern.compile("[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

    private static final Pattern TRANSFORM_PATTERN =
            Pattern.compile("([a-zA-Z]+)\\s*\\(([^)]*)\\)");

    private static final Pattern PATH_TOKEN_PATTERN =
            Pattern.compile(
                    "[AaCcHhLlMmQqSsTtVvZz]|[-+]?(?:\\d*\\.\\d+|\\d+\\.?)(?:[eE][-+]?\\d+)?");

    private static final int DEFAULT_CURVE_SEGMENTS = 20;
    private static final int DEFAULT_ARC_SEGMENTS = 24;

    private double width;
    private double height;
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    public List<String> convertSVG(Path svgPath) {
        Objects.requireNonNull(svgPath, "svgPath must not be null");
        resetBounds();

        try (InputStream inputStream = Files.newInputStream(svgPath)) {
            Document document = parseXml(inputStream);
            Element root = document.getDocumentElement();
            if (root == null || !"svg".equalsIgnoreCase(root.getLocalName() != null ? root.getLocalName() : root.getNodeName())) {
                return Collections.emptyList();
            }

            SvgViewport viewport = readRootViewport(root);
            this.width = viewport.width();
            this.height = viewport.height();

            AffineTransform rootTransform = new AffineTransform();
            rootTransform.concatenate(viewport.viewBoxTransform());
            rootTransform.concatenate(parseTransformAttribute(root.getAttribute("transform")));

            List<String> result = new ArrayList<>();
            traverse(root, rootTransform, result);

            return Collections.unmodifiableList(result);
        } catch (Exception ex) {
            throw new IllegalStateException("SVG konnte nicht verarbeitet werden: " + svgPath, ex);
        }
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getMinX() {
        return minX;
    }

    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }

    public double getMaxY() {
        return maxY;
    }

    private void traverse(Element element, AffineTransform inheritedTransform, List<String> output) {
        String localName = element.getLocalName();
        if (localName == null || localName.isBlank()) {
            localName = element.getNodeName();
        }

        AffineTransform currentTransform = new AffineTransform(inheritedTransform);

        if (!"svg".equalsIgnoreCase(localName)) {
            currentTransform.concatenate(parseTransformAttribute(element.getAttribute("transform")));
        }

        if ("path".equalsIgnoreCase(localName)) {
            String d = element.getAttribute("d");
            if (d != null && !d.isBlank()) {
                output.addAll(convertPathToLines(d, currentTransform));
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child instanceof Element childElement) {
                traverse(childElement, currentTransform, output);
            }
        }
    }

    private Document parseXml(InputStream inputStream) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        factory.setXIncludeAware(false);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);

        try {
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        } catch (IllegalArgumentException ignored) {
            // Optional je nach JAXP-Implementierung.
        }

        try {
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        } catch (Exception ignored) {
            // Optional je nach Parser-Implementierung.
        }

        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        } catch (Exception ignored) {
            // Optional je nach Parser-Implementierung.
        }

        try {
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        } catch (Exception ignored) {
            // Optional je nach Parser-Implementierung.
        }

        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(inputStream);
    }

    private List<String> convertPathToLines(String d, AffineTransform transform) {
        PathTokenizer tokenizer = new PathTokenizer(d);
        List<String> result = new ArrayList<>();

        Point current = new Point(0.0, 0.0);
        Point subpathStart = new Point(0.0, 0.0);
        Point lastCubicControl = null;
        Point lastQuadraticControl = null;
        char command = 0;

        while (tokenizer.hasNext()) {
            if (tokenizer.peekIsCommand()) {
                command = tokenizer.nextCommand();
            } else if (command == 0) {
                throw new IllegalArgumentException("Ungültige SVG-Pfaddaten: erster Token ist kein Befehl.");
            }

            switch (command) {
                case 'M': {
                    Point p = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                    current = p;
                    subpathStart = p;
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    appendMove(result, transformPoint(transform, p));

                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    command = 'L';
                    break;
                }
                case 'm': {
                    Point p = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                    current = p;
                    subpathStart = p;
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    appendMove(result, transformPoint(transform, p));

                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    command = 'l';
                    break;
                }
                case 'L': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'l': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'H': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = new Point(tokenizer.nextNumber(), current.y());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'h': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = current.add(tokenizer.nextNumber(), 0.0);
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'V': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = new Point(current.x(), tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'v': {
                    while (tokenizer.hasNextNumber()) {
                        Point lineEnd = current.add(0.0, tokenizer.nextNumber());
                        appendLine(result, List.of(
                                transformPoint(transform, current),
                                transformPoint(transform, lineEnd)
                        ));
                        current = lineEnd;
                    }
                    lastCubicControl = null;
                    lastQuadraticControl = null;
                    break;
                }
                case 'C': {
                    while (tokenizer.hasNextNumber()) {
                        Point c1 = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point c2 = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenCubic(current, c1, c2, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastCubicControl = c2;
                        lastQuadraticControl = null;
                    }
                    break;
                }
                case 'c': {
                    while (tokenizer.hasNextNumber()) {
                        Point c1 = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point c2 = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenCubic(current, c1, c2, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastCubicControl = c2;
                        lastQuadraticControl = null;
                    }
                    break;
                }
                case 'S': {
                    while (tokenizer.hasNextNumber()) {
                        Point c1 = lastCubicControl == null ? current : reflect(lastCubicControl, current);
                        Point c2 = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenCubic(current, c1, c2, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastCubicControl = c2;
                        lastQuadraticControl = null;
                    }
                    break;
                }
                case 's': {
                    while (tokenizer.hasNextNumber()) {
                        Point c1 = lastCubicControl == null ? current : reflect(lastCubicControl, current);
                        Point c2 = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenCubic(current, c1, c2, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastCubicControl = c2;
                        lastQuadraticControl = null;
                    }
                    break;
                }
                case 'Q': {
                    while (tokenizer.hasNextNumber()) {
                        Point c = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenQuadratic(current, c, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = c;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 'q': {
                    while (tokenizer.hasNextNumber()) {
                        Point c = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());
                        Point end = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenQuadratic(current, c, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = c;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 'T': {
                    while (tokenizer.hasNextNumber()) {
                        Point c = lastQuadraticControl == null ? current : reflect(lastQuadraticControl, current);
                        Point end = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenQuadratic(current, c, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = c;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 't': {
                    while (tokenizer.hasNextNumber()) {
                        Point c = lastQuadraticControl == null ? current : reflect(lastQuadraticControl, current);
                        Point end = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenQuadratic(current, c, end, DEFAULT_CURVE_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = c;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 'A': {
                    while (tokenizer.hasNextNumber()) {
                        double rx = tokenizer.nextNumber();
                        double ry = tokenizer.nextNumber();
                        double xAxisRotation = tokenizer.nextNumber();
                        int largeArcFlag = tokenizer.nextFlag();
                        int sweepFlag = tokenizer.nextFlag();
                        Point end = new Point(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenArc(
                                current, rx, ry, xAxisRotation, largeArcFlag != 0, sweepFlag != 0, end, DEFAULT_ARC_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = null;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 'a': {
                    while (tokenizer.hasNextNumber()) {
                        double rx = tokenizer.nextNumber();
                        double ry = tokenizer.nextNumber();
                        double xAxisRotation = tokenizer.nextNumber();
                        int largeArcFlag = tokenizer.nextFlag();
                        int sweepFlag = tokenizer.nextFlag();
                        Point end = current.add(tokenizer.nextNumber(), tokenizer.nextNumber());

                        List<Point> polyline = flattenArc(
                                current, rx, ry, xAxisRotation, largeArcFlag != 0, sweepFlag != 0, end, DEFAULT_ARC_SEGMENTS);
                        appendLine(result, transformPoints(transform, polyline));

                        current = end;
                        lastQuadraticControl = null;
                        lastCubicControl = null;
                    }
                    break;
                }
                case 'Z':
                case 'z': {
                    appendLine(result, List.of(
                            transformPoint(transform, current),
                            transformPoint(transform, subpathStart)
                    ));
                    current = subpathStart;
                    lastQuadraticControl = null;
                    lastCubicControl = null;
                    break;
                }
                default:
                    throw new IllegalArgumentException("Nicht unterstützter Pfadbefehl: " + command);
            }
        }

        return result;
    }

    private SvgViewport readRootViewport(Element root) {
        double svgWidth = parseLength(root.getAttribute("width"), 0.0);
        double svgHeight = parseLength(root.getAttribute("height"), 0.0);

        String viewBoxValue = root.getAttribute("viewBox");
        String preserveAspectRatioValue = root.getAttribute("preserveAspectRatio");

        if (viewBoxValue == null || viewBoxValue.isBlank()) {
            return new SvgViewport(svgWidth, svgHeight, new AffineTransform());
        }

        double[] vb = parseNumberList(viewBoxValue);
        if (vb.length != 4) {
            throw new IllegalArgumentException("Ungültiges viewBox-Attribut: " + viewBoxValue);
        }

        double minX = vb[0];
        double minY = vb[1];
        double vbWidth = vb[2];
        double vbHeight = vb[3];

        if (vbWidth <= 0 || vbHeight <= 0) {
            throw new IllegalArgumentException("viewBox width/height müssen > 0 sein.");
        }

        if (svgWidth <= 0) {
            svgWidth = vbWidth;
        }
        if (svgHeight <= 0) {
            svgHeight = vbHeight;
        }

        AffineTransform viewBoxTransform =
                buildViewBoxTransform(minX, minY, vbWidth, vbHeight, svgWidth, svgHeight, preserveAspectRatioValue);

        return new SvgViewport(svgWidth, svgHeight, viewBoxTransform);
    }

    private AffineTransform buildViewBoxTransform(
            double vbMinX,
            double vbMinY,
            double vbWidth,
            double vbHeight,
            double viewportWidth,
            double viewportHeight,
            String preserveAspectRatio) {

        if (preserveAspectRatio == null || preserveAspectRatio.isBlank()) {
            preserveAspectRatio = "xMidYMid meet";
        }

        String[] parts = preserveAspectRatio.trim().split("\\s+");
        String align = parts.length > 0 ? parts[0] : "xMidYMid";
        String meetOrSlice = parts.length > 1 ? parts[1] : "meet";

        double scaleX = viewportWidth / vbWidth;
        double scaleY = viewportHeight / vbHeight;
        double translateX;
        double translateY;

        if ("none".equals(align)) {
            translateX = -vbMinX * scaleX;
            translateY = -vbMinY * scaleY;
            AffineTransform at = new AffineTransform();
            at.translate(translateX, translateY);
            at.scale(scaleX, scaleY);
            return at;
        }

        double uniformScale = "slice".equals(meetOrSlice)
                ? Math.max(scaleX, scaleY)
                : Math.min(scaleX, scaleY);

        double viewWidth = vbWidth * uniformScale;
        double viewHeight = vbHeight * uniformScale;

        double extraX = viewportWidth - viewWidth;
        double extraY = viewportHeight - viewHeight;

        double alignX = switch (align) {
            case "xMinYMin", "xMinYMid", "xMinYMax" -> 0.0;
            case "xMidYMin", "xMidYMid", "xMidYMax" -> extraX / 2.0;
            case "xMaxYMin", "xMaxYMid", "xMaxYMax" -> extraX;
            default -> extraX / 2.0;
        };

        double alignY = switch (align) {
            case "xMinYMin", "xMidYMin", "xMaxYMin" -> 0.0;
            case "xMinYMid", "xMidYMid", "xMaxYMid" -> extraY / 2.0;
            case "xMinYMax", "xMidYMax", "xMaxYMax" -> extraY;
            default -> extraY / 2.0;
        };

        translateX = alignX - (vbMinX * uniformScale);
        translateY = alignY - (vbMinY * uniformScale);

        AffineTransform at = new AffineTransform();
        at.translate(translateX, translateY);
        at.scale(uniformScale, uniformScale);
        return at;
    }

    private AffineTransform parseTransformAttribute(String transformValue) {
        AffineTransform result = new AffineTransform();
        if (transformValue == null || transformValue.isBlank()) {
            return result;
        }

        Matcher matcher = TRANSFORM_PATTERN.matcher(transformValue);
        while (matcher.find()) {
            String type = matcher.group(1);
            double[] values = parseNumberList(matcher.group(2));
            AffineTransform operation = switch (type) {
                case "matrix" -> parseMatrixTransform(values);
                case "translate" -> parseTranslateTransform(values);
                case "scale" -> parseScaleTransform(values);
                case "rotate" -> parseRotateTransform(values);
                case "skewX" -> parseSkewXTransform(values);
                case "skewY" -> parseSkewYTransform(values);
                default -> throw new IllegalArgumentException("Nicht unterstützte Transformation: " + type);
            };
            result.concatenate(operation);
        }

        return result;
    }

    private AffineTransform parseMatrixTransform(double[] v) {
        if (v.length != 6) {
            throw new IllegalArgumentException("matrix(...) erwartet 6 Werte.");
        }
        return new AffineTransform(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    private AffineTransform parseTranslateTransform(double[] v) {
        if (v.length != 1 && v.length != 2) {
            throw new IllegalArgumentException("translate(...) erwartet 1 oder 2 Werte.");
        }
        double tx = v[0];
        double ty = v.length == 2 ? v[1] : 0.0;
        return AffineTransform.getTranslateInstance(tx, ty);
    }

    private AffineTransform parseScaleTransform(double[] v) {
        if (v.length != 1 && v.length != 2) {
            throw new IllegalArgumentException("scale(...) erwartet 1 oder 2 Werte.");
        }
        double sx = v[0];
        double sy = v.length == 2 ? v[1] : sx;
        return AffineTransform.getScaleInstance(sx, sy);
    }

    private AffineTransform parseRotateTransform(double[] v) {
        if (v.length != 1 && v.length != 3) {
            throw new IllegalArgumentException("rotate(...) erwartet 1 oder 3 Werte.");
        }
        double angleRad = Math.toRadians(v[0]);
        if (v.length == 1) {
            return AffineTransform.getRotateInstance(angleRad);
        }
        return AffineTransform.getRotateInstance(angleRad, v[1], v[2]);
    }

    private AffineTransform parseSkewXTransform(double[] v) {
        if (v.length != 1) {
            throw new IllegalArgumentException("skewX(...) erwartet 1 Wert.");
        }
        return AffineTransform.getShearInstance(Math.tan(Math.toRadians(v[0])), 0.0);
    }

    private AffineTransform parseSkewYTransform(double[] v) {
        if (v.length != 1) {
            throw new IllegalArgumentException("skewY(...) erwartet 1 Wert.");
        }
        return AffineTransform.getShearInstance(0.0, Math.tan(Math.toRadians(v[0])));
    }

    private void appendMove(List<String> output, Point point) {
        updateBounds(point);
        output.add("M " + format(point.x()) + " " + format(point.y()));
    }

    private void appendLine(List<String> output, List<Point> points) {
        if (points.size() < 2) {
            return;
        }

        StringBuilder sb = new StringBuilder("L");
        boolean first = true;
        for (Point p : points) {
            if (!first) {
                sb.append(' ');
            } else {
                sb.append(' ');
                first = false;
            }
            updateBounds(p);
            sb.append(format(p.x())).append(' ').append(format(p.y()));
        }
        output.add(sb.toString());
    }

    private List<Point> transformPoints(AffineTransform transform, List<Point> points) {
        List<Point> result = new ArrayList<>(points.size());
        for (Point p : points) {
            result.add(transformPoint(transform, p));
        }
        return result;
    }

    private Point transformPoint(AffineTransform transform, Point point) {
        double[] src = { point.x(), point.y() };
        double[] dst = new double[2];
        transform.transform(src, 0, dst, 0, 1);
        return new Point(dst[0], dst[1]);
    }

    private List<Point> flattenCubic(Point p0, Point p1, Point p2, Point p3, int segments) {
        int effectiveSegments = Math.max(1, segments);
        List<Point> points = new ArrayList<>(effectiveSegments + 1);
        points.add(p0);

        for (int i = 1; i <= effectiveSegments; i++) {
            double t = (double) i / effectiveSegments;
            double oneMinusT = 1.0 - t;

            double x =
                    (oneMinusT * oneMinusT * oneMinusT * p0.x())
                            + (3.0 * oneMinusT * oneMinusT * t * p1.x())
                            + (3.0 * oneMinusT * t * t * p2.x())
                            + (t * t * t * p3.x());

            double y =
                    (oneMinusT * oneMinusT * oneMinusT * p0.y())
                            + (3.0 * oneMinusT * oneMinusT * t * p1.y())
                            + (3.0 * oneMinusT * t * t * p2.y())
                            + (t * t * t * p3.y());

            points.add(new Point(x, y));
        }

        return points;
    }

    private List<Point> flattenQuadratic(Point p0, Point p1, Point p2, int segments) {
        int effectiveSegments = Math.max(1, segments);
        List<Point> points = new ArrayList<>(effectiveSegments + 1);
        points.add(p0);

        for (int i = 1; i <= effectiveSegments; i++) {
            double t = (double) i / effectiveSegments;
            double oneMinusT = 1.0 - t;

            double x =
                    (oneMinusT * oneMinusT * p0.x())
                            + (2.0 * oneMinusT * t * p1.x())
                            + (t * t * p2.x());

            double y =
                    (oneMinusT * oneMinusT * p0.y())
                            + (2.0 * oneMinusT * t * p1.y())
                            + (t * t * p2.y());

            points.add(new Point(x, y));
        }

        return points;
    }

    private List<Point> flattenArc(
            Point start,
            double rx,
            double ry,
            double xAxisRotationDegrees,
            boolean largeArcFlag,
            boolean sweepFlag,
            Point end,
            int segments) {

        if (start.equals(end)) {
            return List.of(start, end);
        }

        double absRx = Math.abs(rx);
        double absRy = Math.abs(ry);

        if (absRx == 0.0 || absRy == 0.0) {
            return List.of(start, end);
        }

        double phi = Math.toRadians(xAxisRotationDegrees % 360.0);
        double cosPhi = Math.cos(phi);
        double sinPhi = Math.sin(phi);

        double dx2 = (start.x() - end.x()) / 2.0;
        double dy2 = (start.y() - end.y()) / 2.0;

        double x1Prime = cosPhi * dx2 + sinPhi * dy2;
        double y1Prime = -sinPhi * dx2 + cosPhi * dy2;

        double rxSq = absRx * absRx;
        double rySq = absRy * absRy;
        double x1PrimeSq = x1Prime * x1Prime;
        double y1PrimeSq = y1Prime * y1Prime;

        double lambda = (x1PrimeSq / rxSq) + (y1PrimeSq / rySq);
        if (lambda > 1.0) {
            double scale = Math.sqrt(lambda);
            absRx *= scale;
            absRy *= scale;
            rxSq = absRx * absRx;
            rySq = absRy * absRy;
        }

        double numerator = (rxSq * rySq) - (rxSq * y1PrimeSq) - (rySq * x1PrimeSq);
        double denominator = (rxSq * y1PrimeSq) + (rySq * x1PrimeSq);

        double factor;
        if (denominator == 0.0) {
            factor = 0.0;
        } else {
            double signed = Math.max(0.0, numerator / denominator);
            factor = Math.sqrt(signed);
            if (largeArcFlag == sweepFlag) {
                factor = -factor;
            }
        }

        double cxPrime = factor * ((absRx * y1Prime) / absRy);
        double cyPrime = factor * (-(absRy * x1Prime) / absRx);

        double centerX =
                cosPhi * cxPrime - sinPhi * cyPrime + ((start.x() + end.x()) / 2.0);
        double centerY =
                sinPhi * cxPrime + cosPhi * cyPrime + ((start.y() + end.y()) / 2.0);

        double ux = (x1Prime - cxPrime) / absRx;
        double uy = (y1Prime - cyPrime) / absRy;
        double vx = (-x1Prime - cxPrime) / absRx;
        double vy = (-y1Prime - cyPrime) / absRy;

        double theta1 = angleBetween(1.0, 0.0, ux, uy);
        double deltaTheta = angleBetween(ux, uy, vx, vy);

        if (!sweepFlag && deltaTheta > 0.0) {
            deltaTheta -= Math.PI * 2.0;
        } else if (sweepFlag && deltaTheta < 0.0) {
            deltaTheta += Math.PI * 2.0;
        }

        int effectiveSegments = Math.max(1, segments);
        List<Point> points = new ArrayList<>(effectiveSegments + 1);
        points.add(start);

        for (int i = 1; i <= effectiveSegments; i++) {
            double t = (double) i / effectiveSegments;
            double angle = theta1 + (deltaTheta * t);

            double cosAngle = Math.cos(angle);
            double sinAngle = Math.sin(angle);

            double x =
                    centerX
                            + (absRx * cosPhi * cosAngle)
                            - (absRy * sinPhi * sinAngle);
            double y =
                    centerY
                            + (absRx * sinPhi * cosAngle)
                            + (absRy * cosPhi * sinAngle);

            points.add(new Point(x, y));
        }

        points.set(points.size() - 1, end);
        return points;
    }

    private double angleBetween(double ux, double uy, double vx, double vy) {
        double dot = (ux * vx) + (uy * vy);
        double lengths = Math.hypot(ux, uy) * Math.hypot(vx, vy);

        if (lengths == 0.0) {
            return 0.0;
        }

        double value = Math.max(-1.0, Math.min(1.0, dot / lengths));
        double angle = Math.acos(value);

        double cross = (ux * vy) - (uy * vx);
        return cross < 0.0 ? -angle : angle;
    }

    private Point reflect(Point control, Point around) {
        return new Point(
                (2.0 * around.x()) - control.x(),
                (2.0 * around.y()) - control.y()
        );
    }

    private double parseLength(String value, double defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        Matcher matcher = LENGTH_NUMBER_PATTERN.matcher(value.trim());
        if (!matcher.find()) {
            return defaultValue;
        }

        return Double.parseDouble(matcher.group());
    }

    private double[] parseNumberList(String value) {
        if (value == null || value.isBlank()) {
            return new double[0];
        }

        Matcher matcher = LENGTH_NUMBER_PATTERN.matcher(value);
        List<Double> numbers = new ArrayList<>();
        while (matcher.find()) {
            numbers.add(Double.parseDouble(matcher.group()));
        }

        double[] result = new double[numbers.size()];
        for (int i = 0; i < numbers.size(); i++) {
            result[i] = numbers.get(i);
        }
        return result;
    }

    private String format(double value) {
        if (value == -0.0d) {
            value = 0.0d;
        }
        return String.format(Locale.ROOT, "%.6f", value);
    }

    private void resetBounds() {
        width = 0.0;
        height = 0.0;
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
    }

    private void updateBounds(Point point) {
        minX = Math.min(minX, point.x());
        minY = Math.min(minY, point.y());
        maxX = Math.max(maxX, point.x());
        maxY = Math.max(maxY, point.y());
    }

    private record SvgViewport(double width, double height, AffineTransform viewBoxTransform) { }

    private record Point(double x, double y) {
        Point add(double dx, double dy) {
            return new Point(x + dx, y + dy);
        }
    }

    private static final class PathTokenizer {
        private final List<String> tokens;
        private int index;

        PathTokenizer(String pathData) {
            Objects.requireNonNull(pathData, "pathData must not be null");

            Matcher matcher = PATH_TOKEN_PATTERN.matcher(pathData);
            List<String> parsedTokens = new ArrayList<>();
            while (matcher.find()) {
                parsedTokens.add(matcher.group());
            }

            this.tokens = List.copyOf(parsedTokens);
            this.index = 0;
        }

        boolean hasNext() {
            return index < tokens.size();
        }

        boolean peekIsCommand() {
            return hasNext() && isCommand(tokens.get(index));
        }

        boolean hasNextNumber() {
            return hasNext() && !peekIsCommand();
        }

        char nextCommand() {
            if (!peekIsCommand()) {
                throw new IllegalArgumentException("Pfadbefehl erwartet an Position " + index + ".");
            }
            return tokens.get(index++).charAt(0);
        }

        double nextNumber() {
            if (!hasNextNumber()) {
                throw new IllegalArgumentException("Zahl erwartet an Position " + index + ".");
            }
            return Double.parseDouble(tokens.get(index++));
        }

        int nextFlag() {
            double value = nextNumber();
            if (value == 0.0d) {
                return 0;
            }
            if (value == 1.0d) {
                return 1;
            }
            throw new IllegalArgumentException("Arc-Flag muss 0 oder 1 sein, war aber: " + value);
        }

        private boolean isCommand(String token) {
            return token.length() == 1
                    && "AaCcHhLlMmQqSsTtVvZz".indexOf(token.charAt(0)) >= 0;
        }
    }
}	