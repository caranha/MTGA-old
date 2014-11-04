package ui;

import javax.swing.*;        

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import org.jfree.chart.*;

import engine.Portfolio;

public class AssetPricePane implements ActionListener{

	JPanel pane;
	AssetChart ac;
	AssetChkWeightList ckb;
	
	JPanel buttonpane;
	Portfolio user_p;
	WeightSliderList list;
	JButton submit;
	JButton clear;
	MarketComparatorPane mcc;
	
	
	public AssetPricePane(MarketComparatorPane m)
	{
		pane = new JPanel(new BorderLayout());

		user_p = new Portfolio();
		mcc = m;
		
		ac = new AssetChart();        
       ckb = new AssetChkWeightList(ac);        		

		submit = new JButton("Save Portfolio");
		submit.addActionListener(this);
		clear = new JButton("Clear Portfolio");
		clear.addActionListener(this);
		buttonpane = new JPanel(new BorderLayout());
		buttonpane.add(submit, BorderLayout.EAST);
		buttonpane.add(clear, BorderLayout.WEST);		
       buttonpane.setPreferredSize(new Dimension(0,30));
		
		ChartPanel chartp = new ChartPanel(ac.chart);
		chartp.setPreferredSize(new Dimension(300,300));
       
		
       pane.add(ckb.panel, BorderLayout.WEST);
       pane.add(buttonpane, BorderLayout.SOUTH);
       pane.add(chartp,BorderLayout.CENTER);
       

	}
	
	public JPanel getPanel()
	{
		return pane;
	}

	public void actionPerformed(ActionEvent e)
	{
		Component[] clist = ckb.list.getComponents();
		
		if(e.getActionCommand().equalsIgnoreCase("Save Portfolio"))
		{
			Double weight[] = new Double[clist.length/2];

			for (int i = 0; i < clist.length; i++)
			{
				if (i%2 == 1)
				{
					WeightSlider slide = (WeightSlider) clist[i];
					weight[i/2] = new Double(slide.getValue());
					weight[i/2] = weight[i/2]/1000.0;
				}
			}
		
			user_p = new Portfolio();
			user_p.setWeights(weight);
			user_p.normalizeWeight();
		
			for (int i = 0; i < clist.length; i++)
			{
				if (i%2 == 1)
				{
					Double value;
					WeightSlider slide = (WeightSlider) clist[i];
					value = user_p.getWeightByIndex(i/2) * 1000;

					slide.setValue(value.intValue());
				}
			}
			
			mcc.newUserTrader(user_p);
		}
		else
		{
			for (int i = 0; i < clist.length; i++)
			{
				if (i%2 == 1)
				{
					WeightSlider slide = (WeightSlider) clist[i];
					slide.setValue(0);
				}
			}
			user_p = new Portfolio();
		}
		
	}
	
}
