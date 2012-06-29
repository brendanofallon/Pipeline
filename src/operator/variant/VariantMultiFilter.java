package operator.variant;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.FileBuffer;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.Operator;
import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * An operator that creates a new variant pool 
 * @author brendan
 *
 */
public class VariantMultiFilter extends IOOperator {

	enum Zygosity {ALL, HET, HOM};
	
	public static final String POP_FREQ = "pop.freq.cutoff";
	public static final String INCLUDE_DBSNP = "inc.dbsnp";
	public static final String DEPTH_CUTOFF = "depth.cutoff";
	public static final String VAR_DEPTH_CUTOFF = "var.depth.cutoff";
	public static final String VAR_FREQ_CUTOFF = "var.freq.cutoff";
	public static final String ZYGOSITY = "zygosity";
	
	VariantPool inVariants = null;
	VariantPool outVariants = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (inVariants == null) {
			throw new OperationFailedException("No input variants found", this);
		}
		if (outVariants == null) {
			throw new OperationFailedException("No output variants found", this);
		}
		
		
		//Read attributes
		final Double popFreqCutoff = readDoubleAttribute(POP_FREQ);
		final Double depthCutoff = readDoubleAttribute(DEPTH_CUTOFF);
		final Double varDepthCutoff = readDoubleAttribute(VAR_DEPTH_CUTOFF);
		final Double varFreqCutoff = readDoubleAttribute(VAR_FREQ_CUTOFF);
		
		Zygosity zygFilter = Zygosity.ALL;
		String zygStr = this.getAttribute(ZYGOSITY);
		if (zygStr != null) {
			if (zygStr.toUpperCase().startsWith("HET")) {
				zygFilter = Zygosity.HET;
			}
			if (zygStr.toUpperCase().startsWith("HOM")) {
				zygFilter = Zygosity.HOM;
			}
		}
		
		String message = "Filtering variants with following filters:"+ "\n";
		
		String dbSnpStr = this.getAttribute(INCLUDE_DBSNP);
		final Boolean includedbSNP;
		if (dbSnpStr != null && (!Boolean.parseBoolean(dbSnpStr))) {
			includedbSNP = false;
		}
		else 
			includedbSNP = true;
		message = message + " Including vars in dbSNP : " + includedbSNP + "\n";
		message = message + " Pop freq. cutoff : " + popFreqCutoff + "\n";
		message = message + " Var depth cutoff : " + varDepthCutoff+ "\n";
		message = message + " Var freq cutoff : " + varFreqCutoff+ "\n";
		message = message + " Zygosities included cutoff : " + zygFilter + "\n";
		logger.info(message);
		
		
		//Create filters
		List<VariantFilter> filters = new ArrayList<VariantFilter>();
		if (popFreqCutoff != null) {
			filters.add(VarFilterUtils.getPopFreqFilter(popFreqCutoff));
		}
		
		
		if (zygFilter == Zygosity.HET) {
			filters.add(VarFilterUtils.getHeteroFilter());
		}
		if (zygFilter == Zygosity.HOM) {
			filters.add(VarFilterUtils.getHomoFilter());
		}
		
		
		if (depthCutoff != null) {
			filters.add(new VariantFilter() {
				@Override
				public boolean passes(VariantRec rec) {
					Double depth = rec.getProperty(VariantRec.DEPTH);
					if (depth == null || depth > depthCutoff)
						return true;
					return false;
				}
			});
		}
		
		
		if (varDepthCutoff != null) {
			filters.add(new VariantFilter() {
				public boolean passes(VariantRec rec) {
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					if (varDepth == null || varDepth > varDepthCutoff)
						return true;
					return false;
				}
			});
		}
		
		if (varFreqCutoff != null) {
			filters.add(new VariantFilter() {
				public boolean passes(VariantRec rec) {
					Double totDepth = rec.getProperty(VariantRec.DEPTH);
					Double varDepth = rec.getProperty(VariantRec.VAR_DEPTH);
					if (totDepth == null || varDepth == null)
						return true;
					
					Double varFreq = varDepth / totDepth;
					
					if (varFreq > varFreqCutoff)
						return true;
					return false;
				}
			});
		}
		
		if (! includedbSNP) {
			filters.add(new VariantFilter() {
				@Override
				public boolean passes(VariantRec rec) {
					if (rec.getAnnotation(VariantRec.RSNUM) != null)
						return false;
					return true;
				}
				
			});
		}
		
		for(String contig : inVariants.getContigs()) {
			for(VariantRec var : inVariants.getVariantsForContig(contig)) {
				if (passesAllFilters(filters, var)) {
					outVariants.addRecordNoSort(var);
				}
			}
		}
		
		outVariants.sortAllContigs();
		
		logger.info("Successfully performed variant multi filter, retained " + outVariants.size() + " of " + inVariants.size() + " variants");
	}
	
	/**
	 * Returns true if the given variant passes all filters in the list of filters
	 * @param filters
	 * @param rec
	 * @return
	 */
	private static boolean passesAllFilters(List<VariantFilter> filters, VariantRec rec) {
		for(VariantFilter filter : filters) {
			if ( ! filter.passes(rec))
				return false;
		}
		return true;
	}

	private Double readDoubleAttribute(String anno) {
		String val = this.getAttribute(anno);
		if (val == null)
			return null;
		else
			return Double.parseDouble(val);
	}

	@Override
	public void initialize(NodeList children) {
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof VariantPool) {
						inVariants = (VariantPool)obj;
					}
					else {
						throw new IllegalArgumentException("Found non-variantpool object in output list for variant filter operator " + getObjectLabel());
					}
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof VariantPool) {
						outVariants = (VariantPool)obj;
					}
					else {
						throw new IllegalArgumentException("Found non-variantpool object in output list for variant filter operator " + getObjectLabel());
					}
				}
			}
		}
	}

}
