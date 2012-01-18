package util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;


/**
 * A class to extract a fasta sequence from a vcf file
 * @author brendan
 *
 */
public class VcfToFasta {


	public static void main(String[] args) {
		
		args = new String[]{"/home/brendan/resources/human_g1k_v37.fasta", "test.vcf" };
		
		
		if (args.length != 2) {
			System.out.println("Usage : java -jar vcfToFasta.jar reference.fasta input.vcf");
			return;
		}
		
		try {
			FastaReader ref = new FastaReader(new File(args[0]));
			VCFLineParser vcf = new VCFLineParser(new File(args[1]));
			
			PrintWriter writer = new PrintWriter( System.out );
			
			//Loop counts over variants in vcf file
			for(int i=0; i<500; i++) {

				int nextVarPos = vcf.getPosition();
				String contig = vcf.getContig();
				Integer track = null;
				if (contig.contains("X"))
					track = 23;
				if (contig.contains("Y")) 
					track = 24;
				if (track == null)
					track = Integer.parseInt( contig.replace(">", "").replace("chr", ""));

				ref.advanceToTrack( track );
				ref.emitBasesTo(nextVarPos-1, writer);
				String alt = vcf.getAlt(); //Alternative sequence at this position
				String refSeq = vcf.getRef();

				int refLength = refSeq.length();

				writer.write((1+ref.getCurrentPos()) + "\t" + alt + "\n");
				if (refLength == 1) {
					char refChar = ref.getCurrentBase();
					char vcfRef = refSeq.charAt(0);
					if (refChar != vcfRef) {
						writer.close(); //flushes the stream
						System.err.println("Ref bases do not match! Reference file: " + refChar + " vcf ref: " + vcfRef);
						return;
					}
					//DON'T advance here - we already advanced reference by one when we called ref.nextPos
				}
				else {
					ref.advance(refLength);	
				}

				vcf.advanceLine();
			}
			writer.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
