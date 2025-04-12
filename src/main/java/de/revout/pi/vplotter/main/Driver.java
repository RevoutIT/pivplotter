package de.revout.pi.vplotter.main;

import java.io.File;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.pi4j.Pi4J;
import com.pi4j.context.Context;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.saves.SettingsManager;
import de.revout.pi.vplotter.saves.VPlotterPropertiesManager;
import de.revout.pi.vplotter.saves.VPlotterPropertiesManager.KEY;

public class Driver {

    // GPIO-Pin-Konfiguration
    private int ENALeft;
    private int ENARight;
    private int DIRLeft;
    private int DIRRight;
    private int PULLeft;
    private int PULLRight;
    private float penaway;
    private float pendraw;
    
    private float servoCurrentPosition=Integer.MIN_VALUE;

    private final static int RANGE = 20;

    // Plotter-Parameter
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;
    private double paperHeight;
    private double paperWidth;
    // Offsets zur Zentrierung
    private double offsetX;
    private double offsetY;
    private double[] plotterSide = new double[2];
    private int lastDraw;
    private double lengthBetween;
    private double step;
    private double stepsPerMM;
    private double scale;
    private Pair startPoint;
    private Pair currentPoint;

    private long stepPauseDraw;
    private long stepPauseSlack;
    private long currentPause;

    private Pair motorLinks;
    private Pair motorRechts;

    private boolean gpio = false;
    private static Driver current;

    private boolean simulation = false;
    private boolean pause;
    private boolean stop;
    private boolean used = false;
    
    private ArrayList<DriverMoveObserverIf> observerList;
    private boolean init;

    // Globaler Pi4J-Kontext – wird beim Start der Anwendung initialisiert
    private Context pi4j;

    private ServoMotor servoMotor;

    // Digitale Ausgänge
    private DigitalOutput enaLeftOutput;
    private DigitalOutput enaRightOutput;
    private DigitalOutput dirLeft;
    private DigitalOutput dirRight;
    private DigitalOutput pulLeft;
    private DigitalOutput pulRight;

    public static Driver getCurrent() {
        if (current == null) {
            current = new Driver();
        }
        return current;
    }

    private Driver() {
        setGpioSetting();
        init = false;
        pause = false;
        stop = false;
        // Letzter Status des Stiftes (2 als Initialwert)
        lastDraw = 2;
        scale = 0;
        observerList = new ArrayList<>();
    }

    private void setGpioSetting() {
        Path path = Paths.get("conf/gpio.properties");
        if (Files.exists(path)) {
            try (InputStream inputStream = Files.newInputStream(path)) {
                Properties properties = new Properties();
                properties.loadFromXML(inputStream);
                ENALeft = Integer.parseInt(properties.getProperty("ENALEFT"));
                ENARight = Integer.parseInt(properties.getProperty("ENARIGHT"));
                DIRLeft = Integer.parseInt(properties.getProperty("DIRLEFT"));
                DIRRight = Integer.parseInt(properties.getProperty("DIRRIGHT"));
                PULLeft = Integer.parseInt(properties.getProperty("PULLEFT"));
                PULLRight = Integer.parseInt(properties.getProperty("PULLRIGHT"));
                penaway = Integer.parseInt(properties.getProperty("PENAWAY"));
                pendraw = Integer.parseInt(properties.getProperty("PENDRAW"));
            } catch (Exception e) {
                throw new RuntimeException(path.toString() + " Error:" + e.getMessage());
            }
            if (pi4j == null) {
                pi4j = Pi4J.newAutoContext();
            }
        } else {
            throw new RuntimeException(path.toString() + " not found!");
        }
    }

    /**
     * Führt einen Testlauf durch, indem Testdaten erzeugt und geplottet werden.
     */
    public void test() {
        List<String> testData = new ArrayList<>();
        loadProperty();
        testData.add("0,0,0");
        testData.add("1,0," + plotterSide[1]);
        testData.add("1," + plotterSide[0] + "," + plotterSide[1]);
        testData.add("1," + plotterSide[0] + ",0");
        testData.add("1,0,0");
        testData.add("1," + plotterSide[0] + "," + plotterSide[1]);
        testData.add("0," + plotterSide[0] + ",0");
        testData.add("1,0," + plotterSide[1]);
        plotte(testData, plotterSide[0], plotterSide[1]);
    }

    /**
     * Startet einen neuen Plot-Prozess, der eine Liste von Zeichenbefehlen (als CSV-Strings) abarbeitet.
     *
     * @param commands Liste der Befehle im Format "drawState,x,y"
     * @param width    Breite des Plotterbereichs
     * @param height   Höhe des Plotterbereichs
     */
    public void plotte(List<String> commands, double width, double height) {
        if (!simulation && !gpioInit()) {
            return;
        }
        observerList.forEach(DriverMoveObserverIf::init);
        motorsOn();
        stop = false;
        pause = false;
        int commandSize = commands.size();

        new Thread(() -> {
            try {
                scale = calculateScale(width, height);
                ArrayList<Pair> pathPoints = new ArrayList<Pair>();
                for (int i = 0; i < commandSize; i++) {
                	final int commandIndex = i;
                    // Bei Pause schrittweise warten
                    while (pause) {
                        if (stop) return;
                        Thread.sleep(1000);
                    }
                    String[] parts = commands.get(i).split(",");
                    int drawState = Integer.parseInt(parts[0]);
                    if (lastDraw != drawState) {
                        draw(drawState);
                    }
                    // Umrechnung der Koordinaten unter Berücksichtigung der Skalierung und des Versatzes
                    Pair toPoint = new Pair((Double.parseDouble(parts[1]) / scale) + minX,
                                             (Double.parseDouble(parts[2]) / scale) + minY);
                    getPathPoints(pathPoints, currentPoint, toPoint);
                    for (Pair point : pathPoints) {
                        if (isInRange(point)) {
                            Pair diff = getDifferences(currentPoint, point);
                            Pair driveDelta = move(diff);
                            calculateRealCurrentPosition(driveDelta);
                        }
                    }
                    observerList.forEach(observer ->
                        observer.currentMove(drawState, currentPoint, commandSize, commandIndex)
                    );
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finish();
            }
        }).start();
    }

    private boolean gpioInit() {
        if (!init) {
            init = gpioStart();
        }
        return init;
    }

    /**
     * Berechnet anhand der Verschiebungsdifferenz die neue aktuelle Position.
     */
    private void calculateRealCurrentPosition(Pair driveDelta) {
        double leftBefore = getDistance(motorLinks, currentPoint);
        double rightBefore = getDistance(motorRechts, currentPoint);
        double leftAfter = leftBefore + driveDelta.getX();
        double rightAfter = rightBefore + driveDelta.getY();
        double cosAlpha = (Math.pow(lengthBetween, 2) + Math.pow(leftAfter, 2) - Math.pow(rightAfter, 2))
                          / (2 * lengthBetween * leftAfter);
        double newX = cosAlpha * leftAfter;
        double newY = Math.sqrt(Math.max(0, Math.pow(leftAfter, 2) - Math.pow(newX, 2)));
        currentPoint.setX(newX);
        currentPoint.setY(newY);
    }

    /**
     * Bewegt den Plotter zum angegebenen Zielpunkt.
     */
    public void goTo(Pair targetPosition) {
        if (isReady()) {
            draw(0);
            Pair diff = getDifferences(currentPoint, targetPosition);
            Pair driveDelta = move(diff);
            calculateRealCurrentPosition(driveDelta);
        }
    }

    /**
     * Nach Abschluss des Plottens wird der Stift zur Ausgangsposition gefahren
     * und alle registrierten Beobachter werden informiert.
     */
    private void finish() {
        used = true;
        goTo(startPoint);
        observerList.forEach(DriverMoveObserverIf::finish);
    }

    // --- GPIO/Servosteuerung ---

    private boolean gpioStart() {
        if (!simulation) {
            try {
                enaLeftOutput = createDigitalOutput(pi4j, ENALeft);
                enaRightOutput = createDigitalOutput(pi4j, ENARight);
                dirLeft = createDigitalOutput(pi4j, DIRLeft);
                dirRight = createDigitalOutput(pi4j, DIRRight);
                pulLeft = createDigitalOutput(pi4j, PULLeft);
                pulRight = createDigitalOutput(pi4j, PULLRight);
                servoMotor = new ServoMotor(pi4j, 2, 50, -90.0f, 90.0f, 2.0f, 12f);
                gpio = true;
            } catch (Throwable exc) {
                gpio = false;
                simulation = true;
                exc.printStackTrace();
            }
        }
        return gpio;
    }

    public void motorsOff() {
        if (!simulation && gpioInit()) {
            enaLeftOutput.high();
            enaRightOutput.high();
        }
        pause = true;
        stop();
    }

    public void motorsOn() {
        if (!simulation && gpioInit()) {
            enaLeftOutput.low();
            enaRightOutput.low();
        }
    }

    public void penAway() {
        if (!simulation && gpioInit()) {
        	smoothMoveServo(penaway,0.5f,50);
        }
    }

    public void penDraw() {
        if (!simulation && gpioInit()) {
        	smoothMoveServo(pendraw,0.5f,50);
        }
    }
    
    /**
     * Ändert den Winkel des Servomotors langsam von der aktuellen Position zum Zielwinkel.
     * 
     * @param currentPosition Der aktuelle Winkel des Servomotors.
     * @param targetAngle     Der Zielwinkel, den der Servo erreichen soll.
     * @param stepSize        Die Größe der einzelnen Winkeländerungsschritte.
     * @param pauseMillis     Die Pause in Millisekunden zwischen den einzelnen Schritten.
     */
    private void smoothMoveServo(float targetAngle, float stepSize, long pauseMillis) {
        
    	if(servoCurrentPosition==Integer.MIN_VALUE) {
    		servoCurrentPosition = targetAngle;
    		servoMotor.setAngle(servoCurrentPosition);
    		return;
    	}
    	
    	// Berechne die Richtung (+1 oder -1)
        float direction = targetAngle > servoCurrentPosition ? 1.0f : -1.0f;
        float newPosition = servoCurrentPosition;
        
        // Solange der aktuelle Winkel noch nicht das Ziel erreicht hat,
        // wird in kleinen Schritten der Winkel geändert.
        while ((direction > 0 && newPosition < targetAngle) || (direction < 0 && newPosition > targetAngle)) {
            newPosition += stepSize * direction;
            
            // Überschreiten des Zielwinkels verhindern
            if ((direction > 0 && newPosition > targetAngle) || (direction < 0 && newPosition < targetAngle)) {
                newPosition = targetAngle;
            }
            
            // Setzt den aktuellen Winkel am Servo – hier wird angenommen,
            // dass 'servoMotor' eine Instanz des ServoMotor ist und setPercent(float) den Winkel setzt.
            servoMotor.setAngle(newPosition);
            
            // Falls die aktuelle Position in einer Instanzvariable gespeichert werden soll:
            servoCurrentPosition = newPosition;
            
            try {
                Thread.sleep(pauseMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    

    public void gpioStop() {
        if (isReady()) {
            try {
                if (gpioInit()) {
                    enaLeftOutput.low();
                    enaRightOutput.low();
                }
            } catch (Exception e) {
                // Fehler ignorieren
            }
        }
    }

    // --- Berechnung und Hilfsmethoden ---

    // Berechnet den Skalierungsfaktor, um den Plotterbereich in den vorgegebenen Maßen abzubilden.
    private double calculateScale(double width, double height) {
        return Double.max(width / plotterSide[0], height / plotterSide[1]);
    }

    // Bewegt den Stift (über den Servo), um den Zeichenstatus zu ändern.
    private void draw(int drawState) {
        if (isReady()) {
            try {
                if (drawState == 0) {
                    currentPause = stepPauseSlack;
                    penAway();
                } else {
                    currentPause = stepPauseDraw;
                    penDraw();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        lastDraw = drawState;
    }

    public void showPageLocation() {
        List<String> testData = new ArrayList<>();
        loadProperty();
        // Offset-Anpassungen
        minX -= offsetX;
        minY -= offsetY;
        testData.add("0,15,0");
        testData.add("1,0,0");
        testData.add("1,0,15");
        testData.add("0," + (paperWidth - 15) + ",0");
        testData.add("1," + paperWidth + ",0");
        plotte(testData, paperWidth, paperHeight);
    }

    // Prüft, ob der gegebene Punkt noch innerhalb des erlaubten Bereichs liegt.
    private boolean isInRange(Pair point) {
        return !(point.getX() < (minX - RANGE) || point.getX() > (maxX + RANGE) ||
                 point.getY() < (minY - RANGE) || point.getY() > (maxY + RANGE));
    }

    // Berechnet den Unterschied der Seillängen (links und rechts) zwischen zwei Punkten.
    private Pair getDifferences(Pair currentPoint, Pair targetPoint) {
        double leftDiff = getDistance(motorLinks, targetPoint) - getDistance(motorLinks, currentPoint);
        double rightDiff = getDistance(motorRechts, targetPoint) - getDistance(motorRechts, currentPoint);
        return new Pair(leftDiff, rightDiff);
    }

    // Berechnet die Distanz zwischen zwei Punkten.
    private double getDistance(Pair a, Pair b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Führt die physische Bewegung durch, indem die Differenz in Schritte umgerechnet und die
     * entsprechenden Signale an die Motoren gesendet werden.
     *
     * @param diff Differenz in den Seillängen (links und rechts)
     * @return Die tatsächlich gefahrenen Delta-Werte
     */
    private Pair move(Pair diff) {
        int stepLeft = getSteps(diff.getX());
        int stepRight = getSteps(diff.getY());
        double leftDelta = stepLeft / stepsPerMM;
        double rightDelta = stepRight / stepsPerMM;
        if (diff.getX() < 0) {
            leftDelta *= -1;
        }
        if (diff.getY() < 0) {
            rightDelta *= -1;
        }
        Pair result = new Pair(leftDelta, rightDelta);
        if (simulation) {
            return result;
        }
        // Setzt die Fahrtrichtung
        if (diff.getX() > 0) {
            dirLeft.low();
        } else {
            dirLeft.high();
        }
        if (diff.getY() > 0) {
            dirRight.high();
        } else {
            dirRight.low();
        }
        int maxSteps = Math.max(stepLeft, stepRight);
        while (maxSteps > 0) {
            pause(currentPause);
            if (stepLeft > 0) {
                stepLeft--;
                pulLeft.high();
            }
            if (stepRight > 0) {
                stepRight--;
                pulRight.high();
            }
            pause(currentPause);
            pulLeft.low();
            pulRight.low();
            maxSteps--;
        }
        return result;
    }

    // Berechnet die Anzahl der Schritte, die für eine gegebene Strecke notwendig sind.
    private int getSteps(double diff) {
        return new BigDecimal(Math.abs(diff * stepsPerMM))
                   .setScale(0, RoundingMode.HALF_UP)
                   .intValue();
    }

    // Erzeugt Zwischenpunkte entlang einer Strecke, um eine glattere Bewegung zu ermöglichen.
    private void getPathPoints(ArrayList<Pair> paramPairList, Pair currentPoint, Pair targetPoint) {
        paramPairList.clear();
    	double distance = getDistance(currentPoint, targetPoint);
        double path = step;
        while (path < distance) {
        	paramPairList.add(calculatePoint(path, currentPoint, targetPoint));
            path += step;
        }
        paramPairList.add(targetPoint);
    }

    // Berechnet einen Punkt auf der Strecke zwischen currentPoint und targetPoint bei gegebener Entfernung.
    private Pair calculatePoint(double path, Pair currentPoint, Pair targetPoint) {
        double totalDistance = getDistance(currentPoint, targetPoint);
        if (totalDistance == 0) {
            return currentPoint;
        }
        double dx = (targetPoint.getX() - currentPoint.getX()) * path / totalDistance;
        double dy = (targetPoint.getY() - currentPoint.getY()) * path / totalDistance;
        return new Pair(currentPoint.getX() + dx, currentPoint.getY() + dy);
    }

    public void addDriverMoveObserverIf(DriverMoveObserverIf observer) {
        observerList.add(observer);
    }

    public void setPause(boolean pause) {
        this.pause = pause;
    }

    public void stop() {
        stop = true;
    }

    public void setSimulation(boolean simulation) {
        this.simulation = simulation;
    }

    /**
     * Lädt Konfigurationen aus der Plotter-Konfigurationsdatei und aktualisiert interne Parameter.
     */
    public void loadProperty() {
        String configFile = SettingsManager.getCurrent().getValue(SettingsManager.KEY.CONFIG);
        if (configFile == null) {
            configFile = "conf/default.plotterconf";
        }
        File file = new File(configFile);
        if (file.exists()) {
            VPlotterPropertiesManager.getCurrent().load(file);
        }
        paperHeight = Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.PAPERHEIGHT));
        paperWidth = Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.PAPERWIDTH));
        minX = Integer.parseInt(VPlotterPropertiesManager.getCurrent().getValue(KEY.MINX));
        minY = Integer.parseInt(VPlotterPropertiesManager.getCurrent().getValue(KEY.MINY));
        plotterSide[0] = Integer.parseInt(VPlotterPropertiesManager.getCurrent().getValue(KEY.PLOTTERWIDTH));
        plotterSide[1] = Integer.parseInt(VPlotterPropertiesManager.getCurrent().getValue(KEY.PLOTTERHEIGHT));
        maxX = minX + plotterSide[0];
        maxY = minY + plotterSide[1];
        lengthBetween = Integer.parseInt(VPlotterPropertiesManager.getCurrent().getValue(KEY.SPACEBETWEEN));
        step = Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.SECTORLENGTH));
        stepsPerMM = Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPSPERMM));
        startPoint = new Pair(
                Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STARTPOINTX)), 
                Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STARTPOINTY))
        );
        stepPauseDraw = Long.parseLong(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPPAUSEDRAW));
        stepPauseSlack = Long.parseLong(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPPAUSESLACK));
        currentPoint = new Pair(startPoint.getX(), startPoint.getY());
        motorLinks = new Pair(0, 0);
        motorRechts = new Pair(lengthBetween, 0);
        // Berechne Offsets zur Zentrierung des Plotterbereichs
        offsetX = (paperWidth - plotterSide[0]) / 2;
        offsetY = (paperHeight - plotterSide[1]) / 2;
    }

    private DigitalOutput createDigitalOutput(Context pi4j, int pin) {
        var config = DigitalOutput.newConfigBuilder(pi4j)
                        .address(pin)
                        .shutdown(DigitalState.LOW)
                        .initial(DigitalState.LOW)
                        .provider("gpiod-digital-output");
        return pi4j.create(config);  
    }

    public double[] getPlotterSide() {
        return plotterSide;
    }

    public Pair getNullPoint() {
        return new Pair(minX, minY);
    }

    private boolean isReady() {
        return !simulation && gpio;
    }

    public boolean isUsed() {
        return used;
    }

    public Pair getStartPoint() {
        return startPoint;
    }

    /**
     * Wartet für die gegebene Zeit (in Mikrosekunden).
     */
    private void pause(long micros) {
        long targetTime = System.nanoTime() + micros * 1000;
        while (System.nanoTime() < targetTime);
    }
}
