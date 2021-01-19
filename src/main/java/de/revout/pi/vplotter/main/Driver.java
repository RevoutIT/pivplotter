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

import com.pi4j.wiringpi.Gpio;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.saves.SettingsManager;
import de.revout.pi.vplotter.saves.VPlotterPropertiesManager;
import de.revout.pi.vplotter.saves.VPlotterPropertiesManager.KEY;

public class Driver {

	private int ENALeft;
	private int ENARight;
	private int DIRLeft;
	private int DIRRight;
	private int PULLeft;
	private int PULLRight;
	private int servoPin;
	private int penaway;
	private int pendraw;

	private final static int RANGE = 20; 
	private double minX;
	private double minY;
	private double maxX;
	private double maxY;
	private double paperHeight;
	private double paperWidth;
	private double x;
	private double y; 
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
	private long drawPause;

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

		// Letzer Status des Stiftes
		lastDraw = 2;
		// Scale
		scale = 0;

		observerList = new ArrayList<>();

	}

	
	private void setGpioSetting() {
		Path path = Paths.get("conf/gpio.properties");
		if (Files.exists(path)) {
			try(InputStream inputStream = Files.newInputStream(path)) {
				Properties properties= new Properties();
				properties.loadFromXML(inputStream);
				ENALeft=Integer.parseInt(properties.getProperty("ENALEFT"));
				ENARight=Integer.parseInt(properties.getProperty("ENARIGHT"));
				DIRLeft=Integer.parseInt(properties.getProperty("DIRLEFT"));
				DIRRight=Integer.parseInt(properties.getProperty("DIRRIGHT"));
				PULLeft=Integer.parseInt(properties.getProperty("PULLEFT"));
				PULLRight=Integer.parseInt(properties.getProperty("PULLRIGHT"));
				servoPin=Integer.parseInt(properties.getProperty("SERVOPIN"));
				penaway=Integer.parseInt(properties.getProperty("PENAWAY"));
				pendraw=Integer.parseInt(properties.getProperty("PENDRAW"));
			} catch (Exception e) {
				throw new RuntimeException(path.toString()+" Error:"+e.getMessage());
			}
		}else {
			throw new RuntimeException(path.toString()+" not found!");
		}
	}

	public void test() {
		List<String> testData = new ArrayList<>();
		loadProperty();
		testData.add("0,0,0"); //$NON-NLS-1$
		testData.add("1,0,"+plotterSide[1]); //$NON-NLS-1$
		testData.add("1,"+plotterSide[0]+","+plotterSide[1]); //$NON-NLS-1$ //$NON-NLS-2$
		testData.add("1,"+plotterSide[0]+",0"); //$NON-NLS-1$ //$NON-NLS-2$
		testData.add("1,0,0"); //$NON-NLS-1$
		testData.add("1,"+plotterSide[0]+","+plotterSide[1]); //$NON-NLS-1$ //$NON-NLS-2$
		testData.add("0,"+plotterSide[0]+",0"); //$NON-NLS-1$ //$NON-NLS-2$
		testData.add("1,0,"+plotterSide[1]); //$NON-NLS-1$
		plotte(testData, plotterSide[0], plotterSide[1]);
	}
	
	
	public void plotte(List<String> paramList, double paramWidth, double paramHeight) {
		if(!simulation) {
			if(!gpioInit()) {
				return;
			}
		}
		for (DriverMoveObserverIf driverMoveObserverIf : observerList) {
			driverMoveObserverIf.init();
		}
		motorsOn();
		stop = false;
		pause = false;

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					
					String[] split;
					scale = calculateScale(paramWidth, paramHeight);
					for (int i = 0; i < paramList.size(); i++) {

						while (pause) {
							if (stop) {
								return;
							}
							Thread.sleep(1000);
						}
						split = paramList.get(i).split(","); //$NON-NLS-1$
						int drawState = Integer.parseInt(split[0]);
						if (lastDraw != drawState) {
							draw(drawState);
						}

						Pair toPoint = new Pair((Double.parseDouble(split[1]) / scale) + minX,
								(Double.parseDouble(split[2]) / scale) + minY);

						ArrayList<Pair> pathPoints = getPathPoints(currentPoint, toPoint);

						for (Pair pathPoint : pathPoints) {
							if (isInRange(pathPoint)) {
								Pair diff = getDifferences(currentPoint, pathPoint);
								Pair driveDelta = move(diff);
								calculateRealCurrentPosition(driveDelta);
							}
						}
						for (DriverMoveObserverIf driverMoveObserverIf : observerList) {
							driverMoveObserverIf.currentMove(drawState, currentPoint, paramList.size(), i);
						}
						
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				} finally {
					finish();
				}
			}

		}).start();
	}

	private boolean gpioInit() {
		if (!init) {
			init = gpioStart();
		}
		return init;
	}

	private void calculateRealCurrentPosition(Pair driveDelta) {
		double lengtLeftFrom = getLenght(motorLinks, currentPoint);
		double lengtRightFrom = getLenght(motorRechts, currentPoint);
		double lengtLeftAftermove = lengtLeftFrom + driveDelta.getX();
		double lengtRightAftermove = lengtRightFrom + driveDelta.getY();
		double cosAlpha = (Math.pow(lengthBetween, 2) + Math.pow(lengtLeftAftermove, 2)
				- Math.pow(lengtRightAftermove, 2)) / (2 * lengthBetween * lengtLeftAftermove);
		double x = cosAlpha * lengtLeftAftermove;
		double y = Math.sqrt(Math.pow(lengtLeftAftermove, 2) - Math.pow(x, 2));
		currentPoint.setX(x);
		currentPoint.setY(y);
	}
	
	public void goTo(Pair paramPosition) {
		if(isReady()) {
			draw(0);
			Pair diff = getDifferences(currentPoint, paramPosition);
			Pair driveDelta = move(diff);
			calculateRealCurrentPosition(driveDelta);
		}
	}

	private void finish() {
		used=true;
		goTo(startPoint);
		for (DriverMoveObserverIf driverMoveObserverIf : observerList) {
			driverMoveObserverIf.finish();
		}
	}
	

	// Pins initialisieren
	private boolean gpioStart() {
		if (!simulation) {
			try {
				Gpio.wiringPiSetupGpio();
				Gpio.pinMode(servoPin, Gpio.PWM_OUTPUT);
				Gpio.pwmSetMode(Gpio.PWM_MODE_MS);
				Gpio.pwmSetClock(384);
				Gpio.pwmSetRange(1000);
				Gpio.pinMode(ENALeft, Gpio.OUTPUT);
				Gpio.digitalWrite(ENALeft, false);
				Gpio.pinMode(ENARight, Gpio.OUTPUT);
				Gpio.digitalWrite(ENARight, false);
				Gpio.pinMode(DIRLeft, Gpio.OUTPUT);
				Gpio.digitalWrite(DIRLeft, true);
				Gpio.pinMode(DIRRight, Gpio.OUTPUT);
				Gpio.digitalWrite(DIRRight, true);
				Gpio.pinMode(PULLeft, Gpio.OUTPUT);
				Gpio.digitalWrite(PULLeft, false);
				Gpio.pinMode(PULLRight, Gpio.OUTPUT);
				Gpio.digitalWrite(PULLRight, false);
				gpio=true;
			}catch(Throwable exc) {
				gpio=false;
				simulation=true;
			}
		}
		return gpio;
		
	}
	
	public void motorsOff() {
		if(!simulation) {
			if(gpioInit()) {
				Gpio.digitalWrite(ENALeft, true);
				Gpio.digitalWrite(ENARight, true);
			}
		}
		pause=true;
		stop();
	}
	
	public void motorsOn() {
		if(!simulation) {
			if(gpioInit()) {
				Gpio.digitalWrite(ENALeft, false);
				Gpio.digitalWrite(ENARight, false);
			}
		}
	}

	public void gpioStop() {
		if(isReady()) {
			try {
				if(gpioInit()) {
					Gpio.digitalWrite(ENALeft, false);
					Gpio.digitalWrite(ENARight, false);
				}
			} catch (Exception e) {

			}
		}
	}

	// Skalierung berechnen
	private double calculateScale(double paramWidth, double paramHeight) {
		return Double.max(paramWidth / plotterSide[0], paramHeight / plotterSide[1]);
	}

	// Servo bewegen, um den Malstatus zu ändern
	private void draw(int paramDrawState) {
		if(isReady()) {
			try {
				if (paramDrawState == 0) {
					currentPause = stepPauseSlack;
					Gpio.pwmWrite(servoPin, penaway);
					pause(drawPause);
				} else {
					currentPause = stepPauseDraw;
					Gpio.pwmWrite(servoPin, pendraw);
					pause(drawPause);
				}
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}

		lastDraw = paramDrawState;
	}
	
	public void showPageLocation() {
		List<String> testData = new ArrayList<>();
		loadProperty();
		minX=minX-x;
		minY=minY-y;
		testData.add("0,15,0"); //$NON-NLS-1$
		testData.add("1,0,0"); //$NON-NLS-1$
		testData.add("1,0,15"); //$NON-NLS-1$
		testData.add("0,"+(paperWidth-15)+",0"); //$NON-NLS-1$
		testData.add("1,"+paperWidth+",0"); //$NON-NLS-1$
		plotte(testData,paperWidth, paperHeight);
	}


	// Prüfen, ob der Punkt noch innerhalb des Maximalen der Motoren ist
	private boolean isInRange(Pair paramPoint) {
		if (paramPoint.getX() < (minX-RANGE) || paramPoint.getX() > (maxX+RANGE) || paramPoint.getY() < (minY-RANGE)
				|| paramPoint.getY() > (maxY+RANGE)) {
			return false;
		}
		return true;
	}

	// Benötigte veränderung berechnen, um von einem Punkt zu einem andere Punkt zu
	// bewegen
	private Pair getDifferences(Pair paramCurrentPoint, Pair paramToPoint) {
		double lengthDifferenceLeft = getLenght(motorLinks, paramToPoint) - getLenght(motorLinks, paramCurrentPoint);
		double lengthDifferenceRight = getLenght(motorRechts, paramToPoint) - getLenght(motorRechts, paramCurrentPoint);
		return new Pair(lengthDifferenceLeft, lengthDifferenceRight);
	}

	// Länge zwischen zwei Punkten berechnen
	private double getLenght(Pair currentPoint, Pair toPoint) {
		return Math.sqrt(Math.pow((toPoint.getY() - currentPoint.getY()), 2)
				+ Math.pow(toPoint.getX() - currentPoint.getX(), 2));
	}

	/**
	 * @param paramPair
	 * @return Liefert die tatsächliche Änderung der Seilen (links und rechts)
	 */
	private Pair move(Pair paramPair) {
		
		int stepLeft = getSteps(paramPair.getX());
		int stepRight = getSteps(paramPair.getY());

		double leftDelta = ((double) stepLeft) / stepsPerMM;
		double rightDelta = ((double) stepRight) / stepsPerMM;

		if (paramPair.getX() < 0) {
			leftDelta *= -1;
		}
		if (paramPair.getY() < 0) {
			rightDelta *= -1;
		}

		Pair result = new Pair(leftDelta, rightDelta);

		if (simulation) {
			return result;
		}

		if (paramPair.getX() > 0) {
			Gpio.digitalWrite(DIRLeft, false);
		} else {
			Gpio.digitalWrite(DIRLeft, true);
		}
		if (paramPair.getY() > 0) {
			Gpio.digitalWrite(DIRRight, true);
		} else {
			Gpio.digitalWrite(DIRRight, false);
		}

		int maxStep = Integer.max(stepLeft, stepRight);

		while (maxStep > 0) {
			pause(currentPause);
			if (stepLeft > 0) {
				stepLeft--;
				Gpio.digitalWrite(PULLeft, true);
			}

			if (stepRight > 0) {
				stepRight--;
				Gpio.digitalWrite(PULLRight, true);
			}
			pause(currentPause);
			Gpio.digitalWrite(PULLeft, false);
			Gpio.digitalWrite(PULLRight, false);
			maxStep--;
		}
		return result;
	}

	// Schritte berechnen, die für eine Strecke notwendig sind
	private int getSteps(double paramDifference) {
		return new BigDecimal(Math.abs(paramDifference * stepsPerMM)).setScale(0, RoundingMode.HALF_UP).intValue();
	}

	// Abschnittspunkte berechnen
	private ArrayList<Pair> getPathPoints(Pair paramCurrentPoint, Pair paramToPoint) {
		ArrayList<Pair> pathPoints = new ArrayList<>();
		double lenght = getLenght(paramCurrentPoint, paramToPoint);
		double path = step;
		while (path < lenght) {
			pathPoints.add(calculatePoint(path, paramCurrentPoint, paramToPoint));
			path += step;
		}
		pathPoints.add(paramToPoint);
		return pathPoints;

	}

	// Koordinaten für Abschnittspunkt bestimmen
	private Pair calculatePoint(double path, Pair paramCurrentPoint, Pair paramToPoint) {
		double C = getLenght(paramCurrentPoint, paramToPoint);
		if (C == 0) {
			return paramCurrentPoint;
		}
		double y = (paramToPoint.getY() - paramCurrentPoint.getY()) * path / C;
		double x = (paramToPoint.getX() - paramCurrentPoint.getX()) * path / C;

		Pair newPoint = new Pair(paramCurrentPoint.getX() + x, paramCurrentPoint.getY() + y);

		return newPoint;
	}

	public void addDriverMoveObserverIf(DriverMoveObserverIf driverMoveObserverIf) {
		observerList.add(driverMoveObserverIf);
	}

	public void setPause(boolean pause) {
		this.pause = pause;
	}

	public void stop() {
		stop = true;
	}

	public void setSimulation(boolean paramSimulation) {
		simulation = paramSimulation;
	}

	public void loadProperty() {
		String configFile = SettingsManager.getCurrent().getValue(de.revout.pi.vplotter.saves.SettingsManager.KEY.CONFIG);
		if(configFile==null) {
			configFile= "conf/default.plotterconf";
		}
		File file = new File(configFile);
		if(file.exists()) {
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
		drawPause = Long.parseLong(VPlotterPropertiesManager.getCurrent().getValue(KEY.DRAWPAUSE));
		stepsPerMM = Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPSPERMM));
		startPoint = new Pair(Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STARTPOINTX)), Double.parseDouble(VPlotterPropertiesManager.getCurrent().getValue(KEY.STARTPOINTY)));
		stepPauseDraw = Long.parseLong(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPPAUSEDRAW));
		stepPauseSlack = Long.parseLong(VPlotterPropertiesManager.getCurrent().getValue(KEY.STEPPAUSESLACK));
		currentPoint = new Pair(startPoint.getX(), startPoint.getY());
		motorLinks = new Pair(0, 0);
		motorRechts = new Pair(lengthBetween, 0);
		x = (paperWidth-plotterSide[0])/2;
		y = (paperHeight-plotterSide[1])/2;
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
	
	private void pause(long paramMicros) {
		long toTime = System.nanoTime()+paramMicros*1000;
		while(toTime>System.nanoTime());
	}

}
