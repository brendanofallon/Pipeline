package operator;

import java.util.Collection;

import ncbi.GeneInfoDB;

import org.w3c.dom.NodeList;

public class GeneRanker extends Operator {

	public static void main(String[] args) {
		GeneInfoDB geneInfo = GeneInfoDB.getDB();
		Collection<String> geneNames = geneInfo.getAllGenes();
		Graph geneGraph = 
		
	}

	@Override
	public void performOperation() throws OperationFailedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}

}
