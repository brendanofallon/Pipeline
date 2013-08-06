package operator.hook;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

import json.JSONObject;
import operator.OperatorHook;

public abstract class ServiceUpdateHook implements OperatorHook{
	protected static final String serviceURL = "localhost:8080/OperatorUpdate";
	protected static final String success = "{\"Success\"}";
	
	protected String opCanName;
	protected String opName;
	protected String ipAddress;
	protected int jobID;
	
	public ServiceUpdateHook(String opCanName,
							String opName,
							String ipAddress,
							int jobID){
		this.opCanName = opCanName;
		this.opName = opName;
		this.ipAddress = ipAddress;
		this.jobID = jobID;
	}
	
	/**
	 * Sets the Operator canonical name
	 * @param opCanName
	 */
	public void setOpCanName(String opCanName){
		this.opCanName = opCanName;
	}
	
	/**
	 * Sets the Operator name
	 * @param opName
	 */
	public void setOpName(String opName){
		this.opName = opName;
	}

	/**
	 * Sets the Job ID
	 * @param jobID
	 */
	public void setJobID(int jobID){
		this.jobID = jobID;
	}
	
	public void HttpPostJSON(String url, JSONObject js) throws IOException{
		String content = js.toString();
		
		URLConnection conn = new URL(url).openConnection();
		conn.setUseCaches(false);
		conn.setDoInput(true);
		conn.setDoOutput(true);
		conn.setRequestProperty("Content-Length", "" + content.length());
		conn.setRequestProperty("Content-Type", "application/json");
		
		OutputStream out = conn.getOutputStream();
		out.write(content.getBytes());
		out.close();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
		String response = br.readLine();
		br.close();
		if(!response.equals(success)) throw new IOException("Error when posting update to .NET service");
	}
	
	/**
	 * Any initialization that needs to be done for this hook
	 */
	public abstract void initialize();

}
