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
 * and writes to one or more output files. Almost all non-trivial operators are subclasses of this class.
 * Input and Output are specified by FileBuffers, which are basically just wrappers for files. 
 * 
 * 
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
	
	/**
	 * Returns the first input buffer of the given class
	 * @param clz
	 * @return
	 */
	public FileBuffer getInputBufferForClass(Class clz) {
		for(FileBuffer buff :  inputBuffers) {
			if (buff.getClass().equals( clz ))
				return buff;
		}
		return null;
	}
	
	/**
	 * Returns a list of all input buffers whose class matches the given class
	 * @param clz
	 * @return
	 */
	public List<FileBuffer> getAllInputBuffersForClass(Class clz) {
		List<FileBuffer> buffers = new ArrayList<FileBuffer>();
		for(FileBuffer buff :  inputBuffers) {
			if (buff.getClass().equals( clz ))
				buffers.add(buff);
		}
		return buffers;
	}
	
	/**
	 * Returns the first output buffer of the given class
	 * @param clz
	 * @return
	 */
	public FileBuffer getOutputBufferForClass(Class clz) {
		for(FileBuffer buff :  outputBuffers) {
			if (buff.getClass().equals( clz ))
				return buff;
		}
		return null;
	}
	
	@Override
	public void initialize(NodeList children) {
		
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
