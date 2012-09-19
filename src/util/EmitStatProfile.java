package util;

import java.io.File;

import util.bamreading.BamWindow;

import math.Histogram;

import buffer.BAMFile;

public class EmitStatProfile {

	
	
	public static void emitProfile(BAMFile inputBAM) {
		BamWindow window = new BamWindow(inputBAM.getFile());
				
		Histogram coverageHisto = new Histogram(0, 500, 50);
		Histogram insertSizeHisto = new Histogram(0, 500, 50);
		
		for(int i=5000000; i<100000000; i+=10) {
			window.advanceTo("1", i);
			if (window.getCoverage()> 4) {
				int cov = window.getCoverage();
				double insert = window.meanInsertSize();
				coverageHisto.addValue(cov);
				insertSizeHisto.addValue(insert);
				System.out.println(i + "\t" + cov + "\t" + insert);
			}
		}
		
		System.out.println("Coverage histo: \n " + coverageHisto.toString());
		System.out.println("Insert size histo: \n " + insertSizeHisto.toString());
	}
	
	public static void main(String[] args) {
		
		BAMFile input = new BAMFile(new File("/media/MORE_DATA/HHT/bams/HHT22.final.bam"));
		
		EmitStatProfile.emitProfile(input);
	}
}
