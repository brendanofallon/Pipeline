package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class RowFilter extends IOOperator {

	public static String FILTER = "filter";
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		String filter = properties.get(FILTER);
		String inputPath = inputBuffers.get(0).getAbsolutePath();
		String outputFile = outputBuffers.get(0).getAbsolutePath();
		
		//Right now we emit all lines that are 'comments' (which start with '#') and/or
		//lines which contain the expression in 'filter'
		
		try {
			BufferedReader reader = new BufferedReader(new FileReader(inputPath));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			String line = reader.readLine();
			while(line != null) {
				if (line.startsWith("#") || line.contains(filter)) {
					writer.write(line + "\n");
				}
				line = reader.readLine();
			}
			writer.close();
			reader.close();
		} catch (IOException ex) {
			throw new OperationFailedException("Could not open input file " + inputPath + "\n" + ex.getLocalizedMessage(), this);
		}
		
		
	}

}
