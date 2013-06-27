package operator.samtools;

import java.io.File;

import operator.MultiOperator;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;

public class MultiRemoveDuplicates extends MultiOperator {

	public static final String PATH = "path";
	public static final String FORCESINGLEEND = "force.single.end";

	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	protected boolean forceSingleEnd = false;
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		Object samPropsPath = getPipelineProperty(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		String forceSingleEndVal = properties.get(FORCESINGLEEND);
		if(forceSingleEndVal != null){
			forceSingleEnd = true;
		}
		
		String inputPath = inputBuffer.getAbsolutePath();
		int index = inputPath.lastIndexOf(".");
		String prefix = inputPath;
		if (index>0)
			prefix = inputPath.substring(0, index);
		String outputPath = prefix + ".dedup.bam";
		
		BAMFile outputBAM = new BAMFile(new File(outputPath), inputBuffer.getContig());
		addOutputFile(outputBAM);
		
		String fileIsSam = "";
		if (inputPath.endsWith("sam") || forceSingleEnd)
			fileIsSam = " -S ";
		
		String command = samtoolsPath + " rmdup " + fileIsSam + inputPath + " " + outputPath;
		return new String[]{command};
	}

}
