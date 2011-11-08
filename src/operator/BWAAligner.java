package operator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BWAAligner extends IOOperator {
	
	
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	protected int defaultThreads = 4;
	protected int threads = defaultThreads;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Runtime r = Runtime.getRuntime();
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String readsPath = inputBuffers.get(1).getAbsolutePath();
		String outputPath = outputBuffers.get(0).getAbsolutePath();
		
		String command = pathToBWA + " aln -t " + threads + " " + referencePath + " " + readsPath + " > " + outputPath;
		
		if (verbose) {
			System.out.println("BWA aligner command : " + command);
		}
		
		try {
			Process p = r.exec(command);
			BufferedReader outputReader = new BufferedReader( new InputStreamReader( p.getInputStream() ) );
			
			// Emit output to stdout
//			String line = outputReader.readLine();
//			while (line != null) {
//				System.out.println(line);
//				outputReader.readLine();
//			}
			
			try {
				if (p.waitFor() != 0) {
					throw new OperationFailedException("BWA aln process terminated with nonzero exit value", this);
				}
			} catch (InterruptedException e) {
				throw new OperationFailedException("BWA aln process was interrupted : " + e.getLocalizedMessage(), this);
			}
			
			outputReader.close();
			
		} catch (IOException e) {
			throw new OperationFailedException("BWA aln process was failed with IOException : " + e.getLocalizedMessage(), this);
		}
	}

}
