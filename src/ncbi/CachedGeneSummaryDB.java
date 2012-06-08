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
 * A class that stores Gene Summaries that have been downloaded from NCBI (using the FetchGeneInfo class)
 * so that we're not constantly re-downloading the same summaries over and over.  This creates a hidden file
 * in the users home directory to store cached data, but should be able to gracefully handle cases where
 * the file gets deleted. In fact, deleting the file periodically may be good since it will force a re-download
 * of all genes. 
 *  Right now we write all information to the cache file every 10 cache misses, so if we're constantly missing
 * we write a lot, but if we we haven't downloaded any new info then we don't write much.
 *  
 * @author brendan
 *
 */
public class CachedGeneSummaryDB {
	
	private Map<String, GeneSummary> map = null;
	private String cacheFilePath = System.getProperty("user.home") + System.getProperty("file.separator") + ".geneinfocache"; 
	private FetchGeneInfo fetcher = new FetchGeneInfo(); //Fetches gene summaries from ncbi
    private GeneInfoDB geneInfo; //Stores symbol / refgene id information so we can look genes up by symbol
    
    public static final int expirationDays = 30; // Force re-downloading of records older than one month
    private int missesSinceLastWrite = 0; //Number of cache misses since last writeToFile
    
	public CachedGeneSummaryDB() throws IOException {
	    geneInfo = new GeneInfoDB(new File("/home/brendan/resources/Homo_sapiens.gene_info"));
		buildMapFromFile();
	}
	
	/**
	 * Obtain the full string containing ncbi's summary for the given gene symbol (i.e. 'RASA1'). 
	 * If the summary is already in the local cache and has not expired, just return it. If not, we 
	 * query ncbi and try to get it. 
	 * @param symbol
	 * @return
	 */
	public String getSummaryForGene(String symbol) {
		GeneSummary summary = map.get(symbol);
		
		//Sweet, cache hit. Return the info right away
		if (summary != null) {
			//System.out.println("Got cache hit for gene : " + symbol);
			return summary.summary;
		}
		
		//Dang, gotta look this one up... and put it in the cache
		String id = geneInfo.idForSymbol(symbol);
		if (id == null) {
			//System.out.println("Invalid id lookup for gene symbol : " + symbol);
			return null;
		}
		
        try {

			System.out.println("Fetching summary from NCBI for gene : " + symbol);
			GeneRecord rec = fetcher.fetchInfoForGene(id);
			String summaryString = rec.getSummary();
			//Sanity check
			if (! rec.getSymbol().equals(symbol)) {
				throw new IllegalArgumentException("Obtained symbol does not match requested symbol!");
			}
			
			summary= new GeneSummary();
			summary.symbol = symbol;
			summary.date = "" + System.currentTimeMillis();
			summary.summary = summaryString;
			map.put(symbol, summary);
			missesSinceLastWrite++;
			if (missesSinceLastWrite > 10)
				writeMapToFile();
			return summaryString;
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
		for(String symbol : map.keySet()) {
			GeneSummary sum = map.get(symbol);
			writer.write(symbol + "\t" + sum.date + "\t" + sum.summary + "\n");
		}
		System.out.println("Wrote " + map.keySet().size() + " summaries to cache");
		writer.close();
		missesSinceLastWrite = 0;
	}
	
	public void buildMapFromFile() throws IOException {
		map = new HashMap<String, GeneSummary>();
		System.out.println("Initializing cached gene summary db");
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
			String symbol = toks[0];
			String time = toks[1];
			String summary = toks[2];
			GeneSummary sum = new GeneSummary();
			sum.symbol = symbol;
			sum.date = time;
			sum.summary = summary;
			if (! isExpired(sum))
				map.put(symbol, sum);
			else 
				expiredRecords++;
			
			line = reader.readLine();
		}
		System.out.println("Read in " + map.size() + " cached summaries from file " + cache.getAbsolutePath() + " " + expiredRecords + " of which were past expiration date");
	}
			
	/**
	 * Returns true if the date portion of a gene summary is sufficiently old. This ensures that we're always
	 * using relatively recent gene summary info. 
	 * @param sum
	 * @return
	 */
	private static boolean isExpired(GeneSummary sum) { 
		String recordBirthDate = sum.date;
		Long bd = Long.parseLong(recordBirthDate);
		long elapsedMS = System.currentTimeMillis() - bd;
		long elapsedSecs = elapsedMS / 1000;
		long elapsedMins = elapsedSecs / 60;
		long elapsedHours = elapsedMins / 60;
		long elapsedDays = elapsedHours / 24;
		if (elapsedDays >= expirationDays) {
			return true;
		}
			return false;
		
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
	
	class GeneSummary {
		String symbol;
		String summary;
		String date;
		//Potentially more information here
	}
}
