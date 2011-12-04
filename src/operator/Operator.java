package operator;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * This is the base class of all things that perform an "operation" on some data. There's not a lot
 * of functionality here - we just store some properties (which typically come in as attributes
 * from the input xml file), and define an abstract method 'performOperation' which gets called
 * when this operator is to do its job. 
 * 
 * Most important subclass is IOOperator, which is a type of operator that takes data from an
 * input file and (usually) writes it to an output file. 
 * 
 * @author brendan
 *
 */
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
