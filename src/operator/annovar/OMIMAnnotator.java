package operator.annovar;

import java.util.List;

import buffer.variant.OMIMVariants;
import buffer.variant.VariantRec;
import operator.OperationFailedException;

/**
 * This class searches through all variants given and adds OMIM id numbers to all matching vars
 * This uses an OMIMVariants variant pool to store / find all known omim variants
 * @author brendan
 *
 */
public class OMIMAnnotator extends Annotator {
	
	OMIMVariants omimVars = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);

		if (omimVars == null) 
			omimVars = new OMIMVariants();
		
		
		for(String contig : variants.getContigs()) {
			List<VariantRec> vars = variants.getVariantsForContig(contig);
			for(VariantRec rec : vars) {
				VariantRec omimVar = omimVars.findRecordNoWarn(contig, rec.getStart());
				if (omimVar != null) {
					rec.addAnnotation(VariantRec.OMIM_ID, omimVar.getAnnotation(VariantRec.OMIM_ID));
				}
			}
			
		}
	}
	

}
