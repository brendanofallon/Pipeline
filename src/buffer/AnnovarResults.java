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

import buffer.variant.AbstractVariantPool;
import buffer.variant.FileAnnotator;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import pipeline.Pipeline;

/**
 * AnnovarResults is a collection of variants that are annotated using some tools in annovar. 
 * Its main functions are to build a list of VariantRec's from an annovar-output file, and to add
 * some additional annotations to those records based on some annovar filtering tools (like sift & polyphen
 * scores). 
 * 
 * @author brendan
 *
 */
public class AnnovarResults extends AbstractVariantPool {

	private final File variantFuncFile;
	private final File exonicFuncFile;
	private final File avsiftFile;
	private final File polyphenFile;
	private final File mtFile;
	private final File tkgFile;
	private final File phyloPFile;
	
	
	
	public AnnovarResults(File variantFuncFile, 
						File exonicFuncFile, 
						File avsiftFile, 
						File polyphenFile, 
						File mtFile, 
						File tkgFile, 
						File phyloPFile) throws IOException {
		this.variantFuncFile = variantFuncFile;
		this.exonicFuncFile = exonicFuncFile;
		this.avsiftFile = avsiftFile;
		this.polyphenFile = polyphenFile;
		this.mtFile = mtFile;
		this.tkgFile = tkgFile;
		this.phyloPFile = phyloPFile;
		
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
			if (rec != null) {
				String contig = rec.getContig();
				List<VariantRec> varList = vars.get(contig);
				if (varList == null) {
					varList = new ArrayList<VariantRec>(512);
					vars.put(contig, varList);
				}

				varList.add(rec);
			}
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
			if (line.length()>1) {
				String[] toks = line.split("\\t");
				String exonicFunc = toks[1];
				
				String NM = "NA";
				String exonNum = "NA";
				String cDot = "NA";
				String pDot = "NA";
				
				String[] details = toks[2].split(":");
				if (details.length>4) {
					NM = details[1];
					exonNum = details[2];
					cDot = details[3];
					pDot = details[4];
					int idx = pDot.indexOf(",");
					if (idx > 0)
						pDot = pDot.substring(0, idx);
				}
				
				String contig = toks[3];
				int pos = Integer.parseInt( toks[4] );
				
				VariantRec rec = findRecord(contig, pos);
				if (rec != null)
					rec.addAnnotation(VariantRec.EXON_FUNCTION, exonicFunc);
					rec.addAnnotation(VariantRec.NM_NUMBER, NM);
					rec.addAnnotation(VariantRec.CDOT, cDot);
					rec.addAnnotation(VariantRec.PDOT, pDot);
			}
			line = reader.readLine();
		}
		reader.close();
		
		//Add some annotations/ properties to the variant records  
//		FileAnnotator siftAnnotator = new FileAnnotator(avsiftFile, VariantRec.SIFT_SCORE, 1, this);
//		siftAnnotator.annotateAll();
//		
//		FileAnnotator polyphenAnnotator = new FileAnnotator(polyphenFile, VariantRec.POLYPHEN_SCORE, 1, this);
//		polyphenAnnotator.annotateAll();
//				
//		FileAnnotator mtAnnotator = new FileAnnotator(mtFile, VariantRec.MT_SCORE, 1, this);
//		mtAnnotator.annotateAll();
//		
//		FileAnnotator freqAnnotator = new FileAnnotator(tkgFile, VariantRec.POP_FREQUENCY, 1, this);
//		freqAnnotator.annotateAll();
//		
//		FileAnnotator phyloPAnnotator = new FileAnnotator(phyloPFile, VariantRec.PHYLOP_SCORE, 1, this);
//		phyloPAnnotator.annotateAll();

	}
	

	
	/**
	 * Create a variant record by parsing values from the given line. The line is assumed
	 * to be in basic annovar format. 
	 * @param line
	 * @return
	 */
	private VariantRec buildRecord(String line) {		
		String[] toks = line.split("\\t");
		String variantType = toks[0];
		String gene = toks[1];
		String contig = toks[2];
		
		try {
			int start = Integer.parseInt(toks[3]);
			int end = Integer.parseInt(toks[4]);
			String ref = toks[5];
			String alt = toks[6];
			
			//Sometimes there are two 
			int indexOffset = 0;
			if (toks[7].length()<2)
				indexOffset=1;
			boolean isHet = toks[7+indexOffset].contains("het");
			double qual = Double.parseDouble(toks[8+indexOffset]);

			VariantRec rec = new VariantRec(contig, start, end, ref, alt, qual, variantType.contains("exonic"), isHet);
			rec.addAnnotation(VariantRec.GENE_NAME, gene);
			rec.addAnnotation(VariantRec.VARIANT_TYPE, variantType);
			return rec;
		}
		catch(NumberFormatException nfe) {
			System.out.println("Error parsing record from annovar line: " + line);
			System.out.println("Tokens: " );
			for(int i=0; i<toks.length; i++) {
				System.out.println(i + " : " + toks[i]);
			}
			return null;
		}
	}
	
	/**
	 * Populate the variant records with quartile information. Right now we do
	 * this for sift scores, polyphen scores, and mutation taster scores. The quartiles are
	 * determined by ordering all variants for which scores are available and dividing into four equal-
	 * sized pieces.  
	 */
	public void addQuartileInfo() {
		
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
			rec.addProperty(VariantRec.MT_QUARTILE, Math.floor(4.0 * (double)i / (double)mtRanked.size()));
		}
				
		
		
		List<VariantRec> phyloPRanked = new ArrayList<VariantRec>(1024);
		for(String contig : vars.keySet()) {
			for(VariantRec rec : vars.get(contig)) {
				if (rec.hasProperty(VariantRec.PHYLOP_SCORE))
					phyloPRanked.add( rec );
			}
		}
		
		Collections.sort(phyloPRanked, VariantRec.getPropertyComparator(VariantRec.PHYLOP_SCORE));
		for(int i=0; i<phyloPRanked.size(); i++) {
			VariantRec rec = phyloPRanked.get(i);
			rec.addProperty(VariantRec.PHYLOP_QUARTILE, Math.floor(4.0 * (double)i / (double)phyloPRanked.size()));
		}
		
		//Make a nicely-formatted little summary of the ranked variants
		StringBuffer rankingSummary = new StringBuffer();
		double siftMidpoint = siftRanked.get(siftRanked.size()/2).getProperty(VariantRec.SIFT_SCORE);
		double polyphenMidpoint = polyphenRanked.get(polyphenRanked.size()/2).getProperty(VariantRec.POLYPHEN_SCORE);
		double mtMidpoint = mtRanked.get(mtRanked.size()/2).getProperty(VariantRec.MT_SCORE);
		double phylopMidpoint = phyloPRanked.get(phyloPRanked.size()/2).getProperty(VariantRec.PHYLOP_SCORE);

		rankingSummary.append("Ranking summary: \n");
		rankingSummary.append("SIFT variants total \t: " + siftRanked.size() + " midpoint: " + siftMidpoint + "\n");
		rankingSummary.append("Polyphen variants total\t: " + polyphenRanked.size() + " midpoint: " + polyphenMidpoint + "\n");
		rankingSummary.append("MT variants total \t: " + mtRanked.size() + " midpoint: " + mtMidpoint + "\n");
		rankingSummary.append("PhyloP variants total\t: " + phyloPRanked.size() + " midpoint: " + phylopMidpoint + "\n");
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(rankingSummary.toString());
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


	
}
