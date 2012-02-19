package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.SwingWorker;

import operator.variant.CompareVCF;
import operator.variant.CompoundHetFinder;

import buffer.BEDFile;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.GenePool;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantRec;

public class VarUtils {

	
	public static void emitUsage() {
		System.out.println("\tVariant Utils, v0.01 \n\t Brendan O'Fallon \n\t ARUP Labs");

		System.out.println(" java -jar varUtils.jar compare varsa.vcf varsb.vcf ");
		System.out.println("			Perform simple comparison of variants in both files");
		
		System.out.println(" java -jar varUtils.jar intersect varsa.vcf varsb.vcf [outputfile]");
		System.out.println("			Find intersection of two variant files");
		
		System.out.println(" java -jar varUtils.jar subtract varsa.vcf varsb.vcf [outputfile]");
		System.out.println("			Subtract variants in second list from first list");

		System.out.println(" java -jar varUtils.jar homFilter vars.vcf [outputfile]");
		System.out.println("			Write only homozygotes from given variant file");

		System.out.println(" java -jar varUtils.jar bedFilter vars.vcf bedFile.bed [outputFile]");
		System.out.println("			Write only variants that are in regions defined by given .bed file");

		System.out.println(" java -jar varUtils.jar hetsByContig vars.vcf");
		System.out.println("			Emit list of how many heterozygotes are in each contig");

		System.out.println(" java -jar varUtils.jar compoundHet kidVars.csv parent1.csv parent2.csv");
		System.out.println("			Write genes which have two heterozygotes in kid, with one from each parent");

	}
	
	
	
	public static void handleCompoundHet(String[] args) {
		if (! args[0].equals("compoundHet")) {
			throw new IllegalArgumentException("First arg must be compoundHet");
		}
		
		if (args.length != 4) {
			System.err.println("Please enter the names of variant files for kid, parent1, and parent2, in that order");
			return;
		}
		
		File kidVars = new File(args[1]);
		if (! kidVars.exists()) {
			System.err.println("Counld not find file " + args[1]);
			return;
		}
		
		File par1Vars = new File(args[2]);
		if (! kidVars.exists()) {
			System.err.println("Could not find file " + args[2]);
			return;
		}
		
		File par2Vars = new File(args[3]);
		if (! kidVars.exists()) {
			System.err.println("Could not find file " + args[3]);
			return;
		}
		
		
		try {
			VariantPool kidPool  = getPool(kidVars);
			VariantPool par1Pool = getPool(par1Vars);
			VariantPool par2Pool = getPool(par2Vars);
			
			CompoundHetFinder.computeCompoundHets(kidPool, par1Pool, par2Pool, System.out);
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
	}
	

	
	/**
	 * Obtain an AbstractVariantPool from a file
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static VariantPool getPool(File inputFile) throws IOException {
		VariantPool variants = null;
		if (inputFile.getName().endsWith(".csv")) {
			variants = new VariantPool(new CSVFile(inputFile));	
		} else {
			if (inputFile.getName().endsWith(".vcf")) {
				variants = new VariantPool(new VCFFile(inputFile));	
			}
			else {
				throw new IllegalArgumentException("Unrecognized file suffix for input file: " + inputFile.getName());
			}
		}
		return variants;
	}
	
	public static void main(String[] args) {
		
		//args = new String[]{"subtract", "/media/DATA/whitney_genome/52Kid2/contig_22.realigned.sorted.recal.vcf", "/media/DATA/whitney_genome/52Kid3/contig_22.realigned.sorted.recal.vcf"};
		
		if (args.length==0) {
			emitUsage();
			return;
		}
		
		String firstArg = args[0];
		
		if (firstArg.equals("compare")) {
			if (args.length != 3) {
				System.out.println("Enter the names of two variant (vcf or csv) files to compare");
				return;
			}
			
			try {
				File fileA = new File(args[1]);
				File fileB = new File(args[2]);
				DecimalFormat formatter = new DecimalFormat("#0.000");
				
				VariantPool varsA = getPool(new File(args[1]));
				VariantPool varsB = getPool(new File(args[2]));
				
				System.out.println("Total variants in " + fileA.getName() + " : " + varsA.size() + " mean quality: " + formatter.format(varsA.meanQuality()));
				System.out.println("Total variants in " + fileB.getName() + " : " + varsB.size() + " mean quality: " + formatter.format(varsB.meanQuality()));
				
				System.out.println("Transitions / Transversions in " + fileA.getName() + " : " + varsA.countTransitions() + " / " + varsA.countTransverions());
				System.out.println("Transitions / Transversions in " + fileB.getName() + " : " + varsB.countTransitions() + " / " + varsB.countTransverions());
				
				System.out.println("TT ratio for " + fileA.getName() + " : " + formatter.format(varsA.computeTTRatio()));
				System.out.println("TT ratio for " + fileB.getName() + " : " + formatter.format(varsB.computeTTRatio()));
				
				CompareVCF.compareVars(varsA, varsB, System.out);
				
				VariantPool intersection = (VariantPool) varsA.intersect(varsB);
				
				
				VariantPool uniqA = new VariantPool(varsA);
				uniqA.removeVariants(intersection);
				VariantPool uniqB = new VariantPool(varsB);
				uniqB.removeVariants(intersection);
				
				System.out.println("Number of variants unique to " + fileA.getName() + " : " + uniqA.size());
				System.out.println("Number of variants unique to " + fileB.getName() + " : " + uniqB.size());
				
				System.out.println("TT ratio in variants unique to " + fileA.getName() + " : " + uniqA.computeTTRatio());
				System.out.println("TT ratio in variants unique to " + fileB.getName() + " : " + uniqB.computeTTRatio());
				
				int hetsA = varsA.countHeteros();
				int hetsB = varsB.countHeteros();
				System.out.println("Heterozyotes in " + fileA.getName() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)varsA.size()) + " % )");
				System.out.println("Heterozyotes in " + fileB.getName() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)varsB.size()) + " % )");
				

				System.out.println("Total intersection size: " + intersection.size());
				System.out.println("%Intersection in " + fileA.getName() + " : " + formatter.format( (double)intersection.size() / (double)varsA.size()));
				System.out.println("%Intersection in " + fileB.getName() + " : " + formatter.format( (double)intersection.size() / (double)varsB.size()));
				
				
				System.out.println("Mean quality of sites in intersection: " + formatter.format(intersection.meanQuality()));
				System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(uniqA.meanQuality()));
				System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(uniqB.meanQuality()));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			return;
		}
		
		if (firstArg.equals("intersect")) {
			if (args.length < 3) {
				System.out.println("Enter the names of two variant (vcf or csv) files to intersect");
				return;
			}
			
			File outputFile = null;
			if (args.length==4) {
				outputFile = new File(args[3]);
			}
			
			try {
				VariantPool varsA = getPool(new File(args[1]));
				VariantPool varsB = getPool(new File(args[2]));
				
				VariantPool intersection = (VariantPool) varsA.intersect(varsB);
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				intersection.listAll(outputStream);
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		if (firstArg.equals("subtract")) {
			if (args.length < 3) {
				System.out.println("Enter the names of two variant (vcf or csv) files to subtract");
				return;
			}
			
			File outputFile = null;
			if (args.length==4) {
				outputFile = new File(args[3]);
			}
			
			try {
				VariantPool varsA = getPool(new File(args[1]));
				VariantPool varsB = getPool(new File(args[2]));
				
				varsA.removeVariants(varsB);
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				varsA.listAll(outputStream);
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}


		if (firstArg.equals("homFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file to filter");
				return;
			}
			
			try {
				VariantPool varsA = getPool(new File(args[1]));
				List<VariantRec> homos = varsA.filterPool(VarFilterUtils.getHomoFilter());
				File outputFile = null;
				if (args.length==3) {
					outputFile = new File(args[2]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				
				outputStream.println( VariantRec.getSimpleHeader() );
				for(VariantRec rec : homos) {
					outputStream.println(rec.toSimpleString());
				}
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		
		if (firstArg.equals("bedFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file and a bed file to filter by");
				return;
			}
			
			try {
				VariantPool varsA = getPool(new File(args[1]));
				BEDFile bedFile = new BEDFile(new File(args[2]));
				
				VariantPool filteredVars = varsA.filterByBED(bedFile);
				
				File outputFile = null;
				if (args.length==4) {
					outputFile = new File(args[3]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				
				filteredVars.listAll(outputStream);
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		if (firstArg.equals("hetFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file to filter");
				return;
			}
			
			try {
				VariantPool varsA = getPool(new File(args[1]));
				List<VariantRec> homos = varsA.filterPool(VarFilterUtils.getHeteroFilter());
				File outputFile = null;
				if (args.length==3) {
					outputFile = new File(args[2]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				outputStream.println( VariantRec.getSimpleHeader() );
				for(VariantRec rec : homos) {
					outputStream.println(rec.toSimpleString());
				}
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		
		if (firstArg.equals("hetsByContig")) {
			try {
				VariantPool vars = getPool(new File(args[1]));
				DecimalFormat formatter = new DecimalFormat("#0.000");
				int totalTot = 0;
				int totalHets = 0;
				for(String contig : vars.getContigs()) {
					List<VariantRec> conVars = vars.getVariantsForContig(contig);
					int tot = 0;
					int het = 0;
					for(VariantRec rec : conVars) {
						tot++;
						totalTot++;
						if (rec.isHetero()) {
							het++;
							totalHets++;
						}
					}
					System.out.println("Chromosome: " + contig + "\t Variants: " + tot + "\t hets: " + het + " fraction: " + formatter.format( (double)het / (double)tot));
					
				}
				System.out.println("Total variants: " + totalTot + "\t hets: " + totalHets + " fraction: " + formatter.format( (double)totalHets / (double)totalTot));

		
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}

		
		if (firstArg.equals("compoundHet")) {
			handleCompoundHet(args);
			return;
		}

		
		System.out.println("Unrecognized command : " + args[0]);
		emitUsage();
	}
	
	
	class ReadVariants extends SwingWorker {
		
		private final File inputFile;
		private VariantPool variants = null;
		private boolean done = false;
		
		public ReadVariants(File inputFile) {
			this.inputFile = inputFile;
		}
		
		public VariantPool getVariantPool() {
			if (! isDone()) {
				throw new IllegalArgumentException("Not done yet!");
			}
			
			return variants;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			if (inputFile.getName().endsWith(".csv")) {
				variants = new VariantPool(new CSVFile(inputFile));	
			} else {
				if (inputFile.getName().endsWith(".vcf")) {
					variants = new VariantPool(new VCFFile(inputFile));	
				}
				else {
					throw new IllegalArgumentException("Unrecognized file suffix for input file: " + inputFile.getName());
				}
			}
			
			done = true;
			return null;
		}
		
	}
}
