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
				//Attempt to read data in 1Mb chunks, this is a lot faster than
				//doing things one byte at a time
				byte[] data = new byte[1024];
				int bytesRead = inpStr.read(data);
				while(bytesRead >= 0) {
					writer.write(data, 0, bytesRead);
					bytesRead = inpStr.read(data);
				}
				writer.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
}
