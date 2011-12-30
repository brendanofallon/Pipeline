package buffer.variant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;

/**
 * Base class for things that maintain a collection of VariantRecs
 * @author brendan
 *
 */
public class AbstractVariantPool implements VariantPool {

	protected Map<String, List<VariantRec>>  vars = new HashMap<String, List<VariantRec>>();

	/**
	 * Search the 'vars' field for a VariantRec at the given contig and position
	 * @param contig
	 * @param pos
	 * @return
	 */
	public VariantRec findRecord(String contig, int pos) {
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("AnnovarResults could not find contig: " + contig);
			return null;
		}
		
		VariantRec qRec = new VariantRec(contig, pos, pos, 'x', 'x', 0, false, false);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return null;
		}
		
		return varList.get(index);
		
	}
	
	@Override
	public int getContigCount() {
		return vars.size();
	}

	@Override
	public Collection<String> getContigs() {
		return vars.keySet();
	}

	@Override
	public List<VariantRec> getVariantsForContig(String contig) {
		List<VariantRec> varList = vars.get(contig);
		if (varList != null)
			return varList;
		else 
			return new ArrayList<VariantRec>();
	}

	@Override
	public List<VariantRec> filterPool(VariantFilter filter) {
		List<VariantRec> passing = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (filter.passes(rec))
					passing.add(rec);
			}
		}
		return passing;
	}
	
	/**
	 * Return a new list of variants that are those in the given list that pass the given filter
	 * @param filter
	 * @param list
	 * @return
	 */
	public static List<VariantRec> filterList(VariantFilter filter, List<VariantRec> list) {
		List<VariantRec> passing = new ArrayList<VariantRec>(list.size()/2+1);
		for(VariantRec rec : list) {
			if (filter.passes(rec))
				passing.add(rec);
		}
		return passing;
	}
	
}
