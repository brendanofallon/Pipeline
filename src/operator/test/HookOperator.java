package operator.test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.w3c.dom.NodeList;

import operator.OperationFailedException;
import operator.Operator;
import operator.hook.ServiceUpdateEndHook;
import operator.hook.ServiceUpdateStartHook;

/**
 * A test operator to try out hooks
 * @author quin
 *
 */
public class HookOperator extends Operator {

	@Override
	public void performOperation() throws OperationFailedException {
		System.out.println("Test Operator");
	}

	@Override
	public void initialize(NodeList children) {
		String address = "";
		try {
			address = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		ServiceUpdateStartHook startHook = 
				new ServiceUpdateStartHook(this.getClass().getCanonicalName(),
										   this.getObjectLabel(),
										   address,
										   0);
		ServiceUpdateEndHook endHook = 
				new ServiceUpdateEndHook(this.getClass().getCanonicalName(), 
										 this.getObjectLabel(), 
										 address, 
										 0);
		this.addStartHook(startHook);
		this.addEndHook(endHook);
	}

}
