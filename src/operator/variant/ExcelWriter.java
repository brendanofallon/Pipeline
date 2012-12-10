package operator.variant;

import gene.Gene;

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
								 //VariantRec.INTERACTION_SCORE,
								 //VariantRec.GO_EFFECT_PROD,
								 VariantRec.EFFECT_RELEVANCE_PRODUCT,
								 VariantRec.POP_FREQUENCY,
								 //VariantRec.AMR_FREQUENCY,
								 VariantRec.EXOMES_FREQ,
								 VariantRec.ARUP_TOT,
								 VariantRec.ARUP_FREQ,
								 //VariantRec.CG69_FREQUENCY,
								 VariantRec.RSNUM, 
//								 VariantRec.OMIM_ID,
								 VariantRec.HGMD_HIT,
//								 VariantRec.VQSR,
//								 VariantRec.FALSEPOS_PROB,
//								 VariantRec.FS_SCORE,
								 VariantRec.SIFT_SCORE, 
								 VariantRec.POLYPHEN_SCORE,
								 VariantRec.POLYPHEN_HVAR_SCORE,
								 VariantRec.PHYLOP_SCORE, 
								 VariantRec.MT_SCORE,
								 VariantRec.GERP_SCORE,
								 VariantRec.GERP_NR_SCORE,
								 VariantRec.LRT_SCORE,
								 VariantRec.SIPHY_SCORE,
								 VariantRec.MA_SCORE,
								 VariantRec.SVM_EFFECT
								 };
	
	public static String[] geneKeys = {
		Gene.HGMD_INFO,
		Gene.GENE_RELEVANCE,
//		Gene.SUMMARY_SCORE, 
//		Gene.PUBMED_SCORE, 
//		Gene.INTERACTION_SCORE, 
//		Gene.DBNSFP_MIMDISEASE, 
//		Gene.DBNSFP_DISEASEDESC, 
//		Gene.PUBMED_HIT
		};
	
	@Override
	public void writeHeader(PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(VariantRec.getSimpleHeader());
		for(int i=0; i<keys.length; i++) {
			builder.append("\t " + keys[i]);
		}
		
		for(int i=0; i<geneKeys.length; i++) {
			builder.append("\t " + geneKeys[i]);
		}

		outputStream.println(builder.toString());
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream outputStream) {
		StringBuilder builder = new StringBuilder();
		builder.append(rec.toSimpleString());
		for(int i=0; i<keys.length; i++) {
			String val = rec.getPropertyOrAnnotation(keys[i]).trim();			
			builder.append("\t" + val);
		}
		
		for(int i=0; i<geneKeys.length; i++) {
			Gene g = rec.getGene();
			if (g == null) {
				String geneName = rec.getAnnotation(VariantRec.GENE_NAME);
				if (geneName != null && genes != null)
					g = genes.getGeneByName(geneName);
			}
			
			String val = "-";
			if (g != null) {
				val = g.getPropertyOrAnnotation(geneKeys[i]).trim();
			}
			
			builder.append("\t" + val);
		}
		
		
		outputStream.println(builder.toString());
	}

	

}
