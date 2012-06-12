package gui.variantTable;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Arrays;

import gui.variantTable.flexList.FlexList;
import gui.variantTable.geneList.GeneListPanel;
import gui.widgets.BorderlessButton;
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
		
		
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(LabelFactory.makeLabel("Enter genes", 14f), BorderLayout.NORTH);
		
		GeneListPanel genesPanel = new GeneListPanel();
		genesPanel.setData(Arrays.asList(new String[]{"ENG", "A!@", "ASKDJ", "ASLDK"}));
		rightPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		rightPanel.add(genesPanel, BorderLayout.CENTER);
		
		
		this.add(termsPanel);
		this.add(rightPanel);
		
		JPanel bottomPanel = new JPanel();
		BorderlessButton beginButton= new BorderlessButton("Begin");
		bottomPanel.setLayout(new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
		bottomPanel.add(Box.createHorizontalGlue());
		bottomPanel.add(beginButton);
		bottomPanel.add(Box.createRigidArea(new Dimension(30, 30)));
		this.add(bottomPanel, BorderLayout.SOUTH);
		
	}

}
