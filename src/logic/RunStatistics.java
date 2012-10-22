package logic;

public class RunStatistics {

	public Integer[][] bestInputNodesTotal;
	private double[] all_err;
	private double[] all_err_test;
	public double[] total_all_err;
	public double[] total_all_err_test;
	Integer numberOfNodes;
	Integer numberOfInputNodes;
	Integer numberOfRuns;
	
	private int progress;           //an integer telling the progress of the pruning
	private int totalIterations;	//the total iterations that the progress have to go through
	private boolean isFinnished;		//a boolean keeping track if the algorithm is finished
	
	private Double[][] normalizationValues;
	
	public RunStatistics(Integer numberOfNodes, Integer numberOfInputNodes, Integer numberOfRuns)
	{
		this.progress = 0;
		this.numberOfNodes = numberOfNodes;
		this.numberOfInputNodes = numberOfInputNodes;
		this.numberOfRuns = numberOfRuns;
		Integer numberOfNodesToEliminate = numberOfNodes - 1;
		
		all_err = new double[numberOfNodesToEliminate];
		all_err_test = new double[numberOfNodesToEliminate]; 
		total_all_err = new double[numberOfNodesToEliminate];
		total_all_err_test = new double[numberOfNodesToEliminate]; 
		
		
		//the total number of iterations the main loop will perform
		for(int i = 0; i < numberOfNodes + 1; i++){
			totalIterations += i;
		}
		totalIterations -= 1;
		totalIterations *= numberOfRuns;
		isFinnished = false;
		
		bestInputNodesTotal = new MatrixHelper().FillMatrix(numberOfNodes, numberOfInputNodes, 0);
	}

	public void StoreMinErrorFor(double err_min, int numberOfWeights) {
		this.all_err[numberOfWeights - 1] = err_min;
	}


	public void StoreTestErrorFor(double testError, int numberOfWeights) {
		this.all_err_test[numberOfWeights - 1] = testError;
	}
	
	public void AddLastPruningRunToTotalStatistics()
	{
		for(int i = 0; i < this.numberOfNodes - 1; i++){
			this.total_all_err[i] += 1.0 / numberOfRuns * this.all_err[i];
			this.total_all_err_test[i] += 1.0 / numberOfRuns * this.all_err_test[i];
		}
	}
	
	public boolean IsFinnished()
	{
		return this.isFinnished;
	}
	
	public void SetFinnished()
	{
		this.isFinnished = true;
	}
	
	public void CancelRun()
	{
		this.progress = 0;
		//TODO maybe to other stuff aswell
	}

	public double getRunProgress() {
		return this.progress / (double) this.totalIterations;
	}

	public void IncrementProgress() {
		this.progress++;
		
	}
	
	public void RevertNormalizeOnErrors(int V_tot)
	{
		this.RevertNormalizeOnError(this.total_all_err, V_tot);
		this.RevertNormalizeOnError(this.total_all_err_test, V_tot);
	}
	
	/*
	 * The following code will revert the normalize on a array which represent an column, or for instance the errors
	 */
	private void RevertNormalizeOnError(double[] array, int V_tot){
		double normMin = this.GetNormalizationMin(V_tot);
		double normMax = this.GetNormalizationMax(V_tot);
		double min, max; 

		min = this.GetNormalizationMin(V_tot - 1);
		max = this.GetNormalizationMax(V_tot - 1);
		//normalize each element in the array
		for(Integer i = 0; i<this.numberOfNodes - 1; i++){
			array[i] = array[i] / (normMax-normMin) * (max-min);
		}
			
	}
	
	private Double GetNormalizationMax(Integer index)
	{
		return normalizationValues[index][1];
	}
	
	private Double GetNormalizationMin(Integer index)
	{
		return normalizationValues[index][0];
	}

	public void SetNormalizationMatrix(Double[][] normalizationMatrix) {
		this.normalizationValues = normalizationMatrix;
		
	}

	public void StoreBestNodesForRun(Integer[][] bestInputNodesForRun) {
		for(int i = 0; i<numberOfNodes; i++)
		{
			for(int j = 0; j<numberOfInputNodes; j++)
			{
				bestInputNodesTotal[i][j] += bestInputNodesForRun[i][j];
			}
		}
		
	}

}
