package buffer.variant;

import java.io.File;

import buffer.FileBuffer;

/**
 * A file ostensibly formatted in GVF
 * @author brendan
 *
 */
public class GVFFile extends FileBuffer {

	public GVFFile() {
	}
	
	public GVFFile(File file) {
		super(file);
	}

	@Override
	public String getTypeStr() {
		return "GVFFile";
	}

	
}
