package operator;

import java.io.IOException;
import java.sql.Time;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.ElapsedTimeFormatter;
import util.StringOutputStream;

/**
 * Base class for operators that invoke a system call, but do not emit their output to standard out. Unlike
 * PipedCommandOp, these operators don't need any fancy tricks to capture the output - it's just written to
 * a file by the command that we call (for instance, in GaTK the result is always written to a file). Hence
 * these operators do not need to capture the output stream, and just simply invoke a system call and wait for
 * it to complete. 
 * 
 * @author brendan
 *
 */
public abstract class CommandOperator extends IOOperator {
	
	protected StringOutputStream errStream = new StringOutputStream();

	protected abstract String getCommand() throws OperationFailedException;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		String command = getCommand();
		
		Date now = new Date();
		long beginMillis = System.currentTimeMillis();
		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " Executing command : " + command );
		Runtime r = Runtime.getRuntime();
		Process p;

		try {
			p = r.exec(command);
			Thread errorHandler = new StringPipeHandler(p.getErrorStream(), errStream);
			errorHandler.start();

			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("Operator: " + getObjectLabel() + " terminated with nonzero exit value : " + errStream.toString(), this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Operator: " + getObjectLabel() + " was interrupted : " + errStream.toString() + "\n" + e.getLocalizedMessage(), this);
			}


		}
		catch (IOException e1) {
			throw new OperationFailedException("Operator: " + getObjectLabel() + " was encountered an IO exception : " + errStream.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
		
		now = new Date();
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - beginMillis;
		logger.info("[ " + now + "] Operator: " + getObjectLabel() + " has completed. Time taken = " + elapsedMillis + " ms ( " + ElapsedTimeFormatter.getElapsedTime(beginMillis, endMillis) + " )");		
	}

}
