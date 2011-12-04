package pipeline;

import operator.Operator;

/**
 * Interface for things that listen for events issueing from a Pipeline. Right now
 * this just amounts to operators starting and ending, as well as the mysterious
 * 'message' event.
 * @author brendan
 *
 */
public interface PipelineListener {

	public void operatorCompleted(Operator op);
	
	public void operatorBeginning(Operator op);
	
	/**
	 * Called when the pipeline has finishing all operators
	 */
	public void pipelineFinished();
	
	public void message(String messageText);
	
	
}
