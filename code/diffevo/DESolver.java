package diffevo;
/*
 * GASolver Class
 * 
 * This class solves the portfolio problem by mean of GA. It should receive the data 
 * to be solved, and the parameters for resolution. It tries to find, by GA, the 
 * best portfolio for each timestep. 
 * 
 * This class has methods to print out information about the quality of the solution, 
 * like the profit per timestep, sharpe for the whole solution, total profit, best 
 * solution per time step, fitness and number of epochs per time step.
 * 
 */

import java.util.*;

import engine.*;
import ga.Genome;

public class DESolver {

    // variables
	
    DEPopulation pop;     // this will hold the population needed to evolve  the solution.
    
    /* Parameter names:
     * scenario range -> how many scenarios do at once
     * legacy -> legacy size
     */
    int scenariorange = 12;
    int legacy = 1;	    // how many individuals are transferred from the old generation to the new?
    
    public Genome[] solution;	// the solution for each timestep
    double[] profit;	// profits for this run
    double[] cumprofit; // cumulative profits for this run
    double[] distance; 	// distance for this run
	
    
    /* Filename constructor: Constructs based on parameters from file, or default parameters when lacking
     * comp -> initial genome for MOGA and Legacy - can be safely set to null
     */
    public DESolver(Genome comp)
    {
	
    Parameter param = Parameter.getInstance();
    pop = new DEPopulation(comp);
	
    String paramval;

	paramval = param.getParam("legacy size");
	if (paramval != null)
		legacy = Integer.valueOf(paramval);
    
	paramval = param.getParam("scenario range");
	if (paramval != null)
		scenariorange = Integer.valueOf(paramval);

	solution = new Genome[scenariorange];
	profit = new double[scenariorange];
	cumprofit = new double[scenariorange];
	distance = new double[scenariorange];
		
    }	
   
    /**
     * This function runs the population in order to acquire an 
     * answer for the portfolio problem.
     * 
     * This must be called after a "populateData"     *
     */    
    public void calculateanswer(Date startd)
    {
    	
    	Date currdate;
    	Date prevdate;
    	Portfolio p = new Portfolio();
    	Trader t = new Trader();
    	    	
    	// set the start date, and the progressive dates.
    	Calendar c = Calendar.getInstance();
    	c.setTime(startd);
    	currdate = startd;

    	for (int i = 0; i < scenariorange; i++)
    	{
    		    		
    		// Each scenario is one month
    		
    		prevdate = currdate;
    		c.add(Calendar.MONTH, 1);
    		currdate = c.getTime();		
    		
    		// FIXME: Calling the function which calculates the 
    		// solution is missing here.
    		
    		// get the results - best individual, profit and cumulative profit.
    		p.setWeights(solution[i].getNormalWeights());
      		t.setPortfolio(p);
    		t.setDates(prevdate, currdate);
    		t.doTrade();
    		profit[i] = Math.log(t.port_value[1]/t.port_value[0]);
    		cumprofit[i] = (profit[i] + 1)*(i == 0?1:cumprofit[i - 1]);        		
    		/* TODO: Add distance metric between the results */    		

    		
	    }
    }
    
    public void printResults()
    {
    	for (int i = 0; i < profit.length; i++)
    		System.out.println(i+" "+profit[i]+" "+cumprofit[i]);
    }
    
    /* Returns ratio of average profit against deviation of profit.
     * Sharpe ratio of the application for past analysis, not future.
     * 
     * SHOULD NOT BE USED FOR A SMALL SCENARIO RANGE
     */
    public double sharpeRatio()
    {
    	double deviation = 0;
    	double sum = 0;
    	
    	for (int i = 0; i < profit.length; i++)
    		sum += profit[i];
    	
    	for (int i = 0; i < profit.length; i++)
    		deviation += Math.pow((sum/profit.length) - profit[i],2);
    	
    	deviation = Math.sqrt(deviation/(profit.length-1));
    	
    	return ((sum/profit.length)/deviation);
    }
    
    
    /* Returns the percentage of times that the solution 
     * got positive returns
     * 
     * SHOULD NOT BE USED FOR A SMALL SCENARIO RANGE
     */
    public double hitrate()
    {
    	return hitrate(0,profit.length);
    }
    public double hitrate(int start,int range)
    {
    	double ret = 0;
    	for (int i = 0; i < range; i++)
    		if (profit[i + start] > 0) ret++;
    	return (ret/range);
    }
    
}
