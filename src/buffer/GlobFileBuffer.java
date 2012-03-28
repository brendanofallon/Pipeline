package buffer;

import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.ObjectCreationException;
import pipeline.Pipeline;
import pipeline.PipelineObject;

/**
 * This MultiFileBuffer contains all files matching the regexp given in the filename attribute
 * For instance, if filename="*.bam" is an attribute, this thing will contain all files in PROJECT_HOME
 * that end in .bam
 * @author brendan
 *
 */
public class GlobFileBuffer extends MultiFileBuffer {

	private boolean guessContig = true; //Attempt to associate contig numbers with input files
	private FileMatcher fileMatcher = null; //Object that will look for files matching the pattern provided
	
	
	/**
	 * During initialization we determine the file pattern to be matched, but we only
	 * actually look for files when this method gets called..
	 */
	public void findFiles() {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);

		String projHome = System.getProperty("user.dir");
		if (! fileMatcher.inputPattern.startsWith("/")) {
			String projHomeAttr = properties.get(Pipeline.PROJECT_HOME);
			if (projHomeAttr != null) {
				projHome = projHomeAttr;
			}
		}
		
		File projDir = new File(projHome);
		File[] listing = projDir.listFiles(fileMatcher);
		
		if (listing.length == 0) {
			logger.severe("GlobFileBuffer with pattern " + fileMatcher.inputPattern + " matched zero files!");
		//	throw new IllegalArgumentException("Glob file buffer did not match any files, this is probably an error");
		}
		
		for(int i=0; i<listing.length; i++) {
			FileBuffer buffer = FileTypeGuesser.GuessFileType( listing[i]);
			if (guessContig) {
				String contig = guessContig(buffer);
				buffer.setContig(contig);
			}
			addFile( buffer );
		}
	}
	
	@Override
	public void initialize(NodeList children) {
		String guessAttr = properties.get(GUESS_CONTIG);
		if (guessAttr != null) {
			guessContig = Boolean.parseBoolean(guessAttr);
		}
		
		final String pattern = properties.get(FILENAME_ATTR);

		System.out.println("pattern is: " + pattern);
		fileMatcher = new FileMatcher(pattern, Pattern.compile(pattern));
		findFiles();
	}

	
	public class FileMatcher implements FilenameFilter {
		
		Pattern pattern;
		String inputPattern;
		public FileMatcher(String inputString, Pattern pattern) {
			this.inputPattern = inputString;
			this.pattern = pattern;
		}
		
		@Override
		public boolean accept(File dir, String name) {
			Matcher matcher = pattern.matcher(name);
//			if (matcher.matches()) {
//				System.out.println("Matcher matches name: " + name);
//			}
//			else {
//				System.out.println("Matcher does NOT match name: " + name);
//			}
			return matcher.matches();
		}
		
	}

}
