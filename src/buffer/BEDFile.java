package buffer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import pipeline.Pipeline;
import util.VCFLineParser;

public class BEDFile extends FileBuffer {

	private Map<String, List<Interval>> intervals = null;
	private final IntervalComparator intComp = new IntervalComparator();
	
	public BEDFile() {
	}
	
	public BEDFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "BEDfile";
	}
	
	/**
	 * Build an intervals map from this BED file and strip 'chr' from all contigs
	 * @throws IOException
	 */
	public void buildIntervalsMap() throws IOException {
		buildIntervalsMap(true);
	}
	
	/**
	 * Construct/initialize a map which allows us to easily look up which sites are in
	 * the intervals described by this BED file. If arg is true, strip chr from all contig labels
	 * @throws IOException 
	 */
	public void buildIntervalsMap(boolean stripChr) throws IOException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info("Building intervals map for BED file " + getFilename());
		BufferedReader reader = new BufferedReader(new FileReader(getAbsolutePath()));
		String line = reader.readLine();
		intervals = new HashMap<String, List<Interval>>();
		while (line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\s");
				String contig = toks[0];
				if (stripChr)
					contig = contig.replace("chr", "");
				Integer begin = Integer.parseInt(toks[1]);
				Integer end = Integer.parseInt(toks[2]);
				Interval interval = new Interval(begin, end);

				List<Interval> contigIntervals = intervals.get(contig);
				if (contigIntervals == null) {
					contigIntervals = new ArrayList<Interval>(2048);
					intervals.put(contig, contigIntervals);
					//System.out.println("BED file adding contig: " + contig);
				}
				contigIntervals.add(interval);
			}
			line = reader.readLine();
		}
		
		sortAllContigs();
		
		logger.info("Done building intervals map for " + getFilename());
//		for(String contig : intervals.keySet()) {
//			List<Interval> list = intervals.get(contig);
//			int tot = countSize(list);
//			System.out.println(contig + "\t :" + list.size() + "\t" + tot);
//		}
	}
	
	/**
	 * Sort all intervals in all contigs by starting position
	 */
	private void sortAllContigs() {
		for(String contig : intervals.keySet()) {
			List<Interval> list = intervals.get(contig);
			Collections.sort(list, new IntervalComparator());
		}
	}

	/**
	 * Count number of bases subtended by all intervals in the list
	 * @param list
	 * @return
	 */
	private static int countSize(List<Interval> list) {
		int size = 0;
		for(Interval inter : list) {
			size += inter.end - inter.begin;
		}
		return size;
	}
	
	/**
	 * Returns true if the intervals map has been created
	 * @return
	 */
	public boolean isMapCreated() {
		return intervals != null;
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
	
	class Interval implements Comparable {
		final int begin;
		final int end;
		
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
	
	
	public static void main(String[] args) {
		File file = new File("/home/brendan/exomeBEDfiles/SureSelect_50mb_with_annotation_b37.bed");
		try {
			BEDFile bed = new BEDFile(file);
			bed.buildIntervalsMap();
			boolean c1 = bed.contains("1", 14000);
			boolean c2 = bed.contains("1", 610388);
//			boolean c3 = bed.contains("1", 14468);
//			boolean c4 = bed.contains("1", 14587);
//			boolean c5 = bed.contains("1", 14588);
//			boolean c6 = bed.contains("1", 14600);
//			boolean c7 = bed.contains("1", 14639);
//			boolean c8 = bed.contains("1", 35670);
//			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}
