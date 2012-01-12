package operator.annovar;

import java.io.IOException;

import operator.OperationFailedException;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.AnnovarInputFile;
import buffer.FileBuffer;

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

		try {
			p = r.exec(command);

			try {
				if (p.waitFor() != 0) {
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
}
