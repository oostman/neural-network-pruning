package logic;

public class RunStatistics {

	public double[] all_err;
	public double[] all_err_test;
	public double[] total_all_err;
	public double[] total_all_err_test;
	Integer numberOfNodes;
	Integer numberOfRuns;
	
	private int progress;           //an integer telling the progress of the pruning
	private int totalIterations;	//the total iterations that the progress have to go through
	private boolean isFinnished;		//a boolean keeping track if the algorithm is finished
	
	public RunStatistics(Integer numberOfNodes, Integer numberOfRuns)
	{
		this.progress = 0;
		this.numberOfNodes = numberOfNodes;
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
	}

	public void StoreMinErrorFor(double err_min, int numberOfWeights) {
		this.all_err[numberOfWeights - 1] = err_min;
		
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

}
