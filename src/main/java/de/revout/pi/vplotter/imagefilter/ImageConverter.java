package de.revout.pi.vplotter.imagefilter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ImageConverter{

	private ArrayList<Filter> filterList;
	
	public ImageConverter() {
		filterList = new ArrayList<>();
	}
	
	public BufferedImage filter(BufferedImage paramImage) {
		BufferedImage image = paramImage;
		for(Filter filter:filterList) {
			image = filter.filter(image);
		}
		filterList.clear();
		return image;
	}

	public void addFilter(Filter paramFilter) {
		filterList.add(paramFilter);
	}
	
	public void removeFilter(int paramIndex) {
		filterList.remove(paramIndex);
	}
	
	public void clear() {
		filterList.clear();
	}
}
