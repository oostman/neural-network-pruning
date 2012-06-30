package logic;

import java.util.Random;

public class NeuralNetworkData {
	private int nin; 				// Number of input nodes
	private int nout; 				//Number of outputs A guess since, I do not know what this is good for
	private int nhid; 				//Number of hidden nodes
	
	private Random rand;
	
	public Double[][] origWeights;
	public Double[][] weights;
	public Double[][] pruningWeights;
	
	public double[][] pruningHiddenLayerOutput;
	public double[][] hiddenLayerOutput;
	public double[][] inputs;
	public Integer[][] weights_left;
	public Integer[][] bestInputNodes;
	
	public NeuralNetworkData()
	{
		rand = new Random();
		this.nin = 3;
		this.nhid = 5;
		nout = 1;
		
	}
	
	public void setNin(int _nin)
	{
		this.nin = _nin;
	}
	
	public int getNin()
	{
		return this.nin;
	}
	
	public int getNumberOfOutNodes()
	{
		return this.nout;
	}
	
	public int getNhid()
	{
		return this.nhid;
	}
	
	public int setNhid(int _nhid)
	{
		return this.nhid = _nhid;
	}
	
	public int getNumberOfNodes()
	{
		return this.nhid * this.nin - 1;
	}

	public void Init() {
		//Setup the lower-layers weights as random gaussian values, with mean 1 and variance 2
		this.weights = initWeights();
		
		this.weights_left = fillMatrix(this.nhid, this.nin, 1);
		
		//the best inputs nodes at all different weights left
		this.bestInputNodes = fillMatrix(this.getNumberOfNodes(), this.nhid, 0);
		
	}
	
	/*
	 * Sets up the lower-layer weights as random gaussian values
	 * with the mean 1 and variance 2
	 */
	private Double[][] initWeights(){
		Double[][] weights = new Double[nhid][nin + 1];
		for(Integer i = 0; i < nhid; i++){
			for(Integer j = 0; j < nin + 1; j++){
				weights[i][j] = 1 - 2* rand.nextGaussian();
			}
		}
		
		this.origWeights = cloneMatrix(weights, this.nhid, this.nin + 1);
		return weights;
	}
	

	/*
	 * test if there are any non-zero connections for the i_min:th hidden node
	 */
	public void SaveBestInputNodes(int numberOfWeightsLeft){
		for(Integer i = 0; i < nin; i++){
			for(Integer j = 0; j < nhid; j++){
				if(weights_left[j][i] != 0){
					bestInputNodes[numberOfWeightsLeft-1][i] += 1;
					break;
				}
			}
		}		
	}
	
	
	/*
	 * calculates the net input of the hidden nodes
	 */
	public void CalculateHiddenNodesInputAndOutput(double[][] x_build, int N_build){
		this.inputs = new double[N_build][this.nhid];
		this.hiddenLayerOutput = new double[N_build][nhid + 1];
		
		for(Integer i = 0; i<N_build; i++){
			this.hiddenLayerOutput[i][0] = 1.0;	//bias
		}
		for(Integer i = 0; i<N_build; i++){
			for(Integer j = 0; j < this.nhid; j++){
				this.inputs[i][j] = this.weights[j][0]; //bias
				for(Integer k = 1; k < this.nin + 1; k++){
					this.inputs[i][j] += this.weights[j][k] * x_build[i][k-1];
				}
				this.hiddenLayerOutput[i][j+1] = 1/(1+Math.exp( -this.inputs[i][j]));
			}
		}

	}
	
	/*
	 * calculate the modified net output of the hidden nodes
	 */
	public void CalculateModifiedHiddenNodesOutput(double[][] x_build, int N_build, int i, int j){
		double[][] input_new = new double[N_build][this.getNhid()];
		for(Integer k = 0; k < N_build; k++){
			input_new[k][i] = inputs[k][i] - this.weights[i][j]*x_build[k][j-1]; //old weight
			this.pruningHiddenLayerOutput[k][i+1] = 1/(1+Math.exp(-input_new[k][i]));
		}
	}
	
	/*
	 * Fills a matrix with the preferred value
	 */
	protected Integer[][] fillMatrix(final int row, final int col, final int value){
		Integer[][] filledMatrix = new Integer[row][col];
		for(Integer i = 0; i<row; i++){
			for(Integer j = 0; j < col; j++){
				filledMatrix[i][j] = value;
			}
		}
		return filledMatrix;
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

	public boolean IsWeightAlreadyEliminated(Integer i, Integer j) {
		return this.weights[i][j] == 0;
	}

	public void PruneOneWeight(Integer i, Integer j) {
		this.pruningWeights[i][j] = 0.0;
		
	}

	public void ResetWeightsAndHiddenLayerOutputs(int N_build) {
		this.pruningWeights = cloneMatrix(this.weights, this.getNhid(), this.getNin() + 1);
		this.pruningHiddenLayerOutput = cloneMatrix(this.hiddenLayerOutput, N_build, this.getNhid() + 1);
		
	}

		
}
