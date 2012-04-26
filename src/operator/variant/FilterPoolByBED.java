package operator.variant;

import java.io.IOException;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.BEDFile;
import buffer.FileBuffer;
import buffer.MultiFileBuffer;
import buffer.variant.VariantPool;

import operator.IOOperator;
import operator.OperationFailedException;
import operator.Operator;
import pipeline.PipelineObject;

public class FilterPoolByBED extends Operator {

	VariantPool poolToFilter = null;
	BEDFile bedFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		try {
			poolToFilter.filterByBED(bedFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new OperationFailedException("IO Error filtering variant pool by BED file, " + e.getMessage(), this);
		}
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof VariantPool) {
					poolToFilter = (VariantPool)obj;
				}
				if (obj instanceof BEDFile) {
					bedFile = (BEDFile)obj;
				}

			}
		}
	}

}
