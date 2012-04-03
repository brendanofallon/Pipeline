package operator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.logging.Logger;

import pipeline.Pipeline;

import math.Histogram;
import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.TextBuffer;

/**
 * Computes some simple summary metrics for a bam file
 * @author brendan
 *
 */
public class BamMetrics extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		FileBuffer inputBAM = super.getInputBufferForClass(BAMFile.class);
		if (inputBAM == null) {
			throw new OperationFailedException("No input bam file specified", this);
		}
		
		logger.info("Computing summary metrics for input bam file " + inputBAM.getAbsolutePath());
		
		FileBuffer outputFile = getOutputBufferForClass(TextBuffer.class);
		
		
		BAMMetrics metrics = computeBAMMetrics( (BAMFile)inputBAM);
		
		String metricsSummary = getBAMMetricsSummary(metrics);
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.getFile()));
			writer.write(metricsSummary);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error writing to output file: " + outputFile.getAbsolutePath(), this);
		}
		
		logger.info("Done computing summary metrics for input bam file " + inputBAM.getAbsolutePath());
	}
	
	public static BAMMetrics computeBAMMetrics(BAMFile inputBAM) {
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		final SAMFileReader inputSam = new SAMFileReader(inputBAM.getFile());
		inputSam.setValidationStringency(ValidationStringency.LENIENT);
		

		int readCount = 0;
		int unmappedCount = 0;
		int unmappedMate = 0;
		Histogram insertSizeHisto = new Histogram(0, 1000, 100);
		Histogram baseQHisto = new Histogram(0, 41, 41);
		int dupCount = 0;
		int failsVendorQuality = 0;
		int hugeInsertSize = 0;
		long basesAbove30 = 0;
		long basesAbove20 = 0;
		long basesAbove10 = 0;
		long totalBaseCount = 0;
		
		for (final SAMRecord samRecord : inputSam) {
			readCount++;
						
			if (samRecord.getMateUnmappedFlag())
				unmappedMate++;
			if (samRecord.getReadUnmappedFlag())
				unmappedCount++;
			if (samRecord.getReadFailsVendorQualityCheckFlag())
				failsVendorQuality++;
			if (samRecord.getDuplicateReadFlag())
				dupCount++;
			
			byte[] baseQuals = samRecord.getBaseQualities();
			for(int i=0; i<baseQuals.length; i++) {
				if (baseQuals[i] > 30)
					basesAbove30++;
				if (baseQuals[i] > 20)
					basesAbove20++;
				if (baseQuals[i] > 10)
					basesAbove10++;
				baseQHisto.addValue(baseQuals[i]);
				
			}
			totalBaseCount += baseQuals.length;
			int insertSize = Math.abs( samRecord.getInferredInsertSize() );
			if (insertSize > 10000) {
				hugeInsertSize++;
			}
			else {
				insertSizeHisto.addValue(insertSize);
			}				
		}


		inputSam.close();
		
		BAMMetrics metrics = new BAMMetrics();
		metrics.path = inputBAM.getFile().getAbsolutePath();
		metrics.totalReads = readCount;
		metrics.unmappedReads = unmappedCount;
		metrics.duplicateReads = dupCount;
		metrics.unmappedMates = unmappedMate;
		metrics.lowVendorQualityReads = failsVendorQuality;
		metrics.insertSizeHistogram = insertSizeHisto;
		metrics.baseQualityHistogram = baseQHisto;
		metrics.hugeInsertSize = hugeInsertSize;
		metrics.basesQAbove10 = basesAbove10;
		metrics.basesQAbove20 = basesAbove20;
		metrics.basesQAbove30 = basesAbove30;
		metrics.basesRead = totalBaseCount;
		return metrics;
	}
	
	public static String getBAMMetricsSummary(BAMMetrics metrics) {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");
		StringBuilder sum = new StringBuilder("Summary for .bam file: " + metrics.path + lineSep);
		sum.append("Total number of reads : " + metrics.totalReads + lineSep);
		
		sum.append("Number of unmapped reads : " + metrics.unmappedReads + " ( " + formatter.format(100.0*metrics.unmappedReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of reads with unmapped mates : " + metrics.unmappedMates + " ( " + formatter.format(100.0*metrics.unmappedMates / metrics.totalReads) + "% )"+ lineSep);
		sum.append("Number of duplicate reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.duplicateReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of low vendor quality reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.lowVendorQualityReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of pairs with insert size > 10K : " + metrics.hugeInsertSize + lineSep);
		
		sum.append("Bases with quality > 30 : " + metrics.basesQAbove30 + " ( " + formatter.format(100.0*metrics.basesQAbove30 / metrics.basesRead) + "% )" + lineSep );
		sum.append("Bases with quality > 20 : " + metrics.basesQAbove20 + " ( " + formatter.format(100.0*metrics.basesQAbove20 / metrics.basesRead) + "% )" + lineSep );
		sum.append("Bases with quality > 10 : " + metrics.basesQAbove10 + " ( " + formatter.format(100.0*metrics.basesQAbove10 / metrics.basesRead) + "% )" + lineSep );
		
		sum.append(" Distribution of insert sizes : " + lineSep);
		sum.append(metrics.insertSizeHistogram.toString() + lineSep);
		sum.append(" Mean insert size:" + formatter.format(metrics.insertSizeHistogram.getMean()) + lineSep);		
		sum.append(" Stdev insert size:" + formatter.format(metrics.insertSizeHistogram.getStdev()) + lineSep );
		sum.append(" Insert size range: " + metrics.insertSizeHistogram.getMinValueAdded() + " - " + metrics.insertSizeHistogram.getMaxValueAdded() + lineSep );
		
		sum.append("Distribution of base qualities: " + lineSep);
		sum.append(metrics.baseQualityHistogram.toString() + lineSep);
		sum.append(" Mean quality :" + formatter.format(metrics.baseQualityHistogram.getMean()) + lineSep);		
		sum.append(" Stdev quality:" + formatter.format(metrics.baseQualityHistogram.getStdev()) + lineSep );
		
		
		return sum.toString();
	}
	
	public static class BAMMetrics {
		String path;
		int totalReads;
		Histogram insertSizeHistogram;
		Histogram baseQualityHistogram;
		int unmappedReads;
		int unmappedMates;
		int duplicateReads;
		int lowVendorQualityReads;
		int hugeInsertSize;
		long basesRead;
		long basesQAbove30;
		long basesQAbove20;
		long basesQAbove10;
		
	}

}
