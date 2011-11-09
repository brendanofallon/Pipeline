package operator;

/**
 * Uses Picard to coorindate sort a BAM file
 * @author brendan
 *
 */
public class CoordinateSort extends CommandOperator {

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
		
		String createIndxStr = properties.get(CREATE_INDEX);
		if (createIndxStr != null) {
			Boolean b = Boolean.parseBoolean(createIndxStr);
			createIndex = b;
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "java -Xms2g -Xmx8g -jar " + picardDir + "/SortSam.jar" + " INPUT=" + inputPath + " OUTPUT=" + outputPath + " SORT_ORDER=coordinate VALIDATION_STRINGENCY=LENIENT CREATE_INDEX=" + createIndex + " ";
		return command;
	}

}
