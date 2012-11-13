package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ComputeRefAlt {

	
	public static void main(String[] args) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(new File("/home/brendan/oldhome/HGMDdatadump/HGMD_PRO/hgmd_sorted.csv")));
		FastaReader faReader = new FastaReader(new File("/home/brendan/resources/human_g1k_v37.fasta"));
		
		String line = reader.readLine();
		
		List<String> lines = new ArrayList<String>(50000);
		String prevTrack ="Z";
		
		while(line != null) {
			String[] toks = line.split("\t");
			String contig = toks[0];
			if (! contig.equals(prevTrack)) {
				faReader.advanceToTrack(contig);
				prevTrack = contig;
			}
			
			
			int pos = Integer.parseInt(toks[1]);
			String cdot = toks[2];
			
			String refBaseFromCdot = cdot.charAt(index);
			
			char refBase = faReader.getBaseAt(contig, pos);
			
			faReader.advanceToTrack(contig);
			line = reader.readLine();
		}
		reader.close();
		
		System.out.println("Read " + lines.size() + " lines");
		
		Collections.sort(lines, new LineSorter());
		
		for(String linex : lines) {
			System.out.println(linex);
		}
		
		System.out.flush();
	}
	
	
	static class LineSorter implements Comparator<String> {

		@Override
		public int compare(String o1, String o2) {
			String[] t1 =o1.split("\t");
			String[] t2 = o2.split("\t");
			
			if (! (t1[0].equals(t2[0]))) {
				return t1[0].compareTo(t2[0]);
			}
			else {
				int p1 = 0;
				int p2 = 0;
				try {
					p1 = Integer.parseInt(t1[1]);
				}
				catch (NumberFormatException nfe) {
					
				}
				
				try {
					p2 = Integer.parseInt(t2[1]);
				}
				catch (NumberFormatException nfe) {
					
				}
				
				return p1 - p2;
				
			}
			
		}
		
	}
}
