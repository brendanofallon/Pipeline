package operator;

public class Simulator extends PipedCommandOp {

	public static final String PATH = "path";
	public static final String READ_PAIRS = "readpairs";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	protected int readPairs = 100000;
	
	
	@Override
	protected String getCommand() {
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String rpStr = properties.get(READ_PAIRS);
		if (rpStr != null) {
			readPairs = Integer.parseInt(rpStr);
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		
		String mutatedSites = outputBuffers.get(0).getAbsolutePath(); 
		String outputReads1 = outputBuffers.get(1).getAbsolutePath();
		String outputReads2 = outputBuffers.get(2).getAbsolutePath();
		
		
		//Ouput handled automagically!
		String command = samPath + "wgsim " + inputPath + " " + outputReads1 + " " + outputReads2;
		return command;
	}

}
