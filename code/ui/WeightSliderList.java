package ui;

import data.*;
import javax.swing.*;
import java.awt.*;

public class WeightSliderList {
	
	JPanel list;
	public JScrollPane panel;
	
	public WeightSliderList()
	{
		WeightSlider ckb;
		JLabel label;
		Market m = Market.getInstance();
		list = new JPanel();
		
		list.setLayout(new GridLayout(0,2));
		
		for (int i = 0; i < m.assets.size(); i++)
		{
			label = new JLabel(m.assets.get(i).name);
			ckb = new WeightSlider(m.assets.get(i).name,m.assets.get(i).name);
			list.add(label);
			list.add(ckb);
		}
		
		panel = new JScrollPane(list);
		panel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel.setPreferredSize(new Dimension(500,0));
		
	}
}
