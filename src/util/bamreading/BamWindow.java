package util.bamreading;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Iterator;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileReader.ValidationStringency;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMRecordIterator;

/**
 * Not really a window, a collections of SAMRecords that covers a particular spot
 * and which can be moved in one direction
 * @author brendan
 *
 */
public class BamWindow {

	final File bamFile;
	final SAMFileReader samReader; 
	private SAMRecordIterator recordIt; //Iterator for traversing over SAMRecords
	private SAMRecord nextRecord; //The next record to be added to the window, may be null if there are no more
	
	private String currentContig = null;
	private int currentPos = -1; //In reference coordinates
	final ArrayDeque<MappedRead> records = new ArrayDeque<MappedRead>(1024);
	
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
	
	/**
	 * Return total number of reads at the current position
	 * @return
	 */
	public int size() {
		//number of records on current position
		return records.size();
	}
	
	/**
	 * Return the mean inferred insertion size of all records in this window
	 * @return
	 */
	public double meanInsertSize() {
		double sum = 0;
		for(MappedRead rec : records) {
			sum += Math.abs(rec.getRecord().getInferredInsertSize());
		}
		return sum / (double)records.size();
	}
	
	/**
	 * Obtain an interator for the SAMRecords at the current position
	 * @return
	 */
	public Iterator<MappedRead> getIterator() {
		return records.iterator();
	}
	
	/**
	 * Advance the current position by the given number of bases
	 * @param bases
	 */
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
			expand();
			shrinkTrailingEdge();
		}
	}
	
	public int getLeadingEdgePos() {
		return records.getFirst().getRecord().getAlignmentEnd();
	}
	
	public int getTrailingEdgePos() {
		return records.getLast().getRecord().getAlignmentStart();
	}
	
	public MappedRead getTrailingRecord() {
		if (records.size()==0)
			return null;
		return records.getLast();
	}
	
	/**
	 * Push new records onto the queue. Unmapped reads and reads with unmapped mates are skipped.
	 */
	private void expand() {
		if (nextRecord == null)
			return;
		
		//System.out.println("Pushing record starting at : " + nextRecord.getAlignmentStart());
		records.push(new MappedRead(nextRecord));
		
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
		MappedRead trailingRecord = getTrailingRecord();
		
		while(trailingRecord != null && 
				(trailingRecord.getRecord().getAlignmentStart() + trailingRecord.getRecord().getReadLength() < currentPos)) {
			MappedRead removed = records.pollLast();
			//System.out.println("Trimming record starting at : " + removed.getRecord().getAlignmentStart() + "-" + (removed.getRecord().getAlignmentStart()+removed.getRecord().getReadLength()));
			trailingRecord = getTrailingRecord();
		}
	}
	
}
