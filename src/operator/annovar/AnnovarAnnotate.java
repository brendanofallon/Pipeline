package operator.annovar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.AnnovarInputFile;
import buffer.AnnovarResults;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;
import buffer.VCFFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantFilter;
import buffer.variant.VariantRec;
import operator.CommandOperator;
import operator.MultiOperator;
import operator.OperationFailedException;
import operator.MultiOperator.TaskOperator;
import pipeline.Pipeline;
import pipeline.PipelineObject;
import pipeline.PipelineXMLConstants;
import util.ElapsedTimeFormatter;

/**
 * Use annovar to produce variant annotation files. 
 * Right now this assumes that in the annovar directory there is a database directory called "humandb"
 * that contains all required db info 
 * @author brendan
 *
 */
public class AnnovarAnnotate extends MultiOperator {

	
	public static final String BUILD_VER = "buildver";
	protected String annovarPath = "~/annovar/";
	protected String buildVer = "hg19";
	public static final String FORMAT = "format";
	protected String format = "vcf4"; //Default assumed input format
	protected double siftThreshold = 0.0; // by default emit all variants 
	private String annovarPrefix = "annovar.output";
	//protected double freqFilter = 0.0; //Frequency filtering level, all variants with frequency higher than this level in 1000g will be moved to the dropped file
	
	
	/**
	 * Works in a manner similar to MultiOperator.performOperation... but a command has to be executed before
	 * the all of the others... and there's some work to do at after all tasks have completed
	 */
	public void performOperation() throws OperationFailedException {
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
		
		
		String generateInputFile = "perl " + annovarPath + "convert2annovar.pl -format " + format + " " + inputPath + " --outfile " + annovarInput.getAbsolutePath(); 
		
		executeCommand(generateInputFile);
		
		String annovarInputPath = annovarInput.getAbsolutePath();
		
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( getPreferredThreadCount() );
		
		Date start = new Date();
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		logger.info("Beginning parallel annotation operation " + getObjectLabel());
		
		List<TaskOperator> jobs = new ArrayList<TaskOperator>();
		
		String commands[] = getCommands(annovarInputPath);
		for(int i=0; i<commands.length; i++) {
			String command = commands[i];
			if (command != null) {
				TaskOperator task = new TaskOperator(new String[]{command}, logger);
				jobs.add(task);
				threadPool.submit(task);
			}
		}

		
		try {
			logger.info("All tasks have been submitted to multioperator " + getObjectLabel() + ", now awaiting termination...");
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
			
			//Check for errors
			boolean allOK = true;
			for(TaskOperator job : jobs) {
				if (job.isError()) {
					allOK = false;
					logger.severe("Parallel task in operator " + getObjectLabel() + " encountered error: " + job.getException());
				}
			}
			if (!allOK) {
				throw new OperationFailedException("One or more tasks in parallel operator " + getObjectLabel() + " encountered an error.", this);
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		generateOutputFiles();
		
		Date end = new Date();
		logger.info("Annotation job " + getObjectLabel() + " has completed (Total time " + ElapsedTimeFormatter.getElapsedTime(start.getTime(), end.getTime()) + " )");

	}
	
	/**
	 * Obtain the list of commands that will be submitted to the thread pool
	 * @return
	 */
	protected String[] getCommands(String annovarInputPath) {

		//Filter all variants by 1000g, setting maf to 0 means that all variants will be put into dropped file 
		String command15 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype 1000g2010nov_all -maf 0 --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		
		String command2 = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//SIFT stuff
		String command3 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype avsift --buildver " + buildVer + " --sift_threshold " + siftThreshold + "  " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//Polyphen 
		String command4 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_pp2 --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";

		//GERP
		//Annovar currently broken with gerp
		//String command5 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_gerp++ --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		//File gerpResults = new File(outputPath + "hg19_ljb_gerp++_filtered"); 
		
		//MutationTaster
		String command6 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_mt --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";

		//PhyloP
		String command7 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_phylop --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";
		
		return new String[]{command2};
		//return new String[]{command15, command2, command3, command4, /*command5, */ command6, command7 };
	}
	
	/**
	 * Grab info from all of the files that annovar has generated and create a VariantPool with 
	 *  that have 
	 * @throws OperationFailedException 
	 */
	private void generateOutputFiles() throws OperationFailedException {	 
		File varFunc = new File(annovarPrefix + ".variant_function"); 

		File exvarFunc = new File(annovarPrefix + ".exonic_variant_function"); 
		File siftFile = new File(annovarPrefix + ".hg19_avsift_dropped"); 
		File polyphenFile = new File(annovarPrefix + ".hg19_ljb_pp2_dropped"); 
		File mtFile = new File(annovarPrefix + ".hg19_ljb_mt_dropped"); 
		File TKGFile = new File(annovarPrefix + ".hg19_ALL.sites.2010_11_dropped");
		File phlyoPFile = new File(annovarPrefix + ".hg19_ljb_phylop_dropped");
		try {
			AnnovarResults variants = new AnnovarResults(varFunc, exvarFunc, siftFile, polyphenFile, mtFile, TKGFile, phlyoPFile);

			List<String> keys = new ArrayList<String>();
			keys.add(VariantRec.GENE_NAME);
			keys.add(VariantRec.VARIANT_TYPE);
			keys.add(VariantRec.EXON_FUNCTION);
			keys.add(VariantRec.NM_NUMBER);
			keys.add(VariantRec.CDOT);
			keys.add(VariantRec.PDOT);
//			keys.add(VariantRec.POP_FREQUENCY);
//			keys.add(VariantRec.SIFT_SCORE);
//			keys.add(VariantRec.POLYPHEN_SCORE);
//			keys.add(VariantRec.MT_SCORE);
//			keys.add(VariantRec.PHYLOP_SCORE);
			
			VCFFile inputVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
			String outputFilename = inputVCF.getAbsolutePath();
			outputFilename = outputFilename.replace(".vcf", ".annotated.csv");
			variants.listAll(new PrintStream(new FileOutputStream(outputFilename)), keys);
		} catch (IOException e) {
			throw new OperationFailedException("Error opening annovar results files : " + e.getMessage(), this);
		}


	}
	
	@Override
	public void initialize(NodeList children) {
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj == null)
						throw new IllegalArgumentException("Unknown object reference to MultiOperator " + getObjectLabel());
					if (obj instanceof MultiFileBuffer) 
						throw new IllegalArgumentException("Annotation operator cannot currently handle multi-file buffers");
					
					if (obj instanceof ReferenceFile) {
						reference = (ReferenceFile) obj;
					}
					if (obj instanceof FileBuffer) {
						addInputBuffer( (FileBuffer)obj);
					}
					
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj == null)
						throw new IllegalArgumentException("Unknown object reference to MultiOperator " + getObjectLabel());
					if (obj instanceof FileBuffer) {
						addOutputBuffer( (FileBuffer)obj );
					}
				}
			}
		}
		
		if (inputFiles == null) {
			Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
			logger.warning("No file found for input to Annotation operator " + getObjectLabel());
			if (inputBuffers.size()==0 || (inputBuffers.size()==1 && reference != null)) {
				logger.severe("Also, no file buffers as input either. This is probably an error.");
				throw new IllegalArgumentException("No input buffers found for multi-operator " + getObjectLabel());
			}
		}
	}

	protected String getCommand() throws OperationFailedException {
		//Nothing to do since we've overridden getCommands()
		return null;
	}

	/**
	 * A filter that passes variants that are in the top quartile for sift, polyphen, and mutation taster scores
	 * @author brendan
	 *
	 */
	class HighQuartilesFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_QUARTILE) && rec.hasProperty(VariantRec.POLYPHEN_QUARTILE) && rec.hasProperty(VariantRec.MT_QUARTILE)) {
				Double siftQ = rec.getProperty(VariantRec.SIFT_QUARTILE);
				Double ppQ = rec.getProperty(VariantRec.POLYPHEN_QUARTILE);
				Double mtQ = rec.getProperty(VariantRec.MT_QUARTILE);
				if (siftQ == 0 && ppQ == 0 && mtQ == 0) {
					return true;
				}
			}
			return false;
		}
	}
		
	
	/**
	 * A filter that passes variants that are in the bottom quartile for sift, polyphen, and mutation taster scores
	 * @author brendan
	 *
	 */
	class LowQuartilesFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_QUARTILE) && rec.hasProperty(VariantRec.POLYPHEN_QUARTILE) && rec.hasProperty(VariantRec.MT_QUARTILE)) {
				Double siftQ = rec.getProperty(VariantRec.SIFT_QUARTILE);
				Double ppQ = rec.getProperty(VariantRec.POLYPHEN_QUARTILE);
				Double mtQ = rec.getProperty(VariantRec.MT_QUARTILE);
				if (siftQ > 1 && ppQ > 1 && mtQ > 1) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * A filter that passes all variants that are not 'bad', that is those that have high sift scores and low polyphen and mt scores
	 * @author brendan
	 *
	 */
	class NonBadFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_SCORE) && rec.hasProperty(VariantRec.POLYPHEN_SCORE) && rec.hasProperty(VariantRec.MT_SCORE)) {
				Double siftScore = rec.getProperty(VariantRec.SIFT_SCORE);
				Double ppScore = rec.getProperty(VariantRec.POLYPHEN_SCORE);
				Double mtScore = rec.getProperty(VariantRec.MT_SCORE);
				//Polyphen score is the posterior probability that the variant IS DAMAGING, so small
				//scores indicate low probability that the variant is damaging. 
				
				if (siftScore > 0.05 && ppScore < 0.20 && mtScore < 0.20) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * This filter passes all variants where sift, polyphen, and mt all seem to agree on deleteriousness.
	 * These are the most likely to be disease causing
	 * @author brendan
	 *
	 */
	class BadFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_SCORE) && rec.hasProperty(VariantRec.POLYPHEN_SCORE) && rec.hasProperty(VariantRec.MT_SCORE)) {
				Double siftScore = rec.getProperty(VariantRec.SIFT_SCORE);
				Double ppScore = rec.getProperty(VariantRec.POLYPHEN_SCORE);
				Double mtScore = rec.getProperty(VariantRec.MT_SCORE);
				//Polyphen score is the posterior probability that the variant IS DAMAGING, so small
				//scores indicate low probability that the variant is damaging. 
				
				if (siftScore <= 0.05 && ppScore > 0.80 && mtScore > 0.80) {
					return true;
				}
			}
			return false;
		}
	}

	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		//Nothing to do since we've overridden performOperation
		return null;
	}
}

//	out.println("contig \t start \t end \t variant.type \t exon.func \t pop.freq \t het \t qual \t sift \t polyphen \t mt");  

