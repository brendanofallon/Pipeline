package util.bamreading;

import java.util.Iterator;

/**
 * Counts the number of times each base appears
 * @author brendan
 *
 */
public class VarCountComputer implements ColumnComputer {

	final Double[] values = new Double[5];
	
	static final int A = 0;
	static final int C = 1;
	static final int G = 2;
	static final int T = 3;
	static final int N = 4;
	
	
	
	@Override
	public String getName() {
		return "var.counts";
	}

	@Override
	public Double[] computeValue(AlignmentColumn col) {
		values[A] = 0.0;
		values[C] = 0.0;
		values[T] = 0.0;
		values[G] = 0.0;
		values[N] = 0.0;
		
		if (col.getDepth() > 0) {
			Iterator<MappedRead> it = col.getIterator();
			while(it.hasNext()) {
				MappedRead read = it.next();
				if (read.hasBaseAtReferencePos(col.getCurrentPosition())) {
					byte b = read.getBaseAtReferencePos(col.getCurrentPosition());
					values[baseToIndex(b)]++;
				}
			}
		}
		
		return values;
	}

	static int baseToIndex(byte b) {
		switch(b) {
			case 'A' : return A;
			case 'C' : return C;
			case 'G' : return G;
			case 'T' : return T;
		}
		
		return N;
	}
}
