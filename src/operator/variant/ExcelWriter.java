package operator.variant;

import java.io.PrintStream;

import buffer.variant.VariantRec;

/**
 * Writes a variant pool in a nifty format designed to work well with microsoft excel
 * @author brendan
 *
 */
public class ExcelWriter extends VariantPoolWriter {

	String[] keys = new String[]{VariantRec.GENE_NAME,
								 VariantRec.DEPTH,
								 VariantRec.VARIANT_TYPE, 
								 VariantRec.EXON_FUNCTION, 
								 VariantRec.RSNUM, 
								 VariantRec.POP_FREQUENCY, 
								 VariantRec.OMIM_ID, 
								 VariantRec.CDOT,
								 VariantRec.PDOT,
								 VariantRec.SIFT_SCORE, 
								 VariantRec.POLYPHEN_SCORE, 
								 VariantRec.PHYLOP_SCORE, 
								 VariantRec.MT_SCORE,
								 VariantRec.GO_FUNCTION,
								 VariantRec.GO_PROCESS,
								 VariantRec.GO_COMPONENT};
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append("#contig \t start \t end \t ref \t alt \t quality \t read.depth \t zygosity \t geno.qual");
		for(int i=0; i<keys.length; i++) {
			builder.append("\t " + keys[i]);
		}
		
		builder.append("\t effect.prediction");
		
		outputStream.println(builder.toString());
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(rec.toSimpleString());
		for(int i=0; i<keys.length; i++) {
			String val = rec.getPropertyOrAnnotation(keys[i]);
			if (keys[i] == VariantRec.RSNUM && (!val.equals("-"))) {
				val = "=HYPERLINK(\"http://www.ncbi.nlm.nih.gov/snp/?term=" + val + "\", \"" + val + "\")";
			}
			
			builder.append("\t" + val);
		}
		
		String effect = getEffectPrediction(rec);
		builder.append("\t" + effect);
		
		outputStream.println(builder.toString());
	}

	/**
	 * Returns a string that attempts to summarize sift, polyphen, mutation taster, and phylop
	 * scores into a couple qualitative categories
	 * @param rec
	 * @return
	 */
	private String getEffectPrediction(VariantRec rec) {
		Double sift = rec.getProperty(VariantRec.SIFT_SCORE);
		Double pp = rec.getProperty(VariantRec.POLYPHEN_SCORE);
		Double mt = rec.getProperty(VariantRec.MT_SCORE);
		Double phylop = rec.getProperty(VariantRec.PHYLOP_SCORE);
		
		int siftVal = 0;
		int ppVal = 0;
		int mtVal = 0;
		int phylopVal = 0;
		
		if (sift != null) {
			if (sift < 0.0001)
				siftVal = 3;
			else if (sift < 0.1)
				siftVal = 2;
			else 
				siftVal = 1;
		}
		
		if (pp != null) {
			if (pp > 0.899)
				ppVal = 3;
			else if (pp > 0.5)
				ppVal = 2;
			else 
				ppVal = 1;
		}
		
		if (mt != null) {
			if (mt > 0.899)
				mtVal = 3;
			else if (mt > 0.5)
				mtVal = 2;
			else 
				mtVal = 1;
		}
		
		if (phylop != null) {
			if (phylop > 0.899)
				phylopVal = 3;
			else if (phylop > 0.5)
				phylopVal = 2;
			else 
				phylopVal = 1;
		}
		
		int sum = siftVal + ppVal + mtVal + phylopVal;
		if (sum < 3) 
			return "unknown";
		if (sum < 6)
			return "benign";
		if (sum < 10)
			return "inconclusive";
		
		return "damaging";
	}

}
