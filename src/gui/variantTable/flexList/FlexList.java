package gui.variantTable.flexList;

import gui.widgets.BorderlessButton;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * A FlexList displays a list of text components in which each text component is associated
 * with a JSpinner (to 'score' the text) and a removal button
 * @author brendan
 *
 */
public class FlexList extends JPanel {

	private List<String> data;
	private JScrollPane scrollPane;
	private JPanel listPanel;
	
	public FlexList() {
		initComponents();
	}
	
	/**
	 * Returns a map containing all entries along with their scores
	 * @return
	 */
	public Map<String, Integer> getScoreMap() {
		Map<String, Integer> map = new HashMap<String, Integer>();
		
		for(int i=0; i<listPanel.getComponentCount(); i++) {
			Component comp = listPanel.getComponent(i);
			if (comp instanceof FlexListCellRenderer) {
				FlexListCellRenderer cell = (FlexListCellRenderer)comp;
				map.put(cell.getText(), cell.getScore());
			}
		}
		return map;
	}
	
	private void initComponents() {
		setLayout(new BorderLayout());
		
		BorderlessButton newItemButton = new BorderlessButton("Add");
		newItemButton.setPreferredSize(new Dimension(40, 30));
		newItemButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				addNewItem();
			}
		});
		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		topPanel.add(Box.createHorizontalGlue());
		topPanel.add(newItemButton);
		this.add(topPanel, BorderLayout.NORTH);
		
		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		scrollPane = new JScrollPane(listPanel);
		this.add(scrollPane, BorderLayout.CENTER);
		
	}

	protected void addNewItem() {
		FlexListCellRenderer cell = new FlexListCellRenderer(this, "Enter new term");
		listPanel.add(cell);
		listPanel.revalidate();
		listPanel.repaint();
	}

	public FlexList(List<String> data) {
		this();
		setData(data);
	}
	
	
	public void setData(List<String> data) {
		listPanel.removeAll();
		for(String str : data) {
			FlexListCellRenderer cell = new FlexListCellRenderer(this, str);
			listPanel.add(cell);
		}
		listPanel.revalidate();
		listPanel.repaint();
	}

	/**
	 * Remove this given list item and revalidates and repaints the list
	 * @param listItem
	 */
	public void removeListItem(FlexListCellRenderer listItem) {
		listPanel.remove(listItem);
		listPanel.revalidate();
		repaint();
	}
	
}
