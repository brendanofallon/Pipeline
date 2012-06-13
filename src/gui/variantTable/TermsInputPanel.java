package gui.variantTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import gui.variantTable.flexList.FlexList;
import gui.variantTable.geneList.GeneListPanel;
import gui.widgets.BorderlessButton;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;
import gui.widgets.LabelFactory;
import gui.widgets.PrettyLabel;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import buffer.VCFFile;

public class TermsInputPanel extends JPanel {
	
	public TermsInputPanel() {
		initComponents();
	}
	
	
	private void initComponents() {
		setLayout(new BorderLayout());
		
		JPanel filesPanel = new JPanel();
		filesPanel.setLayout(new BoxLayout(filesPanel, BoxLayout.Y_AXIS));
		
		fileChooser = new JFileChooser(System.getProperty("user.dir"));
		FileSelectionPanel filePanel = new FileSelectionPanel("Choose a vcf file", "(no file selected)", fileChooser);
		filePanel.addListener(new FileSelectionListener() {
			@Override
			public void fileSelected(File file) {
				importVariantFile(file);
			}	
		});
		filePanel.setBorder(BorderFactory.createEmptyBorder(40, 25, 20, 0));
		filePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		filesPanel.add(filePanel);

		dirChooser = new JFileChooser(System.getProperty("user.dir"));
		dirChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		FileSelectionPanel outputDirPanel = new FileSelectionPanel("Choose an output folder", "(none selected)", dirChooser);
		outputDirPanel.addListener(new FileSelectionListener() {

			@Override
			public void fileSelected(File file) {
				chooseOutputDirectory(file);
			}
			
		});
		outputDirPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		outputDirPanel.setBorder(BorderFactory.createEmptyBorder(0, 25, 20, 0));
		filesPanel.add(outputDirPanel);
		
		
		
		JPanel prefixPanel = new JPanel();
		prefixPanel.setLayout(new BoxLayout(prefixPanel, BoxLayout.X_AXIS));
		prefixPanel.add(Box.createRigidArea(new Dimension(25, 30)));
		prefixField = new JTextField("output");
		prefixField.setPreferredSize(new Dimension(200, 30));
		prefixField.setMaximumSize(new Dimension(200, 30));
		JLabel label = new JLabel("Prefix for output files");
		prefixPanel.add(label);
		prefixPanel.add(prefixField);
		prefixPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		prefixPanel.add(Box.createHorizontalGlue());
		prefixPanel.add(Box.createHorizontalStrut(50));
		filesPanel.add(prefixPanel);
		filesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
		
		this.add(filesPanel, BorderLayout.NORTH);
		
		JPanel listsPanel = new JPanel();
		listsPanel.setLayout(new BoxLayout(listsPanel, BoxLayout.X_AXIS));
		this.add(listsPanel, BorderLayout.CENTER);
		
		JPanel termsPanel = new JPanel();
		termsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		termsPanel.setLayout(new BoxLayout(termsPanel, BoxLayout.Y_AXIS));
		termsPanel.add(LabelFactory.makeLabel("Summary / abstract terms"));
		termsList = new FlexList();
		String[] terms = new String[]{"First", "Second", "Third"};
		termsList.setData(Arrays.asList(terms));
		termsPanel.add(termsList);
		termsList.setPreferredSize(new Dimension(200, 300));
		termsPanel.add(Box.createVerticalGlue());
		
		JPanel goTermsPanel =new JPanel();
		goTermsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		goTermsPanel.setLayout(new BoxLayout(goTermsPanel, BoxLayout.Y_AXIS));
		goTermsPanel.add(LabelFactory.makeLabel("Gene Ontology (GO) terms"));
		goTermsList = new FlexList();
		String[] goTerms = new String[]{"First", "Second", "Third"};
		goTermsList.setData(Arrays.asList(goTerms));
		goTermsPanel.add(goTermsList);
		goTermsList.setPreferredSize(new Dimension(200, 300));
		goTermsPanel.add(Box.createVerticalGlue());
		
		
		JPanel genesPanel = new JPanel();
		genesPanel.setLayout(new BorderLayout());
		genesPanel.add(LabelFactory.makeLabel("Key genes", 14f), BorderLayout.NORTH);
		
		genesList = new GeneListPanel();
		genesList.setData(Arrays.asList(new String[]{"ENG", "ACVRL1"}));
		genesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		genesPanel.add(genesList, BorderLayout.CENTER);
		
		
		listsPanel.add(termsPanel);
		listsPanel.add(goTermsPanel);
		listsPanel.add(genesPanel);
	}
	
	protected void chooseOutputDirectory(File file) {
		this.outputDirectory = file;
	}


	protected void importVariantFile(File file) {
		variantFile = new VCFFile(file);
	}
	
	public File getOutputDirectory() {
		return outputDirectory;
	}


	public VCFFile getVariantFile() {
		return variantFile;
	}
	
	public List<String> getGeneList() {
		return genesList.getGenes();
	}

	public Map<String, Integer> getTermsList() {
		return termsList.getScoreMap();
	}
	
	public Map<String, Integer> getGOTermsList() {
		return goTermsList.getScoreMap();
	}
	
	public String getPrefix() {
		return prefixField.getText().replace(" " , "_");
	}
	
	JTextField prefixField;
	VCFFile variantFile = null;
	File outputDirectory = null;
	JFileChooser dirChooser = null;
	JFileChooser fileChooser = null;
	FlexList goTermsList;
	FlexList termsList;
	GeneListPanel genesList;
}
