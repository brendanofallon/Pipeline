package operator.annovar;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import buffer.variant.VariantPool;
import buffer.variant.VariantRec;

import pipeline.PipelineObject;

/**
 * Base class for things that can take a variant pool and add an annotation of some sort to
 * the variants
 * @author brendan
 *
 */
public abstract class Annotator extends Operator {

	protected VariantPool variants = null;
	

	/**
	 * Compute or obtain an annotation for the given variant and add it to the list of
	 * annotations or properties stored for the variant
	 * @param var
	 */
	public abstract void annotateVariant(VariantRec var);
	
	public void performOperation() throws OperationFailedException {
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				annotateVariant(rec);
			}
		}
			
	}
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				PipelineObject obj = getObjectFromHandler(el.getNodeName());
				if (obj instanceof VariantPool) {
					variants = (VariantPool)obj;
				}

			}
		}
	}
}
