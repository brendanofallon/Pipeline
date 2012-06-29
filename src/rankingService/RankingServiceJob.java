package rankingService;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingWorker;

import operator.Operator;
import operator.ParallelOperator;
import operator.annovar.GeneAnnotator;
import operator.variant.DBNSFPAnnotator;
import operator.variant.GOTermRanker;
import operator.variant.GeneSummaryRanker;
import operator.variant.InteractionRanker;
import operator.variant.PubmedRanker;

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
	private boolean error = false;
	private Exception exception = null;
	
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
	
	public String getPathToFinalVariants() {
		String rootDir = pipeline.getProjectHome();
		if (! rootDir.endsWith(System.getProperty("file.separator")))
			rootDir = rootDir + System.getProperty("file.separator");
		String path = rootDir + settings.prefix + ".analysis.csv";
		return path;
	}
	
	public String getPathToAllVariants() {
		String rootDir = pipeline.getProjectHome();
		if (! rootDir.endsWith(System.getProperty("file.separator")))
			rootDir = rootDir + System.getProperty("file.separator");
		String path = rootDir + settings.prefix + ".all.csv";
		return path;
	}
	
	/**
	 * Obtain a CSV file referencing the list of ranked variants created by the analysis
	 * @return
	 */
	protected CSVFile getFinalVariants() {
		String path = getPathToFinalVariants();
		CSVFile vars = new CSVFile(new File(path));
		return vars;
	}
	
	protected List<VariantRec> getFinalVariantsAsList() throws IOException {
		CSVFile file = getFinalVariants();
		VariantPool pool = new VariantPool(file);
		List<VariantRec> varList = pool.toList();
		return varList;
	}
	
	/**
	 * Returns true if an exception was thrown by the Pipeline during execution
	 * @return
	 */
	public boolean isError() {
		return error;
	}
	
	/**
	 * Returns any exception that was thrown by the Pipeline during execution
	 * @return
	 */
	public Exception getException() {
		return exception;
	}
	
	public String getStatusMessage() {
		if (! pipeline.isInitialized()) {
			return "Waiting to initialize";
		}
		if (pipeline.isInitialized() && (! pipeline.isExecuteStarted())) {
			return "Initialized, waiting to execute";
		}
		if (pipeline.isExecuteStarted() && (! pipeline.isExecuteCompleted())) {
			Operator op = pipeline.getCurrentOperator();
			String opStr = "Reading variants";
			if (op.getClass().equals(GeneAnnotator.class)) {
				opStr = "Determining gene information";
			}
			
			if (op.getClass().equals(DBNSFPAnnotator.class)) {
				opStr = "Annotating variants";
			}
			if (op.getClass().equals(PubmedRanker.class)) {
				opStr = "Examining pubmed abstracts";
			}
			if (op.getClass().equals(GeneSummaryRanker.class)) {
				opStr = "Examining gene summaries";
			}
			if (op.getClass().equals(InteractionRanker.class)) {
				opStr = "Examining gene interaction network";
			}
			if (op.getClass().equals(GOTermRanker.class)) {
				opStr = "Examining G.O. terms";
			}
			return "Running - " + opStr;
		}
		if (pipeline.isExecuteCompleted()) {
			return "Completed";
		}
		
		return "Unknown status";
	}
	
	@Override
	protected Object doInBackground() throws Exception {

		try {
			pipeline.execute();
			results = new RankingResults();
			results.variants = getFinalVariantsAsList();
		}
		catch (Exception ex) {
			error = true;
			exception = ex;
		}
		
		return null;
	}
	
	@Override 
	protected void done() {
		//Called when analysis has completed... notify someone?
		
	}



}
