package de.revout.pi.vplotter.model;

import java.awt.image.BufferedImage;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;

import de.revout.pi.vplotter.converter.Pair;
import de.revout.pi.vplotter.converter.SVGToPolygonConverter;
import de.revout.pi.vplotter.converter.Section;
import de.revout.pi.vplotter.imagefilter.EdgeDetectionFilter;
import de.revout.pi.vplotter.imagefilter.Filter;
import de.revout.pi.vplotter.imagefilter.GrayscaleFilter;
import de.revout.pi.vplotter.imagefilter.RotateFilter;
import de.revout.pi.vplotter.setting.Setting;

public class Model {
	// Mainview
	private List<String> svgData;
	private List<Section> sectionList;
	private static  Model current;
	private boolean sameFile;
	private SVGToPolygonConverter svgToPolygonConverter;
	private BufferedImage image;
	private BufferedImage originalImage;
	private BufferedImage svgImage;
	private List<Filter> filter;
	private List<Filter> usedFilter;
	private List<Update> updateList; 
	
	private Model() {
		updateList = new ArrayList<Update>();
		svgData = Collections.emptyList();
		sameFile = true;
		filter = new ArrayList<Filter>();
		usedFilter = new ArrayList<Filter>();
		filter.add(new RotateFilter());
		filter.add(new GrayscaleFilter());
		filter.add(new EdgeDetectionFilter());
		svgToPolygonConverter = new SVGToPolygonConverter();
		sectionList = new ArrayList<>();
	}
	
	public void register(Update paramUpdate) {
		updateList.add(paramUpdate);
	}
	
	private void update() {
		for (Update update : updateList) {
			update.update();
		}
	}

	public static Model getCurrent() {
		if(current== null) {
			current = new Model();
		}
		return current;
	}
	// plotterView
	/**
	 * 
	 * @param startPoint
	 * @param endPoint
	 * @return länge der entfernung
	 */
	public double getLenght(Pair startPoint, Pair endPoint) {
		return Math.sqrt(Math.pow(startPoint.getX() - endPoint.getX(), 2) + Math.pow(startPoint.getY() - endPoint.getY(), 2));
	}

	/**
	 * 
	 * @param lastPoint
	 * @param list
	 * @return Section die als nächster drann ist
	 */
	public Section getNextSection(Pair lastPoint, List<Section> list) {
		Section section = list.get(0);
		if (list.size() > 1) {
			double way = getLenght(lastPoint, section.getStartPoint());
			for (int i = 1; i < list.size(); i++) {
				Section s = list.get(i);
				double cWay = getLenght(lastPoint, s.getStartPoint());
				if (way > cWay) {
					section = s;
					way = cWay;
				}
			}
		}
		return section;

	}

	/**
	 * 
	 * @return Liste mit Sections
	 */
	public List<Section> createSectionList() {
		List<Section> list = new ArrayList<>();
		List<Section> result = new ArrayList<>();
		Section section = null;

		for (String line : svgData) {
			if (line.startsWith("M")) { //$NON-NLS-1$
				if (section != null) {
					list.add(section);
					section = null;
				}
				double[] linedata = Arrays.asList(line.substring(1).split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$
				section = new Section(linedata[linedata.length - 2], linedata[linedata.length - 1]);
			} else {
				section.addLineData(line.substring(1));
			}
		}

		if (section != null) {
			list.add(section);
			section = null;
		}

		if (!list.isEmpty()) {
			Section current = list.get(0);
			list.remove(0);
			result.add(current);

			while (list.size() > 0) {
				current = getNextSection(current.getLastPoint(), list);
				list.remove(current);
				result.add(current);
			}
		}
		return result;
	}

	// LoadingPannel
	/**
	 * 
	 * @param paramBufferedImage
	 * @return pfad des erstellten bildes oder null
	 */
	public String saveBMPTemp() {
		try {
			Path file = Files.createTempFile("Buff_TO_BMP_", ".bmp"); //$NON-NLS-1$ //$NON-NLS-2$
//			Path file = Paths.get("temp/tempbmp"+System.currentTimeMillis()+".bmp");
			BufferedImage image = this.getImage();
			ImageIO.write(image, "bmp", file.toFile()); //$NON-NLS-1$
			return file.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;

	}
	public boolean generateSVGFromBMP(String paramPathToPotrace, String paramPath) throws Exception{
		if (!"".equals(paramPath) && paramPath.endsWith(".bmp")) { //$NON-NLS-1$ //$NON-NLS-2$
				if(svgData.size()==0||!sameFile) {
					Path file = Files.createTempFile("BMP_TO_SVG_", ".svg"); //$NON-NLS-1$ //$NON-NLS-2$
					Runtime rt = Runtime.getRuntime();
	
					if(paramPathToPotrace!=null && Files.exists(Paths.get(paramPathToPotrace))){
						String[] command = { paramPathToPotrace, "-o", file.toFile().getAbsolutePath(), "--flat", "-b", "svg", paramPath }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						Process install = rt.exec(command);
						install.waitFor();
						return generateDataFromSVG(file.toString());
					}else {
						throw new FileNotFoundException("the program potrace not found! Do check your settings!");
					}
				}
				return false;
		}
		return false;
	}
	

	public boolean generateDataFromSVG(String paramPath) {
		if (!"".equals(paramPath) && paramPath.endsWith(".svg")) { //$NON-NLS-1$ //$NON-NLS-2$
			Path path = Paths.get(paramPath);
			try {
				if (Files.size(path)==0) {
					return false;
				}
				svgData = svgToPolygonConverter.convertSVG(path);
				svgImage= new BufferedImage((int)svgToPolygonConverter.getWidth(), (int)svgToPolygonConverter.getHeight(), 1);
				sameFile = true;
				return true;
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
		return false;
	}
	
	public List<String> getSvgData() {
		return svgData;
	}

	public void setSvgData(List<String> svgData) {
		this.svgData = svgData;
	}

	public List<Section> getSectionList() {
		return sectionList;
	}

	public void setSectionList(List<Section> sectionList) {
		this.sectionList = sectionList;
	}

	public boolean isSameFile() {
		return sameFile;
	}

	public void setSameFile(boolean sameFile) {
		this.sameFile = sameFile;
	}

	public SVGToPolygonConverter getSvgToPolygonConverter() {
		return svgToPolygonConverter;
	}

	public void setSvgToPolygonConverter(SVGToPolygonConverter svgToPolygonConverter) {
		this.svgToPolygonConverter = svgToPolygonConverter;
	}

	public BufferedImage getImage() {
		return image;
	}

	public void setImage(BufferedImage image) {
		this.image = image;
	}

	public BufferedImage getOriginalImage() {
		return originalImage;
	}

	public void setOriginalImage(BufferedImage originalImage) {
		this.originalImage = originalImage;
		
	}

	public boolean checkIfGrey() {
		for (int x = 0; x < getImage().getWidth(); x++) {
			for (int y = 0; y < Model.getCurrent().getImage().getHeight(); y++) {
				int rgb = Model.getCurrent().getImage().getRGB(x, y);
				int red = (rgb >> 16) & 0xff;
				int green = (rgb >> 8) & 0xff;
				int blue = rgb & 0xff;
				if(red != blue || blue != green || green != red ) {
					 return false;
				}
			}
		}
		return true;
	}

	public List<Filter> getFilter() {
		return filter;
	}

	public BufferedImage getSvgImage() {
		return svgImage;
	}

	public void setSvgImage(BufferedImage svgImage) {
		this.svgImage = svgImage;
	}
	
	public void editFilterSetting(Filter paramFilter,Setting paramSetting,String paramSettingValue) {
		paramFilter.getSetting(paramSetting).setValue(paramSettingValue);
		updateImage();
		update();
	}
	
	public void addFilter(Filter paramFilter) {
		usedFilter.add(paramFilter);
		updateImage();
		update();
	}
	
	public void removeFilter(Filter paramFilter) {
		usedFilter.remove(paramFilter);
		updateImage();
		update();
	}
	
	public void removeAllFilter() {
		usedFilter.clear();
		updateImage();
		update();
	}

	public void updateImage() {
		image=originalImage;
		if (!usedFilter.isEmpty()) {
			for (Filter filter : usedFilter) {
				image=filter.filter(image);
			}	
		}
	}

	public List<Filter> getUsedFilter() {
		return usedFilter;
	}

	public void newProcess() {
		usedFilter.clear();
		sectionList.clear();
		svgData.clear();
		image=null;
		originalImage=null;
		svgImage=null;
		update();
	}

	
}
