package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A GenePool contains a bunch of variant records but organizes them by
 * gene, not genomic position. 
 * @author brendan
 *
 */
public class GenePool {

	//Map is keyed by gene and values are a (possibly empty) list of variant records
	Map<String, List<VariantRec>> pool = new HashMap<String, List<VariantRec>>();
	
	public GenePool() {
		//Create an empty pool
	}
	
	/**
	 * Create a GenePool using the variant pool given. Variants without the annotation
	 * GENE_NAME are ignored
	 */
	public GenePool(VariantPool variants) {
		int count = 0;
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				boolean added = addRecordNoWarn(rec);
				if (added)
					count++;
			}
		}
		System.out.println("Added " + count + " records to gene pool");
	}
	
	public GenePool(List<VariantRec> variants) {
		int count = 0;
		for(VariantRec rec : variants) {
			boolean added = addRecordNoWarn(rec);
				if (added)
					count++;
		}
		System.out.println("Added " + count + " records to gene pool");
	}
	
	/**
	 * Reads the given file and adds the gene names found to this pool.
	 * @param inputFile
	 * @throws IOException 
	 */
	public GenePool(File inputFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while (line != null) {
			String[] toks = line.split("\t");
			String geneName = toks[0];
			System.out.println("Adding gene " + geneName);
			addGene(geneName);
		}
	}

	/**
	 * Get the list of variants for a given gene name. May return an empty or 
	 * NULL list 
	 * @param geneName
	 * @return
	 */
	public List<VariantRec> getVariantsForGene(String geneName) {
		return pool.get(geneName);
	}
	
	/**
	 * Return a collection containing all gene names used as keys in this pool
	 * @return
	 */
	public Collection<String> getGenes() {
		return pool.keySet();
	}
	
	public void removeVariant(VariantRec variant) {
		String geneName = variant.getAnnotation(VariantRec.GENE_NAME);
		if (geneName == null)
			throw new IllegalArgumentException("Variant does not have a GENE_NAME");
		List<VariantRec> list = pool.get(geneName);
		boolean removed = list.remove(variant);
		if (!removed) {
			throw new IllegalArgumentException("Variant was not in pool");
		}
	}
	
	/**
	 * Add a new single gene to the pool, with no variants associated. 
	 * Has no effect if there is already a list of variants associated with the gene
	 * @param geneName
	 */
	public void addGene(String geneName) {
		List<VariantRec> vars = pool.get(geneName);
		if (vars == null) {
			vars = new ArrayList<VariantRec>();
			pool.put(geneName, vars);
		}
	}
	
	/**
	 * Attempt to add the given record to this pool, but don't warn
	 * if the record does not contain the GENE_NAME annotation
	 * returns true if the record was added.
	 * @param rec
	 */
	public boolean addRecordNoWarn(VariantRec rec) {
		String variantType = rec.getAnnotation(VariantRec.VARIANT_TYPE);
		
		//Skip these - there's often a few with no variant type annotation
		if (variantType == null) {
			return false;
		}
		
		//Ignore everything except exonic 
		if ( !variantType.contains("exonic")) {
			return false;
		}
		
		String geneName = rec.getAnnotation(VariantRec.GENE_NAME); 
		
		if (geneName != null) {
			List<VariantRec> vars = pool.get(geneName);
			if (vars == null) {
				vars = new ArrayList<VariantRec>();
				pool.put(geneName, vars);
			}
			vars.add(rec);
			return true;
		}
		return false;
	}
	
}
