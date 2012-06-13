package gui.variantTable;

import gui.PipelineApp;
import gui.PipelineWindow;

import java.awt.EventQueue;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import pipeline.ArgumentParser;

public class VarViewerApp {

	static VarViewerApp app;
	
	static VarViewerFrame frame;
	
	static String pathToPropsFile = null;

	public static void showMainWindow() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new VarViewerFrame(pathToPropsFile);
					frame.setVisible(true);
				} catch (Exception e) {
					System.err.println("Caught exception : " + e);
					e.printStackTrace();
				}
			}
		});
		
	}
	
	private void loadProperties() {
		
	}
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		
		for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels())
	        System.out.println(info.getClassName() );
	    
		
		try {
			
			
        	String plaf = UIManager.getSystemLookAndFeelClassName();
        	String gtkLookAndFeel = "com.sun.java.swing.plaf.gtk.GTKLookAndFeel";
        	String nimbusLookAndFeel = "com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel";
        	//Attempt to avoid metal look and feel if possible
        	System.out.println("Look and feel is : " + plaf);
        	if (plaf.contains("Metal") || plaf.contains("Motif")) {
        		UIManager.setLookAndFeel(gtkLookAndFeel);
        		
        	}

        	UIManager.setLookAndFeel( plaf );
		}
        catch (Exception e) {
            System.err.println("Could not set look and feel, exception : " + e.toString());
        }	
		
		ArgumentParser argParser = new ArgumentParser();
		argParser.parse(args);
		
		//File to obtain properties from
		String propsPath = argParser.getStringOp("props");
		if (propsPath != null)
			pathToPropsFile = propsPath;
		
		showMainWindow();
	}
	
}
