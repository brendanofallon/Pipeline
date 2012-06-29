package gui.variantTable;

import gui.ErrorWindow;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingWorker;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import buffer.CSVFile;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;
import pipeline.PipelineListener;
import rankingService.AnalysisSettings;
import rankingService.templates.TemplateTransformer;

public class AnalysisRunner {

	public static final int defaultGraphSize = 50;
	
	public static final String INPUT_FILE = "INPUTFILE";
	public static final String PREFIX = "PREFIX";
	public static final String GO_TERMS_FILE = "GOTERMSFILE";
	public static final String KEY_TERMS = "KEYTERMS";
	public static final String SOURCE_GENES = "SOURCEGENES";
	public static final String GRAPH_SIZE = "GRAPHSIZE";
	public static final String CAPTURE_BED = "CAPTUREBED";
	
	AnalysisSettings settings;
	InputStream templateStream;
	Document inputDoc = null;
	Pipeline pipeline = null;
	private List<DoneListener> listeners = new ArrayList<DoneListener>();
	
	public AnalysisRunner(AnalysisSettings settings, InputStream templateStream) {
		this.settings = settings;
		this.templateStream = templateStream;
	}
	
	public PipelineRunner getPipelineRunner() {
		if (inputDoc == null)
			throw new IllegalStateException("Runner not initialized");
		
		pipeline = new Pipeline(inputDoc, settings.pathToPipelineProperties);
		
		pipeline.setProperty(Pipeline.PROJECT_HOME, settings.rootPath + System.getProperty("file.separator"));
		
		PipelineRunner runner = null;
		try {
			System.out.println("Initializing pipeline with home :" + pipeline.getProjectHome());
			pipeline.initializePipeline();
			
		} catch (PipelineDocException e) {
			e.printStackTrace();
			ErrorWindow.showErrorWindow(e, "Could not create document");
		} catch (ObjectCreationException e) {
			e.printStackTrace();
			ErrorWindow.showErrorWindow(e, "Could not create object " + e.getOffendingElement().getNodeName());
		}
		
		runner = new PipelineRunner(pipeline);
		return runner;
	}
	
	/**
	 * Generates an XML DOM document that can be used as input to pipeline 
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public void initialize() throws IOException, ParserConfigurationException, SAXException {
		Map<String, String> subs = createSubsFromSettings(); 
		inputDoc = TemplateTransformer.transformTemplate(templateStream, subs);
	}
	
	/**
	 * Obtain a CSV file referencing the list of ranked variants created by the analysis
	 * @return
	 */
	public CSVFile getFinalVariants() {
		String rootDir = pipeline.getProjectHome();
		if (! rootDir.endsWith(System.getProperty("file.separator")))
			rootDir = rootDir + System.getProperty("file.separator");
		String path = rootDir + settings.prefix + ".ranking.analysis.csv";
		
		System.out.println("path to ranked variants : " + path);
		CSVFile vars = new CSVFile(new File(path));
		return vars;
	}

	private Map<String, String> createSubsFromSettings() throws IOException {
		Map<String, String> subs = new HashMap<String, String>();
		
		subs.put(INPUT_FILE, settings.pathToVCF);
		subs.put(PREFIX, settings.prefix);
		
		File goFile = createGOTermsFile(settings.rootPath, settings.goTerms);
		File keyFile = createKeyTermsFile(settings.rootPath, settings.goTerms);
		String genes = createGenesList(settings.genes);
		
		subs.put(GO_TERMS_FILE, goFile.getAbsolutePath());
		subs.put(KEY_TERMS, keyFile.getAbsolutePath());
		subs.put(SOURCE_GENES, genes);
		subs.put(GRAPH_SIZE, "" + settings.graphSize);
		//subs.put(CAPTURE_BED, defaultBEDFile);
		
		return subs;
	}

	private String createGenesList(List<String> genes) {
		StringBuilder strB = new StringBuilder();
		for(String gene : genes) {
			strB.append(gene + ",");
		}
		
		//Trim off last comma
		if (strB.length() > 1)
			strB.replace(strB.length()-1, strB.length(), "");
		
		return strB.toString();
	}

	private File createKeyTermsFile(String rootPath, Map<String, Integer> goTerms) throws IOException {
		File destination = new File(rootPath + System.getProperty("file.separator") + "keyterms.txt");
		destination.createNewFile();
		
		writeToFile(destination, goTerms);	
		return destination;
	}

	private File createGOTermsFile(String rootPath, Map<String, Integer> goTerms) throws IOException {
		File destination = new File(rootPath + System.getProperty("file.separator") + "goterms.txt");
		destination.createNewFile();
		
		writeToFile(destination, goTerms);	
		return destination;
	}
	
	private void writeToFile(File dest, Map<String, Integer> info) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String lineSep = System.getProperty("line.separator");
		for(String key : info.keySet()) {
			writer.write(key + "\t" + info.get(key) + lineSep);
		}
		writer.close();
		
	}
	
	/**
	 * Add a listener to be notified of when the pipeline completes
	 * @param newListener
	 */
	public void addListener(DoneListener newListener) {
		this.listeners.add(newListener);
	}
	
	public void firePipelineDone() {
		for(DoneListener dl : listeners) {
			dl.done();
		}
	}
	
	class PipelineRunner extends SwingWorker {

		final Pipeline pipeline;
		
		public PipelineRunner(Pipeline pipeline) {
			this.pipeline = pipeline;
		}
		
		public Pipeline getPipeline() {
			return pipeline;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			try {
				pipeline.execute();
			}
			catch (OperationFailedException ef) {
				ErrorWindow.showErrorWindow(ef);
			}
			return pipeline;
		}

		@Override
		protected void done() {
			firePipelineDone();
		}
		
		
	}
	
}
