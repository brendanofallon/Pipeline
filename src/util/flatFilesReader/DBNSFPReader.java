package util.flatFilesReader;

import java.io.File;
import java.io.IOException;

/**
 * Reads values from a dbNSFP database 
 * @author brendan
 *
 */
public class DBNSFPReader {

	public static final String defaultPath = System.getProperty("user.home") + "/resources/dbNSFP2.0";
	private FlatFilesReader reader;
	private String[] curToks = null;
	
	public DBNSFPReader() {
		reader = new FlatFilesReader(new File(defaultPath));
	}
	
	/**
	 * Read in the next line, whatever it may be
	 * @return
	 */
	public boolean advanceLine() {
		boolean hasNext = reader.advanceLine();
		if (hasNext) {
			curToks = reader.getCurrentLine().split("\t");
		}
		else {
			curToks = null;
		}
		return curToks != null;
	}
	
	public boolean advanceTo(String contig, int pos) throws IOException {
		if (reader.getCurrentContig() != null && reader.getCurrentContig().equals(contig) && pos < reader.getCurrentPos()) {
			//System.out.println("requested pos " + pos + " is earlier than readers current pos, " + reader.getCurrentPos() + ", skipping");
			return false;
		}
		String line = reader.getRow(contig, pos);
				
		if (line == null) {
			curToks = null;
			return false;
		}
		curToks = line.split("\t");
		return true;
	}
	
	/**
	 * Read information from the given contig, position, and alt allele. If no position
	 * is available at that site, returns false. 
	 * @param contig
	 * @param pos
	 * @param alt
	 * @return
	 * @throws IOException
	 */
	public boolean advanceTo(String contig, int pos, char alt) throws IOException {
		if (reader.getCurrentContig() != null && reader.getCurrentContig().equals(contig) && pos < reader.getCurrentPos()) {
			//System.out.println("requested pos " + pos + " is earlier than readers current pos, " + reader.getCurrentPos() + ", skipping");
			return false;
		}
		String line = reader.getRow(contig, pos, alt);
				
		if (line == null) {
			curToks = null;
			return false;
		}
		curToks = line.split("\t");
		return true;
	}

	public boolean hasValue(int col) {
		if (curToks == null)
			return false;
		if (curToks[col].equals("-") || curToks[col].equals("."))
			return false;
		return true;
	}
	
	public String getRef() {
		if (curToks == null)
			return null;
		return curToks[2];
	}
	
	public String getAlt() {
		if (curToks == null)
			return null;
		return curToks[3];
	}
	
	/**
	 * Returns NaN if no value can be parsed from column
	 * @param col
	 * @return
	 */
	public Double getValue(int col) {
		if (curToks == null)
			return null;
		
		try {
			Double val = Double.parseDouble(curToks[col]);
			return val;
		}
		catch (NumberFormatException nfe) {
			return Double.NaN;
		}
	}
	
	public int getCurrentPos() {
		return reader.getCurrentPos();
	}
	
	
	public String getString(int col) {
		if (curToks == null)
			return null;
		return curToks[col];
	}
	
	 public static final int CHR = 0;
	 public static final int POS = 1;
	 public static final int REF = 2;
	 public static final int ALT = 3;
	 public static final int AAREF = 4;
	 public static final int AAALT = 5;
	 //public static final int HG18POS = 6;
	 public static final int GENE = 7;
	 public static final int UNIPROT_ACC = 8;
	 public static final int SIFT = 21;
	 public static final int GERP = 31; 
	 public static final int LRT = 26;
	 public static final int PP = 22;
	 public static final int MT = 28;
	 public static final int PHYLOP = 32;
	 public static final int SIPHY = 34;
	 public static final int TKG = 38;
	 public static final int TKG_AF = 40;
	 public static final int TKG_EUR = 42;
	 public static final int TKG_AMR = 44;
	 public static final int TKG_ASN = 46;
	 public static final int ESP5400 = 48;
	 
	 /**
	      Uniprot_id	9 
	      Uniprot_aapos  10 
	      Interpro_domain	11
	      cds_strand    12
	      refcodon   13
	      SLR_test_statistic	14
	      codonpos   15
	      fold-degenerate 16 
	      Ancestral_allele 	17
	      Ensembl_geneid 18
	      Ensembl_transcriptid 19
	      aapos   20
	      SIFT_score 21
	      Polyphen2_HDIV_score 22
	      Polyphen2_HDIV_pred 23
	      Polyphen2_HVAR_score24
	      Polyphen2_HVAR_pred 25
	      LRT_score 26
	      LRT_pred 27
	      MutationTaster_score 28
	      MutationTaster_pred 29 
	      GERP++_NR 30
	      GERP++_RS 31
	      phyloP 32
	      29way_pi 33
	      29way_logOdds 34
	      LRT_Omega 34
	      UniSNP_ids 36
	      1000Gp1_AC 37
	      1000Gp1_AF 38
	      1000Gp1_AFR_AC 39
	      1000Gp1_AFR_AF 40
	      1000Gp1_EUR_AC 41
	      1000Gp1_EUR_AF 42
	      1000Gp1_AMR_AC 43
	      1000Gp1_AMR_AF 44
	      1000Gp1_ASN_AC 45
	      1000Gp1_ASN_AF 46
	      ESP5400_AA_AF 47
	      ESP5400_EA_AF 48
	      
	      **/
}
