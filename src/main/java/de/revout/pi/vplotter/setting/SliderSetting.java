package de.revout.pi.vplotter.setting;

public class SliderSetting extends Setting{

	private int min;
	private int max;
	private int tick;
	
	public SliderSetting(String paramName, String paramValue, int paramMin, int paramMax, int paramTick) {
		super(paramName, paramValue);
		min = paramMin;
		max = paramMax;
		tick = paramTick;
	}
	
	public int getMax() {
		return max;
	}

	public void setMax(int max) {
		this.max = max;
	}

	public int getMin() {
		return min;
	}

	public void setMin(int min) {
		this.min = min;
	}

	public int getTick() {
		return tick;
	}

	public void setTick(int tick) {
		this.tick = tick;
	}

}
