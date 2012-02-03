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
import buffer.variant.GenePool;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantRec;

public class VarUtils {

	
	public static void emitUsage() {
		System.out.println("\tVariant Utils, v0.01 \n\t Brendan O'Fallon \n\t ARUP Labs");

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

		System.out.println(" java -jar varUtils.jar compoundHet kidVars.vcf parent1.vcf parent2.vcf");
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
			System.err.println("Count not find file " + args[1]);
			return;
		}
		
		File par1Vars = new File(args[2]);
		if (! kidVars.exists()) {
			System.err.println("Count not find file " + args[2]);
			return;
		}
		
		File par2Vars = new File(args[3]);
		if (! kidVars.exists()) {
			System.err.println("Count not find file " + args[3]);
			return;
		}
		
		try {
			AbstractVariantPool kidPool = getPool(kidVars);
			AbstractVariantPool par1Pool = getPool(par1Vars);
			AbstractVariantPool par2Pool = getPool(par2Vars);
			
			GenePool kidGenes = new GenePool(kidPool);
			GenePool par1Genes = new GenePool(par1Pool);
			GenePool par2Genes = new GenePool(par2Pool);
			
			for(String gene : kidGenes.getGenes()) {
				List<VariantRec> kidList = kidGenes.getVariantsForGene(gene);
				List<VariantRec> par1List = par1Genes.getVariantsForGene(gene);
				List<VariantRec> par2List = par2Genes.getVariantsForGene(gene);
				
				boolean isCompoundHet = hasCompoundHet(kidList, par1List, par2List);
				
				if (isCompoundHet)
					System.out.println(gene + " has at least one compound heterozygote");
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	/**
	 * Returns true if a) no list is empty (or null)
	 * b) the kid list contains at least one hetero var that is also hetero in par1, but absent from par2
	 * c) the kid list contains at least one hetero var that is also hetero in par2, but absent from par1
	 * 
	 * @param kidList
	 * @param par1List
	 * @param par2List
	 * @return
	 */
	private static boolean hasCompoundHet(List<VariantRec> kidList,
										  List<VariantRec> par1List, 
										  List<VariantRec> par2List) {
		
		if (kidList == null || kidList.size() < 2) //Need at least two hets in kid list
			return false;
		if (par1List == null || par1List.size()==0)
			return false;
		if (par2List == null || par2List.size()==0)
			return false;
		
		boolean hasPar1Het = false; //True if any kid var is het and also het in par 1 and absent par2
		boolean hasPar2Het = false; 
		
		for(VariantRec rec : kidList) {
			if (rec.isHetero()) {
				boolean par1Het = isHetero(rec.getStart(), par1List);
				boolean par2Contains = contains(rec.getStart(), par2List);
				
				boolean par2Het = isHetero(rec.getStart(), par2List);
				boolean par1Contains = contains(rec.getStart(), par1List);
				
				hasPar1Het = hasPar1Het || (par1Het && (!par2Contains));
				hasPar2Het = hasPar2Het || (par2Het && (!par1Contains)); 

				if (hasPar1Het && hasPar2Het) {
					return true;
				}
			}
			
		}
		
		return false;
	}

	/**
	 * Returns true if there is a variant at the given start position
	 * is the list AND the variant is a heterozygote 
	 * @param pos
	 * @param list
	 * @return
	 */
	public static boolean isHetero(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos && rec.isHetero()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the given var list contains a variant that starts at the
	 * given position
	 * @param pos
	 * @param list
	 * @return
	 */
	public static boolean contains(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos) {
				return true;
			}
		}
		return false;
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
			return;
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
			return;
		}

		if (firstArg.equals("hetFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of a variant (vcf or csv) file to filter");
				return;
			}
			
			try {
				AbstractVariantPool varsA = getPool(new File(args[1]));
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
