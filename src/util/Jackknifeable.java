package util;

public interface Jackknifeable {

	/**
	 * Restore this object to its original state, before any removal of elements
	 */
	public void restore(); 
	
	/**
	 * Eliminate one element or data point from this object
	 */
	public void removeElement(int which);
	
	/**
	 * The number of elements that could potentially be removed from scoring procedure
	 * @return
	 */
	public int getRemoveableElementCount();
	
}
