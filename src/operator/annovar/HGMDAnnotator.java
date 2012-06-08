package operator.annovar;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import buffer.variant.VariantRec;

import operator.OperationFailedException;
import operator.annovar.HGMDB.HGMDInfo;
import pipeline.Pipeline;

/**
 * An annotator that uses he HGMD db to find diseases associated with individual variants. We just
 * query the db to see if it contains a record for a given contig:position, and if so add an annotation
 * to the variant in question
 * @author brendan
 *
 */
public class HGMDAnnotator extends Annotator {
	
	public static final String HGMDB_PATH = "hgmd.path";
	
	HGMDB db = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
		Logger logger = Logger.getLogger(Pipeline.primaryLoggerName);
		if (db == null) {
			db = new HGMDB();
			Object hgmdObj =  Pipeline.getPipelineInstance().getProperty(HGMDB_PATH);
			if (hgmdObj== null) {
				throw new OperationFailedException("Could not initialize HGMD db, no path to db file specified (use " + HGMDB_PATH + ")", this);
			}
			File dbFile = new File(hgmdObj.toString());
			if (! dbFile.exists()) {
				throw new OperationFailedException("HGMD db file at path " + hgmdObj.toString() + " does not exist", this);
			}
			logger.info("Initializing hgmd db from file: " + dbFile.getAbsolutePath());
			try {
				db.initializeMap(dbFile);
			} catch (IOException e) {
				throw new OperationFailedException("Error reading HGMD db file at path " + hgmdObj.toString() + " : " + e.getMessage(), this);
			}
			
		}
		
		
		if (variants == null)
			throw new OperationFailedException("No variant pool specified", this);
		
		for(String contig : variants.getContigs()) {
			for(VariantRec rec : variants.getVariantsForContig(contig)) {
				annotateVariant(rec);
			}
		}
			
		
	}

	/**
	 * Add info from HGMD to the given variant under the annotation HGMD_INFO, if
	 * there is a record associated with the variant's position in the HGMD db. Otherwise,
	 * don't add anything. 
	 * @param rec
	 */
	public void annotateVariant(VariantRec rec) {
		if (db == null) {
			throw new IllegalStateException("DB not initialized");
		}
		
		HGMDInfo info = db.getRecord(rec.getContig(), rec.getStart());
		if (info != null) {
			String infoStr = info.condition + "," + info.hgmdID + "," + info.nm + "," + info.cDot;
			rec.addAnnotation(VariantRec.HGMD_INFO, infoStr);
		}
	}

}
