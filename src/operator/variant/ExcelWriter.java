package operator.variant;

import java.io.PrintStream;

import buffer.variant.VariantRec;

/**
 * Writes a variant pool in a nifty format designed to work well with microsoft excel
 * @author brendan
 *
 */
public class ExcelWriter extends VariantPoolWriter {

	public final static String[] keys = new String[]{VariantRec.GENE_NAME,
								 VariantRec.NM_NUMBER,
								 VariantRec.CDOT,
								 VariantRec.PDOT,
								 VariantRec.EXON_NUMBER,
								 VariantRec.VARIANT_TYPE, 
								 VariantRec.EXON_FUNCTION,
								 VariantRec.EFFECT_PREDICTION2,
								 VariantRec.INTERACTION_SCORE,
								 VariantRec.GO_EFFECT_PROD,
								 VariantRec.POP_FREQUENCY,
								 VariantRec.AMR_FREQUENCY,
								 VariantRec.EXOMES_FREQ,
								 VariantRec.CG69_FREQUENCY,
								 VariantRec.RSNUM, 
								 VariantRec.OMIM_ID,
								 VariantRec.HGMD_INFO,
								 VariantRec.VQSR,
								 VariantRec.FALSEPOS_PROB,
								 VariantRec.FS_SCORE,
								 VariantRec.SIFT_SCORE, 
								 VariantRec.POLYPHEN_SCORE, 
								 VariantRec.PHYLOP_SCORE, 
								 VariantRec.MT_SCORE,
								 VariantRec.GERP_SCORE,
								 VariantRec.LRT_SCORE,
								 VariantRec.SIPHY_SCORE};
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(VariantRec.getSimpleHeader());
		for(int i=0; i<keys.length; i++) {
			builder.append("\t " + keys[i]);
		}

		outputStream.println(builder.toString());
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(rec.toSimpleString());
		for(int i=0; i<keys.length; i++) {
			String val = rec.getPropertyOrAnnotation(keys[i]).trim();
//			if (keys[i] == VariantRec.RSNUM && (!val.trim().equals("-"))) {
//				val = "=HYPERLINK(\"http://www.ncbi.nlm.nih.gov/snp/?term=" + val + "\", \"" + val + "\")";
//			}
			
			builder.append("\t" + val);
		}
		
		
		outputStream.println(builder.toString());
	}

	

}
