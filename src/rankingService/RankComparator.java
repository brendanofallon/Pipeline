package rankingService;

import java.util.Comparator;

import buffer.variant.VariantRec;

/**
 * Compares two variants to see which has the greater GO_EFFECT_PROD score
 * @author brendan
 *
 */
public class RankComparator implements Comparator<VariantRec> {

	@Override
	public int compare(VariantRec v0, VariantRec v1) {
		Double rank0 = v0.getProperty(VariantRec.GO_EFFECT_PROD);
		Double rank1 = v1.getProperty(VariantRec.GO_EFFECT_PROD);
		if (rank0 == null && rank1 == null || (rank1 == rank0))
			return 0;
		
		if (rank0 == null)
			return -1;
		if (rank1 == null)
			return -1;
		
		if (rank1 == rank0)
			return 0;
		
		if (rank1 < rank0)
			return -1;
		else
			return 1;
		
	}

}
