package operator;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class BWAIndexer extends IOOperator {

	public static final String PATH = "path";
	public static final String ALGORITHM = "algorithm";
	
	protected String defaultAlgorithm = "bwtsw"; //Other options are "is" and "div", both of which do not work for long genomes
	protected String algorithm = defaultAlgorithm;
	
	protected String filePathToIndex = null;
	
	protected String pathToBWA = "bwa";
	
	@Override
	public void performOperation() throws OperationFailedException {

		Runtime r = Runtime.getRuntime();

		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
		
		String algoAttr = properties.get(ALGORITHM);
		if (algoAttr != null) {
			algorithm = algoAttr;
		}
		
		filePathToIndex = inputBuffers.get(0).getAbsolutePath();
		
		String command = pathToBWA + " index -a " + algorithm + " " + filePathToIndex;
		
		if (verbose) {
			System.out.println("BWA indexer command : " + command);
		}
		
		try {
			Process p = r.exec(command);
			BufferedReader outputReader = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			
			// Emit output to stdout
			String line = outputReader.readLine();
			while (line != null) {
				System.out.println(line);
				outputReader.readLine();
			}
			
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("BWA index process terminated with nonzero exit value", this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("BWA index process was interrupted : " + e.getLocalizedMessage(), this);
			}
			
			outputReader.close();
			
		} catch (IOException e) {
			throw new OperationFailedException("BWA index process was failed with IOException : " + e.getLocalizedMessage(), this);
		}
	}

	
}
