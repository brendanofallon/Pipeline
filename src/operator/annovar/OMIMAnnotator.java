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
	public void annotateVariant(VariantRec var) {
		if (omimVars == null) 
			omimVars = new OMIMVariants(this.getObjectHandler());
		
		VariantRec omimVar = omimVars.findRecordNoWarn(var.getContig(), var.getStart());
		if (omimVar != null) {
			var.addAnnotation(VariantRec.OMIM_ID, omimVar.getAnnotation(VariantRec.OMIM_ID));
		}
	}
	

}
