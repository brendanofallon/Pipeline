package operator.qc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import math.Histogram;
import math.LazyHistogram;
import operator.OperationFailedException;
import operator.Operator;
import operator.python.CreateFigure;
import operator.qc.BamMetrics.BAMMetrics;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.ElapsedTimeFormatter;
import util.StringWriter;
import buffer.BAMFile;
import buffer.DOCMetrics;
import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Builds a small html report containing some QC information. 
 * @author brendan
 *
 */
public class QCReport extends Operator {

	public static final String QC_STYLE_SHEET = "qc.style.sheet";
	
	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMFile rawBAMFile = null;
	BAMFile finalBAMFile = null;
	//VCFFile variantsFile = null;
	VariantPool variantPool = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		File homeDir = new File( getProjectHome() );
		if ( (! homeDir.exists()) || (! homeDir.isDirectory()) ) {
			throw new OperationFailedException("Could not open project home directory : " + homeDir.getAbsolutePath(), this);
		}
	
		if (rawBAMFile == null) {
			throw new OperationFailedException("No raw bam file specified", this);
		}
		
		if (finalBAMFile == null) {
			throw new OperationFailedException("No final bam file specified", this);
		}
		
		if (variantPool == null) {
			throw new OperationFailedException("No variant pool specified", this);
		}
		
		BAMMetrics rawMetrics = BamMetrics.computeBAMMetrics(rawBAMFile);
		BAMMetrics finalMetrics = BamMetrics.computeBAMMetrics(finalBAMFile);
		
		
		
		logger.info("Creating qc report for raw bam file:" + rawBAMFile.getAbsolutePath() + "\n final BAM: " + finalBAMFile.getAbsolutePath() + " variant pool with:" + variantPool.size() + " variants");
		
		String projHome = getProjectHome();				
		try {
			File outputDir = new File(projHome + "qc-report");
			outputDir.mkdir();
			
			String outputPath = outputDir.getAbsolutePath();
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath + "/qc-metrics.html"));
			QCPageWriter pageWriter = new QCPageWriter();
			
			//Write summary (index) page
			StringWriter summary = new StringWriter();
			writeSummary(summary, getPipelineOwner());
			pageWriter.writePage(writer, summary.toString());
			writer.close();
			
			//Write base qualities page...
			writer = new BufferedWriter(new FileWriter(outputPath + "/basequalities.html"));
			StringWriter basequalities = new StringWriter();
			writeBaseQualities(basequalities, rawMetrics, outputDir);
			pageWriter.writePage(writer, basequalities.toString());
			writer.close();
			
			//Write alignment metrics page...
			writer = new BufferedWriter(new FileWriter(outputPath + "/alignment.html"));
			StringWriter alnWriter = new StringWriter();
			
			writeBAMMetricsBlock(alnWriter, rawMetrics, rawCoverageMetrics, "Metrics for raw bam file " + rawBAMFile.getFilename(), outputDir);			
			writeBAMMetricsBlock(alnWriter, finalMetrics, finalCoverageMetrics, "Metrics for final bam file " + finalBAMFile.getFilename(), outputDir);
			pageWriter.writePage(writer, alnWriter.toString());
			writer.close();
			
			//Writer variant report
			writer = new BufferedWriter(new FileWriter(outputPath + "/variants.html"));
			StringWriter variants = new StringWriter();
			writeVariantReport(variants, variantPool, outputDir);
			pageWriter.writePage(writer, variants.toString());
			writer.close();
			
			//Finally, copy style sheet to directory...
			String styleSheetPath = getPipelineProperty(QC_STYLE_SHEET);

			if (styleSheetPath != null) {

				File styleDir = new File(outputPath + "/styles");
				styleDir.mkdir();

				File styleSheetSrc = new File(styleSheetPath);
				if (! styleSheetSrc.exists()) {
					logger.warning("QC style sheet at path " + styleSheetSrc.getAbsolutePath() + " does not exist");
					return;
				}
				
				File styleSheetDest = new File(styleDir.getAbsolutePath() + "/style.css");
				copyFile(styleSheetSrc, styleSheetDest);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Could not write qc eport : " + e.getMessage(), this);
		}
		
	}
	



	

	private void writeVariantReport(StringWriter writer, VariantPool varPool, File outputDir) throws IOException {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");
		
		writer.write("<h2> Raw variant summary </h2>");
		writer.write("<p> Total number of variants identified : " + varPool.size() + " </p>" + lineSep );
		writer.write("<p> Number of contigs : " + varPool.getContigCount() + " </p>" + lineSep );
		writer.write("<p> Number of SNPs : " + varPool.countSNPs() + " </p>" + lineSep );
		
		writer.write("<p> Number of insertions : " + varPool.countInsertions() + " </p>" + lineSep );
		writer.write("<p> Number of deletions : " + varPool.countDeletions() + " </p>" + lineSep );
		String heteroFraction = formatter.format(varPool.countHeteros() / (double)varPool.size());
		writer.write("<p> Number of heterozygotes : " + varPool.countHeteros() + " ( " + heteroFraction + ") </p>" + lineSep );
		writer.write("<p> Ts/ Tv ratio : " + formatter.format(varPool.computeTTRatio()) + " </p>" + lineSep );
		writer.write("<p> Mean variant quality : " + varPool.meanQuality() + " </p>" + lineSep );

		VariantPool novels = new VariantPool();
		VariantPool knowns = new VariantPool();
		for(String contig : varPool.getContigs()) {
			for(VariantRec var : varPool.getVariantsForContig(contig)) {
				Double freq = var.getProperty(VariantRec.POP_FREQUENCY);
				if (freq != null && freq > 1e-5)
					knowns.addRecordNoSort(var);
				else
					novels.addRecordNoSort(var);
			}
		}
				
		
		writer.write("<h2> Known variant summary </h2>");
		if (knowns.size()==0) {
			writer.write("<p> No population frequency computed, cannot determine known / novel properties </p>" + lineSep );
		}
		else {
			writer.write("<p> Total number of novel variants : " + knowns.size() + " </p>" + lineSep );
			writer.write("<p> Number of contigs : " + knowns.getContigCount() + " </p>" + lineSep );
			writer.write("<p> Number of SNPs : " + knowns.countSNPs() + " </p>" + lineSep );	
			writer.write("<p> Number of insertions : " + knowns.countInsertions() + " </p>" + lineSep );
			writer.write("<p> Number of deletions : " + knowns.countDeletions() + " </p>" + lineSep );
			heteroFraction = formatter.format(knowns.countHeteros() / (double)knowns.size());
			writer.write("<p> Number of heterozygotes : " + knowns.countHeteros() + " ( " + heteroFraction + ") </p>" + lineSep );
			writer.write("<p> Ts/ Tv ratio : " + formatter.format(knowns.computeTTRatio()) + " </p>" + lineSep );
			writer.write("<p> Mean variant quality : " + knowns.meanQuality() + " </p>" + lineSep );

			writer.write("<h2> Novel variant summary </h2>");
			writer.write("<p> Total number of known variants : " + novels.size() + " </p>" + lineSep );
			writer.write("<p> Number of contigs : " + novels.getContigCount() + " </p>" + lineSep );
			writer.write("<p> Number of SNPs : " + novels.countSNPs() + " </p>" + lineSep );

			writer.write("<p> Number of insertions : " + novels.countInsertions() + " </p>" + lineSep );
			writer.write("<p> Number of deletions : " + novels.countDeletions() + " </p>" + lineSep );
			heteroFraction = formatter.format(novels.countHeteros() / (double)novels.size());
			writer.write("<p> Number of heterozygotes : " + novels.countHeteros() + " ( " + heteroFraction + ") </p>" + lineSep );
			writer.write("<p> Ts/ Tv ratio : " + formatter.format(novels.computeTTRatio()) + " </p>" + lineSep );
			writer.write("<p> Mean variant quality : " + novels.meanQuality() + " </p>" + lineSep );
		}
		
		
		
		Histogram histo = new Histogram(0, 1, 50);
		LazyHistogram overallQualHisto = new LazyHistogram(100);
		LazyHistogram snpQualHisto = new LazyHistogram(100);
		LazyHistogram hetQualHisto = new LazyHistogram(100);
		LazyHistogram homoQualHisto = new LazyHistogram(100);
		LazyHistogram indelQualHisto = new LazyHistogram(100);
		
		for(String contig : varPool.getContigs()) {
			for(VariantRec var : varPool.getVariantsForContig(contig)) {
				Double varDepth = var.getProperty(VariantRec.VAR_DEPTH);
				Double depth = var.getProperty(VariantRec.DEPTH);
				if (varDepth != null && depth != null) {
					Double frac = varDepth / depth;
					histo.addValue(frac);
				}
				
				overallQualHisto.addValue(var.getQuality());
				if (var.isSNP()) {
					snpQualHisto.addValue(var.getQuality());
				}
				if (var.isIndel())
					indelQualHisto.addValue(var.getQuality());
				if (var.isHetero()) 
					hetQualHisto.addValue(var.getQuality());
				else {
					homoQualHisto.addValue(var.getQuality());
				}
			}
		}
		
		String figName = "vardepthfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
		String figPath = outputDir.getAbsolutePath() + "/" + figName;
		CreateFigure.generateFigure(getPipelineOwner(), histo, "Fraction", "Allele fraction", "Frequency", figPath);
		writer.write("<h2> Distribution of variant depths: " + " </h2>" +lineSep);
		writer.write("<img src=\"" + figName + "\">");
		
		
		List<Histogram> histos = new ArrayList<Histogram>();
		histos.add(overallQualHisto.getHistogram());
		histos.add(snpQualHisto.getHistogram());
		histos.add(indelQualHisto.getHistogram());
		histos.add(hetQualHisto.getHistogram());
		histos.add(homoQualHisto.getHistogram());
		
		List<String> labels = new ArrayList<String>();
		labels.add("All variants");
		labels.add("SNPs only");
		labels.add("Indels only");
		labels.add("Heterozygotes only");
		labels.add("Homozygotes only");
		
		if (histo.getCount() > 0) {
			figName = "varqualfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
			figPath = outputDir.getAbsolutePath() + "/" + figName;
			CreateFigure.generateFigure(getPipelineOwner(), histos, labels, "Allele fraction", "Frequency", figPath);
			writer.write("<h2> Distribution of variant qualities: " + " </h2>" +lineSep);
			writer.write("<img src=\"" + figName + "\">");
		}
		else {
			writer.write("<p> (No variant depth information found): " + " </p>" +lineSep);
		}
		
	}


	private void writeBaseQualities(StringWriter writer, BAMMetrics metrics, File outputDir) throws IOException {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");

		writer.write("<h2> Raw base qualities </h2>");

		writer.write("<p> Bases with quality > 30 : " + metrics.basesQAbove30 + " ( " + formatter.format(100.0*metrics.basesQAbove30 / metrics.basesRead) + "% )" +" </p>" + lineSep );
		writer.write("<p> Bases with quality > 20 : " + metrics.basesQAbove20 + " ( " + formatter.format(100.0*metrics.basesQAbove20 / metrics.basesRead) + "% )" + " </p>" +lineSep );
		writer.write("<p> Bases with quality > 10 : " + metrics.basesQAbove10 + " ( " + formatter.format(100.0*metrics.basesQAbove10 / metrics.basesRead) + "% )" + " </p>" +lineSep );
		writer.write(" Mean quality :" + formatter.format(metrics.baseQualityHistogram.getMean()) + " </p>" +lineSep);		
		writer.write(" Stdev quality:" + formatter.format(metrics.baseQualityHistogram.getStdev()) + " </p>" +lineSep );

//		String rpFigStr =  "rpfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
//		String rpFigFullPath = outputDir.getAbsolutePath() + "/" + rpFigStr;
//		CreateFigure.generateHistoImage(metrics.readPosQualHistos, "Read position", "Quality score distribution", rpFigFullPath);
//		writer.write("<h2> Base qualities by read position: " + " </h2>" +lineSep);
//		writer.write("<img src=\"" + rpFigStr + "\">");
		
		
		String bqFigStr =  "bqfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
		String bqFigFullPath = outputDir.getAbsolutePath() + "/" + bqFigStr;
		CreateFigure.generateFigure(getPipelineOwner(), metrics.baseQualityHistogram, "Quality score", "Base qualities", "Frequency", bqFigFullPath);
		writer.write("<h2> Distribution of base qualities: " + " </h2>" +lineSep);
		writer.write("<img src=\"" + bqFigStr + "\">");
	}


	private void writeSummary(Writer writer, Pipeline ppl) throws IOException {
		writer.write("<h2> Pipeline run summary </h2>");
		String lineSep = System.getProperty("line.separator");
		writer.write(" <p> Report creation time : " + (new Date()).toString() + " </p> " + lineSep);
		writer.write(" <p> Pipeline input file : " + ppl.getSourceFile().getName() + " </p> " +  lineSep);
		writer.write(" <p> Input file modified date: " + new Date(ppl.getSourceFile().lastModified()).toString() + " </p> " + lineSep);
		Date startTime = ppl.getStartTime();
		writer.write(" <p> Project home directory : " + ppl.getProjectHome() + " </p> " + lineSep);
		writer.write(" <p> Pipeline execution start time : " + startTime.toString() + " </p> " + lineSep);
		
		Date now = new Date();
		String formattedElapsedTime = ElapsedTimeFormatter.getElapsedTime(startTime.getTime(), now.getTime());
		writer.write(" <p> Pipeline elapsed time : " + formattedElapsedTime + " </p> " + lineSep);
		
		
		writer.write("<h2> Operator summary </h2>");
		List<Operator> opList = ppl.getOperatorList();
		
		//Operator summary table...
		writer.write("<table border=\"0\" padding=\"5\" width=\"600\"> <tr>	<th>Operator </th>	<th> Duration </th>	 <th> Completion state </th></tr> ");
		for(Operator op : opList) {
			
			writer.write(" <tr> ");
			String timeStr = "?";
			String startStr = op.getAttribute(Pipeline.START_TIME);
			Long startMS = 0L;
			if (startStr != null) {
				startMS = Long.parseLong(startStr);
			}
			String endStr = op.getAttribute(Pipeline.END_TIME);
			Long endMS = 0L;
			if (endStr != null) {
				endMS = Long.parseLong(endStr);
			}
			
			System.out.println("Found start time: " + startMS );
			System.out.println("Found end time: " + endMS );
			
			if (endMS > 0)
				timeStr = ElapsedTimeFormatter.getElapsedTime(startMS, endMS);
			
			writer.write(" <td> " + op.getObjectLabel() + "</td>" + lineSep);
			writer.write(" <td> " + timeStr + "</td>" + lineSep);
			writer.write(" <td> " + op.getState() + "</td>" + lineSep);
			writer.write(" </tr> ");
		}
		writer.write("</table>");
		
		writer.write("<h2> Pipeline properties </h2>");
		writer.write("<table border=\"0\" padding=\"5\" width=\"700\"> ");
		for(Object keyObj : ppl.getPropertyKeys()) {
			String key = keyObj.toString();
			Object value = ppl.getProperty(key);
			if (value != null) {
				writer.write(" <tr> ");
				writer.write(" <td> " + key + "</td>" + lineSep);
				writer.write(" <td> " + value.toString() + "</td>" + lineSep);
				writer.write(" </tr> ");
			}
		}
		writer.write("</table>");
	}


	private void writeBAMMetricsBlock(Writer writer, BAMMetrics metrics, DOCMetrics docMetrics, String header, File outputDir) throws IOException {
		DecimalFormat formatter = new DecimalFormat("#0.00");
		String lineSep = System.getProperty("line.separator");
		writer.write("<div class=\"bammetrics\">");
		writer.write("<h2> " + header + "</h2>");
		writer.write("Total number of reads : " + metrics.totalReads + lineSep);
		
		writer.write("<p> Number of unmapped reads : " + metrics.unmappedReads + " ( " + formatter.format(100.0*metrics.unmappedReads / metrics.totalReads) + "% )" + " </p>" + lineSep);
		writer.write("<p> Number of reads with unmapped mates : " + metrics.unmappedMates + " ( " + formatter.format(100.0*metrics.unmappedMates / metrics.totalReads) + "% )"+ " </p>" +lineSep);
		writer.write("<p> Number of duplicate reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.duplicateReads / metrics.totalReads) + "% )" + " </p>" +lineSep);
		writer.write("<p> Number of low vendor quality reads : " + metrics.duplicateReads + " ( " + formatter.format(100.0*metrics.lowVendorQualityReads / metrics.totalReads) + "% )" + " </p>" +lineSep);
		writer.write("<p> Number of pairs with insert size > 10K : " + metrics.hugeInsertSize + " </p>" +lineSep);
		
		writer.write("<h2>  Coverage statistics : " + " </h2>" +lineSep);
		if (docMetrics == null) {
			writer.write("<p id=\"error\">  No coverage information found </p>" +lineSep);
		}
		else {
			writer.write("<p>  Mean overall coverage:" + formatter.format(docMetrics.getMeanCoverage()) + " </p>" + lineSep);
			if (docMetrics.getCutoffs() == null) {
				writer.write("<p id=\"error\">  Count not find cutoff values! </p>" +lineSep);
			}
			else {
				for(int i=0; i<docMetrics.getCutoffs().length; i++) {
					double val  = docMetrics.getFractionAboveCutoff()[i];
					writer.write("<p>  Fraction of bases with coverage > " + docMetrics.getCutoffs()[i] + " : " + formatter.format(val) + " </p>" +lineSep);
				}
			}
			
			if (docMetrics.getFlaggedIntervals() == null) {
				writer.write("<p> Number of low coverage intervals : No information found </p>" +lineSep);
			}
			else {
				writer.write("<p> Number of low coverage intervals : " + docMetrics.getFlaggedIntervals().size() + " </p>" +lineSep);
				for(String intervalInfo : docMetrics.getFlaggedIntervals()) {
					writer.write("<p>chr" + intervalInfo + " </p>" +lineSep);
				}
			
			}
			
			
		}
		
		writer.write("<h2>  Distribution of insert sizes : " + " </h2>" +lineSep);
		
		writer.write("<p>  Mean insert size:" + formatter.format(metrics.insertSizeHistogram.getMean()) + " </p>" +lineSep);		
		writer.write("<p>  Stdev insert size:" + formatter.format(metrics.insertSizeHistogram.getStdev()) + " </p>" +lineSep );
		writer.write("<p>  Insert size range: " + metrics.insertSizeHistogram.getMinValueAdded() + " - " + metrics.insertSizeHistogram.getMaxValueAdded() + " </p>" + lineSep );

		String figStr =  "metricsfig-" + ("" + System.currentTimeMillis()).substring(6) + ".png";
		String figFullPath = outputDir.getAbsolutePath() + "/" + figStr;
		CreateFigure.generateFigure(getPipelineOwner(), metrics.insertSizeHistogram, "Insert size (bp)", "Insert size", "Frequency", figFullPath);
		
		writer.write("<img src=\"" + figStr + "\">");		
		
		//writer.write(metrics.baseQualityHistogram.toString() +" </p>" + lineSep);
		writer.write("</div>");
	}


	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					if (rawBAMFile == null)
						rawBAMFile = (BAMFile)obj;
					else if (finalBAMFile == null) {
						finalBAMFile= (BAMFile)obj;
						System.out.println("Found final bam file : " + finalBAMFile.getAbsolutePath());
					}
					else
						throw new IllegalArgumentException("Too many input bam files to QC report");
				}
				
				if (obj instanceof DOCMetrics) {
					if (rawCoverageMetrics == null)
						rawCoverageMetrics = (DOCMetrics) obj;
					else {
						finalCoverageMetrics = (DOCMetrics) obj;
					}
				}
				if (obj instanceof VCFFile) {
					throw new IllegalArgumentException("Got a straight-up VCF file as input to QC metrics, this now needs to be a variant pool.");
				}
				if (obj instanceof VariantPool) {
					variantPool = (VariantPool)obj;
				}
				// ?
			}
		}
	}

	/**
	 * Fancy nio-enabled file copying tool...
	 * @param sourceFile
	 * @param destFile
	 * @throws IOException
	 */
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
	
}

