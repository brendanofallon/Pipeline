package operator;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.FileBuffer;

/**
 * An Input/Output operator. These are a type of operator that works on one or more input files
 * and writes to one or more output files. 
 * @author brendan
 *
 */
public abstract class IOOperator extends Operator {

	
	protected List<FileBuffer> inputBuffers = new ArrayList<FileBuffer>();
	protected List<FileBuffer> outputBuffers = new ArrayList<FileBuffer>();
	
	
	public void addInputBuffer(FileBuffer buff) {
		inputBuffers.add(buff);
	}
	
	public void addOutputBuffer(FileBuffer buff) {
		outputBuffers.add(buff);
	}
	
	
	@Override
	public void initialize(NodeList children) {
		System.out.println("Initializing operator ");
		
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addInputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addOutputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
	}
	
}
