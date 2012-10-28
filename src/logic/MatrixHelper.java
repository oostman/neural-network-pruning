package logic;

public class MatrixHelper {
	
	/*
	 * Fills a matrix with the preferred value
	 */
	public Integer[][] FillMatrix(final int row, final int col, final int value){
		Integer[][] filledMatrix = new Integer[row][col];
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
	public Integer[] FillArray(final int length, final int value){
		Integer[] filledArray = new Integer[length];
		for(Integer i = 0; i<length; i++){
			filledArray[i] = value;
		}
		return filledArray;
	}
		
	/*
	 * clones a matrix in a way that the second matrix does not point at the first one
	 */
	public Double[][] CloneMatrix(final Double[][] matrix, final int row, final int col){
		Double[][] newMatrix = new Double[row][col];
		for(Integer j=0; j<row; j++){
			for(Integer i=0; i<col; i++){
				newMatrix[j][i] = matrix[j][i];
			}
		}
		return newMatrix;
	}
}
