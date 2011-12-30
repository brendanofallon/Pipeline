package buffer.variant;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * A class to store some basic information about a single variant
 * @author brendan
 *
 */
public class VariantRec {

	String contig;
	int start;
	int end;
	char ref;
	char alt;
	Double qual;
	boolean isExonic;
	boolean isHetero;
	private Map<String, Double> props = new HashMap<String, Double>();
	private Map<String, String> annotations = new HashMap<String, String>();
	
	public VariantRec(String contig, 
							int start, 
							int end, 
							char ref, 
							char alt, 
							double qual, 
							boolean isExon,
							boolean isHetero) {
		this.contig = contig;
		this.start = start;
		this.end = end;
		this.ref = ref;
		this.alt = alt;
		this.qual = qual;
		this.isExonic = isExon;
		this.isHetero = isHetero;
	}
	
	public void addProperty(String key, Double val) {
		props.put(key, val);
	}
	
	public void addAnnotation(String key, String anno) {
		annotations.put(key, anno);
	}
	
	public Double getProperty(String key) {
		return props.get(key);
	}
	
	public String getAnnotation(String key) {
		return annotations.get(key);
	}
	
	public boolean hasProperty(String key) {
		return props.get(key)!=null;
	}
	
	public String getContig() {
		return contig;
	}
	
	public int getStart() {
		return start;
	}
	
	public int getEnd() {
		return end;
	}
	
	public double getQuality() {
		return qual;
	}
	
	public boolean isHetero() {
		return isHetero;
	}
		
	public String toString() {
		String variantType = "-";
		String vType = getAnnotation(VariantRec.VARIANT_TYPE);
		if (vType != null)
			variantType = vType;

		String exFunc = "-";
		String exType = getAnnotation(VariantRec.EXON_FUNCTION);
		if (exType != null)
			exFunc = exType;
		
		String het = "het";
		if (! isHetero())
			het = "hom";
		
		String sift = "NA";
		Double score = getProperty(VariantRec.SIFT_SCORE);
		if (score != null)
			sift = "" + score;

		String polyphen = "NA";
		Double ppScore = getProperty(VariantRec.POLYPHEN_SCORE);
		if (ppScore != null)
			polyphen = "" + ppScore;
		
		String mt = "NA";
		Double mtScore = getProperty(VariantRec.MT_SCORE);
		if (mtScore != null)
			mt = "" + mtScore;
		
		String freq = "0.0";
		Double pFreq = getProperty(VariantRec.POP_FREQUENCY);
		if (pFreq != null)
			freq = "" + pFreq;
		
		return contig + "\t" + start + "\t" + end + "\t" + variantType + "\t" + exFunc + "\t" + freq + "\t" + het + "\t" + qual + "\t" + sift + "\t" + polyphen + "\t" + mt;  
	}
	
	/**
	 * Obtain an object that compares two variant records for start site
	 * @return
	 */
	public static PositionComparator getPositionComparator() {
		return new PositionComparator();
	}
	
	/**
	 * Obtain an object that compares two variant records for the property
	 * associated with the given key
	 * @return
	 */
	public static PropertyComparator getPropertyComparator(String key) {
		return new PropertyComparator(key);
	}
	
	
	static class PositionComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec o1, VariantRec o2) {
				if (o1 == o2 || (o1.equals(o2))) {
					return 0;
				}
				
				return o1.start - o2.start;
			}
	}
	
	/**
	 * A generic comparator that retrieves the property with the given key and compares
	 * two variant records to see which value is greater. An exception is thrown if
	 * one record does not contain a property associated with the key
	 * @author brendan
	 *
	 */
	static class PropertyComparator implements Comparator<VariantRec> {

		final String key;
		
		public PropertyComparator(String key) {
			this.key= key;
		}
		
		@Override
		public int compare(VariantRec o1, VariantRec o2) {
			Double val1 = o1.getProperty(key);
			Double val2 = o2.getProperty(key);
			if (val1 == null)
				throw new IllegalArgumentException("Cannot compare variant record for key " + key + " because it has not been assigned to var1");
			if (val2 == null)
				throw new IllegalArgumentException("Cannot compare variant record for key " + key + " because it has not been assigned to var2");
			
			return val1 < val2 ? -1 : 1;
		}
		
	}
	
	
	
	//A few oft-used property keys
	public static final String POP_FREQUENCY = "popfreq";
	public static final String SIFT_SCORE = "siftscore";
	public static final String POLYPHEN_SCORE = "ppscore";
	public static final String MT_SCORE = "mtscore";
	public static final String SIFT_QUARTILE = "siftquartile";
	public static final String MT_QUARTILE = "mtquartile";
	public static final String POLYPHEN_QUARTILE = "ppquartile";
	public static final String VARIANT_TYPE = "varianttype";
	public static final String EXON_FUNCTION = "exonfunction";
	public static final String NM_NUMBER = "nmnumber";
	public static final String GENE_NAME = "gene";
	
}
