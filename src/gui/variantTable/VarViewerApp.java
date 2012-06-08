package gui.variantTable;

import gui.PipelineApp;
import gui.PipelineWindow;

import java.awt.EventQueue;

public class VarViewerApp {

	static VarViewerApp app;
	
	static VarViewerFrame frame;

	public static void showMainWindow() {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frame = new VarViewerFrame();
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
		showMainWindow();
	}
	
}
