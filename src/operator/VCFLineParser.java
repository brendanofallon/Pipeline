package operator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * This class provides a uniform interface for extracting values from a single line of a vcf file.
 * It uses a differed-implementation strategy so we dont have to store too much info 
 * @author brendan
 *
 */
public class VCFLineParser {

		private File file;
		private BufferedReader reader;
		private int currentLineNumber = -1;
		private String currentLine = null;
		private String[] lineToks = null;
		
		public VCFLineParser(File file) throws IOException {
			this.file = file;
			this.reader = new BufferedReader(new FileReader(file));
			currentLine = reader.readLine();
			readHeader();
		}

		private void readHeader() throws IOException {
			while (currentLine != null && currentLine.startsWith("#")) {
				advanceLine();
			}
		}
		
		public boolean isPassing() {
			return currentLine.contains("PASS");
		}
		
		/**
		 * Read one more line of input, returns false if line cannot be read
		 * @return
		 * @throws IOException
		 */
		public boolean advanceLine() throws IOException {
			currentLine = reader.readLine();
			if (currentLine == null)
				lineToks = null;
			else
				lineToks = currentLine.split("\\t");

			currentLineNumber++;
			return currentLine != null;
		}
		
		public String getContig() {
			if (lineToks != null) {
				return lineToks[0];
			}
			else
				return null;
		}
		
		/**
		 * Return position item for current line
		 * @return
		 */
		public int getPosition() {
			if (lineToks != null) {
				return Integer.parseInt(lineToks[1]);
			}
			else
				return -1;
		}
		
		public Double getQuality() {
			if (lineToks != null) {
				return Double.parseDouble(lineToks[5]);
			}
			else
				return -1.0;
		}
		
		public char getRef() {
			if (lineToks != null) {
				return lineToks[3].charAt(0);
			}
			else
				return '?';
		}
		
		public char getAlt() {
			if (lineToks != null) {
				return lineToks[4].charAt(0);
			}
			else
				return '?';
		}
		
		public int getLineNumber() {
			return currentLineNumber;
		}
		
		/**
		 * 
		 */
		public boolean isHetero() {
			if (lineToks != null) {
				String[] fields = lineToks[9].split(":");
				//Right now we assume genotype is in FIRST format-field element, this may not always be true 
				if (fields[0].length() != 3) {
					throw new IllegalStateException("Wrong number of characters in string for is hetero... (got " + fields[0].length() + ", but should be 3)");
				}
				
				if (fields[0].charAt(1) == '/' || fields[0].charAt(1) == '|') {
					if (fields[0].charAt(0) != fields[0].charAt(2))
						 return true;
					else
						return false;
						
				}
				else {
					throw new IllegalStateException("Genotype separator char does not seem to be normal (found " + fields[0].charAt(1) + ")");
				}
				
			}
			else
				return false;
		}
		
		public boolean isHomo() {
			return ! isHetero();
		}

		public String getCurrentLine() {
			return currentLine;
		}
		
//		public static void main(String[] args) {
//			File file = new File("/media/DATA/exome_compare/ex1.cap.pass.vcf");
//			try {
//				VCFLineParser vParser = new VCFLineParser(file);
//				for(int i=0; i<10;i++) {
//					System.out.println(vParser.getLineNumber() + " : " + vParser.getContig() + "\t" + vParser.getPosition() + "\t" + vParser.getQuality() + "\t het: " + vParser.isHetero());
//					vParser.advanceLine();
//				}
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			
//		}
}
