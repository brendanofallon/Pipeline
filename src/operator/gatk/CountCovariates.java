package operator.gatk;

import java.util.List;

import operator.CommandOperator;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

/**
 * Uses the GATK to count the covariates
 * @author brendan
 *
 */
public class CountCovariates extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 4;
	
	@Override
	protected String getCommand() {
		
		
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
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		List<FileBuffer> knownSitesFiles = getAllInputBuffersForClass(VCFFile.class);
		
		StringBuffer knownSitesStr = new StringBuffer();
		for(FileBuffer buff : knownSitesFiles) {
			knownSitesStr.append("-knownSites " + buff.getAbsolutePath() + " ");
		}
		
		
		String csvOutput = outputBuffers.get(0).getAbsolutePath();
		
		String covariateList = "-cov QualityScoreCovariate -cov CycleCovariate -cov DinucCovariate -cov MappingQualityCovariate ";
		
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + " -R " + reference + " -I " + inputFile + " -T CountCovariates -nt " + threads + " " + covariateList + " " + knownSitesStr.toString() + " -recalFile " + csvOutput;
		return command;
	}

}
