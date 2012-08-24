package operator;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.WranglerStatusWriter;

/**
 * Creates a new WranglerStatusWriter and registers it as a PipelineListener, it will
 * then log events in a JobWrangler-friendly way
 * @author brendan
 *
 */
public class StatusLogger extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
		Pipeline ppl = this.getPipelineOwner();
		WranglerStatusWriter writer = new WranglerStatusWriter();
		ppl.addListener(writer);
	}

	@Override
	public void initialize(NodeList children) {
		//No initialization needed
	}

}
