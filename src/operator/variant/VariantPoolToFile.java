package operator.variant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.CSVFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantRec;

/**
 * Writes a variant pool to a CSV file. 
 * @author brendan
 *
 */
public class VariantPoolToFile extends Operator {

	private AbstractVariantPool variants = null;
	private CSVFile outputFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null) {
			throw new OperationFailedException("Variant pool not specified", this);
		}
		
		try {
			PrintStream outStream = new PrintStream(new FileOutputStream( outputFile.getFile()));
			List<String> keys = variants.getPropertyKeys();
			
			System.out.println("Found following properties: ");
			for(String key : keys)
				System.out.println(key);
			
			
			keys.add(VariantRec.RSNUM);
			keys.add(VariantRec.OMIM_ID);
			
			variants.emitToTable(keys, outStream);
			outStream.close();
		} catch (FileNotFoundException e) {
			throw new OperationFailedException("Could not write to file : " + outputFile.getFile().getAbsolutePath(), this);
		}
		
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof AbstractVariantPool) {
					variants = (AbstractVariantPool)obj;
				}
				if (obj instanceof CSVFile) {
					outputFile = (CSVFile)obj;
				}

			}
		}
		
		if (outputFile == null) {
			throw new IllegalArgumentException("Output CSV file not specified");
		}
		
		if (variants == null) {
			throw new IllegalArgumentException("Variant pool not specified");
		}
	}

}
