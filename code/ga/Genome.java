package ga;
/**
 * June/2008
 * Modified the Genome object to read parameters and calculate
 * fitness in the same way as the MemeNode object. Also, 
 * it now has support for Binary/Real/Mixed genomes.
 */

import data.Market;
import engine.*;

public class Genome implements Comparable<Genome>
{
	// 0 is boolean, 1 is real, 2 is mixed
    public int type = 2;

    public double[] weight; // genomic values of weights - non normalized
    public boolean[] index; // says wether the index is in the portfolio or not

	public Double fitness = 0.0;
	public Double Ereturn = 0.0; // estimated return
	public Double Risk    = 0.0; // risk measure
	public Double Sharpe  = 0.0; // sharpe ratio    

	/**
	 * Constructs an empty genome
	 */ 
	public Genome()
	{
		
	}
	
	public void init_empty()
	{
		fitness = 0.0;
    	Ereturn = 0.0;
    	Risk = 0.0;
    	Sharpe = 0.0;
		
		Market mkt = Market.getInstance();
		
		weight = new double[mkt.assets.size()];
    	index = new boolean[mkt.assets.size()];
	}
	
	/**
	 * Initializes a random genome.
	 */
	public void init() 
    {
    	Parameter p = Parameter.getInstance();
    	RNG dice = RNG.getInstance();
    	Market mkt = Market.getInstance();
    	
    	String param;
    	double frate = 0.6;
    	
    	fitness = 0.0;
    	Ereturn = 0.0;
    	Risk = 0.0;
    	Sharpe = 0.0;
    	
    	param = p.getParam("Fill Rate");
    	if (param != null)
    		frate = Double.valueOf(param);
    	else
    	{
    		System.err.println("Genome Constructor Error: no Fill Rate parameter");
    		System.exit(0);
    	}
    	
    	param = p.getParam("Array Type");
    	if (param != null)
    		type = Integer.valueOf(param);
    	else
    	{
    		System.err.println("Genome Constructor Error: no Array Type parameter");
    		System.exit(0);
    	}
    	
    	weight = new double[mkt.assets.size()];
    	index = new boolean[mkt.assets.size()];

    	for (int i = 0; i < weight.length; i++)
    	{
    		if (dice.nextDouble() < frate)
    			index[i] = true;
    		else
    			index[i] = false;
    		weight[i] = dice.nextDouble();	

    		// Avoiding bad assets
    		if (mkt.assets.get(i).badness == true)
    		{
    			index[i] = false;
    			weight[i] = 0;
    		}
    		
    	}
    }
	
	/**
	 * Generates a copy of the current genome
	 * @return
	 */
    public Genome copy()
    {
    	Genome resp = new Genome();

    	resp.weight = weight.clone();
    	resp.index = index.clone();
    	resp.type = type;
    	resp.fitness = fitness;
    	resp.Ereturn = Ereturn;
    	resp.Sharpe = Sharpe;
    	resp.Risk = Risk;
    	
    	return resp;
    }
    
    /**
     * Crossover operator, modifies this genome and 
     * the genome being crossovered with.
     * @param p2
     */
    public void crossover(Genome p2)
    {
	RNG dice = RNG.getInstance();
    	
	for (int i = 0; i < weight.length; i++)
	    if (dice.nextDouble() < 0.4) // do nothing
		{
		}
	    else if (dice.nextDouble() < 0.666) // switch
		{
	    	double tmp1 = p2.weight[i];
	    	boolean tmp2 = p2.index[i];
	    	p2.weight[i] = weight[i];
	    	p2.index[i] = index[i];
	    	weight[i] = tmp1;
	    	index[i] = tmp2;
		}
	    else // average (only weight)
	    {
	    	weight[i] = (weight[i] + p2.weight[i])/2;
	    	p2.weight[i] = weight[i];
	    }
	
	
    } 
    
    public void mutation()
    {
	RNG dice = RNG.getInstance();
    Parameter p = Parameter.getInstance();	
	String param = p.getParam("change rate");
    Double change = Double.valueOf(param);
    Market mkt = Market.getInstance();
    
	for (int i = 0; i < weight.length; i++)
	{
		if (dice.nextDouble() < change)
		{
			weight[i] = weight[i]*0.7+(dice.nextDouble()*0.6);
			if (weight[i] > 1)
				weight[i] = 1.0;
			if (weight[i] < 0)
				weight[i] = 0.0;
		}
		if (dice.nextDouble() < change)
		{
			index[i] = !index[i];
		}
		
		// Avoiding bad assets
		if (mkt.assets.get(i).badness == true)
		{
			index[i] = false;
			weight[i] = 0;
		}
		
	}
	
    } 
    
    /**
     * this implements High fitness is better 
     * switch signal to change
     */
    public int compareTo(Genome gt)
    {
    	if (fitness > gt.fitness)
    		return -1;
    	if (fitness < gt.fitness)
    		return 1;
    	return 0;
    
    }
    
 
    /* returns the weights of this individual, up to len, zeroed on non-indexed weights 
     * and normalized on the others.
     */
    public double[] getNormalWeights()
    {
    	double ret[] = new double[weight.length];
    	double totalsum = 0.0;
    	double truesum = 0.0;
    	int totalindex = 0;
    	
    	for (int i = 0; i < weight.length; i++)
    	{
    		totalsum += weight[i];
    		if (index[i])
    		{
    			truesum += weight[i];
    			totalindex++;
    		}
    	}
    	
    	for (int i = 0; i < weight.length; i++)
    	switch (type)    	
    	{
    	case 0: // boolean
    		if (index[i])
    			ret[i] = 1.0/totalindex;
    		break;
    	case 1: // real
    		ret[i] = weight[i]/totalsum;
    		break;
    	case 2: // mix
    		if (index[i])
    			ret[i] = weight[i]/truesum;
    		break;
    	}

    	return ret;
    }
      

/**
 * Calculate the fitness of this genome for training data T.
 * @param T
 */
    public void eval(Trainer T)
    {
		double[] port = getNormalWeights();
		
		/* DEBUG - trying to Zero small weights */
		
		//for (int i = 0; i < port.length; i++)
		//{
		//	if (port[i] < 0.00001)
		//		weight[i] = 0;
		//}
		
		//port = getNormalWeights();
		
		/* END DEBUG */
		
		Parameter p = Parameter.getInstance();
		String param;
		
		param = p.getParam("risk type");
		if (param == null)
		{
			System.err.println("Error. Parameter -risk type- not set");
			System.exit(0);
		}
		int running = param.compareToIgnoreCase("running");
		
		param = p.getParam("riskless return");
		if (param == null)
		{
			System.err.println("Error. Parameter \"riskless return\" not set");
			System.exit(0);
		}
		double r0 = Double.parseDouble(param);
		
		// calculate return and risk;
		Ereturn = T.getExpectedReturn(port);
		if (running == 0)
			Risk = T.getRunningRisk(port);
		else
			Risk = T.getCovarRisk(port);
		Sharpe = (Ereturn - r0)/Risk;
		
		/*
		 * TODO: Here I decide what the fitness 
		 * actually IS. I need to make this more complex
		 * to accomodate for other kinds of fitness.
		 */		
		fitness = Sharpe;
    	
    }
    
    /**
     * Returns the euclidean distance between the current 
     * genome and the parameter genome
     * Distance is zero if there's no other genome to 
     * compare to. 
     */
    public double eucDistance(Genome G)
    {
    	double dist = 0;
    	
    	if (G == null) return 0;

    	double[] port1 = getNormalWeights();
    	double[] port2 = G.getNormalWeights();
    	
    	
    	for (int i = 0; i < weight.length; i++)
    	{
    		dist += Math.pow(port1[i] - port2[i], 2);
    	}
    	    	
    	return Math.sqrt(dist);
    }

    
    public int countAsset(double tresh)
    {
    	int ret = 0;
    	double[] w = getNormalWeights();
    	for (int i = 0; i < w.length; i++)
    		if (w[i] > tresh) 
    			ret++;
    	return ret;    		
    }
    
    /**
     * Returns a string with the boolean and 
     * double values for this genome
     */
    //FIXME: Complete Dump Genome
}
