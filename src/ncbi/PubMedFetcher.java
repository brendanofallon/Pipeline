package ncbi;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A class that uses eutils to fetch a pubmed abstract & associated info for a particular pubmed id.
 * PubMed ID's for a gene can be obtained by querying GenePubMedDB. 
 * @author brendan
 *
 */
public class PubMedFetcher {
	
	final String eutilsURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
	final static String ARTICLE_SET = "PubmedArticleSet";
	final static String PUBMED_ARTICLE = "PubmedArticle";
	final static String MEDLINE_CITATION = "MedlineCitation";
	final static String PUB_DATE = "PubDate";
	final static String YEAR = "Year";
	final static String ARTICLE = "Article";
	final static String TITLE = "ArticleTitle";
	final static String ABSTRACT = "Abstract";
	final static String ABSTRACT_TEXT = "AbstractText";
	final static String JOURNAL = "Journal";
	final static String JOURNAL_TITLE = "Title";
	final static String JOURNAL_ISSUE = "JournalIssue";
	final static String VOLUME = "Volume";
	final static String ISSUE = "Issue";

	
	
	public PubMedRecord getPubMedRecordForID(Integer pubmedID) throws IOException, ParserConfigurationException, SAXException {
		URL eutils = new URL(eutilsURL + "db=pubmed&id=" + pubmedID + "&retmode=xml");
        URLConnection yc = eutils.openConnection();
        
        //Create an DOM xml document from the input stream... note that we do not do 
        //any error checking here
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse( yc.getInputStream() );
				
		//From the DOM document construct a gene record object
        PubMedRecord rec = parseDomToPubMed(doc);
        rec.pubMedID = pubmedID;
        return rec;
	}
	
	/**
	 * Take a DOM document and parse it to obtain 
	 * @param doc
	 * @return
	 */
	private PubMedRecord parseDomToPubMed(Document doc) {
		PubMedRecord rec = new PubMedRecord();
		Element rootEl = doc.getDocumentElement();
		if (rootEl == null || (!rootEl.getNodeName().equals(ARTICLE_SET))) {
			throw new IllegalArgumentException("This does not look like an NCBI article set document");
		}
		
		Element pubmedArticleEl = getChildByName(rootEl, PUBMED_ARTICLE);
		if (pubmedArticleEl == null) {
			throw new IllegalArgumentException("Could not find pubmed article");
		}
		Element medlineCitationEl = getChildByName(pubmedArticleEl, MEDLINE_CITATION);
		if (medlineCitationEl == null) {
			throw new IllegalArgumentException("Could not find medline citation element");
		}
		
		
		
		
		
		Element articleEl = getChildByName(medlineCitationEl, ARTICLE);
		//Journal title & info
		Element journalEl = getChildByName(articleEl, JOURNAL);
		Element journalIssueEl = getChildByName(journalEl, JOURNAL_ISSUE);
		Element journalVolumeEl = getChildByName(journalIssueEl, VOLUME);
		Element issueEl = getChildByName(journalIssueEl, ISSUE);
		String volume = getTextContent(journalVolumeEl);
		String issue = getTextContent(issueEl);
		Element pubDateEl = getChildByName(journalIssueEl, PUB_DATE);
		Element yearEl = getChildByName(pubDateEl, YEAR);
		Integer year = Integer.parseInt( getTextContent(yearEl));
				
		Element journalTitleEl = getChildByName(journalEl, JOURNAL_TITLE);
		String journalTitle = getTextContent(journalTitleEl);
		String citation = journalTitle + " " + volume + ":" + issue + " (" + year + ")";
				
		Element articleTitleEl = getChildByName(articleEl, TITLE);
		String title = getTextContent(articleTitleEl);
		
		Element abstractEl = getChildByName(articleEl, ABSTRACT);
		StringBuilder absStr = new StringBuilder();
		NodeList abstractChildren = abstractEl.getChildNodes();
		for(int i=0; i<abstractChildren.getLength(); i++) {
			Node child = abstractChildren.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(ABSTRACT_TEXT)) {
				absStr.append( getTextContent( (Element)child));
			}
		}
		
		
		rec.title = title.replace("\t", " ");
		rec.abs = absStr.toString().replace("\t", " ");
		rec.yearCreated = year;
		rec.citation = citation;
		return rec;
	}
	
	/**
	 * Searches the children of this element for an element whose node-name is the given childName
	 * and returns it, or null if there is no such node
	 * @param el
	 * @param childName
	 * @return
	 */
	private static Element getChildByName(Element el, String childName) {
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals(childName) ) {
				return (Element)child;
			}
		}
		return null;
	}
	
	/**
	 * Searches the child list of this element and returns the value of the first text-node
	 * @param el
	 * @return
	 */
	private static String getTextContent(Element el) {
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.TEXT_NODE) {
				return child.getNodeValue();
			}
		}
		return null;
	}


	
}
