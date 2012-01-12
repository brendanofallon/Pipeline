package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import pipeline.Pipeline;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.TextBuffer;
import buffer.VCFFile;

/**
 * A class to build a little summary of the results of an exome alignment / variant call
 * @author brendan
 *
 */
public class ExomeSummary extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		String referencePath = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		//BEDFile captureFile = (BEDFile) getInputBufferForClass(BEDFile.class);
		//BAMFile inputBAM = (BAMFile) getInputBufferForClass(BAMFile.class);
		VCFFile inputVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
		TextBuffer output = (TextBuffer) getOutputBufferForClass(TextBuffer.class);
		
		//boolean hasBAM = inputBAM != null;
		boolean hasVCF = inputVCF != null;
		
		StringBuffer summary = new StringBuffer();
		
//		if (hasBAM) {
//			StringBuffer bamSummary = buildBAMSummary(referencePath, inputBAM);
//			summary.append(bamSummary.toString());
//		}
		
		if (hasVCF) {
			StringBuffer vcfSummary;
			try {
				vcfSummary = buildVCFSummary(referencePath, inputVCF);
				summary.append(vcfSummary.toString());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(output.getFile()));
			writer.write(summary + "\n");
			writer.close();
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not write to exome summary file " + output.getAbsolutePath() + " \n" + e.getMessage());			
		}

		
		System.out.println(summary.toString());
		
	}

	private StringBuffer buildVCFSummary(String referencePath, VCFFile inputVCF) throws IOException {
		
		StringBuffer buf = new StringBuffer();
		buf.append("Summary for vcf file : " + inputVCF.getAbsolutePath() + "\n");
		VCFLineParser vParser= new VCFLineParser(inputVCF.getFile());
		
		int totVars =0 ;
		int totHets = 0;
		int passingVars =0 ;
		int passingHets = 0;
		double totQualSum = 0;
		double passingQualSum = 0;
		
		while(vParser.advanceLine()) {
			totVars++;
			if (vParser.isHetero())
				totHets++;
			totQualSum += vParser.getQuality();
			if (vParser.isPassing()) {
				passingVars++;
				passingQualSum += vParser.getQuality();
				if (vParser.isHetero()) {
					passingHets++;
				}
			}
		}
		
		buf.append("Total number of variants		: " + totVars + "\n");
		buf.append("Total number of heterozygotes	: " + totHets + "\n");
		buf.append("Total number of homozygotes		: " + (totVars-totHets) + "\n");
		buf.append("Mean quality					: " + totQualSum / (double)totVars + "\n");
		
		buf.append("Number of passing variants		: " + passingVars + "\n");
		buf.append("Number of passing hets			: " + passingHets + "\n");
		buf.append("Number of passing homs			: " + (passingVars-passingHets) + "\n");
		buf.append("Mean quality of passing sites	: " + passingQualSum / (double)passingVars + "\n");
		return buf;
	}

	private StringBuffer buildBAMSummary(String referencePath, BAMFile inputBAM) {
		// TODO Auto-generated method stub
		return null;
	}


	private int lineCount(FileBuffer file) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(file.getAbsolutePath()));
			int count = 0;
			String line = reader.readLine();
			while (line != null) {
				if (! line.startsWith("#"))
					count++;
				line = reader.readLine();
				
			}
			return count;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not read from file " + file.getAbsolutePath() + " to count number of lines\n" + e.getMessage());
			return 0;
		} catch (IOException e) {
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not read from file " + file.getAbsolutePath() + " to count number of lines\n" + e.getMessage());
			return 0;
		}
		
	}

}
