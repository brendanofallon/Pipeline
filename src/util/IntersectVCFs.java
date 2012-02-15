package util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import buffer.VCFFile;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * A small utility class that emits the intersection (as well as unique sites) of two vcf files
 * @author brendan
 *
 */
public class IntersectVCFs {

	private VCFFile fileA;
	private VCFFile fileB;
	
	private VariantPool intersection = null;
	
	public IntersectVCFs(File fileA, File fileB) {
		this.fileA = new VCFFile(fileA);
		this.fileB = new VCFFile(fileB);
	}
	
	public void performIntersection() {
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
		
		VariantPool variantsA = varReaderA.getVariantPool();
		VariantPool variantsB = varReaderB.getVariantPool();
		
		intersection = (VariantPool) variantsA.intersect(variantsB);
	}
	
	public VariantPool getIntersection() {
		return intersection;
	}
	
	
	
	public static void main(String[] args) {
		
		if (args.length < 3) {
			System.out.println("Usage : java -jar intersectVCFs.jar fileA.vcf fileB.vcf outputPrefix");
			return;
		}
		

		File fileA = new File(args[0]);
		File fileB = new File(args[1]);
		String outputPrefix = args[2];
		
		IntersectVCFs intersector = new IntersectVCFs(fileA, fileB);
		
		intersector.performIntersection();	
	
		File intersectionFile = new File(outputPrefix + "_intersection.csv");
	
		try {
			PrintStream stream = new PrintStream(new FileOutputStream(intersectionFile));
			VariantPool intersection = intersector.getIntersection();
			List<String> keys = new ArrayList<String>();
			keys.add(VariantRec.altB);
			keys.add(VariantRec.zygosityB);
			intersection.listAll(stream, keys);
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}
	
	
	class ReadVariants extends SwingWorker {
		
		private final VCFFile inputFile;
		private VariantPool variants = null;
		private boolean done = false;
		
		public ReadVariants(VCFFile inputFile) {
			this.inputFile = inputFile;
		}
		
		public VariantPool getVariantPool() {
			if (! isDone()) {
				throw new IllegalArgumentException("Not done yet!");
			}
			
			return variants;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			variants = new VariantPool(inputFile);
			done = true;
			return null;
		}
		
	}
	
}
