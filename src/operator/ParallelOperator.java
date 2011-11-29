package operator;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import javax.swing.SwingWorker;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import util.ElapsedTimeFormatter;
import buffer.FileBuffer;

/**
 * This operator wraps a bunch of other operators and runs them in parallel on separate threads. All bets are off
 * if the operators write to the same file 
 * @author brendan
 *
 */
public class ParallelOperator extends Operator {

	protected List<Operator> operators = new ArrayList<Operator>();
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		
		Date now = new Date();
		long beginMillis = System.currentTimeMillis();
		logger.info("[" + now + "] ParallelOperator is launching " + operators.size() + " simultaneous jobs");
		
		List<OpWrapper> wraps = new ArrayList<OpWrapper>();
		
		for(Operator op : operators)  {
			OpWrapper opw = new OpWrapper(op);
			wraps.add(opw);
			opw.execute();
		}
		
		//Wait for all operators to finish...
		for(OpWrapper opw : wraps)  {
			try {
				opw.get();
			} catch (InterruptedException e) {
				throw new OperationFailedException("Operator " + opw.op.getObjectLabel() + " was interrupted during parallel execution", opw.op);
			} catch (ExecutionException e) {
				throw new OperationFailedException("Operator " + opw.op.getObjectLabel() + " was interrupted during parallel execution", opw.op);
			}
		}
		
		now = new Date();
		long endMillis = System.currentTimeMillis();
		long elapsedMillis = endMillis - beginMillis;
		logger.info("[ " + now + "] Parallel operator: " + getObjectLabel() + " has completed. Time taken = " + elapsedMillis + " ms ( " + ElapsedTimeFormatter.getElapsedTime(beginMillis, endMillis) + " )");		

	}
	
	/**
	 * Add the given operator to the list of those operators that will be executed
	 * when performOperation is called
	 * @param op
	 */
	protected void addOperator(Operator op) {
		operators.add(op);
	}

	@Override
	public void initialize(NodeList children) {
		
		for(int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(node.getNodeName());
				if (obj instanceof Operator) {
					addOperator( (Operator)obj );
				}
				else {
					throw new IllegalArgumentException("Found non-FileBuffer object in input list for Operator " + getObjectLabel());
				}
			}
		}
	}

	/**
	 * Thin wrapper for operators so we can run them in separate threads
	 * @author brendan
	 *
	 */
	class OpWrapper extends SwingWorker {
		
		private Operator op;
		
		public OpWrapper(Operator op) {
			this.op = op;
		}
		
		@Override
		protected Object doInBackground() throws Exception {
			
			op.performOperation();
			return op;
		}
		
	}
}
