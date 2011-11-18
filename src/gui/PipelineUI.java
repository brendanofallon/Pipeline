package gui;

import java.awt.EventQueue;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JLabel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.UIManager;

public class PipelineUI {

	private JFrame frame;
	private JTextField reads1Field;
	private JTextField reads2Field;
	private JTextField txtHumanrefvfasta;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					PipelineUI window = new PipelineUI();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public PipelineUI() {
		
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
        	
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 701, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		JPanel panel = new JPanel();
		frame.getContentPane().add(panel, BorderLayout.SOUTH);
		
		JLabel statusLabel = new JLabel("Status");
		panel.add(statusLabel);
		
		JPanel centerPanel = new JPanel();
		frame.getContentPane().add(centerPanel, BorderLayout.CENTER);
		
		reads1Field = new JTextField();
		reads1Field.setText("Enter fastq file here");
		reads1Field.setColumns(10);
		
		JButton chooseReads1Button = new JButton("Choose");
		
		reads2Field = new JTextField();
		reads2Field.setText("Enter second fastq file here");
		reads2Field.setColumns(10);
		
		JButton chooseReads2Button = new JButton("Choose");
		
		txtHumanrefvfasta = new JTextField();
		txtHumanrefvfasta.setText("human_ref_v37.fasta");
		txtHumanrefvfasta.setColumns(10);
		
		JLabel lblReferenceFile = new JLabel("Reference file");
		
		JLabel lblInputFastqFiles = new JLabel("Input fastq files");
		
		JButton chooseRefButton = new JButton("Choose");
		
		JButton beginButton = new JButton("Begin");
		GroupLayout gl_centerPanel = new GroupLayout(centerPanel);
		gl_centerPanel.setHorizontalGroup(
			gl_centerPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_centerPanel.createSequentialGroup()
					.addGap(237)
					.addGroup(gl_centerPanel.createParallelGroup(Alignment.LEADING)
						.addComponent(lblInputFastqFiles)
						.addComponent(lblReferenceFile)
						.addGroup(gl_centerPanel.createSequentialGroup()
							.addGroup(gl_centerPanel.createParallelGroup(Alignment.TRAILING, false)
								.addComponent(txtHumanrefvfasta, Alignment.LEADING)
								.addComponent(reads2Field, Alignment.LEADING)
								.addComponent(reads1Field, Alignment.LEADING, GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE))
							.addPreferredGap(ComponentPlacement.RELATED)
							.addGroup(gl_centerPanel.createParallelGroup(Alignment.LEADING)
								.addComponent(chooseRefButton)
								.addComponent(chooseReads1Button)
								.addComponent(chooseReads2Button))))
					.addContainerGap(332, Short.MAX_VALUE))
				.addGroup(Alignment.TRAILING, gl_centerPanel.createSequentialGroup()
					.addContainerGap(754, Short.MAX_VALUE)
					.addComponent(beginButton)
					.addGap(27))
		);
		gl_centerPanel.setVerticalGroup(
			gl_centerPanel.createParallelGroup(Alignment.LEADING)
				.addGroup(gl_centerPanel.createSequentialGroup()
					.addGap(64)
					.addComponent(lblReferenceFile)
					.addGap(18)
					.addGroup(gl_centerPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(txtHumanrefvfasta, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(chooseRefButton))
					.addGap(32)
					.addComponent(lblInputFastqFiles)
					.addGap(18)
					.addGroup(gl_centerPanel.createParallelGroup(Alignment.BASELINE)
						.addComponent(reads1Field, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(chooseReads1Button))
					.addPreferredGap(ComponentPlacement.RELATED)
					.addGroup(gl_centerPanel.createParallelGroup(Alignment.TRAILING)
						.addComponent(reads2Field, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.addComponent(chooseReads2Button))
					.addPreferredGap(ComponentPlacement.RELATED, 157, Short.MAX_VALUE)
					.addComponent(beginButton)
					.addContainerGap())
		);
		centerPanel.setLayout(gl_centerPanel);
	}
}
