package operator.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buffer.variant.VariantRec;

/**
 * Stores information about variants that have previously been observed at ARUP, right now 
 * this expects things to be in the .csv type flatfile produced by the varUtils tool 'buildvcfdb'
 * @author brendan
 *
 */
public class ARUPDB {

	private List<VariantRec> contigVars = null;
	private File dbFile;
	private BufferedReader reader = null;
	private String currentContig = null;
	private String currentLine = null;
	private Map<Integer, String> headerToks = new HashMap<Integer, String>();
	
	
	public ARUPDB(File dbFile) throws IOException {
		if (! dbFile.exists()) {
			throw new IOException("File " + dbFile.getAbsolutePath() + " does not exist");
		}
		this.dbFile = dbFile;
		
		//Read the header...
		reader = new BufferedReader(new FileReader(dbFile));
		String header = reader.readLine();
		String[] toks = header.split("\t");
		for(int i=4; i<toks.length; i++) {
			headerToks.put(i, toks[i]);
		}
		
		reader.close();
	}
	
	
	public String getInfoForPostion(String contig, int pos) throws IOException {
		if (currentContig == null || (! currentContig.equals(contig)))
			advanceToContig(contig);
		
		if (currentLine == null)
			return null;
		
		
		String[] toks = currentLine.split("\t");
		Integer qPos = Integer.parseInt(toks[1]);
		while(contig.equals(toks[0]) && qPos < pos) {
			advanceLine();
			toks = currentLine.split("\t");
			qPos = Integer.parseInt(toks[1]);
		}
		
		if (qPos == pos) {
			//Found one..
			String retStr = Integer.parseInt(toks[4].replace(".0", "")) + " total";
			for(int i=5; i<toks.length; i++) {
				int count = (int) Double.parseDouble(toks[i]);
				if (count > 0) {
					retStr = retStr + ", " + count + ":" + headerToks.get(i);
				}
			}
			return retStr;
		}
		return null;
	}
	
	private boolean advanceLine() throws IOException {
		currentLine = reader.readLine();
		return currentLine != null;
	}
	
	private void advanceToContig(String contig) throws IOException {
		if (reader != null)
			reader.close();
		
		reader = new BufferedReader(new FileReader(dbFile));
		
		String line = reader.readLine();
		line = reader.readLine();
		String contigStr = line.split("\t")[0];
		while(line != null && (! contigStr.equals(contig))) {
			contigStr = line.split("\t")[0];
			line = reader.readLine();
		}
		
		if (line == null) {
			currentContig = null;
			currentLine = null;
		}
		else {
			currentContig = contig;
			currentLine = line;
		}	
	}
	
	
	public static void main(String[] args) throws IOException {
		ARUPDB db = new ARUPDB(new File("all_db.csv"));
		System.out.println(db.getInfoForPostion("19", 80833));
		
		System.out.println(db.getInfoForPostion("15", 30307205));
		System.out.println(db.getInfoForPostion("15", 30309238));
		
	}
}
