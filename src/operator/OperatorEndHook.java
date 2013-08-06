package operator;

/**
 * A hook that can be run after an Operator executes
 * 
 * @author quin
 *
 */
public interface OperatorEndHook extends OperatorHook{
	public void doHookEnd() throws Exception;
}
