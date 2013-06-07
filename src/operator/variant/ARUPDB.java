package operator.variant;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.broad.tribble.readers.TabixReader;

/**
 * Stores information about variants that have previously been observed at ARUP, right now 
 * this expects things to be in the .csv type flatfile produced by the varUtils tool 'buildvcfdb'
 * ... and now *MUST* be tabix-compressed and indexed
 * @author brendan
 *
 */
public class ARUPDB {

	private File dbFile;
	private Map<Integer, String> headerToks = new HashMap<Integer, String>();
	private TabixReader reader = null;
	private int overallInfoIndex = -1; //Stores column index of "overall" information for speedy lookup
	private double overallSampleCount = 0;
	private Map<Integer, Integer> sampleCounts = new HashMap<Integer, Integer>(); //List of sample count totals for all columns
	
	public ARUPDB(File dbFile) throws IOException {
		if (! dbFile.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		this.dbFile = dbFile;
		
		reader = new TabixReader(dbFile.getAbsolutePath());
		
		//Read header
		String header = reader.readLine();
		String[] toks = header.split("\t");
		for(int i=3; i<toks.length; i++) {
			String headerDesc = toks[i];
			if (toks[i].contains("[")) {
				headerDesc = toks[i].substring(0, toks[i].indexOf("["));
			}
			headerToks.put(i, headerDesc);
			if  (headerDesc.trim().equalsIgnoreCase("overall")) {
				overallInfoIndex = i;
				overallSampleCount = parseSampleCount(toks[i]);
			}
			sampleCounts.put(i, parseSampleCount(toks[i]));
		}
		
	}

	/**
	 * Parse and return the sample count associated with a column header
	 * @param header
	 * @return
	 */
	private static int parseSampleCount(String header) {
		if (header.contains("[") && header.contains("]")) {
			String count = header.substring(header.indexOf("[") +1, header.indexOf("]"));
			return Integer.parseInt(count);
		}
		else {
			return 0;
		}
	}
	
	public String[] getInfoForPostion(String contig, int pos) throws IOException {
		String queryStr = contig + ":" + pos + "-" + (pos);
		
		try {
			TabixReader.Iterator iter = reader.query(queryStr);

			if(iter != null) {
					String str = iter.next();
					while(str != null) {
						String[] toks = str.split("\t");
						Integer qPos = Integer.parseInt(toks[1]);
						
						
						if (qPos == pos) {
							//Found one..
							
							String[] overallToks = toks[overallInfoIndex].split(","); 
							double overallHets = Double.parseDouble(overallToks[0]);
							double overallHoms = Double.parseDouble(overallToks[1]);
							double overallAF = (overallHets + 2.0*overallHoms)/(double)(2.0*overallSampleCount); 
							String overallStr = "" + overallAF;
							//return overallStr;
							
							

							//Create fancier details string here...
							String details = "";
							for(int i=3; i<toks.length; i++) {
								int sampleCount = sampleCounts.get(i);
								String headerDesc = headerToks.get(i);
								String[] typeToks = toks[i].split(",");
								double hets = Double.parseDouble(typeToks[0]);
								double homs = Double.parseDouble(typeToks[1]);
								double typeFreq = (hets + 2.0*homs) / (double)(2.0*sampleCount);
								String typeStr = "" + 100.0*typeFreq;
								if (typeStr.length() > 4) {
									typeStr = typeStr.substring(0, 4);
								}
								details = details + headerDesc + ": " + typeStr + "%; ";
							}
							
							return new String[]{overallStr, details};
	
							//Older version, compatible with early / bad version of ARUP freq data
//							String retStr = Integer.parseInt(toks[4].replace(".0", "")) + " total";
//							for(int i=5; i<toks.length; i++) {
//								int count = (int) Double.parseDouble(toks[i]);
//								if (count > 0) {
//									retStr = retStr + ", " + count + ":" + headerToks.get(i);
//								}
//							}
//							return retStr;
						}
						if (qPos > pos) {
							break;
						}
						str = iter.next();
					}
			}
		}
		catch (RuntimeException rex) {
			//Bad contigs will cause an array out-of-bounds exception to be thrown by
			//the tabix reader. There's not much we can do about this since the methods
			//are private... right now we just ignore it and skip this variant
		}
		
		
		
		return null;
	}
	
	
//	public static void main(String[] args) throws IOException {
//		ARUPDB db = new ARUPDB(new File("/home/brendan/resources/arup_db_20121220.csv.gz"));
//		
//		System.out.println( db.getInfoForPostion("17", 22259905));
//	}
}
