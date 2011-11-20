package gui;

import javax.swing.JPanel;
import javax.swing.JProgressBar;

import operator.OperationFailedException;
import operator.Operator;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;
import pipeline.PipelineListener;

public class ProgressPanel extends JPanel implements PipelineListener {

	protected Pipeline pipeline;
	protected Operator currentOperator = null;
	
	private JProgressBar progressBar;
	
	
	public ProgressPanel(Pipeline pipe) {
		this.pipeline = pipe;
		pipeline.addListener(this);
	}

	public void executePipeline() {
		
		try {
			pipeline.initializePipeline();
			initializePanels();
			
			pipeline.execute();
		} catch (PipelineDocException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ObjectCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (OperationFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void initializePanels() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void operatorCompleted(Operator op) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void operatorBeginning(Operator op) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void message(String messageText) {
		// TODO Auto-generated method stub
		
	}
	
	
}
