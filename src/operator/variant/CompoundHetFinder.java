package operator.variant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Logger;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.CSVFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;
import buffer.variant.VariantPool;
import buffer.variant.GenePool;
import buffer.variant.VariantFilter;
import buffer.variant.VariantRec;

import operator.IOOperator;
import operator.OperationFailedException;
import pipeline.Pipeline;
import pipeline.PipelineObject;

public class CompoundHetFinder extends IOOperator {

	private VariantPool kidPool = null;
	private VariantPool parent1Pool = null;
	private VariantPool parent2Pool = null;
	
	//User to filter input variant pools for nonsynonymous variants only
	private static VariantFilter nonSynFilter = new NonSynFilter();
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		
		logger.info("CompoundHetFinder : Assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent 1 variants \n " + parent2Pool.getObjectLabel() + " contains parent 2 variants");
	
		File outputFile = outputBuffers.get(0).getFile();
		try {
			PrintStream outputStream = new PrintStream(new FileOutputStream(outputFile));
			
			computeCompoundHets(kidPool, parent1Pool, parent2Pool, outputStream);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new OperationFailedException(e.getMessage(), this);
		}
		
		
		
	}

	/**
	 * Static method to actually perform compound het finding. This method is also used in varUtils.compoundHet
	 * @param kidVars
	 * @param parent1Vars
	 * @param parent2Vars
	 * @param outputStream
	 */
	public static void computeCompoundHets(VariantPool kidVars,
			VariantPool parent1Vars, 
			VariantPool parent2Vars,
			PrintStream outputStream) {


		GenePool kidGenes = new GenePool(kidVars.filterPool( nonSynFilter ));
		GenePool par1Genes = new GenePool(parent1Vars.filterPool( nonSynFilter ));
		GenePool par2Genes = new GenePool(parent2Vars.filterPool( nonSynFilter ));

		for(String gene : kidGenes.getGenes()) {
			List<VariantRec> kidList = kidGenes.getVariantsForGene(gene);
			List<VariantRec> par1List = par1Genes.getVariantsForGene(gene);
			List<VariantRec> par2List = par2Genes.getVariantsForGene(gene);

			boolean isCompoundHet = hasCompoundHet(kidList, par1List, par2List);

			if (isCompoundHet) {
				outputStream.println("Gene : " + gene);
				outputStream.println("Kid variants: ");
				for(VariantRec rec : kidList) {
					outputStream.println( rec.toSimpleString() + "\t" + rec.getPropertyOrAnnotation(VariantRec.VARIANT_TYPE) + "\t" + rec.getPropertyOrAnnotation(VariantRec.EXON_FUNCTION) + "\t" + rec.getPropertyOrAnnotation(VariantRec.POP_FREQUENCY) );
				}

			}
		}
	}
	
	
	/**
	 * Returns true if a) no list is empty (or null)
	 * b) the kid list contains at least one hetero var that is also hetero in par1, but absent from par2
	 * c) the kid list contains at least one hetero var that is also hetero in par2, but absent from par1
	 * 
	 * @param kidList
	 * @param par1List
	 * @param par2List
	 * @return
	 */
	private static boolean hasCompoundHet(List<VariantRec> kidList,
										  List<VariantRec> par1List, 
										  List<VariantRec> par2List) {
		
		if (kidList == null || kidList.size() < 2) //Need at least two hets in kid list
			return false;
		if (par1List == null || par1List.size()==0)
			return false;
		if (par2List == null || par2List.size()==0)
			return false;
		
		boolean hasPar1Het = false; //True if any kid var is het and also het in par 1 and absent par2
		boolean hasPar2Het = false; 
		
		for(VariantRec rec : kidList) {
			if (rec.isHetero()) {
				boolean par1Het = isHetero(rec.getStart(), par1List);
				boolean par2Contains = contains(rec.getStart(), par2List);
				
				boolean par2Het = isHetero(rec.getStart(), par2List);
				boolean par1Contains = contains(rec.getStart(), par1List);
				
				hasPar1Het = hasPar1Het || (par1Het && (!par2Contains));
				hasPar2Het = hasPar2Het || (par2Het && (!par1Contains)); 

				if (hasPar1Het && hasPar2Het) {
					return true;
				}
			}
			
		}
		
		return false;
	}

	/**
	 * Returns true if there is a variant at the given start position
	 * is the list AND the variant is a heterozygote 
	 * @param pos
	 * @param list
	 * @return
	 */
	public static boolean isHetero(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos && rec.isHetero()) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Returns true if the given var list contains a variant that starts at the
	 * given position
	 * @param pos
	 * @param list
	 * @return
	 */
	public static boolean contains(int pos, List<VariantRec> list) {
		for(VariantRec rec : list) {
			if (rec.getStart()==pos) {
				return true;
			}
		}
		return false;
	}
	
	
	public void initialize(NodeList children) {	
		Element inputList = getChildForLabel("input", children);
		Element outputList = getChildForLabel("output", children);
		
		if (inputList != null) {
			NodeList inputChildren = inputList.getChildNodes();
			for(int i=0; i<inputChildren.getLength(); i++) {
				Node iChild = inputChildren.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof VariantPool) {
						if (kidPool == null) {
							kidPool = (VariantPool) obj;
						}
						else {
							if (parent1Pool == null) {
								parent1Pool = (VariantPool) obj;
							}
							else {
								if (parent2Pool == null) {
									parent2Pool = (VariantPool) obj;
								}
								else {
									throw new IllegalArgumentException("Found too many input variant pools (should be exactly 3)");
								}
							}
						}
					}
					else {
						throw new IllegalArgumentException("Found a non-variant pool input to CompoundHetFinder : " + obj.getObjectLabel());
					}
					
					
				}
			}
		}
		
		if (outputList != null) {
			NodeList outputChilden = outputList.getChildNodes();
			for(int i=0; i<outputChilden.getLength(); i++) {
				Node iChild = outputChilden.item(i);
				if (iChild.getNodeType() == Node.ELEMENT_NODE) {
					PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
					if (obj instanceof FileBuffer) {
						addOutputBuffer( (FileBuffer)obj );
					}
					else {
						throw new IllegalArgumentException("Found non-FileBuffer object in output list for Operator " + getObjectLabel());
					}
				}
			}
		}
		
		if ( requiresReference() ) {
			ReferenceFile ref = (ReferenceFile) getInputBufferForClass(ReferenceFile.class);
			if (ref == null) {
				throw new IllegalArgumentException("Operator " + getObjectLabel() + " requires reference file, but none were found");
			}
		}
	}
	
	/**
	 * Filtering class to remove all synonymous variants
	 * @author brendan
	 *
	 */
	public static class NonSynFilter implements VariantFilter {
		
		public boolean passes(VariantRec rec) {
			String func = rec.getAnnotation(VariantRec.EXON_FUNCTION);
			if (func == null)
				return true;
			if ((!func.contains("nonsynonymous")) && func.contains("synonymous")) {
				return false;
			}
			return true;
		}
		
	}
	
}
