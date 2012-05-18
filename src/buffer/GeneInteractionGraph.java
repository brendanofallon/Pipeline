package buffer;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import math.graph.Graph;
import math.graph.GraphFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

/**
 * A weighted graph of genes, where the weights connecting genes represent interaction intensities / strengths
 * @author brendan
 *
 */
public class GeneInteractionGraph extends PipelineObject {

	protected Graph graph = null;
	protected Map<String, String> props = new HashMap<String, String>(); 
	protected FileBuffer source = null;

	public Graph getGraph() throws IOException {
		if (graph == null) {
			graph = GraphFactory.constructGraphFromFile(source.getFile()); 
		}
		return graph;
	}
	
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof FileBuffer) {
					source = (FileBuffer)obj;
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
	}
	
	
	@Override
	public void setAttribute(String key, String value) {
		props.put(key, value);
	}

	@Override
	public String getAttribute(String key) {
		return props.get(key);
	}

	@Override
	public Collection<String> getAttributeKeys() {
		return props.keySet();
	}

}
