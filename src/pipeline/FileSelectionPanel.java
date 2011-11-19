package pipeline;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * A JPanel with a text field and button that allows users to choose a file
 * @author brendan
 *
 */
public class FileSelectionPanel extends JPanel {

	private JLabel label;
	private JTextField field;
	private JButton browseButton;
	private JFileChooser fileChooser;
	private File selectedFile = null;
	
	public FileSelectionPanel(String labelText, String fieldText, JFileChooser chooser) {
		this.setLayout(new FlowLayout(FlowLayout.LEFT));
		this.fileChooser = chooser;
		label = new JLabel(labelText == null ? "" : labelText);
		add(label);
		field = new JTextField(fieldText);
		field.setMinimumSize(new Dimension(100, 10));
		field.setPreferredSize(new Dimension(100, 32));
		field.setMaximumSize(new Dimension(100, 1000));
		field.setEditable(false);
		add(field);
		
		browseButton = new JButton("Choose");
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				chooseFile();
			}
		});
		add(browseButton);
	}

	protected void chooseFile() {
		int x = fileChooser.showOpenDialog(this);
		if (x == JFileChooser.APPROVE_OPTION) {
			selectedFile = fileChooser.getSelectedFile();
			field.setText(selectedFile.getName());
		}
	}
	
	public File getSelectedFile() {
		return selectedFile;
	}
	
	public boolean hasSelectedFile() {
		return selectedFile != null;
	}
	
}
