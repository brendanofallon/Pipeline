package pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import operator.Operator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is responsible for reading an xml DOM document and creating Pipeline objects based
 * on the xml. Element creation is recursively handled by a call to createElement(root of DOM), which happens
 * prior to any actual execution of the document. Execution occurs when some other object calls .performOperation()
 * on an operator object, causing the operator to perform its operation (whatever it may be).
 *  
 * @author brendan
 *
 */
public class ObjectHandler {

	protected Document doc;
	protected List<Operator> operatorList = null;
	
	protected Map<String, PipelineObject> objectMap = new HashMap<String, PipelineObject>();
	
	private final boolean verbose = false;
	
	public ObjectHandler(Document doc) {
		this.doc = doc;
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
		createElement(root); //Recursively creates all child elements
				
		//Build operator list. WE assume all operators are at top level for now
		operatorList = new ArrayList<Operator>();
		NodeList children = root.getChildNodes();
		for(int i=0; i<children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String label = child.getNodeName();
				PipelineObject obj = objectMap.get(label);
				if (obj instanceof Operator) {
					operatorList.add( (Operator)obj);
				}
			}
		}
		
	}
	
	/**
	 * Get the list of Operators defined at top level in the xml file 
	 * @return
	 */
	public List<Operator> getOperatorList() {
		return operatorList;
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
			
			//We're here because a class string has been listed as an argument to this element, meaning that it maps
			//to an object we should create. If there's already an object with the same label but a different
			//class in the objectMap, we should throw an error
			String label = el.getNodeName();
			PipelineObject preObj = objectMap.get(label);
			if (preObj != null) {
				if (! preObj.getClass().getCanonicalName().equals(classStr)) {
					throw new ObjectCreationException("Found two objects with label " + label + " but conflicting classes", el);
				}
			}
			
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
				
				obj.initialize(children);
				
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

	
	
}
