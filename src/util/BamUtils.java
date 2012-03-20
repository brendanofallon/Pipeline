package util;

import java.io.File;

/**
 * A few command-line utilities for .bam (or .sam) files
 * This uses the samtools library frequently
 * 
 * @author brendan
 *
 */
public class BamUtils {

	public static void emitInsertSizeDistro(File inputBam) {
		
	}
	
	public static void emitUsage() {
		System.out.println(" BamUtils : Some utilities for examining / manipulating .bam files");
		
	}
	
	public static void main(String[] args) {
		
		if (args.length==0) {
			emitUsage();
			return;
		}
		
	}
}
