package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import math.Histogram;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.optimization.GoalType;
import org.apache.commons.math3.optimization.PointValuePair;
import org.apache.commons.math3.optimization.PointVectorValuePair;
import org.apache.commons.math3.optimization.direct.CMAESOptimizer;
import org.apache.commons.math3.optimization.direct.NelderMeadSimplex;
import org.apache.commons.math3.optimization.direct.SimplexOptimizer;

import cern.jet.random.Normal;
import cern.jet.random.Uniform;
import cern.jet.random.engine.MersenneTwister;
import cern.jet.random.engine.RandomEngine;

public class Optimizer implements MultivariateFunction {

	//static double[][] practiceA = new double[][]{{0.8, 0.022, 0.5}, {0.82, 0.1, 0.4}, {0.77, 0.05, 0.2}, {0.95, 0.01, 0.3}};
	//static double[][] practiceB = new double[][]{{0.1, 0.89, 0.4},{0.1, 0.915, 0.5},  {0.01, 0.8, 0.2},  {0.11, 0.9, 0.33}};
	
	List<double[]> practiceA;
	List<double[]> practiceB;
	
	Histogram histogramA = null;
	Histogram histogramB = null;
	

	RandomEngine rng = new MersenneTwister();
	Normal sanity = new Normal(0, 25, rng); //Just used to calculate normal pdf
	
	/**
	 * Return the probability that a variant with the given scores is in group A
	 * @param vec
	 * @return
	 */
	public static double sum(double[] vec, double[] weights) {
		double sum = 0;
		for(int i=0; i<vec.length; i++) {
			sum += vec[i]*weights[i];
		}
		
		//ALL TWO-WAY INTERACTIONS!
//		int index = vec.length;
//		for(int i=0; i<vec.length; i++) {
//			for(int j=i+1; j<vec.length; j++) {
//				sum += weights[index] * vec[i] * vec[j];
//				index++;
//			}
//		}
		//include one interaction term...
//		int interactor1 = 2; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		int interactor2 = 0; //4 = gerp
//		sum += weights[5] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 2; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 1; //4 = gerp
//		sum += weights[6] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 2; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 3; //4 = gerp
//		sum += weights[7] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 2; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 4; //4 = gerp
//		sum += weights[8] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 2; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 5; //4 = gerp
//		sum += weights[9] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 4; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 3; //4 = gerp
//		sum += weights[10] * vec[interactor1] * vec[interactor2];
//		
//		interactor1 = 5; //0 = sift, 1 = pp, 2=mt, 3=phylop, 4=gerp
//		interactor2 = 4; //4 = gerp
//		sum += weights[11] * vec[interactor1] * vec[interactor2];

		return sum;
	}
	
	public void loadDataA(File file) throws IOException {
		practiceA = new ArrayList<double[]>(1024);
		loadData(file, practiceA);
		System.out.println("Loaded " + practiceA.size() + " variants from " + file.getName());
	}

	public void loadDataB(File file) throws IOException {
		practiceB = new ArrayList<double[]>(1024);
		loadData(file, practiceB);
		System.out.println("Loaded " + practiceB.size() + " variants from " + file.getName());
	}
	
	private void loadData(File file, List<double[]> vals) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(file));
		String line = reader.readLine();
		while(line != null) {
			String[] toks= line.split("\t");
			try {
				Double sift = Double.parseDouble(toks[14]);
				Double pp = Double.parseDouble(toks[15]);
				Double mt = Double.parseDouble(toks[16]);
				Double phylop = Double.parseDouble(toks[17]);
				Double gerp = Double.parseDouble(toks[18]);
				Double fx = Double.parseDouble(toks[19]);
				double[] arr = new double[6];
				arr[0] = sift;
				arr[1] = pp;
				arr[2] = mt;
				arr[3] = phylop;
				arr[4] = gerp;
				arr[5] = fx;
				vals.add(arr);
			}
			catch (NumberFormatException nfe) {
				//dont stress it
			}
			
			line = reader.readLine();
		}
	}
	
	public double classify(double[] weights) {
		Histogram histoA = new Histogram(-50, 50, 50);
		Histogram histoB = new Histogram(-50, 50, 50);
		
		double penalty = 0;
		
		for(int i=0; i<practiceA.size(); i++) {
			double val = sum(practiceA.get(i), weights);
			
			histoA.addValue(val);
		}
		
		for(int i=0; i<practiceB.size(); i++) {
			double val = sum(practiceB.get(i), weights);
			histoB.addValue(val);
		}
		
		for(int i=0; i<weights.length; i++) {
			penalty += 1.0 / (sanity.pdf(weights[i])+1);
		}
		
		double meanA = histoA.getMean();
		double meanB = histoB.getMean();
		double stdEvA = histoA.getStdev();
		double stdEvB = histoB.getStdev();
		
		double res = (stdEvA + stdEvB+0.1)/(Math.abs(meanA - meanB) + 0.1) + penalty ;
		
		
		return res;
	}

	
	private double computeHistoOverlap(Histogram histoA, Histogram histoB) {
		double sum = 0;
		for(int i=0; i<histoA.getBinCount(); i++) {
			sum += histoA.getFreq(i)*histoB.getFreq(i);
		}
		
		sum += histoA.getLessThanMin() / histoA.getCount() *  histoB.getLessThanMin() / histoB.getCount(); 
		sum += histoA.getMoreThanMax() / histoA.getCount() *  histoB.getMoreThanMax() / histoB.getCount();
		
		return sum /(double) histoA.getBinCount();
	}

	@Override
	public double value(double[] point) {
		return classify(point);
	}
	
	public void emit(double[] w) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter("/home/brendan/hgmd_test/hgmdvals.csv"));
		System.out.println("Disease variants");
		for(int i=0; i<practiceA.size(); i++) {
			writer.write(sum(practiceA.get(i), w) + "\n");
		}
		writer.close();
		
		writer = new BufferedWriter(new FileWriter("/home/brendan/hgmd_test/esp5400.csv"));
		for(int i=0; i<practiceB.size(); i++) {
			writer.write(sum(practiceB.get(i), w) + "\n");
		}
		writer.close();
	}
	
	public void generateHistograms(double[] weights) {
		histogramA = new Histogram(-50, 50, 50);
		histogramB = new Histogram(-50, 50, 50);
		
		
		for(int i=0; i<practiceA.size(); i++) {
			double val = sum(practiceA.get(i), weights);
			histogramA.addValue(val);
		}
		
		for(int i=0; i<practiceB.size(); i++) {
			double val = sum(practiceB.get(i), weights);
			histogramB.addValue(val);
		}
	}

	public Histogram getHistoA(double[] w) {
		generateHistograms(w);
		return histogramA;
	}
	
	public Histogram getHistoB(double[] w) {
		generateHistograms(w);
		return histogramB;
	}
	
	/**
	 * The relative probability that the given variant came from A
	 * @param histA
	 * @param histB
	 * @param values
	 * @param weights
	 * @return
	 */
	public double testA(double[] values, double[] weights) {
		
		double val = sum(values, weights);
		int bin = histogramA.getBin(val);
		
		double probA = histogramA.getFreq(bin);
		double probB = histogramB.getFreq(bin);
		return probA / (probA + probB);
	}
	
	
	
	public static void main(String[] args) {
		CMAESOptimizer optim = new CMAESOptimizer();
		//SimplexOptimizer optim = new SimplexOptimizer();
		//optim.setSimplex(new NelderMeadSimplex(12));
		
		double[] bestWeights = null;
		double bestVal = 1000.0;
		
		//double[] startVals = new double[21];
		double[] startVals = new double[]{1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
		
		DecimalFormat formatter = new DecimalFormat("#0.0#");
		
		RandomEngine rng = new MersenneTwister();
		Uniform uni = new Uniform(rng);
		
		try {
			Optimizer func = new Optimizer();
			//func.loadDataA(new File("/home/brendan/hgmd_test/hgmd.analysis.csv"));
			//func.loadDataB(new File("/home/brendan/hgmd_test/highfreqs.0.1.analysis.csv"));
	
			func.loadDataA(new File("/home/brendan/hgmd_test/hgmd.dmonly.analysis.csv"));
			func.loadDataB(new File("/home/brendan/hgmd_test/tgk.esp5400.combo.analysis.csv"));
			
			//func.emit(new double[]{0, 0, 0, 0, 1, 0}); 
			//func.emit(new double[]{-8, -0.1, 23, -7.8, 0.6, 3.9}); //This one is good for no interaction term
			
			//func.emit(new double[]{-20, 5, 15, -9, -2.5, 4.74, 6.29}); //sift / gerp interaction, sum = 2.05e-4
			
			//Current best, using as mt interactions
			//func.emit(new double[]{-11, 0.6, 10, -11, -0.5, 7, 12, 1.6, 3, -2.39, -5.5, 2.0}); //sift / gerp interaction, sum = 2.05e-4
		
			//All two-way interactions 
			//func.emit(new double[]{-35.26,-6.27,-15.49,-10.94,0.68,8.63,17.2,34.63,-7.04,1.06,-5.86,24.78,-7.02,0.37,1.91,34.29,-0.52,-2.98,-0.64,-4.37,0.53});
			
			//All two-way interactions, new weighting
			
			//func.emit(new double[]{2.74,0.84,-5.3,2.23,-0.32,-1.36,-3.53,-5.02,0.99,0.12,0.42,-4.8,0.4,-0.28,0.6,-1.47,0.51,-0.2,-0.04,0.26,-0.09});
			
			
			//Linear only, new function ... this one is almost exactly equivalent to the all-interactions version,
			//and is slightly better than linear-only histogram-overlap function 
			//func.emit(new double[]{-0.05,1.46,6.29,-1.48,-0.03,0.87});	
			func.emit(new double[]{-0.5,1.46,6.29,-1.48,-0.3,0.87});
			System.exit(0);
			
			for(int i=0; i<1000; i++) {
				PointValuePair res = optim.optimize(100000, func, GoalType.MINIMIZE, startVals);
				double[] sol = res.getPoint();
				System.out.print(i + ":\t");
				for(int j=0; j<sol.length; j++) {
					System.out.print(sol[j] + "\t");
				}
				System.out.println(" sum : " + res.getValue());
				
				if ((!res.getValue().isInfinite()) && res.getValue() < bestVal) {
					bestWeights = res.getPoint();
					bestVal = res.getValue();
				}
				System.out.print("Best so far: \t");
				for(int j=0; j<bestWeights.length; j++) {
					System.out.print(formatter.format(bestWeights[j]) + "\t");
					
				}
				System.out.println(" sum : " + bestVal);
				Histogram a = func.getHistoA(bestWeights);
				Histogram b = func.getHistoB(bestWeights);
				System.out.println("Mean of A: "+ formatter.format(a.getMean()) + " stdev: " + formatter.format(a.getStdev()));
				System.out.println("Mean of B: "+ formatter.format(b.getMean()) + " stdev: " + formatter.format(b.getStdev()));
				
				System.arraycopy(bestWeights, 0, startVals, 0, bestWeights.length);
				for(int j=0; j<startVals.length; j++) {
					double mul = 5;
					if (i%10==0)
						mul = 25;
					startVals[j] += mul*(uni.nextDouble()-0.5);
				}
			}
			
			
			System.out.println("Best found : " );
			for(int j=0; j<bestWeights.length; j++) {
				System.out.println(bestWeights[j]);
			}
			System.out.println("Value : " + bestVal);
			

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}


	
	
}
