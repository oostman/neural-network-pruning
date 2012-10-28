package logic;

import Jama.Matrix;

public class PruningLogic extends Thread{
	
	private boolean cancelPruning;	// a boolean that can be used to cancel a pruning in progress
	private RunStatistics runStatistics;
	private NeuralNetworkData neuralNetworkData;
	private RunMatrix runMatrix;
	
	private Integer numberOfRuns;
	
	public PruningLogic(RunMatrix matrix, NeuralNetworkData nnData, Integer runs)
	{
		numberOfRuns = runs;
		runMatrix = matrix;
		neuralNetworkData = nnData;
		
		cancelPruning = false;
		
		neuralNetworkData.setNin(runMatrix.GetNumberOfVariables() - 1);
	}
	
	public void run(){
		cancelPruning = false;
		System.out.println("start of the prunings");
		
		//the errors for each pruning need to be initialized here since nhid/nin can change after the constructor
		//the error at all_err[0] will be the error for one weight ... and so on
		
		runStatistics = new RunStatistics(neuralNetworkData.getNumberOfNodes(), neuralNetworkData.getNin(), numberOfRuns);
		
		for(int i = 0; i < numberOfRuns; i++){
			perform_pruning(); 
			runStatistics.AddLastPruningRunToTotalStatistics();
		}
		
		runStatistics.SetNormalizationMatrix(runMatrix.GetNormalizationMatrix());
		runStatistics.RevertNormalizeOnErrors(runMatrix.GetNumberOfVariables());
		
		runStatistics.SetStatusFinnished();
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
		Integer[] nodes_left = new MatrixHelper().FillArray(neuralNetworkData.getNhid(),1);
								
		//number of weights in lower layer
		int numberOfWeightsLeft = neuralNetworkData.getNumberOfNodes();
		
		//err is the current error and err_min is the smallest error
		double err_min, err;
		int i_min = 0, j_min = 0;
							
		Matrix upperLayerWeights_min = null, upperLayerWeights = null;
		
		// the loop that eliminates the weights one at a time until there is only one weight left
		while(numberOfWeightsLeft > 1){
			
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
						Matrix Y_prediction = hiddenLayerOutput.times(upperLayerWeights);
						err = ( YbMatrix.minus(Y_prediction).norm2() ) /Math.sqrt(runMatrix.N_build);
						
					}//else loop ends here
					
					if(err < err_min){		//note the best network
						err_min = err;
						i_min = i;
						j_min = j;
						upperLayerWeights_min = upperLayerWeights;
						
					}
				}
			} //the "master" for loop ends
			
			numberOfWeightsLeft--; 		//one weight has been removed ...
			neuralNetworkData.RemoveWeight(i_min, j_min);
			neuralNetworkData.SaveBestInputNodes(numberOfWeightsLeft); //belongs to statistics?
			//test if there are any non-zero connections for the i_min:th hidden node
			nodes_left[i_min] = testForNonZeroConnections(neuralNetworkData.weights_left, i_min);
			
			
			//saves the error for each iteration, i.e. the removing of an weight
			runStatistics.StoreBuildDataErrorFor(err_min, numberOfWeightsLeft);			
			
			double testError = ErrorForModelOnTestData(nodes_left, upperLayerWeights_min);				
			runStatistics.StoreTestDataErrorFor(testError, numberOfWeightsLeft);
			
		}//the master while loop ends
		
		runStatistics.StoreBestNodesForRun(neuralNetworkData.bestInputNodesForRun);
	}

	private double ErrorForModelOnTestData(Integer[] nodes_left, Matrix upperLayerWeights_min) {
		/*
		 * Evaluate the model on the test data //y_test = new double[N_test][nout]
		 */
		double[][] y_test_prediction = modelOnTheTestData(runMatrix.N_test, neuralNetworkData.lowerWeights, nodes_left, upperLayerWeights_min, runMatrix.x_test);
		
		Matrix Y_test_hat = new Matrix(y_test_prediction);
		Matrix Y_test_matrix = new Matrix(runMatrix.y_test);
		// should be saved as 0,1,2,3, ....
		
		double testError = ( Y_test_matrix.minus(Y_test_hat).norm2() ) / Math.sqrt(runMatrix.N_test);
		return testError;
	}
	
	/*
	 * test if there are any non-zero connections for the i_min:th hidden node
	 */
	private int testForNonZeroConnections(Integer[][] weights_left, int i_min){
		for(Integer i = 0; i<neuralNetworkData.getNin(); i++){
			if(weights_left[i_min][i] != 0){
				return 1;
			}
		}
		return 0;
	}
	
	private double[][] modelOnTheTestData(final int N_test, final Double[][] ww, final Integer[] nodes_left, final Matrix W_min, final double[][] x_test){
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
	
	public void CancelRun()	{ cancelPruning = false; }	
	public double[] getTotal_all_err() { return runStatistics.GetMeanModelErrorForBuildData(); }	
	public double[] getTotal_all_err_test() { return runStatistics.GetMeanModelErrorForTestData(); }	
	public boolean IsFinnished() { return runStatistics.IsFinnished(); }	
	public double GetProgressInPercentage() { return runStatistics.getRunProgress(); }

	public Integer[] BestInputNodesTotal(Integer numberOfWeightsLeft) {
		if(numberOfWeightsLeft <= neuralNetworkData.getNumberOfNodes()){
			return runStatistics.bestInputNodesTotal[numberOfWeightsLeft-1];
		}
		
		return new Integer[neuralNetworkData.getNumberOfNodes()];
	}
}
