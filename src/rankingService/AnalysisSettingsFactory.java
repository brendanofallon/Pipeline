package rankingService;

/**
 * A class to generate an AnalysisSettings object with a few of the fields already
 * filled in with sensible defaults
 * @author brendan
 *
 */
public class AnalysisSettingsFactory {

	public static final String propsPath = System.getProperty("user.home") + "/.pipelineprops.xml";
	
	public static AnalysisSettings getDefaultAnalysisSettings() {
		AnalysisSettings settings = new AnalysisSettings();
		
		settings.graphSize = 200;
		settings.pathToPipelineProperties = propsPath;

		
		return settings;
	}
}
