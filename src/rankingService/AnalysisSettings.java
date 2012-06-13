package rankingService;

import java.util.List;
import java.util.Map;

/**
 * A container class for information about the settings for a particular analysis run
 * @author brendan
 *
 */
public class AnalysisSettings {

	List<String> genes = null;
	Map<String, Integer> goTerms = null;
	Map<String, Integer> summaryTerms = null;
	Integer graphSize = null;
	String rootPath = null;
	String pathToVCF = null;
	String prefix = null;
	String pathToPipelineProperties = null;
	
}
