package util.bamreading;

import java.util.Iterator;

/**
 * Computes deviation in read position   
 * @author brendan
 *
 */
public class PosDevComputer extends VarCountComputer {

	//Buffers used to compute variance in single pass
	private final double[] M = new double[5];
	private final double[] S = new double[5];
	private final double[] K = new double[5];	
	
	@Override
	public String getName() {
		return "pos.dev";
	}

	@Override
	public Double[] computeValue(AlignmentColumn col) {
		values[A] = 0.0;
		values[C] = 0.0;
		values[T] = 0.0;
		values[G] = 0.0;
		values[N] = 0.0;
		
		M[A] = 0.0;
		M[C] = 0.0;
		M[T] = 0.0;
		M[G] = 0.0;
		M[N] = 0.0;
		
		S[A] = 0.0;
		S[C] = 0.0;
		S[T] = 0.0;
		S[G] = 0.0;
		S[N] = 0.0;
		
		K[A] = 0.0;
		K[C] = 0.0;
		K[T] = 0.0;
		K[G] = 0.0;
		K[N] = 0.0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();

			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					
					int readPos = read.refPosToReadPos(col.getCurrentPosition());
		
					int index = baseToIndex(b);
					K[index]++;
					
					double prevM = M[index];
					M[index] += (readPos - M[index])/K[index];
					S[index] += (readPos - prevM)*(readPos-M[index]);
					
					if (K[index] > 1)
						values[index] = S[index]/(K[index]-1);
				}
			}
		}
		
		return values;
	}

}

