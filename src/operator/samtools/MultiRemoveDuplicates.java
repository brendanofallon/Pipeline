package operator.samtools;

import java.io.File;

import buffer.BAMFile;
import buffer.FileBuffer;
import operator.MultiOperator;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class MultiRemoveDuplicates extends MultiOperator {

	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand(FileBuffer inputBuffer) {
		Object samPropsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".dedup.bam";
		
		BAMFile outputBAM = new BAMFile(new File(outputPath));
		addOutputFile(outputBAM);
		
		
		String fileIsSam = "";
		if (inputPath.endsWith("sam"))
			fileIsSam = " -S ";
		
		String command = samtoolsPath + " rmdup " + fileIsSam + inputPath + " " + outputPath;
		return command;
	}

}
