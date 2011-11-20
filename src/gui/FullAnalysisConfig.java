package gui;

import gui.PipelineGenerator.InjectableItem;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Collection;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;


public class FullAnalysisConfig extends JPanel  {

	protected final PipelineGenerator generator;
	protected PipelineWindow window;

	private JComboBox refBox; //Different references to use
	private String[] refTypes = new String[]{"Human build 37", "Human build 36"};
	
	public FullAnalysisConfig(PipelineWindow window) {
		this.window = window;
		generator = new PipelineGenerator( PipelineWindow.getFileResource("templates/practice_template.xml"));

		initComponents();
	}
	
	/**
	 * Inject reference sequence info into template, must happen before run begins
	 */
	protected void updateReference() {
		//TODO get path to resources directory from properties file
		String pathToResourceDirs = "/home/brendan/";
		if (refBox.getSelectedIndex()==0) {
			generator.inject("reference", pathToResourceDirs + "resources/human_g1k_v37.fasta");
		}
		if (refBox.getSelectedIndex()==1) {
			generator.inject("reference", pathToResourceDirs + "resources_b36/human_ref_b36.fasta");
		}
	}
	
	private void initComponents() {
		JPanel centerPanel = new JPanel();
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(centerPanel, BorderLayout.CENTER);
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		
		refBox = new JComboBox(refTypes);
		JPanel refPanel = new JPanel();
		refPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		refPanel.add(new JLabel("<html><b>Reference:</b></html>"));
		refPanel.add(refBox);
		refPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		centerPanel.add(refPanel);
		
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
		
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		readsTwoPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		centerPanel.add(readsOnePanel);
		centerPanel.add(readsTwoPanel);
		
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(new JSeparator(JSeparator.HORIZONTAL), BorderLayout.NORTH);
		JButton beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateReference();
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
