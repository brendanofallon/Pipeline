package rankingService;

import gene.Gene;

import java.io.PrintStream;

import operator.variant.VariantPoolWriter;
import buffer.variant.VariantRec;

/**
 * Handles writing of output in a simple, user-friendly format 
 * @author brendan
 *
 */
public class ResultsWriter extends VariantPoolWriter  {

	public static final String header = "Gene	cDot	pDot	disease.potential	gene.relevance	overall.score	rsNumber	population.frequency	top.pubmed.hit	goterm.hits	interaction.score	summary.score";
	
		
	public ResultsWriter() {
		
	}
	
	@Override
	public void writeHeader(PrintStream out) {
		out.println(header);
	}

	@Override
	public void writeVariant(VariantRec rec, PrintStream out) {
		
		Gene g = rec.getGene();
		if (g != null)
			g = genes.getGeneByName(rec.getAnnotation(VariantRec.GENE_NAME));
		
		if (g == null) {
			out.println( rec.getAnnotation(VariantRec.GENE_NAME) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.EFFECT_PREDICTION2) + "\t" + 
					"-" + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.GO_EFFECT_PROD) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.RSNUM) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + 
					"-" + "\t" + 
					"-" + "\t" + 
					"-" + "\t" + 
					"-");	
		}
		else {
			out.println( rec.getAnnotation(VariantRec.GENE_NAME) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.CDOT) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.PDOT) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.EFFECT_PREDICTION2) + "\t" + 
					g.getPropertyOrAnnotation(Gene.GENE_RELEVANCE) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.GO_EFFECT_PROD) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.RSNUM) + "\t" + 
					rec.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + 
					g.getPropertyOrAnnotation(Gene.PUBMED_HIT) + "\t" + 
					g.getPropertyOrAnnotation(Gene.GO_HITS) + "\t" + 
					g.getPropertyOrAnnotation(Gene.INTERACTION_SCORE) + "\t" + 
					g.getPropertyOrAnnotation(Gene.SUMMARY_SCORE));
		}
	}

}
