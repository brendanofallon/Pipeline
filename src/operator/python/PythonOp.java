package operator.python;

import org.w3c.dom.NodeList;

import operator.CommandOperator;
import operator.OperationFailedException;

/**
 * Base class for Operators that execute python commands / scripts
 * @author brendan
 *
 */
public abstract class PythonOp extends CommandOperator {

	public static final String PYTHON_PATH = "python.path";
	protected String pythonPath = "python";
	
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pythonPathProp = properties.get(PYTHON_PATH);
		if (pythonPathProp != null)
			pythonPath = pythonPathProp;
	}
}
