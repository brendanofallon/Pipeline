package gui.variantTable;

import gui.widgets.BorderlessButton;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class TableHeaderRenderer extends JPanel implements TableCellRenderer {

	private String text = null;
	Font font = new Font("Sans", Font.PLAIN, 14);
	static final Color topColor = Color.WHITE;
	static final Color bottomColor = new Color(0.75f, 0.75f, 0.75f);
	static final Color textColor = Color.DARK_GRAY;
	static final Color textShadow = new Color(1f, 1f, 1f, 0.5f);
	private VariantTableModel tableModel = null;
	
	public TableHeaderRenderer(VariantTableModel model, String headerText) {
		this.text = headerText;
		this.tableModel = model;
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		this.add(Box.createRigidArea(new Dimension(10, 30)));
		this.add(Box.createHorizontalGlue());
		BorderlessButton sortButton = new BorderlessButton("X");
		sortButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sortByColumn();
			}
		});
		this.add(sortButton);
	}
	
	protected void sortByColumn() {
		System.out.println("Button clicked!");
		tableModel.sortByKey(text);
	}

	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		return this;
	}
	
	
	public void paintComponent(Graphics g) {
		
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		
		GradientPaint gp = new GradientPaint(0, 0, topColor, 0, getHeight(), bottomColor);
		g2d.setPaint(gp);
		g2d.fillRect(0, 0, getWidth(), getHeight());
		
		super.paintComponent(g);
		
		g.setFont(font);
		int strWidth = g.getFontMetrics().stringWidth(text);
		int leftEdge = Math.max(getWidth()/2 - strWidth/2, 1);
		g2d.setColor(textShadow);
		g2d.drawString(text, leftEdge+1, getHeight()-3);
		g2d.setColor(textColor);
		g2d.drawString(text, leftEdge, getHeight()-4);
		
	}

}
