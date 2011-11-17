package operator.samtools;

import java.util.logging.Logger;

import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.SAMFile;
import operator.OperationFailedException;
import operator.PipedCommandOp;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class SamtoolsPileup extends PipedCommandOp {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() throws OperationFailedException {
	
		Object samPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPath != null)
			samtoolsPath = samPath.toString();
		
		String samUserPath = properties.get(PATH);
		if (samUserPath != null) {
			samtoolsPath = samUserPath;
		}
		
		
		
		String inputPath = getInputBufferForClass(SAMFile.class).getAbsolutePath();
		FileBuffer reference = getInputBufferForClass(ReferenceFile.class);
		String refPath = "";
		if (reference != null){
			refPath = " -t " + reference.getAbsolutePath() + " ";
		}
		else {
			throw new OperationFailedException("No reference specified", this);
		}
		
		//Ouput handled automagically!
		String command = samtoolsPath + " mpileup -C50 -E -D -f " + refPath + " " + inputPath;
		return command;
	}
	
}
