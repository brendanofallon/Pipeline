package gui.variantTable;

import gui.ErrorWindow;
import gui.variantTable.AnalysisRunner.PipelineRunner;
import gui.widgets.LabelFactory;
import gui.widgets.PrettyLabel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import buffer.CSVFile;
import buffer.variant.VariantPool;

import operator.Operator;

import pipeline.PipelineListener;

public class ProgressPanel extends JPanel implements PipelineListener, DoneListener {

	PipelineRunner runner;
	AnalysisRunner analRunner;
	VarViewerFrame viewerFrame;
	int opCount;
	int operatorsCompleted = 0;
	
	public ProgressPanel(VarViewerFrame viewer, AnalysisRunner analRunner, PipelineRunner runner) {
		this.runner = runner;
		this.analRunner = analRunner;
		this.viewerFrame = viewer;
		analRunner.addListener(this);
		opCount = runner.getPipeline().getObjectHandler().getOperatorList().size();
		runner.getPipeline().addListener(this);
		initComponents();
	}
	
	private void initComponents() {
		setLayout(new BorderLayout());
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
	
		JPanel midPanel = new JPanel();
		midPanel.setLayout(new BoxLayout(midPanel, BoxLayout.Y_AXIS));
		midPanel.setPreferredSize(new Dimension(500, 100));
		midPanel.setBorder(BorderFactory.createEmptyBorder(100, 50, 0, 50));
		label = new JLabel("Beginning analysis");
		midPanel.add(label);
		midPanel.add(progressBar);
		messageLabel = new JLabel(" ");
		midPanel.add(messageLabel);
		this.add(midPanel, BorderLayout.CENTER);
	}
	
	public void setLabelText(String text) {
		label.setText(text);
		label.revalidate();
		label.repaint();
	}
	

	@Override
	public void operatorCompleted(Operator op) {
		operatorsCompleted++;
	}

	@Override
	public void operatorBeginning(Operator op) {
		setLabelText("Executing operator " + op.getObjectLabel() + " (" + operatorsCompleted + " / " + opCount + ")");
		progressBar.setValue((int)Math.round( 100 *(double)operatorsCompleted / (double)opCount));
		
	}

	@Override
	public void errorEncountered(Operator op) {
		setLabelText("Error : operator " + op.getObjectLabel());
	}


	public void done() {
		setLabelText("Done!");
		progressBar.setValue(100);

		CSVFile variants = analRunner.getFinalVariants();
		try {
			System.out.println("Starting to show variants list from file :" + variants.getAbsolutePath());
			VariantPool varPool = new VariantPool(variants);
			System.out.println("Created variant pool with " + varPool.size() + " variants");
			viewerFrame.showVariantsList(varPool);
		} catch (IOException e) {
			ErrorWindow.showErrorWindow(e, "Could not find final variants file");
		}
	}

	@Override
	public void pipelineFinished() {
		//This is called from within the swing worker thread, NOT the EDT, don't use it!
		//Use pipelineCompleted instead
		
	}

	@Override
	public void message(String messageText) {
		messageLabel.setText(messageText);
		messageLabel.revalidate();
		messageLabel.repaint();
	}

	
	JLabel messageLabel;
	JLabel label;
	JProgressBar progressBar;

}
