package operator.hook;

import operator.IOperatorStartHook;


public abstract class OperatorStartHook extends OperatorHook implements IOperatorStartHook{

	@Override
	public abstract void doHook() throws Exception;

}
