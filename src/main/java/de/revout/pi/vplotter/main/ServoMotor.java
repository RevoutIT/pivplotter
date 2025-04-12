package de.revout.pi.vplotter.main;

import com.pi4j.context.Context;
import com.pi4j.io.pwm.Pwm;
import com.pi4j.io.pwm.PwmType;

public class ServoMotor {
    // Konstanten
    protected static final int DEFAULT_FREQUENCY = 50;
    protected static final float DEFAULT_MIN_ANGLE = -90f;
    protected static final float DEFAULT_MAX_ANGLE = 90f;
    protected static final float DEFAULT_MIN_DUTY_CYCLE = 2f;
    protected static final float DEFAULT_MAX_DUTY_CYCLE = 12f;

    // Servo-Parameter
    private final float minAngle;
    private final float maxAngle;
    private final float minDutyCycle;
    private final float maxDutyCycle;

    // Benutzerdefinierter Wertebereich (Standard: 0 bis 1)
    private float minRange = 0;
    private float maxRange = 1;
    
    protected final Pwm pwm;

    /**
     * Erzeugt einen neuen ServoMotor mit benutzerdefinierter PWM-Konfiguration sowie Winkel- und Duty-Cycle-Bereichen.
     *
     * @param pi4j         Pi4J-Kontext
     * @param channel      BCM-Pin-Adresse
     * @param frequency    PWM-Frequenz
     * @param minAngle     Minimaler Winkel in Grad
     * @param maxAngle     Maximaler Winkel in Grad
     * @param minDutyCycle Minimaler Duty Cycle (in Prozent, 0 bis 100)
     * @param maxDutyCycle Maximaler Duty Cycle (in Prozent, 0 bis 100)
     */
    public ServoMotor(Context pi4j, int channel, int frequency, float minAngle, float maxAngle, float minDutyCycle, float maxDutyCycle) {
        pwm = pi4j.create(Pwm.newConfigBuilder(pi4j)
                .address(channel)
                .pwmType(PwmType.HARDWARE)
                .frequency(frequency)
                .initial(0)
                .shutdown(0)
                .provider("linuxfs-pwm")
                .build());
        this.minAngle = minAngle;
        this.maxAngle = maxAngle;
        this.minDutyCycle = minDutyCycle;
        this.maxDutyCycle = maxDutyCycle;
    }

    /**
     * Setzt den ServoMotor auf den Winkel 0 und schaltet den PWM-Ausgang aus.
     */
    public void reset() {
        setAngle(0);
        pwm.off();
    }

    /**
     * Rotiert den ServoMotor auf den angegebenen Winkel.
     * Der Winkel wird auf den Bereich [minAngle, maxAngle] begrenzt.
     *
     * @param angle Neuer absoluter Winkel in Grad.
     */
    public void setAngle(float angle) {
        pwm.on(mapAngleToDutyCycle(angle));
    }

    /**
     * Steuert den ServoMotor anhand eines Prozentwerts.
     * 0% entspricht dem minimalen, 50% dem mittleren und 100% dem maximalen Winkel.
     *
     * @param percent Prozentwert, automatisch zwischen 0 und 100 begrenzt.
     */
    public void setPercent(float percent) {
        moveOnRange(percent, 0, 100);
    }

    /**
     * Bewegt den ServoMotor, indem ein Wert aus dem benutzerdefinierten Bereich auf den vollen Winkelbereich abgebildet wird.
     * Falls kein eigener Bereich definiert wurde, wird der Standardbereich von 0 bis 1 verwendet.
     *
     * @param value Wert, der abgebildet werden soll.
     */
    public void moveOnRange(float value) {
        moveOnRange(value, minRange, maxRange);
    }

    /**
     * Bewegt den ServoMotor, indem ein Wert aus einem angegebenen Bereich auf den vollen Winkelbereich abgebildet wird.
     *
     * @param value    Wert, der abgebildet werden soll.
     * @param minValue Minimalwert des Eingabebereichs.
     * @param maxValue Maximalwert des Eingabebereichs.
     */
    public void moveOnRange(float value, float minValue, float maxValue) {
        pwm.on(mapToDutyCycle(value, minValue, maxValue));
    }

    /**
     * Legt den benutzerdefinierten Wertebereich fest, der bei {@link #moveOnRange(float)} verwendet wird.
     * Diese Änderung betrifft zukünftige Aufrufe, nicht jedoch die aktuelle Position.
     *
     * @param minValue Minimalwert des Bereichs.
     * @param maxValue Maximalwert des Bereichs.
     */
    public void setRange(float minValue, float maxValue) {
        this.minRange = minValue;
        this.maxRange = maxValue;
    }

    /**
     * Gibt den minimal konfigurierten Winkel zurück.
     *
     * @return Minimaler Winkel in Grad.
     */
    public float getMinAngle() {
        return minAngle;
    }

    /**
     * Gibt den maximal konfigurierten Winkel zurück.
     *
     * @return Maximaler Winkel in Grad.
     */
    public float getMaxAngle() {
        return maxAngle;
    }

    /**
     * Wandelt einen Winkel in den entsprechenden Duty Cycle um.
     *
     * @param angle Gewünschter Winkel in Grad.
     * @return Entsprechender Duty Cycle.
     */
    private float mapAngleToDutyCycle(float angle) {
        return mapToDutyCycle(angle, minAngle, maxAngle);
    }

    /**
     * Wandelt einen Wert aus einem bestimmten Eingabebereich in einen Duty Cycle um.
     *
     * @param input      Der umzuwandelnde Wert.
     * @param inputStart Minimaler Eingabewert.
     * @param inputEnd   Maximaler Eingabewert.
     * @return Duty Cycle, der der gewünschten Position entspricht.
     * @throws IllegalArgumentException wenn inputStart gleich inputEnd ist.
     */
    private float mapToDutyCycle(float input, float inputStart, float inputEnd) {
        return mapRange(input, inputStart, inputEnd, minDutyCycle, maxDutyCycle);
    }

    /**
     * Wandelt einen Wert von einem Eingabebereich in einen Ausgabebereich um.
     * Der Eingabewert wird automatisch in den gültigen Bereich eingeschränkt.
     * Die Abbildung ist umgekehrt, d.h. ein Wert am Anfang des Eingabebereichs
     * entspricht dem Ende des Ausgabebereichs.
     *
     * @param input       der umzuwandelnde Wert
     * @param inputStart  Start des Eingabebereichs
     * @param inputEnd    Ende des Eingabebereichs
     * @param outputStart Start des Ausgabebereichs
     * @param outputEnd   Ende des Ausgabebereichs
     * @return der abgebildete Wert im Ausgabebereich
     * @throws IllegalArgumentException wenn inputStart gleich inputEnd ist
     */
    private float mapRange(float input, float inputStart, float inputEnd, float outputStart, float outputEnd) {
        if (inputStart == inputEnd) {
            throw new IllegalArgumentException("inputStart und inputEnd müssen unterschiedliche Werte sein");
        }
        
        // Sicherstellen, dass der Eingabebereich in aufsteigender Reihenfolge vorliegt
        if (inputStart > inputEnd) {
            float tmp = inputStart;
            inputStart = inputEnd;
            inputEnd = tmp;
        }
        
        // Sicherstellen, dass der Ausgabebereich in aufsteigender Reihenfolge vorliegt
        if (outputStart > outputEnd) {
            float tmp = outputStart;
            outputStart = outputEnd;
            outputEnd = tmp;
        }
        
        // Clamp des Eingabewerts auf den gültigen Bereich
        float clampedInput = Math.max(inputStart, Math.min(input, inputEnd));
        
        // Berechne das Verhältnis des geclampeten Wertes im Eingabebereich
        float ratio = (clampedInput - inputStart) / (inputEnd - inputStart);
        
        // Umgekehrte Abbildung: Bei ratio == 0 wird outputEnd, bei ratio == 1 outputStart zurückgegeben
        return outputEnd - (outputEnd - outputStart) * ratio;
    }
}
