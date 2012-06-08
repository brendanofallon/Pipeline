package gui.variantTable.flexList;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class FlexList extends JPanel {

	private List<String> data;
	private JScrollPane scrollPane;
	private JPanel listPanel;
	
	public FlexList() {
		initComponents();
		
		
	}
	
	private void initComponents() {
		setLayout(new BorderLayout());
		listPanel = new JPanel();
		listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
		scrollPane = new JScrollPane(listPanel);
		this.add(scrollPane, BorderLayout.CENTER);
		
	}

	public FlexList(List<String> data) {
		this();
		setData(data);
	}
	
	
	public void setData(List<String> data) {
		listPanel.removeAll();
		for(String str : data) {
			FlexListCellRenderer cell = new FlexListCellRenderer(str);
			listPanel.add(cell);
		}
		listPanel.revalidate();
		listPanel.repaint();
	}
	
}
