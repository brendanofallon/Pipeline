package rankingService;

import java.io.File;
import java.io.IOException;

import buffer.variant.CSVLineReader;
import buffer.variant.VariantRec;

/**
 * Special variant reader for files written by the "ResultsWriter", which are not normally
 * parseable by CSVLineReaders
 * @author brendan
 *
 */
public class ResultsReader extends CSVLineReader {

	public static final String header = "Gene	cDot	pDot	disease.potential	gene.relevance	overall.score	rsNumber	population.frequency	top.pubmed.hit	goterm.hits	interaction.score	summary.score";
	
	public ResultsReader(File csvFile) throws IOException {
		super(csvFile);
		// TODO Auto-generated constructor stub
	}

	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		VariantRec rec = null;
		
		if (toks.length != 12) {
			System.err.println("ResultsReader: Incorrect number of tokens on line : " + currentLine);
			return null;
		}
		
		try {
			String contig = getContig(toks);
			Integer start = getStart(toks);
			Integer end = getEnd(toks);
			String ref = getRef(toks);
			String alt = getAlt(toks);
			Double qual = getQuality(toks);
			Double depth = getDepth(toks); 
			boolean isHet = getHet(toks);
			Double genoQual = getGenotypeQuality(toks);


			if (alt.length() != ref.length()) {
				//Remove initial characters if they are equal and add one to start position
				if (alt.charAt(0) == ref.charAt(0)) {
					alt = alt.substring(1);
					ref = ref.substring(1);
					if (alt.length()==0)
						alt = "-";
					if (ref.length()==0)
						ref = "-";
					start++;
				}

				if (ref.equals("-"))
					end = start;
				else
					end = start + ref.length();
			}

			rec = new VariantRec(contig, start, start+ref.length(), ref, alt, qual, isHet);
			rec.addProperty(VariantRec.DEPTH, depth);
			rec.addProperty(VariantRec.GENOTYPE_QUALITY, genoQual);

			//Parse additional annotations / properties from header
			if (hasHeader() && toks.length > 8) {
				if (toks.length != headerToks.length) {
					for(int i=0; i<toks.length; i++) {
						System.out.println(i + "\t" + headerToks[i] + " : " + toks[i]);
					}
					throw new IllegalArgumentException("Incorrect number of columns for variant, header shows " + headerToks.length + ", but this variant has: " + toks.length + "\n" + currentLine);

				}
				for(int i=9; i<toks.length; i++) {
					String key = headerToks[i].trim();
					//System.out.println("Adding annotation for key: " + key + " value:" + toks[i]);
					if (toks[i].equals("-") || toks[i].equals("NA") || toks[i].equals("?"))
						continue; 

					try {
						Double val = Double.parseDouble(toks[i]);
						rec.addProperty(key, val);
					}
					catch (NumberFormatException ex) {
						//this is expected, we just assume it's an annotation, not a property
					}

					rec.addAnnotation(key, toks[i].trim());
				}
			}

			if (sourceFile != null)
				rec.addAnnotation(VariantRec.SOURCE, sourceFile.getName());

		}
		catch (NumberFormatException nfe) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);

		}
		return rec;
	}
}
