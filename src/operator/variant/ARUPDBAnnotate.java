package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import pipeline.Pipeline;
import buffer.variant.VariantRec;


/**
 * Adds the ARUP_FREQ annotation to a variant. Uses an ARUPDB object to provide db info. 
 * @author brendan
 *
 */
public class ARUPDBAnnotate extends Annotator {

	private ARUPDB arupDB = null;
	
	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		if (arupDB == null) {
			String arupDBFile = this.getPipelineProperty("arup.db.path");
			if (arupDBFile == null) {
				throw new OperationFailedException("No path to ARUP db specified in pipeline properties", this);
			}
			else {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Looking up ARUP frequency db info in : " + arupDBFile);
			}
			try {
				arupDB = new ARUPDB(new File(arupDBFile));
			} catch (IOException e) {
				e.printStackTrace();
				throw new OperationFailedException("Error opening ARUP db file: " + e.getMessage(), this);
			}
		}
		
		String dbInfo;
		try {
			dbInfo = arupDB.getInfoForPostion(var.getContig(), var.getStart());
			if (dbInfo != null) {
				var.addAnnotation(VariantRec.ARUP_FREQ, dbInfo);
				//parse total
				String totStr = dbInfo.split(" ")[0];
				try {
					Integer tot = Integer.parseInt(totStr);
					var.addProperty(VariantRec.ARUP_TOT, new Double(tot));
				}
				catch (NumberFormatException nfe) {
					//don't sweat it
				}
			}
			else {
				var.addProperty(VariantRec.ARUP_TOT, 0.0);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
