package operator.annovar;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Logger;

import buffer.AnnovarInputFile;
import buffer.AnnovarResults;
import buffer.FileBuffer;
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
	private String annovarPrefix = "annovar.output";
	//protected double freqFilter = 0.0; //Frequency filtering level, all variants with frequency higher than this level in 1000g will be moved to the dropped file
	
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
		
		
		annovarPrefix = Pipeline.getPipelineInstance().getProjectHome() + annovarPrefix;
		
		//First step is to convert input VCF to annovar format
		String formatStr = properties.get(FORMAT);
		if (formatStr != null)
			format = formatStr;
		
		VCFFile inputVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
		String inputPath = inputVCF.getAbsolutePath();
		AnnovarInputFile annovarInput = new AnnovarInputFile(new File(Pipeline.getPipelineInstance().getProjectHome() + "annovar.input"));
		
		
		String command1 = "perl " + annovarPath + "convert2annovar.pl -format " + format + " " + inputPath + " --outfile " + annovarInput.getAbsolutePath(); 
		
		String annovarInputPath = annovarInput.getAbsolutePath();
		
		//Filter all variants by 1000g, setting maf to 0 means that all variants will be put into dropped file 
		String command15 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype 1000g2010nov_all -maf 0 --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		String command2 = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//SIFT stuff
		String command3 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype avsift --buildver " + buildVer + " --sift_threshold " + siftThreshold + "  " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//Polyphen 
		String command4 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_pp2 --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";

		//GERP
		//Annovar currently broken with gerp
		//String command5 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_gerp++ --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		//File gerpResults = new File(outputPath + "hg19_ljb_gerp++_filtered"); 
		
		//MutationTaster
		String command6 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_mt --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
				
		
		return new String[]{command1, command15, command2, command3, command4, /*command5, */ command6};
	}
	
	public void performOperation() throws OperationFailedException {
		super.performOperation();
		 
		File varFunc = new File(annovarPrefix + ".variant_function"); 

		File exvarFunc = new File(annovarPrefix + ".exonic_variant_function"); 
		File siftFile = new File(annovarPrefix + ".hg19_avsift_dropped"); 
		File polyphenFile = new File(annovarPrefix + ".hg19_ljb_pp2_dropped"); 
		File mtFile = new File(annovarPrefix + ".hg19_ljb_mt_dropped"); 
		File TKGFile = new File(annovarPrefix + ".hg19_ALL.sites.2010_11_dropped");
		try {
			AnnovarResults rec = new AnnovarResults(varFunc, exvarFunc, siftFile, polyphenFile, mtFile, TKGFile);
			
			FileBuffer resultsFile = outputBuffers.get(0);
			rec.compareRanks(new PrintStream(new FileOutputStream("annovar.tophits.csv")));
			rec.emitNonsynonymousVars(new PrintStream(new FileOutputStream(resultsFile.getAbsolutePath())) );
			
		} catch (IOException e) {
			throw new OperationFailedException("Error opening annovar results files : " + e.getMessage(), this);
		}


	}


	@Override
	protected String getCommand() throws OperationFailedException {
		//Nothing to do since we've overridden getCommands()
		return null;
	}

}
