package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import operator.variant.DBNSFPGene.GeneInfo;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Provides annotations from the DBNSFP-GENE database
 * 
 * 
 * @author brendan
 *
 */
public class DBNSFPGeneAnnotator extends Annotator {

	DBNSFPGene db;

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (db == null) {
			
			String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
			try {
				db = DBNSFPGene.getDB(new File(pathToDBNSFP + "/dbNSFP2.0b2_gene"));
			} catch (IOException e) {
				throw new OperationFailedException("Could not initialize dbNSFP gene file", this);
			}
		}
		
		
		String geneName = var.getAnnotation(VariantRec.GENE_NAME);
		if (geneName == null) {
			return;
		}
		
		GeneInfo info = db.getInfoForGene(geneName);
		if (info == null)
			return;
		
		var.addAnnotation(VariantRec.DBNSFP_DISEASEDESC, info.diseaseDesc);
		var.addAnnotation(VariantRec.DBNSFP_FUNCTIONDESC, info.functionDesc);
		var.addAnnotation(VariantRec.DBNSFP_MIMDISEASE, info.mimDisease);
		
	}
}
