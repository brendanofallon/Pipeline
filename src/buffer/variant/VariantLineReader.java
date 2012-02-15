package buffer.variant;

import java.io.IOException;

/**
 * Interface for things that can look at a line of text and parse a 
 * variant record from it
 *
 * @author brendan
 *
 */
public interface VariantLineReader {

	/**
	 * Advance this line reader to the next variant record
	 * returns false is line cannot be read
	 */
	public boolean advanceLine() throws IOException; 
	
	/**
	 * Obtain a new variant record with information from a current line
	 */	
	public VariantRec toVariantRec();
	
}
