package operator.samtools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import operator.CommandOperator;
import operator.IOOperator;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

public class MergeFiles extends CommandOperator {
	
	public static final String PATH = "path";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	
	@Override
	protected String getCommand() {
		Object samPropsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		
		//List<String> inputPaths = new ArrayList<String>();
		StringBuffer inputPaths = new StringBuffer();
		for(int i=0; i<inputBuffers.size(); i++) {
			FileBuffer buff = inputBuffers.get(i);
			if (buff instanceof BAMFile) {
				inputPaths.append(" " + buff.getAbsolutePath());
			}
			
			if (buff instanceof MultiFileBuffer) {
				MultiFileBuffer mBuf = (MultiFileBuffer)buff;
				for(int j=0; j<mBuf.getFileCount(); j++) {
					inputPaths.append(" " + mBuf.getFile(j).getAbsolutePath());
				}
			}
		}
		

		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = samtoolsPath + " merge " + outputPath + " " + inputPaths ;
		
		
		return command;
	}

}
