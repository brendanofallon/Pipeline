package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

public class StringPipeHandler extends Thread {

		InputStream inpStr;
		PrintStream stream;
		
		public StringPipeHandler(InputStream inpStr, PrintStream stream) {
			this.inpStr = inpStr;
			this.stream = stream;
		}

		public void run() {
			try {
				InputStreamReader inpStrd = new InputStreamReader(inpStr);
				BufferedReader buffRd = new BufferedReader(inpStrd);
				String line = null;
				while((line = buffRd.readLine()) != null) {
					if (stream != null)
						stream.println(line);
				}
				buffRd.close();

			} catch(Exception e) {
				System.out.println(e);
			}

		}
}
