package operator;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.logging.Logger;

import pipeline.Pipeline;

public class BWASampe extends IOCommandOp {

	public static final String READ_GROUP = "readgroup";
	public static final String PATH = "path";
	protected String pathToBWA = "bwa";
	
	protected String defaultRG = "@RG\\tID:unknown\\tSM:unknown\\tPL:ILLUMINA";
	protected String readGroupStr = defaultRG;
	
	
	@Override
	protected String getCommand() {
		
		String bwaPathAttr = properties.get(PATH);
		if (bwaPathAttr != null) {
			pathToBWA = bwaPathAttr;
		}
			
		String rgStr = properties.get(READ_GROUP);
		if (rgStr != null) {
			readGroupStr = rgStr;
		}
		String referencePath = inputBuffers.get(0).getAbsolutePath();
		String reads1SAI = inputBuffers.get(1).getAbsolutePath();
		String reads2SAI = inputBuffers.get(2).getAbsolutePath();
		String reads1Path = inputBuffers.get(3).getAbsolutePath();
		String reads2Path = inputBuffers.get(4).getAbsolutePath();

		String command = pathToBWA + " sampe -r " + readGroupStr + " " + referencePath + " " + reads1SAI + " " + reads2SAI + " " + reads1Path + " " + reads2Path;
		return command;		
	}

}
