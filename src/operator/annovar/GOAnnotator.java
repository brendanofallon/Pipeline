package operator.annovar;

import java.util.List;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;
import operator.OperationFailedException;

public class GOAnnotator extends Annotator {

	GOTerms goTerms = null;
	
	/**
	 * Set the variant pool to be annotated
	 * @param pool
	 */
	public void setVariantPool(VariantPool pool) {
		this.variants = pool;
	}

	@Override
	public void annotateVariant(VariantRec rec) {
		if (goTerms == null)
			goTerms = new GOTerms();
		
		String gene = rec.getAnnotation(VariantRec.GENE_NAME);
		if (gene == null) {
			return;
		}
		List<String> functions = goTerms.getFunctionsForGene(gene);
		String funcStr = combineStrings(functions);
		rec.addAnnotation(VariantRec.GO_FUNCTION, funcStr);
		
		List<String> procs = goTerms.getProcessesForGene(gene);
		String procsStr = combineStrings(procs);
		rec.addAnnotation(VariantRec.GO_PROCESS, procsStr);
		
		List<String> comps = goTerms.getComponentsForGene(gene);
		String compsStr = combineStrings(comps);
		rec.addAnnotation(VariantRec.GO_COMPONENT, compsStr);
	}

	private static String combineStrings(List<String> strs) {
		if (strs.size() == 0) {
			return "-";
		}
		else {
			StringBuilder strB = new StringBuilder();
			for(int i=0; i<strs.size()-1; i++)
				strB.append(strs.get(i) + ", ");
			strB.append(strs.get(strs.size()-1));
			return strB.toString();
		}
	}



}