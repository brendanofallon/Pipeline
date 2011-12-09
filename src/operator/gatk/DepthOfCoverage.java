package operator.gatk;

import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class DepthOfCoverage extends CommandOperator {

	public final String defaultMemOptions = " -Xms512m -Xmx2g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
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
		

		String bedPath = "null";
		if (bedFile != null)
			bedPath = bedFile.getAbsolutePath();
		logger.info("DepthOfCoverage operator " + getObjectLabel() + " is calculating using bedfile: " + bedPath + " and output prefix: " + outputPrefix);
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + referenceFile.getAbsolutePath() + 
				" -I " + inputPath + 
				" -T DepthOfCoverage" +
				" --omitDepthOutputAtEachBase ";
		command = command + " -o " + outputPrefix;
		
		if (inputBuffer.getContig() != null) {
			command = command + " -L " + inputBuffer.getContig() + " ";
		}
		if (inputBuffer.getContig() == null && bedFile != null)
			command = command + " -L:intervals,BED " + bedFilePath;
		return command;
	}

}
