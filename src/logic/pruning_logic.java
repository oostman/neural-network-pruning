package logic;

import java.util.Arrays;

import java.util.Collections;
import java.util.Random;
import Jama.*;

public class pruning_logic{
	
	private RunStatistics runStatistics;
	private RunSettings runSettings;
	private RunMatrix runMatrix;
	
	private Integer[][] bestInputNodes;
	
	private double buildToTestRatio;
	private Random rand;
	private int numberOfRuns; 		//how many runs the pruning algorithm should make
	
	private int N_tot;				//N_tot is the number of measurements
	private int V_tot;				//V_tot is the number of variables		
	
	
	
	private boolean cancelPruning;	// a boolean that can be used to cancel a pruning in progress
	
	
	/*
	 * a function for giving the input matrix
	 */
	public void setInputMatrix(
			final double[][] inputMatrix, //inputMatrix is the data matrix
			final int N_tot, 				//N_tot is the number of measurements
			final int V_tot				//V_tot is the number of variables
			){


		runMatrix.setInputMatrix(inputMatrix, N_tot, V_tot);
		
		this.runSettings = new RunSettings(V_tot - 1);
						
		
	}
	public void makeSampleProblem(
			final int sampleProblem //sets a problem to be used
			){
		
		this.runMatrix.makeSampleProblem(sampleProblem);

		do_prunings();

	}
	
	/*
	 * The constructor
	 */
	public pruning_logic(){
		
		
		this.runSettings = new RunSettings(V_tot - 1);
		
		
		
		numberOfRuns = 5;
		
		cancelPruning = false;
		
		//a = 0.5;
		//b = 0.2;
		runMatrix = new RunMatrix();
		rand = new Random();
		
	}
	
	/*
	 * This is the function which is called from the outside to start the pruning.
	 * It controls the number of pruning runs.
	 */
	public void do_prunings(){
		
		//number of inputs is the columns in the inputMatrix - the output
		this.runSettings = new RunSettings(V_tot - 1);
		
		makingThePruning oneInstance = new makingThePruning();
		oneInstance.start();
	}
	private class makingThePruning extends Thread{
		public void run(){
			cancelPruning = false;
			System.out.println("start of the prunings");
			//printMatrix(inputMatrix, N_tot, V_tot);
			
			//the errors for each pruning need to be initialized here since nhid/nin can change after the constructor
			//the error at all_err[0] will be the error for one weight ... and so on
			
			runStatistics = new RunStatistics(runSettings.getNumberOfNodes(), numberOfRuns);
			
			//the best inputs nodes at all different weights left
			bestInputNodes = new Integer[runSettings.getNumberOfNodes()][runSettings.getNin()];
			for(Integer i = 0; i<runSettings.getNin(); i++)
				for(Integer j = 0; j<runSettings.getNumberOfNodes(); j++)
					bestInputNodes[j][i] = 0;
				
			for(int i = 0; i < numberOfRuns; i++){
				//perform the pruning
				perform_pruning(); 
				//save the results
				runStatistics.AddLastPruningRunToTotalStatistics();
			}
			
			revertNormalizeOnError(runStatistics.total_all_err, V_tot-1);
			revertNormalizeOnError(runStatistics.total_all_err_test, V_tot-1);
			
			
			runStatistics.SetFinnished();

		}
		
		/*
		 * perform_pruuning is as the name suggests were the pruning takes place
		 */
		protected void perform_pruning(){
			
			//printfMatrix(inputMatrix, N_tot, V_tot);
			
			int N_build = (int)(N_tot * buildToTestRatio);
			int N_test = N_tot - N_build;
			
			
			/* 
			 * The following function will shuffle the data set
			 * so it can be divided into unbiased testing 
			 * and building sets.
			 */
			runMatrix.ShuffleDataMatrix();

			/*
			 * Turn the inputMatrix into a x (input) and y (output) part
			 * which in turn are sliced into a build and train part
			 */
			final double[][] x_build = new double[N_build][V_tot - runSettings.getNumberOfOutNodes()];
			final double[][] x_test = new double[N_test][V_tot - runSettings.getNumberOfOutNodes()];
			final double[][] y_build = new double[N_build][runSettings.getNumberOfOutNodes()];
			final double[][] y_test = new double[N_test][runSettings.getNumberOfOutNodes()];
			runMatrix.splitInputMatrix(x_build, x_test, y_build, y_test, N_build, runSettings.getNumberOfOutNodes());
			
			Matrix YbMatrix = new Matrix(y_build);
			
			//Setup the lower-layers weights as random gaussian values, with mean 1 and variance 2
			Double[][] weights = initWeights();
			
			//Determine the output from the hidden layer
			double[][] s = hiddenLayerOutput(weights, x_build, N_build);
			
			/*
			 * starts the pruning stage
			 */
			
			//weights_left is a matrix keeping track on which weights that are left, 1 means that an weight exists
			Integer[][] weights_left = (Integer[][]) fillMatrix(runSettings.getNhid(), runSettings.getNin(), 1);
			//nodes_left is the vector keeping track on which hidden nodes that are still being used, 1 means it is used
			Integer[] nodes_left = (Integer[]) fillArray(runSettings.getNhid(),1);
			
			//the weight matrix i cloned into the ww, matrix ... for no apparent reason
			Double[][] ww = cloneMatrix(weights, runSettings.getNhid(), runSettings.getNin() + 1);
			
			//printEndOfMatrix(weights, nhid, nin+1);
			
			//number of weights in lower layer
			int numberOfWeights = runSettings.getNumberOfNodes();
			
			//err is the current error and err_min is the smallest error
			double err_min, err;
			int i_min = 0, j_min = 0;
			
			double[][] inputs;
			
			Matrix W_min = null, W = null;
			
			double[][] sss = null;
			int helthyColNode = 0;
			// the loop that eliminates the weights one at a time until there is only one weight left
			while(numberOfWeights > 1){
				
				saveBestInputNodes(weights_left,numberOfWeights);
				
				if(cancelPruning){
					runStatistics.CancelRun();
					return;
				}
				err_min = 10000.1;
				err = 10000.0;
				W_min = null;
				
				//calculate the net input of the hidden nodes, inputs is a double[N_build][nhid].
				double[][] s_orig = new double[N_build][runSettings.getNhid() + 1];
				inputs = calcHiddenNodesInput(ww, s_orig, x_build, N_build);
				
				for(Integer i = 0; i<runSettings.getNhid(); i++){
					for(Integer j = 1; j < runSettings.getNin() + 1; j++){
						Double[][] www = cloneMatrix(ww, runSettings.getNhid(), runSettings.getNin() + 1);
						s = cloneMatrix(s_orig, N_build, runSettings.getNhid() + 1);
						//checks if weight is already pruned/eliminated
						if(www[i][j] == 0){
							//If, yes then don't waste time on it
							err = 10000.0;	
						}else{
							runStatistics.IncrementProgress();
							www[i][j] = 0.0;	//prune one weight
							
							//calculate the modified net input and output of the hidden nodes
							s = calcModHidNodesInput(s, ww, x_build, inputs, N_build, i, j);
							
							/* 
							 * Selects the columns corresponding to the 
							 * connected hidden nodes.
							 */
							helthyColNode = numberOfHealthyRows(www);
							sss = removeDeadColsHidNodesInput(s, www, N_build, helthyColNode);

							//Solve the upper layer weights by least-squares. 
							Matrix A = new Matrix(sss);
							try{
							W = A.solve(YbMatrix);
							}
							catch(Exception e){
								printMatrix(s, N_build, helthyColNode+1);
								System.out.println("\n"+helthyColNode);
								W = A.solve(YbMatrix);
							}
							/*
							 * the residuals and error
							 */
							Matrix Y_hat = A.times(W);
							err = ( YbMatrix.minus(Y_hat).norm2() ) /Math.sqrt(N_build);
							
						}//else loop ends here
						
						if(err < err_min){		//note the best network
							err_min = err;
							i_min = i;
							j_min = j;
							W_min = W;
							
						}
					}
				} //the "master" for loop ends
				//one weight has been removed ...
				numberOfWeights--;
				
				ww[i_min][j_min] = 0.0; 		//removing the least helping weight
				weights_left[i_min][j_min-1] = 0;
				
				//test if there are any non-zero connections for the i_min:th hidden node
				nodes_left[i_min] = testForNonZeroConnections(weights_left, i_min);
				
				//saves the error for each iteration, i.e. the removing of an weight
				runStatistics.StoreMinErrorFor(err_min, numberOfWeights);
				
				/*
				 * Evaluate the model on the test data //y_test = new double[N_test][nout]
				 */
				double[][] y_test_hat = modelOnTheTestData(N_test, ww, nodes_left, W_min, x_test);
				
				Matrix Y_test_hat = new Matrix(y_test_hat);
				Matrix Y_test_matrix = new Matrix(y_test);
				// should be saved as 0,1,2,3, ....
				runStatistics.all_err_test[numberOfWeights - 1] = ( Y_test_matrix.minus(Y_test_hat).norm2() ) /Math.sqrt(N_test);
				/*if(all_err_test[nhid*nin - numberOfWeights] > 10)
				{
					System.out.println(all_err_test[nhid*nin - numberOfWeights]+" "+numberOfWeights+" "+W_min.get(0,0)+" "+W_min.get(1,0)+" "+W_min.get(2,0)+" "+W_min.get(3,0));
					printMatrix(inputs, N_build, nhid);
					System.out.println();
				}*/
				
				
			}//the master while loop ends
			
			
			// TODO: write status? System.out.println("one pruning is done progress: "+progress+"/"+totalIterations);

		}
	}
	
	
	/*
	 * The following code will normalize the data set to fit between the min an max values
	 */
	
	/*
	 * The following code will revert the normalize on a array which represent an column, or for instance the errors
	 */
	protected void revertNormalizeOnError(double[] array, int index){
		double normMin = runMatrix.GetNormalizationMin(V_tot);
		double normMax = runMatrix.GetNormalizationMax(V_tot);
		double min, max; 

		min = runMatrix.GetNormalizationMin(index);
		max = runMatrix.GetNormalizationMax(index);
		//normalize each element in the array
		for(Integer i = 0; i<runSettings.getNumberOfNodes() - 1; i++){
			array[i] = array[i] / (normMax-normMin) * (max-min);
		}
			
	}
	
	
	
	
	
	/*
	 * Sets up the lower-layer weights as random gaussian values
	 * with the mean 1 and variance 2
	 */
	protected Double[][] initWeights(){
		Double[][] weights = new Double[runSettings.getNhid()][runSettings.getNin()+1];
		for(Integer i = 0; i < runSettings.getNhid(); i++){
			for(Integer j = 0; j < runSettings.getNin() + 1; j++){
				weights[i][j] = 1 - 2* rand.nextGaussian();
			}
		}
		return weights;
	}
	
	/*
	 * Determinate the output from the hidden layers inside of the build set.
	 */
	protected double[][] hiddenLayerOutput(Double[][] weights, double[][] x_build, int N_build){
		Double[] input = new Double[runSettings.getNhid()];
		double[][] s = new double[N_build][runSettings.getNhid() + 1];
		for(Integer i = 0; i<N_build; i++){
			s[i][0] = 1.0;	//bias
		}
		for(Integer i = 0; i<N_build; i++){
			for(Integer j = 0; j < runSettings.getNhid(); j++){
				input[j] = weights[j][0]; //bias
				for(Integer k = 1; k < runSettings.getNin() + 1; k++){
					input[j] += weights[j][k] * x_build[i][k-1]; //i ex är det x(i,k-1);
				}
				//hidden node outputs with the weight 1 - true?
				s[i][j+1] = 1/(1+Math.exp(-input[j]));
			}
		}
		return s;
	}
	/*
	 * calculates the net input of the hidden nodes
	 */
	protected double[][] calcHiddenNodesInput(Double[][] ww, double[][] S_orig , double[][] x_build, int N_build){
		double[][] inputs = new double[N_build][runSettings.getNhid()];
		for(Integer i = 0; i<N_build; i++){
			S_orig[i][0] = 1.0;	//bias
		}
		for(Integer i = 0; i<N_build; i++){
			for(Integer j = 0; j < runSettings.getNhid(); j++){
				inputs[i][j] = ww[j][0]; //bias
				for(Integer k = 1; k < runSettings.getNin() + 1; k++){
					inputs[i][j] += ww[j][k] * x_build[i][k-1];
				}
				S_orig[i][j+1] = 1/(1+Math.exp(-inputs[i][j]));
			}
		}
		return inputs;
	}
	/*
	 * calculate the modified net input and output of the hidden nodes
	 */
	protected double[][] calcModHidNodesInput(double[][] s, Double[][] ww, double[][] x_build, double[][] inputs, int N_build, int i, int j){
		double[][] input_new = new double[N_build][runSettings.getNhid()];
		for(Integer k = 0; k < N_build; k++){
			input_new[k][i] = inputs[k][i] - ww[i][j]*x_build[k][j-1];
			s[k][i+1] = 1/(1+Math.exp(-input_new[k][i]));
		}
		return s;
	}
	
	/*
	 * Calculates how many healthy rows there are in www
	 */
	protected int numberOfHealthyRows(Double[][] www){
		int tempsize = 0;
		for(Integer k = 0; k < runSettings.getNhid(); k++){
			for(Integer l = 1; l<runSettings.getNin() + 1; l++){				// l = 0 zero is the bias
				if(www[k][l] != 0){
					tempsize++;
					break;
				}
			}
			
		}
		return tempsize;
	}
	
	/*
	 * Removes the "dead" columns from the Hidden node outputs, 
	 * i.e. if one node is eliminated the input matrix should be one column smaller
	 */
	protected double[][] removeDeadColsHidNodesInput(double[][] s, Double[][] www, int N_build, int tempsize){
		double[][] sss = new double[N_build][tempsize+1]; //tempsize + 1, since my s includes an bias column
		for(Integer m = 0; m < N_build; m++){
			sss[m][0] = s[m][0];
		}
		int temp = 1;
		for(Integer k = 0; k < runSettings.getNhid(); k++){
			for(Integer l = 1; l<runSettings.getNin()+1; l++){				// l = 0 zero is the bias
				if(www[k][l] != 0){
					for(Integer m = 0; m < N_build; m++){
						sss[m][temp] = s[m][k+1];
					}
					temp++;
					break;
				}
			}
		}
		return sss;
	}
	
	/*
	 * A function that fixes the issue with singular matrices
	 */
/*	private Matrix makeInvertable(Matrix matrix, int row, int col){
		
		for(Integer j=0; j<row; j++){
			for(Integer i=1; i<col; i++){
				matrix.set(j,i, matrix.get(j,i) + 0.00001*rand.nextGaussian());
			}
		}
		return matrix;
	}*/
	/*
	 * test if there are any non-zero connections for the i_min:th hidden node
	 */
	protected int testForNonZeroConnections(Integer[][] weights_left, int i_min){
		for(Integer i = 0; i<runSettings.getNin(); i++){
			if(weights_left[i_min][i] != 0){
				return 1;
			}
		}
		return 0;
	}
	protected double[][] modelOnTheTestData(final int N_test, final Double[][] ww, final Integer[] nodes_left, final Matrix W_min, final double[][] x_test){
		/*
		 * Evaluate the model on the test data //y_test = new double[N_test][nout]
		 */
		Integer nodeNumr;
		double[][] s_test = new double[N_test][runSettings.getNhid()];
		double[] input_test = new double[runSettings.getNhid()];
		double[][] y_test_hat = new double[N_test][1];
		
		for(Integer i = 0; i<N_test; i++){
			y_test_hat[i][0] = W_min.get(0,0);
			nodeNumr = 0;
			for(Integer j = 0; j < runSettings.getNhid(); j++){
				if(nodes_left[j]==1){
					input_test[j] = ww[j][0]; //bias
					for(Integer k = 1; k < runSettings.getNhid() + 1; k++){
						input_test[j] += ww[j][k] * x_test[i][k-1]; //i ex är det x(i,k-1);
					}
					s_test[i][j] = 1.0/(1+Math.exp(-input_test[j]));
					y_test_hat[i][0] += s_test[i][j]*W_min.get(nodeNumr+1,0);
					nodeNumr++;
				}
			}
		}
		return y_test_hat;
	}
	/*
	 * Fills a matrix with the preferred value
	 */
	protected Object[][] fillMatrix(final int row, final int col, final int value){
		Integer[][] filledMatrix = new Integer[row][col];
		for(Integer i = 0; i<row; i++){
			for(Integer j = 0; j < col; j++){
				filledMatrix[i][j] = value;
			}
		}
		return filledMatrix;
	}
	/*
	 * Fills a double matrix with the preferred value
	 */
	protected double[][] fillMatrix(final int row, final int col, final double value){
		double[][] filledMatrix = new double[row][col];
		for(Integer i = 0; i<row; i++){
			for(Integer j = 0; j < col; j++){
				filledMatrix[i][j] = value;
			}
		}
		return filledMatrix;
	}
	/*
	 * Fills a array with the preferred value
	 */
	protected Object[] fillArray(final int length, final int value){
		Integer[] filledArray = new Integer[length];
		for(Integer i = 0; i<length; i++){
			filledArray[i] = value;
		}
		return filledArray;
	}
	
	/*
	 * clones a matrix in a way that the second matrix does not point at the first one
	 */
	private Double[][] cloneMatrix(final Double[][] matrix, final int row, final int col){
		Double[][] newMatrix = new Double[row][col];
		for(Integer j=0; j<row; j++){
			for(Integer i=0; i<col; i++){
				newMatrix[j][i] = matrix[j][i];
			}
		}
		return newMatrix;
	}
	/*
	 * clones a matrix in a way that the second matrix does not point at the first one
	 */
	private double[][] cloneMatrix(final double[][] matrix, final int row, final int col){
		double[][] newMatrix = new double[row][col];
		for(Integer j=0; j<row; j++){
			for(Integer i=0; i<col; i++){
				newMatrix[j][i] = matrix[j][i];
			}
		}
		return newMatrix;
	}

	public int getNhid() {
		return runSettings.getNhid();
	}
	public void setNhid(int nhid) {
		this.runSettings.setNhid(nhid);
	}
	public int getNin() {
		return runSettings.getNin();
	}
	public double getBuildToTestRatio() {
		return buildToTestRatio;
	}
	public void setBuildToTestRatio(double _buildToTestRatio) {
		buildToTestRatio = _buildToTestRatio;
	}

	public double[] getTotal_all_err() {
		return runStatistics.total_all_err;
	}
	public void setTotal_all_err(double[] _total_all_err) {
		runStatistics.total_all_err = _total_all_err;
	}
	public double[] getTotal_all_err_test() {
		return runStatistics.total_all_err_test;
	}
	public void setTotal_all_err_test(double[] _total_all_err_test) {
		runStatistics.total_all_err_test = _total_all_err_test;
	}
	public int getNumberOfRuns() {
		return numberOfRuns;
	}
	public void setNumberOfRuns(int _numberOfRuns) {
		numberOfRuns = _numberOfRuns;
	}

	public boolean isFinnished() {
		return runStatistics.IsFinnished();
	}
	
	public double[][] getInputMatrix() {
		return runMatrix.GetInputMatrix();
	}
	public double getA() {
		return runMatrix.a;
	}
	public void setA(double a) {
		runMatrix.a = a;
	}
	public double getB() {
		return runMatrix.b;
	}
	public void setB(double b) {
		runMatrix.b = b;
	}
	//returns the current progress status with 0 being 0% ready and 1 meaning 100% ready.
	public double getProgress() {
		return runStatistics.getRunProgress();
	}
	//cancel the pruning
	public void cancelPruning() {
		cancelPruning = true;
		System.out.println("\n Pruning was canceled");
	}
	
	/*
	 * test if there are any non-zero connections for the i_min:th hidden node
	 */
	protected void saveBestInputNodes(Integer[][] weights_left, int numberOfWeightsLeft){
		for(Integer i = 0; i<runSettings.getNin(); i++){
			for(Integer j = 0; j<runSettings.getNhid(); j++){
				if(weights_left[j][i] != 0){
					bestInputNodes[numberOfWeightsLeft-1][i] += 1;
					break;
				}
			}
		}
		
	}
	
	public Integer[] getBestInputNodes(int numberOfWeightsLeft) {
		if(numberOfWeightsLeft <= runSettings.getNumberOfNodes()){
			return bestInputNodes[numberOfWeightsLeft-1];
		}
		Integer[] emptyArray = new Integer[runSettings.getNumberOfNodes()];
		return emptyArray;
	}
	private void printMatrix(final double[][] matrix, final int row, final int col){
		for(Integer i = 0; i<row; i++){
			System.out.println("");
			for(Integer j = 0; j < col; j++){
				System.out.print(matrix[i][j]+" ");
			}
		}
	}
	/*
	private void printMatrix(final Object[][] matrix, final int row, final int col){
		System.out.print("\n vikten");
		for(Integer i = 0; i<row; i++){
			System.out.println("");
			for(Integer j = 0; j < col; j++){
				System.out.print(matrix[i][j]+" ");
			}
		}
	}*/
	
}
