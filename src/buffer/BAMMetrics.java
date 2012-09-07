package buffer;

import json.JSONException;
import json.JSONObject;
import json.JSONString;
import math.Histogram;

/**
 * Basically just storage for a bunch of summary info about a BAM file
 * @author brendan
 *
 */
public class BAMMetrics extends FileBuffer implements JSONString {

	public String path;
	public int totalReads;
	public Histogram insertSizeHistogram;
	public Histogram baseQualityHistogram;
	public Histogram[] readPosQualHistos;
	public int unmappedReads;
	public int unmappedMates;
	public int duplicateReads;
	public int lowVendorQualityReads;
	public int hugeInsertSize;
	public long basesRead;
	public long basesQAbove30;
	public long basesQAbove20;
	public long basesQAbove10;
	
	@Override
	public String getTypeStr() {
		return "BAM Metrics";
	}

	@Override
	public String toJSONString()  {
		JSONObject obj = null;
		try {
			obj = new JSONObject("{ \"total.reads\" : " + totalReads + "}");
			//obj.
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if (obj == null)
			return null;
		else 
			return obj.toString();
	}
	
//	public static JSONObject toKeyDouble(String key, int val) {
//		return new JSONStringer()
//		      .object()
//		         .key(key)
//		         .value(val)
//		      .endObject();
//	}

	
	public static void main(String[] args) {
		BAMMetrics bm  = new BAMMetrics();
		bm.totalReads = 100;
		
		System.out.println( bm.toJSONString() );
	}
}
