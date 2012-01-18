package operator;

import java.io.IOException;

import org.w3c.dom.NodeList;

import util.VCFLineParser;

import buffer.VCFFile;

/**
 * This thing takes as input a VCF file and emits as output a BED file containing regions that meet 
 * the following criteria:
 * 
 * 1. Greater than X% (X=90%) of the variants in the region are predicted to by homozygous
 * 2. The region is bigger than Y-kb (y=10kb) in size
 * 3. There are a minimum of Z variants in the region (z=3?) 
 * 
 * Thus, we attempt to identify fairly large regions of high homozygosity
 * 
 * @author brendan
 *
 */
public class HomoRegionFinder extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		VCFFile vcf = (VCFFile) getInputBufferForClass(VCFFile.class);
		if (vcf == null)
			throw new OperationFailedException("Input VCF file not found", this);

		VCFLineParser vcfReader = null;
		try {
			vcfReader = new VCFLineParser(vcf.getFile());
		
		
			boolean inRegion = false;
			String currentContig = "?";
			int varCount = 0;
			int homos = 0;
			
			int heteros = 0;
			
			
			while(vcfReader.advanceLine()) {
				
			}
			
		} catch (IOException e) {
			throw new OperationFailedException("Error reading VCF file : " + e.getMessage(), this);

		}
	}


	/**
	 * A single region of high homozygosity
	 * @author brendan
	 *
	 */
	class Region {
		String contig;
		int start;
		int end;
		double percentHomo;
		int variantCount;
		
		public int getLength() {
			return end - start;
		}
	}
}
