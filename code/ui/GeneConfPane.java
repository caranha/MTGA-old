package ui;

import javax.swing.*;
import java.util.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import engine.*;
import ga.*;
import memetic.*;

public class GeneConfPane implements ActionListener {

	JPanel pane;
		
	// Configuration fields
	JTextField Ngen;
	JTextField Npop;
	JTextField Elite;
	JTextField TourK;
	JTextField MutRt;
	JTextField Treesize;
	JTextField MemeChance;
	JTextField GuideChance;
	
	// Progress Bar
	JProgressBar progb;
	
	// maybe not needed.
	Portfolio GAPort;
	Portfolio GPPort;
	
	MarketComparatorPane mcc;
	
	public GeneConfPane(MarketComparatorPane m)
	{
		Parameter p = Parameter.getInstance();
		pane = new JPanel(new BorderLayout());
		mcc = m;
		
		// Setting up buttons
		JButton runGA = new JButton("Run GA");
		JButton runGP = new JButton("Run MTGA");
		JButton submit = new JButton("Set Parameters");
		runGA.addActionListener(this);
		runGP.addActionListener(this);
		submit.addActionListener(this);
		
		int i;
		if (p.getParam("generation size")!=null)
		{
			i = Integer.valueOf(p.getParam("generation size"));
		}
		else i = 100;
		progb = new JProgressBar(0,100);
		progb.setValue(0);
		progb.setStringPainted(true);

		JPanel subbutton = new JPanel(new BorderLayout());
		JPanel buttonp = new JPanel(new BorderLayout());		
		subbutton.add(runGA, BorderLayout.EAST);
		subbutton.add(runGP, BorderLayout.WEST);
		buttonp.add(progb, BorderLayout.NORTH);
		buttonp.add(submit,BorderLayout.WEST);
		buttonp.add(subbutton, BorderLayout.EAST);
		
		// Setting up fields
		JPanel controlp = new JPanel(new GridLayout(0,2));
		JLabel l = new JLabel("Number of Generations: ");
		Ngen = new JTextField(p.getParam("generation number"));
		controlp.add(l);
		controlp.add(Ngen);
		
		l = new JLabel("Population Size: ");
		Npop = new JTextField(p.getParam("population size"));
		controlp.add(l);
		controlp.add(Npop);
	
		l = new JLabel("Elite Size: ");
		Elite = new JTextField(p.getParam("Elite size"));
		controlp.add(l);
		controlp.add(Elite);
		
		l = new JLabel("Tournament K: ");
		TourK = new JTextField(p.getParam("Tournament K"));
		controlp.add(l);
		controlp.add(TourK);
		
		l = new JLabel("Mutation Rate:");
		MutRt = new JTextField(p.getParam("Mutation Rate"));
		controlp.add(l);
		controlp.add(MutRt);
		
		l = new JLabel("Maximum Tree Size:");
		Treesize = new JTextField(p.getParam("tree depth"));
		controlp.add(l);
		controlp.add(Treesize);
		

		l = new JLabel("Memetic Chance:");
		MemeChance = new JTextField(p.getParam("meme chance"));
		controlp.add(l);
		controlp.add(MemeChance);
		
		l = new JLabel("Guided Crossover Chance:");
		GuideChance = new JTextField(p.getParam("guided xover rate"));
		controlp.add(l);
		controlp.add(GuideChance);
		
		
		
		pane.add(controlp,BorderLayout.NORTH);
		pane.add(buttonp, BorderLayout.SOUTH);
	
	
	}
	
	public JPanel getPanel()
	{
		return pane;
	}
	
	// Checks if the button pressed is the RunGA button
	// or the RunGP button. Runs the GA/GP according
	// to the button. Then sends the resulting 
	// portfolio to the Market Comparator pane 
	// and to the PortComppane.
	public void actionPerformed(ActionEvent e)
	{
		Parameter p = Parameter.getInstance();
		Calendar c = Calendar.getInstance();
		Date startd;
		
		String param = p.getParam("initial trade date");
		if (param == null)
			c.set(2005, 00, 05);
		else
		{
			String[] date = param.split("-");
			c.set(Integer.valueOf(date[0]),
					Integer.valueOf(date[1]),
					Integer.valueOf(date[2]));
		}		
		startd = c.getTime();
		
		if(e.getActionCommand().equalsIgnoreCase("Run GA"))
		{
			GAPopulation ga = new GAPopulation(null);
			ga.initPopulation(startd);
			progb.setValue(0);
			for(int i = 0; i < ga.ngens; i++)
			{
				ga.runGeneration();
				System.out.print(".");
				progb.setValue(i);
			}
			ga.eval();			
			Portfolio gap = new Portfolio();
			
			gap.setWeights(ga.individual.get(0).getNormalWeights());
			mcc.newGATrader(gap);
		}
		
		if(e.getActionCommand().equalsIgnoreCase("Run MTGA"))
		{
			//GPSolver gp = new GPSolver(null);
			//gp.calculateanswer(startd);			
			MemePopulation gp = new MemePopulation(null);
			gp.initPopulation(startd);
			progb.setValue(0);
			for(int i = 0; i < gp.ngens; i++)
			{
				gp.runGeneration();
				System.out.print(".");
				progb.setValue(i);
			}
			gp.eval();			
			
			Portfolio gpp = gp.individual.get(0).generatePortfolio();
			System.out.println(gp.individual.get(0).dumptree(false));
			mcc.newGPTrader(gpp);
		}
		
		if(e.getActionCommand().equalsIgnoreCase("Set Parameters"));
		{
			p.setParam("generation number", Ngen.getText());
			p.setParam("population size", Npop.getText());
			p.setParam("Elite Size", Elite.getText());
			p.setParam("Tournament k", TourK.getText());
			p.setParam("mutation rate", MutRt.getText());
			p.setParam("tree depth", Treesize.getText());
			p.setParam("meme chance", MemeChance.getText());
			p.setParam("guided xover rate", GuideChance.getText());

			
			progb.setMaximum(Integer.valueOf(Ngen.getText()));
			progb.setValue(0);
			
		}
	}
	
}
