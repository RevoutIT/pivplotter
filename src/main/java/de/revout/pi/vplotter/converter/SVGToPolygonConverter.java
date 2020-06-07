package de.revout.pi.vplotter.converter;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SVGToPolygonConverter {

	private double width = 0;

	private double height = 0;

	private double translateX = 0;
	private double translateY = 0;

	private double scaleX = 1;
	private double scaleY = 1;

	private double minX = 0;
	private double maxX = 0;

	private double minY = 0;
	private double maxY = 0;

	public List<String> convertSVG(Path paramPathToSVG) {

		
		
		width = 0;
		height = 0;

		translateX = 0;
		translateY = 0;

		scaleX = 1;
		scaleY = 1;

		minX = 0;
		maxX = 0;

		minY = 0;
		maxY = 0;

		
		try (InputStream fileIS = Files.newInputStream(paramPathToSVG, StandardOpenOption.READ)) {
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = builderFactory.newDocumentBuilder();
			
			Document xmlDocument = builder.parse(fileIS);

			Element root = xmlDocument.getDocumentElement();
			String widthStr = root.getAttribute("width"); //$NON-NLS-1$
			String heightStr = root.getAttribute("height"); //$NON-NLS-1$

			width = Double.parseDouble(widthStr.replaceAll("\\p{Alpha}", "")); //$NON-NLS-1$ //$NON-NLS-2$
			height = Double.parseDouble(heightStr.replaceAll("\\p{Alpha}", "")); //$NON-NLS-1$ //$NON-NLS-2$

			NodeList gList = root.getElementsByTagName("g"); //$NON-NLS-1$
			
			if(gList.getLength()>0) {
			Node gNode = gList.item(0);
			
			NamedNodeMap namedNodeMap = gNode.getAttributes();
			String transformStr = namedNodeMap.getNamedItem("transform").getNodeValue(); //$NON-NLS-1$
			double[] translate = getValuePair(transformStr, "translate", 0); //$NON-NLS-1$
			double[] scale = getValuePair(transformStr, "scale", 1); //$NON-NLS-1$

			translateX = translate[0];
			translateY = translate[1];

			scaleX = scale[0];
			scaleY = scale[1];
			
			}else {
				translateX = 0;
				translateY = 0;
				scaleX = 1;
				scaleY = 1;
			}

			XPath xPath = XPathFactory.newInstance().newXPath();

			String expression = "//path"; //$NON-NLS-1$
			NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);

			List<String> dataList = new ArrayList<>();
			String pathData = ""; //$NON-NLS-1$
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node node = nodeList.item(i);
				pathData = node.getAttributes().getNamedItem("d").getNodeValue(); //$NON-NLS-1$
				
				dataList.addAll(getDataFromPath(pathData));
				

			}

			// In absolute koordinaten umrechnen

			double beginX = 0;
			double beginY = 0;

			double lastMoveX = 0;
			double lastMoveY = 0;

			boolean endWithZ = false;

			BezierCurve bezierCurve = new BezierCurve(20);

			List<String> absolutList = new ArrayList<>();

			for (String rowData : dataList) {
				rowData = rowData.trim();
				int lenght = rowData.length();
				endWithZ = false;
				if (rowData.endsWith("z") || rowData.endsWith("Z")) { //$NON-NLS-1$ //$NON-NLS-2$
					endWithZ = true;
					lenght--;
				}
				String com = rowData.substring(0, 1);
				String newLine = com.toUpperCase();
				if (com.matches("[cCvVHh]")) { //$NON-NLS-1$
					newLine = "L"; //$NON-NLS-1$
				}
				
				rowData = rowData.substring(1, lenght);
				rowData = rowData.replaceAll(",", " ");
				double[] values = Arrays.asList(rowData.split(" ")).stream().mapToDouble(Double::parseDouble).toArray(); //$NON-NLS-1$

				double x = beginX;
				double y = beginY;
				for (int i = 0; i < values.length; i = i + 2) {
					if ("M".equals(com)) { //$NON-NLS-1$
						x = values[i];
						y = values[i + 1];
						beginX = x;
						beginY = y;
						lastMoveX = x;
						lastMoveY = y;
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$
						
						
						newLine = newLine.trim();
						absolutList.add(newLine);
						com="L";
						newLine="";
						
					} else if ("m".equals(com)) { //$NON-NLS-1$
						x = beginX + values[i];
						y = beginY + values[i + 1];
						beginX = x;
						beginY = y;
						lastMoveX = x;
						lastMoveY = y;
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$
						newLine = newLine.trim();
						absolutList.add(newLine);
						com="l";
						newLine="";
						
					} else if ("L".equals(com)) { //$NON-NLS-1$
						x = values[i];
						y = values[i + 1];
						beginX = x;
						beginY = y;
						if(newLine.isEmpty()) {
							newLine="L";
						}
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$
					} else if ("l".equals(com)) { //$NON-NLS-1$
						x = beginX + values[i];
						y = beginY + values[i + 1];
						beginX = x;
						beginY = y;
						if(newLine.isEmpty()) {
							newLine="L";
						}
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$
					} else if ("V".equals(com)) { //$NON-NLS-1$
						y = values[i];
						i=i-1;
						beginY = y;
						newLine += "" + convertX(width, beginX, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$
					} else if ("v".equals(com)) { //$NON-NLS-1$
						y = beginY + values[i];
						i=i-1;
						beginY = y;
						newLine += "" + convertX(width, beginX, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, y, translateY, scaleY) + " "; //$NON-NLS-1$						
					} else if ("H".equals(com)) { //$NON-NLS-1$
						x = values[i];
						i=i-1;
						beginX = x;
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, beginY, translateY, scaleY) + " "; //$NON-NLS-1$
					} else if ("h".equals(com)) { //$NON-NLS-1$
						x = beginX + values[i];
						i=i-1;
						beginX = x;
						newLine += "" + convertX(width, x, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
								+ convertY(height, beginY, translateY, scaleY) + " "; //$NON-NLS-1$						

					} else if ("C".equals(com)) { //$NON-NLS-1$
						double x1 = values[i];
						double y1 = values[i + 1];
						double x2 = values[i + 2];
						double y2 = values[i + 3];
						x = values[i + 4];
						y = values[i + 5];
						i = i + 4;

						double[] points = bezierCurve.bezier2D(new double[] { beginX, beginY, x1, y1, x2, y2, x, y });

						for (int j = 2; j < points.length; j = j + 2) {
							newLine += "" + convertX(width, points[j], translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
									+ convertY(height, points[j + 1], translateY, scaleY) + " "; //$NON-NLS-1$
						}

						beginX = x;
						beginY = y;

					} else if ("c".equals(com)) { //$NON-NLS-1$
						double x1 = beginX + values[i];
						double y1 = beginY + values[i + 1];
						double x2 = beginX + values[i + 2];
						double y2 = beginY + values[i + 3];
						x = beginX + values[i + 4];
						y = beginY + values[i + 5];
						i = i + 4;

						double[] points = bezierCurve.bezier2D(new double[] { beginX, beginY, x1, y1, x2, y2, x, y });

						for (int j = 2; j < points.length; j = j + 2) {
							newLine += "" + convertX(width, points[j], translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
									+ convertY(height, points[j + 1], translateY, scaleY) + " "; //$NON-NLS-1$
						}

						beginX = x;
						beginY = y;

					}

				}
				newLine = newLine.trim();
				if(!newLine.isEmpty()) {
					absolutList.add(newLine);
				}
				if (endWithZ) {
					absolutList.add("L" + convertX(width, lastMoveX, translateX, scaleX) + " " //$NON-NLS-1$ //$NON-NLS-2$
							+ convertY(height, lastMoveY, translateY, scaleY));
					beginX = lastMoveX;
					beginY = lastMoveY;
				}
				

			}

			// Aus Minus-Bereich kommen
			width = maxX;
			height = maxY;

			double xCor = 0;
			double yCor = 0;
			if (minX < 0) {
				xCor = Math.abs(minX)+1;
				width += xCor+10;
			}
			if (minY < 0) {
				yCor = Math.abs(minY)+1;
				height += yCor+10;
			}

			List<String> result = new ArrayList<>();
			for (String row : absolutList) {
				String line = row.substring(0, 1);
				double[] values = Arrays.asList(row.substring(1).split(" ")).stream().mapToDouble(Double::parseDouble) //$NON-NLS-1$
						.toArray();
				for (int i = 0; i < values.length; i = i + 2) {
					line += (values[i] + xCor) + " " + (values[i + 1] + yCor) + " "; //$NON-NLS-1$ //$NON-NLS-2$
				}
				result.add(line.trim());
			}

			return result;

		} catch (Exception e) {
			e.printStackTrace();
		}
		return Collections.emptyList();
	}
	
//	private void saveListWithStringsInFile(List<String> absolutList, Path path) {
//		try {
//			Files.write(path, absolutList);
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}

	private List<String> getDataFromPath(String pathData) {
		ArrayList<String> result = new ArrayList<>();
		
		StringTokenizer stringTokenizer = new StringTokenizer(pathData, " ");
		
		String dataRow = "";
		String token = ""; 
		boolean command = false;
		while (stringTokenizer.hasMoreTokens()) {
			token = stringTokenizer.nextToken();
			
			if (token.matches("[MmLlCcVvHh].*")) {
				if(!dataRow.isEmpty()) {
					result.add(dataRow);
				}
				dataRow=token;
				if(token.matches("[MmLlCcVvHh]")) {
					command=true;
				}
			}else if (token.matches(".*[zZ]")) {
				if(token.matches("[zZ]")) {
					dataRow+=token;	
				}else {
					dataRow+=" "+token.replace(',', ' ');	
				}
				result.add(dataRow);
				dataRow="";
				command=false;
			}else if (token.matches("[\\-0-9].*")) {
				String trim = " ";
				if(command) {
					trim = "";
					command=false;
				}
				dataRow+=trim+token.replace(',', ' ');
			}
		}
		
		if(!dataRow.isEmpty()) {
			result.add(dataRow);
		}
		
		return result;
	}

	private void setMinMaxX(double paramValue) {
		minX = Math.min(minX, paramValue);
		maxX = Math.max(maxX, paramValue);
	}

	private void setMinMaxY(double paramValue) {
		minY = Math.min(minY, paramValue);
		maxY = Math.max(maxY, paramValue);
	}

	private double convertX(double paramMaxValue, double paramValue, double paramTranslate, double paramScal) {
		double result = paramValue;
		if (paramScal > 0) {
			result = (paramValue + paramTranslate) * paramScal;
		} else {
			result = paramMaxValue - (paramValue + paramTranslate) * paramScal * -1;
		}
		setMinMaxX(result);
		return result;
	}

	private double convertY(double paramMaxValue, double paramValue, double paramTranslate, double paramScal) {
		double result = paramValue;
		if (paramScal > 0) {
			result = (paramValue + paramTranslate) * paramScal;
		} else {
			result = paramMaxValue - (paramValue + paramTranslate) * paramScal * -1;
		}
		setMinMaxY(result);
		return result;
	}

	private double[] getValuePair(String paramString, String paramKey, double paramDefault) {
		double[] result = new double[] { paramDefault, paramDefault };
		int startIndex = paramString.indexOf(paramKey);
		if (startIndex > -1) {
			String[] value = paramString
					.substring(startIndex + paramKey.length() + 1, paramString.indexOf(")", startIndex)).split(","); //$NON-NLS-1$ //$NON-NLS-2$
			result = Arrays.asList(value).stream().mapToDouble(Double::parseDouble).toArray();
		}
		return result;
	}

	public double getWidth() {
		return width;
	}

	public double getHeight() {
		return height;
	}

	public double getTranslateX() {
		return translateX;
	}

	public double getTranslateY() {
		return translateY;
	}

	public double getScaleX() {
		return scaleX;
	}

	public double getScaleY() {
		return scaleY;
	}

	public double getMaxX() {
		return maxX;
	}

	public double getMaxY() {
		return maxY;
	}

}
