package operator;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import operator.annovar.GOAnnotator;
import operator.annovar.GOTerms;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.GenePool;
import buffer.variant.VariantRec;

/**
 * Takes as input a GenePool with GO terms added as annotations. Then emits a histogram
 * showing how the distribution of frequencies of GO terms.  
 * @author brendan
 *
 */
public class GOTermHistogram extends Operator {
	
	protected static GOTerms goTerms = null;
	protected VariantPool variants = null;
	private boolean uniqify = true; //Output histogram for genes, not variants
	
	protected CSVFile outputFile = null;
	
	//Storage for histograms
	Map<String, Integer> processCount = new HashMap<String, Integer>();
	Map<String, Integer> functionCount = new HashMap<String, Integer>();
	Map<String, Integer> compCount = new HashMap<String, Integer>();
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null) {
			throw new OperationFailedException("VariantPool not set for GOTermHistogram", this);
		}
	
		if (goTerms == null)
			goTerms = new GOTerms();
		
		
		processCount = new HashMap<String, Integer>();
		functionCount = new HashMap<String, Integer>();
		compCount = new HashMap<String, Integer>();
			
		
		if (uniqify) {
			//Build list of all gene names
			Set<String> geneNames = new HashSet<String>();
			for(String contig : variants.getContigs()) {
				for(VariantRec rec : variants.getVariantsForContig(contig)) {
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					geneNames.add(gene);
				}
			}
			
			System.out.println("Found " + geneNames.size() + " unique genes in input");
			String[] names = geneNames.toArray(new String[]{});
			for(int i=0; i<names.length; i++) {
				addGeneToHistogram(names[i]);
			}
		}
		else {
			for(String contig : variants.getContigs()) {
				for(VariantRec rec : variants.getVariantsForContig(contig)) {
					String gene = rec.getAnnotation(VariantRec.GENE_NAME);
					if (gene != null) {
						addGeneToHistogram(gene);
					}
				}
			}
		}
		
		PrintStream outputStream = System.out;
		if (outputFile != null) {
			try {
				outputStream = new PrintStream(new FileOutputStream(outputFile.getFile()));
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}
		
		emitHistograms(outputStream);
				
	}

	/**
	 * Add GO info for the given gene to each histogram
	 * @param name
	 */
	private void addGeneToHistogram(String name) {
		List<String> procs = goTerms.getProcessesForGene(name);
		for(String proc : procs) {
			Integer c = processCount.get(proc);
			if (c == null) {
				c = 0;
			}
			processCount.put(proc, c+1);	
		}
		
		
		List<String> funcs = goTerms.getFunctionsForGene(name);
		for(String func : funcs) {
			Integer c = functionCount.get(func);
			if (c == null) {
				c = 0;
			}
			functionCount.put(func, c+1);
		}
		
		
		List<String> comps = goTerms.getComponentsForGene(name);
		for(String comp : comps) {
			Integer c = compCount.get(comp);
			if (c == null) {
				c = 0;
			}
			compCount.put(comp, c+1);
		}
	}

	/**
	 * Write a text summary of histograms to the given print stream
	 * @param out
	 */
	public void emitHistograms(PrintStream out) {
		out.println("Processes:");
		for(String proc : goTerms.getAllProcesses()) {
			Integer c = processCount.get(proc);
			if (c == null)
				c = 0;
			out.println(proc + "\t : " + c);
		}
		out.println();
		
		out.println("Functions:");
		for(String proc : goTerms.getAllFunctions()) {
			Integer c = functionCount.get(proc);
			if (c == null)
				c = 0;
			out.println(proc + "\t : " + c);
		}
		out.println();
		
		out.println("Components:");
		for(String proc : goTerms.getAllComponents()) {
			Integer c = compCount.get(proc);
			if (c == null)
				c = 0;
			out.println(proc + "\t : " + c);
		}
		out.println();
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}
				if (obj instanceof CSVFile) {
					outputFile = (CSVFile)obj;
				}
			}
		}
	}

}
