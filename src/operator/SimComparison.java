package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[1]);
			simVariantMap.put(pos, 1);
			line = reader.readLine();
		}
		reader.close();
	}
	
	private String inspectSim(File file, List<Integer> positions) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		Collections.sort(positions);
		
		//skip initial comments, etc
		while(line != null && line.startsWith("#"))
			line = reader.readLine();
		
		StringBuffer msg = new StringBuffer();
		
		while(line != null) {
			String[] toks = line.split("\\s");
			int pos = Integer.parseInt(toks[1]);
			if (Collections.binarySearch(positions, pos) >= 0) {
				msg.append("false.pos.line=\"" + line + " \" \n");
			}
			
			line = reader.readLine();
		}
		reader.close();
		return msg.toString();
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
			
			
			buildTrueMap(trueVariants.getFile());
			
			buildSimMap(simVariants.getFile());
			
			
			//Count number of variants found that are actually true variants
			int simsInTrue = 0;
			List<Integer> falsePositivesList = new ArrayList<Integer>();
			for(Integer simPos : simVariantMap.keySet()) {
				boolean truth = trueVariantMap.get(simPos) != null;
				if (truth)
					simsInTrue++;
				else {
					//This was a snp found in the simulates set, but NOT in the true set, so it's a false negative
					falsePositivesList.add(simPos);
				}
			}
			

			
			
			report.write("true.variants.found=" + simsInTrue + "\n");
			
			int trueVariantsMissed = trueTotalCount - simsInTrue;
			report.write("false.negatives=" + trueVariantsMissed + "\n");
			
			int falsePositives = simTotalCount - simsInTrue;
			report.write("false.positives=" + falsePositives + "\n");
			
			String fpSummary = inspectSim(simVariants.getFile(), falsePositivesList);
			report.write(fpSummary + "\n");
			
			
			report.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
}
