package buffer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NodeList;

import pipeline.ObjectCreationException;
import pipeline.PipelineObject;

/**
 * Base class of buffers that refer to a file. 
 * @author brendan
 *
 */
public abstract class FileBuffer extends PipelineObject {

	public static final String FILENAME_ATTR = "filename";
	
	protected File file;
	protected Map<String, String> properties = new HashMap<String, String>();
	
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
	
	public void initialize(NodeList children) throws IllegalStateException {
		String filename = properties.get(FILENAME_ATTR);
		if (filename == null || filename.length()==0) {
			throw new IllegalStateException("Property '" + FILENAME_ATTR + "' required to create file buffer object");
		}
		file = new File(filename);
	}
	
	/**
	 * Return the name of the file
	 * @return
	 */
	public String getFilename() {
		if (file == null)
			return null;
		return file.getName();
	}
	
	/**
	 * Return the absolute path of the file
	 * @return
	 */
	public String getAbsolutePath() {
		if (file==null)
			return null;
		return file.getAbsolutePath();
	}
	
	/**
	 * Obtain the unique type string associated with this file type
	 * @return
	 */
	public abstract String getTypeStr();
}
