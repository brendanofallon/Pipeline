package operator;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import buffer.FileBuffer;

import pipeline.Pipeline;
import pipeline.PipelineObject;

public abstract class Operator extends PipelineObject {

	protected Map<String, String> properties = new HashMap<String, String>();
	
	protected boolean verbose = true;
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
		Logger.getLogger(Pipeline.primaryLoggerName).info("Operator : " + this.getObjectLabel() + " adding attribute " + key + " = " + value);
	}
		
	
	public abstract void performOperation() throws OperationFailedException;
}
