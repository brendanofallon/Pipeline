package operator;

import org.w3c.dom.NodeList;

public class WaitOperator extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
		try {
			System.out.println("Waiting....");
			Thread.sleep(10000); 
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("..done!");
	}

	@Override
	public void initialize(NodeList children) {
		// TODO Auto-generated method stub
		
	}

}
