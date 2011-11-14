package operator.annovar;

import operator.CommandOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use annovar to produce variant annotation files. 
 * Right now this assumes that in the annovar directory there is a database directory called "humandb"
 * that contains all required db info 
 * @author brendan
 *
 */
public class AnnovarAnnotate extends CommandOperator {

	
	public static final String BUILD_VER = "buildver";
	protected String annovarPath = "~/annovar/";
	protected String buildVer = "hg19";
	
	
	@Override
	protected String getCommand() {
		Object path = Pipeline.getPropertyStatic(PipelineXMLConstants.ANNOVAR_PATH);
		if (path != null)
			annovarPath = path.toString();
		
		//User can override path specified in properties
		String userBuildVer = properties.get(BUILD_VER);
		if (userBuildVer != null) {
			buildVer = userBuildVer;
		}
				
		//User can override path specified in properties
		String userPath = properties.get(PipelineXMLConstants.PATH);
		if (userPath != null) {
			annovarPath = userPath;
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + inputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		return command;
	}

}
