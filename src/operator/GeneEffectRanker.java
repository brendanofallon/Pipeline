package operator;

import gene.Gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import operator.gene.DBNSFPGeneRanker;
import operator.gene.GOTermRanker;
import operator.gene.GeneSummaryRanker;
import operator.gene.PubmedRanker;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import util.Jackknifeable;
import buffer.GeneList;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A new and much cleaner implementation of the gene-effect-relevance-ranking procedure
 * The basic idea is that we have a list of genes with annotations and a list of variants
 * with annotations, and we examine each independently ...
 * @author brendan
 *
 */
public class GeneEffectRanker extends Operator {

	int jackknifeVarCount = 100; //Only jackknife the top # of variants so as to save time
	int jackknifeReps = 0; //Number of jackknife replicates to perform
	
	GeneList genes = null;
	VariantPool vars = null;
	
	GeneSummaryRanker summaryRanker = null;
	DBNSFPGeneRanker dbnsfpRanker = null;
	PubmedRanker pubmedRanker = null;
	GOTermRanker goTermRanker = null;
	
	Map<String, Integer> searchTerms = null;
	Map<String, Integer> goTerms = null;
	List<String> interactionGenes = null;
	
	
	@Override
	public void performOperation() throws OperationFailedException {

		List<VariantRec> topHits = new ArrayList<VariantRec>();

		//Ensure each variant is associated with a gene
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Gene g = var.getGene();
				if (g == null && var.getAnnotation(VariantRec.GENE_NAME) != null) {
					g = genes.getGeneByName( var.getAnnotation(VariantRec.GENE_NAME));
					
					if (g != null)
						var.setGene(g);
				}
			}
		}
		
		int skipped = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				
				double effectProd = computeScore(var);
				var.addProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT, effectProd);
				
				
				if (effectProd > 0) {
					topHits.add(var);
					Collections.sort(topHits, new ScoreComparator());
				}
				while(topHits.size() > jackknifeVarCount) {
					topHits.remove( topHits.size()-1);
				}
				
				

			}
		}
		
//		for(VariantRec hit : topHits) {
//			System.out.println("Gene : " + hit.getAnnotation(VariantRec.GENE_NAME) + " var: "+ hit.getAnnotation(VariantRec.PDOT) + " score : " + hit.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT) +
//					" pubmed:" + hit.getGene().getProperty(Gene.PUBMED_SCORE)
//					+ " dbNSFP:" + hit.getGene().getProperty(Gene.DBNSFPGENE_SCORE) + 
//					" summary:" + hit.getGene().getProperty(Gene.SUMMARY_SCORE) + 
//					" goterms:" + hit.getGene().getProperty(Gene.GO_SCORE));
//		}
		
		System.out.println("Skipped " + skipped + " of " + vars.size() + " total variants");
		
//		if (jackknifeReps > 0) {
//			performJackknife(topHits);
//		}
		
		
		
	}

	
//	private void performJackknife(List<VariantRec> topHits) {
//		
//		//Some initialization...
//		searchTerms = summaryRanker.getRankingMap();
//		SearchTermJackknife searchTermJK = new SearchTermJackknife();
//		
//		//GOterm jackknife
//		//gene jackknife
//		
//		for(int i=0; i<jackknifeReps; i++) {
//			System.out.println("Computing JK rep #" + i);
//			jackknifeAndCompute(searchTermJK, topHits);
//		}
//		
//		
//		//Summarize jk scores...
//		System.out.println("Jackknife ranking results");
//		for(VariantRec var : topHits) {
//			System.out.println(var.toBasicString() + "\n" + var.getAnnotation(VariantRec.JACKKNIFE_RANKS));
//		}
//		
//	}


//	protected void jackknifeAndCompute(Jackknifeable jk, List<VariantRec> vars) {
//		
//		int itemToRemove = (int)Math.floor(Math.random() * jk.getRemoveableElementCount());
//		jk.removeElement(itemToRemove);
//		
//		//Now compute scores for all variants
//		for(VariantRec var : vars) {
//			
//			// Tell all rankers to re-rank....
//			//var.addProperty(VariantRec.JACKKNIFE_SCORE, score);				
//		}
//		
//		//Sort by newly computed jk score
//		Collections.sort(vars, new JKScoreComparator());
//		for(int i=0; i<vars.size(); i++) {
//			String jkRanks = vars.get(i).getAnnotation(VariantRec.JACKKNIFE_RANKS);
//			if (jkRanks == null)
//				jkRanks = "" + i;
//			else 
//				jkRanks = jkRanks + "," + i;
//			vars.get(i).addAnnotation(VariantRec.JACKKNIFE_RANKS, jkRanks);
//		}
//		
//		
//		jk.restore();
//	}
	
	/**
	 * Compute the combined ranking score for the given variant. 
	 * @param var
	 * @return
	 */
	protected double computeScore(VariantRec var) {
		Double varScore = var.getProperty(VariantRec.EFFECT_PREDICTION2);
		if (varScore == null || varScore == 0) {
			return 0;
		}
		
		Gene g = var.getGene();
	
		if (g==null)
			return 0.0;
		
		Double summaryScore = g.getProperty(Gene.SUMMARY_SCORE);
		Double abstractsScore = g.getProperty(Gene.PUBMED_SCORE);
		Double goScore = g.getProperty(Gene.GO_SCORE);
		Double dbNSFPScore = g.getProperty(Gene.DBNSFPGENE_SCORE);
		Double interactionScore = g.getProperty(Gene.INTERACTION_SCORE);
		

		if (summaryScore == null)
			summaryScore = 0.0;
		if (abstractsScore == null)
			abstractsScore = 0.0;
		if (goScore == null)
			goScore = 0.0;
		if (dbNSFPScore == null)
			dbNSFPScore = 0.0;
		if (interactionScore == null)
			interactionScore = 0.0;
		
		Double relevanceScore = (goScore + summaryScore + abstractsScore/2.0 + dbNSFPScore + 10*interactionScore);
		g.addProperty(Gene.GENE_RELEVANCE, relevanceScore);
		
		Double effectProd = varScore * relevanceScore;
		return effectProd;
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof VariantPool) {
					vars = (VariantPool)obj;
				}
				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
				
				if (obj instanceof GeneSummaryRanker) {
					summaryRanker = (GeneSummaryRanker)obj;
				}
				if (obj instanceof DBNSFPGeneRanker) {
					dbnsfpRanker = (DBNSFPGeneRanker)obj;
				}
				if (obj instanceof GOTermRanker) {
					goTermRanker = (GOTermRanker)obj;
				}
				if (obj instanceof PubmedRanker) {
					pubmedRanker = (PubmedRanker)obj;
				}
			}
		}
		
		if (genes == null)
			throw new IllegalArgumentException("No gene list provided to GeneEffectRanker");

		if (vars == null)
			throw new IllegalArgumentException("No variant provided to GeneEffectRanker");

		
	}
	
	class SearchTermJackknife implements Jackknifeable {

		List<String> orderedKeys;
		Map<String, Integer> modMap; //Modified map
		String removedTerm = null;
		Integer removedScore = null;
		
		public SearchTermJackknife() {
			orderedKeys = new ArrayList<String>();
			for(String key : searchTerms.keySet()) {
				orderedKeys.add(key);
			}
		}
		
		@Override
		public void restore() {
		
			summaryRanker.setRankingMap(searchTerms);
			dbnsfpRanker.setRankingMap(searchTerms);
			//pubmedRanker.setRankingMap(searchTerms)
		}

		@Override
		public void removeElement(int which) {
			removedTerm = orderedKeys.get(which);
			removedScore = searchTerms.get(removedTerm);
			System.out.println("Removing term : " + removedTerm);
			modMap = new HashMap<String, Integer>();
			for(String key : searchTerms.keySet()) {
				if (!key.equals(removedTerm)) {
					modMap.put(key, searchTerms.get(key));
				}
			}
			
			summaryRanker.setRankingMap(modMap);
			dbnsfpRanker.setRankingMap(modMap);
			//pubmedranker...
			
		}

		@Override
		public int getRemoveableElementCount() {
			return searchTerms.size();
		}
		
	}
	
	class ScoreComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec var0, VariantRec var1) {
			Double score0 = var0.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			Double score1 = var1.getProperty(VariantRec.EFFECT_RELEVANCE_PRODUCT);
			
			if (score0 != null && score1 != null) {
				if (score0 == score1)
					return 0;
				return score0 < score1 ? 1 : -1;
			}
			
			return 0;
		}
		
	}
	
	class JKScoreComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec var0, VariantRec var1) {
			Double score0 = var0.getProperty(VariantRec.JACKKNIFE_SCORE);
			Double score1 = var1.getProperty(VariantRec.JACKKNIFE_SCORE);
			
			if (score0 != null && score1 != null) {
				if (score0 == score1)
					return 0;
				return score0 < score1 ? 1 : -1;
			}
			
			return 0;
		}
		
	}

}
