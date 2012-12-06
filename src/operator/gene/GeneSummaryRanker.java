package operator.gene;

import gene.Gene;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import ncbi.CachedGeneSummaryDB;
import operator.OperationFailedException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.GeneList;
import buffer.TextBuffer;

/**
 * An annotator that computes a score for variants based on how many hits a particular term has
 * in the gene summary associated with the gene that this variant is in. No score is computed
 * for variants that are not in a gene. Furthermore, since gene summaries are only available
 * for some subset of genes, many genes will not be scored with the algorithm. 
 * @author brendan
 *
 */
public class GeneSummaryRanker extends AbstractGeneAnnotator {

	public static final String GENE_INFO_PATH = "gene.info.path";
	public static final String NO_DOWNLOADS = "no.downloads";
	protected TextBuffer termsFile = null;
	protected CachedGeneSummaryDB summaryDB = null;
	protected Map<String, Integer> rankingMap;
	protected int examined = 0;
	protected int scored = 0;
	
	public void performOperation() throws OperationFailedException {
		super.performOperation();
		 
		//Force a final write to the cache when we're done. 
		if (summaryDB != null) {
			try {
				summaryDB.writeMapToFile();
			} catch (IOException e) {
				//Not a huge deal really, we just wont have cached gene summaries for next time. 
				e.printStackTrace();
			}
		}
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(this.getObjectLabel() + " found " + scored + " hits in " + examined + " total genes");
	}
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return true;
	}
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (summaryDB == null) {
			initializeDB();
		}
		
		examined++;
		//this.getPipelineOwner().fireMessage("Examining summary for gene : " + g.getName());
    	String summary = summaryDB.getSummaryForGene( g.getName() );
    	if (summary == null)
    		return;
    	
    	double score = scoreSummary(summary.toLowerCase());
    	if (score > 0)
    		scored++;
    	g.addProperty(Gene.SUMMARY_SCORE, score);
	}
	
	/**
	 * Computes a score for the given gene summary by seeing how many key terms, defined in a text
	 * file, are present in the summary
	 * @param summary
	 * @return
	 */
	public double scoreSummary(String summary) {
		double score = 0;
		for(String term : rankingMap.keySet()) {
			if (summary.contains(term)) {
				score += rankingMap.get(term);
			}
		}
		return score;
	}

	protected void buildRankingMap() throws IOException {
		rankingMap = new HashMap<String, Integer>();
		
		BufferedReader reader = new BufferedReader(new FileReader(termsFile.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length<2) {
				System.out.println("Cannot parse summary ranking term for this line: " + line);
				line = reader.readLine();
				continue;
			}
			Integer score = Integer.parseInt(toks[1].trim());
			String key = toks[0].trim().toLowerCase();
			rankingMap.put(key, score);
			line = reader.readLine();
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info("Read " + rankingMap.size() + " terms in from search terms in file: " + termsFile.getAbsolutePath());
		reader.close();
	}
	
	
	private void initializeDB() {
		try {
			buildRankingMap();
			
			String pathToGeneInfo = this.getAttribute(GENE_INFO_PATH);
			if (pathToGeneInfo != null) {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is using custom location for cache: " + pathToGeneInfo);
				summaryDB = new CachedGeneSummaryDB(pathToGeneInfo);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is using default location for local cache");
				summaryDB = new CachedGeneSummaryDB();
			}
			
			String dlAttr = this.getAttribute(NO_DOWNLOADS);
			if (dlAttr != null) {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Gene summary is setting prohibit downloads to : " + dlAttr);
				Boolean prohibitDLs = Boolean.parseBoolean(dlAttr);
				summaryDB.setProhibitNewDownloads(prohibitDLs);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof TextBuffer) {
					termsFile = (TextBuffer)obj;
				}
				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
			}
		}
		
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to GeneSummaryRanker");
		
		if (termsFile == null)
			throw new IllegalArgumentException("No term file provided to GeneSummaryRanker");
		
	}

	public Map<String, Integer> getRankingMap() {
		return rankingMap;
	}
	
	public void setRankingMap(Map<String, Integer> newMap) {
		this.rankingMap = newMap;
	}
	
	

}
