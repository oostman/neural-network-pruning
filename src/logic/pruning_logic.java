package logic;

import Jama.*;

public class pruning_logic{
	
	private RunStatistics runStatistics;
	private NeuralNetworkData neuralNetworkData;
	private RunMatrix runMatrix;
	
	private double buildToTestRatio;
	
	private int numberOfRuns; 		//how many runs the pruning algorithm should make
		
	private SampleProblemCreator sampleProblemCreator;
	private boolean cancelPruning;	// a boolean that can be used to cancel a pruning in progress
	
	public pruning_logic(){
		this.neuralNetworkData = new NeuralNetworkData();
		this.sampleProblemCreator = new SampleProblemCreator();
		
		numberOfRuns = 5;
		
		cancelPruning = false;
		
		this.runMatrix = new RunMatrix();
		
		
	}
	
	public void setInputMatrix(
			final double[][] inputMatrix, //inputMatrix is the data matrix
			final int N_tot, 				//N_tot is the number of measurements
			final int V_tot				//V_tot is the number of variables
			){


		runMatrix.setInputMatrix(inputMatrix, N_tot, V_tot);
		
		this.neuralNetworkData.setNin(V_tot - 1);
						
		
	}
	public void makeSampleProblem(final int sampleProblem)
	{
		
		try {
			this.runMatrix = this.sampleProblemCreator.MakeSampleProblem(sampleProblem);
		} catch (Exception e) {
			// TODO: add some exception handling to UI, and throw exception from this method
			e.printStackTrace();
		}

		do_prunings();
	}
		
	/*
	 * This is the function which is called from the outside to start the pruning.
	 * It controls the number of pruning runs.
	 */
	public void do_prunings(){
		
		//number of inputs is the columns in the inputMatrix - the output
		this.neuralNetworkData.setNin(runMatrix.GetNumberOfVariables() - 1);
		
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
			
			runStatistics = new RunStatistics(neuralNetworkData.getNumberOfNodes(), neuralNetworkData.getNin(), numberOfRuns);
			
			for(int i = 0; i < numberOfRuns; i++){
				//perform the pruning
				perform_pruning(); 
				//save the results
				runStatistics.AddLastPruningRunToTotalStatistics();
			}
			
			runStatistics.SetNormalizationMatrix(runMatrix.GetNormalizationMatrix());
			runStatistics.RevertNormalizeOnErrors(runMatrix.GetNumberOfVariables());
			
			runStatistics.SetFinnished();

		}
		
		/*
		 * perform_pruuning is as the name suggests were the pruning takes place
		 */
		protected void perform_pruning(){
			
			runMatrix.NewRun(neuralNetworkData.getNumberOfOutNodes());
			
			Matrix YbMatrix = new Matrix(runMatrix.y_build);
			
			neuralNetworkData.Init();
			
			neuralNetworkData.CalculateHiddenNodesInputAndOutput(runMatrix.x_build, runMatrix.N_build);
						
			/*
			 * starts the pruning stage
			 */
						
			//nodes_left is the vector keeping track on which hidden nodes that are still being used, 1 means it is used
			Integer[] nodes_left = (Integer[]) fillArray(neuralNetworkData.getNhid(),1);
									
			//number of weights in lower layer
			int numberOfWeights = neuralNetworkData.getNumberOfNodes();
			
			//err is the current error and err_min is the smallest error
			double err_min, err;
			int i_min = 0, j_min = 0;
								
			Matrix upperLayerWeights_min = null, upperLayerWeights = null;
			
			// the loop that eliminates the weights one at a time until there is only one weight left
			while(numberOfWeights > 1){
				
				if(cancelPruning){
					runStatistics.CancelRun();
					return;
				}
				err_min = 10000.1;
				err = 10000.0;
				upperLayerWeights_min = null;
				
				neuralNetworkData.CalculateHiddenNodesInputAndOutput(runMatrix.x_build, runMatrix.N_build);
								
				for(Integer i = 0; i<neuralNetworkData.getNhid(); i++){
					for(Integer j = 1; j < neuralNetworkData.getNin() + 1; j++){
						
						//checks if weight is already pruned/eliminated
						if(neuralNetworkData.IsWeightAlreadyEliminated(i, j)){
							//If, yes then don't waste time on it
							err = 10000.0;	
						}else{							
							runStatistics.IncrementProgress();
							
							neuralNetworkData.ResetWeightsAndHiddenLayerOutputs(runMatrix.N_build);
																					
							neuralNetworkData.PruneOneWeight(i,j);
							
							neuralNetworkData.CalculateModifiedHiddenNodesOutput(runMatrix.x_build, runMatrix.N_build, i, j);
													
							//Solve the upper layer weights by least-squares. 
							Matrix hiddenLayerOutput = neuralNetworkData.GetConnectedHiddenLayerOutputs(runMatrix.N_build);
							try
							{
								upperLayerWeights = hiddenLayerOutput.solve(YbMatrix);
							}
							catch(Exception e){
								/*printMatrix(neuralNetworkData.pruningHiddenLayerOutput, runMatrix.N_build, helthyColNode+1);
								System.out.println("\n"+helthyColNode);
								W = A.solve(YbMatrix);*/
								
								System.out.println("stuff failed");
							}
							/*
							 * the residuals and error
							 */
							Matrix Y_hat = hiddenLayerOutput.times(upperLayerWeights);
							err = ( YbMatrix.minus(Y_hat).norm2() ) /Math.sqrt(runMatrix.N_build);
							
						}//else loop ends here
						
						if(err < err_min){		//note the best network
							err_min = err;
							i_min = i;
							j_min = j;
							upperLayerWeights_min = upperLayerWeights;
							
						}
					}
				} //the "master" for loop ends
				//one weight has been removed ...
				numberOfWeights--;
				
				neuralNetworkData.RemoveWeight(i_min, j_min);
				neuralNetworkData.SaveBestInputNodes(numberOfWeights); //belongs to statistics
				
				//test if there are any non-zero connections for the i_min:th hidden node
				nodes_left[i_min] = testForNonZeroConnections(neuralNetworkData.weights_left, i_min);
				
				//saves the error for each iteration, i.e. the removing of an weight
				runStatistics.StoreMinErrorFor(err_min, numberOfWeights);
				
				/*
				 * Evaluate the model on the test data //y_test = new double[N_test][nout]
				 */
				double[][] y_test_hat = modelOnTheTestData(runMatrix.N_test, neuralNetworkData.lowerWeights, nodes_left, upperLayerWeights_min, runMatrix.x_test);
				
				Matrix Y_test_hat = new Matrix(y_test_hat);
				Matrix Y_test_matrix = new Matrix(runMatrix.y_test);
				// should be saved as 0,1,2,3, ....
				
				double testError = ( Y_test_matrix.minus(Y_test_hat).norm2() ) /Math.sqrt(runMatrix.N_test);				
				runStatistics.StoreTestErrorFor(testError, numberOfWeights);
				
			}//the master while loop ends
			
			runStatistics.StoreBestNodesForRun(neuralNetworkData.bestInputNodesForRun);
			// TODO: write status? System.out.println("one pruning is done progress: "+progress+"/"+totalIterations);

		}
		
	}	
			
	/*
	 * test if there are any non-zero connections for the i_min:th hidden node
	 */
	protected int testForNonZeroConnections(Integer[][] weights_left, int i_min){
		for(Integer i = 0; i<neuralNetworkData.getNin(); i++){
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
		double[][] s_test = new double[N_test][neuralNetworkData.getNhid()];
		double[] input_test = new double[neuralNetworkData.getNhid()];
		double[][] y_test_hat = new double[N_test][1];
		
		for(Integer i = 0; i<N_test; i++){
			y_test_hat[i][0] = W_min.get(0,0);
			nodeNumr = 0;
			for(Integer j = 0; j < neuralNetworkData.getNhid(); j++){
				if(nodes_left[j]==1){
					input_test[j] = ww[j][0]; //bias
					for(Integer k = 1; k < neuralNetworkData.getNin() + 1; k++)
					{
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
	 * Fills a double matrix with the preferred value
	 
	protected Double[][] fillMatrix(final int row, final int col, final double value){
		Double[][] filledMatrix = new Double[row][col];
		for(Integer i = 0; i<row; i++){
			for(Integer j = 0; j < col; j++){
				filledMatrix[i][j] = value;
			}
		}
		return filledMatrix;
	}*/
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
	
	

	public int getNhid() {
		return neuralNetworkData.getNhid();
	}
	public void setNhid(int nhid) {
		this.neuralNetworkData.setNhid(nhid);
	}
	public int getNin() {
		return neuralNetworkData.getNin();
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
	public double[] getTotal_all_err_test() {
		return runStatistics.total_all_err_test;
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
		return this.sampleProblemCreator.a;
	}
	public void setA(double a) {
		this.sampleProblemCreator.a = a;
	}
	public double getB() {
		return this.sampleProblemCreator.b;
	}
	public void setB(double b) {
		this.sampleProblemCreator.b = b;
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
	
	public Integer[] getBestInputNodes(int numberOfWeightsLeft) {
		if(numberOfWeightsLeft <= neuralNetworkData.getNumberOfNodes()){
			return runStatistics.bestInputNodesTotal[numberOfWeightsLeft-1];
		}
		Integer[] emptyArray = new Integer[neuralNetworkData.getNumberOfNodes()];
		return emptyArray;
	}
	/*
	private void printMatrix(final double[][] matrix, final int row, final int col){
		for(Integer i = 0; i<row; i++){
			System.out.println("");
			for(Integer j = 0; j < col; j++){
				System.out.print(matrix[i][j]+" ");
			}
		}
	}*/
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
