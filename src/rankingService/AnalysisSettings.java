package rankingService;

import java.util.List;
import java.util.Map;

/**
 * A container class for information about the settings for a particular analysis run
 * @author brendan
 *
 */
public class AnalysisSettings {

	public List<String> genes = null;
	public Map<String, Integer> goTerms = null;
	public Map<String, Integer> summaryTerms = null;
	public Integer graphSize = null;
	public String rootPath = null;
	public String pathToVCF = null;
	public String prefix = null;
	public String pathToPipelineProperties = null;
	
	public double popFreqCutoff = 0.10;
	public double depthCutoff = 0;
	public double varDepthCutoff = 0;
	public double varFreqCutoff = 0;
	public boolean includeDBSNP = true;
	public String zygosityFilter = "ALL";
	
}
