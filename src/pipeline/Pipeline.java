package pipeline;

import java.io.File;
import java.io.IOException;
import java.sql.Time;
import java.util.Date;
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

public class Pipeline {

	protected File source;
	protected Document xmlDoc;
	public static final String PROJECT_HOME="projecthome";
	public static final String primaryLoggerName = "pipeline.primary";
	protected Logger primaryLogger = Logger.getLogger(primaryLoggerName);
	protected String defaultLogFilename = "pipelinelog";
	
	
	//Right now DEBUG just emits all log messages to std out
	public static final boolean DEBUG = true;
	
	public Pipeline(File inputFile) {
		this.source = inputFile;
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			xmlDoc = builder.parse(source);
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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

				@Override
				public void close() throws SecurityException {					
				}
				
			});
		}
	
		initializeLogger();
		
	}
	
	/**
	 * Perform a bit of initialization for the main Logger
	 */
	private void initializeLogger() {

		try {
			String timestamp = "" + System.currentTimeMillis();
			timestamp = timestamp.substring( timestamp.length()-6 );
			FileHandler logHandler = new FileHandler(defaultLogFilename + "-" + timestamp + ".xml");
			primaryLogger.addHandler( logHandler );
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.err.println("ERROR :  Could not open log file for writing! \n " + e.getCause() + e.getLocalizedMessage());
			e.printStackTrace();
		}
		
	}
	/**
	 * Attempt to run the pipeline defined in the source input file
	 * @throws PipelineDocException If there are errors in document structure
	 * @throws ObjectCreationException If errors arise regarding instiation of particular objects
	 */
	public void execute() throws PipelineDocException, ObjectCreationException {
		
		primaryLogger.info("\n\n***************************************************************** \n " + new Date() + " Beginning new Pipeline run");
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
		
		//Set the project home field
		String projectHomeStr = docElement.getAttribute(PROJECT_HOME);
		if (projectHomeStr.length() > 0) {
			//make sure home dir ends with a /
			if (! projectHomeStr.endsWith("/")) {
				projectHomeStr += "/";
			}
			handler.setProjectHome(projectHomeStr);
			primaryLogger.info("Setting project home to : " + projectHomeStr);
		}
				
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
