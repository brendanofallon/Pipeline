package fpClassifier;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import cern.jet.random.Binomial;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;
import math.ContinuousDistribution;
import math.Histogram;
import math.UnitDistribution;

public class Classifier {

	ContinuousDistribution distOne;
	ContinuousDistribution distTwo;
	
	FreqStore freqStore;
	
	public Classifier(File dataFile, ContinuousDistribution distOne, ContinuousDistribution distTwo) throws IOException {
		this.distOne = distOne;
		this.distTwo = distTwo;
		freqStore = new FreqStore();
		freqStore.readFromFile(dataFile);
	}
	
	public double getDistOneLikelihood(double[] vals) {
		double logL = 0.0;
		for(int i=0; i<vals.length; i++) {
			logL += Math.log( distOne.getPDF(vals[i]));
		}
		return logL;
	}
	
	public double getDistTwoLikelihood(double[] vals) {
		double logL = 0.0;
		for(int i=0; i<vals.length; i++) {
			logL += Math.log( distTwo.getPDF(vals[i]));
		}
		return logL;
	}
	
	public double getProbOneAtPos(String contig, int pos) {
		double[] freqs = freqStore.getFreqs(contig, pos);
		return getDistOneLikelihood(freqs);
	}
	
	public double getProbTwoAtPos(String contig, int pos) {
		double[] freqs = freqStore.getFreqs(contig, pos);
		return getDistTwoLikelihood(freqs);
	}
	
	public double getRatioForVariant(VariantRec var) {
		if (freqStore.hasEntry(var.getContig(), var.getStart())) {
			double probNovel = getProbOneAtPos(var.getContig(), var.getStart());
			double probKnown = getProbTwoAtPos(var.getContig(), var.getStart());
			
			double varFreq = var.getProperty(VariantRec.VAR_DEPTH) / var.getProperty(VariantRec.DEPTH);
			probNovel += Math.log( distOne.getPDF( varFreq) );
			probKnown += Math.log( distTwo.getPDF( varFreq) );
			
			return probNovel - probKnown;
		}
		else {
			return 1.0;
		}
	}
	
	public double getFalsePosPosterior(VariantRec var) {
		final double q = 0.5; //Prior probability that variant is not false-positive
		
		double probNovel = 1.0;
		double probKnown = 1.0;
		if (freqStore.hasEntry(var.getContig(), var.getStart())) {
			probNovel = getProbOneAtPos(var.getContig(), var.getStart());
			probKnown = getProbTwoAtPos(var.getContig(), var.getStart());
		}

		double varFreq = var.getProperty(VariantRec.VAR_DEPTH) / var.getProperty(VariantRec.DEPTH);
		probNovel += Math.log( distOne.getPDF( varFreq) );
		probKnown += Math.log( distTwo.getPDF( varFreq) );

		probNovel = Math.exp(probNovel);
		probKnown = Math.exp(probKnown);

		return probNovel*(1.0-q) / (probKnown*q + probNovel*(1-q));
	}
	
	public FreqStore getFreqStore() {
		return freqStore;
	}
	
	public static void main(String[] args) {
		
		//args = new String[1];
		//args[0] = "/media/MORE_DATA/detect_fp/HHT26_testvars.csv";
		
		UnitDistribution novelDist = new UnitDistribution(novelVarFreqs);
		UnitDistribution knownDist = new UnitDistribution(knownVarFreqs);
		
		File freqDB = new File("/media/MORE_DATA/detect_fp/novel_csvs/novel_exome_db_noHHT26.csv");
		
		Classifier classifier;
		try {
			classifier = new Classifier(freqDB, novelDist, knownDist);
			
//			classifier.getFreqStore().emitFreqDistro();
//			System.exit(0);
			
			int hits = 0;
			
			VariantPool pool = new VariantPool(new CSVFile(new File(args[0])));
			VariantPool falseposPool = new VariantPool();
			
			Histogram posteriorDist = new Histogram(0, 1, 25);
			
			for(String contig : pool.getContigs()) {
				for(VariantRec var : pool.getVariantsForContig(contig)) {
					//double ratio = classifier.getRatioForVariant(var);
					double posterior = classifier.getFalsePosPosterior(var);
					if ( classifier.getFreqStore().hasEntry(var.getContig(), var.getStart())) {
						hits++;
						var.addProperty(VariantRec.FALSEPOS_PROB, posterior);
						if (posterior > 0.75)
							falseposPool.addRecordNoSort(var);
						posteriorDist.addValue(posterior);
					}
				}
			}
			
			System.err.println("Total hits : " + hits);
			System.err.println("Histogram of posteriors probs:\n" + posteriorDist.toString());
			
			falseposPool.sortAllContigs();
			List<String> keys = new ArrayList<String>();
			keys.add(VariantRec.DEPTH);
			keys.add(VariantRec.VAR_DEPTH);
			keys.add(VariantRec.FALSEPOS_PROB);
			falseposPool.listAll(System.out, keys);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		
	}
	
	
	
	
	public static final double[] knownVarFreqs = new double[]{0.0004,0.0009,0.0009,0.0012,0.0024,0.0046,0.006,0.007,0.0072,0.0091,0.0074,0.0093,0.0101,0.0102,0.0128,0.0124,0.0172,0.018,0.0235,0.0326,0.0321,0.0437,0.0476,0.0514,0.0634,0.0366,0.0383,0.0311,0.0236,0.0202,0.0111,0.0106,0.0066,0.0086,0.005,0.0038,0.0031,0.0044,0.0025,0.0035,0.002,0.003,0.0031,0.0033,0.0044,0.0049,0.0073,0.0135,0.0326,0.2854};
	public static final double[] novelVarFreqs = new double[]{0.0094,0.0172,0.0188,0.0253,0.0471,0.0597,0.0594,0.0561,0.0414,0.0485,0.0269,0.0309,0.036,0.0228,0.0264,0.0204,0.0428,0.0206,0.0221,0.0291,0.0223,0.0246,0.0247,0.0237,0.0539,0.0161,0.0177,0.018,0.0143,0.0139,0.0081,0.0086,0.0056,0.011,0.0053,0.0042,0.0026,0.0056,0.0027,0.0033,0.0018,0.0024,0.002,0.0016,0.0017,0.0013,0.0013,0.0013,0.0015,0.0377};
}
