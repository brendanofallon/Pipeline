package operator.samtools;

import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Uses samtools to convert a SAM file to a BAM file
 * @author brendan
 *
 */
public class ConvertSamBam extends PipedCommandOp {


	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
	
		Object samPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPath != null)
			samtoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			samtoolsPath = samUserPath;
		}
		
		String inputPath = inputBuffers.get(0).getAbsolutePath();
				
		//Ouput handled automagically!
		String command = samtoolsPath + " view -Sb " + inputPath;
		return command;
	}

}
