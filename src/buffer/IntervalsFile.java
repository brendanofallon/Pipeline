package buffer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Any file that describes a list of genomic intervals
 * @author brendan
 *
 */
public abstract class IntervalsFile extends FileBuffer {

	protected Map<String, List<Interval>> intervals = null;
	protected final IntervalComparator intComp = new IntervalComparator();
	
	public IntervalsFile(File source) {
		super(source);
	}
	
	public IntervalsFile() {
		//Blank on purpose
	}
	
	
	/**
	 * Read the source file and build a list of intervals in memory
	 * @throws IOException
	 */
	public abstract void buildIntervalsMap() throws IOException;
	
	
	/**
	 * Returns true if the intervals map has been created
	 * @return
	 */
	public boolean isMapCreated() {
		return intervals != null;
	}

	/**
	 * Returns a list of all intervals overlapping the given position
	 * @param contig
	 * @param pos
	 * @return
	 */
	public List<Interval> getOverlappingIntervals(String contig, int pos) {
		List<Interval> cInts = intervals.get(contig);
		Interval qInterval = new Interval(pos, pos);
		List<Interval> intervals = new ArrayList<Interval>();
		
		if (cInts == null) {
			return intervals;
		}
		else {
			int index = Collections.binarySearch(cInts, qInterval, intComp);
			if (index > 0) {
				//pos is on an interval boundary
			}
			else {
				index = -1*index -1;
			}
			
			
			
		}
		return intervals;
	}
	
	public boolean contains(String contig, int pos) {
		return contains(contig, pos, true);
	}
	
	public boolean contains(String contig, int pos, boolean warn) {
		List<Interval> cInts = intervals.get(contig);
		Interval qInterval = new Interval(pos, pos);
		if (cInts == null) {
			if (warn)
				System.out.println("Contig " + contig + " is not in BED file!");
			return false;
		}
		else {
			int index = Collections.binarySearch(cInts, qInterval, intComp);
			if (index >= 0) {
				//System.out.println("Interval " + cInts.get(index) + " contains the position " + pos);
				//An interval starts with the query position so we do contain the given pos
				return true;
			}
			else {
				//No interval starts with the query pos, but we 
				int keyIndex = -index-1 -1;
				if (keyIndex < 0) {
					//System.out.println("Interval #0 does NOT contain the position " + pos);
					return false;
				}
				Interval cInterval = cInts.get(keyIndex);
				if (pos >= cInterval.begin && pos < cInterval.end) {
					//System.out.println("Interval " + cInterval + " contains the position " + pos);
					return true;
				}
				else {
					//System.out.println("Interval " + cInterval + " does NOT contain the position " + pos);
					return false;
				}
			}
		}
	}
	
	/**
	 * Returns the number of bases covered by all of the intervals
	 * @return
	 */
	public int getExtent() {
		int size = 0;
		if (! isMapCreated()) {
			try {
				buildIntervalsMap();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}
		
		if (intervals == null) {
			return 0;
		}
		
		for(String contig : intervals.keySet()) {
			List<Interval> intList = intervals.get(contig);
			for(Interval interval : intList) {
				size += interval.end - interval.begin;
			}
		}
		return size;
	}
	
	/**
	 * Returns the number of intervals in this interval collections
	 * @return
	 */
	public int getIntervalCount() {
		if (! isMapCreated()) {
			try {
				buildIntervalsMap();
			} catch (IOException e) {
				e.printStackTrace();
				return 0;
			}
		}
		
		if (intervals == null) {
			return 0;
		}
		
		int size = 0;
		for(String contig : intervals.keySet()) {
			List<Interval> intList = intervals.get(contig);
			size += intList.size();
		}
		return size;
	}
	
	public class Interval implements Comparable {
		
		final int begin;
		final int end;
		Object info = null; //Optional information associated with this interval. 
		
		public Interval(int begin, int end, Object info) {
			this.begin = begin;
			this.end = end;
			this.info = info;
		}
		
		public Interval(int begin, int end) {
			this.begin = begin;
			this.end = end;
		}

		@Override
		public int compareTo(Object o) {
			if (o instanceof Interval) {
				Interval inter = (Interval)o;
				return this.begin - inter.begin;
			}
			return 0;
		}
		
		public String toString() {
			return "[" + begin + "-" + end + "]";
		}
	}
	
	public class IntervalComparator implements Comparator<Interval> {

		@Override
		public int compare(Interval o1, Interval o2) {
			return o1.begin - o2.begin;
		}
		
	}
	
}
