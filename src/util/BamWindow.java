package util;

import java.io.File;
import java.util.ArrayDeque;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

public class BamWindow {

	final File bamFile;
	final SAMFileReader samReader; 
	private SAMRecordIterator recordIt; //Iterator for traversing over SAMRecords
	private SAMRecord nextRecord; //The next record to be added to the window, may be null if there are no more
	
	private String currentContig = null;
	private int currentPos = -1;
	final ArrayDeque<SAMRecord> records = new ArrayDeque<SAMRecord>(1024);
	
	public BamWindow(File bamFile) {
		this.bamFile = bamFile;
		
		SAMFileReader.setDefaultValidationStringency(ValidationStringency.LENIENT);
		samReader = new SAMFileReader(bamFile);
		samReader.setValidationStringency(ValidationStringency.LENIENT);
		recordIt = samReader.iterator();
		nextRecord = recordIt.next();
	}
	
	public int getCurrentPosition() {
		return currentPos;
	}
	
	public int getCoverage() {
		//number of records on current position
		return records.size();
	}
	
	public double meanInsertSize() {
		double sum = 0;
		for(SAMRecord rec : records) {
			sum += Math.abs(rec.getInferredInsertSize());
		}
		return sum / (double)records.size();
	}
	
	
	
	public void advanceBy(int bases) {
		int newTarget = currentPos + bases;
		advanceTo(currentContig, newTarget);
	}
	
	public void advanceTo(String contig, int pos) {
		//Advance to wholly new site
		//Expand leading edge until the next record is beyond target pos
		currentPos = pos;
		
		shrinkTrailingEdge();
		while(nextRecord != null && nextRecord.getAlignmentStart() <= pos) {
			//System.out.println("Expanding, next record start is  " + nextRecord.getAlignmentStart() );
			step();
		}
	}
	
	public int getLeadingEdgePos() {
		return records.getFirst().getAlignmentEnd();
	}
	
	public int getTrailingEdgePos() {
		return records.getLast().getAlignmentStart();
	}
	
	public SAMRecord getTrailingRecord() {
		if (records.size()==0)
			return null;
		return records.getLast();
	}

	/**
	 * Identical to expand() followed by shrinkTrailingEdge()
	 */
	private void step() {
		expand();
		shrinkTrailingEdge();
	}
	
	/**
	 * Push new records onto the queue. Unmapped reads and reads with unmapped mates are skipped.
	 */
	private void expand() {
		if (nextRecord == null)
			return;
		
		//System.out.println("Pushing record starting at : " + nextRecord.getAlignmentStart());
		records.push(nextRecord);
		
		//Find next suitable record
		nextRecord = recordIt.next();
		//Automagically skip unmapped reads and reads with unmapped mates
		while(nextRecord != null && (nextRecord.getMappingQuality()==0 || nextRecord.getMateUnmappedFlag())) {
			//System.out.println("Skipping record with mapping quality: " + nextRecord.getMappingQuality() + " mate mapping quality: " + nextRecord.getMateUnmappedFlag());
			nextRecord = recordIt.next();
		}
	}

	
	/**
	 * Remove from queue those reads whose right edge is less than the current pos
	 */
	private void shrinkTrailingEdge() {
		SAMRecord trailingRecord = getTrailingRecord();
		
		while(trailingRecord != null && (trailingRecord.getAlignmentStart() + trailingRecord.getReadLength() < currentPos)) {
			SAMRecord removed = records.pollLast();
			//System.out.println("Trimming record starting at : " + removed.getAlignmentStart() + "-" + (removed.getAlignmentStart()+removed.getReadLength()));
			trailingRecord = getTrailingRecord();
		}
	}
	
}
