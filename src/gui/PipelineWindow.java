package gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;

import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;

import pipeline.FileSelectionPanel;

public class PipelineWindow extends JFrame {

	FileSelectionPanel readsOnePanel;
	FileSelectionPanel readsTwoPanel;
	JFileChooser chooser;
	
	public PipelineWindow() {
		super("Pipeliner");
		
		try {
        	String plaf = UIManager.getSystemLookAndFeelClassName();
        	String gtkLookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        	//Attempt to avoid metal look and feel if possible
        	if (plaf.contains("metal")) {

        		UIManager.setLookAndFeel(gtkLookAndFeel);
        	}

        	UIManager.setLookAndFeel( plaf );
		}
        catch (Exception e) {
            System.err.println("Could not set look and feel, exception : " + e.toString());
        }	
		
		
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new BorderLayout());
		
		JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		
		chooser = new JFileChooser( System.getProperty("user.home"));
		readsOnePanel = new FileSelectionPanel("First read set:", "Choose file", chooser);
		readsTwoPanel = new FileSelectionPanel("Second read set:", "Choose file", chooser);
		readsOnePanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		readsTwoPanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		centerPanel.add(readsOnePanel);
		
		centerPanel.add(readsTwoPanel);
		contentPane.add(centerPanel, BorderLayout.CENTER);
		pack();
		setLocationRelativeTo(null);
	}
}
