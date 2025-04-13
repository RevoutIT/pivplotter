package de.revout.pi.vplotter.main;

import de.revout.pi.vplotter.converter.Pair;

public interface DriverMoveObserverIf {

	public void currentMove(int paramState, Pair paramToPoint, long paramStepCount, long paramActualStep);
	public void finish();
	public void init();
	
}
