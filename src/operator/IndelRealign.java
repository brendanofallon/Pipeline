package operator;

import buffer.BAMFile;
import buffer.CSVFile;
import buffer.ReferenceFile;

public class IndelRealign extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	protected String getCommand() {
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		String intervalsFile = getInputBufferForClass(CSVFile.class).getAbsolutePath();
		
		String realignedBam = outputBuffers.get(0).getAbsolutePath();
		
		String command = "java " + defaultMemOptions + " -jar " + gatkPath + " -R " + reference + " -I " + inputFile + " -T IndelRealigner -targetIntervals " + intervalsFile + " -o " + realignedBam;
		return command;
	}

}
