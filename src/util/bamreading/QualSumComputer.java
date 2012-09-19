package util.bamreading;

import java.util.Iterator;

public class QualSumComputer extends VarCountComputer {
	
	@Override
	public String getName() {
		return "quality.sums";
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
					byte q = read.getQualityAtReferencePos(col.getCurrentPosition());
					values[baseToIndex(b)] += q;
				}
			}
		}
		
		return values;
	}

}
