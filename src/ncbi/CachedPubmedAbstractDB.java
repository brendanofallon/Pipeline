package ncbi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Analagous to CachedGeneSUmmaryDB, this stores pubmed abstract information locally so we're not always
 * re-downloading it from ncbi
 * @author brendan
 *
 */
public class CachedPubmedAbstractDB {

	private Map<Integer, PubMedRecord> map = null;
	private String cacheFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + ".pubmedcache"; 
	private PubMedFetcher fetcher = new PubMedFetcher(); //Fetches gene summaries from ncbi
    private GenePubMedDB genePubMed; //Stores gene-pubmed id mapping
    private int missesSinceLastWrite = 0; //Number of cache misses since last writeToFile
    
	public CachedPubmedAbstractDB() throws IOException {
	    genePubMed = new GenePubMedDB(new File("/home/brendan/resources/gene2pubmed_human"));
		buildMapFromFile();
	}
	
	/**
	 * Obtain the full string containing ncbi's summary for the given gene symbol (i.e. 'RASA1'). 
	 * If the summary is already in the local cache and has not expired, just return it. If not, we 
	 * query ncbi and try to get it. 
	 * @param symbol
	 * @return
	 */
	public PubMedRecord getRecordForID(Integer pubmedID) {
		PubMedRecord summary = map.get(pubmedID);
		
		//Sweet, cache hit. Return the info right away
		if (summary != null) {
			//System.out.println("Got cache hit for gene : " + symbol);
			return summary;
		}

		
        try {

			System.out.println("Fetching abstract #" + pubmedID);
			
			
			PubMedRecord rec = fetcher.getPubMedRecordForID(pubmedID);

			//Sanity check
			if (!(rec.pubMedID == pubmedID)) {
				throw new IllegalArgumentException("Obtained pubmedID does not match requested ID!");
			}
			
			map.put(pubmedID, summary);
			missesSinceLastWrite++;
			if (missesSinceLastWrite > 50)
				writeMapToFile();
			return summary;
		} catch (Exception e) {
			e.printStackTrace();
		}
        
		return null;        
		
	}
	
	public void writeMapToFile() throws IOException {
		System.out.println("Writing cached gene summaries to " + cacheFilePath);
		if (map == null) //Map may not have been initialized
			return;
		
		File cache = new File(cacheFilePath);
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}
		
		BufferedWriter writer = new BufferedWriter(new FileWriter(cache));
		for(Integer id : map.keySet()) {
			PubMedRecord sum = map.get(id);
			writer.write(sum.toString() + "\n");
		}
		System.out.println("Wrote " + map.keySet().size() + " pubmed abstracts to cache");
		writer.close();
		missesSinceLastWrite = 0;
	}
	
	public void buildMapFromFile() throws IOException {
		map = new HashMap<Integer, PubMedRecord>();
		System.out.println("Initializing cached pubmed db");
		File cache = new File(cacheFilePath);
		if (!cache.exists()) {
			try {
				cache.createNewFile();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return;
		}
		
		int expiredRecords = 0;
		BufferedReader reader = new BufferedReader(new FileReader(cache));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length() == 0) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			Integer id = Integer.parseInt(toks[0]);
			Integer year = Integer.parseInt(toks[1]);
			String title = toks[2];
			String citation = toks[3];
			String abs = toks[4];
			
			PubMedRecord rec = new PubMedRecord();
			rec.pubMedID = id;
			rec.yearCreated = year;
			rec.title = title;
			rec.citation = citation;
			rec.abs = abs;
			map.put(id, rec);
			line = reader.readLine();
		}
		System.out.println("Read in " + map.size() + " cached abstracts from cache");
	}
	
	
	public void finalize() {
		try {
			writeMapToFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			super.finalize();
		} catch (Throwable e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	

	
}
