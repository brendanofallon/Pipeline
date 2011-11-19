package buffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.NodeList;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * Base class of buffers that refer to a file. 
 * @author brendan
 *
 */
public abstract class FileBuffer extends PipelineObject {

	public static final String FILENAME_ATTR = "filename";
	public static final String BINARY_ATTR = "binary";
	
	protected File file;
	protected Map<String, String> properties = new HashMap<String, String>();
	
	public void setAttribute(String key, String value) {
		properties.put(key, value);
	}
	
	/**
	 * Returns true if the binary flag has been set for this FileBuffer, indicating that reads/writes
	 * should be done in binary form
	 * @return
	 */
	public boolean isBinary() {
		String binStr = properties.get(BINARY_ATTR);
		if (binStr == null) {
			return false;
		}
		else {
			Boolean binary = Boolean.parseBoolean(binStr);
			return binary;
		}
	}
	
	public FileInputStream getInputStream() throws FileNotFoundException {
		if (file == null)
			return null;
		else
			return new FileInputStream(file);
	}
	
	public FileOutputStream getOutputStream() throws FileNotFoundException {
		if (file == null)
			return null;
		else 
			return new FileOutputStream(file);
	}
	
	public void initialize(NodeList children) throws IllegalStateException {
		String filename = properties.get(FILENAME_ATTR);
		if (filename == null || filename.length()==0) {
			throw new IllegalStateException("Property '" + FILENAME_ATTR + "' required to create file buffer object");
		}
		
		String pathMod = "";
		String projHome = properties.get(Pipeline.PROJECT_HOME);
		if (projHome != null) {
			pathMod = projHome;
		}
		file = new File(pathMod + filename);
	}
	
	/**
	 * Obtain the file object associated with this buffer
	 * @return
	 */
	public File getFile() {
		return file;
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
