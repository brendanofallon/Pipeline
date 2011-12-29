package operator.gatk;

import java.io.File;
import java.util.List;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.MultiOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Recalibrates base quality scores and applies the recalibration to a list of files
 * @author brendan
 *
 */
public class MultiCountCovariates extends MultiOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 4;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
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
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = inputBuffer.getAbsolutePath();
		List<FileBuffer> knownSitesFiles = getAllInputBuffersForClass(VCFFile.class);
		
		StringBuffer knownSitesStr = new StringBuffer();
		for(FileBuffer buff : knownSitesFiles) {
			knownSitesStr.append("-knownSites " + buff.getAbsolutePath() + " ");
		}
		
		
		String csvOutput = inputBuffer.getAbsolutePath() + ".recal.csv";
		
		String covariateList = "-cov QualityScoreCovariate -cov CycleCovariate -cov DinucCovariate -cov MappingQualityCovariate ";
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference + 
				" -I " + inputFile + 
				" -T CountCovariates " + 
				covariateList + " "	+ 
				knownSitesStr.toString() +
				" -recalFile " + csvOutput;
		
		//We also want to APPLY the recalibration after calculating it
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".recal.bam";
		
		String command2 = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference + 
				" -I " + inputFile + 
				" -T TableRecalibration " + 
				" -o " + outputPath + 
				" -recalFile " + csvOutput;
		
		BAMFile outputFile = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputFile);
		
		return new String[]{command, command2};
	}

}
