package buffer;

import java.io.File;

/**
 * Base class of buffers that refer to a file. 
 * @author brendan
 *
 */
public abstract class FileBuffer {

	protected final File file;
	
	public FileBuffer(File file) {
		this.file = file;
	}
	
	/**
	 * Obtain the unique type string associated with this file type
	 * @return
	 */
	public abstract String getTypeStr();
}
