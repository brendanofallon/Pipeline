package operator;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class Genotyper extends CommandOperator {

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
		FileBuffer dbsnpFile = getInputBufferForClass(VCFFile.class);
			
		
		String outputVCF = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T UnifiedGenotyper";
		command = command + " -o " + outputVCF;
		if (dbsnpFile != null)
			command = command + " --dbsnp " + dbsnpFile.getAbsolutePath();
		command = command + " --dbsnp " + dbsnpFile;
		command = command + " -glm BOTH";
		command = command + " -stand_call_conf 30.0";
		command = command + " -stand_emit_conf 10.0";
		command = command + " -nt " + threads;
		return command;
	}

}
