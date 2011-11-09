package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;

public abstract class IOCommandOp extends IOOperator {
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String command = getCommand();
		
		//Default to writing to first output buffer if it exists
		FileOutputStream writer = null;
		String outputPath = null;
				
		if (outputBuffers.size()>0) {
			outputPath = outputBuffers.get(0).getAbsolutePath();
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
			
		try {
			Runtime r = Runtime.getRuntime();
			Process p = r.exec(command);
			
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value", this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Operator: " + getObjectLabel() + " was interrupted : " + e.getLocalizedMessage(), this);
			}
			
			BufferedReader errorReader = new BufferedReader(new InputStreamReader( p.getErrorStream() ));
			String errLine = errorReader.readLine();
			while (errLine != null) {
				System.err.println("Error stream : " + errLine);
				errLine = errorReader.readLine();
			}
			System.err.flush();
			
			if (writer != null) {
				InputStream outputReader =  p.getInputStream();

				if (outputBuffers.get(0).isBinary()) {
					logger.info("Output buffer " + outputBuffers.get(0).getObjectLabel() + " is binary, switching to binary IO mode");
					int c;
					while ((c = outputReader.read()) != -1) {
						writer.write(c);
					}
				}
				else {
					BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
					String line = reader.readLine();
					while (line != null) {
						writer.write(line.getBytes());
						line = reader.readLine();
					}
				}
				
				outputReader.close();
				writer.close();
			}
			
			logger.info("[ " + now + "] Operator: " + getObjectLabel() + " has completed");
			
		} catch (IOException e) {
			throw new OperationFailedException("Operator: " + getObjectLabel() + " failed with IOException : " + e.getLocalizedMessage(), this);
		}
	}

	/**
	 * Return the string containing the command to be executed
	 * @return
	 */
	protected abstract String getCommand();
	
}
