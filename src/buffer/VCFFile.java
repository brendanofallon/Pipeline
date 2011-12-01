package buffer;

import java.io.File;

public class VCFFile extends FileBuffer {

	public VCFFile() {
		//blank on purpose
	}
	
	public VCFFile(File file) {
		super(file);
	}
	
	@Override
	public String getTypeStr() {
		return "VCFFile";
	}

}
