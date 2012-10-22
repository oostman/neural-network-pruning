package logic;

import java.util.Random;

public class SampleProblemCreator {
		
	private RunMatrix runMatrix;
	
	public double a;				//a is a parameter for the sample problems
	public double b;				//b is the signal to noise parameter for the sample problems
	private Random rand;
	
	public SampleProblemCreator()
	{
		a = 0.5;
		b = 0.2;
		this.rand = new Random();
		
		this.runMatrix = new RunMatrix();
	}
	
	public RunMatrix MakeSampleProblem(final int sampleProblem) throws Exception
	{
		
		double[][] inputMatrix;
		int V_tot;
		
		int N_tot = 100;

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
			throw new Exception("Used a non-existing sampleProblem number!");

		}
		
		
		runMatrix.setInputMatrix(inputMatrix, N_tot, V_tot);
		
		//runMatrix.normalizeMatrix(-1.0, 1.0);
		
		
		return runMatrix;
	}

}
