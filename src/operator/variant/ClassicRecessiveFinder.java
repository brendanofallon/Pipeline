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
import buffer.variant.VarFilterUtils;
import buffer.variant.VariantFilter;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

public class ClassicRecessiveFinder extends Operator {

	private VariantPool kidPool = null;
	private VariantPool parent1Pool = null;
	private VariantPool parent2Pool = null;
	
	GeneList genes = null;
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		
		logger.info("Classic Recessive Finder : Assuming " + kidPool.getObjectLabel() + " contains offspring variants \n " + parent1Pool.getObjectLabel() + " contains parent 1 variants \n " + parent2Pool.getObjectLabel() + " contains parent 2 variants");

		
		List<VariantRec> hits = findRecessiveHomos(kidPool, parent1Pool, parent2Pool, genes);
		
		int count = 0;
		for(VariantRec hit : hits) {
			Gene g = hit.getGene();
			Double score = g.getProperty(Gene.GENE_RELEVANCE);
			if (score != null && score > 0) {
				count++;
				
			}
		}
		
		if (count == 0) {
			System.out.println("No scores found in any classic recessive gene");
		}
		
		for(VariantRec hit : hits) {
			Gene g = hit.getGene();
			if (g.getProperty(Gene.GENE_RELEVANCE) != null && g.getProperty(Gene.GENE_RELEVANCE)>5) {
				System.out.print("\n\nGene : " + g.getName() + " score: " + g.getProperty(Gene.GENE_RELEVANCE) + "\n");
				System.out.println("\t Variant :\t" + hit.getAnnotation(VariantRec.EXON_FUNCTION) + "   " + hit.getAnnotation(VariantRec.PDOT) + "\t (" + hit.getAnnotation(VariantRec.NM_NUMBER) + ":" + hit.getAnnotation(VariantRec.CDOT) + "   " + hit.getContig() + ":" + hit.getStart() + ")");
				System.out.println("\t Disease:\t" + g.getAnnotation(Gene.DBNSFP_DISEASEDESC) );
				System.out.println("\t OMIM #s:\t" + g.getAnnotation(Gene.DBNSFP_MIMDISEASE) );
				System.out.println("\t    HGMD:\t" + g.getAnnotation(Gene.HGMD_INFO) );
				System.out.println("\t Summary:\t" + g.getAnnotation(Gene.SUMMARY) );
				System.out.println("\t  Pubmed:\t" + g.getAnnotation(Gene.PUBMED_HIT) );
			}
			
		}		
	}
	
	
	
	private static List<VariantRec> findRecessiveHomos(VariantPool kidVars,
			VariantPool p1Vars, VariantPool p2Vars, GeneList genes) {
		
		GeneInfoDB geneDB = GeneInfoDB.getDB();
		
		List<VariantRec> hits = new ArrayList<VariantRec>();
		
		VariantPool p1Homos = new VariantPool( p1Vars.filterPool( VarFilterUtils.getHomoFilter() ) );
		VariantPool p2Homos = new VariantPool( p2Vars.filterPool( VarFilterUtils.getHomoFilter() ) );
		
		VariantPool kidVarsFiltered = new VariantPool( kidVars.filterPool( nonSynFilter ) );
		
		kidVarsFiltered.removeVariants( p1Homos );
		kidVarsFiltered.removeVariants( p2Homos );
		
		kidVarsFiltered = new VariantPool( kidVarsFiltered.filterPool( VarFilterUtils.getHomoFilter() ) );

		
		//Filter out intergenic, etc. mutations...
		
		
		for(String contig : kidVarsFiltered.getContigs()) {
			for(VariantRec var : kidVarsFiltered.getVariantsForContig(contig)) {
				Gene g = var.getGene();
				String geneName = var.getAnnotation(VariantRec.GENE_NAME);
				if (g == null && (geneName != null)) {
					g = genes.getGeneByName( geneName );
					if ( g == null) {
						System.err.println("Hmm, could not find gene for name : " + geneName);
						String synonym = geneDB.symbolForSynonym(geneName);
						if (synonym != null) {
							System.out.println("Found synonym for gene :" + geneName + " -> " + synonym);
							g = genes.getGeneByName(synonym);
						}
						else {
							System.out.println("No synonym for gene :" + geneName + " :(");
						}
					}
					
				}
				
				if (g != null) {
					var.setGene(g);
					if (g.getProperty(Gene.GENE_RELEVANCE) != null && g.getProperty(Gene.GENE_RELEVANCE)>0) {
						hits.add(var);
					}
				}
			}
		}
		
		Collections.sort(hits, new GeneRelComparator());
		
		return hits;
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
	
	
	static class GeneRelComparator implements Comparator<VariantRec> {

		@Override
		public int compare(VariantRec v1, VariantRec v2) {
			Gene g0 = v1.getGene();
			Gene g1 = v2.getGene();
			
			if (g0 == null) {
				System.err.println("No gene found for variant at : " + v1.getContig() + ":" + v1.getStart());
				return 0;
			}
			
			if (g1 == null) {
				System.err.println("No gene found for variant at : " + v2.getContig() + ":" + v2.getStart());
				return 0;
			}
			
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
}
