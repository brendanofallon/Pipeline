package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import ncbi.CachedPubmedAbstractDB;
import ncbi.GeneInfoDB;
import ncbi.GenePubMedDB;
import ncbi.PubMedRecord;
import buffer.TextBuffer;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.PipelineObject;

/**
 * Annotates variants with the PUBMED_SCORE property, which is computed by looking at pubmed abstracts
 * stored in a CachedPubmedAbstractDB 
 * 
 * @author brendan
 *
 */
public class PubmedRanker extends Annotator {

	//String constants for some XML properties we may use
	public static final String DISABLE_CACHE_WRITES = "disable.cache.writes";
	public static final String GENE_INFO_PATH = "gene.info.path";
	public static final String PUBMED_PATH = "pubmed.path";

	CachedPubmedAbstractDB abstractDB = null; //DB for abstracts that we query
	GenePubMedDB geneToPubmed = null; //Look up pub med id's associated with a certain gene
	GeneInfoDB geneInfo = null; //Look up gene IDs for gene symbols
	TextBuffer termsFile = null; //Stores key terms we use to score pub med records (abstracts)
	Map<String, Integer> rankingMap = null;
	private boolean disableCacheWrites = false; //Disable writing of new variants to cache, useful if we have multiple instances running
	
	
	/**
	 * If true, we write some progress indicators to system.out
	 * @return
	 */
	protected boolean displayProgress() {
		return true;
	}
	
	public void performOperation() throws OperationFailedException {
		
		String disableStr = properties.get(DISABLE_CACHE_WRITES);
		if (disableStr != null) {
			Boolean disable = Boolean.parseBoolean(disableStr);
			disableCacheWrites = disable;
		}
		
		super.performOperation();
		
		if (abstractDB != null) {
			try {
				abstractDB.writeMapToFile();
			} catch (IOException e) {
				//Probably not a big deal, some abstracts won't get cached
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void annotateVariant(VariantRec var) {
		if (abstractDB == null) {
			try {
				String pathToPubmedDB = "/home/brendan/resources/gene2pubmed_human";
				String pubmedAttr = this.getAttribute(PUBMED_PATH);
				if (pubmedAttr != null) {
					pathToPubmedDB = pubmedAttr;
				}
				abstractDB = new CachedPubmedAbstractDB(pathToPubmedDB);
			} catch (IOException e) {
				throw new IllegalStateException("IO error reading cached abstract db : " + e.getMessage());
			}
		}
		
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				throw new IllegalStateException("IO error reading pubmed terms file : " + e.getMessage());

			}
		}
		
		if (geneInfo == null) {
			try {
				String geneInfoPath = "/home/brendan/resources/Homo_sapiens.gene_info";
				String geneInfoAttr = this.getAttribute(GENE_INFO_PATH);
				if (geneInfoAttr != null) {
					geneInfoPath = geneInfoAttr;
				}
				geneInfo = new GeneInfoDB(new File(geneInfoPath));
			} catch (IOException e) {
				throw new IllegalStateException("Error opening gene info file : " + e.getMessage());
			}
		}
		
	
		String geneName = var.getAnnotation(VariantRec.GENE_NAME);
		if (geneName == null || geneName.startsWith("HLA-") || geneName.startsWith("MUC")) {
			return;
		}
		
		if (geneToPubmed == null) {
			try {
				String pubmedPath = "/home/brendan/resources/gene2pubmed_human";
				String pubmedAttr = this.getAttribute(PUBMED_PATH);
				if (pubmedAttr != null) {
					pubmedPath = pubmedAttr;
				}
				geneToPubmed = new GenePubMedDB(new File(pubmedPath));
			} catch (IOException e) {
				throw new IllegalStateException("Error opening gene2pubmed file : " + e.getMessage());
			}
		}
		
		
		String idStr = geneInfo.idForSymbol(geneName);
		if (idStr == null) {
			if (geneName.length() < 8)
				System.err.println("Could not find gene id for symbol: " + geneName);
			return;
		}
		
		Integer geneID = Integer.parseInt( idStr );
		
		List<Integer> pubmedIDs = geneToPubmed.getPubMedIDsForGene(geneID);
		if (pubmedIDs == null) {
			//System.out.println("Found 0 ids for gene : " + geneName);
			return;
		}
		
		//Grab in batches of 500 if there are more than 500

		List<PubMedRecord> records = new ArrayList<PubMedRecord>(Math.min(pubmedIDs.size(), 512));
		int start = 0;
		int end = 500;
		while(start < pubmedIDs.size()) {
			//System.out.println("Fetching records from " + start + " to " + Math.min(pubmedIDs.size(), end) + " of " + pubmedIDs.size() + " total requested");
			List<PubMedRecord> subrec = abstractDB.getRecordForIDs(pubmedIDs.subList(start, Math.min(pubmedIDs.size(), end)), disableCacheWrites);
			records.addAll(subrec);
			start = end;
			end += 500;
		}
		

		this.getPipelineOwner().fireMessage("Examining " + records.size() + " abstracts for gene : " + geneName);
		
		//System.out.println("Found " + records.size() + " records for " + pubmedIDs.size() + " ids for for gene : " + geneName);
		//We take the *maximum* score found among all abstracts
		Double maxScore = 0.0;
		String maxHit = "-";
		for(PubMedRecord rec : records) {
			if (rec != null) {
				Double abstractScore = computeScore( rec );	
				if (abstractScore > maxScore) {
					maxScore = abstractScore;
					maxHit = rec.getTitle() + "," + rec.getCitation();
				}
			}
		}
		 
		var.addProperty(VariantRec.PUBMED_SCORE, maxScore);
		if (maxScore > 0) {
			var.addAnnotation(VariantRec.PUBMED_HIT, maxHit);
		}
	}

	/**
	 * Compute a score 
	 * @param rec
	 * @return
	 */
	private Double computeScore(PubMedRecord rec) {
		String title = rec.getTitle();
		String abs = rec.getAbstract();
		
		if (title != null)
			title = title.toLowerCase();
		if (abs != null)
			abs = abs.toLowerCase();
		
		double score = 0;
		for(String term : rankingMap.keySet()) {
			if (title != null && title.contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
			if (abs != null && abs.contains(term)) {
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
			if (toks.length != 2) {
				System.err.println("Warning : could not parse line for pubmed key terms : " + line);
				line = reader.readLine();
				continue;
			}
			Integer score = Integer.parseInt(toks[1].trim());
			rankingMap.put(toks[0].trim().toLowerCase(), score);
			line = reader.readLine();
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
