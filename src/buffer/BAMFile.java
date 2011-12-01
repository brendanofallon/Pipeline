package buffer;

import java.io.File;

public class BAMFile extends FileBuffer {

	public BAMFile() {
		//blank on purpose
	}
	
	public BAMFile(File file) {
		super(file);
	}
	
	public boolean isBinary() {
		return true;
	}
	
	
	
	@Override
	public String getTypeStr() {
		return "BAMFile";
	}

}
