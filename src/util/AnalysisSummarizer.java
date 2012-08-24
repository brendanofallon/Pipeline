package util;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Builds a more reader-friendly document from a 'VarRanker' analysis 
 * @author brendan
 *
 */
public class AnalysisSummarizer {

	
	public static void main(String[] args) {
		if (args.length==0) {
			System.out.println("Please provide the name of a VarRanker analysis file to summarize");
		}
		
		//args = new String[]{"/home/brendan/MORE_DATA/noonan/ranking/noonan20.capture.vqsr.analysis.output.csv"};
		
		File file = new File(args[0]);
		summarizeFile(file);
	}

	private static void summarizeFile(File file) {
		
		VariantPool pool;
		try {
			pool = new VariantPool(new CSVFile(file));
			List<VariantRec>  topRanked = findTopRankingVariants(pool);
			
			if (topRanked.size()==0) {
				System.err.println("Could not find any variants with ranking scores, you sure this is a VarRanker analysis file?");
				return;
			}
			
			for(VariantRec var : topRanked) {
				emitRecord(var);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
	}

	private static void emitRecord(VariantRec var) {
		DecimalFormat formatter = new DecimalFormat("0.00");
//		List<String> annos = new ArrayList<String>();
//		annos.add(VariantRec.GO_EFFECT_PROD);
//		annos.add(VariantRec.GENE_RELEVANCE);
//		annos.add(VariantRec.EFFECT_PREDICTION2);
		//System.out.println(var.toSimpleString() + "\t" + var.getPropertyString(annos));
		System.out.println("Gene : " + var.getAnnotation(VariantRec.GENE_NAME) + "\t - " + var.getAnnotation(VariantRec.EXON_FUNCTION));
		System.out.println("Change details : " + var.getAnnotation(VariantRec.PDOT) + "\t" + var.getAnnotation(VariantRec.CDOT) + "\t" + var.getAnnotation(VariantRec.NM_NUMBER));
		System.out.println("Overall score : " + formatter.format( var.getProperty(VariantRec.GO_EFFECT_PROD) ) + " ( damage: " + formatter.format(var.getProperty(VariantRec.EFFECT_PREDICTION2) ) + ", relevance: " + formatter.format( var.getProperty(VariantRec.GENE_RELEVANCE) ) + " ) ");
		System.out.println("Quality : " + var.getQuality() + " Coverage : " + var.getProperty(VariantRec.DEPTH) + " Variant depth: " + var.getProperty(VariantRec.VAR_DEPTH));
		System.out.println("Population frequencies : \t 1000 Genomes: " + var.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t ESP5400: " + var.getPropertyOrAnnotation(VariantRec.EXOMES_FREQ) + "\tComplete Genomics 69: " + var.getPropertyOrAnnotation(VariantRec.CG69_FREQUENCY));
		System.out.println("Disease associations : " + var.getAnnotation(VariantRec.DBNSFP_DISEASEDESC));
		
		//System.out.println("Gene summary: " + var.getAnnotation(VariantRec.S))
		System.out.println("Gene function : " + var.getAnnotation(VariantRec.DBNSFP_FUNCTIONDESC));
		
		
		System.out.println("\n");
		
	}

	private static List<VariantRec> findTopRankingVariants(VariantPool pool) {
		final int listSize = 10; //Number of variants to get
		List<VariantRec> allVars = new ArrayList<VariantRec>();
		for(String contig : pool.getContigs()) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				if (var.getProperty(VariantRec.GO_EFFECT_PROD) != null) {
					allVars.add(var);
				}
			}
		}
		
		Collections.sort(allVars, new EffectComparator());
		
		List<VariantRec> topVars = new ArrayList<VariantRec>();
		for(int i=0; i<Math.min(listSize, allVars.size()); i++) {
			topVars.add( allVars.get(i) );
		}
		return topVars;
	}
	
	static class EffectComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v0, VariantRec v1) {
			Double e0 = v0.getProperty(VariantRec.GO_EFFECT_PROD);
			Double e1 = v1.getProperty(VariantRec.GO_EFFECT_PROD);
			
			if (e0 != null && e1 != null) {
				if (e0 == 0 && e1 == 0) 
					return 0;
				return e1 < e0 ? -1 : 1;
			}
			
			return 0;
		}
		
	}
}
