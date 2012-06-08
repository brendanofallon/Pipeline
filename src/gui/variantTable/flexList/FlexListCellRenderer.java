package gui.variantTable.flexList;

import gui.widgets.BorderlessButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SpinnerNumberModel;

public class FlexListCellRenderer extends JPanel {
	
	
	public FlexListCellRenderer(String text) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBackground(Color.white);
		this.text = new JTextArea(text);
		this.text.setEditable(true);
		this.text.setBorder(BorderFactory.createEmptyBorder());
		
		this.text.setBackground(this.getBackground());
		this.text.setMinimumSize(new Dimension(50, 30));
		this.text.setPreferredSize(new Dimension(200, 30));
		this.text.setMaximumSize(new Dimension(1000, 36));
		this.text.setRows(1);
		
		button = new BorderlessButton(" X ");
		this.add(this.text);
		//this.add(Box.createGlue());
		
		valSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		valSpinner.setMaximumSize(new Dimension(40,30));
		this.add(valSpinner);
		this.add(button);
		this.setMaximumSize(new Dimension(500, 36));
	}

	private JSpinner valSpinner;
	private JTextArea text;
	private BorderlessButton button;
	
	
}
