package operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import buffer.FileBuffer;
import buffer.VCFFile;

import pipeline.Pipeline;

public class CompareVCF extends IOOperator {

	protected Map<String, Map<Integer, VariantRecord>> variantsA = new HashMap<String, Map<Integer, VariantRecord>>();
	protected Map<String, Map<Integer, VariantRecord>> variantsB = new HashMap<String, Map<Integer, VariantRecord>>();
	
	public static Double parseValue(String line, String key) {
		if (! key.endsWith("="))
			key = key + "=";
		int index = line.indexOf(key);
		if (index < 0)
			return null;
		int startIndex = index + key.length();
		int i = startIndex;
		Character c = line.charAt(i);
		while ( Character.isDigit(c)) {
			i++;
			c = line.charAt(i);
		}
		String digStr = line.substring(startIndex, i);
		try {
			Double val = Double.parseDouble(digStr);
			return val;				
		}
		catch (NumberFormatException nfe) {
			System.err.println("Could not parse a value for key: " + key + ", got string: " + digStr);
			return null;
		}
	}
	
	private int buildVariantMap(VCFFile file, Map<String, Map<Integer, VariantRecord>> map) throws IOException {
		//BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
		VCFLineParser vParser = new VCFLineParser(file.getFile());
		int totalVarsCounted = 0;
		//skip initial comments, etc
		
		while(vParser.advanceLine()) {
			String contig = vParser.getContig();
			int pos = vParser.getPosition();
			double qual = vParser.getQuality();
			boolean het = vParser.isHetero();
			//int depth = vParser.
			//double readDepth = parseValue(line, "DP");
			
			
			Map<Integer, VariantRecord> contigMap = map.get(contig);
			if (contigMap == null) {
				contigMap = new HashMap<Integer, VariantRecord>();
				map.put(contig, contigMap);
			}
			VariantRecord rec = new VariantRecord();
			rec.quality = qual;
			rec.hetero = het;
			contigMap.put(pos, rec);
			totalVarsCounted++;
		}
		return totalVarsCounted;
	}

	protected  Map<Integer, VariantRecord> findIntersectionContig(Map<Integer, VariantRecord> varA,  Map<Integer, VariantRecord> varB) {
		Map<Integer, VariantRecord> inter = new HashMap<Integer, VariantRecord>();
		for(Integer pos : varA.keySet()) {
			VariantRecord recB = varB.get(pos);
			if (recB != null) {
				VariantRecord recA = varB.get(pos);
				VariantRecord intRec = new VariantRecord();
				intRec.quality = recA.quality;
				intRec.qualityB = recB.quality;
				inter.put(pos, intRec);
			}
		}
		return inter;
	}

	
	protected Map<String, Map<Integer, VariantRecord>> findIntersection(Map<String, Map<Integer, VariantRecord>> varA, Map<String, Map<Integer, VariantRecord>> varB) {
		Map<String, Map<Integer, VariantRecord>> allIntersection = new HashMap<String, Map<Integer, VariantRecord>>();
		Set<String> contigsA = varA.keySet();
		for(String contigA : contigsA) {
			Map<Integer, VariantRecord> varsA = varA.get(contigA);
			Map<Integer, VariantRecord> varsB = varB.get(contigA);
			
			if (varsB == null) {
				System.out.println("WARNING : Contig '" + contigA + "' found in first variant file but not second!");
			}
			else {
				Map<Integer, VariantRecord> inter = findIntersectionContig(varsA, varsB);
				allIntersection.put(contigA, inter);
			}
		}
		return allIntersection;
	}
	
	/**
	 * Produce a new map that is contains only the variants in the first set, but none of the variants in the second set
	 * @param vars
	 * @param toRemove
	 * @return
	 */
	private Map<String, Map<Integer, VariantRecord>> removeFrom(
			Map<String, Map<Integer, VariantRecord>> vars,
			Map<String, Map<Integer, VariantRecord>> toRemove) {
		
		Map<String, Map<Integer, VariantRecord>> uniq = new HashMap<String, Map<Integer, VariantRecord>>();
		Set<String> contigsA = vars.keySet();
		for(String contigA : contigsA) {
			Map<Integer, VariantRecord> varsContig = vars.get(contigA);
			Map<Integer, VariantRecord> removeContig = toRemove.get(contigA);
			
			if (removeContig == null) {
				System.out.println("WARNING : Contig '" + contigA + "' found in first variant file but not second!");
			}
			else {
				Map<Integer, VariantRecord> inter = removeByContig(varsContig, removeContig);
				uniq.put(contigA, inter);
			}
		}
		
		return uniq;
	}
	
	/**
	 * Returns a new contig map that contains only those variants from the first contig but none from the second 
	 * @param varsContig
	 * @param removeContig
	 * @return
	 */
	private Map<Integer, VariantRecord> removeByContig(
			Map<Integer, VariantRecord> varsContig,
			Map<Integer, VariantRecord> removeContig) {
		Map<Integer, VariantRecord> uniC = new HashMap<Integer, VariantRecord>();
		//I'm guess this does't clone the variantrecords.. so changes in the variant records
		//in the new map will be reflected in the old map
		uniC.putAll(varsContig);
		
		for(Integer pos : removeContig.keySet()) {
			uniC.remove(pos);
		}
		return uniC;
	}

	/**
	 * Returns average of quality scores across all variants in set
	 * @param vars
	 * @return
	 */
	public static double meanQuality(Map<String, Map<Integer, VariantRecord>> vars) {
		double sum = 0;
		double count = 0;
		for(String contig : vars.keySet()) {
			sum += sumQuality( vars.get(contig).values() );
			count += vars.get(contig).size();
		}
		
		return sum/count;
	}
	
	private static double sumQuality(Collection<VariantRecord> recs) {
		double sum = 0;
		for(VariantRecord rec : recs) {
			sum += rec.quality;
		}
		return sum;
	}
	
	public static void emitToTable(Map<String, Map<Integer, VariantRecord>> vars) {
		PrintStream out = System.out;
		
		out.println("CHR\tPOS\tQUAL\tDP\tAF");
		List<String> contigs = new ArrayList<String>();
		contigs.addAll(vars.keySet());
		Collections.sort(contigs);
		for(String contig: contigs) {
			Map<Integer, VariantRecord> varC = vars.get(contig);
			List<Integer> sites = new ArrayList<Integer>(1000);
			sites.addAll(varC.keySet());
			Collections.sort(sites);
			for(Integer pos : sites) {
				VariantRecord rec = varC.get(pos);
				out.println(contig + "\t" + pos + "\t" + rec.quality + "\t" + rec.hetero );
			}
			
		}
	}
	
	/**
	 * Use a VCFLineParser to count the number of heterozygotes in this VCF file
	 * @param file
	 * @return
	 */
	private int countHets(File file) {
		int count = 0;
		try {
			VCFLineParser vp = new VCFLineParser(file);
			while(vp.advanceLine()) {
				if (vp.isHetero()) 
					count++;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return count;
	}
	
	private int countHets(Map<String, Map<Integer, VariantRecord>> recs) {
		int count = 0;
		for(String contig : recs.keySet()) {
			Collection<VariantRecord> varRecs = recs.get(contig).values();
			for(VariantRecord rec : varRecs) {
				if (rec.hetero) 
					count++;
			}
		}
		
		return count;
	}

	private int countVariants(Map<String, Map<Integer, VariantRecord>> recs) {
		int count = 0;
		for(String contig : recs.keySet()) {
			Collection<VariantRecord> varRecs = recs.get(contig).values();
			count += varRecs.size();
			
		}
		
		return count;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		FileBuffer fileA = inputBuffers.get(0);
		FileBuffer fileB = inputBuffers.get(1);
		DecimalFormat formatter = new DecimalFormat("#0.00");
		
		try {
			int totalA = buildVariantMap( (VCFFile)fileA, variantsA);
			int totalB = buildVariantMap( (VCFFile)fileB, variantsB);
			
			System.out.println("Total variants in " + fileA.getFile().getName() + " : " + totalA);
			System.out.println("Total variants in " + fileB.getFile().getName() + " : " + totalB);
			
			Map<String, Map<Integer, VariantRecord>> intersection = findIntersection(variantsA, variantsB);
			
			int intersectionSize = 0;
			List<String> contigs = new ArrayList<String>();
			contigs.addAll(intersection.keySet());
			Collections.sort(contigs);
			for(String contig : contigs) {
				Map<Integer, VariantRecord> contigInt = intersection.get(contig);
				Map<Integer, VariantRecord> varsA = variantsA.get(contig);
				Map<Integer, VariantRecord> varsB = variantsB.get(contig);

				intersectionSize += contigInt.size();
				int uniqueA = varsA.size() - contigInt.size();
				int uniqueB = varsB.size() - contigInt.size();
				System.out.println("Contig: " + contig + "\t" + uniqueA + "\t" + contigInt.size() + "\t" + uniqueB);
			}
			
			Map<String, Map<Integer, VariantRecord>> uniqA = removeFrom(variantsA, intersection);
			Map<String, Map<Integer, VariantRecord>> uniqB = removeFrom(variantsB, intersection);
			
			int hetsA = countHets(variantsA);
			int hetsB = countHets(variantsB);
			System.out.println("Heterozyotes in " + fileA.getFilename() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)countVariants(variantsA)) + " % )");
			System.out.println("Heterozyotes in " + fileB.getFilename() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)countVariants(variantsB)) + " % )");
			

			System.out.println("Total intersection size: " + intersectionSize);
			System.out.println("%Intersection in " + fileA.getFile().getName() + " : " + formatter.format( intersectionSize / (double)totalA));
			System.out.println("%Intersection in " + fileB.getFile().getName() + " : " + formatter.format( intersectionSize / (double)totalB));
			
			
			System.out.println("Mean quality of sites in intersection: " + formatter.format(meanQuality(intersection)));
			System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(meanQuality(uniqA)));
			System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(meanQuality(uniqB)));
			

			int uniqAHets = countHets(uniqA);
			int uniqBHets = countHets(uniqB);
			System.out.println("Number of hets in discordant A sites: " + uniqAHets +  " ( " + formatter.format(100.0*(double)uniqAHets/(double)countVariants(uniqA)) + " % )");
			System.out.println("Number of hets in discordant A sites: " + uniqBHets +  " ( " + formatter.format(100.0*(double)uniqBHets/(double)countVariants(uniqB)) + " % )");
//			System.out.println("Sites unique to " + fileA.getFilename());
//			emitToTable(uniqA);
//			System.out.println("Sites unique to " + fileB.getFilename());
//			emitToTable(uniqB);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	public class VariantRecord {
		Double quality = -1.0;
		Double qualityB = -1.0;
		Boolean hetero = null;
	}
	
}
