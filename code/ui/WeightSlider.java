package ui;

import javax.swing.JSlider
;

public class WeightSlider extends JSlider {

	static final long serialVersionUID = 1;
	
	/* nameID is the name of the asset, or the name of the portfolio
	 * that is related to this checkbox.
	 */
	public String nameID; 
	

	
	
	public WeightSlider(String text, String n) {
		super(0,1000);
		nameID = n;
		setValue(0);
	}

	public String getNameID()
	{
		return nameID;
	}
	

	
}
