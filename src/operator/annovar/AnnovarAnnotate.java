package operator.annovar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import buffer.AnnovarInputFile;
import buffer.AnnovarResults;
import buffer.FileBuffer;
import buffer.VCFFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantFilter;
import buffer.variant.VariantRec;
import operator.CommandOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineXMLConstants;

/**
 * Use annovar to produce variant annotation files. 
 * Right now this assumes that in the annovar directory there is a database directory called "humandb"
 * that contains all required db info 
 * @author brendan
 *
 */
public class AnnovarAnnotate extends CommandOperator {

	
	public static final String BUILD_VER = "buildver";
	protected String annovarPath = "~/annovar/";
	protected String buildVer = "hg19";
	public static final String FORMAT = "format";
	protected String format = "vcf4"; //Default assumed input format
	protected double siftThreshold = 0.0; // by default emit all variants 
	private String annovarPrefix = "annovar.output";
	//protected double freqFilter = 0.0; //Frequency filtering level, all variants with frequency higher than this level in 1000g will be moved to the dropped file
	
	@Override
	protected String[] getCommands() {
		Object path = Pipeline.getPropertyStatic(PipelineXMLConstants.ANNOVAR_PATH);
		if (path != null)
			annovarPath = path.toString();
		
		//User can override path specified in properties
		String userBuildVer = properties.get(BUILD_VER);
		if (userBuildVer != null) {
			buildVer = userBuildVer;
		}
				
		//User can override path specified in properties
		String userPath = properties.get(PipelineXMLConstants.PATH);
		if (userPath != null) {
			annovarPath = userPath;
		}
		
		
		annovarPrefix = Pipeline.getPipelineInstance().getProjectHome() + annovarPrefix;
		
		//First step is to convert input VCF to annovar format
		String formatStr = properties.get(FORMAT);
		if (formatStr != null)
			format = formatStr;
		
		VCFFile inputVCF = (VCFFile) getInputBufferForClass(VCFFile.class);
		String inputPath = inputVCF.getAbsolutePath();
		AnnovarInputFile annovarInput = new AnnovarInputFile(new File(Pipeline.getPipelineInstance().getProjectHome() + "annovar.input"));
		
		
		String command1 = "perl " + annovarPath + "convert2annovar.pl -format " + format + " " + inputPath + " --outfile " + annovarInput.getAbsolutePath(); 
		
		String annovarInputPath = annovarInput.getAbsolutePath();
		
		//Filter all variants by 1000g, setting maf to 0 means that all variants will be put into dropped file 
		String command15 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype 1000g2010nov_all -maf 0 --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		String command2 = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//SIFT stuff
		String command3 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype avsift --buildver " + buildVer + " --sift_threshold " + siftThreshold + "  " + annovarInputPath + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		
		//Polyphen 
		String command4 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_pp2 --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";

		//GERP
		//Annovar currently broken with gerp
		//String command5 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_gerp++ --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + outputPath + " " + annovarPath + "humandb/";
		//File gerpResults = new File(outputPath + "hg19_ljb_gerp++_filtered"); 
		
		//MutationTaster
		String command6 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_mt --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";

		//PhyloP
		String command7 = "perl " + annovarPath + "annotate_variation.pl -filter -dbtype ljb_phylop --buildver " + buildVer + "  " + annovarInputPath + " --outfile " + annovarPrefix + " -score_threshold 0.0 " + annovarPath + "humandb/";
		
		return new String[]{command1, command15, command2, command3, command4, /*command5, */ command6, command7};
	}
	
	public void performOperation() throws OperationFailedException {
		super.performOperation();
		 
		File varFunc = new File(annovarPrefix + ".variant_function"); 

		File exvarFunc = new File(annovarPrefix + ".exonic_variant_function"); 
		File siftFile = new File(annovarPrefix + ".hg19_avsift_dropped"); 
		File polyphenFile = new File(annovarPrefix + ".hg19_ljb_pp2_dropped"); 
		File mtFile = new File(annovarPrefix + ".hg19_ljb_mt_dropped"); 
		File TKGFile = new File(annovarPrefix + ".hg19_ALL.sites.2010_11_dropped");
		File phlyoPFile = new File(annovarPrefix + ".hg19_ljb_phylop_dropped");
		try {
			AnnovarResults variants = new AnnovarResults(varFunc, exvarFunc, siftFile, polyphenFile, mtFile, TKGFile, phlyoPFile);
			
			FileBuffer resultsFile = outputBuffers.get(0);
			variants.addQuartileInfo();
			
			List<VariantRec> lowFreqVars = variants.filterPool(new VariantFilter() {

				@Override
				public boolean passes(VariantRec rec) {
					Double freq = rec.getProperty(VariantRec.POP_FREQUENCY); 
					if (freq == null || freq < 0.02)
						return true;
					else
						return false;
				}
				
			});
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(Pipeline.getPipelineInstance().getProjectHome() + "low.freq.variants.csv"));
			writer.write(VariantRec.getColumnHeaders() + "\n");
			for(VariantRec rec : lowFreqVars) {
				writer.write(rec + "\n");
			}
			writer.close();
			
			VariantFilter topQuartile = new HighQuartilesFilter();
			VariantFilter lowQuartile = new LowQuartilesFilter();
			VariantFilter nonBadFilter = new NonBadFilter();
			VariantFilter badFilter = new BadFilter();
			
			List<VariantRec> topHits = AbstractVariantPool.filterList(topQuartile, lowFreqVars);
			List<VariantRec> bottomHits = AbstractVariantPool.filterList(lowQuartile, lowFreqVars);
			List<VariantRec> nonBadVariants = AbstractVariantPool.filterList(nonBadFilter, lowFreqVars);
			List<VariantRec> badVariants = AbstractVariantPool.filterList(badFilter, lowFreqVars);
			
			
			writer = new BufferedWriter(new FileWriter(Pipeline.getPipelineInstance().getProjectHome() + "top.quartile.variants.csv"));
			writer.write(VariantRec.getColumnHeaders() + "\n");
			for(VariantRec rec : topHits) {
				writer.write(rec + "\n");
			}
			writer.close();
			
			
			writer = new BufferedWriter(new FileWriter(Pipeline.getPipelineInstance().getProjectHome() + "bottom.quartile.variants.csv"));
			writer.write(VariantRec.getColumnHeaders() + "\n");
			for(VariantRec rec : bottomHits) {
				writer.write(rec + "\n");
			}
			writer.close();
			
			writer = new BufferedWriter(new FileWriter(Pipeline.getPipelineInstance().getProjectHome() + "tolerated.variants.csv"));
			writer.write(VariantRec.getColumnHeaders() + "\n");
			for(VariantRec rec : nonBadVariants) {
				writer.write(rec + "\n");
			}
			writer.close();
			
			writer = new BufferedWriter(new FileWriter(Pipeline.getPipelineInstance().getProjectHome() + "not.tolerated.variants.csv"));
			writer.write(VariantRec.getColumnHeaders() + "\n");
			for(VariantRec rec : badVariants) {
				writer.write(rec + "\n");
			}
			writer.close();
	
			variants.emitNonsynonymousVars(new PrintStream(new FileOutputStream(resultsFile.getAbsolutePath())) );
			
			
		} catch (IOException e) {
			throw new OperationFailedException("Error opening annovar results files : " + e.getMessage(), this);
		}


	}


	@Override
	protected String getCommand() throws OperationFailedException {
		//Nothing to do since we've overridden getCommands()
		return null;
	}

	/**
	 * A filter that passes variants that are in the top quartile for sift, polyphen, and mutation taster scores
	 * @author brendan
	 *
	 */
	class HighQuartilesFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_QUARTILE) && rec.hasProperty(VariantRec.POLYPHEN_QUARTILE) && rec.hasProperty(VariantRec.MT_QUARTILE)) {
				Double siftQ = rec.getProperty(VariantRec.SIFT_QUARTILE);
				Double ppQ = rec.getProperty(VariantRec.POLYPHEN_QUARTILE);
				Double mtQ = rec.getProperty(VariantRec.MT_QUARTILE);
				if (siftQ == 0 && ppQ == 0 && mtQ == 0) {
					return true;
				}
			}
			return false;
		}
	}
		
	
	/**
	 * A filter that passes variants that are in the bottom quartile for sift, polyphen, and mutation taster scores
	 * @author brendan
	 *
	 */
	class LowQuartilesFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_QUARTILE) && rec.hasProperty(VariantRec.POLYPHEN_QUARTILE) && rec.hasProperty(VariantRec.MT_QUARTILE)) {
				Double siftQ = rec.getProperty(VariantRec.SIFT_QUARTILE);
				Double ppQ = rec.getProperty(VariantRec.POLYPHEN_QUARTILE);
				Double mtQ = rec.getProperty(VariantRec.MT_QUARTILE);
				if (siftQ > 1 && ppQ > 1 && mtQ > 1) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * A filter that passes all variants that are not 'bad', that is those that have high sift scores and low polyphen and mt scores
	 * @author brendan
	 *
	 */
	class NonBadFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_SCORE) && rec.hasProperty(VariantRec.POLYPHEN_SCORE) && rec.hasProperty(VariantRec.MT_SCORE)) {
				Double siftScore = rec.getProperty(VariantRec.SIFT_SCORE);
				Double ppScore = rec.getProperty(VariantRec.POLYPHEN_SCORE);
				Double mtScore = rec.getProperty(VariantRec.MT_SCORE);
				//Polyphen score is the posterior probability that the variant IS DAMAGING, so small
				//scores indicate low probability that the variant is damaging. 
				
				if (siftScore > 0.05 && ppScore < 0.20 && mtScore < 0.20) {
					return true;
				}
			}
			return false;
		}
	}
	
	/**
	 * This filter passes all variants where sift, polyphen, and mt all seem to agree on deleteriousness.
	 * These are the most likely to be disease causing
	 * @author brendan
	 *
	 */
	class BadFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			if (rec.hasProperty(VariantRec.SIFT_SCORE) && rec.hasProperty(VariantRec.POLYPHEN_SCORE) && rec.hasProperty(VariantRec.MT_SCORE)) {
				Double siftScore = rec.getProperty(VariantRec.SIFT_SCORE);
				Double ppScore = rec.getProperty(VariantRec.POLYPHEN_SCORE);
				Double mtScore = rec.getProperty(VariantRec.MT_SCORE);
				//Polyphen score is the posterior probability that the variant IS DAMAGING, so small
				//scores indicate low probability that the variant is damaging. 
				
				if (siftScore <= 0.05 && ppScore > 0.80 && mtScore > 0.80) {
					return true;
				}
			}
			return false;
		}
	}
}

//	out.println("contig \t start \t end \t variant.type \t exon.func \t pop.freq \t het \t qual \t sift \t polyphen \t mt");  

