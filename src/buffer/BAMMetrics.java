package buffer;

import math.Histogram;

/**
 * Basically just storage for a bunch of summary info about a BAM file
 * @author brendan
 *
 */
public class BAMMetrics extends FileBuffer {

	public String path;
	public int totalReads;
	public Histogram insertSizeHistogram;
	public Histogram baseQualityHistogram;
	public Histogram[] readPosQualHistos;
	public int unmappedReads;
	public int unmappedMates;
	public int duplicateReads;
	public int lowVendorQualityReads;
	public int hugeInsertSize;
	public long basesRead;
	public long basesQAbove30;
	public long basesQAbove20;
	public long basesQAbove10;
	
	@Override
	public String getTypeStr() {
		return "BAM Metrics";
	}

}
