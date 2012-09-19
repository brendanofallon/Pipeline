package util;

import gene.Gene;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import math.Histogram;
import math.LazyHistogram;
import operator.qc.BamMetrics;
import operator.variant.CompareVCF;
import operator.variant.CompoundHetFinder;
import operator.variant.FPComputer;
import util.flatFilesReader.DBNSFPReader;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.CSVFile;
import buffer.IntervalsFile.Interval;
import buffer.VCFFile;
import buffer.variant.CSVLineReader;
import buffer.variant.GenePool;
import buffer.variant.SimpleLineReader;
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantFilter;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
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
		
		System.out.println(" java -jar varUtils.jar filter [property] [value] vars.csv");
		System.out.println("			Emit all variants with the given property GREATER than the given value (e.g. pop.freq 0.025)");

		System.out.println(" java -jar varUtils.jar ufilter [property] [value] vars.csv");
		System.out.println("			Emit all variants with the given property LESS than the given value (e.g. pop.freq 0.025)");

		System.out.println(" java -jar varUtils.jar extract [property|annotation] vars.csv");
		System.out.println("			Emit only the value of the property or annotation for all variants in pool");

		System.out.println(" java -jar varUtils.jar histogram [property] vars.csv");
		System.out.println("			Emit a histogram of the values associated with the given property ");		
		
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
		File par1Vars = new File(args[2]);
		File par2Vars = new File(args[3]);
		
		try {
			VariantPool kidPool  = getPool(kidVars);
			VariantPool par1Pool = getPool(par1Vars);
			VariantPool par2Pool = getPool(par2Vars);
			
			CompoundHetFinder.computeCompoundHets(kidPool, par1Pool, par2Pool, null);
				
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
	public static VariantPool filterByGene(VariantPool vars, GenePool genes, boolean reverse) {
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
				
				if (reverse) {
					if (! genes.containsGene(geneName)) {
						geneVars.addRecord(var);
					}
				}
				else {
					if (genes.containsGene(geneName)) {
						geneVars.addRecord(var);
					}
				}
			}
		}
		
//		if (noAnno > 0)
//			System.err.println("No gene annotation found for " + noAnno + " of " + total + " variants examined");
		return geneVars;
	}

	
	
	/**
	 * Identify genes which have common mutations among the given input variant files.
	 * Synonymous and intergenic variants are ignored, as well as those with a population frequency
	 * greater than popFreqCutoff
	 * @param pools
	 * @throws IOException 
	 */
	public static void compareGeneSets(int cutoff, List<File> variantFiles) throws IOException {
		
		GenePool genePool = new GenePool();
		for(File file : variantFiles) {
			VariantPool vPool = new VariantPool(getPool(file));

//			VariantPool lowFreqVars = new VariantPool(vPool.filterPool(VarFilterUtils.getPopFreqFilter(popFreqCutoff)));
//			VariantPool interestingVars = new VariantPool( lowFreqVars.filterPool(VarFilterUtils.getNonSynFilter()));
			int found = 0;
			for(String contig : vPool.getContigs()) {
				for(VariantRec var : vPool.getVariantsForContig(contig)) {
					//Double prod = var.getProperty(VariantRec.GO_EFFECT_PROD);
					boolean passes = true;
					passes = var.getQuality() > 25.0;
					
//					String func = var.getAnnotation(VariantRec.EXON_FUNCTION);
					
					String type = var.getAnnotation(VariantRec.VARIANT_TYPE);
					if (type == null || (!type.contains("UTR3"))) {
						passes = false;
					}
					
					Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
					if (freq != null && freq > 0.01)
						passes = false;
					
//					if (type == null || (!type.contains("exonic"))) {
//						passes = false;
//					}
					
//					String gene = var.getAnnotation(VariantRec.GENE_NAME);
//					if (gene == null || (! gene.contains("MIR"))) {
//						passes = false;
//					}
					
					
					
//					if (func != null && (func.contains("nonsyn") 
//							|| func.contains("splic")
//							|| func.contains("stopgain")
//							|| func.contains("stoploss")
//							|| func.contains("frameshift"))) {
//						
//						Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
//						if (freq != null && freq > 0.01)
//							passes = false;
//						
//						if (passes) {
//							Double cgFreq = var.getProperty(VariantRec.CG69_FREQUENCY);
//							if (cgFreq != null && cgFreq > 0.02)
//								passes = false;
//						}
//						
//						if (! var.isHetero()) {
//							passes = false;
//						}
//						
//						
//						
//					}
//					else {
//						passes = false;
//					}
					
					if (passes) {
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
		annoKeys.add(VariantRec.EFFECT_PREDICTION2);
		annoKeys.add(Gene.GENE_RELEVANCE);
//		annoKeys.add(VariantRec.SUMMARY_SCORE);
//		annoKeys.add(VariantRec.PUBMED_SCORE);
//		annoKeys.add(VariantRec.PUBMED_HIT);
		annoKeys.add(VariantRec.GO_EFFECT_PROD);
		annoKeys.add(VariantRec.VQSR);
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		annoKeys.add(VariantRec.LRT_SCORE);
		annoKeys.add(VariantRec.SIPHY_SCORE);
		
		genePool.listGenesWithMultipleVars(System.out, cutoff, annoKeys);
		
	}


	private static VariantLineReader getReader(String filename) throws IOException {
		VariantLineReader reader = null;
		if (filename.endsWith("vcf")) {
			reader = new VCFLineParser(new File(filename));
		}
		if (filename.endsWith("csv")) {
			reader = new CSVLineReader(new File(filename));
		}
		return reader;
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
			performGeneFilter(args);
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
			performHapCompare(args);
			return;
		}
		
		if (firstArg.equals("compare")) {
			performCompare(args);
			return;
		}
		
		if (firstArg.equals("propGeneComp")) {
			performPropGeneComp(args);
			return;
		}
		
		if (firstArg.equals("freqComp")) {
			performFreqComp(args);
			return;
		}
		
		if (firstArg.equals("computeFP")) {
			performComputeFP(args);
			return;
		}
		
		if (firstArg.equals("intersect")) {
			performIntersection(args);
			return;
		}
		
		if (firstArg.equals("subtract")) {
			performSubtraction(args);
			return;
		}


		if (firstArg.equals("homFilter")) {
			performHomFilter(args);
			return;
		}

		if (firstArg.equals("geneExtract")) {
			performGenePropExtract(args, false);
			return;
		}
		
		if (firstArg.equals("revGeneExtract")) {
			performGenePropExtract(args, true);
			return;
		}
		
		if (firstArg.equals("bedFilter")) {
			performBedFilter(args);
			return;
		}

		if (firstArg.equals("hetFilter")) {
			performHetFilter(args);
			return;
		}

		
		if (firstArg.equals("compoundHet")) {
			handleCompoundHet(args);
			return;
		}

		
		if (firstArg.equals("geneComp")) {
			performGeneComp(args);
			return;
		}
			
	
		if (firstArg.equals("interestingFilter")) {
			performInterestingFilter(args);
			return;
		}
			
		
		//Extract property or annotation and emit to stdout
		if (firstArg.equals("ex") || firstArg.equals("extract")) {
			performExtract(args);
			return;
		}
		
		//Just like filter, but lists all matching records to stdout
		if (firstArg.equals("emit")) {
			performEmit(args);
			return;
		}
			
		
		//General filter, second arg must match a column header, third arg is value for filter
		if (firstArg.equals("filter") || firstArg.equals("ufilter")) {
			performFilter(args);
			return;
		}
		
		if (firstArg.equals("annofilter") || firstArg.equals("afilter")) {
			performAnnoFilter(args);
			return;
		}
		
		
		if (firstArg.equals("sampleCount") ) {
			performSampleCount(args);
			return;
		}
		
		if (firstArg.equals("combine") || firstArg.equals("union")) {
			performCombine(args);
			return;
		}

		if (firstArg.startsWith("removeDup")) {
			performRemoveDuplicates(args);
			return;
		}

		
		if (firstArg.equals("summary") || firstArg.equals("sum")) {
			performSummary(args);
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
		
		
		if (firstArg.startsWith("hist")) {
			performHistogram(args);
			return;
		}
		
			
		
		System.out.println("Unrecognized command : " + args[0]);
		emitUsage();
	}

	
	
	private static void performAnnoFilter(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		
		String key = args[1];
		String val = args[2];
		try {
			for(int i=3; i<args.length; i++) {
				VariantPool pool = getPool(new File(args[i]));
				VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String anno = var.getAnnotation(key);
						if (anno != null && anno.contains(val)) {				
							filteredVars.addRecord(var);
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
				annoKeys.add(VariantRec.DEPTH);
				annoKeys.add(VariantRec.EFFECT_PREDICTION2);
				//annoKeys.add(VariantRec.PUBMED_SCORE);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				annoKeys.add(VariantRec.TAUFP_SCORE);
				annoKeys.add(VariantRec.FS_SCORE);
				annoKeys.add(VariantRec.MT_SCORE);
				annoKeys.add(VariantRec.POLYPHEN_SCORE);
				annoKeys.add(VariantRec.SIFT_SCORE);
				annoKeys.add(VariantRec.GERP_SCORE);
				annoKeys.add(VariantRec.PHYLOP_SCORE);
				annoKeys.add(key);
				
				
				if (args.length==4) {
					filteredVars.listAll(System.out, annoKeys);
					System.err.println("Found " + filteredVars.size() + " vars with " + key + " containing " + val);
				}
				else {
					//Write results to a file
					String outfilename = args[i].replace(".vcf", "").replace(".csv", "") + ".flt-" + val + ".csv";
					PrintStream out = new PrintStream(new FileOutputStream(new File(outfilename)));
					System.err.println("Emitting vars from file " + args[i] + " to : " + outfilename);
					filteredVars.listAll(out, annoKeys);
					out.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performGeneFilter(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the name of the file containing gene names, then one or more variant files");
			return;
		}
		try {
			GenePool genes = new GenePool(new File(args[1]));
			for(int i=2; i<args.length; i++) {
				VariantPool vars = getPool(new File(args[i]));
				VariantPool geneVars = filterByGene(vars, genes, false);
				List<String> annos = new ArrayList<String>();
				annos.add(VariantRec.GENE_NAME);
				annos.add(VariantRec.NM_NUMBER);
				annos.add(VariantRec.EXON_FUNCTION);
				annos.add(VariantRec.VARIANT_TYPE);
				annos.add(VariantRec.RSNUM);
				annos.add(VariantRec.POP_FREQUENCY);
				annos.add(VariantRec.EXOMES_FREQ);
				annos.add(VariantRec.EFFECT_PREDICTION2);
				annos.add(VariantRec.FS_SCORE);
				annos.add(VariantRec.MT_SCORE);
				annos.add(VariantRec.PHYLOP_SCORE);
				annos.add(VariantRec.GERP_SCORE);
				annos.add(VariantRec.POLYPHEN_SCORE);
				annos.add(VariantRec.CDOT);
				annos.add(VariantRec.PDOT);
				//annos.add(VariantRec.VQSR);
				
				if (args.length==3) {
					geneVars.listAll(System.out, annos);
				}
				else {
					//Write results to a file
					String suffix = args[1].substring(0, args[1].lastIndexOf("."));
					String outfilename = args[i].replace(".vcf", "").replace(".csv", "") + "." + suffix + ".csv";
					PrintStream out = new PrintStream(new FileOutputStream(new File(outfilename)));
					System.err.println("Emitting vars from file " + args[i] + " to : " + outfilename);
					geneVars.listAll(out, annos);
					out.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performPropGeneComp(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the name of the file containing gene names, then one property to extract, then the variant files");
			return;
		}
		try {
			String key = args[2];
			GenePool genes = new GenePool(new File(args[1]));
			for(int i=3; i<args.length; i++) {
				VariantPool vars = getPool(new File(args[i]));
				VariantPool geneVars = filterByGene(vars, genes, false);
				VariantPool nonGeneVars = filterByGene(vars, genes, true);
				
				
				for(String contig : geneVars.getContigs()) {
					for(VariantRec var : geneVars.getVariantsForContig(contig)) {
						if (var.getAnnotation(VariantRec.EXON_FUNCTION) != null && var.getAnnotation(VariantRec.EXON_FUNCTION).contains("nonsynon"))
							try {
								Double val = Double.parseDouble( var.getPropertyOrAnnotation(key) );
								System.out.println(val);
							}
							catch (NumberFormatException nfe) {
								//Do nothing
							}
					}
				}
				
				for(String contig : nonGeneVars.getContigs()) {
					for(VariantRec var : nonGeneVars.getVariantsForContig(contig)) {
						if (var.getAnnotation(VariantRec.EXON_FUNCTION) != null && var.getAnnotation(VariantRec.EXON_FUNCTION).contains("nonsynon")) {
							try {
								Double val = Double.parseDouble( var.getPropertyOrAnnotation(key) );
								System.err.println(val);
							}
							catch (NumberFormatException nfe) {
								//Do nothing
							}
							
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Emit only the property or annotation given as arg #2 for variants present in the gene list for all input files
	 * Works just like extract, but only emits for the desired genes
	 * @param args
	 */
	private static void performGenePropExtract(String[] args, boolean reverse) {
		if (args.length < 4) {
			System.out.println("Enter the name of the file containing gene names, then one property to extract, then the variant files");
			return;
		}
		try {
			String key = args[2];
			GenePool genes = new GenePool(new File(args[1]));
			for(int i=3; i<args.length; i++) {
				VariantPool vars = getPool(new File(args[i]));
				VariantPool geneVars = filterByGene(vars, genes, reverse);
				
				for(String contig : geneVars.getContigs()) {
					for(VariantRec var : geneVars.getVariantsForContig(contig)) {
						System.out.println(var.getPropertyOrAnnotation(key));
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	private static void performHapCompare(String[] args) {
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
			List<String> falseNegList = new ArrayList<String>();
			
			for(String contig : hapmap.getContigs()) {
				for(VariantRec var : hapmap.getVariantsForContig(contig)) {
					VariantRec sampleVar = sample.findRecordNoWarn(contig, var.getStart());
					if (var.isVariant() && sampleVar == null) {
						totHapMapVar++;
						falseNeg++;
						falseNegList.add(contig + "\t" + var.getStart() + "\t" + var.getRef() + "\t" + var.getAlt());
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
			
			//System.out.println("False negatives:");
//			for(String str : falseNegList) {
//				System.out.println(str);
//			}
			
			System.out.println("False positives:");
			
			falsePosPool.listAll(System.out, Arrays.asList(new String[]{VariantRec.FS_SCORE, VariantRec.FALSEPOS_PROB, VariantRec.TAUFP_SCORE}));
			
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performCompare(String[] args) {
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
	}

	private static void performComputeFP(String[] args) {
		try {
			List<String> annos = new ArrayList<String>();
			annos.add(VariantRec.FALSEPOS_PROB);
			
			System.out.print(VariantRec.getSimpleHeader() );
			for(String key : annos) 
				System.out.print("\t" + key);
			System.out.println();
			
			for(int i=1; i<args.length; i++) {		
				VariantLineReader reader;
				if (args[i].endsWith(".csv")) 
					reader = new CSVLineReader(new File(args[i]));
				else
					reader = new VCFLineParser(new VCFFile(new File(args[i])));
				
				do {
					VariantRec rec = reader.toVariantRec();
					Double fpScore = FPComputer.computeFPScore(rec);
					if (! Double.isNaN(fpScore)) {
						rec.addProperty(VariantRec.FALSEPOS_PROB, fpScore);
					}
					System.out.println(rec.toSimpleString() + "\t" + rec.getProperty(VariantRec.FALSEPOS_PROB));
				} while(reader.advanceLine());
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performIntersection(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the names of two (or more) variant (vcf or csv) files to intersect");
			return;
		}

		int tried = 0;
		int found = 0;
		
		try {
			VariantLineReader vars = getReader(args[1]);
			
			System.out.println(vars.getHeader().trim());
			
			VariantPool bigPool = getPool(new File(args[2]));
			do {
				VariantRec var  = vars.toVariantRec();
				tried++;
				if (bigPool.findRecordNoWarn(var.getContig(), var.getStart()) != null) {
					System.out.println(vars.getCurrentLine());
					found++;
				}
			} while(vars.advanceLine());
			

			System.err.println("Found " + found + " intersecting variants in pool of size " + tried);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performSubtraction(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter the names of two or more variant (vcf or csv) files to subtract.");
			System.out.println("If more than two, result will be A - B - C... - N, so all subsequence files are subtracted from first given file");
			return;
		}
		
		List<VariantPool> pools = new ArrayList<VariantPool>();
		
		for(int i=2; i<args.length; i++) {
			try {
				pools.add( getPool(new File(args[i])) );
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
		}
		
		try {
			VariantLineReader baseVars = getReader(args[1]);
			System.out.println(baseVars.getHeader().trim());
			do {
				VariantRec var = baseVars.toVariantRec();
				
				boolean subtract = false;
				for(VariantPool pool : pools) {
					if (pool.findRecordNoWarn(var.getContig(), var.getStart()) != null) {
						subtract = true;
						break;
					}
				}
				
				if (! subtract) {
					System.out.println(baseVars.getCurrentLine());
				}
								
			} while(baseVars.advanceLine());
			
			
			
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void performHomFilter(String[] args) {
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

	private static void performBedFilter(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of a variant (vcf or csv) file and a bed file to filter by");
			return;
		}

		int count = 0;
		int retained = 0;
		
		try {
			VariantLineReader reader = getReader(args[1]);
			BEDFile bedFile = new BEDFile(new File(args[2]));
			bedFile.buildIntervalsMap();

			
			System.out.println(reader.getHeader().trim());
			
			do {
				
				VariantRec var = reader.toVariantRec();
//				if (count%5000==0)
//					System.err.println("Processed " + count + " variants (including " + retained + " so far)");
				if (bedFile.contains(var.getContig(), var.getStart(), false))	{
					retained++;
					System.out.println(reader.getCurrentLine());
				}
				count++;

			} while(reader.advanceLine());
						
	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.err.println("Retained " + retained + " of " + count + " variants");
		return;
	}

	private static void performHetFilter(String[] args) {
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

	private static void performGeneComp(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files for which to build the pool");
			return;
		}
		
		List<File> varFiles = new ArrayList<File>();
		
		int cutoff = 2;
		int startIndex = 1;
		try {
			cutoff = Integer.parseInt(args[1]);
			startIndex = 2;
			System.err.println("Found cutoff : " + cutoff);
		}
		catch (NumberFormatException nfe) {
			
		}
		
		for(int i=startIndex; i<args.length; i++) {
			varFiles.add(new File(args[i]));
		}
		
		try {
			compareGeneSets(cutoff, varFiles);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}

	private static void performInterestingFilter(String[] args) {
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
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		//annoKeys.add(VariantRec.VQSR);
		//annoKeys.add(VariantRec.FALSEPOS_PROB);
		//annoKeys.add(VariantRec.FS_SCORE);
		StringBuilder header = new StringBuilder( VariantRec.getSimpleHeader() );
		for(String key : annoKeys) 
			header.append("\t" + key);
		System.out.println( header );
		
		try {
			double frequencyCutoff = 0.01;
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
			
			for(String contig : pool.getContigs()) {
				for(VariantRec var : pool.getVariantsForContig(contig)) {
					boolean passes = false;
					
					if (var.getProperty(VariantRec.POP_FREQUENCY) == null || (var.getProperty(VariantRec.POP_FREQUENCY) != null && var.getProperty(VariantRec.POP_FREQUENCY) < frequencyCutoff)) {
						passes = true;
						
						//If variant was found in ESP5400 and with a relatively high frequency, it doesn't pass
						if (var.getProperty(VariantRec.EXOMES_FREQ) != null && var.getProperty(VariantRec.EXOMES_FREQ) > frequencyCutoff) {
							passes = false;
						}
					}
					
					
					
					String varType = var.getAnnotation(VariantRec.VARIANT_TYPE);
					String exonFunc = var.getAnnotation(VariantRec.EXON_FUNCTION);
					
					//Exclude variants that are not in exons or splicing 
					if (passes && !(varType != null && (varType.contains("exon") || varType.contains("splic")))) {
						passes = false;
					}
					
					if (passes && (exonFunc != null && exonFunc.trim().startsWith("synony"))) {
						passes = false;
					}
					
					if ((var.getAnnotation(VariantRec.OMIM_ID) != null && var.getAnnotation(VariantRec.OMIM_ID).length()>3) || (var.getAnnotation(VariantRec.HGMD_INFO) != null && var.getAnnotation(VariantRec.HGMD_INFO).length()>3)) {
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

	private static void performExtract(String[] args) {
		String prop = args[1];
		VariantPool pool;
		for(int i=2; i<args.length; i++) {
			try {

				pool = getPool(new File(args[i]));
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String val = var.getPropertyOrAnnotation(prop);
						System.out.println(val);
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return;
	}

	private static void performSampleCount(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to emit sample counts for");
			return;
		}
		
		try {
			VariantPool union = new VariantPool();
			
			for(int i=1; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec uVar = union.findRecordNoWarn(var.getContig(), var.getStart());
					if (uVar == null) {
						var.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						union.addRecord(var);
					}
					else {
						Double count = uVar.getProperty(VariantRec.SAMPLE_COUNT);
						count++;
						uVar.addProperty(VariantRec.SAMPLE_COUNT, count);
					}
				} while(reader.advanceLine());
				
			}
			
			
			PrintStream outputStream = System.out;
			
			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.VQSR);
			annoKeys.add(VariantRec.DEPTH);
			annoKeys.add(VariantRec.VAR_DEPTH);
			annoKeys.add(VariantRec.FALSEPOS_PROB);
			annoKeys.add(VariantRec.FS_SCORE);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.SAMPLE_COUNT);
			annoKeys.add(VariantRec.EFFECT_PREDICTION2);
			union.listAll(outputStream, annoKeys);
			outputStream.close();
			System.err.println("Final pool has " + union.size() + " variants");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	private static void performCombine(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to combine");
			return;
		}
		
		try {
			VariantPool pool = getPool(new File(args[1]));
			for(int i=2; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec existing = pool.findRecordNoWarn(var.getContig(), var.getStart());
					if (existing == null) {
						pool.addRecord(var);
						pool.sortAllContigs();
					}
				} while(reader.advanceLine());
				pool.sortAllContigs();
			}
			
			
			PrintStream outputStream = System.out;
			
			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.VQSR);
			annoKeys.add(VariantRec.DEPTH);
			annoKeys.add(VariantRec.VAR_DEPTH);
			annoKeys.add(VariantRec.FALSEPOS_PROB);
			annoKeys.add(VariantRec.FS_SCORE);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.EFFECT_PREDICTION2);
			pool.listAll(outputStream, annoKeys);
			outputStream.close();
			System.err.println("Final pool has " + pool.size() + " variants");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	
	
	private static void performRemoveDuplicates(String[] args) {
		if (args.length < 2) {
			System.out.println("Enter the names of one or more variant (vcf or csv) files to combine");
			return;
		}
		
		try {
			VariantPool pool = getPool(new File(args[1]));
			for(int i=2; i<args.length; i++) {
				System.err.println("Adding file : " + args[i]);
				VariantLineReader reader = getReader(args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					pool.addRecord(var);
				} while(reader.advanceLine());
				pool.sortAllContigs();
			}
			
			
			pool.removeDuplicates();
			
			PrintStream outputStream = System.out;
			
			List<String> annoKeys = new ArrayList<String>();
			annoKeys.add(VariantRec.RSNUM);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.GENE_NAME);
			annoKeys.add(VariantRec.VARIANT_TYPE);
			annoKeys.add(VariantRec.EXON_FUNCTION);
			annoKeys.add(VariantRec.CDOT);
			annoKeys.add(VariantRec.PDOT);
			annoKeys.add(VariantRec.VQSR);
			annoKeys.add(VariantRec.DEPTH);
			annoKeys.add(VariantRec.VAR_DEPTH);
			annoKeys.add(VariantRec.FALSEPOS_PROB);
			annoKeys.add(VariantRec.FS_SCORE);
			annoKeys.add(VariantRec.POP_FREQUENCY);
			annoKeys.add(VariantRec.EFFECT_PREDICTION2);
			pool.listAll(outputStream, annoKeys);
			outputStream.close();
			System.err.println("Final pool has " + pool.size() + " variants");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return;
	}
	
	

	private static void performSummary(String[] args) {
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
//		System.out.println("Histogram of read depths :");
//		System.out.println(readDepthHisto.toString());
		//System.out.println(varDepthHisto.freqsToCSV());
		
		return;
	}

	private static void performHistogram(String[] args) {
		String prop = args[1];
		LazyHistogram hist = new LazyHistogram(20);
		VariantPool pool;
		try {
			for(int i=2; i<args.length; i++) {
				pool = getPool(new File(args[i]));
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						String val = var.getPropertyOrAnnotation(prop);
						if (val != null && (!val.equals("-"))) {
							try {
								Double x = Double.parseDouble(val);
								hist.addValue(x);
							}
							catch (NumberFormatException nfe) {
								System.err.println("ERROR: could not parse value from : " + val);
							}
						}

					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
		System.out.println(hist.toString());
		return;
	}

	/**
	 * Perform frequency comparison to 1000 Genomes
	 * @param args
	 */
	private static void performFreqComp(String[] args) {
		if (args.length < 3) {
			System.out.println("Enter a BED file and then several variant files you'd like to calculate frequency from");
			return;
		}
		
		BEDFile regions = new BEDFile(new File(args[1]));
		try {
			regions.buildIntervalsMap();


			System.err.println("Merging files and counting samples...");
			VariantPool pool = getPool(new File(args[2]));
			for(int i=3; i<args.length; i++) {
				VariantLineReader reader = getReader(args[i]);
				System.err.println("Merging " + args[i]);
				do {
					VariantRec var = reader.toVariantRec();
					VariantRec existing = pool.findRecordNoWarn(var.getContig(), var.getStart());
					if (existing == null) {
						pool.addRecord(var);
						pool.sortAllContigs();
					}
					else {
						Double count = existing.getProperty(VariantRec.SAMPLE_COUNT);
						if (count == null) {
							existing.addProperty(VariantRec.SAMPLE_COUNT, 1.0);
						}
						else {
							existing.addProperty(VariantRec.SAMPLE_COUNT, count+1.0);
						}
					}
				} while(reader.advanceLine());
				System.err.println("..done, pool has " + pool.size() + " variants");
				pool.sortAllContigs();
			}
			
			
			//System.err.println("Done merging...");
			
			System.out.println("Gene\tpdot\tmtscore\tppscore\tgerpscore\tsiftscore\ttkgfreq\tamrfreq\tsampleFreq");
			
			//Create DBNSFP reader....
			DBNSFPReader reader = new DBNSFPReader();
			
			for(String contig : regions.getContigs() ) {
				if (contig.length() > 2) {
					System.err.println("Skipping weird contig " + contig);
					continue;
				}
				for(Interval interval : regions.getIntervalsForContig(contig)) {
					
					int start = interval.begin;
					int end = interval.end;
					reader.advanceTo(contig, start);
					while(reader.getCurrentPos() < end) {
						Double tkgFreq = reader.getValue(DBNSFPReader.TKG_AF);
						if (tkgFreq == null || Double.isNaN(tkgFreq))
							tkgFreq = 0.0;
						
						Double amrFreq = reader.getValue(DBNSFPReader.TKG_AMR);
						if (amrFreq == null || Double.isNaN(amrFreq))
							amrFreq = 0.0;
						
						VariantRec rec = pool.findRecordNoWarn(contig, reader.getCurrentPos());
						Double sampleCount = 0.0;
						if (rec != null) {
								sampleCount = rec.getProperty(VariantRec.SAMPLE_COUNT);
								if (sampleCount == null)
									sampleCount = 0.0;
						}
						
						double sampleFreq = sampleCount / (double)(args.length-2.0);
						
						double mtScore = reader.getValue(DBNSFPReader.MT);
						double ppScore = reader.getValue(DBNSFPReader.PP);
						double gerpScore = reader.getValue(DBNSFPReader.GERP);
						double siftScore = reader.getValue(DBNSFPReader.SIFT);
						String pDot = reader.getPDot();
						
						if (amrFreq > 0 || tkgFreq > 0.0 )
							System.out.println(reader.getText(DBNSFPReader.GENE) + "\t" + pDot + "\t" + mtScore + "\t" + ppScore + "\t" + gerpScore + "\t" + siftScore + "\t" + tkgFreq + "\t" + amrFreq + "\t" + sampleFreq);
						
						reader.advanceLine();
					}
				}
			}
		
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
	}
	
	private static void performFilter(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		
		boolean greater = true;
		if (args[0].equals("ufilter"))
			greater = false;
		
		String prop = args[1];
		double val = Double.parseDouble(args[2]);
		try {
			for(int i=3; i<args.length; i++) {
				VariantPool pool = getPool(new File(args[i]));
				VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						Double varVal;
						if (prop.startsWith("quality"))
							varVal = var.getQuality();
						else
							varVal = var.getProperty(prop);

						if (prop.startsWith("var.freq")) {
							Double totDepth = var.getProperty(VariantRec.DEPTH);
							Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
							if (totDepth != null && varDepth != null) {
								varVal = varDepth / totDepth;
							}
						}

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
				annoKeys.add(VariantRec.DEPTH);
				annoKeys.add(VariantRec.EFFECT_PREDICTION2);
				//annoKeys.add(VariantRec.PUBMED_SCORE);
				annoKeys.add(VariantRec.FALSEPOS_PROB);
				annoKeys.add(VariantRec.TAUFP_SCORE);
				annoKeys.add(VariantRec.FS_SCORE);
				annoKeys.add(VariantRec.MT_SCORE);
				annoKeys.add(VariantRec.POLYPHEN_SCORE);
				annoKeys.add(VariantRec.SIFT_SCORE);
				annoKeys.add(VariantRec.GERP_SCORE);
				annoKeys.add(VariantRec.PHYLOP_SCORE);
				
				annoKeys.add(prop);
				if (greater)
					System.err.println("Found " + filteredVars.size() + " vars with " + prop + " above " + val);
				else 
					System.err.println("Found " + filteredVars.size() + " vars with " + prop + " below " + val);
				
				if (args.length==4)
					filteredVars.listAll(System.out, annoKeys);
				else {
					//Write results to a file
					String outfilename = args[i].replace(".vcf", "").replace(".csv", "") + ".flt.csv";
					PrintStream out = new PrintStream(new FileOutputStream(new File(outfilename)));
					System.err.println("Emitting vars from file " + args[i] + " to : " + outfilename);
					filteredVars.listAll(out, annoKeys);
					out.close();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private static void performEmit(String[] args) {
		if (args.length < 4) {
			System.out.println("Enter the property to filter by and the cutoff value");
			return;
		}
		String prop = args[1];
		
		List<String> annoKeys = new ArrayList<String>();
		annoKeys.add(VariantRec.RSNUM);
		annoKeys.add(VariantRec.POP_FREQUENCY);
		annoKeys.add(VariantRec.GENE_NAME);
		annoKeys.add(VariantRec.VARIANT_TYPE);
		annoKeys.add(VariantRec.EXON_FUNCTION);
		annoKeys.add(VariantRec.CDOT);
		annoKeys.add(VariantRec.PDOT);
		annoKeys.add(VariantRec.VQSR);
		annoKeys.add(VariantRec.DEPTH);
		annoKeys.add(VariantRec.EFFECT_PREDICTION2);
		//annoKeys.add(VariantRec.PUBMED_SCORE);
		annoKeys.add(VariantRec.FALSEPOS_PROB);
		annoKeys.add(VariantRec.TAUFP_SCORE);
		annoKeys.add(VariantRec.FS_SCORE);
		annoKeys.add(VariantRec.MT_SCORE);
		annoKeys.add(VariantRec.POLYPHEN_SCORE);
		annoKeys.add(VariantRec.SIFT_SCORE);
		annoKeys.add(VariantRec.GERP_SCORE);
		annoKeys.add(VariantRec.PHYLOP_SCORE);
		annoKeys.add(prop);
		
		boolean greater = true;
		
		System.out.print("source" + "\t" + VariantRec.getSimpleHeader());
		for(String key : annoKeys) {
			System.out.print("\t" + key);
		}
		System.out.println();
		
		
		
		double val = Double.parseDouble(args[2]);
		try {
			for(int i=3; i<args.length; i++) {
				VariantPool pool = getPool(new File(args[i]));
				int count = 0;
				//VariantPool filteredVars = new VariantPool();
				for(String contig : pool.getContigs()) {
					for(VariantRec var : pool.getVariantsForContig(contig)) {
						Double varVal;
						if (prop.startsWith("quality"))
							varVal = var.getQuality();
						else
							varVal = var.getProperty(prop);

						if (prop.startsWith("var.freq")) {
							Double totDepth = var.getProperty(VariantRec.DEPTH);
							Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
							if (totDepth != null && varDepth != null) {
								varVal = varDepth / totDepth;
							}
						}

						if (varVal == null) {
							System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
							count++;
						}
						else {
							if ( (!greater) && varVal < val) {
								System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
								count++;
							}
							if (greater && varVal > val) {
								System.out.println(var.getAnnotation(VariantRec.SOURCE) + "\t" + var.toSimpleString() + "\t" + var.getPropertyString(annoKeys));
								count++;
							}
						}
					}
				}
				
				System.err.println(args[i] + " total hits: " + count);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}



