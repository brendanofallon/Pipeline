package operator.annovar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import operator.OperationFailedException;
import pipeline.Pipeline;
import buffer.variant.VariantRec;

/**
 * Adds gene / exon variant functions to variant records. This runs annovar -geneanno to generate the information,
 * then parses the resulting output text files to associate the gene/exon function with variants in the
 * variant pool 
 * @author brendan
 *
 */
public class GeneAnnotator extends AnnovarAnnotator {

	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);
		
		String command = "perl " + annovarPath + "annotate_variation.pl -geneanno --buildver " + buildVer + " " + annovarInputFile.getAbsolutePath() + " --outfile " + annovarPrefix + " " + annovarPath + "humandb/";
		executeCommand(command);
		
		String variantFuncFile =  annovarPrefix + ".variant_function";
		String exonFuncFile = annovarPrefix + ".exonic_variant_function";
		
		try {
			addAnnotations(variantFuncFile, exonFuncFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Error reading variant function files", this);
		}
		
		//Cleanup old annovar files
		File variantFunc = new File(variantFuncFile);
		variantFunc.deleteOnExit();
		File exonVariantFunc = new File(exonFuncFile);
		exonVariantFunc.deleteOnExit();
	}
	
	
	private void addAnnotations(String variantFilePath, String exonicFuncFilePath) throws IOException {
		//Add gene annotations
		int totalVars =0 ;
		int errorVars = 0;
		BufferedReader reader = new BufferedReader(new FileReader(variantFilePath));
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Reading annovar variant output from file " + variantFilePath);
		List<String> lastFewErrors = new ArrayList<String>(); //Stores info about variants not found in annotation file

		String line = reader.readLine();
		while (line != null) {
			String[] toks = line.split("\\t");
			String variantType = toks[0];
			String gene = toks[1];
			String contig = toks[2];
			String ref = toks[5];
			String alt = toks[6];
			
//			if (gene.length() > 8 || gene.contains(",") || gene.contains(";")) {
//				gene = "-";
//			}
			
			if (gene.contains(";")) {
				String before = gene;
				gene = gene.substring(0, gene.indexOf(";"));
				logger.info("Converting gene name : " + before + " to: " + gene);				
			}
			
			int pos = Integer.parseInt(toks[3]);
			
			VariantRec rec = findVariant(contig, pos, ref, alt); //variants.findRecord(contig, pos);

			if (rec == null) {
				errorVars++;
				if (lastFewErrors.size() < 10)
					lastFewErrors.add("Variant not found : " + line);
			}
			else {
				rec.addAnnotation(VariantRec.GENE_NAME, gene);
				rec.addAnnotation(VariantRec.VARIANT_TYPE, variantType);
			}
			totalVars++;
			line = reader.readLine();
		}
		
		if (totalVars > 200 && (errorVars > totalVars*0.01)) {
			for(String err : lastFewErrors) {
				System.err.println(err);
			}
			reader.close();
			throw new IOException("Too many variants not found in variant type file  "+ variantFilePath + ", errors: " + errorVars + " total variants: " + totalVars);
		}
		Logger.getLogger(Pipeline.primaryLoggerName).info(errorVars + " of " + totalVars + " could not be associated with a variant record");
		totalVars = 0;
		errorVars = 0;
		reader.close();
		
		//Add exonic variants functions to records where applicable
		reader = new BufferedReader(new FileReader(exonicFuncFilePath));
		logger.info("Reading annovar exonic variants from file " + exonicFuncFilePath);
		line = reader.readLine();
		lastFewErrors.clear();
		while(line != null) {
			if (line.length()>1) {
				String[] toks = line.split("\\t");
				String exonicFunc = toks[1];
				String ref = toks[6];
				String alt = toks[7];
				String NM = "NA";
				String exonNum = "NA";
				String cDot = "NA";
				String pDot = "NA";
	
				String[] details = toks[2].split(":");
				if (details.length>4) {
					NM = details[1];
					exonNum = details[2];
					cDot = details[3];
					pDot = details[4];
					int idx = pDot.indexOf(",");
					if (idx > 0)
						pDot = pDot.substring(0, idx);
				}
				
				String contig = toks[3];
				int pos = Integer.parseInt( toks[4] );
				
				totalVars++;
				VariantRec rec = findVariant(contig, pos, ref, alt);
				if (rec != null) {
					rec.addAnnotation(VariantRec.EXON_FUNCTION, exonicFunc);
					rec.addAnnotation(VariantRec.NM_NUMBER, NM);
					rec.addAnnotation(VariantRec.EXON_NUMBER, exonNum);
					rec.addAnnotation(VariantRec.CDOT, cDot);
					rec.addAnnotation(VariantRec.PDOT, pDot);
				}
				else {
					errorVars++;
					if (lastFewErrors.size() < 10)
						lastFewErrors.add("Variant not found : " + line);
				}
			}
			line = reader.readLine();
		}
		reader.close();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info(errorVars + " of " + totalVars + " could not be associated with a variant record");
		if (errorVars > totalVars*0.01) {
			for(String err : lastFewErrors) {
				System.err.println(err);
			}
			reader.close();
			throw new IOException("Too many variants not found in exonic func file " + exonicFuncFilePath + ", errors: " + errorVars + " total variants: " + totalVars);
		}
	}

	
}
