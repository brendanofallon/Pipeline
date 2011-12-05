package operator;

import buffer.BAMFile;
import buffer.FileBuffer;
import buffer.ReferenceFile;

/**
 * A class to build a little summary of the results of an exome alignment / variant call
 * @author brendan
 *
 */
public class ExomeSummary extends IOOperator {

	@Override
	public void performOperation() throws OperationFailedException {
		String referencePath = getInputBufferForClass(ReferenceFile.class).getAbsolutePath();
		BAMFile inputBam = (BAMFile) getInputBufferForClass(BAMFile.class);
		
		//Would be nice to use the GaTK's depth of coverage and maybe some variant file analysis stuff to 
		//make a short, readable summary of all the data...
	}



}
