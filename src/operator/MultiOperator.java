package operator;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.ReferenceFile;

/**
 * This operator only recognizes a single, MultiFileBuffer as an input and output argument, and runs
 * the same operation on each input file and adds the result to the output multi file buffer  
 * @author brendan
 *
 */
public abstract class MultiOperator extends IOOperator {

	protected MultiFileBuffer inputFiles;
	protected MultiFileBuffer outputFiles;
	protected ReferenceFile reference;
	protected ThreadPoolExecutor threadPool = null;
	
	public MultiOperator() {
		
	}
	
	/**
	 * Adds a new file to the list of output files. Under normal use getCommand() should call this
	 * to add the appropriate file to the list of output files
	 */
	protected void addOutputFile(FileBuffer outputFile) {
		outputFiles.addFile(outputFile);
	}
	
	protected abstract String getCommand(FileBuffer inputBuffer);
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool( Pipeline.getPipelineInstance().getThreadCount() );
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Beginning parallel multi-operation " + getObjectLabel() + " with " + inputFiles.getFileCount() + " input files");
		
		for(int i=0; i<inputFiles.getFileCount(); i++) {
			
			FileBuffer inputBuffer = inputFiles.getFile(i);
			String command = getCommand(inputBuffer);
			TaskOperator task = new TaskOperator(command, logger);
			threadPool.submit(task);
		}
		
		try {
			threadPool.shutdown(); //No new tasks will be submitted,
			threadPool.awaitTermination(7, TimeUnit.DAYS); //Wait until all tasks have completed
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		logger.info("Parallel multi-operation " + getObjectLabel() + " has completed");

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
					if (obj instanceof MultiFileBuffer) {
						inputFiles = (MultiFileBuffer)obj;
					}
					else {
						if (obj instanceof ReferenceFile) {
							reference = (ReferenceFile) obj;
						}
						if (obj instanceof FileBuffer) {
							addInputBuffer( (FileBuffer)obj);
						}
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
					if (obj instanceof MultiFileBuffer) {
						outputFiles = (MultiFileBuffer)obj;
					}
					else {
						throw new IllegalArgumentException("Found non-MultiFileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
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
	
	/**
	 * Little wrapper for commands so they can be executed in a thread pool
	 * @author brendan
	 *
	 */
	class TaskOperator implements Runnable {

		final String command;
		Logger logger;
		
		public TaskOperator(String command, Logger logger) {
			this.command = command;
			this.logger = logger;
		}
		
		@Override
		public void run() {
			try {
				logger.info("Beginning task with command : " + command);
				executeCommand(command);
				logger.info("Task with command : " + command + " has completed");
			} catch (OperationFailedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
}
