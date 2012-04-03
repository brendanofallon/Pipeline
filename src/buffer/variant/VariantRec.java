package buffer.variant;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
	String ref;
	String alt;
	Double qual;
	boolean isHetero;
	private Map<String, Double> props = new HashMap<String, Double>();
	private Map<String, String> annotations = new HashMap<String, String>();
	
	public VariantRec(String contig, 
							int start, 
							int end, 
							String ref, 
							String alt, 
							double qual, 
							//boolean isExon, 
							boolean isHetero) {
		this.contig = contig;
		this.start = start;
		this.end = end;
		this.ref = ref;
		this.alt = alt;
		this.qual = qual;
		this.isHetero = isHetero;
	}
	
	public void addProperty(String key, Double val) {
		props.put(key, val);
	}
	
	public void addAnnotation(String key, String anno) {
		annotations.put(key, anno);
	}
	
	public String getRef() {
		return ref;
	}
	
	/**
	 * Set the start and end positions for this variant
	 * @param start
	 * @param end
	 */
	public void setPosition(String contig, int start, int end) {
		this.contig = contig;
		this.start = start;
		this.end = end;
	}
	
	/**
	 * Returns true if both the ref and alt allele have length 1 and neither is '-'
	 * @return
	 */
	public boolean isSNP() {
		return (ref.length()==1 && alt.length()==1 && ref.charAt(0) != '-' && alt.charAt(0) != '-');
	}
	
	/**
	 * Returns true if ref and alt are different lengths
	 * @return
	 */
	public boolean isIndel() {
		return isInsertion() || isDeletion();
	}
	
	/**
	 * Returns the length of the insertion / deletion, or 0 if this
	 * is not an insertion or deletion
	 * @return
	 */
	public int getIndelLength() {
		if (isInsertion())
			return alt.length();
		if (isDeletion())
			return ref.length();
		return 0;
	}
	
	/**
	 * True if the ref length is 1 and the alt length is strictly greater than one
	 * @return
	 */
	public boolean isInsertion() {
		return ref.equals("-") && (!alt.equals("-"));
	}
	
	/**
	 * True if ref length > 1 and alt length is equal to 1
	 * @return
	 */
	public boolean isDeletion() {
		return alt.equals("-") && (!ref.equals("-"));
	}
	
	public boolean isTransition() {
		if (ref == null || alt == null)
			throw new IllegalArgumentException("Ref or alt is null");
		if (ref.equals("-") || alt.equals("-")) {
			throw new IllegalArgumentException("Ref or alt not defined");
		}
		if (ref.equals(alt)) {
			System.err.println("WARNING : ref is equal to alt, not a variant");
			return false;
			//throw new IllegalArgumentException("Ref is equal to alt, not a variant");
		}
		
		if ( (ref.equals("A") && alt.equals("G"))
			  || (ref.equals("G") && alt.equals("A"))
			  || (ref.equals("T") && alt.equals("C"))
			  || (ref.equals("C") && alt.equals("T"))) {
			return true;
		}
		return false;
	}
	
	public boolean isTransversion() {
		if (ref == null || alt == null)
			throw new IllegalArgumentException("Ref or alt is null");
		if (ref.equals("-") || alt.equals("-")) {
			throw new IllegalArgumentException("Ref or alt not defined");
		}
		if (ref.equals(alt)) {
			System.err.println("WARNING : ref is equal to alt, not a variant");
			return false;
			//throw new IllegalArgumentException("Ref is equal to alt, not a variant");
		}
		
		if (ref.equals("A") || ref.equals("G")) {
			if (alt.equals("C") || alt.equals("T"))
				return true;
			else
				return false;			
		}
		
		if (ref.equals("T") || ref.equals("C")) {
			if (alt.equals("A") || alt.equals("G"))
				return true;
			else
				return false;			
		}
		
		return false;
	}
	
	/**
	 * Returns true if alt is not equal in value to ref
	 * @return
	 */
	public boolean isVariant() {
		return !getAlt().equals(getRef());
	}
	
	public String getAlt() {
		return alt;
	}
	
	public Double getProperty(String key) {
		return props.get(key);
	}
	
	/**
	 * Returns the property associated with the given key, but if there
	 * is no such property, returns the annotation with the given key, and
	 * if there's no annotation either returns "-";
	 * @param key
	 * @return
	 */
	public String getPropertyOrAnnotation(String key) {
		Double val = getProperty(key);
		if (val != null)
			return "" + val;
		
		String anno = getAnnotation(key);
		if (anno != null)
			return anno;
		
		return "-";
	}
	
	/**
	 * Obtain a Collection containing all keys used to describe properties
	 * @return
	 */
	public Collection<String> getPropertyKeys() {
		return props.keySet();
	}
	
	/**
	 * Collection of all keys used for annotations
	 * @return
	 */
	public Collection<String> getAnnotationKeys() {
		return annotations.keySet();
	}
	
	public String getAnnotation(String key) {
		return annotations.get(key);
	}
	
	public boolean hasProperty(String key) {
		return props.get(key)!=null;
	}
	
	/**
	 * Returns a tab-separated string containing properties (but not annotation) 
	 * values indexed by the list of keys given
	 * @param propKeys
	 * @return
	 */
	public String getPropertyString(List<String> propKeys) {
		StringBuffer buf = new StringBuffer();
		for(String key : propKeys) {
			buf.append("\t " + getPropertyOrAnnotation(key));
//			Double val = props.get(key);
//			if (val != null)
//				buf.append("\t" + val);
//			else {
//				String anno = annotations.get(key);
//				if (key == VariantRec.RSNUM && anno != null) {
//					buf.append("\t =HYPERLINK(\"http://www.ncbi.nlm.nih.gov/snp/?term=" + anno + "\")");
//				}
//				else {
//					if (anno != null)
//						buf.append("\t" + anno);
//					else
//						buf.append("\t NA");
//				}
//			}
		}
		return buf.toString();
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
	
	
	public static String getColumnHeaders() {
		return "contig \t start \t end \t gene \t variant.type \t exon.func \t pop.freq \t het \t qual \t sift \t polyphen \t mt \t phylop";  
	}
	
	/**
	 * Obtain a header string for the column emitted by toBasicString
	 * @return
	 */
	public static String getBasicHeader() {
		return "#contig \tstart \tend \tgene \tvariant.type \texon.function \tzygosity";
	}
	
	/**
	 * Obtain a string containing the following information about this variant record:
	 * 1. contig
	 * 2. start
	 * 3. end
	 * 4. gene name
	 * 5. variant type
	 * 6. exon function (- if not an exon)
	 * 7. hetero/homo
	 * @return
	 */
	public String toBasicString() {
		String gene = getAnnotation(VariantRec.GENE_NAME);
		if (gene == null)
			gene = "-";
		
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
		
		return contig + "\t" + start + "\t" + end + "\t" + gene + "\t" + variantType + "\t" + exFunc + "\t" + het ;  
	}
	
	/**
	 * Return a header row that describes the toSimpleString() columns
	 * @return
	 */
	public static String getSimpleHeader() {
		return "#contig	\t start \t end \t ref \t alt \t quality \t depth \t zygosity \t genotype.quality \t " + VariantRec.VAR_DEPTH;
	}
	
	/**
	 * Return a string with the following columns:
	 * 1. Contig
	 * 2. start
	 * 3. end
	 * 4. ref
	 * 5. alt
	 * 6. variant quality
	 * 7. total read depth
	 * 8. variant read depth
	 * 9. het / hom
	 * 10. genotype quality
	 * @return
	 */
	public String toSimpleString() {
		String het = "het";
		if (! isHetero())
			het = "hom";
		
		Double depth = getProperty(VariantRec.DEPTH);
		String depthStr = "-";
		if (depth != null) 
			depthStr = "" + depth;
		
		Double genotypeQual = getProperty(VariantRec.GENOTYPE_QUALITY);
		String gqStr = "-";
		if (genotypeQual != null)
			gqStr = "" + genotypeQual;
		
		Double varDepth = getProperty(VariantRec.VAR_DEPTH);
		String varDepthStr = "-";
		if (varDepth != null)
			varDepthStr = varDepth + "";
		
		return contig + "\t" + start + "\t" + end + "\t" + getRef() + "\t" + getAlt() + "\t" + getQuality() + "\t" + depthStr + "\t" + het + "\t" + gqStr + "\t" + varDepthStr;  
	}
	
	/**
	 * Adjusts all indel variants in the following manner: Any indel that begins and ends with the same
	 * base, the first base is moved to the last position and 1 is subtracted from the start and end position
	 * So		: 117  - ACGTA
	 * Becomes 	: 116  - CGTAA
	 */
//	public void rotateIndel() {
//		if (isInsertion()) {
//			int count = 0;
//			while (count < 20 && alt.charAt(0) == alt.charAt(alt.length()-1)) {
//				alt = alt.substring(1) + alt.charAt(0);
//				start--;
//				end--;
//				count++;
//			}
//		}
//		
//		if (isDeletion()) {
//			int count = 0;
//			while (count < 20 && ref.charAt(0) == ref.charAt(ref.length()-1)) {
//				ref = ref.substring(1) + ref.charAt(0);
//				start--;
//				end--;
//				count++;
//			}
//		}
//	}
	
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

		String phylopStr = "NA";
		Double phylop = getProperty(VariantRec.POP_FREQUENCY);
		if (phylop != null)
			phylopStr = "" + phylop;
		
		String freq = "0.0";
		Double pFreq = getProperty(VariantRec.POP_FREQUENCY);
		if (pFreq != null)
			freq = "" + pFreq;
		
		String gene = getAnnotation(VariantRec.GENE_NAME);
		if (gene == null)
			gene = "-";
		
		return contig + "\t" + start + "\t" + end + "\t" + ref + "\t" + alt;
		//return contig + "\t" + start + "\t" + end + "\t" + gene + "\t" + variantType + "\t" + exFunc + "\t" + freq + "\t" + het + "\t" + qual + "\t" + sift + "\t" + polyphen + "\t" + mt + "\t" + phylopStr;  
	}
	
	/**
	 * Obtain an object that compares two variant records for start site
	 * @return
	 */
	public static PositionComparator getPositionComparator() {
		return posComparator;
	}
	
	/**
	 * Obtain an object that compares two variant records for the property
	 * associated with the given key
	 * @return
	 */
	public static PropertyComparator getPropertyComparator(String key) {
		return new PropertyComparator(key);
	}
	
	
	public static class PositionComparator implements Comparator<VariantRec> {

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
	
	private static final PositionComparator posComparator =  new PositionComparator();
	
	//A few oft-used property / annotation keys
	public static final String POP_FREQUENCY = "pop.freq";
	public static final String SIFT_SCORE = "sift.score";
	public static final String POLYPHEN_SCORE = "pp.score";
	public static final String MT_SCORE = "mt.score";
	public static final String SIFT_QUARTILE = "sift.quartile";
	public static final String MT_QUARTILE = "mt.quartile";
	public static final String POLYPHEN_QUARTILE = "pp.quartile";
	public static final String PHYLOP_QUARTILE = "phylop.quartile";
	public static final String PHYLOP_SCORE = "phylop.score";
	public static final String VARIANT_TYPE = "variant.type";
	public static final String EXON_FUNCTION = "exon.function";
	public static final String EXON_NUMBER = "exon.number";
	public static final String NM_NUMBER = "nm.number";
	public static final String GENE_NAME = "gene";
	public static final String DEPTH = "depth";
	public static final String CDOT = "cdot";
	public static final String PDOT = "pdot";
	public static final String zygosityB = "zygB"; //When performing intersections, zygosity of variant in other pool
	public static final String altB = "altB"; //Alternate allele in other pool when performing intersections
	public static final String RSNUM = "rsnum"; // rs# from dbSNP
	public static final String OMIM_ID = "omim.id";
	public static final String GO_FUNCTION = "go.function";
	public static final String GO_PROCESS = "go.process";
	public static final String GO_COMPONENT = "go.component";
	public static final String GENOTYPE_QUALITY = "genotype.quality";
	public static final String SOURCE = "source.file";
	public static final String VAR_DEPTH = "var.depth";
	public static final String FALSEPOS_PROB = "fp.prob";

	
	

	
}
