package operator;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.ReferenceFile;

/**
 * Creates indel realignment targets for use with the IndelRealigner
 * @author brendan
 *
 */
public class TargetCreator extends CommandOperator {

	public final String defaultMemOptions = " -Xms2048m -Xmx8g";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	
	@Override
	protected String getCommand() {
		
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		String reference = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		String inputFile = getInputBufferForClass(BAMFile.class).getAbsolutePath();
		
		String indelIntervalsFile = outputBuffers.get(0).getAbsolutePath();
		
		String command = "java " + defaultMemOptions + " -jar " + gatkPath + " -R " + reference + " -I " + inputFile + " -T RealignerTargetCreator -o " + indelIntervalsFile;
		return command;
	}

}
