package operator.python;

import java.awt.geom.Point2D;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.sound.sampled.Line;

import math.Histogram;

import operator.OperationFailedException;
import pipeline.Pipeline;

public class CreateFigure {
	
	private static final String tmpDataPath = ".fig.tmpdata.csv";
	
	public static final String lineFigScript = "saveplot.py";
	public static final String imageScript = "saveimage.py";
	public static String pathToScript = "";
	
	/**
	 * Uses matplotlib to create a .png image file displaying the given points, then
	 * returns the path to the saved file
	 * @param points
	 * @param xLabel
	 * @param yLabel
	 * @return
	 * @throws IOException 
	 */
	public static void generateFigure(Pipeline pipeline, List<Point2D> points, String xLabel, String yLabel, String outputPath) throws IOException {
		String pythonScriptsDir = (String) pipeline.getProperty(Pipeline.PYTHON_SCRIPTS_DIR);
		if (pythonScriptsDir != null) {
			pathToScript = pythonScriptsDir;
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tmpDataPath)));
		String lineSep= System.getProperty("line.separator");
		writer.write(xLabel + "\t" + yLabel + lineSep);
		for(Point2D p : points) {
			writer.write(p.getX() + "\t" + p.getY() + lineSep);
		}
		writer.close();
		
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		
		String command = "python " + pathToScript + lineFigScript + " " + tmpDataPath + " " + outputPath;
		logger.info("Executing figure generation command : " + command);
		
		int exitVal = executeCommand(command);
		if (exitVal != 0) {
			System.err.println("There may have been an error!");
		}
	}

	public static void generateFigure(Pipeline pipeline, Histogram hist, String seriesLabel, String xLabel, String yLabel, String outputPath) throws IOException {
		List<Histogram> histList = new ArrayList<Histogram>();
		histList.add(hist);
		List<String> label = new ArrayList<String>();
		label.add(seriesLabel);
		generateFigure(pipeline, histList, label, xLabel, yLabel, outputPath);
	}
	
	
	public static void generateFigure(Pipeline pipeline, List<Histogram> histList, List<String> seriesLabels, String xLabel, String yLabel, String outputPath) throws IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String pythonScriptsDir = (String)  pipeline.getProperty(Pipeline.PYTHON_SCRIPTS_DIR);
		if (pythonScriptsDir != null) {
			pathToScript = pythonScriptsDir;
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(tmpDataPath)));
		String lineSep= System.getProperty("line.separator");
		
		int index = 0;
		for(Histogram hist : histList) {
			
			if (index == 0) {
				writer.write("#" + seriesLabels.get(index) + "\t" + xLabel + "\t" + yLabel + lineSep);
			}
			else {
				writer.write("#" + seriesLabels.get(index) + lineSep);
			}

			for(int i=0; hist != null && i<hist.getBinCount(); i++) {
				double x = hist.getMin() + i*hist.getBinWidth();
				writer.write(x + "\t" + hist.getCount(i) + lineSep);
			}
			index++;
		}
		writer.close();
		
		
		String command = "python " + pathToScript + lineFigScript + " " + tmpDataPath + " " + outputPath;
		logger.info("Executing figure generation command : " + command);
		int exitVal = executeCommand(command);
		if (exitVal != 0) {
			logger.warning("Error generating figure (nonzero exit value), output path is : " + outputPath);
		}
	}

	
	public static void generateHistoImage(Pipeline pipeline, Histogram[] histos, String xLabel, String yLabel, String outputPath) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(".histodata.csv")));
		String lineSep= System.getProperty("line.separator");
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		String pythonScriptsDir = (String) pipeline.getProperty(Pipeline.PYTHON_SCRIPTS_DIR);
		if (pythonScriptsDir != null) {
			pathToScript = pythonScriptsDir;
		}
		
		for(int i=histos[0].getBinCount()-1; i>=0; i--) {
			for(int j=0; j<histos.length; j++) {
				writer.write(histos[j].getFreq(i) + ",");
			}
			writer.write(lineSep);
		}

		writer.close();
		String command = "python " + pathToScript + imageScript + " .histodata.csv " + outputPath;
		logger.info("Executing image generation command : " + command);
		int exitVal = executeCommand(command);
		if (exitVal != 0) {
			logger.warning("Error generating figure (nonzero exit value), output path is : " + outputPath);
		}
		
	}
	
	/**
	 * Execute the given system command in its own process, and wait until the process has completed
	 * to return. If the exit value of the process is not zero, an OperationFailedException in thrown
	 * @param command
	 * @throws OperationFailedException
	 */
	protected static int executeCommand(String command) {
		Runtime r = Runtime.getRuntime();
		Process p;

		try {
			p = r.exec(command);
			int exitVal = p.waitFor();
			return exitVal;
		} catch (IOException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	
//	public static void main(String[] args) {
//		List<Point2D> points = new ArrayList<Point2D>();
//		
//		points.add(new Point2D.Double(0, 1.0));
//		points.add(new Point2D.Double(1, 1.0));
//		points.add(new Point2D.Double(2, 2.0));
//		points.add(new Point2D.Double(3, 1.0));
//		points.add(new Point2D.Double(4, 5.0));
//		points.add(new Point2D.Double(4.1, 1.0));
//		points.add(new Point2D.Double(4.321, 7.0));
//		
//		try {
//			CreateFigure.generateFigure(points, "X data", "y data", "newlymadefigure.png");
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
}
