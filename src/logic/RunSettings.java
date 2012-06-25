package logic;

public class RunSettings {
	private int nin; 				// Number of input nodes
	private int nout; 				//A guess since, I do not know what this is good for
	private int nhid; 				//Number of hidden nodes
	
	
	public RunSettings(int numberOfInputNodes)
	{
		//nin = 3;
		this.nin = numberOfInputNodes;
		this.nhid = 5;
		nout = 1;
		
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

}
