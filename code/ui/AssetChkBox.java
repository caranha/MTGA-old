package ui;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JCheckBox;

public class AssetChkBox extends JCheckBox {

	static final long serialVersionUID = 1;
	
	/* nameID is the name of the asset, or the name of the portfolio
	 * that is related to this checkbox.
	 */
	public String nameID; 
	
	/* typeID indicates if this checkbox is talking about an asset 
	 * or a portfolio.
	 */
	public String typeID;

	
	public AssetChkBox(String text, String n, String t) {
		super(text);
		typeID = t;
		nameID = n;
	}

	public String getNameID()
	{
		return nameID;
	}
	public String getTypeID()
	{
		return typeID;
	}
	
	public AssetChkBox(Action a) {
		super(a);
	}

	public AssetChkBox(Icon icon, boolean selected) {
		super(icon, selected);
	}

	public AssetChkBox(Icon icon) {
		super(icon);
	}

	public AssetChkBox(String text, boolean selected) {
		super(text, selected);
	}

	public AssetChkBox(String text, Icon icon, boolean selected) {
		super(text, icon, selected);
	}

	public AssetChkBox(String text, Icon icon) {
		super(text, icon);
	}

	public AssetChkBox(String text) {
		super(text);
	}
	
}
