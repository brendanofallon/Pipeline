package operator.variant;

import gene.Gene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;

import ncbi.GeneInfoDB;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.GeneList;
import buffer.variant.GenePool;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class CompoundHetFinder extends Operator {

	private VariantPool kidPool = null;
	private VariantPool parent1Pool = null;
	private VariantPool parent2Pool = null;
	
	GeneList genes = null;
	
	
	//User to filter input variant pools for interesting variants only
	private static VariantFilter nonSynFilter = new VariantFilter() {
		public boolean passes(VariantRec var) {
			String varType = var.getAnnotation(VariantRec.VARIANT_TYPE);
			if (varType != null) {
				if (varType.contains("splic"))
					return true;
				if (varType.contains("intronic"))
					return false;
				if (varType.contains("intergenic"))
					return false;
			}
			
			String exonFunc = var.getAnnotation(VariantRec.EXON_FUNCTION);
			if (exonFunc != null) {
				if (exonFunc.contains("nonsynon"))
					return true;
				if (exonFunc.contains("delet")) {
					return true;
				}
				if (exonFunc.contains("insert")) {
					return true;
				}
				if (exonFunc.contains("stopgain")) {
					return true;
				}
				if (exonFunc.contains("stoploss")) {
					return true;
				}
			}
			
			return false;
		}
	};
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		
		logger.info("CompoundHetFinder : Assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent 1 variants \n " + parent2Pool.getObjectLabel() + " contains parent 2 variants");

		
		List<CompoundHetHit> hits = computeCompoundHets(kidPool, parent1Pool, parent2Pool, genes);
			
		Collections.sort(hits, new HitRankComparator());
		
		int count = 0;
		for(CompoundHetHit hit : hits) {
			Gene g = hit.gene;
			Double score = g.getProperty(Gene.GENE_RELEVANCE);
			if (score != null && score > 0) {
				count++;
				
			}
		}
		
		if (count == 0) {
			System.out.println("No scores found in any compound het gene");
		}
		
		for(CompoundHetHit hit : hits) {
			Gene g = hit.gene;
			if (g.getProperty(Gene.GENE_RELEVANCE) != null && g.getProperty(Gene.GENE_RELEVANCE)>0) {
				System.out.print("\n\nGene : " + g.getName() + " score: " + g.getProperty(Gene.GENE_RELEVANCE) + "\n");
				System.out.println("\t Variant 1:\t" + hit.kidVar1.getAnnotation(VariantRec.PDOT) + "\t (" + hit.kidVar1.getAnnotation(VariantRec.NM_NUMBER) + ":" + hit.kidVar1.getAnnotation(VariantRec.CDOT) + "   " + hit.kidVar1.getContig() + ":" + hit.kidVar1.getStart() + ")");
				System.out.println("\t Variant 2:\t" + hit.kidVar2.getAnnotation(VariantRec.PDOT) + "\t (" + hit.kidVar2.getAnnotation(VariantRec.NM_NUMBER) + ":" + hit.kidVar2.getAnnotation(VariantRec.CDOT) + "   " + hit.kidVar2.getContig() + ":" + hit.kidVar2.getStart() + ")");
				System.out.println("\t Disease:\t" + g.getAnnotation(Gene.DBNSFP_DISEASEDESC) );
				System.out.println("\t OMIM #s:\t" + g.getAnnotation(Gene.DBNSFP_MIMDISEASE) );
				System.out.println("\t    HGMD:\t" + g.getAnnotation(Gene.HGMD_INFO) );
				System.out.println("\t Summary:\t" + g.getAnnotation(Gene.SUMMARY) );
				System.out.println("\t  Pubmed:\t" + g.getAnnotation(Gene.PUBMED_HIT) );
			}
			
		}		
	}

	/**
	 * Static method to actually perform compound het finding. This method is also used in varUtils.compoundHet
	 * @param kidVars
	 * @param parent1Vars
	 * @param parent2Vars
	 * @param outputStream
	 */
	public static List<CompoundHetHit> computeCompoundHets(VariantPool kidVars,
			VariantPool parent1Vars, 
			VariantPool parent2Vars,
			GeneList genes) {


		GeneInfoDB geneDB = GeneInfoDB.getDB();
		
		List<CompoundHetHit> hits = new ArrayList<CompoundHetHit>();
		
		GenePool kidGenes = new GenePool(kidVars.filterPool( nonSynFilter ));
		GenePool par1Genes = new GenePool(parent1Vars.filterPool( nonSynFilter ));
		GenePool par2Genes = new GenePool(parent2Vars.filterPool( nonSynFilter ));

		for(String gene : kidGenes.getGenes()) {
			List<VariantRec> kidList = kidGenes.getVariantsForGene(gene);
			List<VariantRec> par1List = par1Genes.getVariantsForGene(gene);
			List<VariantRec> par2List = par2Genes.getVariantsForGene(gene);

			CompoundHetHit hit = hasCompoundHet(kidList, par1List, par2List);

			if (hit != null) {
				hit.gene = genes.getGeneByName( gene );
				if ( hit.gene == null) {
					System.err.println("Hmm, could not find gene for name : " + gene);
					String synonym = geneDB.symbolForSynonym(gene);
					if (synonym != null) {
						System.out.println("Found synonym for gene :" + gene + " -> " + synonym);
						hit.gene = genes.getGeneByName(synonym);
					}
					else {
						System.out.println("No synonym for gene :" + gene + " :(");
					}
				}
				else {
					hits.add(hit);
				}
				
			}
		}
		
		return hits;
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
	private static CompoundHetHit hasCompoundHet(List<VariantRec> kidList,
										  List<VariantRec> par1List, 
										  List<VariantRec> par2List) {
		
		if (kidList == null || kidList.size() < 2 || kidList.size()>8) //Need at least two hets in kid list
			return null;
		if (par1List == null || par1List.size()==0)
			return null;
		if (par2List == null || par2List.size()==0)
			return null;
		
		boolean hasPar1Het = false; //True if any kid var is het and also het in par 1 and absent par2
		boolean hasPar2Het = false; 
		
		VariantRec kidHit1 = null;
		VariantRec kidHit2 = null;
		
		for(VariantRec rec : kidList) {
			if (rec.isHetero()) {
				boolean par1Het = isHetero(rec.getStart(), par1List);
				boolean par2Contains = contains(rec.getStart(), par2List);
				
				boolean par2Het = isHetero(rec.getStart(), par2List);
				boolean par1Contains = contains(rec.getStart(), par1List);
				
				if ( (!hasPar1Het) && (par1Het && (!par2Contains))) {
					kidHit1 = rec;
				}
				
				if ( (!hasPar2Het) && (par2Het && (!par1Contains))) {
					kidHit2 = rec;
				}
				
				hasPar1Het = hasPar1Het || (par1Het && (!par2Contains));
				hasPar2Het = hasPar2Het || (par2Het && (!par1Contains)); 

				if (hasPar1Het && hasPar2Het) {
					CompoundHetHit hit = new CompoundHetHit();
					hit.kidVar1 = kidHit1;
					hit.kidVar2 = kidHit2;
					return hit;
				}
			}
			
		}
		
		return null;
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
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());

				if (obj instanceof GeneList) {
					genes = (GeneList)obj;
				}
				
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
							}
					}
				}

			}
		}
		
		if (kidPool == null)
			throw new IllegalArgumentException("Kid Variant pool not specified");
		if (parent1Pool == null)
			throw new IllegalArgumentException("Parent 1 Variant pool not specified");
		if (parent2Pool == null)
			throw new IllegalArgumentException("Parent 2 Variant pool not specified");


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
	
	public static class CompoundHetHit {
		Gene gene = null;
		VariantRec kidVar1 = null;
		VariantRec kidVar2 = null;
	}
	
	class HitRankComparator implements Comparator<CompoundHetHit> {

		@Override
		public int compare(CompoundHetHit o1, CompoundHetHit o2) {
			Gene g0 = o1.gene;
			Gene g1 = o2.gene;
			Double s0 = g0.getProperty(Gene.GENE_RELEVANCE);
			Double s1 = g1.getProperty(Gene.GENE_RELEVANCE);
			
			if (s0 == null) {
				System.err.println("Could not compute gene relevance score for gene " + g0.getName());
				return 0;
			}
			
			if (s1 == null) {
				System.err.println("Could not compute gene relevance score for gene " + g1.getName());
				return 0;
			}
				
			
			return s0 < s1 ? 1 : -1;
			
		}
		
	}
}
