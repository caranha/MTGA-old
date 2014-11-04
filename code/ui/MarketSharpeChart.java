package ui;

import org.jfree.data.xy.*;
import org.jfree.chart.*;
import org.jfree.chart.plot.*;
import engine.*;
import org.jfree.chart.renderer.xy.*;

public class MarketSharpeChart {
		
	public XYSeriesCollection chartdata;
	public JFreeChart chart;
	
	public MarketSharpeChart()
	{
		
		chartdata = new XYSeriesCollection();
				
		chart = ChartFactory.createXYLineChart(
					"Portfolio Results 2",
				  "Expected Return", 
				  "Risk",
				  chartdata,
				  PlotOrientation.HORIZONTAL,
				  true,
				  true,
				  false
				);
		
		/* minor incantation to show plot points */
		XYPlot plot = (XYPlot) chart.getPlot();
		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
		renderer.setSeriesLinesVisible(0, true);
		renderer.setSeriesShapesVisible(0,true);
		
		plot.setRenderer(renderer);
		
	}

	/* Adds another asset to this chart */
	public void addPoint(Double risk, Double ret, String name)
	{
		XYSeries s = new XYSeries(name);
		Parameter p = Parameter.getInstance();
		Double riskless = Double.valueOf(p.getParam("riskless return"));
		Double zero = 0.0;
		
		// XYSeries.add(X,Y) actually adds X in the vertical axis, and Y
		// in the horizontal axis. This makes no sense.
		s.add(riskless,zero);
		s.add(ret, risk);
		
		chartdata.addSeries(s);
	}
	
	public void removePoint(Double risk, Double ret, String name)
	{
		XYSeries s = new XYSeries(name);
		Parameter p = Parameter.getInstance();
		Double riskless = Double.valueOf(p.getParam("riskless return"));
		Double zero = 0.0;
		

		// XYSeries.add(X,Y) actually adds X in the vertical axis, and Y
		// in the horizontal axis. This makes no sense.
		s.add(riskless,zero);
		s.add(ret, risk);
		
		
		
		chartdata.removeSeries(s);
	}
	
	
}
