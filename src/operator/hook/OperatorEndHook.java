package operator.hook;

import operator.IOperatorEndHook;

public abstract class OperatorEndHook extends OperatorHook implements IOperatorEndHook {

	@Override
	public abstract void doHook() throws Exception;

}
