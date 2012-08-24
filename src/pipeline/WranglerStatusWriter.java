package pipeline;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import operator.Operator;

/**
 * Implements a simple status-writing tool that is compatible with the status-reading
 * stuff in JobWrangler
 * @author brendan
 *
 */
public class WranglerStatusWriter implements PipelineListener {

	private File statusFile = null;
	private Map<String, String> messages = new HashMap<String, String>();
	
	
	/**
	 * Create a new status writer that writes to user.dir
	 */
	public WranglerStatusWriter() {
		statusFile = new File("wrangler.status.txt");
	}
	
	private void logMessage(Map<String, String> keyVals) {
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(statusFile));
			
			for(String key : keyVals.keySet()) {
				String val = keyVals.get(key);
				writer.write(key + "=" + val + System.getProperty("line.separator"));
			}
			
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Logger.getLogger(Pipeline.primaryLoggerName).warning("Could not write status message to file : " + statusFile + " reason: " + e.getMessage());
		}
				
		
	}
	
	@Override
	public void operatorCompleted(Operator op) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void operatorBeginning(Operator op) {
		messages.clear();
		messages.put("Current operation", op.getObjectLabel());
		logMessage(messages);
	}

	@Override
	public void errorEncountered(Operator op) {
		messages.clear();
		messages.put("Current operation", op.getObjectLabel());
		messages.put("error", "true");
		logMessage(messages);
	}

	@Override
	public void pipelineFinished() {
		messages.put("Finished", "true");
		logMessage(messages);
	}

	@Override
	public void message(String messageText) {
		messages.put("Message", messageText);
		logMessage(messages);
	}

}
