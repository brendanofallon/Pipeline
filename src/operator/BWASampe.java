package operator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;

public class BWASampe extends IOCommandOp {

	public static final String READ_GROUP = "readgroup";
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	
	protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
	protected String readGroupStr = defaultRG;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String command = getCommand();
		
		//Default to writing to first output buffer if it exists
		FileOutputStream writer = null;
		String outputPath = null;
				
		outputPath = outputBuffers.get(0).getAbsolutePath();
		try {
			writer = new FileOutputStream(outputPath);
		} catch (IOException e1) {
			throw new OperationFailedException("Could not open output stream : " + e1.getCause() + " " + e1.getMessage(), this);
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
			
			BufferedReader reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
			
//			errorReader = new BufferedReader(new InputStreamReader( p.getErrorStream() ));
//			errLine = errorReader.readLine();
//			while (errLine != null) {
//				System.err.println("Error stream : " + errLine);
//				errLine = errorReader.readLine();
//			}
//			System.err.flush();
//			
//			
//			outputReader =  p.getInputStream();
//			reader = new BufferedReader(new InputStreamReader( p.getInputStream() ));
//			line = reader.readLine();
//			while (line != null) {
//				writer.write(line.getBytes());
//				line = reader.readLine();
//			}
//
//
//			outputReader.close();
//			writer.close();
			
			
			logger.info("[ " + now + "] Operator: " + getObjectLabel() + " has completed");
			
		} catch (IOException e) {
			throw new OperationFailedException("Operator: " + getObjectLabel() + " failed with IOException : " + e.getLocalizedMessage(), this);
		}
	}
	
	@Override
	protected String getCommand() {
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String rgStr = properties.get(READ_GROUP);
		if (rgStr != null) {
			readGroupStr = rgStr;
		}
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String reads1SAI = inputBuffers.get(1).getAbsolutePath();
		String reads2SAI = inputBuffers.get(2).getAbsolutePath();
		String reads1Path = inputBuffers.get(3).getAbsolutePath();
		String reads2Path = inputBuffers.get(4).getAbsolutePath();

		String command = pathToBWA + " sampe -r " + readGroupStr + " " + referencePath + " " + reads1SAI + " " + reads2SAI + " " + reads1Path + " " + reads2Path;
		return command;		
	}

}
