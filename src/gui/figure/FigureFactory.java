package gui.figure;

import gui.figure.series.XYSeries;
import gui.figure.series.XYSeriesElement;
import gui.figure.series.XYSeriesFigure;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.CellRendererPane;

public class FigureFactory {

	
	/**
	 * Layout all components in the given container and all components in child
	 * containers
	 * @param c
	 */
	public static void layoutComponent(Component c) {
		synchronized (c.getTreeLock()) {
			c.doLayout();
			if (c instanceof Container)
				for (Component child : ((Container) c).getComponents())
					layoutComponent(child);
		}
	}
	
	/**
	 * Layout all components of the figure and render it onto a BufferedImage 
	 * @param size
	 * @param fig
	 * @return
	 */
	public static BufferedImage getFigureImage(Dimension size, Figure fig) {
		fig.setSize(size);
		fig.doLayout();
		layoutComponent(fig);
		
		BufferedImage img = new BufferedImage((int)size.getWidth(), (int)size.getHeight(),
                BufferedImage.TYPE_INT_RGB);
		
		CellRendererPane crp = new CellRendererPane();
        crp.add(fig);
        crp.paintComponent(img.createGraphics(), fig, crp, fig.getBounds());    
        crp.repaint();
        return img; 
	}
	
	/**
	 * Save the figure as a .png image to the given destination. Overwrites existing files 
	 * @param size
	 * @param fig
	 * @param destinationFile
	 * @throws IOException
	 */
	public static void saveFigure(Dimension size, Figure fig, File destinationFile) throws IOException {
		BufferedImage image = getFigureImage(size, fig);
		ImageIO.write(image, "png", destinationFile);
	}
	
	public static XYSeriesFigure createFigure(String xLabel, String yLabel,
			List< List<Point2D>> data, 
			List<String> seriesNames,
			List<Color> colors) {
		XYSeriesFigure fig = new XYSeriesFigure();
		fig.setPreferredSize(new Dimension(500, 500));
		
		int count = 0;
		for(List<Point2D> seriesData : data) {
			XYSeriesElement el = fig.addDataSeries(new XYSeries(seriesData));
			el.setLineColor(colors.get(count));
			el.setName(seriesNames.get(count));
			el.setLineWidth(2f);
			count++;
		}
		
		fig.setXLabel(xLabel);
		fig.setYLabel(yLabel);
		return fig;
	}
	
	
	/**
	 * Create a simple figure with the given X and Y labels, the lists of series as X-Y points,
	 * and the names and colors of the given series.
	 * @param xLabel
	 * @param yLabel
	 * @param data
	 * @param name
	 * @param color
	 * @return
	 */
	public static XYSeriesFigure createFigure(String xLabel, String yLabel,
								List<Point2D> data, 
								String name,
								Color color) {
		List<List<Point2D>> dataList = new ArrayList<List<Point2D>>();
		List<String> names = new ArrayList<String>();
		List<Color> colors = new ArrayList<Color>();
		names.add(name);
		colors.add(color);
		return createFigure(xLabel, yLabel, dataList, names, colors);
	}
	
	
	public static class SeriesProperties {
		String name;
		Color color = Color.blue;
		float width = 2f;
		String style = XYSeriesElement.LINES;
	}
	
	public static void main(String[] args) {	
		List< List<Point2D> > data = new ArrayList< List<Point2D>>();
		data.add(Arrays.asList(new Point2D[]{
				new Point2D.Double(0, 1.0),
				new Point2D.Double(1, 2.0),
				new Point2D.Double(3, 0.1),
				new Point2D.Double(5, -14.0),
				new Point2D.Double(8, 3.0),
				new Point2D.Double(17, 10.0),
				new Point2D.Double(18, 2.0)
		}));
		
		data.add(Arrays.asList(new Point2D[]{
				new Point2D.Double(0, 4.0),
				new Point2D.Double(1, 3.0),
				new Point2D.Double(2, 0.1),
				new Point2D.Double(4, -1.0),
				new Point2D.Double(6, 3.0),
				new Point2D.Double(10, 1.0),
				new Point2D.Double(12, 5.0)
		}));
		
		List<String> names = new ArrayList<String>();
		names.add("My data series");
		names.add("Another data series");
		
		List<Color> colors = new ArrayList<Color>();
		colors.add(Color.green);
		colors.add(Color.magenta);
		
		XYSeriesFigure fig = createFigure("X axis label", "Time", data, names, colors);
		
		File destFile = new File("savedImage.png");
		
		try {
			saveFigure(new Dimension(1000, 1000), fig, destFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		System.exit(0);
		
	}
}
