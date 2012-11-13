package util.flatFilesReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;

import buffer.variant.VariantRec;

/**
 * Small utility to write some values from dbNSFP over a region
 * @author brendan
 *
 */
public class DBNSFPEmitter {

	public static void emitRegion(DBNSFPReader reader, PrintStream writer, String contig, int startPos, int endPos) throws IOException {
		reader.advanceTo("" + contig, startPos);
		int curPos = reader.getCurrentPos();
		while(curPos < endPos) {
			for(int i = 0; i<bases.length; i++) {	
				String ref = reader.getRef();
				if (ref == null || ref.charAt(0) == bases[i]) {
					boolean hasNext = reader.advanceLine();
					if ((!hasNext) || curPos != reader.getCurrentPos()) {
						break;
					}
					continue;
				}

				String gene = reader.getString(DBNSFPReader.GENE);
				String tkgStr = reader.getString(DBNSFPReader.TKG);
				String popStr = reader.getString(DBNSFPReader.TKG_AMR);
				String mtStr = reader.getString(DBNSFPReader.MT);
				String ppStr = reader.getString(DBNSFPReader.PP);
				String siftStr = reader.getString(DBNSFPReader.SIFT);
				String gerpStr = reader.getString(DBNSFPReader.GERP);
				String phylopStr = reader.getString(DBNSFPReader.PHYLOP);
				if (! tkgStr.equals(".")) {
//					Double freq = Double.parseDouble(popStr);
					VariantRec var = new VariantRec("" + contig, curPos, curPos+1, reader.getRef(), reader.getAlt() );
//					var.addProperty(VariantRec.POP_FREQUENCY, freq);
					Double mtVal = Double.NaN;
					if (mtStr.length()>2)
						mtVal = Double.parseDouble(mtStr);
					if (tkgStr.equals(".")) {
						tkgStr = "0.0";
					}
					if (popStr.equals(".")) {
						popStr = "0.0";
					}
					writer.println(var.toSimpleString() + "\t" + gene + "\t" + tkgStr + "\t" + popStr + "\t" + mtVal + "\t" + siftStr + "\t" + ppStr + "\t" + gerpStr + "\t" + phylopStr);
					
					i = bases.length; //If we've found one for this position, skip all additional alts at this site
				}
				
				boolean hasNext = reader.advanceLine();
				if ((!hasNext) || curPos != reader.getCurrentPos()) {
					break;
				}
			}
			curPos = reader.getCurrentPos();
			if (curPos == -1) 
				break;
		}
	}
	
	public static void emitCloseGene(DBNSFPReader reader, PrintWriter writer, String contig, int startPos) throws IOException {
		
		for(int i=startPos+10; i<startPos+200; i++) {
				reader.advanceTo("" + contig, i);
				String gene = reader.getString(DBNSFPReader.GENE);
				System.out.println(contig + "\t" + startPos + "\t" + gene);
				return;	
		}
	}
	
	public static void main(String[] args) throws IOException {
		DBNSFPReader reader = new DBNSFPReader();
		
		//PrintWriter writer = new PrintWriter(new FileWriter("/home/brendan/tkgdata.csv"));
		PrintStream out = System.out;
		out.println(VariantRec.getSimpleHeader() + "gene\tkg.freq\tamr.freq\tmt.score\tsift.score\tpp.score\tgerp.score\tphylop.score");
		String infilePath = args[0];
		File inFile = new File(infilePath);
		BufferedReader fileReader = new BufferedReader(new FileReader(inFile));
		String line = fileReader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = fileReader.readLine();
				continue;
			}
			String[] toks = line.split("\t");
			String contig = toks[0];
			Integer startPos = Integer.parseInt(toks[1].replace("chr", ""));
			Integer endPos = Integer.parseInt(toks[2]);
			//System.out.println("Reading region chr" + contig + ":" + startPos );
			
			emitRegion(reader, out, contig, startPos, endPos);
			//emitCloseGene(reader, writer, "" + contig, startPos);
			line = fileReader.readLine();
		}
		out.flush();
		fileReader.close();
		
		
	}
	
	public static final char[] bases = new char[]{'A', 'C', 'G', 'T'};
}


