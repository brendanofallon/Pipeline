package buffer;

import java.util.List;

import org.w3c.dom.NodeList;

/**
 * A small container class for some Depth Of Coverage metrics computed by the GaTK
 * @author brendan
 *
 */
public class DOCMetrics extends FileBuffer {

	protected String sourceFile = null;
	protected double meanCoverage = -1;
	protected int[] cutoffs;
	protected double[] fractionAboveCutoff;
	protected List<FlaggedInterval> flaggedIntervals = null;
	
	

	public DOCMetrics() {
		System.out.println("Creating new DOC metrics!");
	}
	
	public String getSourceFile() {
		return sourceFile;
	}

	public List<FlaggedInterval> getFlaggedIntervals() {
		return flaggedIntervals;
	}

	public void setFlaggedIntervals(List<FlaggedInterval> flaggedIntervals) {
		this.flaggedIntervals = flaggedIntervals;
	}
	
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public double getMeanCoverage() {
		return meanCoverage;
	}

	public void setMeanCoverage(double meanCoverage) {
		this.meanCoverage = meanCoverage;
	}

	public int[] getCutoffs() {
		return cutoffs;
	}

	public void setCutoffs(int[] cutoffs) {
		this.cutoffs = cutoffs;
	}

	public double[] getFractionAboveCutoff() {
		return fractionAboveCutoff;
	}

	public void setFractionAboveCutoff(double[] fractionAboveCutoff) {
		this.fractionAboveCutoff = fractionAboveCutoff;
	}
	
	
	@Override
	public void initialize(NodeList children) {
		// Nothing to do
	}

	@Override
	public String getTypeStr() {
		return "DOCMetrics";
	}

	public String toString() {
		return "DOC metrics for " + sourceFile + " : " + getMeanCoverage(); // + " coverage at " + cutoffs[0] + " : " + fractionAboveCutoff[0];
	}
	
	

	public static class FlaggedInterval {
		public String info = null;
		public double mean = 0;
		public double frac = 0;
	}
}
