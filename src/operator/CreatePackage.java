package operator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;

public class CreatePackage extends Operator {

	static final String fileSep = System.getProperty("file.separator");
	private List<FileBuffer> files = null;
	private List<MultiFileBuffer> multiBuffers = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		String projHome = (String)Pipeline.getPropertyStatic(Pipeline.PROJECT_HOME);
		
		String packagePath = projHome + fileSep + "results";
		File packageDir = new File(packagePath);
		
		int count = 1;
		while (packageDir.exists()) {
			packagePath = projHome + fileSep + "results-" + count;
			packageDir = new File(packagePath);
			count++;
		}
		
		logger.info("Creating results package directory at path " + packageDir.getAbsolutePath());
		packageDir.mkdir();
		
		//Move some files into it... 
		for(FileBuffer file : files) {
			file.m
		}
		
		
	}

	@Override
	public void initialize(NodeList children) {
		files = new ArrayList<FileBuffer>();
		multiBuffers = new ArrayList<MultiFileBuffer>();
		
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof MultiFileBuffer) {
					MultiFileBuffer multiBuf = (MultiFileBuffer)obj;
					multiBuffers.add(multiBuf);
					
				}
				else {
					if (obj instanceof FileBuffer) {
						files.add( (FileBuffer)obj);
					}
				}
				
			}
		}
		
	}

}
