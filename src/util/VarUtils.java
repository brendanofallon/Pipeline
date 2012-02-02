package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.SwingWorker;

import buffer.BEDFile;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantRec;

public class VarUtils {

	
	public static void emitUsage() {
		System.out.println("\tVariant Utils, v0.01 \n\t Brendan O'Fallon \n\t ARUP Labs");
		System.out.println(" java -jar varUtils.jar intersect varsa.vcf varsb.vcf [outputfile]");
		System.out.println(" java -jar varUtils.jar subtract varsa.vcf varsb.vcf [outputfile]");
		System.out.println(" java -jar varUtils.jar homFilter vars.vcf [outputfile]");
		System.out.println(" java -jar varUtils.jar bedFilter vars.vcf bedFile.bed [outputFile]");
		System.out.println(" java -jar varUtils.jar hetsByContig vars.vcf");
		
	}
	
	/**
	 * Obtain an AbstractVariantPool from a file
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static AbstractVariantPool getPool(File inputFile) throws IOException {
		AbstractVariantPool variants = null;
		if (inputFile.getName().endsWith(".csv")) {
			variants = new AbstractVariantPool(new CSVFile(inputFile));	
		} else {
			if (inputFile.getName().endsWith(".vcf")) {
				variants = new AbstractVariantPool(new VCFFile(inputFile));	
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
				AbstractVariantPool varsA = getPool(new File(args[1]));
				AbstractVariantPool varsB = getPool(new File(args[2]));
				
				AbstractVariantPool intersection = (AbstractVariantPool) varsA.intersect(varsB);
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
				AbstractVariantPool varsA = getPool(new File(args[1]));
				AbstractVariantPool varsB = getPool(new File(args[2]));
				
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
		}


		if (firstArg.equals("homFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file to filter");
				return;
			}
			
			try {
				AbstractVariantPool varsA = getPool(new File(args[1]));
				List<VariantRec> homos = varsA.filterPool(VarFilterUtils.getHomoFilter());
				File outputFile = null;
				if (args.length==3) {
					outputFile = new File(args[2]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				for(VariantRec rec : homos) {
					outputStream.println(rec.toSimpleString());
				}
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		if (firstArg.equals("bedFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file and a bed file to filter by");
				return;
			}
			
			try {
				AbstractVariantPool varsA = getPool(new File(args[1]));
				BEDFile bedFile = new BEDFile(new File(args[2]));
				
				AbstractVariantPool filteredVars = varsA.filterByBED(bedFile);
				
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
			
		}

		if (firstArg.equals("hetFilter")) {
			
		}
		
		
		if (firstArg.equals("hetsByContig")) {
			try {
				AbstractVariantPool vars = getPool(new File(args[1]));
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
			
		}
		
	}
	
	
	class ReadVariants extends SwingWorker {
		
		private final File inputFile;
		private AbstractVariantPool variants = null;
		private boolean done = false;
		
		public ReadVariants(File inputFile) {
			this.inputFile = inputFile;
		}
		
		public AbstractVariantPool getVariantPool() {
			if (! isDone()) {
				throw new IllegalArgumentException("Not done yet!");
			}
			
			return variants;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			if (inputFile.getName().endsWith(".csv")) {
				variants = new AbstractVariantPool(new CSVFile(inputFile));	
			} else {
				if (inputFile.getName().endsWith(".vcf")) {
					variants = new AbstractVariantPool(new VCFFile(inputFile));	
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
