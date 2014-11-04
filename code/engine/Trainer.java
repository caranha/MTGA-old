/* This class unifies the data and the genomes in 
 * a simpler front. At initialization, this object takes 
 * in a date and some parameters, checks the data objects,
 * and generate all the expected return and co-variance 
 * values needed to evaluate a portfolio. 
 * 
 * The genetic candidates then call this object to generate 
 * their return and risk values.
 * 
 * This centralize the data evaluation, and avoid teaching 
 * the individuals about the evaluation parameters. This should
 * also allow me to transparently use both real data and 
 * benchmark data for training on single scenarios.
 * 
 * I think.
 * 
 * 
 */

package engine;

import java.util.*;
import data.*;

public class Trainer {

	public double ereturn[]; // expected return
	public double variance[]; // variance for each asset
	public double preturn[][]; // past return, used for variance calc.
	public double correlation[][]; // correlation between assets
	public int ma_length = 0; // internal value for moving average parameter;
	
	/* Creates a new trader based on a date.
	 * This constructor will read 
	 * the data package objects, and calculate the 
	 * needed expected return and risks from there. 
	 * This date is considered to be the date to be 
	 * predicted. 
	 * 
	 * The prediction method is taken from the 
	 * parameters.
	 */
	public Trainer(Date startd)
	{
		Market m = Market.getInstance();
		Parameter p = Parameter.getInstance();
		
		
		String param = p.getParam("moving average");
		if (param != null)
			ma_length = Integer.parseInt(param);
		else
		{
			System.err.println("Error Generating Trainer: no moving average parameter");
			System.exit(1);
		}		
					
		
		ereturn = new double[m.assets.size()];
		variance = new double[m.assets.size()];
		preturn = new double[m.assets.size()][ma_length];
		correlation = new double[m.assets.size()]
		                         [m.assets.size()];

		boolean badasset[] = new boolean[variance.length];
		boolean bad = false;

		
		/* Feeding preturn and ereturn*/
		/* ereturn is calculated taking into account 
		 * the number of available real values. 
		 */
		
		m.resetBadness();
		
		for (int j = 0; j < m.assets.size(); j++)
		{
			int zeroes = 0;
			int startd_i = m.assets.get(j).getIndexByDate(startd);
			
			// adding return values;
			for (int i = 0; i < ma_length; i++)
			{
				double cur = m.assets.get(j).getPriceByIndex(startd_i - ma_length + i);
				double pre = m.assets.get(j).getPriceByIndex(startd_i - ma_length + i -1);
				
				if (cur == 0.0 || pre == 0.0)
				{
					preturn[j][i] = 0.0;
					zeroes++;
				}
				else
				{
					preturn[j][i] = Math.log(cur/pre);
					ereturn[j] += preturn[j][i];
				}				
			}
			
			// average return;
			if (ereturn[j] != 0.0)
				ereturn[j] = ereturn[j]/(ma_length - zeroes);

			// Recording bad data
			// (data which is not complete for the period of the experiment)
			if (zeroes > 0)
			{
				m.assets.get(j).badness = true;
				badasset[j] = true;
				bad = true; // Michael!
			}	
		
		}
		
		
		/* Reporting Bad data
		 * 
		 * Bad assets are marked inside the marked objects, and terminal won't (hopefully)
		 * select them. Unless the proportion of bad assets is very high, this should not 
		 * affect performance, and marking them in this way helps avoid modifying the 
		 * dataset for every experiment.
		 */
		
		if (bad)
		{
			System.err.print("Incomplete data for period in assets: ");
			for (int i = 0; i < variance.length; i++)
				if (badasset[i]==true)
					System.err.print((i+1) + " ");
			System.err.println();
		}

		
		/*
		 * Correlation (calculated from Pearson's coefficient
		 * from wikipedia. Theoretically, this correlation
		 * has no meaning if the relationship between 
		 * the stocks is not linear, but the financial
		 * engineers don't seem to care about this.
		 * 
		 * This numerical method is supposed to be stable;
		 */
		
		for (int x = 0; x < ereturn.length; x++)
		{
			
			for (int y = 0; y < x; y++)
			{
				double sum_sq_x = 0.0;
				double sum_sq_y = 0.0;
				double sum_coproduct = 0.0;
				double mean_x = preturn[x][0];
				double mean_y = preturn[y][0];
				for (int i = 1; i < ma_length; i++)
				{
					double sweep = i/i+1.0;
					double delta_x = preturn[x][i] - mean_x;
					double delta_y = preturn[y][i] - mean_y;
					sum_sq_x += delta_x * delta_x * sweep;
					sum_sq_y += delta_y * delta_y * sweep;
					sum_coproduct += delta_x * delta_y * sweep;
					mean_x += delta_x / (i + 1.0);
					mean_y += delta_y / (i + 1.0);
				}
				double pop_sd_x = Math.sqrt(sum_sq_x/ma_length);
				double pop_sd_y = Math.sqrt(sum_sq_y/ma_length);
				double cov_x_y = sum_coproduct/ma_length;
				correlation[x][y] = cov_x_y / (pop_sd_x * pop_sd_y);
				correlation[y][x] = correlation [x][y];
				variance[x] = sum_sq_x/ma_length;
				variance[y] = sum_sq_y/ma_length;
			}

			correlation[x][x] = 1.0;
		}

	}
		
	
	/*
	 * This creates a trainer from a scenario file.
	 * It reads a file with the list of expected 
	 * returns for each asset, and the co-variance
	 * for every two assets, and calculates the returns
	 * of the portfolios on demand.
	 */
	public Trainer(String filename)
	{
		
	}
	
	/*
	 * gets a portfolio (normalized weights) and 
	 * returns its expected return.
	 */
	public double getExpectedReturn(double[] port)
	{
		double ret = 0.0;
		
		for (int i = 0; i < port.length; i++)
			ret += port[i]*ereturn[i];
		
		return ret;
	}
	public double getExpectedReturn(Portfolio port)
	{
		double ret = 0.0;
		
		for (int i = 0; i < port.getAssetSize(); i++)
			ret += port.getWeightByPos(i)*ereturn[port.getIndexByPos(i)];
		
		return ret;
	}
	
	/*
	 * gets a portfolio (normalized weights) and 
	 * returns its risk based on the summed values 
	 * of the returns.
	 */
	public double getRunningRisk(double[] port)
	{
		double ret = 0.0;
		Double values[] = new Double[ma_length];
		
		for (int i = 0; i < ma_length; i++)
		{
			values[i] = 0.0;
		}

		
		for (int j = 0; j < port.length; j++)
			if (port[j] > 0)
			{
				for (int i = 0; i < ma_length; i++)
				{
					values[i] += preturn[j][i]*port[j];
				}
			}
		
		// calculating the variance.
		ret = Math.sqrt(MathFun.variance(values));
		
		return ret;
	}
	public double getRunningRisk(Portfolio port)
	{
		double ret = 0.0;
		Double values[] = new Double[ma_length];
		
		for (int i = 0; i < ma_length; i++)
		{
			values[i] = 0.0;
		}

		
		for (int j = 0; j < port.getAssetSize(); j++)
			for (int i = 0; i < ma_length; i++)
			{
				values[i] += preturn[port.getIndexByPos(j)][i]*port.getWeightByPos(j);
			}
		
		// calculating the variance.
		ret = Math.sqrt(MathFun.variance(values));
		
		return ret;
	}
	
	
	/* 
	 * Calculates the risk based on the co-variance between 
	 * the assets;
	 * 
	 * For all assets, taken 2 and 2, multiply their weights,
	 * their variances, and their mutual covariances.
	 * 
	 * Portfolio Volatility definition taken from wikipedia.
	 */
	public double getCovarRisk(double[] port)
	{
		double ret = 0;
		for (int i = 0; i < ereturn.length; i++)
		{
			for (int j = 0; j < ereturn.length; j++)
			{
				ret += variance[i]*variance[j]*correlation[i][j]*port[i]*port[j];
			}
		}
		ret = Math.sqrt(ret);
		
		return ret;
	}
	public double getCovarRisk(Portfolio port)
	{
		double ret = 0;
		int size = port.getAssetSize();
		for (int i = 0; i < size; i++)
		{
			for (int j = 0; j < size; j++)
			{
				int idxi = port.getIndexByPos(i);
				int idxj = port.getIndexByPos(j);
				ret += variance[idxi]*variance[idxj]*
					correlation[idxi][idxj]*port.getWeightByPos(i)*port.getWeightByPos(j);
			}
		}
		ret = Math.sqrt(ret);
		
		return ret;
	}
}
