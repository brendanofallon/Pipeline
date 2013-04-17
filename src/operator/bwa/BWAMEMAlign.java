package operator.bwa;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

import operator.IOOperator;
import operator.OperationFailedException;

import org.w3c.dom.NodeList;

import pipeline.Pipeline;
import buffer.BAMFile;
import buffer.FastQFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * Uses BWA's fancy new 'mem' algorithm to align. ALso pipes output directly
 * into samtools for sorting and bamification. Should be a bit faster.
 * Currently requires paired-end reads and can't handle samples from multiple lanes. 
 *   
 * @author brendan
 *
 */
public class BWAMEMAlign extends IOOperator {
	
	public static final String BWA_PATH = "bwa.path";
	public static final String STREAMSORT_PATH = "streamsort.path";
	public static final String SAMTOOLS_PATH = "samtools.path";
	String sample = "unknown";
	String samtoolsPath = null;
	String streamsortPath = null;
	String bwaPath = null;

	@Override
	public void performOperation() throws OperationFailedException {
		
		
		ReferenceFile refBuf = (ReferenceFile) this.getInputBufferForClass(ReferenceFile.class);
		
		List<FileBuffer> inputBuffers = this.getAllInputBuffersForClass(FastQFile.class);
		
		FileBuffer outputBAMBuffer = this.getOutputBufferForClass(BAMFile.class);
		if (outputBAMBuffer == null) {
			throw new OperationFailedException("No output BAM file found", this);
		}
		
		if (inputBuffers.size() != 2) {
			throw new OperationFailedException("Exactly two fastq files must be provided to this aligner, found " + inputBuffers.size(), this);
		}
		
		String sampleAttr = getAttribute("sample");
		if (sampleAttr != null)
			sample = sampleAttr;
		
		int threads = this.getPipelineOwner().getThreadCount();
		
		Logger.getLogger(Pipeline.primaryLoggerName).info("BWA-MEM is aligning " + inputBuffers.get(0).getFilename() + " and " + inputBuffers.get(1).getFilename() + " with " + threads + " threads");
		
		String tmpDir = System.getProperty("java.io.tmpdir");
		
		String command = bwaPath 
				+ " mem "
				+ refBuf.getAbsolutePath() + " "
				+ inputBuffers.get(0).getAbsolutePath() + " "
				+ inputBuffers.get(1).getAbsolutePath() + " "
				+ " -t " + threads
				+ " -R \"@RG\\tID:unknown\\tSM:" + sample + "\\tPL:ILLUMINA"
				+ " 2> .bwa.mem.stderr.txt "
				+ " | " + " java -Xmx8g -Djava.io.tmpdir=" + tmpDir + " -jar " + streamsortPath + " 2> .sserr.txt| " + samtoolsPath + " view -Sb - 2> .samtoolserr.txt > " + outputBAMBuffer.getAbsolutePath();
					
		executeBASHCommand(command);
	}
	
	private void executeBASHCommand(String command) throws OperationFailedException {
		String filename = this.getProjectHome() + "/bwacommand-" + ((1000000.0*Math.random())+"").substring(0, 6).replace(".", "") + ".sh";
		BufferedWriter writer;
		try {
			writer = new BufferedWriter(new FileWriter(filename));
			writer.write(command + "\n");
			writer.close();
		} catch (IOException e) {
			throw new OperationFailedException("IO Error writing bwa command file : " + e.getMessage(), this);
		}
		
		
		ProcessBuilder procBuilder = new ProcessBuilder("/bin/bash", filename);
		try {
			Process proc = procBuilder.start();
			Logger.getLogger(Pipeline.primaryLoggerName).info("BWA-MEM is executing command: " + command);
			int exitVal = proc.waitFor();
			
			if (exitVal != 0) {
				throw new OperationFailedException("BWA-MEM process exited with nonzero status, aborting", this);
			}
		} catch (IOException e) {
			throw new OperationFailedException("Error running BWA-MEM : " + e.getLocalizedMessage(), this);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
	}

	
	
	@Override
	public void initialize(NodeList children) {
		super.initialize(children);
		
		String pathAttr = this.getAttribute(BWA_PATH);
		if (pathAttr == null) {
			pathAttr = this.getPipelineProperty(BWA_PATH);
		}
		if (pathAttr == null) {
			throw new IllegalArgumentException("No path to BWA found, please specify " + BWA_PATH);
		}
		if (! (new File(pathAttr).exists())) {
			throw new IllegalArgumentException("No file found at BWA path : " + bwaPath);
		}
		this.bwaPath = pathAttr;
		
		
		String ssAttr = this.getAttribute(STREAMSORT_PATH);
		if (ssAttr == null) {
			ssAttr = this.getPipelineProperty(STREAMSORT_PATH);
		}
		if (ssAttr == null) {
			throw new IllegalArgumentException("No path to stream sorter.jar found, please specify " + STREAMSORT_PATH);
		}
		if (! (new File(ssAttr).exists())) {
			throw new IllegalArgumentException("No file found at stream sorter path : " + ssAttr);
		}
		this.streamsortPath = ssAttr;
		
		String samtoolsAttr = this.getAttribute(SAMTOOLS_PATH);
		if (samtoolsAttr == null) {
			samtoolsAttr = this.getPipelineProperty(SAMTOOLS_PATH);
		}
		if (samtoolsAttr == null) {
			throw new IllegalArgumentException("No path to samtools found, please specify " + SAMTOOLS_PATH);
		}
		if (! (new File(samtoolsAttr).exists())) {
			throw new IllegalArgumentException("No file found at samtools path : " + samtoolsAttr);
		}
		this.samtoolsPath = samtoolsAttr;
		
	}

}
