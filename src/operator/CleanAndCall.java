package operator;

import java.io.File;
import java.util.List;

import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.VCFFile;

/**
 * An experimental operator that combines remove dups, local realignment, recalibration, and
 * all the required sorting and indexing operations in one fell swoop. This idea is that since these
 * can be run separately on each contig, we should start the next operation as soon as we can.
 * When each operation is done in a separate operator, we're constantly waiting until all jobs have
 * finished before moving on to the next op, which is wasting a lot of time. 
 * @author brendan
 *
 */
public class CleanAndCall extends MultiOperator {

	public final String defaultMemOptions = " -Xms512m -Xmx2g";
	public static final String PATH = "path";
	public static final String THREADS = "threads";
	public static final String JVM_ARGS="jvmargs";
	protected String defaultSamtoolsPath = "samtools";
	protected String samtoolsPath = defaultSamtoolsPath;
	protected String defaultGATKPath = "~/GenomeAnalysisTK/GenomeAnalysisTK.jar";
	protected String gatkPath = defaultGATKPath;
	protected String jvmARGStr = null;
	private boolean pathsInitialized = false;
	private StringBuffer knownSitesStr; //Stores list of known sites files for covariate counting
	
	private void initializePaths() {
		jvmARGStr = properties.get(JVM_ARGS);
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = (String) Pipeline.getPropertyStatic(JVM_ARGS);
		}
		//If it's still null then be sure to make it the empty string
		if (jvmARGStr == null || jvmARGStr.length()==0) {
			jvmARGStr = "";
		}
		
		Object propsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.GATK_PATH);
		if (propsPath != null)
			gatkPath = propsPath.toString();
		
		String path = properties.get(PATH);
		if (path != null) {
			gatkPath = path;
		}
		
		Object samPropsPath = Pipeline.getPropertyStatic(PipelineXMLConstants.SAMTOOLS_PATH);
		if (samPropsPath != null)
			samtoolsPath = samPropsPath.toString();
	
		String samPath = properties.get(PATH);
		if (samPath != null) {
			samtoolsPath = samPath;
		}
		pathsInitialized = true;
		
		
		List<FileBuffer> knownSitesFiles = getAllInputBuffersForClass(VCFFile.class);
		knownSitesStr = new StringBuffer();
		for(FileBuffer buff : knownSitesFiles) {
			knownSitesStr.append("-knownSites " + buff.getAbsolutePath() + " ");
		}
	}
	
	@Override
	protected String[] getCommand(FileBuffer inputBuffer) {
		if (!pathsInitialized) 
			initializePaths();
		
		String projHome = Pipeline.getPipelineInstance().getProjectHome();
		if (! projHome.endsWith("/"))
			projHome = projHome + "/";
		
		String referencePath = reference.getAbsolutePath();
		String contig = inputBuffer.getContig();
		
		//First command is to remove pcr dups
		String dedupPath = inputBuffer.getAbsolutePath().replace(".bam", ".dedup.bam");
		String rmdupCommand = samtoolsPath + " rmdup " + inputBuffer.getAbsolutePath() + " " + dedupPath;
		
		//Now re-index
		String index1 = samtoolsPath + " index " + dedupPath;
		
		//Now do local realignment
		//First, generate realignment targets
		String targetsPath = projHome + "targets_" + contig + "_" + (int)(10000*Math.random()) + ".intervals";
		String createTargetsCommand = "java -Xmx2g " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + referencePath + 
				" -I " + dedupPath + 
				" -T RealignerTargetCreator -o " + targetsPath;
		if (contig != null)
			createTargetsCommand = createTargetsCommand +	" -L " + contig;
		
		//Now actually do the realignment
		String realignedContigPath = dedupPath.replace(".bam", ".realigned.bam");
		String realignContigCommand = "java -Xmx2g " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + referencePath + 
				" -I " + dedupPath + 
				" -T IndelRealigner " + 
				" -targetIntervals " + targetsPath + " -o " + realignedContigPath;
		if (contig != null)
			realignContigCommand = realignContigCommand +	" -L " + contig;
		
		
		//Now sort the resulting contig
		String sortedContigPrefix = projHome + realignedContigPath.replace(".bam", ".sorted");
		String sortRealignedCommand = samtoolsPath + " sort " + inputBuffer.getAbsolutePath() + " " + sortedContigPrefix;
		String sortedContigPath = sortedContigPrefix + ".bam";
		
		//...and index the re-sorted contig
		String index2 = samtoolsPath + " index " + sortedContigPath;
		
		
		//Now count the covariates for the contig
		String covariateList = "-cov QualityScoreCovariate -cov CycleCovariate -cov DinucCovariate -cov MappingQualityCovariate -cov HomopolymerCovariate ";
		
		CSVFile recalDataFile = new CSVFile(new File(projHome + "contig_" + contig + "_" + (int)(10000*Math.random()) + ".csv"));
		String countCovarCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference.getAbsolutePath() + 
				" -I " + inputBuffer.getAbsolutePath() + 
				" -T CountCovariates " + 
				covariateList + " "	+ 
				knownSitesStr.toString() +
				" -recalFile " + recalDataFile.getAbsolutePath();
		if (contig != null) {
			countCovarCommand = countCovarCommand + " -L " + inputBuffer.getContig();
		}
		
		//And now apply the recalibration
		BAMFile recalBamFile = new BAMFile(new File(projHome + "contig_" + contig + ".recal.bam"), contig);
		String applyRecalCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath + 
				" -R " + reference.getAbsolutePath() + 
				" -I " + inputBuffer.getAbsolutePath() + 
				" -T TableRecalibration " + 
				" -o " + recalBamFile.getAbsolutePath() + 
				" -recalFile " + recalDataFile.getAbsolutePath();
		if (contig != null) {
			applyRecalCommand = applyRecalCommand + " -L " + inputBuffer.getContig();
		}
		
		//And now index that one too
		String index3 = samtoolsPath + " index " + recalBamFile.getAbsolutePath();
		
		//Last but not least multigenotype the deduped-realigned-recaled bam file
		VCFFile outputVCF = new VCFFile(new File(projHome + "contig_" + contig + ".vcf"), contig);
		String callVariantsCommand = "java " + defaultMemOptions + " " + jvmARGStr + " -jar " + gatkPath;
		callVariantsCommand = callVariantsCommand + " -R " + reference.getAbsolutePath() + 
				" -I " + inputBuffer.getAbsolutePath() + 
				" -T UnifiedGenotyper" +
				" -o " + outputVCF.getAbsolutePath() + 
				" -glm BOTH" +
				" -stand_call_conf 30.0" +
				" -stand_emit_conf 10.0";
		if (contig != null) {
			callVariantsCommand = callVariantsCommand + " -L " + contig + " ";
		}
		
		addOutputFile(outputVCF);
		
		String[] command = new String[]{rmdupCommand,
										index1,
										createTargetsCommand,
										realignContigCommand,
										sortRealignedCommand,
										index2,
										countCovarCommand,
										applyRecalCommand,
										index3,
										callVariantsCommand
		};
		return command;
	}

}
