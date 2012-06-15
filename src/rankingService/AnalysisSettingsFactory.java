package rankingService;

/**
 * A class to generate an AnalysisSettings object with a few of the fields already
 * filled in with sensible defaults
 * @author brendan
 *
 */
public class AnalysisSettingsFactory {

	
	public static AnalysisSettings getDefaultAnalysisSettings() {
		AnalysisSettings settings = new AnalysisSettings();
		
		settings.graphSize = 200;
		settings.pathToPipelineProperties = "/home/brendan/.pipelineprops.xml";

		
		return settings;
	}
}
