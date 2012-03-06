package operator.annovar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import buffer.variant.FileAnnotator;
import buffer.variant.VariantRec;
import operator.OperationFailedException;
import pipeline.Pipeline;

/**
 * Adds gene / exon variant functions to variant records
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
		
	}
	
	
	private void addAnnotations(String variantFilePath, String exonicFuncFilePath) throws IOException {
		//Add gene annotations 
		BufferedReader reader = new BufferedReader(new FileReader(variantFilePath));
		String line = reader.readLine();
		while (line != null) {
			String[] toks = line.split("\\t");
			String variantType = toks[0];
			String gene = toks[1];
			String contig = toks[2];
			
			int pos = Integer.parseInt(toks[3]);
			
			//Unfortunately, annovar likes to strip the leading base from deletion sequences, converting
			//a record that looks like pos: 5 ref: CCT alt: C 
			//to something like :      pos: 6 ref: CT alt: -
			//so we won't be able to find the record since its position will have changed. 
			//Below is a kludgy workaround that subtracts one from the position if a deletion is detected

			//Don't use anymore... always remove identical leading bases on indels before making a variantrec
//			if (toks[6].trim().equals("-")) {
//				pos--;
//			}

			//System.out.println("Modified pos is: " + pos);
			
			VariantRec rec = variants.findRecord(contig, pos);
			if (rec == null) {
				System.out.println("record not found, modified position is:" + pos + " tokens are : ");
				for(int i=0; i<toks.length; i++) {
					System.out.println(i + "\t |" + toks[i] + "|");
				}
				
				throw new IOException("Could not find variant record at contig: " + contig + " and pos: " + pos);
			}
			rec.addAnnotation(VariantRec.GENE_NAME, gene);
			rec.addAnnotation(VariantRec.VARIANT_TYPE, variantType);
			line = reader.readLine();
		}
		
		//Add exonic variants functions to records where applicable 
		reader = new BufferedReader(new FileReader(exonicFuncFilePath));
		line = reader.readLine();
		while(line != null) {
			if (line.length()>1) {
				String[] toks = line.split("\\t");
				String exonicFunc = toks[1];
				
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
				
				//Same workaround as above...
				//Don't use anymore... always remove identical leading bases on indels before making a variantrec
//				if (toks[7].trim().equals("-")) {
//					pos--;
//				}
				
				VariantRec rec = variants.findRecord(contig, pos);
				if (rec != null)
					rec.addAnnotation(VariantRec.EXON_FUNCTION, exonicFunc);
					rec.addAnnotation(VariantRec.NM_NUMBER, NM);
					rec.addAnnotation(VariantRec.EXON_NUMBER, exonNum);
					rec.addAnnotation(VariantRec.CDOT, cDot);
					rec.addAnnotation(VariantRec.PDOT, pDot);
			}
			line = reader.readLine();
		}
		reader.close();
	}
	
}
