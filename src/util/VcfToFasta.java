package util;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;


/**
 * A class to extract a fasta sequence from a vcf file. This has a main method that can be used to build 
 * an executable that coverts vcf + reference into fasta.
 * @author brendan
 *
 */
public class VcfToFasta {

	private File referenceFile;
	private File vcfFile;
	private GapMap gapMap = new GapMap();
	List<String> sampleNames = new ArrayList<String>();
	List<StringBuilder> sequences = new ArrayList<StringBuilder>();
	
	public VcfToFasta(File referenceFile, File vcfFile)  {
		this.referenceFile = referenceFile;
		this.vcfFile = vcfFile;
	}
	
	/**
	 * Convert the given contig string into a track number
	 * @param contig
	 * @return
	 */
	public static int contigToTrack(String contig) {
		Integer track = null;
		if (contig.contains("X"))
			track = 23;
		if (contig.contains("Y")) 
			track = 24;
		if (track == null)
			track = Integer.parseInt( contig.replace(">", "").replace("chr", ""));
		return track;
	}
	
	/**
	 * One sequences have been read in, they can be written to various output sources using
	 * this method
	 * @param writer
	 * @throws IOException
	 */
	public void emitSequences(PrintWriter writer) throws IOException {
		for(int i=0; i<sampleNames.size(); i++) {
			String sample = sampleNames.get(i);
			int chr1Index = i*2;
			int chr2Index = i*2+1;
			flushToOutput(sample, sequences.get(chr1Index), sequences.get(chr2Index), writer);
		}
	}
	
	public void readSample(String contig, int startPos, int endPos, String sample) throws IOException {
		FastaReader ref = new FastaReader(referenceFile);
		VCFLineParser vcf;
		if (sample != null)
			vcf = new VCFLineParser(vcfFile, sample);
		else {
			vcf = new VCFLineParser(vcfFile);
		}
		
		//If sample not explicitly set by user, we use whatever the first sample is in the vcf
		if (sample == null) {
			sample = vcf.getSampleName();
		}
		
		//WrappingWriter writer = new WrappingWriter( stream );
	
		
		StringBuilder refWriter = new StringBuilder(endPos-startPos);
		StringBuilder chr1Fasta = new StringBuilder(endPos - startPos);
		StringBuilder chr2Fasta = new StringBuilder(endPos - startPos);
		StringWriter chr1Writer = new StringWriter(chr1Fasta);
		StringWriter chr2Writer = new StringWriter(chr2Fasta);
		
		//Advance vcf to proper position...
		vcf.advanceTo(contig, startPos);
	
		
		Integer track = contigToTrack(contig);
		ref.advanceToTrack( track );
		ref.advance(startPos-1);
		
		//No variants at all, so just emit reference sequence and return
		if (vcf.getPosition() > endPos) {
			ref.emitBasesTo(endPos, chr1Writer, chr2Writer);
			//flushToOutput(sample, chr1Fasta, chr2Fasta, writer);
			return;
		}
		
		//Loop counts over variants in vcf file
		while( vcf.hasLine() && vcf.getContig().equals(contig) && vcf.getPosition() < endPos ) {

			int nextVarPos = vcf.getPosition();

			ref.advanceToTrack( track );
			ref.emitBasesTo(nextVarPos-1, chr1Writer, chr2Writer);
			String refSeq = vcf.getRef();

			int refLength = refSeq.length();
			
			//Find actual base to be written from vcf, it may not be variable for 
			//this sample / pos, in which case we write the reference
			String chr1Alt;
			String chr2Alt;
			if (vcf.firstIsAlt())
				chr1Alt = vcf.getAlt();
			else
				chr1Alt = ref.getCurrentBase() + "";
			
			if (vcf.secondIsAlt())
				chr2Alt = vcf.getAlt();
			else
				chr2Alt = ref.getCurrentBase() + "";			

			//Convert .'s to reference
			if (chr1Alt.equals(".")) {
				chr1Alt = refSeq;
			}
			if (chr2Alt.equals(".")) {
				chr2Alt = refSeq;
			}
			
			

			chr1Writer.write( chr1Alt );
			for(int i=0; i<(refLength-chr1Alt.length()); i++)
				chr1Writer.write( "-" );
			
			chr2Writer.write( chr2Alt );
			for(int i=0; i<(refLength-chr2Alt.length()); i++)
				chr2Writer.write( "-" );
			

			//A bit of error checking
			if (refLength == 1) {
				char refChar = ref.getCurrentBase();
				char vcfRef = refSeq.charAt(0);
				//System.out.println("Assessing reference... ref:" + refChar + " vcf ref: " + vcfRef");
				if (refChar != vcfRef) {
//					flushToOutput(sample, chr1Fasta, chr2Fasta, writer);
//					writer.close(); //flushes the stream
					System.err.println("Ref bases do not match! Position: " + vcf.getPosition() + " Reference file: " + refChar + " vcf ref: " + vcfRef);
					return;
				}
			}

			ref.advance(refLength);	
			int pos = vcf.getPosition(); {
				vcf.advanceLine();
				int newPos = vcf.getPosition();
				while(newPos == pos) {
					vcf.advanceLine();
					newPos = vcf.getPosition();
				}
			}
		}

		ref.emitBasesTo(endPos, chr1Writer, chr2Writer);
		sampleNames.add(sample);
		sequences.add(chr1Fasta);
		sequences.add(chr2Fasta);
		//flushToOutput(sample, chr1Fasta, chr2Fasta, writer);
	}
	
	
	private void flushToOutput(String sampleLabel, StringBuilder chr1Fasta,
			StringBuilder chr2Fasta, Writer writer) throws IOException {
			
		writer.write(">" + sampleLabel + "-chr1\n");
		writer.write(chr1Fasta.toString() + "\n");
		writer.write(">" + sampleLabel + "-chr2\n");
		writer.write(chr2Fasta.toString() + "\n");
		writer.flush();
		
		
	}

	public static void main(String[] args) {
		
		//args = new String[]{"/home/brendan/resources/human_g1k_v37.fasta", "/home/brendan/1000g.1:1-100000.vcf", "NA18516" };
		
		args = new String[]{"/home/brendan/validation/tiny.fa", "/home/brendan/validation/test.vcf", "1", "1", "79"};
		if (args.length < 5) {
			System.out.println("Usage : java -jar vcfToFasta.jar reference.fasta input.vcf contig start end [sample name] [another sample name ....]");
			return;
		}
		
		
		File referenceFile = new File(args[0]);
		File vcfFile = new File(args[1]);
		String contig = args[2];
		String startStr = args[3];
		String endStr = args[4];
		
		Integer startPos = Integer.parseInt(startStr);
		Integer endPos = Integer.parseInt(endStr);
		
		
		if (args.length==5) {
			//No sample was specified in arg list, so use null for sample name, this will default to first sample in vcf
			VcfToFasta fasWriter = new VcfToFasta(referenceFile, vcfFile);
			try {
				fasWriter.readSample(contig, startPos, endPos, null);
				fasWriter.emitSequences( new PrintWriter(System.out));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		else {
			//One or more samples specified in arg list, so loop through them and emit for all...
			VcfToFasta fasWriter = new VcfToFasta(referenceFile, vcfFile);
			try {
				for(int i=5; i<args.length; i++) {
					fasWriter.readSample(contig, startPos, endPos, args[i]);
				}
				fasWriter.emitSequences(new PrintWriter(System.out));
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		
		System.out.flush();
	}
}
