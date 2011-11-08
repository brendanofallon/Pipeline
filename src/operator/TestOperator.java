package operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.FileBuffer;

import pipeline.PipelineObject;

public class TestOperator extends Operator {

	protected File inputFile = null;
	
	@Override
	public void performOperation() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}



	@Override
	public void initialize(NodeList children) {
		System.out.println("Initializing operator ");
		
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		
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
