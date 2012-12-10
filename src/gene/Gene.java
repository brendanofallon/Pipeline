package gene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * This is the gene analogue of a VariantRec. It's used to associate
 * multiple pieces of information with a single gene. Unlike VariantRecs, 
 * relatively little data is required, just a gene name. These are also unordered
 * @author brendan
 *
 */
public class Gene {

	final String name; //This is the NCBI-approved name
	
	private Map<String, String> annotations = new HashMap<String, String>();
	private Map<String, Double> properties = new HashMap<String, Double>();
	
	public Gene(String ncbiName) {
		this.name = ncbiName;
	}
	
	public String getAnnotation(String key) {
		return annotations.get(key);
	}
	
	public Collection<String> getAnnotationKeys() {
		return annotations.keySet();
	}
	
	public void addAnnotation(String key, String value) {
		annotations.put(key, value);
	}
	
	public Double getProperty(String key) {
		return properties.get(key);
	}
	
	public Collection<String> getPropertyKeys() {
		return properties.keySet();
	}
	
	public void addProperty(String key, Double value) {
		properties.put(key, value);
	}

	/**
	 * Get the NCBI approved name for this gene
	 * @return
	 */
	public String getName() {
		return name;
	}
	
	public String getPropertyOrAnnotation(String key) {
		String anno = this.getAnnotation(key);
		if (anno == null) {
			Double val = this.getProperty(key);
			if (val != null)
				anno = "" + val;
		}
		
		if (anno == null)
			anno = "-";
		return anno;
	}
	
	public static final String SUMMARY = "summary";
	public static final String SUMMARY_SCORE = "summary.score";
	
	public static final String DBNSFP_FUNCTIONDESC = "dbnsfp.function.desc";
	public static final String DBNSFP_DISEASEDESC = "dbnsfp.disease.desc";
	public static final String DBNSFP_MIMDISEASE = "dbnsfp.mim.disease";
	public static final String DBNSFPGENE_SCORE = "dbnsfp.score";
	public static final String EXPRESSION = "expression";
	public static final String EXPRESSION_HITS = "expression.hits";
	public static final String EXPRESSION_SCORE = "expression.score";
	
	public static final String GO_FUNCTION = "go.function";
	public static final String GO_PROCESS = "go.process";
	public static final String GO_COMPONENT = "go.component";
	public static final String GO_SCORE = "go.score";
	public static final String GO_HITS = "go.hits";
	
	public static final String HGMD_INFO = "hgmd.info";
	
	public static final String GENE_RELEVANCE = "relevance.score";
	
	public static final String INTERACTION_SCORE = "interaction.score";
	
	public static final String PUBMED_SCORE = "pubmed.score";
	public static final String PUBMED_HIT = "pubmed.hit";


	
}
