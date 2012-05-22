package ncbi;

/**
 * Storage for a few items that describe a paper in pubmed. These are typically obtained by a 
 * PubMedFetcher or a CachedPubmedAbstractsDB
 * @author brendan
 *
 */
public class PubMedRecord {

		Integer pubMedID; //Unique id used to find this record
		String title; //Title of paper
		Integer yearCreated; //Year paper was published
		String citation; //Full citation of paper 
		String abs; //Abstract
		
		/**
		 * This MUST be easily parseable in CachedPubmedAbstractsDB
		 */
		public String toString() {
			return pubMedID + "\t" + yearCreated + "\t" + title + "\t" + citation + "\t" + abs;
		}
	
}
