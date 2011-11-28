package pipeline;

import operator.Operator;

public interface PipelineListener {

	public void operatorCompleted(Operator op);
	
	public void operatorBeginning(Operator op);
	
	public void message(String messageText);
	
	
}
