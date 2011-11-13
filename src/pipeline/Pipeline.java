package pipeline;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import util.ElapsedTimeFormatter;

public class Pipeline {

	protected File source;
	protected Document xmlDoc;
	public static final String primaryLoggerName = "pipeline.primary";
	protected Logger primaryLogger = Logger.getLogger(primaryLoggerName);
	protected String defaultLogFilename = "pipelinelog.xml";
	
	//Right now DEBUG just emits all log messages to std out
	public static final boolean DEBUG = true;
	
	//Stores some basic properties, such as paths to some commonly used executables
	protected Properties props;
	public static final String defaultPropertiesPath = ".pipelineprops.xml";
	public static Pipeline pipelineInstance;
	
	public Pipeline(File inputFile) {
		this.source = inputFile;
		pipelineInstance = this;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(source);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		

		initializeLogger();
		loadProperties();
		
	}
	
	/**
	 * Static getter for main application object
	 * @return
	 */
	public static Pipeline getPipelineInstance() {
		return pipelineInstance;
	}
	
	/**
	 * Obtain the value of the property associated with the given key, or null
	 * if no such property exists or if the properties were not successfully loaded
	 * (for instance, if no properties file was found)
	 * @param key
	 * @return Value associated with key provided
	 */
	public static Object getPropertyStatic(String key) {
		return pipelineInstance.getProperty(key);
	}
	
	public Object getProperty(String key) {
		if (props != null)
			return props.get(key);
		else
			return null;
	}
	
	/**
	 * Attempt to load some basic properties from a persistent file
	 */
	private void loadProperties() {
		//First check to see if properties file is in user dir, if so use it
		String userDir = System.getProperty("user.dir");
		File propsFile = new File(userDir + "/" + defaultPropertiesPath);
		
		//If its not in user.dir, then check home directory, if not there then abort
		if (! propsFile.exists()) {
			String homeDir = System.getProperty("user.home");
			propsFile = new File(homeDir + "/" + defaultPropertiesPath);
			if (! propsFile.exists()) {
				primaryLogger.warning("Could not find default properties file, no file at path " + propsFile.getAbsolutePath());
				return;
			}
		}
		
		props = new Properties();
		try {
			FileInputStream propStream = new FileInputStream(propsFile);
			props.loadFromXML(propStream);
			
			StringBuffer msg = new StringBuffer("Loaded following properties: \n");
			
			for(Object key : props.keySet()) {
				msg.append(key.toString() + " = " + props.getProperty(key.toString()) + "\n");
			}
			primaryLogger.info(msg.toString());
		} catch (FileNotFoundException e) {
			primaryLogger.warning("Could not open stream for properties file at path " + propsFile.getAbsolutePath());
		} catch (InvalidPropertiesFormatException e) {
			primaryLogger.warning("Could not read from default properties file: \n" + e.getCause() + "\n" + e.getLocalizedMessage());
		} catch (IOException e) {
			primaryLogger.warning("Could not read from default properties file: \n" + e.getCause() + "\n" + e.getLocalizedMessage());
		}
		
	}

	/**
	 * Perform a bit of initialization for the main Logger
	 */
	private void initializeLogger() {

		if (DEBUG) {
			primaryLogger.addHandler(new Handler() {
				@Override
				public void publish(LogRecord record) {
					System.out.println(record.getMessage());
				}

				@Override
				public void flush() {
					System.out.flush();
				}
				public void close() throws SecurityException { }
			});
		}
		
		try {
			FileHandler logHandler = new FileHandler(defaultLogFilename);
			primaryLogger.addHandler( logHandler );
	
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
			e.printStackTrace();
		}
		

		Date beginTime = new Date();
		primaryLogger.info("\n\n***************************************************************** \n " + beginTime + " Beginning new Pipeline run");
	}
	/**
	 * Attempt to run the pipeline defined in the source input file
	 * @throws PipelineDocException If there are errors in document structure
	 * @throws ObjectCreationException If errors arise regarding instiation of particular objects
	 */
	public void execute() throws PipelineDocException, ObjectCreationException {
		
		Date beginTime = new Date();
		
		if (xmlDoc == null) {
			primaryLogger.severe(" ERROR : XML document not found / defined, aborting run ");
			throw new IllegalStateException("XMLDoc not defined");
		}
		
		
		Element docElement = xmlDoc.getDocumentElement();
		String docRootName = docElement.getNodeName();
		if (! docRootName.equals(PipelineXMLConstants.DOCUMENT_ROOT)) {
			throw new PipelineDocException("Document root name should be " + PipelineXMLConstants.DOCUMENT_ROOT + ", but found : " + docRootName);
		}
		
		primaryLogger.info("XML Document at path " + source.getAbsolutePath() + " found and parsed, attempting to read objects");
		ObjectHandler handler = new ObjectHandler(xmlDoc);

		//A quick scan for errors / validity would be a good idea
		
		try {
			handler.readObjects();
		}
		catch (ObjectCreationException ex) {
			primaryLogger.severe("Error creating objects : " + ex.getCause() + "\n" + ex.getLocalizedMessage());
			return;
		}

		primaryLogger.info("Document parsed and objects are read, attempting to begin pipeline");
		
		for(Operator op : handler.getOperatorList()) {
			try {
				primaryLogger.info("Executing operator : " + op.getObjectLabel() + " class: " + op.getClass());
				op.performOperation();
				
			} catch (OperationFailedException e) {
				e.printStackTrace();
				primaryLogger.severe("ERROR : Operator : " + op.getObjectLabel() + " (class " + op.getClass() + ") failed \n Cause : " + e.getMessage());
				return;
			}
		}
		
		
		long endTime = System.currentTimeMillis();
		primaryLogger.info("Finished executing all operators, pipeline is done. \n Total elapsed time " + ElapsedTimeFormatter.getElapsedTime(beginTime.getTime(), endTime ));
	}
	
	public static void main(String[] args) {
		
		//If no args, assume this is a test run and attempt to execute testInput.xml
		if (args.length == 0) {
			args = new String[]{"src/test/testInput.xml"};
		}
		
		//Otherwise, assume all args are input files and execute them in order
		for(int i=0; i<args.length; i++) {
			File input = new File(args[i]);
			Pipeline pipeline = new Pipeline(input);

			try {
				pipeline.execute();
			} catch (PipelineDocException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ObjectCreationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
