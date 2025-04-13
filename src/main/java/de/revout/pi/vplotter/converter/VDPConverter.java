package de.revout.pi.vplotter.converter;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;

public class VDPConverter {
	
	private double maxWidth;
	private double maxHeight;
	private Path fileWithData;
	public Path getFileWithData() {
		return fileWithData;
	}

	private long size;
	
	public long getSize() {
		return size;
	}

	private VDPConverter() {
	}
	
	public static VDPConverter buildFromSections(List<Section> paramSectionList) throws Exception{
		VDPConverter converter = new VDPConverter();
		converter.fileWithData = Files.createTempFile("VDPData", "");
		converter.createListForVDP(paramSectionList);
		return converter;
	}
	

	public static VDPConverter buildFromStrings(List<String> paramLineList) throws Exception{
		VDPConverter converter = new VDPConverter();
		converter.fileWithData = Files.createTempFile("VDPData", "");
		try(BufferedWriter writer = Files.newBufferedWriter(converter.fileWithData,StandardOpenOption.CREATE)){		
			for (String s : paramLineList) {
				converter.maxWidth = Math.max(converter.maxWidth, Double.parseDouble(s.split(",")[1]));
				converter.maxHeight = Math.max(converter.maxHeight, Double.parseDouble(s.split(",")[2]));

				writer.write(s);
				writer.newLine();
				converter.size++;
			}
		}
		return converter;
		
	}

	private void createListForVDP(List<Section> paramSectionList) throws Exception{
		try(BufferedWriter writer = Files.newBufferedWriter(fileWithData,StandardOpenOption.CREATE)){		
			for (Section s : paramSectionList) {
				createStringForVDP(writer,s);
			}
		}
	}
/**
 * Passt die section an 
 * @param paramSection
 * @return return string mit text der section
 */
	private void createStringForVDP(BufferedWriter writer, Section paramSection) throws Exception{
		maxWidth = Math.max(maxWidth, paramSection.getX());
		maxHeight = Math.max(maxHeight, paramSection.getY());
			writer.write("0," + paramSection.getX() + "," + paramSection.getY());
			writer.newLine();
			size++;
			if (!paramSection.getLineData().isEmpty()) {
				double[] data = Arrays.asList(paramSection.getLineData().split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$
				for (int i = 0; i < data.length; i = i + 2) {
					maxWidth = Math.max(maxWidth, data[i]);
					maxHeight = Math.max(maxHeight, data[i+1]);
					writer.write("1," + data[i] + "," + data[i + 1]); //$NON-NLS-1$ //$NON-NLS-2$
					writer.newLine();
					size++;
				}
			}
	}


public double getMaxWidth() {
	return maxWidth;
}

public double getMaxHeight() {
	return maxHeight;
}
}
