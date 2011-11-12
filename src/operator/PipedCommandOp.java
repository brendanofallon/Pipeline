package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.util.Date;
import java.util.logging.Logger;

import buffer.FileBuffer;

import pipeline.Pipeline;
import util.StringOutputStream;

public abstract class PipedCommandOp extends IOOperator {
	
	protected StringOutputStream errStream = new StringOutputStream();
	
	/**
	 * The file buffer to which output to stdout will be directed
	 * @return
	 */
	public FileBuffer getPipeDestinationBuffer() {
		return outputBuffers.get(0);
	}
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String command = getCommand();
		
		//Default to writing to first output buffer if it exists
		OutputStream writer = null;
		String outputPath = null;
				
		boolean binaryOutput = false;
		if (outputBuffers.size()>0) {
			outputPath = getPipeDestinationBuffer().getAbsolutePath();
			binaryOutput = getPipeDestinationBuffer().isBinary();
			try {
				writer = new FileOutputStream(outputPath);
			} catch (IOException e1) {
				throw new OperationFailedException("Could not open output stream : " + e1.getCause() + " " + e1.getMessage(), this);
			}
		}
		
		Date now = new Date();
		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " Executing command : " + command );
		if (writer != null);
			logger.info(" Operator : " + getObjectLabel() + " is writing to path : " + outputPath);
			
			Runtime r = Runtime.getRuntime();
			Process p;
			try {
				p = r.exec(command);

				//The process we just started may hang if the output buffer its writing into gets full
				//to avoid this, we start a couple of threads whose job it is to immediately 
				//read from the output (both to stdout and to stderr) and store the resulting data
				//Furthermore, binary output and text output are handled a bit differently
				Thread outputHandler = null; 
				if (writer != null) {
					if (binaryOutput)
						outputHandler = new BinaryPipeHandler(p.getInputStream(), writer);
					else
						outputHandler = new StringPipeHandler(p.getInputStream(), new PrintStream(getPipeDestinationBuffer().getFile()));
					

					outputHandler.start();
				}

				Thread errorHandler = new StringPipeHandler(p.getErrorStream(), errStream);
				errorHandler.start();
				
				try {
					if (p.waitFor() != 0) {
						throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value \n" + errStream.toString(), this);
					}
				} catch (InterruptedException e) {
					throw new OperationFailedException("Operator: " + getObjectLabel() + " was interrupted \n" + errStream.toString() + "\n" + e.getLocalizedMessage(), this);
				}

				//Wait for output handling thread to die
				if (outputHandler != null && outputHandler.isAlive())
					outputHandler.join();
			}
			catch (IOException e1) {
				throw new OperationFailedException("Operator: " + getObjectLabel() + " was encountered an IO exception : \n" + errStream.toString() + "\n" + e1.getLocalizedMessage(), this);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			logger.info("[ " + now + "] Operator: " + getObjectLabel() + " has completed");
		
	}

	/**
	 * Return the string containing the command to be executed
	 * @return
	 */
	protected abstract String getCommand();
	
}
