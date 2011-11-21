package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;


public class FullAnalysisConfig extends TemplateConfigurator  {
	
	public FullAnalysisConfig(PipelineWindow window) {
		super(window);
		generator = new PipelineGenerator( PipelineWindow.getFileResource("templates/practice_template.xml"));

		initComponents();
	}
	
}
