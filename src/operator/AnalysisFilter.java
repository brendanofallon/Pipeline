package operator;

import java.util.logging.Logger;

import operator.annovar.EffectPredictionAnnotator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BEDFile;
import buffer.GeneInteractionGraph;
import buffer.variant.GenePool;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Performs several filters designed to weed out non-interesting variants. We typically ignore 
 * variants with high frequency (defined vaguely), intergenic & synonymous variants, and those
 * with consistently benign gerp/sift/polyphen/phyloP scores
 * @author brendan
 *
 */
public class AnalysisFilter extends Operator {

	protected VariantPool inputVars = null;
	protected GenePool genes = null;
	protected GeneInteractionGraph graph = null;
	protected VariantPool outputPool = null;
	
	public static final String POP_FREQ_CUTOFF = "pop.freq.cutoff";
	public static final String COVERAGE_CUTOFF = "coverage.cutoff";
	public static final String VQSR_CUTOFF = "vqsr.cutoff";
	public static final String VAR_FREQ_CUTOFF = "var.freq.cutoff";
	
	protected double popFreqCutoff = 0.01; //Ignore variants with pop freqs greater than the given value
	protected double coverageCutoff = 5.0; //Ignore variants with less coverage than this
	protected double varFreqCutoff = 0.15; //Ignore variants with variant allele freqs less than this
	protected double vqsrCutoff = -10; //Ignore variants with VQSR scores less than the given value	
	protected double fpCutoff = -0.1; //Ignore variants with FP scores greater than the given value

	@Override
	public void performOperation() throws OperationFailedException {
		if (inputVars == null)
			throw new OperationFailedException("No input variants", this);
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Filtering variants input variant pool " + inputVars.getObjectLabel() + " for analysis");
		
		outputPool.clear();
		
		String attr = this.getAttribute(POP_FREQ_CUTOFF);
		if (attr != null) {
			popFreqCutoff = Double.parseDouble(attr);
		}
		
		attr = this.getAttribute(COVERAGE_CUTOFF);
		if (attr != null) {
			coverageCutoff = Double.parseDouble(attr);
		}
		
		attr = this.getAttribute(VQSR_CUTOFF);
		if (attr != null) {
			vqsrCutoff = Double.parseDouble(attr);
		}
		
		attr = this.getAttribute(VAR_FREQ_CUTOFF);
		if (attr != null) {
			varFreqCutoff = Double.parseDouble(attr);
		}
		
		logger.info("Initializing filter with following cutoffs: \n pop. freq :" + popFreqCutoff + "\n coverage :" + coverageCutoff + "\n vqsr:" + vqsrCutoff + "\n var. freq cutoff :" + varFreqCutoff);
		//First we filter by the gene pool, if not null

		int examined = 0;
		int passesFilters = 0;
		int ranked = 0;
		
		for(String contig : inputVars.getContigs()) {
			for(VariantRec var : inputVars.getVariantsForContig(contig)) {
				examined++;
					boolean passes = variantPassesFilters(var);
					if (passes) {
						passesFilters++;
						outputPool.addRecordNoSort(var);
						if (var.getProperty(VariantRec.GO_EFFECT_PROD) > 0) {
							ranked++;
						}
					}
			}
		}

		//Be sure to sort output pool now
		outputPool.sortAllContigs();
		logger.info("Analysis examined " + examined + " variants, " + passesFilters + " passed filters and " + ranked + " had positive scores");
	}

	private boolean variantPassesFilters(VariantRec var) {
		String type = var.getAnnotation(VariantRec.VARIANT_TYPE);
		String exonFunc = var.getAnnotation(VariantRec.EXON_FUNCTION);
		Double vqsr = var.getProperty(VariantRec.VQSR);
		Double popFreq = var.getProperty(VariantRec.POP_FREQUENCY);
		Double espFreq = var.getProperty(VariantRec.EXOMES_FREQ);
		
		Double depth = var.getProperty(VariantRec.DEPTH);
		Double varFreq = var.getProperty(VariantRec.VAR_DEPTH);
		Double goScore = var.getProperty(VariantRec.GO_SCORE);
		Double summaryScore = var.getProperty(VariantRec.SUMMARY_SCORE);
		Double interactionScore = var.getProperty(VariantRec.INTERACTION_SCORE);
		Double falsePosProb = var.getProperty(VariantRec.FALSEPOS_PROB);
		
		if (depth != null && varFreq != null) {
			if (depth > 10) //Only variant freq apply cutoff to variants with greater than 10 reads
				varFreq = varFreq / depth;
			else
				varFreq = null;
		}
				
		//Hard-coded exclusion of HLA and MUC genes
		String geneName = var.getAnnotation(VariantRec.GENE_NAME);
		if (geneName != null) {
			if (geneName.startsWith("HLA-") || geneName.startsWith("MUC")) {
				return false;
			}
		}
		
		//Ignore all intergenic variants
		if (type != null && (type.equals("intergenic") || type.contains("intron") || type.contains("upstream") || type.contains("downstream")))
			return false;
		
		//Ignore all synonymous variants
		if (exonFunc != null && exonFunc.equals("synonymous SNV")) {
			return false;
		}
		
		if (exonFunc != null && (!exonFunc.contains("nonsynonymous"))) {
			if (exonFunc.contains("synonymous") || exonFunc.trim().equals("-")) {
				return false;
			}
		}
		
		if (vqsr != null && vqsr < vqsrCutoff) {
			return false;
		}
		
		if (falsePosProb != null && falsePosProb > fpCutoff) {
			return false;
		}
		
		if (varFreq != null && varFreq < varFreqCutoff) {
			return false;
		}
		
		
		if (popFreq != null && popFreq > popFreqCutoff) {
			return false;
		}
		
		if (espFreq != null && espFreq > popFreqCutoff) {
			return false;
		}
		
		
		Double effectPred = EffectPredictionAnnotator.getEffectPredictionLinearWeight(var);
		var.addProperty(VariantRec.EFFECT_PREDICTION2, effectPred);
		
		double goVal = 0;
		double sumVal = 0;
		if (goScore != null)
			goVal = goScore;
		if (summaryScore != null)
			sumVal = summaryScore;
		
		double interactionVal = 0;
		if (interactionScore != null)
			interactionVal = interactionScore;
		
		
		double pubmedScore = 0;
		Double pmScore = var.getProperty(VariantRec.PUBMED_SCORE);
		if (pmScore != null) {
			pubmedScore = pmScore;
		}
		
		Double relevanceScore = (goVal + sumVal + 10*interactionVal + pubmedScore/2.0);
		
		Double goEffectProd = effectPred * relevanceScore;
		var.addProperty(VariantRec.GO_EFFECT_PROD, goEffectProd);
		var.addProperty(VariantRec.GENE_RELEVANCE, relevanceScore);
		
		return true;
	}
	
	@Override
	public void initialize(NodeList children) {
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		NodeList inputChildren = inputList.getChildNodes();
		for(int i=0; i<inputChildren.getLength(); i++) {	
			Node iChild = inputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof GenePool) {
					genes = (GenePool)obj;
				}
				if (obj instanceof VariantPool) {
					inputVars = (VariantPool)obj;
				}
				
				if (obj instanceof GeneInteractionGraph) {
					graph = (GeneInteractionGraph)obj;
				}
			}
		}
		
		NodeList outputChildren = outputList.getChildNodes();
		for(int i=0; i<outputChildren.getLength(); i++) {	
			Node iChild = outputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof VariantPool) {
					outputPool = (VariantPool)obj;
				}
		
			}
		}
		
		
	}

}
