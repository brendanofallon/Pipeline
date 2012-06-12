package gui.variantTable;

import java.awt.Dimension;
import java.util.Arrays;

import gui.variantTable.flexList.FlexList;
import gui.variantTable.geneList.GeneListPanel;
import gui.widgets.LabelFactory;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

public class TermsInputPanel extends JPanel {
	
	public TermsInputPanel() {
		initComponents();
	}
	
	
	private void initComponents() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		JPanel termsPanel =new JPanel();
		termsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		termsPanel.setLayout(new BoxLayout(termsPanel, BoxLayout.Y_AXIS));
		termsPanel.add(LabelFactory.makeLabel("Enter terms below"));
		FlexList termsList = new FlexList();
		String[] terms = new String[]{"First", "Second", "Third"};
		termsList.setData(Arrays.asList(terms));
		termsPanel.add(termsList);
		termsList.setPreferredSize(new Dimension(200, 300));
		termsPanel.add(Box.createVerticalGlue());
		
		
		GeneListPanel genesPanel = new GeneListPanel();
		genesPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		
		this.add(termsPanel);
		this.add(genesPanel);
	}

}
