package operator.variant;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import math.graph.Graph;
import math.graph.GraphNode;
import math.graph.GraphUtils;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.FileBuffer;
import buffer.GeneInteractionGraph;
import buffer.variant.VariantRec;
import operator.annovar.Annotator;
import pipeline.PipelineObject;

public class InteractionRanker extends Annotator {

	protected GeneInteractionGraph geneGraph = null;
	public static final String sourceGene = "source";
	protected List<String> sourceGenes = null;
	
	
	@Override
	public void annotateVariant(VariantRec var)  {
		
		Graph g;
		try {
			g = geneGraph.getGraph();
		} catch (IOException e) {
			throw new IllegalArgumentException("Could not read graph from file : " + e.getMessage());
		}
		
		//Parse source gene labels AND compute the shortest path trees for all of them
		if (sourceGenes == null) {
			String sourceProp = this.getAttribute(sourceGene);
			if (sourceProp == null)
				throw new IllegalArgumentException("No source genes specified");
			String[] sourceArray = sourceProp.split(",");
			sourceGenes = new ArrayList<String>();
			for(int i=0; i<sourceArray.length; i++) {
				sourceGenes.add(sourceArray[i].trim());
			}
			
			
			for(String geneLabel : sourceGenes) {
				System.out.println("Computing paths for gene : " + geneLabel);
				GraphUtils.computeShortestPaths(g, geneLabel);	
			}
			
		}
		
		//If no gene name then forget it
		String geneName = var.getAnnotation(VariantRec.GENE_NAME);
		if (geneName == null)
			return;

		double shortestPath = Double.POSITIVE_INFINITY;
		for(String geneLabel : sourceGenes) {
			GraphNode varNode = g.getNodeForLabel(geneName);
			if (varNode != null) {
				Double dist = varNode.getAnnotation("distance.to." + geneLabel);
				if (dist != null && dist < shortestPath) {
					shortestPath = dist;
				}
			}
		}

		if (shortestPath < Double.POSITIVE_INFINITY)
			var.addProperty(VariantRec.INTERACTION_SCORE, 1.0/(shortestPath+0.001));
					

	}

	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		//Search for a gene interaction graph
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof GeneInteractionGraph) {
					geneGraph = (GeneInteractionGraph)obj;
				}
			}
		}
		if (geneGraph == null) {
			throw new IllegalArgumentException("No GeneInteractionGraph specified");
		}
	}
}
