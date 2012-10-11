package operator;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Logger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.InstanceLogFile;
import buffer.VCFFile;

/**
 * Create directories and copy files to the directory where GenomicsReviewApp can see them
 * @author brendan
 *
 */
public class ReviewDirGenerator extends Operator {

	public static final String DEST_DIR = "destiation.dir";
	
	String rootPath = null;
	VCFFile variantFile = null;
	BAMFile finalBAM = null;
	InstanceLogFile logFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		
		rootPath = properties.get(DEST_DIR);
		Logger.getLogger(Pipeline.primaryLoggerName).info("Creating GenomicsReview directory in " + rootPath);
		
		if (rootPath == null) {
			throw new OperationFailedException("No root path specified, not sure where to make files", this);
		}
		
		if (!rootPath.startsWith("/")) {
			throw new OperationFailedException("Root path MUST be absolute", this);
		}
		
		//Create directory structure
		createDir("", rootPath);
		createDir(rootPath, "bam");
		createDir(rootPath, "var");
		createDir(rootPath, "log");
		createDir(rootPath, "depth");
		createDir(rootPath, "nuclear");
		createDir(rootPath, "qc");
		createDir(rootPath, "report");
		createDir(rootPath, "mt");
		createDir(rootPath, "array");
		
		try {
			File varDestination = new File(rootPath + "/var/");
			copyTextFile(variantFile.getFile(), varDestination);

			File logDestination = new File(rootPath + "/log/");
			copyTextFile(logFile.getFile(), logDestination);

			File newBAMLocation = new File(rootPath + "/bam/");
			moveFile(finalBAM, newBAMLocation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Error moving files to review dir destination : " + e.getMessage(), this);
		}
		
	}

	private void copyTextFile(File source, File dest) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source));
		if (! dest.exists()) {
			dest.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String line = reader.readLine();
		while(line != null) {
			writer.write( line);
			line =reader.readLine();
		}
		
		writer.close();
		reader.close();
	}
	
	private void moveFile(FileBuffer sourceFile, File newParentDir) {
		File destinationFile = new File(newParentDir + "/" + sourceFile.getFilename());
		sourceFile.getFile().renameTo(destinationFile);
		sourceFile.setFile(destinationFile);
	}
	
	private boolean createDir(String parent, String dirName) {
		File dir = new File(parent + "/" + dirName);
		boolean ok = dir.mkdir();
		return ok;
	}
	
	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					finalBAM = (BAMFile)obj;
				}
				
				if (obj instanceof VCFFile) {
					variantFile = (VCFFile)obj;
				}
				
				if (obj instanceof InstanceLogFile) {
					logFile = (InstanceLogFile)obj;
				}
			}
		}
		
		if (finalBAM == null) {
			throw new IllegalArgumentException("No BAM file specified to ReviewDirGenerator");
		}
		if (variantFile == null) {
			throw new IllegalArgumentException("No variant file specified to ReviewDirGenerator");
		}
		if (logFile == null) {
			throw new IllegalArgumentException("No log file specified to ReviewDirGenerator");
		}
	}

}
