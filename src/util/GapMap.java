package util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Stores information for where gaps are for accurate fasta writing. This is
 * used by VcfToFasta to emit alignments where things are actually aligned
 * @author brendan
 *
 */
public class GapMap {

	//List of all gaps. Everytime a new gap is added we re-sort the list based
	//on starting position
	private List<Gap> gaps = new ArrayList<Gap>(64);
	
	public void addGap(String name, int start, int end) {
		Gap gap = new Gap();
		gap.owner = name;
		gap.start = start;
		gap.end = end;
		gaps.add(gap);
		Collections.sort(gaps, new GapComparator());
	}
	
	class Gap {
		String owner;
		int start;
		int end;
	}
	
	class GapComparator implements Comparator<Gap> {

		@Override
		public int compare(Gap g1, Gap g2) {
			return g1.start - g2.start;
		}
		
	}
}
