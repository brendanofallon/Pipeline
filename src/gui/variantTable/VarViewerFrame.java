package gui.variantTable;

import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.UIManager;

import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import util.VCFLineParser;

public class VarViewerFrame extends JFrame {

	public VarViewerFrame() {
		super("Variant Ranker");
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
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		initComponents();
		//setLocationRelativeTo(null);
		setPreferredSize(new Dimension(800, 600));
		//setSize(new Dimension(800, 600));
		pack();
	}

	private void initComponents() {
		setLayout(new BorderLayout());
		
		fileChooser = new JFileChooser(System.getProperty("user.dir"));
		FileSelectionPanel filePanel = new FileSelectionPanel("Choose a vcf file", "(no file selected)", fileChooser);
		filePanel.addListener(new FileSelectionListener() {
			@Override
			public void fileSelected(File file) {
				importVariantFile(file);
			}	
		});
		
		this.add(filePanel, BorderLayout.NORTH);
		
		TermsInputPanel termsPanel = new TermsInputPanel();
		this.add(termsPanel, BorderLayout.CENTER);
		
		//variantPanel = new VariantTablePanel();
		//this.add(variantPanel, BorderLayout.CENTER);
		//variantPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	}
	

	protected void importVariantFile(File file) {
		try {
			VariantPool pool = new VariantPool(new VCFFile(file));
			List<VariantRec> vars = pool.toList();
			variantPanel.setVariantPool(vars);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	VariantTablePanel variantPanel = null;
	JFileChooser fileChooser = null;
}
