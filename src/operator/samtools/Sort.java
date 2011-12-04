package operator.samtools;

import java.io.File;

import buffer.FileBuffer;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use samtools to sort the given input file
 * @author brendan
 *
 */
public class Sort extends CommandOperator {

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
		
		FileBuffer outputBuffer = outputBuffers.get(0);
		String path = outputBuffer.getAbsolutePath();
		if (path.endsWith(".bam")) {
			path = path.substring(0, path.length()-1);
		}
		
		String command = samtoolsPath + " sort " + inputBuffer.getAbsolutePath() + " " + outputBuffer.getAbsolutePath();
		
		//CONFUSING! : The last argument to sort is just a file prefix, the new file will be that prefix + .bam. So we
		//need to make sure that the file with the correct name is associated with the outputbuffer...
		File newOutputFile = new File(path + ".bam");
		outputBuffer.setFile(newOutputFile);
		
		return command;
	}

}
