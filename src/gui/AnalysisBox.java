package gui;

import gui.widgets.BorderlessButton;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

public class AnalysisBox extends JPanel {
	
	public static final Color backgroundColor = Color.LIGHT_GRAY;
	
	private final PipelineWindow window;
	
	public AnalysisBox(PipelineWindow window) {
		this.window = window;
		setBackground(backgroundColor);
		setLayout(new FlowLayout(FlowLayout.CENTER));
		
		addAnalysisTypes();
		
	}

	private void addAnalysisTypes() {
		BorderlessButton firstType = new BorderlessButton("Analysis type 1", PipelineWindow.getIcon("icons/pipe_icon2.png"));
		firstType.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.showAnalysisConfig( new FullAnalysisConfig(window) );
			}
		});
		add(firstType);
		
		BorderlessButton secondType = new BorderlessButton("Analysis type 2", PipelineWindow.getIcon("icons/pipe_icon2.png"));
		add(secondType);
		
		BorderlessButton thirdType = new BorderlessButton("Some other analysis", PipelineWindow.getIcon("icons/pipe_icon2.png"));
		add(thirdType);
	}

}
