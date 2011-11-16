package operator.gatk;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class VariantRecalibrator extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 1;
	
	@Override
	protected String getCommand() {
		
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String threadsStr = properties.get(THREADS);
		if (threadsStr != null) {
			threads = Integer.parseInt(threadsStr);
		}
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null)
			jvmARGStr = "";
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		FileBuffer hapmapFile = inputBuffers.get(2);
		FileBuffer genomesFile = inputBuffers.get(3);
		FileBuffer dbsnpFile = inputBuffers.get(4);
			
		
		String outputRecal = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T VariantRecalibrator";		
		command = command + "-resource:hapmap,known=false,training=true,truth=true,prior=15.0 " + hapmapFile.getAbsolutePath() + " ";
		command = command + "-resource:omni,known=false,training=true,truth=false,prior=12.0 " + genomesFile.getAbsolutePath() + " ";
		command = command + "-resource:dbsnp,known=true,training=false,truth=false,prior=8.0 " + dbsnpFile.getAbsolutePath() + " ";
		command = command + "-an QD -an HaplotypeScore -an MQRankSum -an ReadPosRankSum -an FS -an MQ ";
		command = command + "-recalFile " + outputRecal + " ";
		command = command + "-tranchesFile output.tranches ";
		command = command + "-rscriptFile output.plots.R ";
				   
		return command;
	}
	
}
