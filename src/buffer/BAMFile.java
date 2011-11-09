package buffer;

public class BAMFile extends FileBuffer {

	public boolean isBinary() {
		return true;
	}
	
	@Override
	public String getTypeStr() {
		return "BAMFile";
	}

}
