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
		int dupCount = 0;
		int failsVendorQuality = 0;
		
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
			
			int insertSize = Math.abs( samRecord.getInferredInsertSize() );
			insertSizeHisto.addValue(insertSize);
				
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
		
		return metrics;
	}
	
	public String getBAMMetricsSummary(BAMMetrics metrics) {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		StringBuilder sum = new StringBuilder("Summary for .bam file: " + metrics.path);
		String lineSep = System.getProperty("line.separator");
		sum.append("Total number of reads : " + metrics.totalReads + lineSep);
		
		sum.append("Number of unmapped reads : " + metrics.unmappedReads + " ( " + formatter.format(100.0*metrics.unmappedReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of reads with unmapped mates : " + metrics.unmappedMates + " ( " + formatter.format(100.0*metrics.unmappedMates / metrics.totalReads) + "% )"+ lineSep);
		sum.append("Number of duplicate reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.duplicateReads / metrics.totalReads) + "% )" + lineSep);
		sum.append("Number of low vendor quality reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.lowVendorQualityReads / metrics.totalReads) + "% )" + lineSep);
		sum.append(" Distribution of insert sizes : " + lineSep);
		sum.append(metrics.insertSizeHistogram.toString() + lineSep);
		sum.append(" Mean insert size:" + formatter.format(metrics.insertSizeHistogram.getMean()) + lineSep);		
		sum.append(" Stdev insert size:" + formatter.format(metrics.insertSizeHistogram.getStdev()) + lineSep );
		sum.append(" Insert size range: " + metrics.insertSizeHistogram.getMinValueAdded() + " - " + metrics.insertSizeHistogram.getMaxValueAdded() + lineSep );
		return sum.toString();
	}
	
	public static class BAMMetrics {
		String path;
		int totalReads;
		Histogram insertSizeHistogram;
		int unmappedReads;
		int unmappedMates;
		int duplicateReads;
		int lowVendorQualityReads;
		
	}

}
