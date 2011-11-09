package operator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BinaryPipeHandler extends Thread {

	InputStream inpStr;
	OutputStream writer;

	public BinaryPipeHandler(InputStream inpStr, OutputStream writer) {
		this.inpStr = inpStr;
		this.writer = writer;
	}

	public void run() {
			int c;
			try {
				while ((c = inpStr.read()) != -1) {
					writer.write(c);
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
}
