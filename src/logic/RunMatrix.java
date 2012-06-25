package logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

public class RunMatrix {
	
	private double[][] inputMatrix; 	//inputMatrix is the data matrix
	private int N_tot;				//N_tot is the number of measurements
	private int V_tot;				//V_tot is the number of variables	
	
	Double[][] normalizationValues;
	private double buildToTestRatio;
	
	private Random rand;
	
	public double a;				//a is a parameter for the sample problems
	public double b;				//b is the signal to noise parameter for the sample problems
	
	public RunMatrix()
	{
		buildToTestRatio = 0.75; // The part of N_tot which is used for build data between 0.5 - 0.9
		a = 0.5;
		b = 0.2;
	}
	
	public double[][] GetInputMatrix() {
		return this.inputMatrix;
	}
				
	public void setInputMatrix(
				final double[][] _inputMatrix, //inputMatrix is the data matrix
				final int _N_tot, 				//N_tot is the number of measurements
				final int _V_tot				//V_tot is the number of variables
				)
	{
				
		//number of inputs is the columns in the inputMatrix - the output			
		inputMatrix = _inputMatrix;
		N_tot = _N_tot;
		V_tot = _V_tot;
			
		//printMatrix(inputMatrix,N_tot,V_tot);
		normalizeMatrix(-1.0, 1.0);			
	}
	
	/* 
	 * The following code will shuffle the data set
	 * so it can be divided into unbiased testing 
	 * and building sets.
	 */
	public double[][] ShuffleDataMatrix(){
		Integer[] shuffleArray = new Integer[N_tot];
		
		for(Integer i = 0; i<N_tot; i++){
			shuffleArray[i] = i;
		}
		//shuffle the indexarray shuffleArray
		Collections.shuffle(Arrays.asList(shuffleArray));
		//shuffle the inputMatrix the same way as the shuffleArray
		double[][] tempMatrix = new double[N_tot][V_tot];
		for(Integer i = 0; i<N_tot; i++){
			for(Integer j = 0; j<V_tot; j++){
				tempMatrix[i][j] = inputMatrix[shuffleArray[i]][j];
			}
			
		}

		return tempMatrix;
	}
	
	protected void normalizeMatrix(double normMin, double normMax)
	{
		normalizationValues = new Double[V_tot+1][2];
		normalizationValues[V_tot][0] = normMin;
		normalizationValues[V_tot][1] = normMax;
		double min, max; 
		
		for(Integer j = 0; j<V_tot; j++){
			min =  100000; max = -100000;
			//find max and min values
			for(Integer i = 0; i<N_tot; i++){
				if(min > inputMatrix[i][j]){
					min = inputMatrix[i][j];
				}
				if(max < inputMatrix[i][j]){
					max = inputMatrix[i][j];
				}
			}
			normalizationValues[j][0] = min;
			normalizationValues[j][1] = max;
			//normalize each column
			for(Integer i = 0; i<N_tot; i++){
				inputMatrix[i][j] = (inputMatrix[i][j] - min)/((max-min)/(normMax-normMin)) + normMin;
			}
			
		}
	}
	
	/*
	 * Turn the inputMatrix into a x (input) and y (output) part
	 * which in turn are sliced into a build and train part
	 */
	protected void splitInputMatrix(double[][] x_build, double[][] x_test, double[][] y_build, double[][] y_test, int N_build, int outNodes){
		
		//giving the values to X
		for(int i = 0; i < V_tot - outNodes; i++){
			for(int j = 0; j < N_tot; j++){
				if(j < N_build){
					x_build[j][i] = inputMatrix[j][i];
				} else{
					x_test[j-N_build][i] = inputMatrix[j][i];
				}
			}
		}
		//giving the values to Y
		for(int i = V_tot - outNodes; i < V_tot; i++){
			for(int j = 0; j < N_tot; j++){
				if(j < N_build){
					
					y_build[j][i-V_tot + outNodes] = inputMatrix[j][i];
				} else{
					y_test[j-N_build][i-V_tot + outNodes] = inputMatrix[j][i];
				}
			}
		}
	}
	
	public Double GetNormalizationMax(Integer index)
	{
		return normalizationValues[index][1];
	}
	
	public Double GetNormalizationMin(Integer index)
	{
		return normalizationValues[index][0];
	}
	
	
	
	
	//TODO: detta hör ej hemma här
	public void makeSampleProblem(
			final int sampleProblem //sets a problem to be used
			){
		
		buildToTestRatio = 0.75; // The part of N_tot which is used for build data between 0.5 - 0.9
		
		
		N_tot = 100;
		boolean noProblem = true;
		
		//V_tot it the number of inputs + the output
		switch(sampleProblem){
		case 1:
			V_tot = 4;
			inputMatrix = new double[N_tot][V_tot];
			for(Integer i = 0; i<N_tot; i++){
				//the inputs
				for(Integer j = 0; j<V_tot-1; j++){
					inputMatrix[i][j] = rand.nextGaussian();
				}
				//y_1 = a * (x_1^2 0.5 * x_1 * x_2) + (1-a) * (0.5*x_2*x_3 + x_3^2) + b*e
				inputMatrix[i][V_tot-1] = a*( Math.pow(inputMatrix[i][0],2) + 0.5*inputMatrix[i][0]*inputMatrix[i][1] )
						+ (1-a) * ( 0.5 * inputMatrix[i][1]*inputMatrix[i][2] + Math.pow(inputMatrix[i][2],2) )
						+ b * rand.nextGaussian();
			}
			
			
			break;
		case 2:
			V_tot = 3;
			inputMatrix = new double[N_tot][V_tot];
			for(Integer i = 0; i<N_tot; i++){
				//the inputs
				for(Integer j = 0; j<V_tot-1; j++){
					inputMatrix[i][j] = rand.nextGaussian();
				}
				//y_2 = log(x_1 + 4) + sqrt(x_2 + 4) + b*e
				inputMatrix[i][V_tot-1] = Math.log(inputMatrix[i][0] + 4)
						+ Math.sqrt(inputMatrix[i][1] + 4)
						+ b*rand.nextGaussian();
			}
								
			break;
		case 3:
			V_tot = 3;
			inputMatrix = new double[N_tot][V_tot];
			for(Integer i = 0; i<N_tot; i++){
				//the inputs
				for(Integer j = 0; j<V_tot-1; j++){
					inputMatrix[i][j] = rand.nextGaussian();
				}
				//y_3 = -0.5 + 0.2*x_1^2 - 0.1*e^(x_2) + b*e
				inputMatrix[i][V_tot-1] = -0.5 + 0.2 * Math.pow(inputMatrix[i][0],2)
						- 0.1 * Math.exp(inputMatrix[i][1])
						+ b*rand.nextGaussian();
			}
			
			break;
			
			//if none of the problems are picked
		default:
			System.out.println("Used a non-existing sampleProblem number!");
			noProblem = false;
		
		}
		normalizeMatrix(-1.0, 1.0);
		

	}
	
	

}
