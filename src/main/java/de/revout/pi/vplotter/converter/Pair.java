package de.revout.pi.vplotter.converter;

public class Pair {
	
	private double x;
	private double y;
	
	
	public Pair(double paramX, double paramY) {
		x= paramX;
		y= paramY;
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
	
	@Override
	public String toString() {
		return getX()+" : "+getY(); //$NON-NLS-1$
	}
	
	public Pair calulateDifferenceTo(Pair paramToPair) {
		return new Pair(x-paramToPair.getX(), y-paramToPair.getY());
	}
	
	public Pair add(Pair paramToPair) {
		x=x+paramToPair.getX();
		y=y+paramToPair.getY();
		return this;
	}

	
}
