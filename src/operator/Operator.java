package operator;

import java.nio.Buffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import buffer.FileBuffer;

import pipeline.PipelineObject;

public abstract class Operator extends PipelineObject {

	protected Map<String, String> properties = new HashMap<String, String>();
	protected List<FileBuffer> inputBuffers = new ArrayList<FileBuffer>();
	protected List<FileBuffer> outputBuffers = new ArrayList<FileBuffer>();
	
	
	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
		
	public void addInputBuffer(FileBuffer buff) {
		inputBuffers.add(buff);
	}
	
	public void addOutputBuffer(FileBuffer buff) {
		outputBuffers.add(buff);
	}
	
	public abstract void performOperation() throws OperationFailedException;
}
