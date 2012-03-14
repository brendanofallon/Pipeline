package ncbi;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;


/**
 * A test class to obtain ref gene information from NCBI
 * @author brendan
 *
 */
public class FetchGeneInfo {

	final String eutilsURL = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?";
	
	/**
	 * Obtain an xml-style String representation of the document 
	 * @return
	 * @throws TransformerException
	 * @throws IOException 
	 */
	public String getXMLString(Document doc) throws TransformerException, IOException {
		
		 TransformerFactory transfac = TransformerFactory.newInstance();
         Transformer trans = transfac.newTransformer();
         trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
         trans.setOutputProperty(OutputKeys.INDENT, "yes");

         //create string from xml tree
         StringWriter sw = new StringWriter();
         StreamResult result = new StreamResult(sw);
         DOMSource source = new DOMSource(doc);
         trans.transform(source, result);
         String xmlString = sw.toString();

        return xmlString;
	}
	
	public GeneRecord fetchInfoForGene(String geneID) throws IOException, SAXException, ParserConfigurationException, TransformerException {
		//Grab information from NCBI using eutils
		URL eutils = new URL(eutilsURL + "db=gene&id=" + geneID + "&retmode=xml");
        URLConnection yc = eutils.openConnection();
        
        //Create an DOM xml document from the input stream... note that we do not do 
        //any error checking here
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		builder = factory.newDocumentBuilder();
		Document doc = builder.parse( yc.getInputStream() );
		
		System.out.println( getXMLString(doc) );
		
		//From the DOM document construct a gene record object
        GeneRecord rec = GeneRecordParser.parse(doc);
        return rec;
	}
	
	public static void main(String[] args) throws Exception {
        FetchGeneInfo fetcher = new FetchGeneInfo();
        GeneRecord rec = fetcher.fetchInfoForGene("9883");
        System.out.println(rec);
    }
	
}
