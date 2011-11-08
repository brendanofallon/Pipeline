package pipeline;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class ObjectHandler {

	protected Document doc;
	protected Map<String, Object> buffers = new HashMap<String, Object>();
	protected Map<String, Object> operators = new HashMap<String, Object>();
	
	protected Map<String, PipelineObject> objectMap = new HashMap<String, PipelineObject>();
	
	private final boolean verbose = true;
	
	public ObjectHandler(Document doc) {
		this.doc = doc;
		scanDocument();
	}

	/**
	 * Returns the object whose label .equals the given label
	 * @param label
	 * @return
	 */
	public PipelineObject getObjectForLabel(String label) {
		return objectMap.get(label);
	}
	
	public void readObjects() throws ObjectCreationException {
		//Instantiate top-level buffers
		Element root = doc.getDocumentElement();
		NodeList children = root.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element el = (Element)child;
				createElement(el);
			}
		}
	}
	
	
	
	private PipelineObject createElement(Element el) throws ObjectCreationException {
		
		//Recursively create children first
		NodeList children = el.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node childNode = children.item(i);
			if (childNode.getNodeType() == Node.ELEMENT_NODE) {
				Element child = (Element)childNode;
				PipelineObject childObj = createElement(child);
				//We don't actually do anything with the element now
			}	
		}
		
		String classStr = getElementClass(el);
		if (classStr == null || classStr.length()==0) {
			return null;
		}
		
		try {
			Class<Object> clz = loadClass(classStr);
			if (PipelineObject.class.isAssignableFrom(clz)) {
				PipelineObject obj = (PipelineObject) clz.newInstance();
				obj.setObjectLabel(el.getNodeName());
				obj.setObjectHandler(this);

				//Set all attributes found in XML
				NamedNodeMap attrs = el.getAttributes();
				for(int i=0; i<attrs.getLength(); i++) {
					obj.setAttribute(attrs.item(i).getNodeName(), attrs.item(i).getNodeValue());
				}

				if (verbose) {
					System.out.println("Creating object with label : " + obj.getObjectLabel() + " and class " + obj.getClass());
				}
				objectMap.put(obj.getObjectLabel(), obj);

				
				return obj;
			}
			
		} catch (ClassNotFoundException e) {
			throw new ObjectCreationException(e.getCause() + " : " + e.getLocalizedMessage(), el);
		} catch (InstantiationException e) {
			throw new ObjectCreationException(e.getCause() + " : " + e.getLocalizedMessage(), el);
		} catch (IllegalAccessException e) {
			throw new ObjectCreationException(e.getCause() + " : " + e.getLocalizedMessage(), el);
		} catch (Exception e) {
			throw new ObjectCreationException(e.getCause() + " : " + e.getLocalizedMessage(), el);
		}
	
		return null;
	}

	private Class loadClass(String classStr) throws ClassNotFoundException {
		//TODO We'd like to be able to search other paths, not just already loaded classes
		ClassLoader systemLoader = ClassLoader.getSystemClassLoader();
		Class clazz = systemLoader.loadClass(classStr);
		return clazz;
	}

	/**
	 * Returns the value of the attribute associated with the key CLASS_ATTR, or 
	 * an *EMPTY STRING* (not null) if no such attribute exists 
	 * @param el
	 * @return
	 */
	private String getElementClass(Element el) {
		String val = el.getAttribute(PipelineXMLConstants.CLASS_ATTR);
		return val;
	}
	
	/**
	 * Scan over all first-level elements in the document 
	 */
	private void scanDocument() {
		Element root = doc.getDocumentElement();
		NodeList children = root.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				addElement( (Element)child);
				
			}
		}
	}
	
	
	
	/**
	 * Add 
	 * @param el
	 */
	private void addElement(Element el) {
		// ?
	}
	
}
