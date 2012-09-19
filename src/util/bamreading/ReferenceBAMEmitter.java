package util.bamreading;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import util.FastaReader;

public class ReferenceBAMEmitter {

	final FastaReader refReader;
	final AlignmentColumn alnCol;
	
	List<ColumnComputer> counters;
	
	public ReferenceBAMEmitter(File reference, File bamFile, List<ColumnComputer> counters) throws IOException {
		refReader = new FastaReader(reference);
		alnCol = new AlignmentColumn(bamFile);
		this.counters = counters;
	}
	
	public void emitLine(PrintStream out) {
		out.print(refReader.getCurrentBase() + " : " + alnCol.getBasesAsString());
		for(ColumnComputer counter : counters) {
			Double[] values = counter.computeValue(alnCol);
			for(int i=0; i<values.length; i++) {
				out.print("\t" + values[i] );
			}
		}
		out.println();
	}
	
	public void emitWindow(String contig, int start, int end) throws IOException {
		refReader.advanceToTrack(contig);
		refReader.advance(start);
		alnCol.advanceTo(contig, start+1);
		
		int curPos = start;
		while(curPos < end) {
			
			if (alnCol.getDepth() > 2 && alnCol.hasDifferingBase(refReader.getCurrentBase()))
				emitLine(System.out);
			
			refReader.advance(1);
			alnCol.advance(1);
			curPos++;
			
			//Sanity check
			if (refReader.getCurrentPos() != curPos) {
				System.err.println("Yikes, reference reader position is not equal to current position");
			}
			if (alnCol.getCurrentPosition() != (curPos+1)) {
				System.err.println("Yikes, bam reader position is not equal to current position");
			}
		}
		
	}
	
	public static void main(String[] args) throws IOException {
		
		File reference = new File("/home/brendan/resources/human_g1k_v37.fasta");
		File bam = new File("/home/brendan/bamreading/medtest.chr10.bam");
		List<ColumnComputer> counters = new ArrayList<ColumnComputer>();
		counters.add( new DepthComputer());
		counters.add( new QualSumComputer());
		counters.add( new PosDevComputer());
		
		
		ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(reference, bam, counters);
		
		
		emitter.emitWindow("10", 1000000, 20000000);
		
	}
}
