/* This class implements a time series 
 * Chart for a given asset - monthly data
 * based on the yahoo finance format.
 * 
 * The chart listens to a AssetChkMenu, which should contain a group of 
 * AssetChkBoxes, one for each asset.
 */

package ui;

import data.*;
import engine.Parameter;
import java.util.*;
import org.jfree.data.time.*;
import org.jfree.chart.*;
import java.awt.event.*;

public class AssetChart implements ItemListener{
	
	public TimeSeriesCollection chartdata;
	public JFreeChart chart;
	
	Date startd, endd;
	
	public AssetChart()
	{
		Calendar c = Calendar.getInstance();
		Parameter p = Parameter.getInstance();
		String[] date;
		
		String sd = p.getParam("initial user date");
		String ed = p.getParam("final user date");
		
		if (sd == null)		
		{
			c.set(2003, 0, 10);
			System.out.println("Bing");
		}
		else
		{			
			date = sd.split("-");
			c.set(Integer.valueOf(date[0]), 
					Integer.valueOf(date[1]), 
					Integer.valueOf(date[2]));
		}
		startd = c.getTime();
		
		if (ed == null)		
			c.set(2004, 11, 10);
		else
		{			
			date = ed.split("-");
			c.set(Integer.valueOf(date[0]), 
					Integer.valueOf(date[1]), 
					Integer.valueOf(date[2]));
		}
		endd = c.getTime();
		
		chartdata = new TimeSeriesCollection();
				
		chart = ChartFactory.createTimeSeriesChart(
				  "Asset Prices",
				  "Date", 
				  "Closing Price",
				  chartdata,
				  true,
				  true,
				  false
				);
		
	}

	/* Adds another asset to this chart */
	public void addAsset(Asset ass)
	{
		
		chartdata.addSeries(ass.getPriceTS(startd,endd));
	}
	
	public void removeAsset(Asset ass)
	{
		chartdata.removeSeries(ass.getPriceTS(startd,endd));
	}

	public void itemStateChanged(ItemEvent e)
	{
	
		Market m = Market.getInstance();
		
		AssetChkBox obj = (AssetChkBox) e.getItem();
		
		String ename = obj.nameID;
		
		
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			addAsset(m.getAssetFromName(ename));
		}
		else
		{
			removeAsset(m.getAssetFromName(ename));
		}
		
				
	}
}


