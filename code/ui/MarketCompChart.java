/* This class implements a time series 
 * Chart for a given asset - monthly data
 * based on the yahoo finance format.
 * 
 * The chart listens to a AssetChkMenu, which should contain a group of 
 * AssetChkBoxes, one for each asset.
 */

package ui;

import org.jfree.data.time.*;
import org.jfree.chart.*;
import java.awt.event.*;

public class MarketCompChart implements ActionListener{
	
	public TimeSeriesCollection chartdata;
	public JFreeChart chart;
	
	public MarketCompChart()
	{
		
		chartdata = new TimeSeriesCollection();
				
		chart = ChartFactory.createTimeSeriesChart(
				  "Portfolio Results",
				  "Date", 
				  "Return",
				  chartdata,
				  true,
				  true,
				  false
				);
	}

	/* Adds another asset to this chart */
	public void addAsset(TimeSeries ts)
	{
		chartdata.addSeries(ts);
	}
	
	public void removeAsset(TimeSeries ts)
	{
		chartdata.removeSeries(ts);
	}

	public void actionPerformed(ActionEvent e)
	{
	}
}


