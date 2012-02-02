package util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import util.IntersectVCFs.ReadVariants;
import buffer.CSVFile;
import buffer.VCFFile;
import buffer.variant.AbstractVariantPool;
import buffer.variant.VariantRec;

public class SubtractVariants {
	
	private File fileA;
	private File fileB;
	
	private AbstractVariantPool remainder = null;
	
	public SubtractVariants(File fileA, File fileB) {
		this.fileA = fileA;
		this.fileB = fileB;
	}
	
	public void performSubtraction() {
		ReadVariants varReaderA = new ReadVariants(fileA);			
		ReadVariants varReaderB = new ReadVariants(fileB);
		
		varReaderA.execute();
		varReaderB.execute();
		
		boolean done = varReaderA.isDone() && varReaderB.isDone();
		while(! done) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			done = varReaderA.isDone() && varReaderB.isDone();
		}
		
		AbstractVariantPool variantsA = varReaderA.getVariantPool();
		AbstractVariantPool variantsB = varReaderB.getVariantPool();
		
		variantsA.removeVariants(variantsB);
		remainder = variantsA;
	}
	
	public AbstractVariantPool getRemainder() {
		return remainder;
	}
	
	
	
	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("Usage : java -jar subtractVars.jar fileA.vcf fileB.vcf outputPrefix");
			return;
		}
		

		File fileA = new File(args[0]);
		File fileB = new File(args[1]);
		String outputPrefix = args[2];
		
		SubtractVariants subtractor = new SubtractVariants(fileA, fileB);
		
		subtractor.performSubtraction();	
	
		File output = new File(outputPrefix + "_subtraction.csv");
	
		try {
			PrintStream stream = new PrintStream(new FileOutputStream(output));
			AbstractVariantPool intersection = subtractor.getRemainder();
			List<String> keys = new ArrayList<String>();
			intersection.listAll(stream, keys);	
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	class ReadVariants extends SwingWorker {
		
		private final File inputFile;
		private AbstractVariantPool variants = null;
		private boolean done = false;
		
		public ReadVariants(File inputFile) {
			this.inputFile = inputFile;
		}
		
		public AbstractVariantPool getVariantPool() {
			if (! isDone()) {
				throw new IllegalArgumentException("Not done yet!");
			}
			
			return variants;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			if (inputFile.getName().endsWith(".csv")) {
				variants = new AbstractVariantPool(new CSVFile(inputFile));	
			} else {
				if (inputFile.getName().endsWith(".vcf")) {
					variants = new AbstractVariantPool(new VCFFile(inputFile));	
				}
				else {
					throw new IllegalArgumentException("Unrecognized file suffix for input file: " + inputFile.getName());
				}
			}
			
			done = true;
			return null;
		}
		
	}
	
}
