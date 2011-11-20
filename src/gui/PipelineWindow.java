package gui;

import gui.widgets.FileSelectionPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.URL;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.UIManager;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineDocException;

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
		centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		contentPane.add(centerPanel, BorderLayout.CENTER);
		
		AnalysisBox analyses = new AnalysisBox(this);
		centerScrollPane = new JScrollPane(analyses);
		centerScrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
		centerPanel.add(centerScrollPane, BorderLayout.CENTER);
		
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new BorderLayout());
		bottomPanel.add(new JSeparator(JSeparator.HORIZONTAL));

		contentPane.add(bottomPanel, BorderLayout.SOUTH);
		
		pack();
		setLocationRelativeTo(null);
	}

	/**
	 * Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static File getFileResource(String url) {
		File file = null;
		try {
			URL fileLocation = PipelineWindow.class.getResource(url);
			file = new File(fileLocation.getFile());
		}
		catch (Exception ex) {
			System.out.println("Error loading icon from resouce : " + ex);
		}
		return file;
	}
	
	/**
	 * Returns an icon from the given URL, with a bit of exception handling. 
	 * @param url
	 * @return
	 */
	public static ImageIcon getIcon(String url) {
		ImageIcon icon = null;
		try {
			java.net.URL imageURL = PipelineWindow.class.getResource(url);
			icon = new ImageIcon(imageURL);
		}
		catch (Exception ex) {
			System.out.println("Error loading icon from resouce : " + ex);
		}
		return icon;
	}
	
	/**
	 * Initiate a new Pipeline run that executes the given document
	 * @param doc
	 */
	protected void beginRun(Document doc) {
		Transformer t;
		try {
			t = TransformerFactory.newInstance().newTransformer();

			t.setOutputProperty(OutputKeys.METHOD, "xml");

			t.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC,
					"-//W3C//DTD XHTML 1.0 Transitional//EN");

			t.setOutputProperty(OutputKeys.DOCTYPE_SYSTEM,
					"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd");

			t.setOutputProperty(OutputKeys.METHOD, "html");
			t.transform(new DOMSource( doc), new StreamResult(System.out));

		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
//		Pipeline pipeline = new Pipeline(doc);
//		try {
//			pipeline.execute();
//		} catch (PipelineDocException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (ObjectCreationException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public void showAnalysisConfig(FullAnalysisConfig fullAnalysisConfig) {
		
		centerScrollPane.setViewportView(fullAnalysisConfig);
		revalidate();
		repaint();
	}
	
	
	JScrollPane centerScrollPane;
}
