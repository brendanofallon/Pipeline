package util.varFreqDB;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import util.VCFLineParser;
import util.reviewDir.ReviewDirInfo;
import util.reviewDir.ReviewDirParseException;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class CompareVarFreqs {

	//Map from analysis type to collection of variants
	public static final String SAMPLES = "samples";
	public static final String HETS = "hets";
	public static final String HOMS = "homs";
	
	Map<String, PoolInfo> allVars = new HashMap<String, PoolInfo>();
	
	public void addSample(ReviewDirInfo info) {
		File vcf = info.getVCF();
		File bed = info.getBED();
		String analysis = info.getAnalysisType();
		if (analysis.contains("(")) {
			analysis = analysis.substring(0, analysis.indexOf("(")).trim();
		}
		
		PoolInfo pInfo = allVars.get(analysis);
		
		if (pInfo == null) {
			pInfo = new PoolInfo();
			pInfo.totalSamples = 1;
			allVars.put(analysis, pInfo);
		}
		else {
			pInfo.totalSamples = pInfo.totalSamples + 1;
		}
		
		VariantPool aVars = pInfo.pool;
		VariantPool varsToAdd = new VariantPool(); //Newly found vars are placed here, then dumped en masse into aVars when we're done reading
		try {
			VariantLineReader variantReader = new VCFLineParser(vcf);
			VariantRec var = variantReader.toVariantRec();
			while(var != null) {
				
				VariantRec existingVar = aVars.findRecordNoWarn(var.getContig(), var.getStart());
				if (existingVar != null) {
					existingVar.addProperty(SAMPLES, existingVar.getProperty(SAMPLES)+1);
					if (var.isHetero()) {
						existingVar.addProperty(HETS, existingVar.getProperty(HETS)+1);
					}
					else {
						existingVar.addProperty(HOMS, existingVar.getProperty(HOMS)+1);
					}
				}
				else {
					VariantRec newVar = new VariantRec(var.getContig(), var.getStart(), var.getEnd(), var.getRef(), var.getAlt());
					newVar.addProperty(SAMPLES, 1.0);
					if (var.isHetero()) {
						newVar.addProperty(HETS, 1.0);
						newVar.addProperty(HOMS, 0.0);
					}
					else {
						newVar.addProperty(HOMS, 1.0);
						newVar.addProperty(HETS, 0.0);
					}
					varsToAdd.addRecordNoSort(newVar);
				}
				
				
				variantReader.advanceLine();
				var = variantReader.toVariantRec();
			}
			
			aVars.addAll(varsToAdd);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}
	
	public void emitPool(PoolInfo poolInfo) {
		VariantPool pool = poolInfo.pool;
		System.out.println("##total.samples=" + poolInfo.totalSamples);
		for(String contig: pool.getContigs()) {
			for(VariantRec var : pool.getVariantsForContig(contig)) {
				System.out.println(contig + "\t" + var.getStart() + "\t" + var.getAlt() + "\t" + formatProperty(var.getPropertyOrAnnotation(HETS)) + "\t" + formatProperty(var.getPropertyOrAnnotation(HOMS)));
			}
		}
	}
	
	private static String formatProperty(String prop) {
		if (prop.equals("-")) {
			return "0.0";
		}
		else 
			return prop;
	}
	
	public void readSamplesInDir(File dir) throws ReviewDirParseException {
		File[] files = dir.listFiles();
		for(int i=0; i<files.length; i++) {
			if (files[i].isDirectory()) {
				ReviewDirInfo info = ReviewDirInfo.create(files[i].getAbsolutePath());
				addSample(info);
			}
		}
		
	}
	
	public void emitAll() {
		for(String type : allVars.keySet()) {
			System.out.println("##type="+ type);
			emitPool(allVars.get(type));
		}
	}
	
	public void emitTabulated() {
		List<String> types = new ArrayList<String>();
		types.addAll(allVars.keySet());
		
		//Make on big pool...
		System.err.println("Tabulating variants, this may take a moment....");
		VariantPool everything = new VariantPool();
		for(String type: types) {
			everything.addAll(allVars.get(type).pool);
		}
		
		int overallTotalSamples = 0;
		System.out.print("#chr\tpos\talt");
		for(String type : types) {
			System.out.print("\t" + type + "[" + allVars.get(type).totalSamples + "]");
			overallTotalSamples += allVars.get(type).totalSamples;
		}
		
		System.out.print("\toverall[" + overallTotalSamples + "]");
		System.out.println();
		
		for(String contig: everything.getContigs()) {
			for(VariantRec var : everything.getVariantsForContig(contig)) {
				System.out.print(contig + "\t" + var.getStart() + "\t" + var.getAlt());
				
				int totSamples = 0;
				int totHets = 0;
				int totHoms = 0;
				for(String type : types) {
					VariantPool pool = allVars.get(type).pool;
					VariantRec tVar = pool.findRecordNoWarn(contig, var.getStart());
					if (tVar != null) {
						System.out.print("\t" + tVar.getPropertyOrAnnotation(SAMPLES) + "," + tVar.getPropertyOrAnnotation(HETS) + "," + tVar.getPropertyOrAnnotation(HOMS));
						totSamples += tVar.getProperty(SAMPLES)!=null 
								? tVar.getProperty(SAMPLES) 
								: 0;
						totHets += tVar.getProperty(HETS)!=null 
								? tVar.getProperty(HETS) 
								: 0;
						totHoms += tVar.getProperty(HOMS)!=null 
								? tVar.getProperty(HOMS) 
								: 0;
					}
					else {
						System.out.print("\t0.0,0.0,0.0");
					}
				}
				System.out.print("\t" + totSamples + "," + totHets + "," + totHoms);
				System.out.println();
			}
		}
		
		
	}
	
	public static void main(String[] args) {
		CompareVarFreqs cFreqs = new CompareVarFreqs();
		
		for(int i=0; i<args.length; i++) {
			
			try {
				cFreqs.addSample( ReviewDirInfo.create(args[i]));
			} catch (ReviewDirParseException e) {
				System.err.println("Warning: Skipping file : " + args[i]  + " : " + e.getLocalizedMessage());
			}	
		}
		
		//cFreqs.addSample( ReviewDirInfo.create("/home/brendan/MORE_DATA/clinical_exomes/2013-05-20/trioB/13070300197"));
		//cFreqs.addSample( ReviewDirInfo.create("/home/brendan/MORE_DATA/clinical_exomes/2013-05-20/trioB/13070300200"));
		//cFreqs.addSample( ReviewDirInfo.create("/home/brendan/MORE_DATA/clinical_exomes/2013-05-20/trioA/13064545653"));
		cFreqs.emitTabulated();
	}
	
	
	class PoolInfo {
		int totalSamples = 0;
		VariantPool pool = new VariantPool();
	}
	
}
