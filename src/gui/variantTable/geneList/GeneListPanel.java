package gui.variantTable.geneList;

import gui.widgets.BorderlessButton;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class GeneListPanel extends JPanel {

	public GeneListPanel() {
		initComponents();
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
		JScrollPane scrollPane = new JScrollPane(listPanel);
		this.add(scrollPane, BorderLayout.CENTER);
		
	}
	
	public void setData(List<String> geneNames) {
		for(String name : geneNames) {
			GeneItemRenderer geneItem = new GeneItemRenderer(this, name);
			listPanel.add(geneItem);
		}
		listPanel.revalidate();
		listPanel.repaint();
	}
	
	
	protected void addNewItem() {
		GeneItemRenderer geneItem = new GeneItemRenderer(this, "New gene");
		listPanel.add(geneItem);
	}

	public void removeItem(GeneItemRenderer geneItem) {
		listPanel.remove(geneItem);
		listPanel.revalidate();
		listPanel.repaint();
	}

	private JPanel listPanel;


	
}
