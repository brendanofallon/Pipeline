package gui;

import gui.PipelineGenerator.InjectableItem;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSeparator;


public class FullAnalysisConfig extends JPanel  {

	protected final PipelineGenerator generator;
	protected PipelineWindow window;

	public FullAnalysisConfig(PipelineWindow window) {
		this.window = window;
		generator = new PipelineGenerator( PipelineWindow.getFileResource("templates/practice_template.xml"));
		Collection<InjectableItem> items = generator.getInjectables();	
		initComponents();
	}
	
	
	private void initComponents() {
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		
		chooser = new JFileChooser( System.getProperty("user.home"));
		readsOnePanel = new FileSelectionPanel("First read set:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("readsOne", file.getAbsolutePath());
					
				}
			}
		});
		readsTwoPanel = new FileSelectionPanel("Second read set:", "Choose file", chooser);
		readsOnePanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("readsTwo", file.getAbsolutePath());
					
				}
			}
		});
		
		readsOnePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		readsTwoPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		centerPanel.add(readsOnePanel);
		centerPanel.add(readsTwoPanel);
		
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
		JButton beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				window.beginRun(generator.getDocument());
			}
		});
		bottomPanel.add(beginButton, BorderLayout.EAST);
		centerPanel.add(bottomPanel);
		
	}
	
	private FileSelectionPanel readsTwoPanel;
	private FileSelectionPanel readsOnePanel;
	private JFileChooser chooser;


}
