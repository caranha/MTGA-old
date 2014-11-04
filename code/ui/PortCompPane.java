package ui;

import javax.swing.*;        
import java.awt.*;

public class PortCompPane {

	JPanel pane;
	
	JLabel LabelUser, LabelGA, LabelGP;
	
	JTextArea BoxUser, BoxGA, BoxGP;
	
	public PortCompPane()
	{
		JScrollPane scrollpane;
		JPanel tpane;
		
		pane = new JPanel(new BorderLayout());

		//FIXME: fix the default x,y sizes below
		LabelUser = new JLabel("User Portfolio");
		BoxUser = new JTextArea(10,20);
		BoxUser.setEditable(false);
		scrollpane = new JScrollPane(BoxUser);
		tpane = new JPanel(new BorderLayout());
		tpane.add(LabelUser, BorderLayout.NORTH);
		tpane.add(scrollpane,BorderLayout.SOUTH);
		pane.add(tpane,BorderLayout.WEST);
		
		LabelGA = new JLabel("GA Portfolio");
		BoxGA = new JTextArea(10,20);
		BoxGA.setEditable(false);
		scrollpane = new JScrollPane(BoxGA);
		tpane = new JPanel(new BorderLayout());
		tpane.add(LabelGA, BorderLayout.NORTH);
		tpane.add(scrollpane,BorderLayout.SOUTH);
		pane.add(tpane,BorderLayout.CENTER);
		
		LabelGP = new JLabel("GP Portfolio");
		BoxGP = new JTextArea(10,20);
		BoxGP.setEditable(false);
		scrollpane = new JScrollPane(BoxGP);
		tpane = new JPanel(new BorderLayout());
		tpane.add(LabelGP, BorderLayout.NORTH);
		tpane.add(scrollpane,BorderLayout.SOUTH);
		pane.add(tpane,BorderLayout.EAST);
		
	}
	
	public JPanel getPanel()
	{
		return pane;
	}	
	
}
