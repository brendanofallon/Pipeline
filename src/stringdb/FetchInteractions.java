package stringdb;

import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

public class FetchInteractions {

	final String stringdbURL = "http://string-db.org/api/psi-mi-tab/interactionsList?";
	protected int defaultAdditionalNodeCount = 10;
			
	public List<InteractionNode> getInteractionsList(List<String> geneIDs) {
		return getInteractionsList(geneIDs, defaultAdditionalNodeCount);
	}
			
	public List<InteractionNode> getInteractionsList(List<String> geneIDs, int additionalNodeCount) {
		StringBuilder urlStr = new StringBuilder(stringdbURL + "identifiers=");
		for(int i=0; i<geneIDs.size()-1; i++) {
			urlStr.append(geneIDs.get(i) + "\n");
		}
		urlStr.append(geneIDs.get(geneIDs.size()-1));
		urlStr.append("&species=9606");
		urlStr.append("&additional_network_nodes=" + additionalNodeCount);
	
		URL stringURL = new URL(urlStr.toString());
		URLConnection yc = stringURL.openConnection();
         yc.getInputStream();
         
         //Info will be in easy-to-read text format
         //check out : http://string-db.org/api/psi-mi-tab/interactionsList?identifiers=ENG%0AACVRL1&species=9606&additional_network_nodes=2
         
         
	}

	
	class InteractionNode {
		String first;
		String second;
		double score;
	}
}
