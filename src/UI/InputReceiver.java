package UI;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.vaadin.ui.Upload.Receiver;



@SuppressWarnings("serial")
public class InputReceiver implements Receiver{

	private String fileName;
    private String mtype;
    
    private ByteArrayOutputStream stream;
    
    //variables related to the input processing
    boolean loadedStreamToString = false;
	boolean closedTheStream = false;
	boolean turnedTheSplittedStringToAMatrix = false;
	
	boolean turnedTheSplittedStringMatrixToDoubleMatrix = false;
	
	private int columns=0;
	private int rows=0;
	private int oldColumns=0;
	private int oldRows=0;
	private double[][] inMatrix;
	private double[][] oldInMatrix;
	private String[][] stringMatrix;
    
    
    /**
     * return an OutputStream that simply counts line ends
     */
    public ByteArrayOutputStream receiveUpload(String filename, String MIMEType) {

        fileName = filename;
        mtype = MIMEType;
        
        return setStream(new ByteArrayOutputStream());
        
        
        
    }
    /*
     * Processes the uploaded file
     */
    public void processUploadedFile() {
    	loadedStreamToString = false;
    	closedTheStream = false;
    	turnedTheSplittedStringToAMatrix = false;
    	turnedTheSplittedStringMatrixToDoubleMatrix = false;
    	columns=0;
    	rows=0;
    	oldColumns=0;
    	oldRows=0;

    	String content = null;
    	//reads in the stream
		try {
			content=getStream().toString("UTF-8");
			loadedStreamToString = true;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return;
		}
		try {
			getStream().close();
			closedTheStream = true;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		/*
		 * some ugly code that should accept:
		 * with 1-2 spaces between each number
		 * that does not have spaces during numbers that are between lines
		 */
		content = " "+content;
		content = content.replaceAll("\t"," ");
		content = content.replaceAll("\n","\n ");
		content = content.replaceAll("   "," ");
		content = content.replaceAll("  "," ");
		
		final int searchedByteRow = '\n';
		final int searchedByteCol = ' ';
		
		
		int i = 0;
		//counting the columns
		while(content.charAt(i) != searchedByteRow){
			if(content.charAt(i) == searchedByteCol && content.charAt(i+1) != searchedByteCol){
				columns++;
				oldColumns++;
			}
			i++;
		}
		
		i = 0;
		while(content.length() > i){
			if(content.charAt(i) == searchedByteRow){
				rows++;
				oldRows++;
			}
			i++;
		}
		//incase last sign is not a "\n" then add one too the number of rows -2 since I added a " " too the end
		if(content.charAt(content.length()-2) != searchedByteRow)
		{
			rows++;
			oldRows++;
		}
		//makes sure its not an empty file in any direction or without spaces ...
		if(rows == 0 || columns == 0){
			return;
		}
		/*
		 * All values have either one or two spaces between them. 
		 * So first all double spaces are made into single spaces.
		 * Then the content is split everywhere a space is found.
		 */

		//content = content.replaceAll("\t"," ");
		
		String[] inputStringArray = content.split(" ");
		
		System.out.println("\n"+rows+" "+columns);
		System.out.println(content);
		stringMatrix = new String[rows][columns];
		inMatrix = new double[rows][columns];
		oldInMatrix  = new double[rows][columns];
		
		//a try an catch to check if the uploaded file really is a matrix with numbers
				try{
					//System.out.print(inputStringArray.length+" "+(rows*columns)+ " k"
					//			+inputStringArray[0]+"k "+inputStringArray[rows*columns]);
					for(i = 0; i<inputStringArray.length; i++)
						System.out.println(i+"\t"+inputStringArray[i]);
					
					for(i = 1; i<inputStringArray.length; i++){
						//System.out.println("\n"+(i-1)/columns+" "+(i-1)%columns);
						stringMatrix[(i-1)/columns][(i-1)%columns] = inputStringArray[i];
						System.out.println(i+" "+stringMatrix[(i-1)/columns][(i-1)%columns]);
					}
					
					turnedTheSplittedStringToAMatrix = true;
					

				}catch(Exception e){
					e.printStackTrace();
					return;
				}
		
		//a try an catch to check if the uploaded file really is a matrix with numbers
		try{
			
			for(i = 1; i<inputStringArray.length; i++){
				//System.out.println("\n"+(i-1)/columns+" "+(i-1)%columns);
				inMatrix[(i-1)/columns][(i-1)%columns] = Double.valueOf(stringMatrix[(i-1)/columns][(i-1)%columns]);
				System.out.println(inMatrix[(i-1)/columns][(i-1)%columns]);
				oldInMatrix[(i-1)/columns][(i-1)%columns] = Double.valueOf(stringMatrix[(i-1)/columns][(i-1)%columns]);
			}
			
			turnedTheSplittedStringMatrixToDoubleMatrix = true;
			//theLogic.setInputMatrix(inMatrix, rows,columns);
			
			//tells the runButton listener that it can perform the pruning if being clicked
			//isRunable = true;
			//runButton.setEnabled(true);
			/*getWindow().showNotification(
					"Upload completed",
                    "The uploaded file is available for pruning");
                    */

		}catch(Exception e){
			e.printStackTrace();
			return;
			/*getWindow().showNotification(
                    "Upload failed",
                    "The uploaded file was of the wrong format",
                    Notification.TYPE_ERROR_MESSAGE);
			*/
			//isRunable = false;
			//runButton.setEnabled(false);

		}
        
    }
    
    public boolean noError(){
    	if (loadedStreamToString && closedTheStream 
    			&& turnedTheSplittedStringToAMatrix 
    			&& turnedTheSplittedStringMatrixToDoubleMatrix)
    	{
    		return true;
    	}

    	return false;
    }
    public String getErrorMessage()
    {
    	if(!loadedStreamToString)
    		return "Error message: could not load the file into a string.";
    	if(!closedTheStream)
    		return "Error message: could not close the file stream.";
    	if(!turnedTheSplittedStringToAMatrix)
    		return "Error message: could not split the string into an matrix, make sure the file has an equal number of rows and columns.";
    	if(!turnedTheSplittedStringMatrixToDoubleMatrix)
    		return "Error message: the uploaded data contains entries that are not number.";
    	
    	return "There does not seem to be any error";
    }
    
    public String getFileName() {
        return fileName;
    }

    public String getMimeType() {
        return mtype;
    }

	public ByteArrayOutputStream getStream() {
		return stream;
	}

	public ByteArrayOutputStream setStream(ByteArrayOutputStream stream) {
		this.stream = stream;
		return stream;
	}
	public int getColumns() {
		return columns;
	}
	public int getRows() {
		return rows;
	}
	public double[][] getInMatrix() {
		return inMatrix;
	}
	public String[][] getStringMatrix() {
		return stringMatrix;
	}
	//checks if the input file is of a correct shape but contains letters
	public boolean isCorrectShapeButContainsNonNumbners() {
		return turnedTheSplittedStringToAMatrix && !turnedTheSplittedStringMatrixToDoubleMatrix;
	}
	// undo the last change to inMatrix
	public void undoLastChange(){
		columns = oldColumns;
		rows = oldRows;
		inMatrix = new double[rows][columns];
		for(int i = 0; i<rows*columns; i++){
			inMatrix[i/columns][i%columns] = oldInMatrix[i/columns][i%columns];
		}
	}
	//lags all values from the matrix except for the last one
	public void lagAllInput(int lag){
		updateOldInMatrix();
		//lag all inputs as much as the lag
		int newRows = rows-lag;
		double[][] tempMatrix = new double[newRows][columns];
		for(Integer j = 0; j < columns - 1; j++){
			for(Integer i = 0; i < newRows; i++){
				tempMatrix[i][j] = inMatrix[i][j];
			}	
		}
		for(Integer i = lag; i < rows; i++){
			tempMatrix[i-lag][columns - 1] = inMatrix[i][columns - 1];
		}
		rows = newRows;
		inMatrix = tempMatrix;
	}
	public void lagOneInput(int lag, int col){
		updateOldInMatrix();
		//lag one column as much as the lag
		
		int newRows = rows-lag;
		double[][] tempMatrix = new double[newRows][columns];
		for(Integer j = 0; j < columns; j++){
			if(j != col){
				for(Integer i = lag; i < rows; i++){
					tempMatrix[i - lag][j] = inMatrix[i][j];
				}
			}	
		}
		for(Integer i = 0; i < newRows; i++){
			tempMatrix[i][col] = inMatrix[i][col];
		}
		rows = newRows;
		inMatrix = tempMatrix;
	}
	public void deleteColumn(int col){
		updateOldInMatrix();
		//delete one column
		double[][] tempMatrix = new double[rows][columns-1];
		int currentCol = 0;
		for(Integer j = 0; j < columns; j++){
			if(j != col){
				for(Integer i = 0; i < rows; i++){
					tempMatrix[i][currentCol] = inMatrix[i][j];
				}
				currentCol++;
			}	
		}
		columns = columns - 1;
		inMatrix = tempMatrix;
	}
	public void deleteRow(int row){
		updateOldInMatrix();
		//delete one row
		double[][] tempMatrix = new double[rows-1][columns];
		int currentRow = 0;
		for(Integer j = 0; j < columns; j++){
			currentRow = 0;
			for(Integer i = 0; i < rows; i++){
				if(i != row){
					tempMatrix[currentRow][j] = inMatrix[i][j];
					currentRow++;
				}
			}	
		}
		rows = rows - 1;
		inMatrix = tempMatrix;
	}
	
	private void updateOldInMatrix(){
		oldColumns = columns;
		oldRows = rows;
		oldInMatrix = new double[oldRows][oldColumns];
		for(int i = 0; i<rows*columns; i++){
			oldInMatrix[i/columns][i%columns] = inMatrix[i/columns][i%columns];
		}
	}


}
