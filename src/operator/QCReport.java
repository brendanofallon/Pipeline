package operator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.FileBuffer;

import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * Builds a small html report containing some QC information. 
 * @author brendan
 *
 */
public class QCReport extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
	
		File homeDir = new File( Pipeline.getPipelineInstance().getProjectHome() );
		if ( (! homeDir.exists()) || (! homeDir.isDirectory()) ) {
			throw new OperationFailedException("Could not open project home directory : " + homeDir.getAbsolutePath(), this);
		}
		
		
		File fastqcDir = new File(homeDir.getAbsolutePath() + "/fastqc");
		String fqResultsPath = null;
		if (fastqcDir.exists() && fastqcDir.isDirectory()) {
			File[] fqFiles = fastqcDir.listFiles();
			for(int i=0; i<fqFiles.length; i++) {
				if (fqFiles[i].isDirectory() && fqFiles[i].getName().endsWith("fastqc")) {
					fqResultsPath = fqFiles[i].getName() + "/fastqc_report.html";
				}
			}
		}
		
		
		File metricsDir = new File(homeDir.getAbsolutePath() + "/metrics");
		List<String> metricsPaths = new ArrayList<String>();
		if (metricsDir.exists() && metricsDir.isDirectory()) {
			File[] metricsFiles = metricsDir.listFiles();
			for(int i=0; i<metricsFiles.length; i++) {
				if (metricsFiles[i].getName().endsWith(".pdf"))
					metricsPaths.add("../metrics/" + metricsFiles[i].getName());
			}
		}
		
		
		File outputFile = new File(fastqcDir.getAbsolutePath() + "/qc_report.html");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile.getAbsolutePath()));
			
			writeHeader(writer);
			
			writer.write("<p> QC is awesome </p>");
			
			writer.write("<a href=\"" + fqResultsPath + "\"> FastQ report </a>");
			
			if (metricsPaths.size()==0) {
				writer.write("No metrics files found.");
			}
			else {
				for(int i=0; i<metricsPaths.size(); i++) {
					writer.write("<img src=\"" + metricsPaths.get(i) + "\"> : " + metricsPaths.get(i));
				}
			}
			
			writeFooter(writer);
			
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("Could not write qc_report : " + e.getMessage(), this);
		}
		
		
		File[] files = homeDir.listFiles();
		
		
	}

	private void writeFooter(BufferedWriter writer) throws IOException {
		writer.write("</body>\n </html> ");
	}

	private void writeHeader(BufferedWriter writer) throws IOException {
		writer.write("<html>\n <head> \n <link rel=\"stylesheet\" type=\"text/css\" href=\"acg.css\" /> \n	</head> <body> \n");
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				// ?
			}
		}
	}

}
