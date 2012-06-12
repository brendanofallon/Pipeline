package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import math.Histogram;

import operator.qc.BamMetrics;
import operator.qc.BamMetrics.BAMMetrics;
import operator.variant.CompareVCF;
import operator.variant.CompoundHetFinder;
import operator.variant.FPComputer;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.GeneLineReader;
import buffer.variant.SimpleLineReader;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.GenePool;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantRec;
import buffer.variant.VariantRec.PositionComparator;

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
		
		System.out.println(" java -jar varUtils.jar geneComp vars1.csv vars2.csv ...");
		System.out.println("			Perform gene-intersection and emit genes with multiple nonsynonymous hits.");
		System.out.println("			** VARIANTS MUST HAVE GENE ANNOTATIONS **");

		System.out.println(" java -jar varUtils.jar summary vars.csv");
		System.out.println("			Emit summary information for the variants");
		
		System.out.println(" java -jar varUtils.jar filterGene vars.csv genelistfile.txt");
		System.out.println("			Emit only those variants in genes given in genelistfile.txt");
		System.out.println("			** VARIANTS MUST HAVE GENE ANNOTATIONS **");

	}
	
	/**
	 * Generate a histogram of the variant depths in the given pool
	 * @param vars
	 * @return
	 */
	public static void computeVarDepthHisto(VariantPool vars, Histogram hist) {
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Double totDepth = var.getProperty(VariantRec.DEPTH);
				Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				if (totDepth == null || varDepth == null)
					continue;
				//System.out.println(var.getContig() + "\t" + var.getStart() + "\t" + totDepth + "\t" + varDepth);
				double ratio = 0.99999*(varDepth / totDepth);
				hist.addValue(ratio);
			}
		}	
	}
	
	
	private static void computeReadDepthHisto(VariantPool vars,	Histogram hist) {
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				Double totDepth = var.getProperty(VariantRec.DEPTH);
				//Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				//System.out.println(var.getContig() + "\t" + var.getStart() + "\t" + totDepth + "\t" + varDepth);
				double ratio = 0.99999*( totDepth);
				hist.addValue(ratio);
			}
		}
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
	 * Emit only those variants that have a GENE_NAME annotation that matches
	 * a gene name in the given gene pool
	 * @param vars
	 * @param genes
	 * @return
	 */
	public static VariantPool filterByGene(VariantPool vars, GenePool genes) {
		VariantPool geneVars = new VariantPool();
		int total = 0;
		int noAnno = 0;
		for(String contig : vars.getContigs()) {
			for(VariantRec var : vars.getVariantsForContig(contig)) {
				total++;
				String geneName = var.getAnnotation(VariantRec.GENE_NAME);
				if (geneName == null) {
					//System.err.println("Variant " + var + " does not have gene name annotation!");
					noAnno++;
				}
				if (genes.containsGene(geneName)) {
					geneVars.addRecord(var);
				}
			}
		}
		
		System.err.println("No gene annotation found for " + noAnno + " of " + total + " variants examined");
		return geneVars;
	}

	
	
	/**
	 * Identify genes which have common mutations among the given input variant files.
	 * Synonymous and intergenic variants are ignored, as well as those with a population frequency
	 * greater than popFreqCutoff
	 * @param pools
	 * @throws IOException 
	 */
	public static void compareGeneSets(List<File> variantFiles) throws IOException {
		
		GenePool genePool = new GenePool();
		for(File file : variantFiles) {
			VariantPool vPool = new VariantPool(getPool(file));

//			VariantPool lowFreqVars = new VariantPool(vPool.filterPool(VarFilterUtils.getPopFreqFilter(popFreqCutoff)));
//			VariantPool interestingVars = new VariantPool( lowFreqVars.filterPool(VarFilterUtils.getNonSynFilter()));
			int found = 0;
			for(String contig : vPool.getContigs()) {
				for(VariantRec var : vPool.getVariantsForContig(contig)) {
					Double prod = var.getProperty(VariantRec.GO_EFFECT_PROD); 
					if (prod != null && prod > 50) {
						genePool.addRecordNoWarn(var);
						found++;
					}
				}
			}
			
			if (found == 0) {
				System.err.println("WARNING : Found 0 variants with correct annotations for file : " + file.getName());
			}
			System.err.println("WARNING : Found " + found + " variants with correct annotations for file : " + file.getName());

		}
		
		List<String> annoKeys = new ArrayList<String>();		
		annoKeys.add(VariantRec.GENE_NAME);
		annoKeys.add(VariantRec.VARIANT_TYPE);
		annoKeys.add(VariantRec.EXON_FUNCTION);
		annoKeys.add(VariantRec.NM_NUMBER);
		annoKeys.add(VariantRec.CDOT);
		annoKeys.add(VariantRec.PDOT);
		annoKeys.add(VariantRec.RSNUM);
		annoKeys.add(VariantRec.POP_FREQUENCY);
		annoKeys.add(VariantRec.EXOMES_FREQ);
		annoKeys.add(VariantRec.EFFECT_PREDICTION2);
		annoKeys.add(VariantRec.SUMMARY_SCORE);
		annoKeys.add(VariantRec.PUBMED_SCORE);
		annoKeys.add(VariantRec.PUBMED_HIT);
		annoKeys.add(VariantRec.GO_EFFECT_PROD);
		annoKeys.add(VariantRec.VQSR);
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		
		
		genePool.listGenesWithMultipleVars(System.out, 2, annoKeys);
		
	}

	/**
	 * Compare the variants in the given files and emit a bunch of info describing their 
	 * differences
	 * @param trueMutsFile
	 * @param inferredMutsFile
	 * @throws IOException
	 */
	public static void compareAndEmitVars(File trueMutsFile, File inferredMutsFile) throws IOException {
		SimpleLineReader tParser = new SimpleLineReader(trueMutsFile);
		VCFLineParser vParser = new VCFLineParser(new VCFFile(inferredMutsFile));
		
		VariantRec trueVar = tParser.toVariantRec();
		VariantRec qVar = vParser.toVariantRec();
		int totalTrueVars = 0;
		int totalFoundVars = 0;
		
		VariantPool perfSNPs = new VariantPool();
		VariantPool falsePosSNPs = new VariantPool();
		VariantPool falseNegSNPs = new VariantPool();
		VariantPool falsePosIndels = new VariantPool();
		VariantPool falseNegIndels = new VariantPool();
		VariantPool perfIndels = new VariantPool();
		VariantPool closeIndels = new VariantPool();
		VariantPool notSoCloseIndels = new VariantPool();
		
		Histogram trueIndelHisto = new Histogram(0, 100, 100);
		Histogram foundIndelHisto = new Histogram(0, 100, 100);
		Histogram notFoundIndelHisto = new Histogram(0, 100, 100);
		
		//WHAT THE CODES MEAN:
		//0 : A perfect match
		//1 : Variant at same position, but different alt allele
		//-1: True variant not found, a false negative
		//-2: Query variant not in reference, a false positive
		//-3: Indels in different spots but exact same sequences
		//-4: Indels in different spots with different sequences, but same length
		//-5: Indels in different spots with different sequences of different lengths
		PositionComparator pComp = VariantRec.getPositionComparator();
		while (trueVar != null && qVar != null) {		
			int dif = pComp.compare(trueVar, qVar);
			
			//Variants are at same position
			if (dif == 0) {
				int result;
				if (trueVar.getRef().equals(qVar.getRef()) && trueVar.getAlt().equals(qVar.getAlt())) {
					result = 0; //Perfect match
					
				}
				else {
					result = 1;
				}
				
				if (trueVar.isIndel()) {
					trueIndelHisto.addValue(trueVar.getIndelLength());
				}
				if (qVar.isIndel()) {
					foundIndelHisto.addValue(qVar.getIndelLength());
				}
				
				System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t " + result);
				tParser.advanceLine();
				vParser.advanceLine();
				
				
				if (qVar.isIndel()) {
					perfIndels.addRecord(qVar);
				}
				else
					perfSNPs.addRecord(qVar);
				totalTrueVars++;
				totalFoundVars++;
			}

			//A bit of fuzzy-matching for insertions and deletions...
			if (dif != 0 && ((qVar.isInsertion() && trueVar.isInsertion()) || (qVar.isDeletion() && trueVar.isDeletion()))) {

				if ( qVar.getAlt().equals(trueVar.getAlt()) && qVar.getRef().equals(trueVar.getRef())) {
					System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -3);
					closeIndels.addRecordNoSort(trueVar);
				} else {
					if (qVar.getIndelLength() == trueVar.getIndelLength()) {
						System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -4);
						closeIndels.addRecordNoSort(trueVar);
					}
					else {
						System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t " + -5);
						System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t " + -5);
						notSoCloseIndels.addRecordNoSort(trueVar);
					}
				}
				
				if (trueVar.isIndel()) {
					trueIndelHisto.addValue(trueVar.getIndelLength());			
				}
				if (qVar.isIndel()) {
					foundIndelHisto.addValue(qVar.getIndelLength());
				}
				
				tParser.advanceLine();
				vParser.advanceLine();
				totalTrueVars++;
				totalFoundVars++;
			}
			else {
				if (dif < 0) {
					//TrueVar exists, but no matching qvar
					System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t -1");
					tParser.advanceLine();
					totalTrueVars++;
					if (trueVar.isIndel()) {
						falseNegIndels.addRecordNoSort(trueVar);
						trueIndelHisto.addValue(trueVar.getIndelLength());
					}
					else {
						falseNegSNPs.addRecordNoSort(trueVar);
					}
				}
				if (dif > 0) {
					//QVar exists, no matching true var
					System.out.println(qVar.getContig() + "\t" + qVar.getStart() + "\t" + qVar.getRef() + "\t" + qVar.getAlt() + "\t -2");
					vParser.advanceLine();
					totalFoundVars++;
					if (qVar.isIndel()) {
						falsePosIndels.addRecordNoSort(qVar);
						foundIndelHisto.addValue(qVar.getIndelLength());
					}
					else
						falsePosSNPs.addRecordNoSort(qVar);
				}
			}
			
			trueVar = tParser.toVariantRec();
			qVar = vParser.toVariantRec();
		}
		
		while(trueVar != null) {
			System.out.println(trueVar.getContig() + "\t" + trueVar.getStart() + "\t" + trueVar.getRef() + "\t" + trueVar.getAlt() + "\t -1");
			tParser.advanceLine();
			totalTrueVars++;
			if (trueVar.isIndel()) {
				falseNegIndels.addRecordNoSort(trueVar);
				trueIndelHisto.addValue(trueVar.getIndelLength());
			}
			else {
				falseNegSNPs.addRecordNoSort(trueVar);
			}
			
			tParser.advanceLine();
			trueVar = tParser.toVariantRec();
		}
		
		while (qVar != null) {
			totalFoundVars++;
			if (qVar.isIndel()) {
				falsePosIndels.addRecordNoSort(qVar);
				foundIndelHisto.addValue(qVar.getIndelLength());
			}
			else
				falsePosSNPs.addRecordNoSort(qVar);
			
			vParser.advanceLine();
			qVar = vParser.toVariantRec();
		}
		
		System.err.println("Total true variants: " + totalTrueVars);
		System.err.println("Total found variants: " + totalFoundVars);
		System.err.println("Total perfect matches: " + (perfSNPs.size() + perfIndels.size()) );
		System.err.println("Total perfect SNP matches: " + perfSNPs.size() );
		System.err.println("Total SNP false positives : "+ falsePosSNPs.size());
		System.err.println("Total SNP false negatives : "+ falseNegSNPs.size());
		
		System.err.println("Total perfect indel matches: " + perfIndels.size() );
		System.err.println("Total indel false positives : "+ falsePosIndels.size());
		System.err.println("Total indel false negatives : "+ falseNegIndels.size());
		
		System.err.println("Total indel near-misses : "+ closeIndels.size());
		System.err.println("Total indel sort-of-near-misses : "+ notSoCloseIndels.size());
		

		System.err.println("Histogram of true indel sizes: ");
		System.err.println(trueIndelHisto.toString());
		System.err.println("Histo size: " + trueIndelHisto.getCount());
		
		for(String contig : falseNegIndels.getContigs()) {
			List<VariantRec> recs = falseNegIndels.getVariantsForContig(contig);
			for(VariantRec rec : recs) 
				notFoundIndelHisto.addValue(rec.getIndelLength());
		}

		System.err.println("Histogram of not found (false neg) indels sizes: ");
		System.err.println(notFoundIndelHisto.toString());
		System.err.println("Histo size: " + notFoundIndelHisto.getCount());
		
		Histogram falsePosIndelHisto = new Histogram(0, 100, 100);
		for(String contig : falsePosIndels.getContigs()) {
			List<VariantRec> recs = falsePosIndels.getVariantsForContig(contig);
			for(VariantRec rec : recs) 
				falsePosIndelHisto.addValue(rec.getIndelLength());
		}

		System.err.println("Histogram of false pos indel sizes: ");
		System.err.println(falsePosIndelHisto.toString());
		System.err.println("Histo size: " + falsePosIndelHisto.getCount());
				
		
		System.err.println("Histogram of all indels found (regardless of correctness): ");
		System.err.println(foundIndelHisto.toString());
		System.err.println("Histo size: " + foundIndelHisto.getCount());
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
			//variants = new VariantPool(new SimpleLineReader(inputFile));
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
	
	/**
	 * Obtain an VariantPool from a CSV using a 'SimpleLineReader', which only
	 * 
	 * @param inputFile
	 * @return
	 * @throws IOException
	 */
	public static VariantPool getSPool(File inputFile) throws IOException {
		VariantPool variants = new VariantPool(new SimpleLineReader(inputFile));	
		return variants;
	}
	
	public static void main(String[] args) {
		
		//args = new String[]{"subtract", "/media/DATA/whitney_genome/52Kid2/contig_22.realigned.sorted.recal.vcf", "/media/DATA/whitney_genome/52Kid3/contig_22.realigned.sorted.recal.vcf"};
		
		if (args.length==0) {
			emitUsage();
			return;
		}
		
		String firstArg = args[0];
		
		if (firstArg.equals("geneFilter")) {
			if (args.length != 3) {
				System.out.println("Enter the names input variants file and a file containing gene names to filter on");
				return;
			}
			try {
				VariantPool vars = getPool(new File(args[1]));
				GenePool genes = new GenePool(new File(args[2]));
				
				VariantPool geneVars = filterByGene(vars, genes);
				List<String> annos = new ArrayList<String>();
				annos.add(VariantRec.GENE_NAME);
				annos.add(VariantRec.NM_NUMBER);
				annos.add(VariantRec.EXON_FUNCTION);
				annos.add(VariantRec.VARIANT_TYPE);
				annos.add(VariantRec.RSNUM);
				annos.add(VariantRec.POP_FREQUENCY);
				annos.add(VariantRec.EXOMES_FREQ);
				annos.add(VariantRec.CDOT);
				annos.add(VariantRec.PDOT);
				//annos.add(VariantRec.VQSR);
				geneVars.listAll(System.out, annos);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return;
		}
		
		if (firstArg.equals("wcompare")) {
			if (args.length != 3) {
				System.out.println("Enter the names of the truemuts.csv file and a vcf file to compare");
				return;
			}
			
			try {
				File fileA = new File(args[1]);
				File fileB = new File(args[2]);
				
				compareAndEmitVars(fileA, fileB);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return;
		}
		
		//Compare 'truemuts'-style csv to a vcf 
		if (firstArg.equals("scompare")) {
			if (args.length != 3) {
				System.out.println("Enter the names of the truemuts.csv file and a vcf file to compare");
				return;
			}
			
			try {
				File fileA = new File(args[1]);
				File fileB = new File(args[2]);
				DecimalFormat formatter = new DecimalFormat("#0.000");
				
				VariantPool trueVars = getSPool(new File(args[1]));
				VariantPool queryVars = getPool(new File(args[2]));
				
				System.out.println("Total true variants " + fileA.getName() + " : " + trueVars.size() );
				System.out.println("Total inferred variants " + fileB.getName() + " : " + queryVars.size() + " mean quality: " + formatter.format(queryVars.meanQuality()));
				
				CompareVCF.compareVars(trueVars, queryVars, System.out);		
								
				VariantPool trueSNPs = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isSNP();
					}
				}));
				
				VariantPool qSNPs = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isSNP();
					}
				}));
				
				System.out.println("*** SNPs Only Comparison ********");
				System.out.println("True number of SNPs : " + trueSNPs.size());
				System.out.println("Inferred number of SNPs : " + qSNPs.size());
				CompareVCF.compareVars(trueSNPs, qSNPs, System.out);
				
				VariantPool trueInsertions = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isInsertion();
					}
				}));
				
				VariantPool qInserts = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isInsertion();
					}
				}));
				
				System.out.println("*** Insertions only Comparison ********");
				System.out.println("True number of insertions : " + trueInsertions.size());
				System.out.println("Inferred number of insertions : " + qInserts.size());
				CompareVCF.compareVars(trueInsertions, qInserts, System.out);
				
				
				VariantPool trueDeletions = new VariantPool(trueVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isDeletion();
					}
				}));
				
				VariantPool qDeletes = new VariantPool(queryVars.filterPool(new VariantFilter() {
					public boolean passes(VariantRec rec) {
						return rec.isDeletion();
					}
				}));
				
				System.out.println("*** Deletions only Comparison ********");
				System.out.println("True number of deletions : " + trueDeletions.size());
				System.out.println("Inferred number of deletions : " + qDeletes.size());
				
				CompareVCF.compareVars(trueDeletions, qDeletes, System.out);
				
				
				System.out.println(" (finding intersection, this may take a minute..)");
				VariantPool intersection = (VariantPool) trueVars.intersect(queryVars);
	
				VariantPool uniqA = new VariantPool(trueVars);
				uniqA.removeVariants(intersection);
				VariantPool uniqB = new VariantPool(queryVars);
				uniqB.removeVariants(intersection);
				
				System.out.println("Total mumber true vars not found (false negatives) : " + uniqA.size());
				System.out.println("Total number false positives : " + uniqB.size());
				
				int hetsA = trueVars.countHeteros();
				int hetsB = queryVars.countHeteros();
				System.out.println("Heterozyotes in " + fileA.getName() + " : " + hetsA + " ( " + formatter.format(100.0*(double)hetsA/(double)trueVars.size()) + " % )");
				System.out.println("Heterozyotes in " + fileB.getName() + " : " + hetsB +  " ( " + formatter.format(100.0*(double)hetsB/(double)queryVars.size()) + " % )");
				

				System.out.println("Total intersection size: " + intersection.size());
				System.out.println("%Intersection in " + fileA.getName() + " : " + formatter.format( (double)intersection.size() / (double)trueVars.size()));
				System.out.println("%Intersection in " + fileB.getName() + " : " + formatter.format( (double)intersection.size() / (double)queryVars.size()));
				
				
				System.out.println("Mean quality of sites in intersection: " + formatter.format(intersection.meanQuality()));
				System.out.println("Mean quality of sites in A but not in intersection: " + formatter.format(uniqA.meanQuality()));
				System.out.println("Mean quality of sites in B but not in intersection: " + formatter.format(uniqB.meanQuality()));

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return;
		}
		
		if (firstArg.equals("hapCompare")) {
			if (args.length != 3) {
				System.out.println("Enter the names of the sample-from-hapmap csv file and the query file to compare");
				return;
			}
	
			DecimalFormat formatter = new DecimalFormat("#0.000");

			try {
				VariantPool hapmap = getPool(new File(args[1]));
				VariantPool sample = getPool(new File(args[2]));
				
				int falsePoz = 0;
				int falseNeg = 0;
				int truePoz = 0;
				int trueNeg = 0;
				int totHapMapVar = 0;
				int wrongZygosity = 0;
				
				VariantPool falsePosPool = new VariantPool();
				VariantPool falseNegPool = new VariantPool();
				
				for(String contig : hapmap.getContigs()) {
					for(VariantRec var : hapmap.getVariantsForContig(contig)) {
						VariantRec sampleVar = sample.findRecordNoWarn(contig, var.getStart());
						if (var.isVariant() && sampleVar == null) {
							totHapMapVar++;
							falseNeg++;
						}
						if (var.isVariant() && sampleVar != null) {
							totHapMapVar++;
							truePoz++;
							int flag = 0;
							if (var.isHetero())
								flag++;
							if (sampleVar.isHetero())
								flag++;
							if (flag==1)
								wrongZygosity++;
						}
						if ((!var.isVariant()) && sampleVar==null) {
							trueNeg++;
						}
						if ((!var.isVariant()) && sampleVar!=null) {
							falsePoz++;
							falsePosPool.addRecord(sampleVar);
						}
						
					}
					
				}
				
				System.out.println("Found " + hapmap.size() + " sites from hapmap");
				System.out.println("Of these " + totHapMapVar + " were non-reference");
				System.out.println("Total true positives: " + truePoz);
				System.out.println("Total true negatives: " + trueNeg);
				System.out.println("Total false positives: " + falsePoz);
				System.out.println("Total false negatives: " + falseNeg);
				System.out.println("Total true positives, but wrong zygosity : " + wrongZygosity);
				
				double falsePosRate = (double)falsePoz / (double)(hapmap.size() - totHapMapVar);
				double falseNegRate = (double)falseNeg / (double)(totHapMapVar);
				System.out.println(" False positive percentage : " + formatter.format(100* falsePosRate) + "%" );
				System.out.println(" False negative percentage : " + formatter.format(100* falseNegRate) + "%" );
				
				System.out.println("False positives TT ratio : " + falsePosPool.computeTTRatio());
				//falsePosPool.listAll(System.out);
				
				return;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
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
				System.out.println("Enter the names of two (or more) variant (vcf or csv) files to intersect");
				return;
			}

			
			try {
				VariantPool vars = getPool(new File(args[1]));
				
				for(int i=2; i<args.length; i++) {
					VariantPool varsB = getPool(new File(args[i]));
					VariantPool intersection = (VariantPool) vars.intersect(varsB);
					vars = intersection;
				}
				
				List<String> annos = new ArrayList<String>();
				annos.add(VariantRec.GENE_NAME);
				annos.add(VariantRec.NM_NUMBER);
				annos.add(VariantRec.EXON_FUNCTION);
				annos.add(VariantRec.VARIANT_TYPE);
				annos.add(VariantRec.RSNUM);
				annos.add(VariantRec.POP_FREQUENCY);
				annos.add(VariantRec.EXOMES_FREQ);
				annos.add(VariantRec.EFFECT_PREDICTION);
				annos.add(VariantRec.EFFECT_PREDICTION2);
				annos.add(VariantRec.SUMMARY_SCORE);
				annos.add(VariantRec.GO_SCORE);
				annos.add(VariantRec.GO_EFFECT_PROD);
				annos.add(VariantRec.VQSR);
				annos.add(VariantRec.CDOT);
				annos.add(VariantRec.PDOT);
				annos.add(VariantRec.HGMD_INFO);

				PrintStream outputStream = System.out;

				vars.listAll(outputStream, annos);
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
				
				List<String> annoKeys = new ArrayList<String>();
				annoKeys.add(VariantRec.RSNUM);
				annoKeys.add(VariantRec.POP_FREQUENCY);
				annoKeys.add(VariantRec.GENE_NAME);
				annoKeys.add(VariantRec.VARIANT_TYPE);
				annoKeys.add(VariantRec.EXON_FUNCTION);
				annoKeys.add(VariantRec.CDOT);
				annoKeys.add(VariantRec.PDOT);
				annoKeys.add(VariantRec.VQSR);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				annoKeys.add(VariantRec.FS_SCORE);
				varsA.listAll(outputStream, annoKeys);
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
				VariantPool homos = new VariantPool(varsA.filterPool(VarFilterUtils.getHomoFilter()));
				File outputFile = null;
				if (args.length==3) {
					outputFile = new File(args[2]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				homos.listAll(outputStream);
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
				List<String> annoKeys = new ArrayList<String>();
				annoKeys.add(VariantRec.RSNUM);
				annoKeys.add(VariantRec.POP_FREQUENCY);
				annoKeys.add(VariantRec.GENE_NAME);
				annoKeys.add(VariantRec.VARIANT_TYPE);
				annoKeys.add(VariantRec.EXON_FUNCTION);
				annoKeys.add(VariantRec.CDOT);
				annoKeys.add(VariantRec.PDOT);
				annoKeys.add(VariantRec.VQSR);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				annoKeys.add(VariantRec.FS_SCORE);
				filteredVars.listAll(outputStream, annoKeys);
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
				VariantPool homos = new VariantPool(varsA.filterPool(VarFilterUtils.getHeteroFilter()));
				File outputFile = null;
				if (args.length==3) {
					outputFile = new File(args[2]);
				}
				
				PrintStream outputStream = System.out;
				if (outputFile != null) {
					outputStream = new PrintStream(new FileOutputStream(outputFile));
				}
				homos.listAll(outputStream);
				outputStream.close();
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

		
		if (firstArg.equals("geneComp")) {
			if (args.length < 2) {
				System.out.println("Enter the names of one or more variant (vcf or csv) files for which to build the pool");
				return;
			}
			
			List<File> varFiles = new ArrayList<File>();
			for(int i=1; i<args.length; i++) {
				varFiles.add(new File(args[i]));
			}
			
			try {
				compareGeneSets(varFiles);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
	
		if (firstArg.equals("interestingFilter")) {
			if (args.length < 2) {
				System.out.println("Enter the names of one or more variant (vcf or csv) files to examine");
				return;
			}

			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.NM_NUMBER);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.EXOMES_FREQ);
			
			annoKeys.add(VariantRec.OMIM_ID);
			annoKeys.add(VariantRec.HGMD_INFO);
			
			//annoKeys.add(VariantRec.VQSR);
			//annoKeys.add(VariantRec.FALSEPOS_PROB);
			//annoKeys.add(VariantRec.FS_SCORE);
			StringBuilder header = new StringBuilder( VariantRec.getSimpleHeader() );
			for(String key : annoKeys) 
				header.append("\t" + key);
			System.out.println( header );
			
			try {
				double frequencyCutoff = 0.05;
				boolean hasUserCutoff = false;
				//See if we can parse a double from args[1]
				try {
					Double val = Double.parseDouble(args[1]);
					frequencyCutoff = val;
					hasUserCutoff = true;
				}
				catch (NumberFormatException nfe) {
					//dont worry about it
					hasUserCutoff = false;
				}
				
				VariantPool pool;
				if (hasUserCutoff)
					pool = getPool(new File(args[2]));
				else
					pool = getPool(new File(args[1]));
				
				//List<VariantRec> novelVars = pool.filterPool(VarFilterUtils.getPopFreqFilter(frequencyCutoff));
				
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						boolean passes = false;
						
						if (var.getProperty(VariantRec.POP_FREQUENCY) == null || (var.getProperty(VariantRec.POP_FREQUENCY) != null && var.getProperty(VariantRec.POP_FREQUENCY) < frequencyCutoff)) {
							passes = true;
						}
						
						String varType = var.getAnnotation(VariantRec.VARIANT_TYPE);
						String exonFunc = var.getAnnotation(VariantRec.EXON_FUNCTION);
						
						//Exclude variants that are not in exons or splicing 
						if (passes && !(varType != null && varType.contains("exon") || varType.contains("splic"))) {
							passes = false;
						}
						
						if (passes && (exonFunc != null && exonFunc.trim().startsWith("synony"))) {
							passes = false;
						}
						
						if (var.getAnnotation(VariantRec.OMIM_ID) != null || var.getAnnotation(VariantRec.HGMD_INFO) != null) {
							passes = true;
						}
						
						if (passes) {
							System.out.println(var.toSimpleString() + var.getPropertyString(annoKeys));
						}
					}
				}
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		//General filter, second arg must match a column header, third arg is value for filter
		if (firstArg.equals("filter") || firstArg.equals("ufilter")) {
			if (args.length < 4) {
				System.out.println("Enter the property to filter by and the cutoff value");
				return;
			}
			
			boolean greater = true;
			if (firstArg.equals("ufilter"))
				greater = false;
			
			String prop = args[1];
			double val = Double.parseDouble(args[2]);
			try {
				VariantPool pool = getPool(new File(args[3]));
				VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						Double varVal = var.getProperty(prop);
						if (varVal == null) {
							filteredVars.addRecord(var);
						}
						else {
							if ( (!greater) && varVal < val) {
								filteredVars.addRecord(var);
							}
							if (greater && varVal > val) {
								filteredVars.addRecord(var);
							}
						}
					}
				}
				
				List<String> annoKeys = new ArrayList<String>();
				annoKeys.add(VariantRec.RSNUM);
				annoKeys.add(VariantRec.POP_FREQUENCY);
				annoKeys.add(VariantRec.GENE_NAME);
				annoKeys.add(VariantRec.VARIANT_TYPE);
				annoKeys.add(VariantRec.EXON_FUNCTION);
				annoKeys.add(VariantRec.CDOT);
				annoKeys.add(VariantRec.PDOT);
				annoKeys.add(VariantRec.VQSR);
				annoKeys.add(prop);
				filteredVars.listAll(System.out, annoKeys);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		
			return;
		}
		
		
		if (firstArg.equals("summary") || firstArg.equals("sum")) {
			if (args.length < 2) {
				System.out.println("Enter the names of one or more variant (vcf or csv) files to examine");
				return;
			}
			
			Histogram varDepthHisto = new Histogram(0, 1.0, 50); 
			Histogram readDepthHisto = new Histogram(1, 250, 100); 
			double qualitySum = 0;
			double tstvSum = 0;
			double hetSum = 0;
			double varSum = 0;
			
			for(int i=1; i<args.length; i++) {
				try {
					DecimalFormat formatter = new DecimalFormat("#0.00");
					if (i>1)
						System.out.println("\n\n");
					VariantPool pool = getPool(new File(args[i]));
					
					System.out.println("Summary for file : " + args[i]);
					System.out.println("Total variants : " + pool.size());
					System.out.println("Mean variant quality : " + pool.meanQuality());
					System.out.println("Total SNPs : " + pool.countSNPs());
					System.out.println("Total indels: " + (pool.countInsertions() + pool.countDeletions()));
					System.out.println("\t insertions: " + pool.countInsertions());
					System.out.println("\t deletions: " + pool.countDeletions());
					System.out.println(" Ts / Tv ratio: " + pool.computeTTRatio());
					
					qualitySum += pool.meanQuality()*pool.size();
					tstvSum += pool.computeTTRatio()*pool.size();
					hetSum += pool.countHeteros();
					varSum += pool.size();
					
					int heteros = pool.countHeteros();
					System.out.println(" Heterozygotes : " + heteros + " (" + formatter.format((double)heteros / (double)pool.size()) + "% )");
					
					
					VariantPool q30Pool = new VariantPool(pool.filterPool(new VariantFilter() {
						public boolean passes(VariantRec rec) {
							return rec.getQuality() > 29.99;
						}
					}));
					
					System.out.println("\n Variants with Q > 30 : ");
					System.out.println("Total variants (q30): " + q30Pool.size());
					System.out.println("Mean variant quality (q30): " + q30Pool.meanQuality());
					System.out.println("Total SNPs (q30): " + q30Pool.countSNPs());
					System.out.println("Total indels (q30): " + (q30Pool.countInsertions() + q30Pool.countDeletions()));
					System.out.println("\t insertions (q30): " + q30Pool.countInsertions());
					System.out.println("\t deletions (q30): " + q30Pool.countDeletions());
					System.out.println(" Ts / Tv ratio (q30): " + q30Pool.computeTTRatio());
					heteros = q30Pool.countHeteros();
					System.out.println(" Heterozygotes : " + heteros + " (" + formatter.format((double)heteros / (double)q30Pool.size()) + "% )");
	
					
					//computeVarDepthHisto(pool, varDepthHisto);
					//computeReadDepthHisto(pool, readDepthHisto);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				
			}
			
			double qualityMean = qualitySum / varSum;
			double hetMean = hetSum / varSum;
			double tstvMean = tstvSum / varSum;
			
			if (args.length > 2) {
				System.out.println("Overall statistics :");
				System.out.println("Total variants examined :" + varSum);
				System.out.println("Mean quality score : " + qualityMean);
				System.out.println("Hetero fraction:" + hetMean);
				System.out.println("Ts/Tv mean:" + tstvMean);
			}
			
			//System.out.println("Histogram of variant frequencies:");
			//System.out.println(varDepthHisto.toString());
//			System.out.println("Histogram of read depths :");
//			System.out.println(readDepthHisto.toString());
			//System.out.println(varDepthHisto.freqsToCSV());
			
			return;
		}
	
		
		
		if (firstArg.equals("bamMetrics")) {
			
			for(int i=1; i<args.length; i++ ) {
				BAMFile bamFile = new BAMFile(new File(args[i]));
				BAMMetrics metrics = BamMetrics.computeBAMMetrics(bamFile);
			
				System.out.println("Summary for " + args[i] + "\n" + BamMetrics.getBAMMetricsSummary(metrics));
			}
				
			
			return;
		}
		
		
		if (firstArg.equals("computeFP")) {
			try {
				VariantPool variants = getPool(new File(args[1]));
				FPComputer.computeFPForPool(variants);
				
				List<String> annoKeys = new ArrayList<String>();
				annoKeys.add(VariantRec.VAR_DEPTH);
				annoKeys.add(VariantRec.VQSR);
				annoKeys.add(VariantRec.FS_SCORE);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				variants.listAll(System.out, annoKeys);
			} catch (IOException e) {
				e.printStackTrace();
			}

			
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

