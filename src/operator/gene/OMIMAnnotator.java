package operator.gene;

import gene.Gene;

import java.io.File;
import java.io.IOException;
import java.util.List;

import operator.OperationFailedException;
import disease.OMIMDB;
import disease.OMIMDB.OMIMEntry;

/**
 * This class adds OMIM - based information to each gene
 * @author brendan
 *
 */
public class OMIMAnnotator extends AbstractGeneAnnotator {

	public static final String OMIM_DIR = "omim.dir";
	OMIMDB omim = null;
	
	@Override
	public void annotateGene(Gene g) throws OperationFailedException {
		if (omim == null) {
			String pathToOMIM = this.getAttribute(OMIM_DIR);
			if (pathToOMIM == null) {
				pathToOMIM = this.getPipelineProperty(OMIM_DIR);
			}
			if (pathToOMIM == null) {
				throw new OperationFailedException("No path to OMIM specified, use omim.dir", this);
			}
			
			try {
				omim = new OMIMDB(new File(pathToOMIM));
			} catch (IOException e) {
				throw new OperationFailedException("Error opening omim directory", this);
			}
		}
		

		List<OMIMEntry> list = omim.getEntriesForGene(g);
		if (list != null) {
			int count = 0;
			for (OMIMEntry entry : list) {
				if (count == 0) {
					g.addAnnotation(Gene.OMIM_DISEASES, entry.diseaseName);
					g.addAnnotation(Gene.OMIM_NUMBERS, entry.diseaseID);
				}
				else {
					g.addAnnotation(Gene.OMIM_DISEASES, g.getAnnotation(Gene.OMIM_DISEASES) + ", " + entry.diseaseName);
					g.addAnnotation(Gene.OMIM_NUMBERS, g.getAnnotation(Gene.OMIM_NUMBERS) + ", " + entry.diseaseID);
				}
			}
		}
	}
	
	

}
