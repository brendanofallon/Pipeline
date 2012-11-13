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
import buffer.MultiFileBuffer;
import buffer.VCFFile;

/**
 * Create directories and copy files to the directory where GenomicsReviewApp can see them
 * @author brendan
 *
 */
public class ReviewDirGenerator extends Operator {

	public static final String DEST_DIR = "destination.dir";
	
	String rootPath = null;
	VCFFile variantFile = null;
	BAMFile finalBAM = null;
	InstanceLogFile logFile = null;
	MultiFileBuffer fastqs1 = null;
	MultiFileBuffer fastqs2 = null;
	
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger.getLogger(Pipeline.primaryLoggerName).info("Creating GenomicsReview directory in " + rootPath);
		
		
		if (!rootPath.startsWith("/")) {
			throw new OperationFailedException("Root path MUST be absolute", this);
		}
		
		//Create directory structure
		createDir("", rootPath);
		createDir(rootPath, "bam");
		createDir(rootPath, "var");
		createDir(rootPath, "log");
		createDir(rootPath, "depth");
		createDir(rootPath, "qc");
		createDir(rootPath, "report");
		createDir(rootPath, "fastq");
		createDir(rootPath, "array");
		
		try {
			File varDestination = new File(rootPath + "/var/" + variantFile.getFilename());
			copyTextFile(variantFile.getFile(), varDestination);

			File logDestination = new File(rootPath + "/log/" + logFile.getFilename());
			copyTextFile(logFile.getFile(), logDestination);

			File newBAMLocation = new File(rootPath + "/bam/");
			moveFile(finalBAM, newBAMLocation);
			
			
			File newFQLocation = new File(rootPath + "/fastq/");
			if (fastqs1 != null) {
				moveFiles(fastqs1, newFQLocation);
			}
			
			if (fastqs2 != null) {
				moveFiles(fastqs2, newFQLocation);
			}
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Error moving files to review dir destination : " + e.getMessage(), this);
		}
		
	}

	/**
	 * Copy the given file to the new destination - this will only work with text files
	 * @param source
	 * @param dest
	 * @throws IOException
	 */
	private void copyTextFile(File source, File dest) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(source));
		if (! dest.exists()) {
			dest.createNewFile();
		}
		BufferedWriter writer = new BufferedWriter(new FileWriter(dest));
		String line = reader.readLine();
		while(line != null) {
			writer.write( line + "\n");
			line =reader.readLine();
		}
		
		writer.close();
		reader.close();
	}
	
	/**
	 * Move all files in the multi-file-buffer to the new parent dir
	 * @param sourceFiles
	 * @param newParentDir
	 */
	private void moveFiles(MultiFileBuffer sourceFiles, File newParentDir) {
		for(int i=0; i<sourceFiles.getFileCount(); i++) {
			moveFile(sourceFiles.getFile(i), newParentDir);
		}
	}
	
	/**
	 * Move the given file to the new destination directory, preserving the short file name
	 * @param sourceFile
	 * @param newParentDir
	 */
	private void moveFile(FileBuffer sourceFile, File newParentDir) {
		File destinationFile = new File(newParentDir + "/" + sourceFile.getFilename());
		Logger.getLogger(Pipeline.primaryLoggerName).info("Renaming " + sourceFile.getFile().getAbsolutePath() + " to " + destinationFile.getAbsolutePath());

		
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
		
		rootPath = properties.get(DEST_DIR);
		if (rootPath == null) {
			throw new IllegalArgumentException("No root path specified, not sure where to make files");
		}
		
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
				
				if (obj instanceof MultiFileBuffer) {
					if (fastqs1 == null)
						fastqs1 = (MultiFileBuffer)obj;
					else
						fastqs2 = (MultiFileBuffer)obj;
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
