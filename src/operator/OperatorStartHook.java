package operator;

/**
 * A hook that can be run before an Operator executes
 * 
 * @author quin
 *
 */
public interface OperatorStartHook extends OperatorHook {
	public void doHookStart() throws Exception;
}
