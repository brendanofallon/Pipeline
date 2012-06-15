package operator.variant;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import operator.annovar.Annotator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.TextBuffer;
import buffer.variant.GenePool;
import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

/**
 * Computes the "go.score" annotation 
 * @author brendan
 *
 */
public class GOTermRanker extends Annotator {

	VariantPool inputVars = null;
	TextBuffer termsFile = null;
	Map<String, Integer> rankingMap = null;
	

	@Override
	public void annotateVariant(VariantRec var) {
		if (rankingMap == null) {
			try {
				buildRankingMap();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}


		double goScore = computeScore(var);
		var.addProperty(VariantRec.GO_SCORE, goScore);

	}
	
	private double computeScore(VariantRec var) {

		String goComponent = var.getAnnotation(VariantRec.GO_COMPONENT).toLowerCase();
		String goProcess = var.getAnnotation(VariantRec.GO_PROCESS).toLowerCase();
		String goFunction = var.getAnnotation(VariantRec.GO_FUNCTION).toLowerCase();
		
		double score = 0;
		
		StringBuilder hitStr = new StringBuilder();
		for(String term : rankingMap.keySet()) {
			
			if (goComponent != null && goComponent.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
			
			if (goProcess != null && goProcess.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
			
			if (goFunction != null && goFunction.contains(term)) {
				score += rankingMap.get(term);
				hitStr.append(term + "(" + score + "), ");
			}
			
		}
		
		if (score > 0)
			var.addAnnotation(VariantRec.GO_HITS, hitStr.toString());
		return score;
	}

	private void buildRankingMap() throws IOException {
		rankingMap = new HashMap<String, Integer>();
		BufferedReader reader = new BufferedReader(new FileReader(termsFile.getAbsolutePath()));
		String line = reader.readLine();
		while(line != null) {
			if (line.trim().length()==0 || line.startsWith("#")) {
				line = reader.readLine();
				continue;
			}
			
			String[] toks = line.split("\t");
			if (toks.length < 2) {
				System.err.println("Warning : could not parse GO term from this line: " + line);
			}
			else {
				Integer score = Integer.parseInt(toks[1].trim());
				rankingMap.put(toks[0].trim().toLowerCase(), score);
			}
			line = reader.readLine();
		}
	}

	public void initialize(NodeList children) {
		super.initialize(children);

		for(int i=0; i<children.getLength(); i++) {	
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof TextBuffer) {
					termsFile = (TextBuffer)obj;
				}
			}
		}
	}



}
