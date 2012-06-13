package operator.variant;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ncbi.CachedGeneSummaryDB;
import buffer.TextBuffer;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.PipelineObject;

/**
 * An annotator that computes a score for variants based on how many hits a particular term has
 * in the gene summary associated with the gene that this variant is in. No score is computed
 * for variants that are not in a gene. Furthermore, since gene summaries are only available
 * for some subset of genes, many genes will not be scored with the algorithm. 
 * @author brendan
 *
 */
public class GeneSummaryRanker extends Annotator {

	public static final String GENE_INFO_PATH = "gene.info.path";
	protected TextBuffer termsFile = null;
	protected CachedGeneSummaryDB summaryDB = null;
	protected Map<String, Integer> rankingMap;
	
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
	}
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return true;
	}
	
	@Override
	public void annotateVariant(VariantRec var) {
		if (summaryDB == null) {
			initializeDB();
		}
		
		String gene = var.getAnnotation(VariantRec.GENE_NAME);
		if (gene == null)
			return;
		
		this.getPipelineOwner().fireMessage("Examining summary for gene : " + gene);
    	String summary = summaryDB.getSummaryForGene( gene );
    	if (summary == null)
    		return;
    	
    	double score = scoreSummary(summary);
    	var.addProperty(VariantRec.SUMMARY_SCORE, score);
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

	private void buildRankingMap() throws IOException {
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
			Integer score = Integer.parseInt(toks[1]);
			rankingMap.put(toks[0], score);
			line = reader.readLine();
		}
	}
	
	
	private void initializeDB() {
		try {
			buildRankingMap();
			String pathToGeneInfo = this.getAttribute(GENE_INFO_PATH);
			if (pathToGeneInfo != null) {
				summaryDB = new CachedGeneSummaryDB(pathToGeneInfo);
			}
			else {
				summaryDB = new CachedGeneSummaryDB();
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
			}
		}
	}

}
