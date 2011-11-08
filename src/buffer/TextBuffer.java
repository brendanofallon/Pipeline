package buffer;

import org.w3c.dom.NodeList;

/**
 * Sample / practice buffer
 * @author brendan
 *
 */
public class TextBuffer extends FileBuffer {

	@Override
	public String getTypeStr() {
		return "Text buffer";
	}

	@Override
	public void initialize(NodeList children) {
		//Don't think we need to do anything
	}

}
