package rankingService;

import java.io.File;
import java.io.IOException;

import buffer.variant.CSVLineReader;
import buffer.variant.VariantLineReader;
import buffer.variant.VariantRec;

public class RankingResultsReader extends CSVLineReader {


	public RankingResultsReader(File csvFile) throws IOException {
		super(csvFile);
	}

	@Override
	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		VariantRec rec = null;
		
		if (toks.length < 8) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);
			return null;
		}
		
		try {
			

		}
		catch (NumberFormatException nfe) {
			System.err.println("ERROR: could not parse variant from file : " + sourceFile.getName() + " line : \n " + currentLine);

		}
		return rec;
	}

}
