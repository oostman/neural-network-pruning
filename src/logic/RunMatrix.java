package logic;

import java.util.Arrays;
import java.util.Collections;

public class RunMatrix {
	
	private double[][] inputMatrix; 	//inputMatrix is the data matrix
	private int N_tot;					//N_tot is the number of measurements
	private int V_tot;					//V_tot is the number of variables	
	
	private Double[][] normalizationValues;
	private double buildToTestRatio;
	
	int N_build;
	int N_test;
		
	public double[][] x_build;
	public double[][] x_test;
	public double[][] y_build;
	public double[][] y_test;
	
    public RunMatrix()
	{
		buildToTestRatio = 0.75; // The part of N_tot which is used for build data between 0.5 - 0.9
		
	}
	
    public void NewRun(int outNodes)
	{
		this.N_build = (int)(N_tot * buildToTestRatio);
		this.N_test = N_tot - N_build;
		
		/* 
		 * The following function will shuffle the data set
		 * so it can be divided into unbiased testing 
		 * and building sets.
		 */
		this.ShuffleDataMatrix();
		
		/*
		 * Turn the inputMatrix into a x (input) and y (output) part
		 * which in turn are sliced into a build and train part
		 */
		x_build = new double[N_build][V_tot - outNodes];
		x_test = new double[N_test][V_tot - outNodes];
		y_build = new double[N_build][outNodes];
		y_test = new double[N_test][outNodes];
		this.splitInputMatrix(x_build, x_test, y_build, y_test, N_build, outNodes);
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
		
	public int GetNumberOfVariables()
	{
		return this.V_tot;
	}

	public Double[][] GetNormalizationMatrix() {
		return this.normalizationValues;
	}
	
	

}
