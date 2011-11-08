package buffer;

public class SAIFile extends FileBuffer {

	public boolean isBinary() {
		return true;
	}
	
	@Override
	public String getTypeStr() {
		return "SAIFile";
	}

}
