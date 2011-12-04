package operator.gatk;

import java.util.ArrayList;
import java.util.List;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

public class MergeFiles extends CommandOperator {

	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	public static final String PATH = "path";
	public static final String JVM_ARGS="jvmargs";
	
	@Override
	protected String getCommand() throws OperationFailedException {
		ReferenceFile reference = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
		List<String> filesToMerge = new ArrayList<String>();
		
		boolean hasBAMs = false;
		boolean hasVCFs = false;
		
		for(FileBuffer buffer : inputBuffers) {
			if (buffer instanceof ReferenceFile) {
				reference = (ReferenceFile) buffer;
				continue;
			}
			if (buffer instanceof MultiFileBuffer) {
				MultiFileBuffer multiFile = (MultiFileBuffer)buffer;
				for(int i=0; i<multiFile.getFileCount(); i++) {
					FileBuffer mFile = multiFile.getFile(i);
					filesToMerge.add( mFile.getAbsolutePath());
					if (mFile instanceof BAMFile)
						hasBAMs = true;
					if (mFile instanceof VCFFile)
						hasVCFs = true;
				}
				
			}
			else {
				filesToMerge.add( buffer.getAbsolutePath() );
				if (buffer instanceof BAMFile) 
					hasBAMs = true;
				if (buffer instanceof VCFFile) {
					hasVCFs = true;
				}
			}
		}
		
		if (hasBAMs && hasVCFs) {
			throw new OperationFailedException("Cannot merge files of different types (found both BAM and VCF)", this);
		}
	
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) Pipeline.getPropertyStatic(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
				
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = "X?";
		
		if (hasBAMs) {
			command = "java -Xms1g -Xmx8g " + jvmARGStr + " -jar " + gatkPath;
			command = command + " -R " + reference.getAbsolutePath() +
					" -T PrintReads ";
			for(String filePath : filesToMerge) {
				command = command + " -I " + filePath;
			}
			command = command + " -o " + outputPath;
		}
		
		if (hasVCFs) {
			command = "java -Xms1g -Xmx8g " + jvmARGStr + " -jar " + gatkPath;
			command = command + " -R " + reference.getAbsolutePath() +
					" -T CombineVariants ";
			for(String filePath : filesToMerge) {
				command = command + " --variant " + filePath;
			}
			command = command + " -o " + outputPath;
		}
		
		return command;
	}

}
