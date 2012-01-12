package buffer.variant;

import java.util.Collection;
import java.util.List;

/**
 * Anything that can harbor a collection of variants, grouped by contig and sorted
 * @author brendan
 *
 */
public interface VariantPool {

	/**
	 * Obtain total number of contigs in the collection
	 * @return
	 */
	public int getContigCount();
	
	/**
	 * Obtain a Collection of all contig names
	 * @return
	 */
	public Collection<String> getContigs();
	
	/**
	 * Obtain a list of all VariantRecs in a contig
	 * @param contig
	 * @return
	 */
	public List<VariantRec> getVariantsForContig(String contig);
	
	/**
	 * Obtain the VariantRec at the given contig and position, or null if there is no such contig
	 * @param contig
	 * @param pos
	 * @return
	 */
	public VariantRec findRecord(String contig, int pos);
	
	/**
	 * Return a list of variants that passes the given filter
	 * @param filter
	 * @return
	 */
	public List<VariantRec> filterPool(VariantFilter filter);
	
}
