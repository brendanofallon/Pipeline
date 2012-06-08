package gui.variantTable;

import java.awt.BorderLayout;
import java.awt.Color;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.TableColumn;

import buffer.variant.VariantRec;

/**
 * A JPanel containing a table (in a scrollpane) that shows a variant pool
 * @author brendan
 *
 */
public class VariantTablePanel extends JPanel {

	
	public VariantTablePanel() {
		initComponents();
	}

	public void setVariantPool(List<VariantRec> variants) {
		model.setVariants(variants);
		repaint();
	}
	
	private void initComponents() {
		this.setLayout(new BorderLayout());
		
		table = new JTable(model);
		table.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
		table.setGridColor(Color.LIGHT_GRAY);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(false);
		table.setDefaultRenderer(Object.class, new VariantCellRenderer());
		
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setViewportBorder(BorderFactory.createEmptyBorder());
		scrollPane.setBorder(BorderFactory.createLineBorder(Color.gray));
		setColumns(defaultColumns);
		this.add(scrollPane, BorderLayout.CENTER);	
	}
	
	public void setColumns(List<String> keys) {
		
		model.setColumnKeys(keys);
		for(int i=0; i<keys.size(); i++) {
			TableColumn col = table.getColumnModel().getColumn(i);
			col.setHeaderValue(keys.get(i));
			col.setHeaderRenderer(new TableHeaderRenderer(model, keys.get(i)));
		}
	}
	
	JTable table;
	VariantTableModel model = new VariantTableModel();
	List<String> defaultColumns = Arrays.asList(new String[]{VariantTableModel.CONTIG, VariantTableModel.POS, VariantTableModel.REF, VariantTableModel.ALT, VariantTableModel.QUALITY, VariantRec.DEPTH});
}
