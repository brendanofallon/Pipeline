package gui.variantTable;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class VariantTableModel extends AbstractTableModel {

	List<VariantRec> variants = null;
	List<String> columnKeys = null;
	
	public void setColumnKeys(List<String> keys) {
		this.columnKeys = keys;
		
		this.fireTableStructureChanged();
	}
	
	
	public void setVariants(List<VariantRec> variants) {
		this.variants = variants;
		this.fireTableDataChanged();
	}
	
	@Override
	public int getRowCount() {
		if (variants == null)
			return 1;
		else
			return variants.size();
	}

	@Override
	public int getColumnCount() {
		if (columnKeys == null) {
			return 1;
		}
		return columnKeys.size();
	}

	public void sortByKey(String key) {
		VariantRowComparator comp = new VariantRowComparator(key);
		Collections.sort(variants, comp);
		setVariants(variants);
	}
	
	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		if(variants == null) {
			return "";
		}
		if (columnKeys == null) {
			return "No columns specified";
		}
		
		VariantRec var = variants.get(rowIndex);
		String key = columnKeys.get(columnIndex);
		return getValueForColumnKey(var, key);
	}

	private static String getValueForColumnKey(VariantRec var, String key) {
		if (key == CONTIG) {
			return var.getContig();
		}
		if (key == POS) {
			return "" + var.getStart();
		}
		if (key == REF) {
			return var.getRef();
		}
		if (key == ALT) {
			return var.getAlt();
		}
		if (key == QUALITY) {
			return "" + var.getQuality();
		}
		
		return var.getPropertyOrAnnotation(key);
	}
	
	
	class VariantRowComparator implements Comparator<VariantRec> {

		final String key;
		
		public VariantRowComparator(String key) {
			this.key = key;
		}
		
		@Override
		public int compare(VariantRec v1, VariantRec v2) {
			String str1 = getValueForColumnKey(v1, key);
			String str2 = getValueForColumnKey(v2, key);
			
			try {
				Double val1 = Double.parseDouble(str1);
				Double val2 = Double.parseDouble(str2);
				if (val1 == val2)
					return 0;
				if (val1 < val2) {
					return -1;
				}
				else {
					return 1;
				}
			}
			catch (NumberFormatException ex) {
				//don't do anything
			}
			
			return str1.compareTo(str2);
		}
		
	}
	
	public static final String CONTIG = "contig";
	public static final String POS = "pos";
	public static final String REF = "ref";
	public static final String ALT = "alt";
	public static final String QUALITY = "qual";

	
}
