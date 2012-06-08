package gui.variantTable;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

public class VariantCellRenderer extends JPanel implements TableCellRenderer {

	Font font = new Font("Sans", Font.PLAIN, 12);
	static final Color topColor = Color.WHITE;
	static final Color stripeColor = new Color(0.9f, 0.9f, 0.9f);
	static final Color textColor = Color.DARK_GRAY;
	static final Color textShadow = new Color(1f, 1f, 1f, 0.5f);
	static final Color selectedColor = new Color(0f, 0.25f, 0.5f, 0.5f);
	private String text = null;
	private int row = 0;
	private boolean selected = false;
	
	@Override
	public Component getTableCellRendererComponent(JTable table, Object value,
			boolean isSelected, boolean hasFocus, int row, int column) {
		
		this.text = value.toString();
		this.row = row;
		this.selected = isSelected;
		return this;
	}
	
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
		
		
		if (row % 2 == 1) {
			g2d.setColor(stripeColor);
			g2d.fillRect(0, 0, getWidth(), getHeight());
		}
		
		if (selected) {
			g2d.setColor(selectedColor);
			g2d.fillRect(0, 0, getWidth(), getHeight());			
		}
		
		g.setFont(font);
		int strWidth = g.getFontMetrics().stringWidth(text);
		int leftEdge = Math.max(getWidth()/2 - strWidth/2, 1);
		
		g2d.setColor(textColor);
		g2d.drawString(text, leftEdge, getHeight()-4);
		
	}

}
