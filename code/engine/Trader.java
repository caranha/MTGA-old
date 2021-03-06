/**
 * Trader.java:
 * 
 * This objects effects the trading logic of the system. This takes portfolios, 
 * and, comparing them with the market, generate the results of the portfolio. 
 * The trader can operate automagically, by re-balancing the portfolio every 
 * month (or not), and discounting trading costs. It saves the performance
 * data (portfolio value and composition) for the whole period.
 * 
 * This object should be used for multi-scenario testing
 * of a existing portfolio - after you get the portfolio you
 * want to use, you call this to know the "real world" value
 * of it.
 */

package engine;

import data.*;
import java.util.*;

public class Trader {

	public Portfolio p_ideal; // Portfolio that the trader WANTS to hold
	public Portfolio p_current; // portfolio currently held (changes after one trade)
	
	public Double c_initial; // Initial capital, for logging purposes
	public Double c_current = 0.0; // Current capital.
	
	public Double[] port_value; // monthly portfolio value.
	public Double[] total_value; // monthly portfolio value plus capital.
	public Double[] cost_value; // monthly costs during trading.
	
	public Boolean docost = false; // This trader uses trading costs
	public Boolean dorebalance = false; // This trader rebalances the portfolio after each 
							// trade (use only for long-term traders)
	
	public Date t_init; // initial date for trading - if none, assume earliest date.
	public Date t_end;  // final date for trading - if none, assume latest date.
	public int current_time = 0;
	
	public Trader()
	{
		Parameter p = Parameter.getInstance();
		c_initial = Double.valueOf(p.getParam("initial capital"));
	}
	
	/* Sets the trader ideal portfolio */
	public void setPortfolio(Portfolio p)
	{
		p.normalizeWeight();
			
		/* Copying the portfolio */
		p_ideal = p.copy();
		p_current = p.copy();		
		
	}
	
	/* Sets the trading limits for this portfolio. */
	public void setDates(Date start, Date end)
	{
		t_init = start;
		t_end = end;
		
		/* TODO: here, size can be size +1, because time 
		 * is used to get the values from the assets, and not to 
		 * calculate the returns.
		 */
		int size = Utils.calcMonths(start, end) +1;
		port_value = new Double[size];
		total_value = new Double[size];
		cost_value = new Double[size];
				
	}
	
	/**
	 *  Do trade assumes that the initial portfolio is already set with weights.
	 * just runs the same portfolio over a period of time, balancing 
	 * it if the flag is set.
	 * 
	 * You need to setDates before running doTrade
	 */
	public void doTrade()
	{
		
		Calendar c = Calendar.getInstance();
		Market mkt = Market.getInstance();

		//if (!p_ideal.weightToLots(c_initial, t_init))
		//{
		//	p_ideal.lotsToweight(t_init);
		//	p_ideal.weightToLots(c_initial, t_init);
		//}
		
		
		p_current = p_ideal.copy();
		c.setTime(t_init);
		c_current = 0.0;
		
		for (int i = 0; i < port_value.length; i++)
		{
			port_value[i] = p_current.totalValue(c.getTime());
			
			/* TODO: not sure if this is working */
			if (dorebalance)
			{
				// Saves the last used portfolio, to compare with the new
				// lots values.
				Portfolio p2 = p_current.copy();
				
				// 1- Calculate the target values for the lots 
				// given the wished-for weights and current portfolio value
				// p_current.weightToLots(p_current.totalValue(c.getTime()), c.getTime());
				
				// 2- Calculate the change in capital to affect this change
				// extra or lacking capital is just stored.

				//for (int j = 0; j < p2.getSize(); j++)
				//{
					
					//c_current += (p2.getLots(j) - p_current.getLots(j)) * 
					//	mkt.assets.get(j).getPriceByDate(c.getTime());
				//}
				
				
			}
			
			total_value[i] = port_value[i] + c_current;
			c.add(Calendar.MONTH, 1);

		}
		
	}
	
	public void doTradeOnce()
	{
		
	}
	
	/*
	 * TODO: Future Improvements for trader. 
	 * These Three functions should be removed from trader.
	 * the trader object should be reduced to simply handling 
	 * the results of effecting trades over one or more periods 
	 * with a portfolio, and returning the resulting return/monetary
	 * values.
	 * 
	 * Risk can be derived from the return values.
	 * Sharpe ratio can be derived from the return values and the 
	 * risk.
	 * 
	 * Expected return can be derived from either the return values
	 * for the portfolio, or by individual calculation of the 
	 * return values of each asset. In any case, it is not the 
	 * responsability of the trader. << specially this
	 * 
	 */
	public Double calcSharpeRatio()
	{
		Parameter p = Parameter.getInstance();
		Calendar c = Calendar.getInstance();
		c.setTime(t_init);
		c.add(Calendar.MONTH, current_time);
		Date pastdate;
		Date curdate;
		
		/* Getting the past values of the portfolio to calculate moving 
		 * average and then sharpe ratio
		 */
		int timeoffset = Integer.parseInt(p.getParam("Moving Average"));
		c.add(Calendar.MONTH, -1 * timeoffset);
		
		Double values[] = new Double[timeoffset];		
		for (int i = 0; i < timeoffset; i ++)
		{			
			pastdate = c.getTime();
			c.add(Calendar.MONTH, 1);
			curdate = c.getTime();
			values[i] = Math.log(p_current.totalValue(curdate)/p_current.totalValue(pastdate));
		}
		
		Double Rf = Double.parseDouble(p.getParam("Riskless Asset"));
		Double sharpe = (MathFun.average(values) - Rf)/Math.sqrt(MathFun.variance(values));		
				
		return sharpe;
	}
	
	public Double calcRisk()
	{
		return calcRisk(0);
	}
	
	public Double calcRisk(int Dflag)
	{
		Parameter p = Parameter.getInstance();
		Calendar c = Calendar.getInstance();
		c.setTime(t_init);
		c.add(Calendar.MONTH, current_time);
		Date pastdate;
		Date curdate;
		
		/* Getting the past values of the portfolio to calculate moving 
		 * average and then sharpe ratio
		 */
		int timeoffset = Integer.parseInt(p.getParam("Moving Average"));
		c.add(Calendar.MONTH, -1 * timeoffset);
		
		Double values[] = new Double[timeoffset];		
		for (int i = 0; i < timeoffset; i ++)
		{			
			pastdate = c.getTime();
			c.add(Calendar.MONTH, 1);
			curdate = c.getTime();
			values[i] = Math.log(p_current.totalValue(curdate)/p_current.totalValue(pastdate));
			if (Dflag == 1)
				System.out.println("values[i]");
		}
						
		return Math.sqrt(MathFun.variance(values));
	}
	public Double calcEReturn()
	{
		Parameter p = Parameter.getInstance();
		Calendar c = Calendar.getInstance();
		c.setTime(t_init);
		c.add(Calendar.MONTH, current_time);
		Date pastdate;
		Date curdate;
		
		/* Getting the past values of the portfolio to calculate moving 
		 * average and then sharpe ratio
		 */
		int timeoffset = Integer.parseInt(p.getParam("Moving Average"));
		c.add(Calendar.MONTH, -1 * timeoffset);
		
		Double values[] = new Double[timeoffset];		
		for (int i = 0; i < timeoffset; i ++)
		{			
			pastdate = c.getTime();
			c.add(Calendar.MONTH, 1);
			curdate = c.getTime();
			values[i] = Math.log(p_current.totalValue(curdate)/p_current.totalValue(pastdate));
		}
						
		return MathFun.average(values);
	}
	
	
}
