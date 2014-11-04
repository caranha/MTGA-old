/*
 * The Main Pane object builds the tabbed pane which holds all other 
 * panes from the interface. Extra panes should be dialogs called f
 * from the Menu.
 */

package ui;

import javax.swing.*;        



public class MainTabbedPane {
	
	JTabbedPane pane;
	
	AssetPricePane assetp;
	JPanel portip;
	MarketComparatorPane marketp;
	PortCompPane portcmp;
	GeneConfPane gaconfp;
	

	public MainTabbedPane()
	{
		pane = new JTabbedPane();
		
		portcmp = new PortCompPane();
		marketp = new MarketComparatorPane(portcmp);
		assetp = new AssetPricePane(marketp);
		gaconfp = new GeneConfPane(marketp);
		
		pane.addTab("Asset Prices", assetp.getPanel());
		pane.addTab("GA Configuration", gaconfp.getPanel());
		pane.addTab("Compare Results", marketp.getPanel());
		pane.addTab("Compare Structure",portcmp.getPanel());
	}
	
	public JTabbedPane getPanel()
	{
		return pane;
	}

}
