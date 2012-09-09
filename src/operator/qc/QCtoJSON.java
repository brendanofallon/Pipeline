package operator.qc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import json.JSONException;
import json.JSONObject;
import operator.OperationFailedException;
import operator.Operator;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import pipeline.PipelineObject;
import buffer.BAMFile;
import buffer.BAMMetrics;
import buffer.BEDFile;
import buffer.DOCMetrics;
import buffer.VCFFile;
import buffer.variant.VariantPool;

/**
 * Writes various QC info bits to a JSON formatted output file
 * @author brendan
 *
 */
public class QCtoJSON extends Operator {


	DOCMetrics rawCoverageMetrics = null;
	DOCMetrics finalCoverageMetrics = null;
	BAMMetrics rawBAMMetrics = null;
	BAMMetrics finalBAMMetrics = null;
	VariantPool variantPool = null;
	BEDFile captureBed = null;
	File jsonFile = null;
	
	@Override
	public void performOperation() throws OperationFailedException {
	
		String outputPath = this.getAttribute("filename");
		if (outputPath == null) {
			throw new OperationFailedException("No output path specified (use filename=path attribute)", this);
		}
		
		JSONObject qcObj = new JSONObject();
		if (rawCoverageMetrics != null) {
			try {
				qcObj.put("raw.coverage.metrics", rawCoverageMetrics.toJSONString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (finalCoverageMetrics != null) {
			try {
				qcObj.put("final.coverage.metrics", finalCoverageMetrics.toJSONString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		if (rawBAMMetrics != null) {
			try {
				qcObj.put("raw.bam.metrics", rawBAMMetrics.toJSONString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		if (finalBAMMetrics != null) {
			try {
				qcObj.put("final.bam.metrics", finalBAMMetrics.toJSONString());
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		File outputFile;
		if (outputPath.startsWith("/"))
			outputFile = new File( outputPath );  //output path is absolute
		else
			outputFile = new File(this.getProjectHome() + "/" + outputPath); //output path relative to proj home
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));
			writer.write( qcObj.toString() );
			writer.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new OperationFailedException("Could not write to output file " + outputFile.getAbsolutePath(), this);
		}
		
	}

	@Override
	public void initialize(NodeList children) {
		for(int i=0; i<children.getLength(); i++) {
			Node iChild = children.item(i);
			if (iChild.getNodeType() == Node.ELEMENT_NODE) {
				PipelineObject obj = getObjectFromHandler(iChild.getNodeName());
				if (obj instanceof BAMFile) {
					throw new IllegalArgumentException("Please supply a BamMetrics object, not a BAMFile object to the qc report (offending object:" + obj.getObjectLabel() +")");
				}
				
				if (obj instanceof BAMMetrics ) {
					if (rawBAMMetrics == null) {
						rawBAMMetrics = (BAMMetrics) obj;
					}
					else {
						if (finalBAMMetrics == null)
							finalBAMMetrics = (BAMMetrics) obj;
						else
							throw new IllegalArgumentException("Too many BAM metrics objects specified, must be exactly 2");
					}
					
				}
				
				if (obj instanceof DOCMetrics) {
					if (rawCoverageMetrics == null)
						rawCoverageMetrics = (DOCMetrics) obj;
					else {
						finalCoverageMetrics = (DOCMetrics) obj;
					}
				}
				if (obj instanceof VCFFile) {
					throw new IllegalArgumentException("Got a straight-up VCF file as input to QC metrics, this now needs to be a variant pool.");
				}
				if (obj instanceof VariantPool) {
					variantPool = (VariantPool)obj;
				}
				
				if (obj instanceof BEDFile) {
					captureBed = (BEDFile) obj;
				}
				// ?
			}
		}
		
		if (rawBAMMetrics == null) {
			throw new IllegalArgumentException("No raw BAM metrics objects specified");
		}
		
		if (finalBAMMetrics == null) {
			throw new IllegalArgumentException("No final BAM metrics objects specified");
		}
		
	}

}
