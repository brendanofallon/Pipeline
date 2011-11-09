package operator;

public class BuildBamIndex extends CommandOperator {

	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	
	@Override
	protected String getCommand() {
	
		String path = properties.get(PATH);
		if (path != null) {
			 picardDir = path;
		}
		if (picardDir.endsWith("/")) {
			picardDir = picardDir.substring(0, picardDir.length()-1);
		}
		
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		//String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "java -Xms2g -Xmx16g -jar " + picardDir + "/BuildBamIndex.jar" + " INPUT=" + inputPath + " VALIDATION_STRINGENCY=LENIENT";
		return command;
	}

}
