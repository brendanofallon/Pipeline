package operator;

import java.io.IOException;
import java.util.logging.Logger;

import operator.qc.QCReport;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import pipeline.WranglerStatusWriter;
import buffer.BAMFile;
import buffer.CSVFile;
import buffer.VCFFile;

/**
 * This class writes information to a StatusLogger that summarizes the final results
 * of a run. Most importantly, it makes symbolic links to some of the results files 
 * (bams, vcfs, etc.) so that they can be accessed by an http server
 * @author brendan
 *
 */
public class StatusFinalizer extends Operator {

	public static final String CREATE_LINKS = "create.links";
	public static final String SAMPLE = "sample";
	static final String WEB_ROOT = "/usr/local/apache2/htdocs/";
	static final String RESULT_DIR = "results/";
	static final String SERVER_URL = "http://10.90.68.168/";
	
	StatusLogger statusLogger = null;
	QCReport qcMetrics = null;
	VCFFile vcfFile = null;
	BAMFile finalBam = null;
	CSVFile annotatedVars = null;
	private boolean createLinks = true;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		String createLinksAttr = this.getAttribute(CREATE_LINKS);
		if (createLinksAttr != null && createLinksAttr.length()>0) {
			createLinks = Boolean.parseBoolean(createLinksAttr);
		}
		
		String sampleID = this.getAttribute(SAMPLE);
		if (sampleID == null) {
			sampleID = "sample-" + ("" + System.currentTimeMillis()).substring(5);
		}
		
		
		WranglerStatusWriter writer = statusLogger.getWriter();
		if (writer == null)
			throw new OperationFailedException("WrangerStatusWriter not initialized, apparently", this);
		
		if (qcMetrics != null) {
			
			String linkName = "quality-metrics-" +sampleID + ".html";
			String linkDir = sampleID + "-QC"; //Can't have trailing slash!
			String linkTarget = qcMetrics.getOutputDir().getAbsolutePath() + "/qc-metrics.html";
			writer.addMessage("QC report",  SERVER_URL + RESULT_DIR + linkDir + "/" + linkName );
			
			if (createLinks) {
				createLink(qcMetrics.getOutputDir().getAbsolutePath(), WEB_ROOT + RESULT_DIR + linkDir);
				createLink(linkTarget, WEB_ROOT + RESULT_DIR + linkDir + "/" + linkName );
			}
		}
		
		if (vcfFile != null) {

			String linkName = sampleID + ".final.vcf"; 
			String linkTarget = vcfFile.getAbsolutePath();
			
			writer.addMessage("Final VCF", SERVER_URL + RESULT_DIR + linkName );
			if (createLinks) {
				createLink(linkTarget, WEB_ROOT + RESULT_DIR + linkName );
			}
		}
		
		if (finalBam != null) {
			
			String linkName = sampleID + ".final.bam"; 
			String linkTarget = finalBam.getAbsolutePath();

			writer.addMessage("Final BAM",  SERVER_URL + RESULT_DIR + linkName );
			
			if (createLinks) {
				createLink(linkTarget, WEB_ROOT +  RESULT_DIR + linkName );
				createLink(linkTarget + ".bai", WEB_ROOT +  RESULT_DIR + linkName + ".bai" );
			}
		}
		
		if (annotatedVars != null) {
			String linkName = sampleID + ".annotated.csv"; 
			String linkTarget = annotatedVars.getAbsolutePath();
			
			writer.addMessage("Annotated variants", SERVER_URL + RESULT_DIR + linkName );
			if (createLinks) {
				createLink(linkTarget, WEB_ROOT + RESULT_DIR + linkName );
			}			
		}
		
		
	}

	/**
	 * Create and execute a process that makes a symbolic link to the target from the linkName
	 * @param target
	 * @param linkName
	 */
	private void createLink(String target, String linkName) {
		Logger.getLogger(Pipeline.primaryLoggerName).info("Creating symbolic link from " + linkName + " to " + target);
		ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", "ln -s " + target + " " + linkName);
		processBuilder.redirectErrorStream(true);
		
		try {
			Process proc = processBuilder.start();
			
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				Logger.getLogger(Pipeline.primaryLoggerName).warning("Nonzero exit value for link-creation process, could not create link: "  + linkName);
			}
		} catch (IOException e) {
			
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
	}

	@Override
	public void initialize(NodeList inputChildren) {
		
		System.out.println("Number of chlidren: " + inputChildren.getLength());
		for(int i=0; i<inputChildren.getLength(); i++) {
			Node iChild = inputChildren.item(i);
			System.out.println("child name:" + iChild.getNodeName() + ":" + iChild.getNodeValue());
		}
		
		for(int i=0; i<inputChildren.getLength(); i++) {
			Node iChild = inputChildren.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				
				if (obj instanceof QCReport) {
					qcMetrics = (QCReport)obj;
					continue;
				}
				
				if (obj instanceof VCFFile) {
					vcfFile = (VCFFile)obj;
					continue;
				}
				
				if (obj instanceof BAMFile) {
					finalBam = (BAMFile)obj;
					continue;
				}
				
				if (obj instanceof StatusLogger) {
					statusLogger = (StatusLogger)obj;
					continue;
				}
				
				if (obj instanceof CSVFile) {
					annotatedVars = (CSVFile)obj;
					continue;
				}
				
				throw new IllegalArgumentException("Unknown input file type:" + obj.getObjectLabel() + " is class: " + obj.getClass().getCanonicalName());
			}
		}
		
		if (statusLogger == null) {
			throw new IllegalArgumentException("No StatusLogger provided to StatusFinalizer");
		}

		
	}

}
