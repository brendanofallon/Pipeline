package buffer;

import java.io.File;

public class BEDFile extends FileBuffer {

	public BEDFile() {
	}
	
	public BEDFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "BEDfile";
	}

}
