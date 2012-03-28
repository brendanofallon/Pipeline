package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import math.Histogram;

import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class JoinVCFs {

	public static VariantRec firstVariant(String contig, List<VariantPool> pools) {
		List< List<VariantRec>> vars = new ArrayList<List<VariantRec>>();
		for(VariantPool pool : pools) {
			List<VariantRec> varList = pool.getVariantsForContig(contig);
			vars.add(varList);
		}
		
		VariantRec firstVariant = vars.get(0).get(0);
		for(List<VariantRec> varList : vars) {
			VariantRec testVar = varList.get(0);
			if (testVar.getStart() < firstVariant.getStart()) {
				firstVariant = testVar;
			}
		}
		
		return firstVariant;
	}
	
	public static VariantRec nextVariant(VariantRec thisVariant, List<VariantPool> pools) {
//		if (thisVariant.getStart() == 1049491) {
//			System.out.println("ouch");
//		}
		
		List<VariantRec> nextVars = new ArrayList<VariantRec>();
		for(VariantPool pool : pools) {
			VariantRec var = pool.nextVariant(thisVariant);
			nextVars.add(var);
		}
		
		//Now find which record comes next
		int startIndex = 0;
		VariantRec firstVar = nextVars.get(startIndex);
		while (firstVar == null && startIndex < nextVars.size()) {
			firstVar = nextVars.get(startIndex);
			startIndex++;
		}
		
		//All next vars are null, we're done
		if (firstVar == null)
			return null;
		
		for(int i=startIndex; i<nextVars.size(); i++) {
			VariantRec var = nextVars.get(i);
			if (var != null && var.getStart() < firstVar.getStart()) {
				firstVar = var;
			}
			
		}
		
		return firstVar;
	}
	
	

	private static void writeLineForVar(VariantRec var, List<VariantPool> pools, Histogram hist) {
		StringBuilder strBFirst = new StringBuilder();
		StringBuilder strBSecond = new StringBuilder();
		strBFirst.append(var.getContig() + "\t" + var.getStart() + "\t" + (var.getStart()+1) + "\t" + var.getRef() + "\t" + var.getAlt());
		
		int count = 0;
		for(VariantPool pool : pools) {
			VariantRec qVar = pool.findRecordNoWarn(var.getContig(), var.getStart());
			if (qVar == null)
				strBSecond.append("\t0.0\t0.0");
			else {
				//strBSecond.append("\t" + qVar.getProperty(VariantRec.DEPTH) + "\t" + qVar.getProperty(VariantRec.VAR_DEPTH));
				double freq = qVar.getProperty(VariantRec.VAR_DEPTH) / qVar.getProperty(VariantRec.DEPTH);
				//strBSecond.append("\t" + qVar.getProperty(VariantRec.DEPTH) + "\t" +  qVar.getProperty(VariantRec.VAR_DEPTH));
				strBSecond.append("\t" + freq);
				if (hist != null && qVar.getProperty(VariantRec.DEPTH) > 4) {
					hist.addValue(freq);
				}
				count++;
			}
		}
		
		//if (count > 1) {
			System.out.print(strBFirst);
			System.out.println("\t" + count + strBSecond);
		//}
	}
	
	public static void main(String[] args) {
		
//		args = new String[2];
//		args[0] = "/media/MORE_DATA/detect_fp/annotated_files/HHT19.novel.csv";
//		args[1] = "/media/MORE_DATA/detect_fp/annotated_files/HHT15.novel.csv";
		
		StringBuilder header = new StringBuilder("#contig\tstart\tend\tref\talt\tcount");
		
		List<VariantPool> pools = new ArrayList<VariantPool>();
		for(int i=0; i<args.length; i++) {
			CSVFile csvFile = new CSVFile(new File(args[i]));
			//header.append("\t" + args[i] +".depth\t" + args[i] + ".vardepth");
			header.append("\t" + args[i] +".varFreq");
			try {
				
				VariantPool pool = new VariantPool(csvFile);
				pools.add(pool);
				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		Histogram afHist = new Histogram(0, 1.0, 50);
		
		System.out.println(header);
		for(String contig : pools.get(0).getContigs()) {
			VariantRec var = firstVariant(contig, pools);
			while (var != null) {
				writeLineForVar(var, pools, afHist);
				var = nextVariant(var, pools);
			}
		}
		
		System.err.println(afHist.toString());
	}
	
	
}
