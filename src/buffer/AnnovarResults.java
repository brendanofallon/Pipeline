package buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import buffer.variant.FileAnnotator;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import pipeline.Pipeline;

public class AnnovarResults implements VariantPool {

	private final File variantFuncFile;
	private final File exonicFuncFile;
	private final File avsiftFile;
	private final File polyphenFile;
	private final File mtFile;
	private final File tkgFile;
	
	private Map<String, List<VariantRec>>  vars = new HashMap<String, List<VariantRec>>();
	
	public AnnovarResults(File variantFuncFile, File exonicFuncFile, File avsiftFile, File polyphenFile, File mtFile, File tkgFile) throws IOException {
		this.variantFuncFile = variantFuncFile;
		this.exonicFuncFile = exonicFuncFile;
		this.avsiftFile = avsiftFile;
		this.polyphenFile = polyphenFile;
		this.mtFile = mtFile;
		this.tkgFile = tkgFile;
		
		//Otherwise mysterious error regarding general contract violation crops up during sorting procedure
		System.setProperty("java.util.Arrays.useLegacyMergeSort", "true");
		
		buildLists();
	}
	
	/**
	 * Traverse the files and add variant functions/ sift scores/ pp scores etc to the 
	 * variant records list. 
	 * @throws IOException
	 */
	private void buildLists() throws IOException {
		//First populate the vars record list
		BufferedReader reader = new BufferedReader(new FileReader(variantFuncFile));
		String line = reader.readLine();
		while(line != null) {
			VariantRec rec = buildRecord(line);
			String contig = rec.getContig();
			List<VariantRec> varList = vars.get(contig);
			if (varList == null) {
				varList = new ArrayList<VariantRec>(512);
				vars.put(contig, varList);
			}
				
			varList.add(rec);
			line = reader.readLine();
			
		}
		reader.close();
		
		//Sort all records within each contig
		for(String contig : vars.keySet()) {
			List<VariantRec> list = vars.get(contig);
			Collections.sort(list, VariantRec.getPositionComparator());
		}
		
		//Add exonic variants functions to records where applicable 
		reader = new BufferedReader(new FileReader(exonicFuncFile));
		line = reader.readLine();
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\t");
				String exonicFunc = toks[1];
				String contig = toks[3];
				int pos = Integer.parseInt( toks[4] );
				
				VariantRec rec = findRecord(contig, pos);
				if (rec != null)
					rec.addAnnotation(VariantRec.EXON_FUNCTION, exonicFunc);
			}
			line = reader.readLine();
		}
		reader.close();
		
		//Add some annotations/ properties to the variant records  
		FileAnnotator siftAnnotator = new FileAnnotator(avsiftFile, VariantRec.SIFT_SCORE, 1, this);
		siftAnnotator.annotateAll();
		
		FileAnnotator polyphenAnnotator = new FileAnnotator(polyphenFile, VariantRec.POLYPHEN_SCORE, 1, this);
		polyphenAnnotator.annotateAll();
				
		FileAnnotator mtAnnotator = new FileAnnotator(mtFile, VariantRec.MT_SCORE, 1, this);
		mtAnnotator.annotateAll();
		
		FileAnnotator freqAnnotator = new FileAnnotator(tkgFile, VariantRec.POP_FREQUENCY, 1, this);
		freqAnnotator.annotateAll();

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
	 * Create a variant record by parsing values from the given line. The line is assumed
	 * to be in basic annovar format. 
	 * @param line
	 * @return
	 */
	private VariantRec buildRecord(String line) {		
		String[] toks = line.split("\\s");
		String variantType = toks[0];
		String gene = toks[1];
		String contig = toks[2];
		int start = Integer.parseInt(toks[3]);
		int end = Integer.parseInt(toks[4]);
		char ref = toks[5].charAt(0);
		char alt = toks[6].charAt(0);
		boolean isHet = toks[7].contains("het");
		double qual = Double.parseDouble(toks[8]);
		
		VariantRec rec = new VariantRec(contig, start, end, ref, alt, qual, variantType.contains("exonic"), isHet);
		rec.addAnnotation(VariantRec.GENE_NAME, gene);
		rec.addAnnotation(VariantRec.VARIANT_TYPE, variantType);
		return rec;
	}
	
	/**
	 * Populate the variant record list with sift ranking information
	 */
	public void compareRanks(PrintStream out) {
		List<VariantRec> siftRanked = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (rec.hasProperty(VariantRec.SIFT_SCORE))
					siftRanked.add( rec );
			}
		}
		
				
		Collections.sort(siftRanked, VariantRec.getPropertyComparator(VariantRec.SIFT_SCORE));
		Collections.reverse(siftRanked); //
		for(int i=0; i<siftRanked.size(); i++) {
			VariantRec rec = siftRanked.get(i);
			//rec.siftRank = i;
			rec.addProperty(VariantRec.SIFT_QUARTILE, Math.floor(4.0 * (double)i / (double)siftRanked.size()));
		}
		
		
		List<VariantRec> polyphenRanked = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (rec.hasProperty(VariantRec.POLYPHEN_SCORE))
					polyphenRanked.add( rec );
			}
		}
		
		Collections.sort(polyphenRanked, VariantRec.getPropertyComparator(VariantRec.POLYPHEN_SCORE));
		for(int i=0; i<polyphenRanked.size(); i++) {
			VariantRec rec = polyphenRanked.get(i);
			//rec.polyphenRank = i;
			rec.addProperty(VariantRec.POLYPHEN_QUARTILE, Math.floor(4.0 * (double)i / (double)polyphenRanked.size()));

		}
		
		List<VariantRec> mtRanked = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (rec.hasProperty(VariantRec.MT_SCORE))
					mtRanked.add( rec );
			}
		}
		
		Collections.sort(mtRanked, VariantRec.getPropertyComparator(VariantRec.MT_SCORE));
		for(int i=0; i<mtRanked.size(); i++) {
			VariantRec rec = mtRanked.get(i);
			//rec.mtRank = i;
			rec.addProperty(VariantRec.MT_QUARTILE, Math.floor(4.0 * (double)i / (double)mtRanked.size()));
		}
		
//		out.println("Top 100 siftranked vars");
//		for(int i=0; i<Math.min(siftRanked.size(), 100); i++) {
//			out.println( siftRanked.get(i) );
//		}
		
		//out.println("Vars in top quartile of all methods: ");
		//out.println("contig \t start \t end \t gene \t variantType \t exonicType \t qual \t siftScore \t siftRank \t siftQuartile \t polyphenScore \t polyphenRank \t polyphenQuartile \t mtScore \t mtRank \t mtQuartile");
		out.println("contig \t start \t end \t variant.type \t exon.func \t pop.freq \t het \t qual \t sift \t polyphen \t mt");  

		for(VariantRec rec : siftRanked) {
			if (rec.hasProperty(VariantRec.SIFT_QUARTILE) && rec.hasProperty(VariantRec.POLYPHEN_QUARTILE) && rec.hasProperty(VariantRec.MT_QUARTILE)) {
				Double siftQ = rec.getProperty(VariantRec.SIFT_QUARTILE);
				Double ppQ = rec.getProperty(VariantRec.POLYPHEN_QUARTILE);
				Double mtQ = rec.getProperty(VariantRec.MT_QUARTILE);
				if (siftQ == 0 && ppQ == 0 && mtQ == 0) {
					out.println(rec);
				}
			}
		}
		
	}
	
	public void emitNonsynonymousVars(PrintStream out) {
		out.println("contig \t start \t end \t gene \t zyg \t freq \t sift \t polyphen \t mutTaster");
		for(String contig : vars.keySet()) {
			out.println("Contig: " + contig);
			List<VariantRec> varList = vars.get(contig);
			for(VariantRec rec : varList) {
				String exFunction = rec.getAnnotation(VariantRec.EXON_FUNCTION);
				if (exFunction != null && (exFunction.contains("nonsynonymous") || exFunction.contains("frameshift"))) {
					String het = "het";
					if (! rec.isHetero()) {
						het = "hom";
					}
					out.println(rec);
				}
			}
		}
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
	
}
