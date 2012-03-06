package operator.gatk;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.StringPipeHandler;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Splits a single input BAM file into contigs based on chromosome and 
 * emits the result as a MultiFileBuffer
 * @author brendan
 *
 */
public class SplitByChromosome extends IOOperator {

	protected BAMFile inputBam;
	protected ThreadPoolExecutor threadPool = null;
	public static final String JVM_ARGS="jvmargs";
	public static final String PATH = "path";
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected String jvmARGStr = "";
	protected String referencePath = null;
	protected MultiFileBuffer multiBAM;
	protected MultiFileBuffer outputFiles;
	
	@Override
	public boolean requiresReference() {
		return true;
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(Pipeline.getPipelineInstance().getThreadCount());
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning splitting operation for operator " + getObjectLabel());		
		
		referencePath = ((ReferenceFile) getInputBufferForClass(ReferenceFile.class)).getAbsolutePath();
		inputBam = (BAMFile) getInputBufferForClass(BAMFile.class);
		
		outputFiles = (MultiFileBuffer) getOutputBufferForClass(MultiFileBuffer.class);
		
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
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

		//Submit all jobs to the thread pool
		for(int i=1; i<25; i++) {
			String contig = "" + i;
			if (i == 23)
				contig = "X";
			if (i==24)
				contig = "Y";

			Split job = new Split(contig);
			threadPool.submit(job);
		}

		try {
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(96, TimeUnit.HOURS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		checkContigs(outputFiles.getFileList()); //Ensure all contigs have been created
		logger.info("Done with splitting operator " + getObjectLabel());		
	}
	
	protected void addOutputFile(FileBuffer outputFile) {
		outputFiles.addFile(outputFile);
	}
	
	/**
	 * Execute the given system command in it's own process
	 * @param command
	 * @throws OperationFailedException
	 */
	protected void executeCommand(String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		Process p;

		try {
			p = r.exec(command);
			Thread errorHandler = new StringPipeHandler(p.getErrorStream(), System.err);
			errorHandler.start();

			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}


		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
	

	
	
	public class Split implements Runnable {

		final String contig;
		
		public Split(String contig) {
			this.contig = contig;
		}
		
		@Override
		public void run() {
			
			String inputPath = inputBam.getAbsolutePath();
			int index = inputPath.lastIndexOf(".");
			String prefix = inputPath;
			if (index>0)
				prefix = inputPath.substring(0, index);
			String outputPath = prefix + ".c" + contig + ".bam";
			
			String command = "java -Xmx4g " + jvmARGStr + " -jar " + gatkPath + 
					" -R " + referencePath + 
					" -I " + inputBam.getAbsolutePath() + 
					" -T PrintReads " +
					" -o " + outputPath +
					" -L " + contig;
			
			try {
				Logger.getLogger(Pipeline.primaryLoggerName).info("Split operator is executing command " + command);		

				executeCommand(command);
				addOutputFile(new BAMFile(new File(outputPath), contig));
			} catch (OperationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

}
