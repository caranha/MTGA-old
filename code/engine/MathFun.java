package engine;


public class MathFun {

	public MathFun()
	{
		
	}

	
	/**
	 * Returns the variance of a group of values that were passed by parameters.
	 *  
	 * @param values
	 * The values from which the variance will be calculated.
	 * @return
	 */
	static public Double variance(Double[] values)
	{
		Double ret = 0.0;
		Double avg = 0.0;
		
		for (int i = 0; i < values.length; i++)
		{
			avg += values[i]/values.length;
		}
		
		for (int i = 0; i < values.length; i++)
		{
			ret += Math.pow((values[i] - avg), 2);
		}		
		
		if (ret < 0)
			System.err.println("negative variance!");
		
		return ret/values.length;
	}
	
	/** Calculates the average of an array of values.
	 * 
	 * @param values
	 * @return
	 */
	static public Double average(Double[] values)
	{
		Double ret = 0.0;

		for (int i = 0; i < values.length; i++)
		{
			ret += values[i];
		}
		
		return ret/values.length;		
	}
	
}
