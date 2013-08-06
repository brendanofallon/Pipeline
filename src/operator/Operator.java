package operator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

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

	enum State {Initialized, Started, Completed, Error};
	
	protected Map<String, String> properties = new HashMap<String, String>();
	protected List<OperatorStartHook> startHooks = new LinkedList<OperatorStartHook>();
	protected List<OperatorEndHook> endHooks = new LinkedList<OperatorEndHook>();
	
	protected State state = State.Initialized;
	protected boolean verbose = true;
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
		//Logger.getLogger(Pipeline.primaryLoggerName).info("Operator : " + this.getObjectLabel() + " adding attribute " + key + " = " + value);
	}
		
	@Override
	public String getAttribute(String key) {
		return properties.get(key);
	}
	
	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}
	
	/**
	 * Get the current State of this operator
	 * @return
	 */
	public State getState() {
		return state;
	}
	
	public void operate() throws OperationFailedException {
		state = State.Started;
		
		// Perform the start hooks
		try{
			Iterator<OperatorStartHook> itStart = startHooks.iterator();
			while(itStart.hasNext()){
				OperatorStartHook osh = itStart.next();
				osh.doHookStart();
			}
		} catch (Exception e) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Operator start hook failed to complete: " + e.getMessage());
		}
		
		// Perform the Operation
		try {
			performOperation();
		}
		catch (OperationFailedException oex) {
			state = State.Error;
			throw oex;
		}
		
		// Perform the end hooks
		try{
			Iterator<OperatorEndHook> itEnd = endHooks.iterator();
			while(itEnd.hasNext()){
				OperatorEndHook oeh = itEnd.next();
				oeh.doHookEnd();
			}
		}catch(Exception e){

			Logger.getLogger(Pipeline.primaryLoggerName).info("Operator end hook failed to complete: " + e.getMessage());
		}
		
		state = State.Completed;
	}
	
	/**
	 * Add a start hook to this Operator
	 * @param start
	 */
	public void addStartHook(OperatorStartHook start){
		startHooks.add(start);
	}
	
	/**
	 * Add an end hook to this operator
	 * @param end
	 */
	public void addEndHook(OperatorEndHook end){
		endHooks.add(end);
	}
	
	public abstract void performOperation() throws OperationFailedException;
}
