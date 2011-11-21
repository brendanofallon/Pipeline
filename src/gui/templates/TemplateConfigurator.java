package gui.templates;

import gui.PipelineGenerator;
import gui.PipelineWindow;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

/**
 * User interface allowing configuration of pipeline template. This is the base
 * class and contains a few useful methods for all such template-configurators 
 * @author brendan
 *
 */
public class TemplateConfigurator extends JPanel {

	protected JComboBox refBox; //Different references to use
	protected String[] refTypes = new String[]{"Human build 37", "Human build 36"};
	
	protected PipelineWindow window;
	protected PipelineGenerator generator;
	
	public TemplateConfigurator(PipelineWindow window) {
		this.window = window;
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
	
	protected void initComponents() {
		//JPanel centerPanel = new JPanel();
		setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
		JTextArea descArea = new JTextArea("Description: " + generator.getDescription().replace('\n', ' '));
		descArea.setWrapStyleWord(true);
		descArea.setEditable(false);
		descArea.setOpaque(false);
		descArea.setBorder(BorderFactory.createLineBorder(Color.gray, 1, true));
		descArea.setAlignmentX(Component.RIGHT_ALIGNMENT);
		descArea.setLineWrap(true);
		descArea.setMaximumSize(new Dimension(1000, 100));
		descArea.setPreferredSize(new Dimension(400, 100));
		add(descArea, BorderLayout.NORTH );
		
		add(Box.createVerticalStrut(20));
		add(Box.createVerticalGlue());
		refBox = new JComboBox(refTypes);
		JPanel refPanel = new JPanel();
		refPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		refPanel.add(new JLabel("<html><b>Reference:</b></html>"));
		refPanel.add(refBox);
		refPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(refPanel);
		
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
		readsTwoPanel.addListener(new FileSelectionListener() {
			public void fileSelected(File file) {
				if (file != null) {
					generator.inject("readsTwo", file.getAbsolutePath());
					
				}
			}
		});
		
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		readsTwoPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(readsOnePanel);
		add(readsTwoPanel);
		add(Box.createVerticalGlue());
		
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		add(new JSeparator(JSeparator.HORIZONTAL));
		
		JButton beginButton = new JButton("Begin");
		beginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				updateReference();
				window.beginRun(generator.getDocument());
			}
		});
		beginButton.setAlignmentX(Component.RIGHT_ALIGNMENT);
		add(beginButton);
	}
	
	protected FileSelectionPanel readsTwoPanel;
	protected FileSelectionPanel readsOnePanel;
	protected JFileChooser chooser;
	
}
