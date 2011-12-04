package buffer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;

/**
 * This object is a container for a list of file buffers. 
 * @author brendan
 */

public class MultiFileBuffer extends FileBuffer {

	List<FileBuffer> files = new ArrayList<FileBuffer>();
	
	public MultiFileBuffer() {
		file = null;
	}
	
	public void addFile(FileBuffer buf) {
		files.add(buf);
	}
	
	public void removeFile(FileBuffer buf) {
		files.remove(buf);
	}
	
	public int getFileCount() {
		return files.size();
	}
	
	public FileBuffer getFile(int i) {
		return files.get(i);
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node node = children.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(node.getNodeName());
				if (obj instanceof FileBuffer) {
					addFile( (FileBuffer)obj);
				}
				else {
					throw new IllegalArgumentException("Got non-filebuffer object as argument to MultiFile container, it has name: " + node.getNodeName());
				}
			}
		}
	}

	/**
	 * No single File is associated with this object 
	 * @return
	 */
	public File getFile() {
		return null;
	}
	
	/**
	 * Return the name of the file
	 * @return
	 */
	public String getFilename() {
		throw new IllegalStateException("Cannot read single filename from MultiFileBuffer");
	}
	
	/**
	 * Return the absolute path of the file
	 * @return
	 */
	public String getAbsolutePath() {
		throw new IllegalStateException("Cannot read single file path from MultiFileBuffer");
	}

	@Override
	public String getTypeStr() {
		return "Multi-file buffer";
	}
}
