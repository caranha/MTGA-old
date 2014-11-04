/* Implements a checkbox that list all assets avaiable in a given market 
 * This checkbox turns on-off the line referent to that asset in the 
 * AssetChart class instance.
 */

package ui;

import data.*;
import javax.swing.*;
import java.awt.*;

public class AssetChkList {

	JPanel list;
	public JScrollPane panel;
	
	public AssetChkList(AssetChart a)
	{
		JCheckBox ckb;
		Market m = Market.getInstance();
		list = new JPanel();
		
		list.setLayout(new GridLayout(0,1));
		
		
		
		for (int i = 0; i < m.assets.size(); i++)
		{
			ckb = new AssetChkBox(m.assets.get(i).name,m.assets.get(i).name,"asset");
			ckb.addItemListener(a);
			list.add(ckb);
		}
		
		panel = new JScrollPane(list);
		panel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		panel.setPreferredSize(new Dimension(100,0));
		
	}
	
}

