package operator.hook;


import java.io.IOException;

import json.JSONException;
import json.JSONObject;
import operator.OperatorStartHook;

/**
 * Sends updated messages about an Operator to the .NET service
 * @author quin
 *
 */
public class ServiceUpdateStartHook extends ServiceUpdateHook implements OperatorStartHook {
	
	
	public ServiceUpdateStartHook(String opCanName,
				String opName,
				String ipAddress,
				int jobID){
		super(opCanName, opName, ipAddress, jobID);
	}
	
	
	public void doHookStart() throws IOException{
		JSONObject obj = new JSONObject();
		try {
			obj.put("opCanName", opCanName);
			obj.put("opName", opName);
			obj.put("jobID", jobID);
			obj.put("ipAddress", ipAddress);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		HttpPostJSON(serviceURL, obj);
	}

	@Override
	public void initialize() {
		// TODO Auto-generated method stub
	}


}
