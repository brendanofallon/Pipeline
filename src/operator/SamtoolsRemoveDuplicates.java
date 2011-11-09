package operator;

/**
 * Use samtools to remove duplicates from the input file
 * @author brendan
 *
 */
public class SamtoolsRemoveDuplicates extends PipedCommandOp {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String fileIsSam = "";
		if (inputPath.endsWith("sam"))
			fileIsSam = " -S ";
		
		String command = samPath + " rmdup " + fileIsSam + inputPath + " " + outputPath;
		return command;
	}

}
