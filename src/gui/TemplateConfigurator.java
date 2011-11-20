package gui;

import javax.swing.JPanel;

/**
 * User interface allowing configuration of pipeline template. This is the base
 * class and contains a few useful methods for all such template-configurators 
 * @author brendan
 *
 */
public class TemplateConfigurator extends JPanel {

	protected PipelineWindow window;
	protected PipelineGenerator generator;
	
	public TemplateConfigurator(PipelineWindow window) {
		this.window = window;
	}
	
}
