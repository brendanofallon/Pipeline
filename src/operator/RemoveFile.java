package operator;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.FileBuffer;

/**
 * This just (immediately) deletes the file or files given as arguments
 * @author brendan
 *
 */
public class RemoveFile extends Operator {

	protected List<FileBuffer> files;
	
	@Override
	public void performOperation() throws OperationFailedException {
		for(FileBuffer file : files) {
			Logger.getLogger(Pipeline.primaryLoggerName).info("Deleting file : " + file.getAbsolutePath());
			file.getFile().delete();
		}
	}

	@Override
	public void initialize(NodeList children) {
		files = new ArrayList<FileBuffer>();

		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof FileBuffer) {
					files.add( (FileBuffer)obj);
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
	}
	

}
