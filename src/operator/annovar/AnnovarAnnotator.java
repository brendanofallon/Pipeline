package operator.annovar;

import java.io.IOException;
import java.util.logging.Logger;

import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.AnnovarInputFile;
import buffer.FileBuffer;
import buffer.variant.VariantRec;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import pipeline.PipelineXMLConstants;

/**
 * Base class of things that use Annovar to annotate variant pools
 * @author brendan
 *
 */
public abstract class AnnovarAnnotator extends Annotator {

	public static final String BUILD_VER = "buildver";
	protected String annovarPath = "~/annovar/";
	protected String buildVer = "hg19";
	public static final String FORMAT = "format";
	protected String format = "vcf4"; //Default assumed input format
	protected String annovarPrefix = "annovar.output";

	protected FileBuffer annovarInputFile = null;
	
	public void annotateVariant(VariantRec rec) {
		//Blank on purpose, annovar annotators do something else
	}
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//Find annovar input file
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof AnnovarInputFile) {
					annovarInputFile = (AnnovarInputFile)obj;
				}

			}
		}
		if (annovarInputFile == null)
			throw new IllegalArgumentException("Annovar-based annotators require an annovar input file to run");
		
		Object path = Pipeline.getPropertyStatic(PipelineXMLConstants.ANNOVAR_PATH);
		if (path != null)
			annovarPath = path.toString();
		
		//User can override path specified in properties
		String userBuildVer = properties.get(BUILD_VER);
		if (userBuildVer != null) {
			buildVer = userBuildVer;
		}
				
		//User can override path specified in properties
		String userPath = properties.get(PipelineXMLConstants.PATH);
		if (userPath != null) {
			annovarPath = userPath;
		}
		
		annovarPrefix = Pipeline.getPipelineInstance().getProjectHome() + annovarPrefix;
	}

	
	protected void executeCommand(String command) throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		Process p;
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		logger.info(getObjectLabel() + " executing command : " + command);
		try {
			p = r.exec(command);

			try {
				if (p.waitFor() != 0) {
					logger.info("Task with command " + command + " for object " + getObjectLabel() + " exited with nonzero status");
					throw new OperationFailedException("Task terminated with nonzero exit value : " + System.err.toString() + " command was: " + command, this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("Task was interrupted : " + System.err.toString() + "\n" + e.getLocalizedMessage(), this);
			}

		}
		catch (IOException e1) {
			throw new OperationFailedException("Task encountered an IO exception : " + System.err.toString() + "\n" + e1.getLocalizedMessage(), this);
		}
	}
	
	/**
	 * There have been consistent issues with matching variants found in annovar output files to
	 * those in a variant pool. This is partly because annovar seems to be inconsistent in how it represents
	 * insertions and deletions. For instance, sometimes a variant in a vcf that looks like 1 1025  T  TA
	 * will be converted to an annovar input record of 1  1026 - A, and sometimes 1  1025 - A. Thus, to 
	 * find variants we now look at both 1026 and 1025 (in the variant pool), to see if there's an alt 
	 * allele at either one that matches the one from the file.  
	 * @param contig
	 * @param pos
	 * @param ref
	 * @param alt
	 * @return
	 */
	protected VariantRec findVariant(String contig, int pos, String ref, String alt) {
		VariantRec rec = variants.findRecord(contig, pos);
		if (rec != null)
			return rec;

		//System.out.println("Variant at contig " + contig + " pos: " + pos + " not found, searching for close variants..");
		if (ref.equals("-")) {
			int modPos = pos+1;

			rec = variants.findRecord(contig, modPos);

			if (! rec.getAlt().equals(alt)) {
				//System.out.println("Record found, but alt for record is " + rec.getAlt() + " and alt from file is " + alt);
				return null;
			}

		}
		return rec;
	}
}
