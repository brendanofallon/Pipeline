package operator.annovar;

import buffer.variant.VariantRec;
import operator.OperationFailedException;

public class EffectPredictionAnnotator extends Annotator {

	@Override
	public void annotateVariant(VariantRec var) {
		Integer effect = getEffectPredictionSimple(var);
		var.addProperty(VariantRec.EFFECT_PREDICTION, new Double(effect));

		Double val = getEffectPredictionLinearWeight(var);
		var.addProperty(VariantRec.EFFECT_PREDICTION2, val);

	}

	public static double getEffectPredictionLinearWeight(VariantRec rec) {
		double siftWeight = -8.0;
		double ppWeight = -1.0;
		double mtWeight = 23.0;
		double phylopWeight = -8.0;
		double gerpWeight = 1.0;
		double predWeight = 4;

		Double sift = rec.getProperty(VariantRec.SIFT_SCORE);
		Double pp = rec.getProperty(VariantRec.POLYPHEN_SCORE);
		Double mt = rec.getProperty(VariantRec.MT_SCORE);
		Double phylop = rec.getProperty(VariantRec.PHYLOP_SCORE);
		Double gerp = rec.getProperty(VariantRec.GERP_SCORE);
		Double pred = rec.getProperty(VariantRec.EFFECT_PREDICTION);
		
		if (sift == null)
			sift = 0d;
		if (pp == null)
			pp = 0d;
		if (mt == null)
			mt = 0d;
		if (phylop == null)
			phylop = 0d;
		if (gerp == null)
			gerp = 0d;
		if (pred == null)
			pred = 0d;
		
		double val = sift * siftWeight 
					+ pp * ppWeight
					+ mt * mtWeight
					+ phylop * phylopWeight
					+ gerp + gerpWeight
					+ pred * predWeight;
		
		return val;
	}
	
	/**
	 * Returns an integer that attempts to summarize sift, polyphen, mutation taster, and phylop
	 * scores into simple tripartite categorization
	 * Higher scores are more indicative of damagingness, the highest possible score, 5, means that
	 * every tool thought the given variant was damaging.
	 * Scores near zero are inconclusive, either because no data is available or because the scores conflict
	 * Negative scores mean that the tools believe the variant to be benign
	 * @param rec
	 * @return
	 */
	public static Integer getEffectPredictionSimple(VariantRec rec) {
		Double sift = rec.getProperty(VariantRec.SIFT_SCORE);
		Double pp = rec.getProperty(VariantRec.POLYPHEN_SCORE);
		Double mt = rec.getProperty(VariantRec.MT_SCORE);
		Double phylop = rec.getProperty(VariantRec.PHYLOP_SCORE);
		Double gerp = rec.getProperty(VariantRec.GERP_SCORE);
		
		String exonFuncType = rec.getAnnotation(VariantRec.EXON_FUNCTION);
		if (exonFuncType != null) {
			if (exonFuncType.contains("frameshift")) {
				if (exonFuncType.contains("non"))
					return 3;
				else
					return 5;
			}
			if (exonFuncType.contains("stopgain"))
				return 5;
			if (exonFuncType.contains("stoploss"))
				return 4;
			if (exonFuncType.contains("splice"))
				return 4;
		}
		
		int siftVal = 0;
		int ppVal = 0;
		int mtVal = 0;
		int phylopVal = 0;
		int gerpVal = 0;
		
		int scored = 0;
		
		if (sift != null) {
			if (sift < 0.0001)
				siftVal = 1;
			else if (sift < 0.1)
				siftVal = 0;
			else 
				siftVal = -1;
			scored++;
		}
		
		if (pp != null) {
			if (pp > 0.899)
				ppVal = 1;
			else if (pp > 0.5)
				ppVal = 0;
			else 
				ppVal = -1;
			scored++;
		}
		
		if (mt != null) {
			if (mt > 0.899)
				mtVal = 1;
			else if (mt > 0.5)
				mtVal = 0;
			else 
				mtVal = -1;
			scored++;
		}
		
		if (phylop != null) {
			if (phylop > 0.899)
				phylopVal = 1;
			else if (phylop > 0.5)
				phylopVal = 0;
			else 
				phylopVal = -1;
			scored++;
		}
		
		
		if (gerp != null) {
			if (gerp > 3) 
				gerpVal = 1;
			else if (gerp > 1)
				gerpVal = 0;
			else
				gerpVal = -1;
			scored++;
		}
		
		int sum = siftVal + ppVal + mtVal + phylopVal + gerpVal;
		return sum;
	}
	

}
