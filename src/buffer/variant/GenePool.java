package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.CSVFile;
import buffer.VCFFile;

import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * A GenePool contains a bunch of variant records but organizes them by
 * gene, not genomic position. Generally speaking, variants without a GENE_NAME annotation
 * are ignored. 
 * @author brendan
 *
 */
public class GenePool extends Operator {

	//Map is keyed by gene and values are a (possibly empty, but non-null) list of variant records
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
		//System.out.println("Added " + count + " records to gene pool");
	}
	
	public GenePool(List<VariantRec> variants) {
		int count = 0;
		for(VariantRec rec : variants) {
			boolean added = addRecordNoWarn(rec);
				if (added)
					count++;
		}
		//System.out.println("Added " + count + " records to gene pool");
	}
	
	/**
	 * Reads the given file and adds the gene names found to this pool.
	 * @param inputFile
	 * @throws IOException 
	 */
	public GenePool(File inputFile) throws IOException {
		readGenesFromFile(inputFile);
	}
	
	/**
	 * Read in all genes in the given text file. We assume there's one gene name per line and ignore
	 * blank lines and lines starting with #
	 * 
	 * @param inputFile
	 * @throws IOException 
	 */
	public void readGenesFromFile(File inputFile) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while (line != null) {
			if (line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			String[] toks = line.split("\t");
			String geneName = toks[0].trim();
			if (geneName.length()>0) {
				//System.out.println("Adding gene " + geneName);
				addGene(geneName);
			}
			line = reader.readLine();
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
	
	/**
	 * Returns true if there is an entry for the given gene in this gene pool (even
	 * if the list of variants associated with the gene is empty)
	 * @param geneName
	 * @return
	 */
	public boolean containsGene(String geneName) {
		return pool.containsKey(geneName);
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
	 * Add all variants in the given pool to this gene pool, but don't warn if some variants
	 * aren't in a gene. Returns total number of variants successfully added
	 * @param pool
	 * @return
	 */
	public int addPool(VariantPool pool) {
		int count = 0;
		for(String contig : pool.getContigs()) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				boolean added = addRecordNoWarn(var);
				if (added)
					count++;
			}
		}
		return count;
	}
	
	/**
	 * Returns the number of genes in this gene pool
	 * @return
	 */
	public int size() {
		return pool.size();
	}
	
	/**
	 * Attempt to add the given record to this pool, but don't warn
	 * if the record does not contain the GENE_NAME annotation
	 * returns true if the record was added.
	 * @param rec
	 */
	public boolean addRecordNoWarn(VariantRec rec) {		
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
	
	/**
	 * Counts the number of UNIQUE 'sources' (from VariantRec.SOURCE) found among
	 * all variants associated with the given gene
	 * @param gene
	 * @return
	 */
	public int countSources(String gene) {
		List<VariantRec> vars = pool.get(gene);
		Set<String> sources = new HashSet<String>();
		for(VariantRec var : vars) {
			sources.add(var.getAnnotation(VariantRec.SOURCE));
		}
		return sources.size();
	}
	
	
	public void listAll(PrintStream out) {
		for(String key : pool.keySet()) {
			List<VariantRec> vars = pool.get(key);
			int sourceCount =  countSources(key);
			out.println("GENE:" + key + " : " + vars.size() + "\t" + sourceCount);
			for(VariantRec var : vars) {
				out.println("\t" + var.getAnnotation(VariantRec.SOURCE) + "\t " + var.toSimpleString() + "\t" + var.getAnnotation(VariantRec.EXON_FUNCTION) + "\t" + var.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) + "\t" + var.getAnnotation(VariantRec.CDOT) + "\t" + var.getAnnotation(VariantRec.PDOT));
			}

			out.println();
		}
	}

	public void listGenesWithMultipleVars(PrintStream out, int cutoff, List<String> annoKeys) {
		StringBuilder headerB = new StringBuilder();
		for(String key : annoKeys)
			headerB.append("\t" + key);
		
		out.println(VariantRec.getBasicHeader() + headerB);
		for(String key : pool.keySet()) {
			List<VariantRec> vars = pool.get(key);
			int sourceCount =  countSources(key);
			if (sourceCount >= cutoff ) {
				out.println("GENE:" + key + " : " + vars.size() + "\t" + sourceCount + "\t" + computeMeanProd(vars));
				for(VariantRec var : vars) {
					out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toBasicString() + "\t" + var.getPropertyString(annoKeys));
				}
			}
		}		
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (geneListFile == null) 
			return ; // Just make an empty pool- should actually never happen since we demand that filename be specified
		

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Building gene pool from variants in file " + geneListFile.getAbsolutePath());
		
		try {
			readGenesFromFile(geneListFile);
		} catch (IOException e) {
			
			e.printStackTrace();
			throw new OperationFailedException("Could not read genes in file : " + geneListFile.getAbsolutePath(), this);
		}
				
		logger.info("Created gene pool with " + this.size() + " total genes");
	}

	@Override
	public void initialize(NodeList children) {
		//We just look to see if there was a filename specified as an attribute. We will then read the
		//file in performOperation()
		String inputPath = properties.get(FILENAME);
		if (inputPath == null)
			throw new IllegalArgumentException("No filename specified to create gene list");
		
		File file = new File(inputPath);
		if (! file.exists()) {
			throw new IllegalArgumentException("Gene list input file : " + file.getAbsolutePath() + " does not exist");
		
		}
	}
	
	/**
	 * Computes the mean go_effect_product of the variants in the list
	 * @param vars
	 * @return
	 */
	public static double computeMeanProd(List<VariantRec> vars) {
		double sum = 0;
		for(VariantRec rec : vars) {
			Double prod = rec.getProperty(VariantRec.GO_EFFECT_PROD);
			if (prod != null)
				sum += prod;
		}
		return sum / vars.size();
	}
	
	public static final String FILENAME = "filename";
	private File geneListFile = null;
}
