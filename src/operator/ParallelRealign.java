package operator;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

import buffer.BAMFile;
import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;

/**
 * This operator performs the indel realignment target creation and actual realignment step
 * in parallel, by breaking the input bam into chromosome-sized chunks and running each chunk
 * independently. A threadpool is used to handle the parallelism.
 * After all realignments happen they are stitched back together into a single, realigned BAM  
 * @author brendan
 *
 */
public class ParallelRealign extends IOOperator {

	protected BAMFile inputBam;
	protected ThreadPoolExecutor threadPool = null;
	public static final String JVM_ARGS="jvmargs";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected String jvmARGStr = "";
	protected String referencePath = null;
	protected MultiFileBuffer multiBAM;
	
	public ParallelRealign() {
		
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Pipeline.getPipelineInstance().getThreadCount());
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning ParallelRealignment for operator " + getObjectLabel());
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		referencePath = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		inputBam = (BAMFile) getInputBufferForClass(BAMFile.class);
		MultiFileBuffer multiFile = (MultiFileBuffer) getInputBufferForClass(MultiFileBuffer.class);
		multiBAM = (MultiFileBuffer) getOutputBufferForClass(MultiFileBuffer.class);
		
		//Additional args for jvm
		String jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) Pipeline.getPropertyStatic(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
	

		//If there is an input BAM file, break up by normal human chromosomes and
		//submit all jobs to the thread pool
		if (inputBam != null) {
			logger.info("ParallelRealign: found single input BAM file, submitting by contig");

			for(int i=1; i<24; i++) {
				String contig = "" + i;
				if (i == 22)
					contig = "X";
				if (i==23)
					contig = "Y";

				TargetAndRealign job = new TargetAndRealign(contig, inputBam.getAbsolutePath(), logger, contig);
				threadPool.submit(job);

			}
		}
		
		
		//If we have an input MultiFile, then assume it's already broken up and submit each file individually
		if (multiFile != null) {
			logger.info("ParallelRealign: found multifile with " + multiFile.getFileCount() + " files");
			for(int i=1; i<multiFile.getFileCount(); i++) {
				String filePath = multiFile.getFile(i).getAbsolutePath(); 
				String contig = multiFile.getFile(i).getContig();
				if (contig == null) {
					int a = filePath.lastIndexOf("/");
					int b = filePath.lastIndexOf(".");
					contig = filePath.substring(a+1, b);
				}
				System.out.println("Contig name / file prefix is : " + contig);
				TargetAndRealign job = new TargetAndRealign(multiFile.getFile(i).getContig(), multiFile.getFile(i).getAbsolutePath(), logger, contig);
				threadPool.submit(job);

			}
		}
		
		try {
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(96, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		
		logger.info("All contigs have completed realignment for ParallelRealign operator " + getObjectLabel() + ", done.");
	}
	
	protected void addOutputFile(String pathToFile, String contig) {
		FileBuffer buffer = new BAMFile(new File(pathToFile), contig);
		multiBAM.addFile(buffer);
	}

	protected void executeCommand(String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		Process p;

		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("ParallelRealign is executing command : " + command);
		try {
			p = r.exec(command);
			Thread errorHandler = new StringPipeHandler(p.getErrorStream(), System.err);
			errorHandler.start();

			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Par. target realigner terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Target realigner was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}


		}
		catch (IOException e1) {
			throw new OperationFailedException("Target realigner encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
	
	/**
	 * Small class to handle target creation and realignment in one step
	 * @author brendan
	 *
	 */
	class TargetAndRealign implements Runnable {

		private boolean finished = false;
		private boolean error = false;
		private Exception errorException = null;
		final String contig;
		private Logger logger;
		private final String inputPath;
		private String filePrefix;
		
		public TargetAndRealign(String contig, String inputPath, Logger logger, String filePrefix) {
			this.contig = contig;
			this.logger = logger;
			this.inputPath = inputPath;
			this.filePrefix = filePrefix;
		}
		
		@Override
		public void run() {
			finished = false;
			logger.info("Beginning contig realignment for contig " + filePrefix);
			String rand = "" + (int)Math.round( 100000*Math.random() );
			String targetsPath;
			String realignedContigPath;
			String pathPrefix = (String) Pipeline.getPropertyStatic(Pipeline.PROJECT_HOME);
			if (pathPrefix == null)
				pathPrefix = "";
			
			if (contig != null) {
				targetsPath = pathPrefix + "targets_" + contig + "_" + rand + ".intervals";
				realignedContigPath = pathPrefix + "contig_" + contig + ".realigned.bam";
			}
			else {
				targetsPath = pathPrefix + "targets_" + rand + ".intervals";
				realignedContigPath = pathPrefix + filePrefix + ".realigned.bam";
			}
			
			
			String command = "java -Xmx8g " + jvmARGStr + " -jar " + gatkPath + 
					" -R " + referencePath + 
					" -I " + inputPath + 
					" -T RealignerTargetCreator -o " + targetsPath;
			if (contig != null)
				command = command +	" -L " + contig;
			
			String command2 = "java -Xmx8g " + jvmARGStr + " -jar " + gatkPath + 
					" -R " + referencePath + 
					" -I " + inputPath + 
					" -T IndelRealigner " + 
					" -targetIntervals " + targetsPath + " -o " + realignedContigPath;
			if (contig != null)
					command = command +	" -L " + contig;


			try {
				System.out.println("Executing command : " + command);
				executeCommand(command);
				System.out.println("Executing command : " + command2);
				executeCommand(command2);
				addOutputFile(realignedContigPath, contig);
			}
			catch (OperationFailedException e) {
				error = true;
				errorException = e;
			}
			

			logger.info("Completed realignment for contig " + filePrefix);
			finished = true;
		}
		
		/**
		 * Returns true if an exception was thrown during either command
		 * @return
		 */
		public boolean isError() {
			return error;
		}
		
		public Exception getException() {
			return errorException;
		}
		
		/**
		 * Returns true if this runnable has finished running
		 * @return
		 */
		public boolean isFinished() {
			return finished;
		}
		
		
	}
	

}
