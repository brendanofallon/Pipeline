package operator.gatk;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.DOCMetrics;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import operator.CommandOperator;
import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class DepthOfCoverage extends IOOperator {

	public final String defaultMemOptions = " -Xms2g -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected static int[] cutoffs = new int[]{5, 8, 10, 15, 20, 50};
	
	public boolean requiresReference() {
		return true;
	}
	
	
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) Pipeline.getPropertyStatic(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		
		FileBuffer inputBuffer = getInputBufferForClass(BAMFile.class);
		FileBuffer referenceFile = getInputBufferForClass(ReferenceFile.class);
		String inputPath = inputBuffer.getAbsolutePath();
			
		FileBuffer bedFile = getInputBufferForClass(BEDFile.class);
		String bedFilePath = "";
		if (bedFile != null) {
			bedFilePath = bedFile.getAbsolutePath();
		}
		
		String outputPrefix = inputBuffer.getFile().getName();
		outputPrefix = outputPrefix.replace("_final", "");
		outputPrefix = outputPrefix.replace(".bam", ".DOC");
		String projHome = Pipeline.getPipelineInstance().getProjectHome();
		if (projHome != null && projHome.length() > 0) {
			outputPrefix = projHome + outputPrefix;
		}
		

		logger.info("Computing depth of coverage for file : " + inputBuffer.getAbsolutePath());
		
		String bedPath = "null";
		if (bedFile != null)
			bedPath = bedFile.getAbsolutePath();
		logger.info("DepthOfCoverage operator " + getObjectLabel() + " is calculating using bedfile: " + bedPath + " and output prefix: " + outputPrefix);
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + referenceFile.getAbsolutePath() + 
				" -I " + inputPath + 
				" -T DepthOfCoverage" +
				" --omitDepthOutputAtEachBase ";
		for(int i=0; i<cutoffs.length; i++) {
			command = command + " -ct " + cutoffs[i];
		}
		command = command + " -o " + outputPrefix;
		
		if (inputBuffer.getContig() != null) {
			command = command + " -L " + inputBuffer.getContig() + " ";
		}
		if (inputBuffer.getContig() == null && bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		
		
		executeCommand(command);
		
		//Read output from summary file
		File summaryFile = new File(outputPrefix + ".sample_summary");
		DOCMetrics metrics = new DOCMetrics();
		metrics.setSourceFile( inputBuffer.getFile().getName() );
		try {
			BufferedReader reader = new BufferedReader(new FileReader(summaryFile));
			String first = reader.readLine();
			String second = reader.readLine();
			String[] toks = second.split("\t");
			double mean = Double.parseDouble(toks[2]);
			metrics.setMeanCoverage(mean);
			double[] fractions = new double[cutoffs.length];
			
			for(int i=0; i<cutoffs.length; i++) {
				double fraction = Double.parseDouble(toks[i+6]);
				fractions[i] = fraction;
			}
			metrics.setFractionAboveCutoff(fractions);
			metrics.setCutoffs(cutoffs);
			System.out.println( metrics );
			for(int i=0; i<cutoffs.length; i++) {
				System.out.println(" % bases above " + cutoffs[i] + " : " + metrics.getFractionAboveCutoff()[i]);
			}
			
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error reading output file : " + summaryFile.getAbsolutePath(), this);
		}
		
		System.out.println("Computed metrics : " + metrics);
		super.addOutputBuffer( metrics );
	}

}
