package de.revout.pi.vplotter.setting;

public class Setting {

	private String name;
	private String value;
	
	public Setting(String paramName,String paramValue) {
		value = paramValue;
		name = paramName;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	
	
	
	
}
