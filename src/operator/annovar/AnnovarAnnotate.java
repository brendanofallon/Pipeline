package operator.annovar;

import java.io.File;

import buffer.AnnovarInputFile;
import buffer.VCFFile;
import operator.CommandOperator;
import operator.OperationFailedException;
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
	public static final String FORMAT = "format";
	protected String format = "vcf4"; //Default assumed input format
	protected double siftThreshold = 0.0; // by default emit all variants 
	
	
	
	@Override
	protected String[] getCommands() {
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
		
		
		//First step is to convert input VCF to annovar format
		String formatStr = properties.get(FORMAT);
		if (formatStr != null)
			format = formatStr;
		
		VCFFile inputVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
		String inputPath = inputVCF.getAbsolutePath();
		AnnovarInputFile annovarInput = new AnnovarInputFile(new File(Pipeline.getPipelineInstance().getProjectHome() + "annovar.input.annovar"));
		
		
		String command1 = "perl " + annovarPath + "convert2annovar.pl -format " + format + " " + inputPath + " --outfile " + annovarInput.getAbsolutePath(); 
		
		String annovarInputPath = annovarInput.getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command2 = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		
		//SIFT stuff
		String command3 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype avsift --buildver " + buildVer + " --sift_threshold " + siftThreshold + "  " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		File siftResults = new File(outputPath + "hg19_avsift_dropped"); 
		
		//Polyphen 
		String command4 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_pp2 --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		
		return new String[]{command1, command2, command3, command4};
	}


	@Override
	protected String getCommand() throws OperationFailedException {
		//Nothing to do since we've overridden getCommands()
		return null;
	}

}
