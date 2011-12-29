package buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;

public class AnnovarResults {

	private final File variantFuncFile;
	private final File exonicFuncFile;
	private final File avsiftFile;
	private final File polyphenFile;
//	private final File phyloPFile;
//	private final File gerpFile;
	
	private Map<String, List<VariantRec>>  vars = new HashMap<String, List<VariantRec>>();
	
	public AnnovarResults(File variantFuncFile, File exonicFuncFile, File avsiftFile, File polyphenFile) throws IOException {
		this.variantFuncFile = variantFuncFile;
		this.exonicFuncFile = exonicFuncFile;
		this.avsiftFile = avsiftFile;
		this.polyphenFile = polyphenFile;
		
		buildLists();
	}
	
	private void buildLists() throws IOException {
		//First populate the vars record list
		BufferedReader reader = new BufferedReader(new FileReader(variantFuncFile));
		String line = reader.readLine();
		while(line != null) {
			VariantRec rec = buildRecord(line);
			String contig = rec.contig;
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
			Collections.sort(list);
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
					rec.exonicType = exonicFunc;
			}
			line = reader.readLine();
		}
		reader.close();
		
		//Add sift scores to variant records 
		reader = new BufferedReader(new FileReader(avsiftFile));
		line = reader.readLine();
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\s");
				double siftScore = Double.parseDouble(toks[1]);
				String contig = toks[2];
				int pos = Integer.parseInt( toks[3] );
				
				VariantRec rec = findRecord(contig, pos);
				if (rec != null)
					rec.siftScore = siftScore;
			}
			line = reader.readLine();
		}
		reader.close();
		
		//Add polyphen scores to variant records 
		reader = new BufferedReader(new FileReader(polyphenFile));
		line = reader.readLine();
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\s");
				double siftScore = Double.parseDouble(toks[1]);
				String contig = toks[2];
				int pos = Integer.parseInt( toks[3] );
				
				VariantRec rec = findRecord(contig, pos);
				if (rec != null)
					rec.polyphenScore = siftScore;
			}
			line = reader.readLine();
		}
		
		//Output records 
		for(String contig : vars.keySet()) {
			List<VariantRec> varList = vars.get(contig);
			for(VariantRec rec : varList) {
				System.out.println(rec);
			}
		}
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
		
		int index = Collections.binarySearch(varList, pos);
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
		VariantRec rec = new VariantRec();
		
		String[] toks = line.split("\\s");
		rec.variantType = toks[0];
		rec.gene = toks[1];
		rec.contig = toks[2];
		rec.start = Integer.parseInt(toks[3]);
		rec.end = Integer.parseInt(toks[4]);
		rec.ref = toks[5].charAt(0);
		rec.alt = toks[6].charAt(0);
		rec.isHet = toks[7].contains("het");
		rec.qual = Double.parseDouble(toks[8]);
		
		return rec;
	}
	
	
	class VariantRec implements Comparable<Object> {
		String contig;
		int start;
		int end;
		char ref;
		char alt;
		Double siftScore = Double.NaN;
		Double polyphenScore = Double.NaN;
		Double gerpScore = Double.NaN;
		Double phyloPScore = Double.NaN;
		Double qual;
		String variantType;
		String exonicType = "-";
		String gene;
		String nm;
		String exon;
		boolean isHet;

		@Override
		public int compareTo(Object obj) {
			if (obj instanceof VariantRec) {
				VariantRec vr = (VariantRec)obj;
				return start - vr.start;
			}
			if (obj instanceof Integer) {
				Integer pos = (Integer)obj;
				return start - pos;
			}
			return 0;
		}
		
		public String toString() {
			return contig + "\t" + start + "\t" + end + "\t" + variantType + "\t" + exonicType + "\t" + siftScore + "\t" + polyphenScore;
		}
	}
	
	public static void main(String[] args) {
		File varFunc = new File("/home/brendan/anno_test/annovartest.output.variant_function");
		File exvarFunc = new File("/home/brendan/anno_test/annovartest.output.exonic_variant_function");
		File siftFile = new File("/home/brendan/anno_test/annovartest.output.hg19_avsift_dropped");
		File polyphenFile = new File("/home/brendan/anno_test/annovartest.output.hg19_ljb_pp2_dropped");
		
		try {
			AnnovarResults rec = new AnnovarResults(varFunc, exvarFunc, siftFile, polyphenFile);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
