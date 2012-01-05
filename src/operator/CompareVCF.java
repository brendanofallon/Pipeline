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
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantRec;

import pipeline.Pipeline;

public class CompareVCF extends IOOperator {

	protected AbstractVariantPool variantsA = new AbstractVariantPool();
	protected AbstractVariantPool variantsB = new AbstractVariantPool();
	
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
	
	private int buildVariantMap(VCFFile file, AbstractVariantPool vars) throws IOException {
		//BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
		VCFLineParser vParser = new VCFLineParser(file.getFile());
		int totalVarsCounted = 0;
		//skip initial comments, etc
		
		while(vParser.advanceLine()) {
			vars.addRecord( vParser.toVariantRec() );
//			String contig = vParser.getContig();
//			int pos = vParser.getPosition();
//			double qual = vParser.getQuality();
//			boolean het = vParser.isHetero();
//			//int depth = vParser.
//			//double readDepth = parseValue(line, "DP");
//			
//			
//			Map<Integer, VariantRecord> contigMap = map.get(contig);
//			if (contigMap == null) {
//				contigMap = new HashMap<Integer, VariantRecord>();
//				map.put(contig, contigMap);
//			}
//			VariantRecord rec = new VariantRecord();
//			rec.quality = qual;
//			rec.hetero = het;
//			contigMap.put(pos, rec);
//			totalVarsCounted++;
		}
		return totalVarsCounted;
	}

//	protected AbstractVariantPool findIntersectionContig(VariantPool varA,  VariantPool varB) {
//		Map<Integer, VariantRecord> inter = new HashMap<Integer, VariantRecord>();
//		for(Integer pos : varA.keySet()) {
//			VariantRecord recB = varB.get(pos);
//			if (recB != null) {
//				VariantRecord recA = varB.get(pos);
//				VariantRecord intRec = new VariantRecord();
//				intRec.quality = recA.quality;
//				intRec.qualityB = recB.quality;
//				inter.put(pos, intRec);
//			}
//		}
//		return inter;
//	}

	
//	protected Map<String, Map<Integer, VariantRecord>> findIntersection(Map<String, Map<Integer, VariantRecord>> varA, Map<String, Map<Integer, VariantRecord>> varB) {
//		Map<String, Map<Integer, VariantRecord>> allIntersection = new HashMap<String, Map<Integer, VariantRecord>>();
//		Set<String> contigsA = varA.keySet();
//		for(String contigA : contigsA) {
//			Map<Integer, VariantRecord> varsA = varA.get(contigA);
//			Map<Integer, VariantRecord> varsB = varB.get(contigA);
//			
//			if (varsB == null) {
//				System.out.println("WARNING : Contig '" + contigA + "' found in first variant file but not second!");
//			}
//			else {
//				Map<Integer, VariantRecord> inter = findIntersectionContig(varsA, varsB);
//				allIntersection.put(contigA, inter);
//			}
//		}
//		return allIntersection;
//	}
	
	/**
	 * Produce a new map that is contains only the variants in the first set, but none of the variants in the second set
	 * @param vars
	 * @param toRemove
	 * @return
	 */
//	private Map<String, Map<Integer, VariantRecord>> removeFrom(
//			Map<String, Map<Integer, VariantRecord>> vars,
//			Map<String, Map<Integer, VariantRecord>> toRemove) {
//		
//		Map<String, Map<Integer, VariantRecord>> uniq = new HashMap<String, Map<Integer, VariantRecord>>();
//		Set<String> contigsA = vars.keySet();
//		for(String contigA : contigsA) {
//			Map<Integer, VariantRecord> varsContig = vars.get(contigA);
//			Map<Integer, VariantRecord> removeContig = toRemove.get(contigA);
//			
//			if (removeContig == null) {
//				System.out.println("WARNING : Contig '" + contigA + "' found in first variant file but not second!");
//			}
//			else {
//				Map<Integer, VariantRecord> inter = removeByContig(varsContig, removeContig);
//				uniq.put(contigA, inter);
//			}
//		}
//		
//		return uniq;
//	}
	
	/**
	 * Returns a new contig map that contains only those variants from the first contig but none from the second 
	 * @param varsContig
	 * @param removeContig
	 * @return
	 */
//	private Map<Integer, VariantRecord> removeByContig(
//			Map<Integer, VariantRecord> varsContig,
//			Map<Integer, VariantRecord> removeContig) {
//		Map<Integer, VariantRecord> uniC = new HashMap<Integer, VariantRecord>();
//		//I'm guess this does't clone the variantrecords.. so changes in the variant records
//		//in the new map will be reflected in the old map
//		uniC.putAll(varsContig);
//		
//		for(Integer pos : removeContig.keySet()) {
//			uniC.remove(pos);
//		}
//		return uniC;
//	}

	/**
	 * Returns average of quality scores across all variants in set
	 * @param vars
	 * @return
	 */
	public static double meanQuality(AbstractVariantPool vars) {
		double sum = 0;
		double count = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec rec : vars.getVariantsForContig(contig)) {
				sum += rec.getQuality();
				count++;
			}
		}
		
		return sum/count;
	}
//	
//	private static double sumQuality(Collection<VariantRec> recs) {
//		double sum = 0;
//		for(VariantRec rec : recs) {
//			sum += rec.getQuality();
//		}
//		return sum;
//	}
	
//	public static void emitToTable(Map<String, Map<Integer, VariantRecord>> vars) {
//		PrintStream out = System.out;
//		
//		out.println("CHR\tPOS\tQUAL\tDP\tAF");
//		List<String> contigs = new ArrayList<String>();
//		contigs.addAll(vars.keySet());
//		Collections.sort(contigs);
//		for(String contig: contigs) {
//			Map<Integer, VariantRecord> varC = vars.get(contig);
//			List<Integer> sites = new ArrayList<Integer>(1000);
//			sites.addAll(varC.keySet());
//			Collections.sort(sites);
//			for(Integer pos : sites) {
//				VariantRecord rec = varC.get(pos);
//				out.println(contig + "\t" + pos + "\t" + rec.quality + "\t" + rec.hetero );
//			}
//			
//		}
//	}
	
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
	
	

//	private int countVariants(Map<String, Map<Integer, VariantRecord>> recs) {
//		int count = 0;
//		for(String contig : recs.keySet()) {
//			Collection<VariantRecord> varRecs = recs.get(contig).values();
//			count += varRecs.size();
//			
//		}
//		
//		return count;
//	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		FileBuffer fileA = inputBuffers.get(0);
		FileBuffer fileB = inputBuffers.get(1);
		DecimalFormat formatter = new DecimalFormat("#0.00");
		
		try {
			buildVariantMap( (VCFFile)fileA, variantsA);
			buildVariantMap( (VCFFile)fileB, variantsB);
			
			System.out.println("Total variants in " + fileA.getFile().getName() + " : " + variantsA.size());
			System.out.println("Total variants in " + fileB.getFile().getName() + " : " + variantsB.size());
			
			AbstractVariantPool intersection = (AbstractVariantPool) variantsA.intersect(variantsB);
	
			
			AbstractVariantPool uniqA = new AbstractVariantPool(variantsA);
			uniqA.removeVariants(intersection);
			AbstractVariantPool uniqB = new AbstractVariantPool(variantsB);
			uniqB.removeVariants(intersection);
			
			
			int hetsA = variantsA.countHeteros();
			int hetsB = variantsB.countHeteros();
			System.out.println("Heterozyotes in " + fileA.getFilename() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)variantsA.size()) + " % )");
			System.out.println("Heterozyotes in " + fileB.getFilename() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)variantsB.size()) + " % )");
			

			System.out.println("Total intersection size: " + intersection.size());
			System.out.println("%Intersection in " + fileA.getFile().getName() + " : " + formatter.format( (double)intersection.size() / (double)variantsA.size()));
			System.out.println("%Intersection in " + fileB.getFile().getName() + " : " + formatter.format( (double)intersection.size() / (double)variantsB.size()));
			
			
			System.out.println("Mean quality of sites in intersection: " + formatter.format(meanQuality(intersection)));
			System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(meanQuality(uniqA)));
			System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(meanQuality(uniqB)));
			

			int uniqAHets = uniqA.countHeteros();
			int uniqBHets = uniqB.countHeteros();
			System.out.println("Number of hets in discordant A sites: " + uniqAHets +  " ( " + formatter.format(100.0*(double)uniqAHets/(double)uniqA.size()) + " % )");
			System.out.println("Number of hets in discordant A sites: " + uniqBHets +  " ( " + formatter.format(100.0*(double)uniqBHets/(double)uniqB.size()) + " % )");
//			System.out.println("Sites unique to " + fileA.getFilename());
//			emitToTable(uniqA);
//			System.out.println("Sites unique to " + fileB.getFilename());
//			emitToTable(uniqB);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
