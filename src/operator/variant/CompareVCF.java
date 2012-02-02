package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

import operator.IOOperator;
import operator.OperationFailedException;

import buffer.FileBuffer;
import buffer.VCFFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantRec;

import pipeline.Pipeline;
import util.VCFLineParser;

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
		}
		return totalVarsCounted;
	}



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

		//	System.out.println("\n\n Sites unique to " + fileA.getFilename());
			uniqA.listAll(new PrintStream(new FileOutputStream("unique_to_" + fileA.getFilename())));
		//	System.out.println("\n\nSites unique to " + fileB.getFilename());
			uniqB.listAll(new PrintStream(new FileOutputStream("unique_to_" + fileB.getFilename())));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
}
