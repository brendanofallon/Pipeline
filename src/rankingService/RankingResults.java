package rankingService;

import java.util.List;

import buffer.variant.VariantRec;

/**
 * The result of a RankingServiceJob - this is created when a ranking service job
 * completes and stores the completion state as well as the list of ranked variants
 * 
 * @author brendan
 *
 */
public class RankingResults {

	List<VariantRec> variants = null;
	
}
