package de.revout.pi.vplotter.setting;

import java.util.ArrayList;
import java.util.List;

public class ComboSetting extends Setting {
	public ComboSetting(String paramName, String paramValue) {
		super(paramName,paramValue);
		standardValue = paramValue;
		valueList = new ArrayList<>();
	}

	private List<String> valueList;
	private String standardValue;

	public List<String> getValueList() {
		return valueList;
	}

	public void setValueList(List<String> valueList) {
		this.valueList = valueList;
	}

	public String getStandardValue() {
		return standardValue;
	}

	public void setStandardValue(String standardValue) {
		this.standardValue = standardValue;
	}

}
