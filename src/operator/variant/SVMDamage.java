package operator.variant;

import java.io.File;
import java.text.DecimalFormat;

import operator.OperationFailedException;
import operator.annovar.Annotator;
import buffer.variant.VariantRec;

public class SVMDamage extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) throws OperationFailedException {
		// TODO Auto-generated method stub
		
	}
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		DecimalFormat formatter = new DecimalFormat("#0.00");
		int tot = variants.size();
		
		
		int varsAnnotated = 0;

		//First write all data to a (tmp) file
		File data = new File(this.getProjectHome() + "/.svmdata" + ("" + (10000.0*Math.random())).substring(0, 4) + ".csv");
		//data.deleteOnExit();
		
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				String dataLine = getDataLine(rec);
				
				
				
				
				
				//Now use libsvm to 'predict' the class of each variant
				
				
				//Now read data back in and parse the predicted value
				
				varsAnnotated++;
				
			}
		}
			
	}

	private String getDataLine(VariantRec rec) {
		StringBuffer dataLine = new StringBuffer("1\t");
		
		
		return dataLine.toString();
	}

	public static final double SIFT_MIN = 0.0;
	public static final double SIFT_MAX = 1.0;
	public static final double PP_MIN = 0.0;
	public static final double PP_MAX = 1.0;
	public static final double MT_MIN = 0.0;
	public static final double MT_MAX = 1.0;
	public static final double GERP_MIN = 0.0;
	public static final double GERP_MAX = 0.0;
	public static final double SIFT_MIN = 0.0;
	public static final double SIFT_MAX = 0.0;
	
}
