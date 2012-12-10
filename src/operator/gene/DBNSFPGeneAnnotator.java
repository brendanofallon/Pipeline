package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.variant.DBNSFPAnnotator;
import operator.variant.DBNSFPGene;
import operator.variant.DBNSFPGene.GeneInfo;
import pipeline.Pipeline;

/**
 * Provides a handful of  gene annotations obtained from the dbNSFP database, including
 * disease description, OMIM numbers, functional description, and tissue expression info
 * @author brendan
 *
 */
public class DBNSFPGeneAnnotator extends AbstractGeneAnnotator {

	
	DBNSFPGene db;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		// TODO Auto-generated method stub
		if (db == null) {
			
			String pathToDBNSFP = this.getPipelineProperty(DBNSFPAnnotator.DBNSFP_PATH);
			Logger.getLogger(Pipeline.primaryLoggerName).info("dbNSFP-gene reader using directory : " + pathToDBNSFP);
			try {
				db = DBNSFPGene.getDB(new File(pathToDBNSFP + "/dbNSFP2.0b4_gene"));
			} catch (IOException e) {
				throw new OperationFailedException("Could not initialize dbNSFP gene file", this);
			}
		}
		
		GeneInfo info = db.getInfoForGene( g.getName() );
		if (info == null)
			return;
		
		g.addAnnotation(Gene.DBNSFP_DISEASEDESC, info.diseaseDesc);
		g.addAnnotation(Gene.DBNSFP_FUNCTIONDESC, info.functionDesc);
		g.addAnnotation(Gene.DBNSFP_MIMDISEASE, info.mimDisease);
		g.addAnnotation(Gene.EXPRESSION, info.expression);
	}

}
