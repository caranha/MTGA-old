package testing;

import data.*;
import java.util.*;
import engine.*;
import ga.*;
import memetic.*;
import java.lang.management.*;
import java.io.*;
import java.text.*;

public class Simulation {
	
	public static void main(String[] args) {

	long totalTime = 0;
		
	if (args.length == 0)
	{
		System.out.println("Error: No parameter file indicated!");
		System.exit(1);
	}
	
	System.out.println(":Starting Simulation with parameter file: - " + args[0]);

	
	Parameter param = Parameter.getInstance(); 
	try {
		param.load(args[0]);		
	}
	catch (Exception e) {
		System.err.print(e.getMessage());
		System.exit(1);
	} 

	
	String parameter;
	Calendar c = Calendar.getInstance(); 	
	Market mkt = Market.getInstance();
	RNG die = RNG.getInstance();

	parameter = param.getParam("data directory");
	mkt.loadDir(parameter);	
	
	
	parameter = param.getParam("date");
	String[] date = parameter.split("-");
	c.set(Integer.parseInt(date[0]),
		  Integer.parseInt(date[1]),
		  Integer.parseInt(date[2]));

	parameter = param.getParam("Random Seed");
	die.setSeed(Integer.parseInt(parameter));
	
	parameter = param.getParam("Method");
	
	// Calculating the Time spent in my code
	// use CPU time for total time (including system calls) 
	System.out.println("Starting Evolution...");

    
    if (parameter.compareToIgnoreCase("mtga") == 0)
    {
    	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    	long initTime = bean.getCurrentThreadUserTime( );
    	MemePopulation pop = new MemePopulation(null);
    	pop.initPopulation(c.getTime());	

    	try{
    		BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness"));	
		
    		for (int i = 0; i < pop.ngens; i++)
    		{
    			pop.runGeneration();
    			pop.updateStatus();

    			fitnessfile.write(i + " " +
						pop.max_fitness[i] + " " +
						pop.avg_fitness[i] + " " +
						pop.terminals[i] + " " + 
						pop.bigterminals[i] + " " +
						pop.nodes[i] + " " +
						pop.introns[i] + "\n");
    		}

    		totalTime = bean.getCurrentThreadUserTime() - initTime;
    		fitnessfile.close();
    	}
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
    	try{
    		BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	
				
    		Double runtime = totalTime/1000000000.0;
    		
    		NumberFormat f = DecimalFormat.getInstance();
    		String times = f.format(runtime);
    		
    		finalfile.write(times + "\n");
    		System.out.println(times);
    		
    		finalfile.write("\nSharpe, Return and Risk:\n\n");
    		finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + "\n");
    		finalfile.write("\nPortfolio Structure\n");
    		Portfolio P = pop.individual.get(0).generatePortfolio();
    		finalfile.write(P.dump(0.0));
    		finalfile.close();
			}	
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    	}
    }
    
    if (parameter.compareToIgnoreCase("ga") == 0)
    {
    	ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
    	long initTime = bean.getCurrentThreadUserTime( );
    	GAPopulation pop = new GAPopulation(null);
    	pop.initPopulation(c.getTime());	

    	try{
    		BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness"));	
		
    		for (int i = 0; i < pop.ngens; i++)
    		{
    			pop.runGeneration();
    			pop.updateStatus();

    			fitnessfile.write(i + " " +
						pop.max_fitness[i] + " " +
						pop.avg_fitness[i] + " " +
						pop.terminals[i] + " " + 
						pop.bigterminals[i] + " " +
						0 + " " +
						0 + "\n");
    		}

    		totalTime = bean.getCurrentThreadUserTime() - initTime;
    		fitnessfile.close();
    	}
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
    	try{
    		BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

    		
    		finalfile.write(totalTime + "\n");
    		finalfile.write("\nSharpe, Return and Risk:\n\n");
    		finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + "\n");
    		finalfile.write("\nPortfolio Structure\n");
    		Portfolio P = new Portfolio();
    		P.setWeights(pop.individual.get(0).getNormalWeights());
    		finalfile.write(P.dump(0.0));
    		finalfile.close();
			}	
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    	}
    }
    
	}    
    
	
	

	
	

	
}


