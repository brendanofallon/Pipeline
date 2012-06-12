package gui.variantTable.geneList;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import gui.widgets.BorderlessButton;
import gui.widgets.LabelFactory;
import gui.widgets.PrettyLabel;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;

/**
 * Draws a single item in a GeneList
 * @author brendan
 *
 */
public class GeneItemRenderer extends JPanel {
	
	final GeneListPanel list;

	public GeneItemRenderer(GeneListPanel list, String geneName) {
		this.list = list;
		
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		PrettyLabel label = LabelFactory.makeLabel(geneName);
		this.add(label);
		this.add(Box.createHorizontalGlue());
		
		BorderlessButton button = new BorderlessButton("X");
		button.setPreferredSize(new Dimension(28, 28));
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				removeThisItem();
			}
			
		});
		

	}

	protected void removeThisItem() {
		list.removeItem(this);
	}
}
