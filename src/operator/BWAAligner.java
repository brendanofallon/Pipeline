package operator;

public class BWAAligner extends IOCommandOp {
	
	
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected int threads = defaultThreads;

	@Override
	protected String getCommand() {
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsPath = inputBuffers.get(1).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();

		String command = pathToBWA + " aln -t " + threads + " " + referencePath + " " + readsPath;
		return command;
	}

}
