package operator.gatk;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class VariantRecalibrator extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	protected String[] getCommands() {
		
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
		String inputFile = inputBuffers.get(1).getAbsolutePath();
		FileBuffer hapmapFile = inputBuffers.get(2);
		FileBuffer genomesFile = inputBuffers.get(3);
		FileBuffer dbsnpFile = inputBuffers.get(4);
			

		FileBuffer outputVCF = getOutputBufferForClass(VCFFile.class);
		
		String projHome = Pipeline.getPipelineInstance().getProjectHome();
		String recalFilePath = projHome + "vqsr.output.recal";
		String tranchesPath = projHome + "vqsr.output.tranches";
		String rScriptPath = projHome + "vqsr.output.plots.R";
		
				
		String recalCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		recalCommand = recalCommand + " -R " + reference + " -input " + inputFile + " -T VariantRecalibrator ";		
		recalCommand = recalCommand + "-resource:hapmap,known=false,training=true,truth=true,prior=15.0 " + hapmapFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-resource:omni,known=false,training=true,truth=false,prior=12.0 " + genomesFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-resource:dbsnp,known=true,training=false,truth=false,prior=8.0 " + dbsnpFile.getAbsolutePath() + " ";
		recalCommand = recalCommand + "-an QD -an HaplotypeScore -an MQRankSum -an ReadPosRankSum -an FS ";
		recalCommand = recalCommand + " --maxGaussians 5 ";
		recalCommand = recalCommand + "-recalFile " + recalFilePath + " ";
		recalCommand = recalCommand + "-tranchesFile " + tranchesPath + " ";
		//recalCommand = recalCommand + "-rscriptFile " + rScriptPath + " "; 
				   
		
		
		String applyCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		applyCommand = applyCommand + " -R " + reference + " -input " + inputFile + " -T ApplyRecalibration ";	
		applyCommand = applyCommand + " -recalFile " + recalFilePath + " ";
		applyCommand = applyCommand + " -tranchesFile " + tranchesPath + " ";
		applyCommand = applyCommand + " -o " + outputVCF.getAbsolutePath();
		
		return new String[]{recalCommand, applyCommand};
	}

	@Override
	protected String getCommand() throws OperationFailedException {
		//Nothing to do, we've overridden getCommands()
		return null;
	}
	
}
