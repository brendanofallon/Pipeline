package operator;

/**
 * Uses samtools to convert a SAM file to a BAM file
 * @author brendan
 *
 */
public class ConvertSamBam extends IOCommandOp {


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
				
		//Ouput handled automagically!
		String command = samPath + " view -Sb " + inputPath;
		return command;
	}

}
