package rankingService;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.Timer;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import buffer.variant.VariantRec;

public class RankingServiceTest {

	public static void main(String[] args) {
	
		
		Map<String, Integer> gomap = new HashMap<String, Integer>();
		Map<String, Integer> termsmap = new HashMap<String, Integer>();
		List<String> genes = new ArrayList<String>();
		
		gomap.put("angiogenesis", 10);
		gomap.put("vascular", 8);
		gomap.put("kinase", 2);
		
		termsmap.put("telangiectasia", 5);
		termsmap.put("angiogenesis", 5);
		termsmap.put("arteriovenous malformation", 10);
		termsmap.put("vascular malformation", 10);
		termsmap.put("capillary malformation", 10);
		
		
		genes.add("ENG");
		genes.add("ACVRL1");
		
		AnalysisSettings settings = new AnalysisSettings();
		settings.genes = genes;
		settings.goTerms = gomap;
		settings.summaryTerms = termsmap;
		settings.graphSize = 200;
		settings.pathToPipelineProperties = "/home/brendan/.pipelineprops.xml";
		settings.pathToVCF = "/home/brendan/workspace/Pipeline/testmed.vcf";
		settings.rootPath = "/home/brendan/workspace/Pipeline/testoutput";
		settings.prefix = "servicetest";
		
		try {
			final RankingServiceJob job = RankingService.submitRankingJob(settings);
			
			
			Timer timer = new Timer(1000, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent arg0) {
					checkForResults(job);
				}
			});
			
			job.execute(); //begin ranking job
			timer.start(); //start timer 
			job.get(); //block until job is finished
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Returning from main method");
	}

	protected static void checkForResults(RankingServiceJob job) {
		System.out.println("Checking job..");
		if (job.isDone()) {
			System.out.println("Job is done!");
			RankingResults results = job.getResult();
			List<VariantRec> rankedVars = results.variants;
			Collections.sort(rankedVars, new RankComparator());
			for(VariantRec var : rankedVars) {
				System.out.println(var + "\t" + var.getPropertyOrAnnotation(VariantRec.GO_EFFECT_PROD));
			}
		}
		else {
			System.out.println("Job not done yet");
		}
	}
	
	
}
