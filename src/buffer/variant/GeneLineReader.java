package buffer.variant;

import java.io.File;
import java.io.IOException;

public class GeneLineReader extends CSVLineReader {

	
	public GeneLineReader(File csvFile) throws IOException {
		super(csvFile);
	}

	public VariantRec toVariantRec() {
		if (currentLine == null)
			return null;
		
		String[] toks = currentLine.split("\t");
		
		String contig = getContig(toks);
		Integer start = getStart(toks);
		Integer end = getEnd(toks);
		Double length = Double.parseDouble(toks[3]);
		Double tmrca = Double.parseDouble(toks[4]);
		String geneName = toks[5];
		
		VariantRec rec = new VariantRec(contig, start, start+1, "-", "-", tmrca, true);
		rec.addAnnotation(VariantRec.GENE_NAME, geneName);
		rec.addProperty(VariantRec.DEPTH, length);
		
		
		
		return rec;
	}
}
