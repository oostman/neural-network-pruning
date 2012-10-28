package logic;

import java.util.Random;

import Jama.Matrix;

public class NeuralNetworkData {
	private int nin; 				// Number of input nodes
	private int nout; 				//Number of outputs A guess since, I do not know what this is good for
	private int nhid; 				//Number of hidden nodes
	
	private Random rand;
	
	public Double[][] origLowerWeights;
	public Double[][] lowerWeights;
	public Double[][] pruningLowerWeights;
	
	public Double[][] pruningHiddenLayerOutput;
	public Double[][] hiddenLayerOutput;
	public double[][] inputs;
	public Integer[][] weights_left;
	public Integer[][] bestInputNodesForRun;
	
	public NeuralNetworkData()
	{
		rand = new Random();
		this.nin = 3;
		this.nhid = 5;
		nout = 1;
		
	}
	
	public void setNin(int _nin) { this.nin = _nin; }
	public int getNin()	{ return this.nin; }
	public int getNumberOfOutNodes() { return this.nout; }
	public int getNhid() { return this.nhid; }	
	public int setNhid(int _nhid) { return this.nhid = _nhid; }	
	public int getNumberOfNodes() {	return this.nhid * this.nin - 1; }
	
	public void Init() {
		//Setup the lower-layers weights as random gaussian values, with mean 1 and variance 2
		this.lowerWeights = initWeights();
		
		this.weights_left = new MatrixHelper().FillMatrix(this.nhid, this.nin, 1);
		
		//the best inputs nodes at all different weights left
		this.bestInputNodesForRun = new MatrixHelper().FillMatrix(this.getNumberOfNodes(), this.nin, 0);
		
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
		
		this.origLowerWeights = new MatrixHelper().CloneMatrix(weights, this.nhid, this.nin + 1);
		return weights;
	}
	
	public void RemoveWeight(int i, int j) {
		this.lowerWeights[i][j] = 0.0; 		//removing the least helping weight
		this.weights_left[i][j-1] = 0;
	}
	
	public void SaveBestInputNodes(int numberOfWeightsLeft){
		for(Integer i = 0; i < nin; i++){
			for(Integer j = 0; j < nhid; j++){
				if(this.weights_left[j][i] != 0){
					bestInputNodesForRun[numberOfWeightsLeft][i] += 1;
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
		this.hiddenLayerOutput = new Double[N_build][nhid + 1];
		
		for(Integer i = 0; i<N_build; i++){
			this.hiddenLayerOutput[i][0] = 1.0;	//bias
		}
		for(Integer i = 0; i<N_build; i++){
			for(Integer j = 0; j < this.nhid; j++){
				this.inputs[i][j] = this.lowerWeights[j][0]; //bias
				for(Integer k = 1; k < this.nin + 1; k++){
					this.inputs[i][j] += this.lowerWeights[j][k] * x_build[i][k-1];
				}
				this.hiddenLayerOutput[i][j+1] = 1/(1+Math.exp( -this.inputs[i][j]));
			}
		}

	}
	
	/*
	 * calculate the modified net output of the hidden nodes
	 */
	public void CalculateModifiedHiddenNodesOutput(double[][] x_build, int N_build, int i, int j){
		double[][] input_new = new double[N_build][this.nhid];
		for(Integer k = 0; k < N_build; k++){
			input_new[k][i] = inputs[k][i] - this.lowerWeights[i][j]*x_build[k][j-1]; //old weight
			this.pruningHiddenLayerOutput[k][i+1] = 1/(1+Math.exp(-input_new[k][i]));
		}
	}

	public boolean IsWeightAlreadyEliminated(Integer i, Integer j) {
		return this.lowerWeights[i][j] == 0;
	}

	public void PruneOneWeight(Integer i, Integer j) {
		this.pruningLowerWeights[i][j] = 0.0;
		
	}

	public void ResetWeightsAndHiddenLayerOutputs(int N_build) {
		this.pruningLowerWeights = new MatrixHelper().CloneMatrix(this.lowerWeights, this.getNhid(), this.getNin() + 1);
		this.pruningHiddenLayerOutput = new MatrixHelper().CloneMatrix(this.hiddenLayerOutput, N_build, this.getNhid() + 1);
		
	}

	public Matrix GetConnectedHiddenLayerOutputs(int n_build) {
		
		double[][] sss = RemoveDeadColsHidNodesOutput(this.pruningHiddenLayerOutput, this.pruningLowerWeights, n_build);

		return new Matrix(sss);
	}
	
	/*
	 * Removes the "dead" columns from the Hidden node outputs, 
	 * i.e. if one node is eliminated the input matrix should be one column smaller
	 */
	private double[][] RemoveDeadColsHidNodesOutput(Double[][] hidNodOutput, Double[][] www, int N_build){
		int helthyColNode = numberOfHealthyRows(this.pruningLowerWeights);
		double[][] sss = new double[N_build][helthyColNode + 1]; //helthyColNode + 1, since my hidNodOutput includes an bias column
		
		for(Integer m = 0; m < N_build; m++){
			sss[m][0] = hidNodOutput[m][0];
		}
		int temp = 1;
		for(Integer k = 0; k < this.nhid; k++){
			for(Integer l = 1; l<this.nin+1; l++){				// l = 0 zero is the bias
				if(www[k][l] != 0){
					for(Integer m = 0; m < N_build; m++){
						sss[m][temp] = hidNodOutput[m][k+1];
					}
					temp++;
					break;
				}
			}
		}
		return sss;
	}	
	/*
	 * Calculates how many healthy rows there are in www
	 */
	private int numberOfHealthyRows(Double[][] www){
		int tempsize = 0;
		for(Integer k = 0; k < this.nhid; k++){
			for(Integer l = 1; l<this.nin + 1; l++){				// l = 0 zero is the bias
				if(www[k][l] != 0){
					tempsize++;
					break;
				}
			}
			
		}
		return tempsize;
	}

			
}
