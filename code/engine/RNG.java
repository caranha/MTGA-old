package engine;

import java.util.*;

public class RNG extends Random {
	
	static final long serialVersionUID = 1;
	
	private static RNG instance;
			
	protected RNG () 
	{
		super();
	}
	
	protected RNG(int i)
	{
		super(i);
	}
	
	public static synchronized RNG getInstance() 
	{
		if(instance == null) {
			instance = new RNG();
	      }	
		return instance;
	}	
	
	public void setSeed(int seed)
	{
		instance = new RNG(seed);
	}
	
}
