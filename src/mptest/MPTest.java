package mptest;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class MPTest {

	public static void main(String args) {
		InetAddress addr;
		String hostname = "?";
		String ipStr = "?";
		try {
			addr = InetAddress.getLocalHost();
			byte[] ipAddr = addr.getAddress();
			hostname = addr.getHostName();
			ipStr = ipAddr[0] + "." + ipAddr[1] + "." + ipAddr[2] + "." + ipAddr[3];
		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
}
