package buffer;

import java.io.File;

import org.w3c.dom.NodeList;

public class ReferenceFile extends FileBuffer {

	public static final String TYPE = "Reference file";

	@Override
	public String getTypeStr() {
		return TYPE;
	}


}
