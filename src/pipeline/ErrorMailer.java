package pipeline;

import java.util.ArrayList;
import java.util.List;

import operator.Operator;
import util.PipelineMailer;
import util.SendMail;

public class ErrorMailer implements PipelineListener {

	private String recipient = "brendan.d.ofallon@aruplab.com";
	
	
	private Operator currentOp = null;
	private List<Operator> completedOps = new ArrayList<Operator>();
	
	public void setRecipient(String mailRecipient) {
		this.recipient = mailRecipient;
	}
	
	@Override
	public void errorEncountered(Operator op) {
		StringBuilder message = new StringBuilder();
		message.append("Error encountered in pipeline run \n");
		message.append("Current operator : " + currentOp.getObjectLabel() + " : " + currentOp.getClass() + "\n");
		message.append("Completed operators: \n");
		for(Operator compOp : completedOps) {
			message.append(compOp.getObjectLabel() + " : " + compOp.getClass() + "\n");
		}
		
		PipelineMailer.sendMail(recipient, "Pipeline error", message.toString());
	}

	@Override
	public void pipelineFinished() {
		
	}
	
	@Override
	public void operatorCompleted(Operator op) {
		completedOps.add(op);
	}

	@Override
	public void operatorBeginning(Operator op) {
		currentOp = op;
	}



	@Override
	public void message(String messageText) {
		// TODO Auto-generated method stub
		
	}

}
