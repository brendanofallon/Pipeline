package rankingService;

import gui.ErrorWindow;
import gui.PipelineWindow;
import gui.variantTable.templates.TemplateTransformer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;



public class RankingService {

	public static final int defaultGraphSize = 50;
	
	public static final String INPUT_FILE = "INPUTFILE";
	public static final String PREFIX = "PREFIX";
	public static final String GO_TERMS_FILE = "GOTERMSFILE";
	public static final String KEY_TERMS = "KEYTERMS";
	public static final String SOURCE_GENES = "SOURCEGENES";
	public static final String GRAPH_SIZE = "GRAPHSIZE";
	public static final String CAPTURE_BED = "CAPTUREBED";
	
	//Path to analysis template that stores the steps of the analysis
	public static final String ANALYSIS_TEMPLATE_PATH = "variantTable/templates/analysis_template.xml"; 
	

	
	public static RankingServiceJob submitRankingJob(AnalysisSettings settings) throws IOException, ParserConfigurationException, SAXException {
		return submitRankingJob(settings, null);
	}

	/**
	 * Create a new RankingServiceJob that can conduct a ranking analysis and produce a RankingResults
	 * object to store the results. Note that the job is ready to run, but not running yet. Call
	 * job.execute() to actually begin the run. 
	 * 
	 * @param settings
	 * @return
	 * @throws IOException
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 */
	public static RankingServiceJob submitRankingJob(AnalysisSettings settings, ClassLoader loader) throws IOException, ParserConfigurationException, SAXException {
		Map<String, String> substitutions = createSubsFromSettings(settings);
		
		Document inputDoc = TemplateTransformer.transformTemplate( getTemplateInputStream(), substitutions);
		
		Pipeline pipeline = new Pipeline(inputDoc, settings.pathToPipelineProperties);
		if (loader != null)
			pipeline.setClassLoader(loader);
		
		pipeline.setProperty(Pipeline.PROJECT_HOME, settings.rootPath + System.getProperty("file.separator"));

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

		RankingServiceJob job = new RankingServiceJob(settings, pipeline);
		return job;
	} 
	
	/**
	 * Obtain the analysis template as in InputStream
	 * @return
	 */
	private static InputStream getTemplateInputStream() {
		return getResourceInputStream(ANALYSIS_TEMPLATE_PATH);
	}

	/**
	 * Convert items in the settings file to Strings that can be easily substituted into
	 * a template file to produce a working analysis input file
	 * @param settings
	 * @return
	 * @throws IOException
	 */
	private static Map<String, String> createSubsFromSettings(AnalysisSettings settings) throws IOException {
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
	
	
	private static String createGenesList(List<String> genes) {
		StringBuilder strB = new StringBuilder();
		for(String gene : genes) {
			strB.append(gene + ",");
		}
		
		//Trim off last comma
		if (strB.length() > 1)
			strB.replace(strB.length()-1, strB.length(), "");
		
		return strB.toString();
	}

	private static File createKeyTermsFile(String rootPath, Map<String, Integer> goTerms) throws IOException {
		File destination = new File(rootPath + System.getProperty("file.separator") + "keyterms.txt");
		destination.createNewFile();
		
		writeToFile(destination, goTerms);	
		return destination;
	}

	private static File createGOTermsFile(String rootPath, Map<String, Integer> goTerms) throws IOException {
		File destination = new File(rootPath + System.getProperty("file.separator") + "goterms.txt");
		destination.createNewFile();
		
		writeToFile(destination, goTerms);	
		return destination;
	}
	
	private static void writeToFile(File dest, Map<String, Integer> info) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String lineSep = System.getProperty("line.separator");
		for(String key : info.keySet()) {
			writer.write(key + "\t" + info.get(key) + lineSep);
		}
		writer.close();
	}
	
	
	/** Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static InputStream getResourceInputStream(String url) {
		InputStream resource = null;
		try {
			URL resourceURL = PipelineWindow.class.getResource(url);
			resource = resourceURL.openStream();

		}
		catch (Exception ex) {
			System.out.println("Error loading file from resource " + url + "\n" + ex);
		}
		return resource;
	}
}
