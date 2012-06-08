package ncbi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores information about gene symbols, id's, NMs, and NPs. Primarily used to find the ref seq id# 
 * for a gene given its symbol. 
 * This generates its information from the the flat file  which can be 
 * obtained from :
 *  ftp://ftp.ncbi.nlm.nih.gov/gene/DATA/GENE_INFO/Mammalia/
 * @author brendan
 *
 */
public class GeneInfoDB {

	Map<String, GeneInfo> map = null;
	
	public GeneInfoDB(File dbFile) throws IOException {
		buildDB(dbFile);
	}
	
	/**
	 * Obtain the numerical NCBI id for the gene with the given symbol. The symbol must be the
	 * 'official' ncbi / refseq symbol this thing doesn't look up synonyms
	 * @param symbol
	 * @return
	 */
	public String idForSymbol(String symbol) {
		GeneInfo rec = map.get(symbol);
		if (rec == null)
			return null;
		else
			return rec.id;
	}
	
	
	
	/**
	 * Read the file and push all information to the map we use to store data
	 * @param file
	 * @throws IOException
	 */
	private void buildDB(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		map = new HashMap<String, GeneInfo>();
		
		while(line != null) {
			if (line.trim().length() ==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			String id = toks[1];
			String symbol = toks[2];
			String[] synonyms = toks[4].split("|");
			
			GeneInfo rec = new GeneInfo();

			rec = new GeneInfo();
			map.put(symbol, rec);
			rec.synonyms = synonyms;
			rec.id = id;
			rec.symbol = symbol;
			line = reader.readLine();
		}
		System.out.println("Built gene info db with " + map.size() + " elements");
	}
	
	class GeneInfo {
		String symbol;
		String id;
		String[] synonyms;		
	}
	
}
