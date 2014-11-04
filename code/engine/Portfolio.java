package engine;
import java.util.*;
import data.*;

// PortGA - java version
// This class should implement the basic calculations and generation methods of a 
// portfolio, so that I can use both GA and GP genomes to generate 
// the same portfolio.

// The portfolio is composed of three parts: A vector of names, which contains
// The name of the composing assets, a vector of weights, and a vector of 
// stock. The input in the portfolio might be either the weights, or the 
// number of each stock.

/* Todo:
 * Portfolio Operation:
 * -- Portfolio Adding
 * -- Portfolio subtracting
 * -- Portfolio weight updating from changed stock value
 * -- rounding off lots based on stock price
 */

public class Portfolio {
	
	static final Comparator<PortWeight> CINDEX =
		new Comparator<PortWeight>() 
			{
				public int compare(PortWeight w1, PortWeight w2) 
				{
					if (w1.index < w2.index)
						return -1;
					if (w1.index > w2.index)
						return 1;
					return 0;	
				};
			};
	
	Vector<PortWeight> pwlist;
	Market mkt;
	
	public Portfolio()
	{
		pwlist = new Vector<PortWeight>();
		mkt = Market.getInstance();
	}		
	
	/* Copy the portfolio */
	public Portfolio copy()
	{
		Portfolio cp = new Portfolio();
		for(int i = 0; i < pwlist.size(); i++)
		{
			PortWeight pw = pwlist.get(i);
			cp.pwlist.add(new PortWeight(pw.name,pw.index,pw.weight));
		}		
		return cp;
	}
	
	// Clears the portfolio and set new weights.
	public void setWeights(double[] wgt)
	{
		pwlist.clear();
		for (int i = 0; i < wgt.length; i++)
		{
			if (wgt[i] > 0)
			pwlist.add(new PortWeight(mkt.assets.get(i).name,i,wgt[i]));
		}
	}
	public void setWeights(Double[] wgt)
	{
		pwlist.clear();
		for (int i = 0; i < wgt.length; i++)
		{
			if (wgt[i] > 0)
			pwlist.add(new PortWeight(mkt.assets.get(i).name,i,wgt[i]));
		}	
	}
	
	/* N is the Nth asset in the portfolio - order is not guaranteed */
	public double getWeightByIndex(int n)
	{
		for (int i = 0; i < pwlist.size(); i++)
		{
			PortWeight p = pwlist.get(i);
			if (p.index == n)
				return p.weight;
		}
		return 0;		
	}
	
	public double getWeightByPos(int n)
	{
		return pwlist.get(n).weight;
	}
	public int getIndexByPos(int n)
	{
		return pwlist.get(n).index;
	}

	public int getMaxWeightIndex()
	{
		Collections.sort(pwlist); // This should order the assets by weight
		return pwlist.get(0).index;
	}
	
		
	public int getAssetSize()
	{
		return pwlist.size(); 
	}
	
	public void setWeight(int idx, double wgt)
	{
		for(int i = 0; i < pwlist.size(); i++)
			if (pwlist.get(i).index == idx)
			{
				if (wgt == 0)
					pwlist.remove(i);
				else
					pwlist.get(i).weight = wgt;
				return;
			}
		if (wgt > 0)
			pwlist.add(new PortWeight(mkt.assets.get(idx).name,idx,wgt));
			
		return;		
	}
	
	
	/**
	 *  Compose this portfolio as a merge of the two portfolios given 
	 */
	public void Merge(Portfolio pleft, Portfolio pright, double wgt)
	{
		Collections.sort(pleft.pwlist,CINDEX);
		Collections.sort(pright.pwlist,CINDEX);
		
		int i = 0;
		int j = 0;
				
		Vector<PortWeight> newplist = new Vector<PortWeight>();
		
		while (i < pleft.pwlist.size() && j < pright.pwlist.size())
		{
			PortWeight mypw = pleft.pwlist.get(i);
			PortWeight p2pw = pright.pwlist.get(j);

			if (mypw.index == p2pw.index)
			{
				newplist.add(new PortWeight(mypw.name, mypw.index, (mypw.weight * wgt) + (p2pw.weight * (1 - wgt))));
				i++;
				j++;
			}
			else if (mypw.index > p2pw.index)
			{
				newplist.add(new PortWeight(p2pw.name, p2pw.index,p2pw.weight * (1 - wgt)));
				j++;
			}
			else if (p2pw.index > mypw.index)
			{
				newplist.add(new PortWeight(mypw.name, mypw.index,mypw.weight * wgt));
				i++;
			}
		}
		while (i < pleft.pwlist.size())
		{

			PortWeight mypw = pleft.pwlist.get(i);
			newplist.add(new PortWeight(mypw.name, mypw.index,mypw.weight * wgt));
			i++;
		}
		while (j < pright.pwlist.size())
		{
			PortWeight p2pw = pright.pwlist.get(j);
			newplist.add(new PortWeight(p2pw.name, p2pw.index,p2pw.weight * (1 - wgt)));
			j++;
		}
		
		pwlist = newplist;
		
	}
	
	
	
	// Set sum of weights = 1
	// does not change lots -- need to make internally consistent 
	// after using this.
	public void normalizeWeight()
	{
		double sum = 0;
		for (int i = 0; i < pwlist.size(); i++)
		{
			
			sum += pwlist.get(i).weight;
		}
		for (int i = 0; i < pwlist.size(); i++)
		{
			pwlist.get(i).weight = pwlist.get(i).weight/sum;
		}
	}
	
	

	/* Returns the total value of the portfolio at a given date. It requires
	 * The number of lots to be set.
	 */
	public Double totalValue(Date d)
	{
		double val = 0.0;
		Market mkt = Market.getInstance();
		
		//FIXME: Add real lots later
		for (int i = 0; i < pwlist.size(); i++)
		{
			PortWeight pw = pwlist.get(i);
			val += pw.weight * 100 * mkt.assets.get(pw.index).getPriceByDate(d);
		}
		
		return val;
	}

	/**
	 * 
	 * Generates a String which lists, in decrescent order
	 * the component assets of this Portfolio. 
	 * 
	 * The parameter tresh is the weight treshold for an
	 * asset in the portfolio to be included in the string.
	 * To include all assets (even those with weight 0), 
	 * set tresh to a negative value.
	 * 
	 * @param tresh
	 * @return
	 */
	public String dump(Double tresh)
	{
		String result = new String();
		Collections.sort(pwlist);
		int i = 0;
		while(i < pwlist.size() && pwlist.get(i).weight > tresh)
		{
			result = result + pwlist.get(i).name + ":\t" + pwlist.get(i).weight + "\n";
			i++;
		}
		
		return result;
	}
}
