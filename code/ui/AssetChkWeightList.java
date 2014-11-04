/* Implements a checkbox that list all assets avaiable in a given market 
 * This checkbox turns on-off the line referent to that asset in the 
 * AssetChart class instance.
 */

package ui;

import data.*;
import javax.swing.*;
import java.awt.*;

public class AssetChkWeightList {

	JPanel list;
	public JScrollPane panel;
	
	public AssetChkWeightList(AssetChart a)
	{
		
		WeightSlider wkb;
		AssetChkBox ckb;
		Market m = Market.getInstance();
		list = new JPanel();
		
		list.setLayout(new GridLayout(0,2));
		
		for (int i = 0; i < m.assets.size(); i++)
		{
			ckb = new AssetChkBox(m.assets.get(i).name,m.assets.get(i).name,"asset");
			wkb = new WeightSlider(m.assets.get(i).name,m.assets.get(i).name);

			ckb.addItemListener(a);
			wkb.setPreferredSize(new Dimension(70,0));
			list.add(ckb);
			list.add(wkb);
		}
		
		panel = new JScrollPane(list);
		panel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel.setPreferredSize(new Dimension(200,0));
		
	}
	
}

