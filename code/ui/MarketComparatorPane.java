package ui;

import javax.swing.*;        

import engine.*;
import data.*;
import java.awt.*;
import java.util.*;
import org.jfree.data.time.*;
import org.jfree.chart.*;


public class MarketComparatorPane {

	JPanel pane;
	
	MarketCompChart returnchart;
	MarketSharpeChart sharpechart;
	
	Trader user_made;
	Trader GA;
	Trader GP;
		
	JTextArea BUser,BGA,BGP;
	
	TimeSeries userTS;
	TimeSeries gaTS;
	TimeSeries gpTS;
	
	Date startd, endd;
	
	public MarketComparatorPane(PortCompPane pcmp)
	{		
		pane = new JPanel();
		returnchart = new MarketCompChart();
		sharpechart = new MarketSharpeChart();
		String param;
		String[] date;
		Parameter p;
		
		BUser = pcmp.BoxUser;
		BGA = pcmp.BoxGA;
		BGP = pcmp.BoxGP;
		
		// getting the date;
		Calendar c = Calendar.getInstance();
		p = Parameter.getInstance();
		
		param = p.getParam("initial trade date");
		if (param == null)
			c.set(2005, 00, 05);
		else
		{
			date = param.split("-");
			c.set(Integer.valueOf(date[0]),
					Integer.valueOf(date[1]),
					Integer.valueOf(date[2]));
		}		
		startd = c.getTime();
		
		param = p.getParam("final trade date");
		if (param == null)		
			c.set(2006, 00, 05);
		else
		{
			date = param.split("-");
			c.set(Integer.valueOf(date[0]),
					Integer.valueOf(date[1]),
					Integer.valueOf(date[2]));
		}
		endd = c.getTime();
		doIndex();
		
		ChartPanel preturn = new ChartPanel(returnchart.chart);
		ChartPanel psharpe = new ChartPanel(sharpechart.chart);
		preturn.setPreferredSize(new Dimension(380,380));
		psharpe.setPreferredSize(new Dimension(380,380));
		
		pane.add(preturn, BorderLayout.WEST);
		pane.add(psharpe, BorderLayout.EAST);
	}
		
	/* Generates data for the index trader.
	 */
	public void doIndex()
	{
		Market mkt = Market.getInstance();
		TimeSeries index;
		Calendar c = Calendar.getInstance();
		Double ret;				

		/* The Market ideal index */
		// TODO: clean this messy code up.
		
		int idxpos = mkt.getIndexPosByDate(startd);
		int len = Utils.calcMonths(startd, endd) + 1;
		c.setTime(startd);
		index = new TimeSeries("Market Index", Month.class);
		ret = 1.0;
		
		
		index.add(new Month(c.getTime()),ret);
		for (int i = 1; i < len; i++)
		{
			ret *= 1 + Math.log(mkt.index.get(idxpos+i).p/mkt.index.get(idxpos+i-1).p);			
			c.add(Calendar.MONTH, 1);
			index.add(new Month(c.getTime()), ret);
		}
		
		Parameter p = Parameter.getInstance();
		int size = Integer.parseInt(p.getParam("moving average"));
		Double[] idxv = new Double[size];
		for (int i = 1; i < size; i++)
		{
			idxv[i] = Math.log(mkt.index.get(idxpos-size+i).p/mkt.index.get(idxpos+i-size-1).p);
		}

		/* TODO: see why the index SR is not included */
		//sharpechart.addPoint(MathFun.average(idxv), 0.5, "Market Index");
		returnchart.addAsset(index);
		sharpechart.addPoint(0.0, 0.0, "");
		
	}
	
	public void newGATrader(Portfolio P)
	{
		if (gaTS != null)
		{
			returnchart.removeAsset(gaTS);
		}
		if (GA != null)
		{
			sharpechart.removePoint(GA.calcRisk(), GA.calcEReturn(), "GA Portfolio");
		}

		
		GA = new Trader();
		GA.setPortfolio(P);
		GA.setDates(startd, endd);
		GA.doTrade(); 

		Calendar c = Calendar.getInstance();
		c.setTime(startd);
		
		gaTS = new TimeSeries("GA Trader", Month.class);
		Double ret = 1.0;
		gaTS.add(new Month(c.getTime()), ret);
		
		for (int i = 1; i < GA.port_value.length; i++)
		{
			ret *= 1 + Math.log(GA.port_value[i]/GA.port_value[i-1]);
			c.add(Calendar.MONTH, 1);
			gaTS.add(new Month(c.getTime()), ret);
		}
		
		sharpechart.addPoint(GA.calcRisk(),GA.calcEReturn(),"GA Portfolio");
		returnchart.addAsset(gaTS);
		BGA.setText(P.dump(0.0));		
		
	}
	
	public void newGPTrader(Portfolio P)
	{
		if (gpTS != null)
		{
			returnchart.removeAsset(gpTS);
		}
		if (GP != null)
		{
			sharpechart.removePoint(GP.calcRisk(), GP.calcEReturn(), "GP Portfolio");
		}

		
		GP = new Trader();
		GP.setPortfolio(P);
		GP.setDates(startd, endd);
		GP.doTrade(); 

		Calendar c = Calendar.getInstance();
		c.setTime(startd);
		
		gpTS = new TimeSeries("GP Trader", Month.class);
		Double ret = 1.0;
		gpTS.add(new Month(c.getTime()), ret);
		
		for (int i = 1; i < GP.port_value.length; i++)
		{
			ret *= 1 + Math.log(GP.port_value[i]/GP.port_value[i-1]);
			c.add(Calendar.MONTH, 1);
			gpTS.add(new Month(c.getTime()), ret);
		}
		
		sharpechart.addPoint(GP.calcRisk(),GP.calcEReturn(),"GP Portfolio");
		returnchart.addAsset(gpTS);
		BGP.setText(P.dump(0.0));		
		
	}
	
	public void newUserTrader(Portfolio P)
	{
		if (userTS != null)
		{
			returnchart.removeAsset(userTS);
		}
		
		if (user_made != null)
		{
			sharpechart.removePoint(user_made.calcRisk(), user_made.calcEReturn(), "User Portfolio");
		}
		
		user_made = new Trader();
		user_made.setPortfolio(P);
		user_made.setDates(startd, endd);
		
		Calendar c = Calendar.getInstance();

		user_made.doTrade(); 
		c.setTime(startd);
		
		userTS = new TimeSeries("User Trader", Month.class);
		Double ret = 1.0;
		userTS.add(new Month(c.getTime()), ret);
		
		for (int i = 1; i < user_made.port_value.length; i++)
		{
			ret *= 1 + Math.log(user_made.port_value[i]/user_made.port_value[i-1]);
			c.add(Calendar.MONTH, 1);
			userTS.add(new Month(c.getTime()), ret);
		}
		
		sharpechart.addPoint(user_made.calcRisk(),user_made.calcEReturn(),"User Portfolio");
		returnchart.addAsset(userTS);
		BUser.setText(P.dump(0.0));
	
	}
		
	public JPanel getPanel()
	{
		return pane;
	}
		
		
}
