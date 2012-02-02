package operator.annovar;

import java.util.List;

import buffer.variant.VariantRec;
import operator.OperationFailedException;

public class GOAnnotator extends Annotator {

	GOTerms goTerms = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("Variant pool not initialized", this);

		if (goTerms == null)
			goTerms = new GOTerms();
		
		for(String contig : variants.getContigs()) {
			List<VariantRec> vars = variants.getVariantsForContig(contig);
			for(VariantRec rec : vars) {
				String gene = rec.getAnnotation(VariantRec.GENE_NAME);
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
			
		}
		
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
