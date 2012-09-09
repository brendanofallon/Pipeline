package operator.qc.checkers;

import buffer.BAMMetrics;

/**
 * Checks base qualities and the fraction of unmapped reads
 * @author brendan
 *
 */
public class BAMMetricsChecker extends AbstractChecker<BAMMetrics> {

	@Override
	public operator.qc.checkers.QCItemCheck.QCCheckResult checkItem(
			BAMMetrics bam) {
		
		QCCheckResult result = new QCCheckResult();
		result.message = "";
		result.result = QCItemCheck.ResultType.OK;

		
		double fracUnmapped = bam.unmappedReads / bam.totalReads;
		
		if (fracUnmapped > 0.50) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message = "High fraction of unmapped reads : " + ("" + fracUnmapped*100.0).substring(0, 6) + "%\n";	
		}
		else {
			if (fracUnmapped > 0.25) {
				result.result = QCItemCheck.ResultType.WARNING;
				result.message =  "High fraction of unmapped reads : " + ("" + fracUnmapped*100.0).substring(0, 6) + "%\n";	
			}
		}
		

		
		double fracAbove10 = (double)bam.basesQAbove10 / (double)bam.basesRead;
		if (fracAbove10 < 0.85) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message =  "Few bases above Q10 (" + ("" + 100.0*fracAbove10).substring(0, 6) + "%)\n";
		}
		else {
			if (fracAbove10 < 0.95) {
				result.result = QCItemCheck.ResultType.WARNING;
				result.message =  "Few bases above Q10 (" + ("" + 100.0*fracAbove10).substring(0, 6) + "%)\n";
			}
		}
		
		
		
		
		double fracAbove30 = (double)bam.basesQAbove30 / (double)bam.basesRead;
		if (fracAbove30 < 0.70) {
			result.result = QCItemCheck.ResultType.SEVERE;
			result.message =  "Few bases above Q30 (" + ("" + 100.0*fracAbove30).substring(0, 6) + "%)\n";
		}
		else {
			if (fracAbove30 < 0.80) {
				result.result = QCItemCheck.ResultType.WARNING;
				result.message =  "Few bases above Q30 (" + ("" + 100.0*fracAbove30).substring(0, 6) + "%)\n";
			}
		}
		
		
		
		return result;
	}

}
