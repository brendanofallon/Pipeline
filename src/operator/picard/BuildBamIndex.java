package operator.picard;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class BuildBamIndex extends CommandOperator {

	public static final String PATH = "path";
	public static final String CREATE_INDEX = "createindex";
	protected String defaultPicardDir = "~/picard-tools-1.55/";
	protected String picardDir = defaultPicardDir;
	protected boolean defaultCreateIndex = true;
	protected boolean createIndex = defaultCreateIndex;
	
	@Override
	protected String getCommand() {
	
		Object path = Pipeline.getPropertyStatic(PipelineXMLConstants.PICARD_PATH);
		if (path != null)
			picardDir = path.toString();
		
		//User can override path specified in properties
		String userPath = properties.get(PATH);
		if (userPath != null) {
			 picardDir = userPath;
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
