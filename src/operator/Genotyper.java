package operator;

import buffer.BAMFile;
import buffer.CSVFile;
import buffer.ReferenceFile;
import buffer.VCFFile;

public class Genotyper extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected int threads = 16;
	
	@Override
	protected String getCommand() {
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String dbsnpFile = getInputBufferForClass(VCFFile.class).getAbsolutePath();
		
		
		String outputVCF = outputBuffers.get(0).getAbsolutePath();
				
		String command = "java " + defaultMemOptions + " -jar " + gatkPath;
		command = command + " -R " + reference + " -I " + inputFile + " -T UnifiedGenotyper";
		command = command + " -o " + outputVCF;
		command = command + " --dbsnp " + dbsnpFile;
		command = command + " --glm BOTH";
		command = command + " -stand_call_conf 30.0";
		command = command + " -stand_emit_conf 10.0";
		command = command + " -nt " + threads;
		return command;
	}

}
