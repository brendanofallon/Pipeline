package gui.variantTable.flexList;

import gui.widgets.BorderlessButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

/**
 * Draws a single cell in a FlexList
 * @author brendan
 *
 */
public class FlexListCellRenderer extends JPanel {
	
	private final FlexList list;
	
	public FlexListCellRenderer(final FlexList list, String text) {
		this.list = list;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		this.setBackground(Color.white);
		this.text = new JTextArea(text);
		this.text.setEditable(true);
		this.text.setBorder(BorderFactory.createEmptyBorder());
		
		this.text.setBackground(this.getBackground());
		this.text.setMinimumSize(new Dimension(50, 30));
		this.text.setPreferredSize(new Dimension(200, 30));
		this.text.setMaximumSize(new Dimension(1000, 36));
		this.text.setRows(1);
		this.add(this.text);
		
		button = new BorderlessButton("X");
		button.setPreferredSize(new Dimension(28, 28));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeThisItem();
			}
			
		});
		
		valSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
		valSpinner.setMaximumSize(new Dimension(40,28));
		this.add(valSpinner);
		this.add(button);
		this.setMaximumSize(new Dimension(500, 36));
	}

	public void removeThisItem() {
		list.removeListItem(this);
	}
	
	public Integer getScore() {
		return (Integer)valSpinner.getValue();
	}
	
	public String getText() {
		return text.getText();
	}
	
	
	private JSpinner valSpinner;
	private JTextArea text;
	private BorderlessButton button;
	
}
