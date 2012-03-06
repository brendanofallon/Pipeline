package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

public class FileAnnotator extends PipelineObject {

	protected File inputFile;
	protected String label;
	protected int column;
	protected VariantPool variants;
	protected Map<String, String> properties = new HashMap<String, String>();
	
	public FileAnnotator(File inputFile, String propertyLabel, int column, VariantPool pool) {
		this.inputFile = inputFile;
		this.column = column;
		this.label = propertyLabel;
		this.variants = pool;
	}
	
	
	public void annotateAll() throws IOException {
		annotateAll(true);
	}
	
	/**
	 * Read all lines from the input file, parse the value at the column specified in the constructor,
	 * and add that PROPERTY to the VariantRecord at the given position in the variant pool
	 * @throws IOException 
	 */
	public void annotateAll(boolean isProperty) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\t");
				
				String contig = toks[2];
				int pos = Integer.parseInt( toks[3] );
				
				//Unfortunately, annovar likes to strip the leading base from deletion sequences, converting
				//a record that looks like pos: 5 ref: CCT alt: C 
				//to something like :      pos: 6 ref: CT alt: -
				//so we won't be able to find the record since its position will have changed. 
				//Below is a kludgy workaround that subtracts one from the position if a deletion is detected
				
				//This shouldn't be used now since the new policy is to always remove identical leading
				//bases from all variant records
//				if (toks[6].trim().equals("-")) {
//					pos--;
//				}
				
				VariantRec rec = variants.findRecord(contig, pos);
				if (rec != null) {
					if (isProperty) {
						double score = Double.parseDouble(toks[column]);
						rec.addProperty(label, score);
					}
					else {
						rec.addAnnotation(label, toks[column]);
					}
				}
			}
			line = reader.readLine();
		}
		reader.close();
	}

	@Override
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
	
	public String getAttribute(String key) {
		return properties.get(key);
	}

	public Collection<String> getAttributeKeys() {
		return properties.keySet();
	}
	
	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}
	
}
