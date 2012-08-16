package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.variant.DBNSFPGene.GeneInfo;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Computes a ranking score for a variant (gene, really) by examaining the information
 * from the DBNSFP_
 * @author brendan
 *
 */
public class DBNSFPGeneRanker extends GeneSummaryRanker {

	private DBNSFPGene geneDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (geneDB == null) {
			String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
			try {
				geneDB = DBNSFPGene.getDB(new File(pathToDBNSFP + "/dbNSFP2.0b2_gene"));
			} catch (IOException e) {
				throw new OperationFailedException("Could not initialize dbNSFP gene file", this);
			}
		}
		
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				throw new OperationFailedException("Could not read ranking map", this);
			}
		}
		
		String gene = var.getAnnotation(VariantRec.GENE_NAME);
		if (gene == null)
			return;
		
		GeneInfo info = geneDB.getInfoForGene(gene);
		if (info == null)
			return;
		
    	double score = computeScore(info);
    	var.addProperty(VariantRec.DBNSFPGENE_SCORE, score);
	}

	
	public double computeScore(GeneInfo info) {
		double score = 0;
		
		for(String term : rankingMap.keySet()) {
			if (info.diseaseDesc != null && info.diseaseDesc.toLowerCase().contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
			if (info.functionDesc != null && info.functionDesc.toLowerCase().contains(term)) {
				score += 2.0*rankingMap.get(term);
			}
		}
		return score;
	}
}
