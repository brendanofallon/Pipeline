package ncbi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simialr to GeneInfoDB, this class looks up the PubMed id's associated with a particular gene ID using
 * the gene2pubmed text file that can be obtained from
 *  ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/
 *  
 * This file is likely to get updated fairly rapidly so it should probably be re-downloaded weekly. 
 *  
 * @author brendan
 *
 */
public class GenePubMedDB {

	private Map<Integer, List<Integer>> map = null;
	
	public GenePubMedDB(File dbFile) throws IOException {
		readFile(dbFile);
	}
	
	public List<Integer> getPubMedIDsForGene(Integer geneID) {
		if (map == null)
			throw new IllegalStateException("Map has not been initialized");
		return map.get(geneID);
	}
	
	/**
	 * Parse the contents of the source text file and
	 * @param dbFile
	 * @throws IOException
	 */
	private void readFile(File dbFile) throws IOException {
		map = new HashMap<Integer, List<Integer>>();
		BufferedReader reader = new BufferedReader(new FileReader(dbFile));
		String line = reader.readLine();
		int pubmedsRead = 0;
		
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length != 3) {
				System.err.println("Warning : Cannot parse line : " + line + ", skipping it");
			}
			else {
				Integer geneID = Integer.parseInt(toks[1]);
				Integer pubmedID = Integer.parseInt(toks[2]);
				pubmedsRead++;
				List<Integer> geneList = map.get(geneID);
				if (geneList == null) {
					geneList = new ArrayList<Integer>(4);
					map.put(geneID, geneList);
				}
				geneList.add(pubmedID);
			}
		}
		System.out.println("Initialized PubMed lookup list with " + map.size() + " genes and " + pubmedsRead + " abstracts");
		
	}
}
