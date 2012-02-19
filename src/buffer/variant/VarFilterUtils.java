package buffer.variant;

/**
 * Contains static utils for obtaining a variety of oft-used variant filters
 * @author brendan
 *
 */
public class VarFilterUtils {
	
	public static VariantFilter getQualityFilter(double minQual) {
		return new QualFilter(minQual);
	}

	public static VariantFilter getHeteroFilter() {
		return new HetFilter();
	}
	
	public static VariantFilter getHomoFilter() {
		return new HomFilter();
	}

	
	static class QualFilter implements VariantFilter {

		final double qual;
		
		public QualFilter(double minQual) {
			this.qual = minQual;
		}
		
		@Override
		public boolean passes(VariantRec rec) {
			return rec.getQuality() > qual;
		}
		
	}

	
	static class HomFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			return !rec.isHetero();
		}
		
	}

	
	static class HetFilter implements VariantFilter {

		@Override
		public boolean passes(VariantRec rec) {
			return rec.isHetero();
		}
		
	}
	
}
