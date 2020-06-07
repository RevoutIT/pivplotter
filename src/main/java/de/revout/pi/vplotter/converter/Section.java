package de.revout.pi.vplotter.converter;

import java.util.Arrays;

public class Section{

	private double x;
	private double y;
	
	private String lineData;
	
	public Section(double paramX, double paramY) {
		x= paramX;
		y= paramY;
		lineData = ""; //$NON-NLS-1$
	}

	public double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public String getLineData() {
		return lineData.trim();
	}

	public void addLineData(String lineData) {
		this.lineData+=" "+lineData.trim(); //$NON-NLS-1$
	}

	public Pair getStartPoint() {
		return new Pair(x, y);
		
	}
	public Pair getLastPoint() {
		if(lineData.isEmpty()) {
			return new Pair(x, y);
		}
		double[] data =  Arrays.asList(lineData.trim().split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$
		return new Pair(data[data.length-2], data[data.length-1]);
				
	}
	
	
}
