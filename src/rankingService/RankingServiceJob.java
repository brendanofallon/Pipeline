package rankingService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingWorker;

import buffer.CSVFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import pipeline.Pipeline;

/**
 * Represents a running, or possibly completed, variant ranking job. 
 * @author brendan
 *
 */
public class RankingServiceJob extends SwingWorker {

	private final Pipeline pipeline;
	private final AnalysisSettings settings;
	private RankingResults results = null; // null until job completes.
	
	public RankingServiceJob(AnalysisSettings settings, Pipeline ppl) {
		this.pipeline = ppl;
		this.settings = settings;
	}
	
	public Pipeline getPipeline() {
		return pipeline;
	}
	
	public RankingResults getResult() {
		return results;
	}
	
	/**
	 * Obtain a CSV file referencing the list of ranked variants created by the analysis
	 * @return
	 */
	protected CSVFile getFinalVariants() {
		String rootDir = pipeline.getProjectHome();
		if (! rootDir.endsWith(System.getProperty("file.separator")))
			rootDir = rootDir + System.getProperty("file.separator");
		String path = rootDir + settings.prefix + ".ranking.analysis.csv";
		
		System.out.println("path to ranked variants : " + path);
		CSVFile vars = new CSVFile(new File(path));
		return vars;
	}
	
	protected List<VariantRec> getFinalVariantsAsList() throws IOException {
		CSVFile file = getFinalVariants();
		VariantPool pool = new VariantPool(file);
		List<VariantRec> varList = pool.toList();
		return varList;
	}
	
	@Override
	protected Object doInBackground() throws Exception {
		
		pipeline.execute();
		
		results = new RankingResults();
		results.variants = getFinalVariantsAsList();
		
		return null;
	}
	
	@Override 
	protected void done() {
		//Called when analysis has completed... notify someone?
		
	}

}
