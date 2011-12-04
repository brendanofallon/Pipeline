package operator.samtools;

import buffer.FileBuffer;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use samtools to index the given input file
 * @author brendan
 *
 */
public class Index extends CommandOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
		Object samPropsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		FileBuffer inputBuffer = inputBuffers.get(0);
		
		String command = samtoolsPath + " index " + inputBuffer.getAbsolutePath();
		return command;
	}

}
