package operator;

public interface IOperatorHook {
	public void doHook() throws Exception;
	
	public void initHook(Operator op);
}
