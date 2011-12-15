package util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class QualityHistogram {

	public static void main(String[] args) {
		
		if (args.length==0) {
			System.out.println("Please enter the name of the vcf file to parse");
			System.exit(0);
		}
		
		double min = 0;
		double max = 1000;
	
		try {
			BufferedReader reader = new BufferedReader(new FileReader(args[0]));
			Histogram histo = new Histogram(min, max, 100);
			String line = reader.readLine();
			while(line != null && line.startsWith("#")) {
				line = reader.readLine();
			}
			
			while (line != null) {
				String[] toks = line.split("\\s");
				//String contig = toks[0];
				//int pos = Integer.parseInt(toks[1]);
				double qual = Double.parseDouble(toks[5]);
				histo.addValue(qual);
				line = reader.readLine();
			}
			System.out.println(histo);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
