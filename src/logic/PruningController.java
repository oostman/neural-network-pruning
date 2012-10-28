package logic;

public class PruningController{
	
	PruningLogic pruningLogic;
	
	private SampleProblemCreator sampleProblemCreator;
	
	private RunMatrix runMatrix;
	private NeuralNetworkData neuralNetworkData;
	private int numberOfRuns; 		//how many runs the pruning algorithm should make
		
	public PruningController(){
		sampleProblemCreator = new SampleProblemCreator();
		
		runMatrix = new RunMatrix();
		neuralNetworkData = new NeuralNetworkData();
		numberOfRuns = 5;						
	}
	
	public void setInputMatrix(
			final double[][] inputMatrix, //inputMatrix is the data matrix
			final int N_tot, 				//N_tot is the number of measurements
			final int V_tot				//V_tot is the number of variables
			){


		runMatrix.setInputMatrix(inputMatrix, N_tot, V_tot);		
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
				
		pruningLogic = new PruningLogic(runMatrix, neuralNetworkData, numberOfRuns);
		pruningLogic.start();
	}

	public int getNhid() { return neuralNetworkData.getNhid(); }
	public void setNhid(int nhid) { neuralNetworkData.setNhid(nhid); }
	public int getNin() { return neuralNetworkData.getNin(); }
	
	public double getBuildToTestRatio() { return runMatrix.getBuildToTestRatio(); }
	public void setBuildToTestRatio(double buildToTestRatio) { runMatrix.setBuildToTestRatio(buildToTestRatio);	}

	public double[] getTotal_all_err() { return pruningLogic.getTotal_all_err(); }
	public double[] getTotal_all_err_test() { return pruningLogic.getTotal_all_err_test(); }
	public boolean isFinnished() { return pruningLogic.IsFinnished(); }
	
	public int getNumberOfRuns() { return numberOfRuns;	}
	public void setNumberOfRuns(int _numberOfRuns) { numberOfRuns = _numberOfRuns; }
	
	public double[][] getInputMatrix() { return runMatrix.GetInputMatrix();	}
	public double getA() { return this.sampleProblemCreator.a; }
	public void setA(double a) { this.sampleProblemCreator.a = a; }
	public double getB() { return this.sampleProblemCreator.b; }
	public void setB(double b) { this.sampleProblemCreator.b = b; }
	
	public double getProgress() { return pruningLogic.GetProgressInPercentage(); }
	
	public void cancelPruning() {
		pruningLogic.CancelRun();
		System.out.println("\n Pruning was canceled");
	}
	public Integer[] getBestInputNodes(Integer numberOfWeightsLeft) { return pruningLogic.BestInputNodesTotal(numberOfWeightsLeft);	}
	
}
