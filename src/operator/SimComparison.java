package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import buffer.FileBuffer;
import buffer.TextBuffer;
import buffer.VCFFile;

/**
 * A class that compares the calls in a .vcf file to those in a tabular "true" variants file
 * @author brendan
 *
 */
public class SimComparison extends IOOperator {

	
	protected Map<Integer, Integer> simVariantMap = new HashMap<Integer, Integer>();
	protected Map<Integer, Integer> trueVariantMap = new HashMap<Integer, Integer>();
	
	/**
	 * Returns the total number of lines in a file
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	private static int countLines(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		int count = 0;
		while (line != null) {
			count++;
			line = reader.readLine();
		}
		reader.close();
		return count;
	}
	
	private void buildSimMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		while(line != null) {
		//	System.out.println("Splitting line " + line );
			String[] toks = line.split("\\s");
			//System.out.println("Found " + toks.length + " tokens");
			int pos = Integer.parseInt(toks[1]);
			simVariantMap.put(pos, 1);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private void buildTrueMap(File file) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		while(line != null) {
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[1]);
			trueVariantMap.put(pos, 1);
			line = reader.readLine();
		}
		reader.close();
	}
	
	public void performOperation() {
		FileBuffer simVariants = getInputBufferForClass(VCFFile.class);
		FileBuffer trueVariants = getInputBufferForClass(TextBuffer.class);
		
		FileBuffer reportBuffer = outputBuffers.get(0);
		
		try {

			BufferedWriter report = new BufferedWriter(new FileWriter(reportBuffer.getFile()));
			int trueTotalCount = countLines(trueVariants.getFile());
			int simTotalCount = countLines(simVariants.getFile());
			
			report.write("# Simulation validation report : \n");
			report.write("true.variants.file=" + trueVariants.getFile().getAbsolutePath() + "\n");
			report.write("sim.variants.file=" + simVariants.getFile().getAbsolutePath() + "\n");
			report.write("true.variants.total=" + trueTotalCount + "\n");
			report.write("sim.variants.total=" + simTotalCount + "\n");
			
			buildSimMap(simVariants.getFile());
			buildTrueMap(trueVariants.getFile());
			
			//Count number of variants found that are actually true variants
			int simsInTrue = 0;
			for(Integer simPos : simVariantMap.keySet()) {
				boolean truth = trueVariantMap.get(simPos) != null;
				if (truth)
					simsInTrue++;
			}
			
			report.write("true.variants.found=" + simsInTrue + "\n");
			
			int trueVariantsMissed = trueTotalCount - simsInTrue;
			report.write("false.negatives=" + trueVariantsMissed + "\n");
			
			int falsePositives = simTotalCount - simsInTrue;
			report.write("false.positives=" + falsePositives + "\n");
			
			
			report.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
