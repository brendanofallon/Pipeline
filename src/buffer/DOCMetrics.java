package buffer;

import java.util.Collection;

import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

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
	
	public DOCMetrics() {
		System.out.println("Creating new DOC metrics!");
	}
	
	public String getSourceFile() {
		return sourceFile;
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
	
	

}
