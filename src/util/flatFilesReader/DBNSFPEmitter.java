package util.flatFilesReader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import buffer.variant.VariantRec;

/**
 * Small utility to write some values from dbNSFP over a region
 * @author brendan
 *
 */
public class DBNSFPEmitter {

	public static void emitRegion(DBNSFPReader reader, PrintWriter writer, String contig, int startPos, int endPos) throws IOException {
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

				String popStr = reader.getString(DBNSFPReader.TKG_AMR);
				if (! popStr.equals(".")) {
					Double freq = Double.parseDouble(popStr);
					VariantRec var = new VariantRec("" + contig, curPos, curPos+1, reader.getRef(), reader.getAlt() );
					var.addProperty(VariantRec.POP_FREQUENCY, freq);
					writer.println(var.toSimpleString() + "\t" + freq);
					
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
		
		PrintWriter writer = new PrintWriter(new FileWriter("/home/brendan/tgk-vars.amr.csv"));
		
		writer.println(VariantRec.getSimpleHeader() + "\tpop.freq");
		String infilePath = "/home/brendan/MORE_DATA/superpanel/vascmalprobes.csv";
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
			Integer startPos = Integer.parseInt(toks[1]);
			//Integer endPos = Integer.parseInt(toks[2]);
			//System.out.println("Reading region chr" + contig + ":" + startPos );
			emitCloseGene(reader, writer, "" + contig, startPos);
			line = fileReader.readLine();
		}
		writer.flush();
		fileReader.close();
		
		
	}
	
	public static final char[] bases = new char[]{'A', 'C', 'G', 'T'};
}

