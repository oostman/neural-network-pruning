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
}
