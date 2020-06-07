package de.revout.pi.vplotter.converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VDPConverter {
	
	private List<String> data;
	private double maxWidth;
	private double maxHeight;
	
	public VDPConverter(List<Section> paramSectionList) {
		createListForVDP(paramSectionList);
		
	}

	private void createListForVDP(List<Section> paramSectionList) {
		data = new ArrayList<>();
		for (Section s : paramSectionList) {
			createStringForVDP(data,s);
		}
	}
/**
 * Passt die section an 
 * @param paramSection
 * @return return string mit text der section
 */
	private void createStringForVDP(List<String> paramRresult, Section paramSection) {
		maxWidth = Math.max(maxWidth, paramSection.getX());
		maxHeight = Math.max(maxHeight, paramSection.getY());
		paramRresult.add("0," + paramSection.getX() + "," + paramSection.getY()); //$NON-NLS-1$ //$NON-NLS-2$
		if (!paramSection.getLineData().isEmpty()) {
			double[] data = Arrays.asList(paramSection.getLineData().split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$
			for (int i = 0; i < data.length; i = i + 2) {
				maxWidth = Math.max(maxWidth, data[i]);
				maxHeight = Math.max(maxHeight, data[i+1]);
				paramRresult.add("1," + data[i] + "," + data[i + 1]); //$NON-NLS-1$ //$NON-NLS-2$
			}
		}
	}

public List<String> getData() {
	return data;
}

public double getMaxWidth() {
	return maxWidth;
}

public double getMaxHeight() {
	return maxHeight;
}
}
