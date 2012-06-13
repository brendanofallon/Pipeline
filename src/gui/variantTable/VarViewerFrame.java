package gui.variantTable;

import gui.ErrorWindow;
import gui.PipelineWindow;
import gui.variantTable.AnalysisRunner.PipelineRunner;
import gui.widgets.BorderlessButton;
import gui.widgets.FileSelectionListener;
import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import rankingService.AnalysisSettings;

import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import util.VCFLineParser;

public class VarViewerFrame extends JFrame {

	private String pathToProperties = null;
	
	public VarViewerFrame(String pathToPropertiesFile) {
		super("Variant Ranker");
		this.pathToProperties = pathToPropertiesFile;
		
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		initComponents();
		setPreferredSize(new Dimension(850, 500));
		pack();
	}

	private void initComponents() {
		setLayout(new BorderLayout());
		
		centerPanel = new JPanel();
		centerPanel.setLayout(new BorderLayout());
		termsPanel = new TermsInputPanel();
		termsPanel.setPreferredSize(new Dimension(300, 400));
		centerPanel.add(termsPanel, BorderLayout.CENTER);
		this.add(centerPanel, BorderLayout.CENTER);
		
		
		JPanel bottomPanel = new JPanel();
		BorderlessButton beginButton= new BorderlessButton("Begin");
		beginButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				beginAnalysis();
			}
			
		});
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(beginButton);
		bottomPanel.add(Box.createRigidArea(new Dimension(30, 30)));
		this.add(bottomPanel, BorderLayout.SOUTH);
	}
	

	protected void beginAnalysis() {
		if (termsPanel.getVariantFile() == null)
			ErrorWindow.showErrorWindow(new FileNotFoundException("Please choose a variant file before beginning"));
		
		if (termsPanel.getOutputDirectory() == null) 
			ErrorWindow.showErrorWindow(new FileNotFoundException("Please choose an output directory before beginning"));
		
		AnalysisSettings settings = new AnalysisSettings();
		//settings.bedFilePath = AnalysisRunner.defaultBEDFile;
		settings.genes = termsPanel.getGeneList();
		settings.goTerms = termsPanel.getGOTermsList();
		settings.summaryTerms = termsPanel.getTermsList();
		settings.rootPath = termsPanel.getOutputDirectory().getAbsolutePath();
		settings.pathToPipelineProperties = this.pathToProperties;
		
		settings.prefix = termsPanel.getPrefix();
		settings.graphSize = 200;
		settings.pathToVCF = termsPanel.getVariantFile().getAbsolutePath();
		
		InputStream templateStream = getResourceInputStream("variantTable/templates/analysis_template.xml");
		
		AnalysisRunner runner = new AnalysisRunner(settings, templateStream);
		try {
			runner.initialize();
			PipelineRunner pplRunner = runner.getPipelineRunner();
			
			centerPanel.removeAll();
			ProgressPanel progPanel = new ProgressPanel(this, runner, pplRunner);
			centerPanel.add(progPanel, BorderLayout.CENTER);
			centerPanel.revalidate();
			repaint();
			pplRunner.execute();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void showVariantsList(VariantPool pool) {
		centerPanel.removeAll();
		VariantTablePanel varTable = new VariantTablePanel();
		List<VariantRec> vars = pool.toList();
		varTable.setVariantPool( vars );
		List<String> colKeys = new ArrayList<String>();
		colKeys.add(VariantRec.GENE_NAME);
		colKeys.add(VariantRec.PDOT);
		colKeys.add(VariantRec.EXON_FUNCTION);
		colKeys.add(VariantRec.CDOT);
		colKeys.add(VariantRec.POP_FREQUENCY);
		colKeys.add(VariantRec.EXOMES_FREQ);
		colKeys.add(VariantRec.EFFECT_PREDICTION2);
		colKeys.add(VariantRec.GO_EFFECT_PROD);
		colKeys.add(VariantTableModel.QUALITY);
		varTable.setColumns(colKeys);
		centerPanel.add(varTable, BorderLayout.CENTER);
		centerPanel.revalidate();
		repaint();
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

	 /** Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static InputStream getResourceInputStream(String url) {
		InputStream resource = null;
		try {
			URL resourceURL = PipelineWindow.class.getResource(url);
			resource = resourceURL.openStream();
			
		}
		catch (Exception ex) {
			System.out.println("Error loading file from resource " + url + "\n" + ex);
		}
		return resource;
	}

	JPanel centerPanel;
	TermsInputPanel termsPanel;
	VariantTablePanel variantPanel = null;
	
}
