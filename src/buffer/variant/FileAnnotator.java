package buffer.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

import buffer.AnnovarResults;

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
	
	
	/**
	 * Read all lines from the input file, parse the value at the column specified in the constructor,
	 * and add that PROPERTY to the VariantRecord at the given position in the variant pool
	 * @throws IOException 
	 */
	public void annotateAll() throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(inputFile));
		String line = reader.readLine();
		while(line != null) {
			if (line.length()>0) {
				String[] toks = line.split("\\t");
				double score = Double.parseDouble(toks[column]);
				String contig = toks[2];
				int pos = Integer.parseInt( toks[3] );
				
				VariantRec rec = variants.findRecord(contig, pos);
				if (rec != null)
					rec.addProperty(label, score);
			}
			line = reader.readLine();
		}
		reader.close();
	}

	@Override
	public void setAttribute(String key, String value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}
	
}
