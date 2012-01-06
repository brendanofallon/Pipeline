package buffer.variant;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import buffer.ReferenceFile;

import pipeline.Pipeline;

/**
 * Base class for things that maintain a collection of VariantRecs
 * @author brendan
 *
 */
public class AbstractVariantPool implements VariantPool {

	protected Map<String, List<VariantRec>>  vars = new HashMap<String, List<VariantRec>>();

	/**
	 * Create a new pool with all the variants in the source pool
	 * @param sourceVars
	 */
	public AbstractVariantPool(VariantPool sourceVars) {
		addAll(sourceVars);
	}
	
	
	/**
	 * Construct new empty variant pool
	 */
	public AbstractVariantPool() {
		//blank on purpose
	}
	
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
	
	/**
	 * Add all variants from source to this pool
	 * @param source
	 */
	public void addAll(VariantPool source) {
		for(String contig : source.getContigs()) {
			for(VariantRec rec : source.getVariantsForContig(contig)) {
				this.addRecord(rec);
			}
		}
	}
	
	/**
	 * Return total number of variants in pool
	 * @return
	 */
	public int size() {
		int count = 0;
		for(String contig : getContigs()) {
			count += this.getVariantsForContig(contig).size();
		}
		return count;
	}
	
	/**
	 * Emit a tab-separated listing of all variants to the given stream
	 * @param out
	 */
	public void listAll(PrintStream out) {
		for(String contig : getContigs() ) {
			for(VariantRec rec : this.getVariantsForContig(contig)) {
				out.println(contig + "\t" + rec.getStart() + "\t . \t" + rec.ref + "\t" + rec.alt + "\t" + rec.getQuality() + "\t" + rec.getProperty(VariantRec.DEPTH));
			}
		}
	}
	
	/**
	 * Return a new variant pool that is the intersection of this pool and the given pool.
	 * Note that variants are not cloned - they are the variants from this pool
	 * @param varsB
	 * @return
	 */
	public VariantPool intersect(VariantPool varsB) {
		AbstractVariantPool intersect = new AbstractVariantPool();
		for(String contig : getContigs()) {
			for(VariantRec rec : getVariantsForContig(contig)) {
				if (varsB.findRecord(rec.getContig(), rec.getStart()) != null) {
					intersect.addRecord(rec);
				}
			}
		}
		return intersect;
	}
	
	/**
	 * Removes from this pool all variants at the sites of the variants in the second pool
	 * @param toRemove
	 * @return
	 */
	public int removeVariants(VariantPool toRemove) {
		int removedCount = 0;
		for(String contig : toRemove.getContigs()) {
			for(VariantRec rec : toRemove.getVariantsForContig(contig)) {
				boolean found = removeRecordAtPos(rec.getContig(), rec.getStart());
				if (found)
					removedCount++;
			}
		}
		return removedCount;
	}
	
	/**
	 * Return total number of heterozygotes in pool
	 * @return
	 */
	public int countHeteros() {
		int count = 0;
		for(String contig : vars.keySet()) {
			Collection<VariantRec> varRecs = this.getVariantsForContig(contig);
			for(VariantRec rec : varRecs) {
				if (rec.isHetero()) 
					count++;
			}
		}

		return count;

	}
	
	public boolean removeRecordAtPos(String contig, int pos) {
		List<VariantRec> varList = vars.get(contig);
		if (varList == null) {
			return false;
		}
		
		VariantRec qRec = new VariantRec(contig, pos, pos, 'x', 'x', 0, false, false);
		
		int index = Collections.binarySearch(varList, qRec, VariantRec.getPositionComparator());
		if (index < 0) {
			return false;
		}
		
		varList.remove(index);
		return true;
	}
	
	/**
	 * Add a new record to the pool. This re-sorts the contig every time to ensure that records are sorted
	 * @param rec
	 */
	public void addRecord(VariantRec rec) {
		List<VariantRec> contigVars = vars.get( rec.getContig() ); 
		if (contigVars == null) {
			contigVars = new ArrayList<VariantRec>(128);
			vars.put(rec.getContig(), contigVars);
		}
		contigVars.add(rec);
		Collections.sort(contigVars, VariantRec.getPositionComparator());
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
	
	public void toVCF(ReferenceFile ref, List<String> annotationKeys, PrintStream out) {
		//Make a VCF header
		out.println("##fileformat=VCFv4.1");
		
		Calendar now = Calendar.getInstance();
		out.println("##fileDate=" + now.get(Calendar.YEAR) + now.get(Calendar.MONTH) + now.get(Calendar.DAY_OF_WEEK_IN_MONTH) );
		out.println("##source=PipelineVariantPool");
		out.println("##reference=" + ref.getFilename()  );
		for(String contig : getContigs()) {
			out.println("##contig=<ID=" + contig + ",species=\"Homo sapiens\">\n" );
		}
		
		
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
