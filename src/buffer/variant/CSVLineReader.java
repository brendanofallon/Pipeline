package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads a CSV file and produces VariantRecords from it, typically one variant per line
 * If file has a header (a first line that begins with "#"), then assume those are column headers
 * and parse values accordingly. 
 * @author brendan
 *
 */
public class CSVLineReader implements VariantLineReader {

	protected BufferedReader reader;
	protected String currentLine = null;
	protected String[] headerToks = null;
	//Contains a map for a column header to a column index
	private Map<String, Integer> headerMap = new HashMap<String, Integer>();
	
	
	public CSVLineReader(File csvFile) throws IOException {
		reader = new BufferedReader(new FileReader(csvFile));
		currentLine = reader.readLine();
		if (currentLine.startsWith("#")) {
			headerToks = currentLine.split("\t");
			for(int i=0; i<headerToks.length; i++)
				headerMap.put(headerToks[i].trim(), i);
			currentLine = reader.readLine();
		}
	}
	
	/**
	 * Returns true if there is a column header that matches the given String 
	 * @param colHeader
	 * @return
	 */
	public boolean hasHeaderCol(String colHeader) {
		return headerMap.get(colHeader) != null;
	}
	
	/**
	 * Returns the column index associated with the given header 
	 * @param colHeader
	 * @return
	 */
	public int getIndexForHeader(String colHeader) {
		if (!hasHeader())
			throw new IllegalArgumentException("No header was found for this file");
		return headerMap.get(colHeader);
	}
	
	
	public String getValueForHeader(String colHeader, String[] toks) {
		if (!hasHeader())
			throw new IllegalArgumentException("No header was found for this file");
		return toks[headerMap.get(colHeader)];
	}
	
	/**
	 * Returns true if the header line that describes the columns has been specified
	 * @return
	 */
	public boolean hasHeader() {
		return headerToks != null;
	}
	
	@Override
	public boolean advanceLine() throws IOException {
		currentLine = reader.readLine();
		//Skip zero-length lines
		while (currentLine != null && currentLine.length()==0)
			currentLine = reader.readLine();
		return currentLine != null;
	}

	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		
		String contig = getContig(toks);
		Integer start = getStart(toks);
		Integer end = getEnd(toks);
		String ref = getRef(toks);
		String alt = getAlt(toks);
		Double qual = getQuality(toks);
		Double depth = getDepth(toks); 
		boolean isHet = getHet(toks);
		Double genoQual = getGenotypeQuality(toks);
		
		
		if (alt.length() != ref.length()) {
			//Remove initial characters if they are equal and add one to start position
			if (alt.charAt(0) == ref.charAt(0)) {
				alt = alt.substring(1);
				ref = ref.substring(1);
				if (alt.length()==0)
					alt = "-";
				if (ref.length()==0)
					ref = "-";
				start++;
			}
			
			if (ref.equals("-"))
				end = start;
			else
				end = start + ref.length();
		}
		
		VariantRec rec = new VariantRec(contig, start, start+ref.length(), ref, alt, qual, isHet);
		rec.addProperty(VariantRec.DEPTH, depth);
		rec.addProperty(VariantRec.GENOTYPE_QUALITY, genoQual);

		//Parse additional annotations / properties from header
		if (hasHeader() && toks.length > 8) {
			for(int i=9; i<toks.length; i++) {
				String key = headerToks[i].trim();
				if (toks[i].equals("-") || toks[i].equals("NA") || toks[i].equals("?"))
					continue; 
				
				try {
					Double val = Double.parseDouble(toks[i]);
					rec.addProperty(key, val);
				}
				catch (NumberFormatException ex) {
					//this is expected, we just assume it's an annotation, not a property
				}
				
				rec.addAnnotation(key, toks[i]);
			}
		}
		
		
		return rec;
	}

	protected String getContig(String[] toks) {
		return toks[0].replace("chr", "");
	}
	
	protected Integer getStart(String[] toks) {
		return Integer.parseInt(toks[1]);
	}
	
	protected Integer getEnd(String[] toks) {
		return Integer.parseInt(toks[2]);
	}
	
	protected String getRef(String[] toks) {
		return toks[3];
	}
	
	protected String getAlt(String[] toks) {
		return toks[4];
	}
	
	protected Double getQuality(String[] toks) {
		return Double.parseDouble(toks[5]);
	}
	
	protected Double getDepth(String[] toks) {
		return Double.parseDouble(toks[6]);
	}
	
	protected Boolean getHet(String[] toks) {
		return toks[7].contains("het");
	}
	
	protected Double getGenotypeQuality(String[] toks) {
		return Double.parseDouble(toks[8]);
	}
	
}
