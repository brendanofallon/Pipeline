package pipeline;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * All objects that can be mapped onto XML 
 * @author brendan
 *
 */
public abstract class PipelineObject {

	protected String objectLabel = null;
	private boolean initialized = false;
	
	private ObjectHandler objectHandler;
	
	public void setObjectHandler(ObjectHandler handler) {
		this.objectHandler = handler;
	}
	
	/**
	 * Returns the object from the handler whose label is equal to the given label
	 * @param label
	 * @return
	 */
	public PipelineObject getObjectFromHandler(String label) {
		return objectHandler.getObjectForLabel(label);
	}
	
	/**
	 * Set the unique label for this object
	 * @param label
	 */
	public void setObjectLabel(String label) {
		if (objectLabel != null)
			throw new IllegalArgumentException("A label has already been set for object with label : " + objectLabel);
		this.objectLabel = label;
		
	}
	
	public String getObjectLabel() {
		return objectLabel;
	}
	
	/**
	 * Returns true if initializeObject has been called on this object
	 * @return
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
	public final void initializeObject(NodeList children) {
		initialize(children);
		initialized = true;
	}
	
	/**
	 * Returns the first *Element* node encountered whose label is .equal to the given key, or
	 * null if no such child element exists 
	 * @param children
	 * @return
	 */
	protected static Element getChildForLabel(String key, NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				if (key.equals( child.getNodeName() )) {
					return (Element)child;
				}
			}
			
		}
		
		return null;
	}
	
	/**
	 * Set the value for some property associated with the given key. 
	 * @param key
	 * @param value
	 */
	public abstract void setAttribute(String key, String value);

	/**
	 * Obtain the value for some property associated with the given key. 
	 * @param key
	 * @param value
	 */	
	public abstract String getAttribute(String key);
	
	/**
	 * Return keys for all attributes
	 * @return
	 */
	public abstract Collection<String> getAttributeKeys();
	
	/**
	 * Called after 
	 */
	public abstract void initialize(NodeList children);
	
	/**
	 * Returns true if all of the attributes in this object are the
	 * same as ( .equals() is true) those specified for the given object
	 * @param obj
	 * @return
	 */
//	public boolean attributesAreEqual(PipelineObject obj) {
//		for(String key : getAttributeKeys()) {
//			if (! (this.getAttribute(key).equals(obj.getAttribute(key))))
//				return false;
//		}
//		return true;
//	}
}
