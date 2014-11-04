package testing;

import data.*;

import java.util.*;

import engine.*;
import memetic.*;
import ga.*;
import diffevo.*;
import mtbma.*;
import java.io.*;
import java.lang.management.*;

import spea2.*;

public class Testmain {
	
	public static void main(String[] args) {

		/* Redirecting STDERR
		try {
			FileOutputStream fos = new FileOutputStream("debug");
			PrintStream ps = new PrintStream(fos);
			System.setErr(ps);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		*/
		
		//asset_distance_ext(args);
		TEC_experiment(args);
			
	}
	
	static void asset_distance_ext(String[] args){
		Parameter param = Parameter.getInstance(); 
		try {
			param.load(args[0]);		
		}
		catch (Exception e) {
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		
		//param.debugPrintContents();
		
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);

		// building a covariance matrix to save trouble

		try
		{
			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");

				BufferedWriter indstart = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".ind_start"));
				BufferedWriter indend = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".ind_end"));
				BufferedWriter avgstart = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".avgall_start"));
				BufferedWriter avgend = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".avgall_end"));
				BufferedWriter avgbest = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".avgbest"));
				BufferedWriter fitness = new BufferedWriter(new FileWriter(args[0] + "." + curr + ".fitness"));
				
				// Individual files: fitness, i, j, dist, i, j, dist, i, j, dist
				// average files: i, j, dist, error, covariance
				MemePopulation pop = new MemePopulation(null);
				pop.initPopulation(c.getTime());
				
				for (int i = 0; i < pop.ngens; i++)
				{
					
					System.out.print(".");

					pop.runGeneration();
					pop.updateStatus();

					// start file
					if (i == 0)
					{	
						fitness.write(pop.individual.get(0).fitness + " ");
						double[][] distavg = new double[mkt.assets.size()][mkt.assets.size()];
						int[][] distcount = new int[mkt.assets.size()][mkt.assets.size()];
						double[][] distvar = new double[mkt.assets.size()][mkt.assets.size()];
						// writing individual file: fitness, i,j, distance
						for (int j = 0; j < pop.individual.size(); j++)
						{
							indstart.write(pop.individual.get(j).fitness.toString());
							int[] rootdist = new int[mkt.assets.size()];
							int[][] ret = pop.individual.get(j).calculateDistanceMatrix(rootdist);
							if (ret != null)
								for (int ii = 0; ii < mkt.assets.size(); ii++)
									for (int jj = 0; jj < ii+1; jj++)
										if (ret[ii][jj] > 0) 
										{
											indstart.write(","+ii+","+jj+","+ret[ii][jj]);
											distcount[ii][jj] ++;
											double delta = ret[ii][jj] - distavg[ii][jj];
											distavg[ii][jj] = distavg[ii][jj] + delta/distcount[ii][jj];
											distvar[ii][jj] = distvar[ii][jj] + delta*(ret[ii][jj] - distavg[ii][jj]);
										}
							indstart.write("\n");										
						}
						// writing average file: i,j,distance,err,cov
						for (int ii = 0; ii < mkt.assets.size(); ii++)
							for (int jj = 0; jj < ii+1; jj++)
								if (distcount[ii][jj] > 1)
								{
									avgstart.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+distvar[ii][jj]/(distcount[ii][jj]-1)+","+pop.T.correlation[ii][jj]+"\n");
								}
								else if (distcount[ii][jj] == 1)
								{
									avgstart.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+0+","+pop.T.correlation[ii][jj]+"\n");
								}
						indstart.close();
						avgstart.close();
					} // End distance file

					// Doing the same thing for the last generation
					if (i == pop.ngens - 1)
					{	
						fitness.write(pop.individual.get(0).fitness + "\n");
						double[][] distavg = new double[mkt.assets.size()][mkt.assets.size()];
						int[][] distcount = new int[mkt.assets.size()][mkt.assets.size()];
						double[][] distvar = new double[mkt.assets.size()][mkt.assets.size()];
						// writing individual file: fitness, i,j, distance
						for (int j = 0; j < pop.individual.size(); j++)
						{
							indend.write(pop.individual.get(j).fitness.toString());
							int[] rootdist = new int[mkt.assets.size()];
							int[][] ret = pop.individual.get(j).calculateDistanceMatrix(rootdist);
							if (ret != null)
								for (int ii = 0; ii < mkt.assets.size(); ii++)
									for (int jj = 0; jj < ii+1; jj++)
										if (ret[ii][jj] > 0) 
										{
											indend.write(","+ii+","+jj+","+ret[ii][jj]);
											distcount[ii][jj] ++;
											double delta = ret[ii][jj] - distavg[ii][jj];
											distavg[ii][jj] = distavg[ii][jj] + delta/distcount[ii][jj];
											distvar[ii][jj] = distvar[ii][jj] + delta*(ret[ii][jj] - distavg[ii][jj]);
										}
							indend.write("\n");										
						}
						// writing average file: i,j,distance,err,cov
						for (int ii = 0; ii < mkt.assets.size(); ii++)
							for (int jj = 0; jj < ii+1; jj++)
								if (distcount[ii][jj] > 1)
								{
									avgend.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+distvar[ii][jj]/(distcount[ii][jj]-1)+","+pop.T.correlation[ii][jj]+"\n");
								}
								else if (distcount[ii][jj] == 1)
								{
									avgend.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+0+","+pop.T.correlation[ii][jj]+"\n");
								}
						indend.close();
						avgend.close();
						fitness.close();
					} // finish end-of-run file 
					
					// creating best-indiv file
					if (i == pop.ngens - 1)
					{	
						double[][] distavg = new double[mkt.assets.size()][mkt.assets.size()];
						int[][] distcount = new int[mkt.assets.size()][mkt.assets.size()];
						double[][] distvar = new double[mkt.assets.size()][mkt.assets.size()];
						// writing individual file: fitness, i,j, distance
						for (int j = 0; j < pop.individual.size()/10; j++)
						{
							int[] rootdist = new int[mkt.assets.size()];
							int[][] ret = pop.individual.get(j).calculateDistanceMatrix(rootdist);
							if (ret != null)
								for (int ii = 0; ii < mkt.assets.size(); ii++)
									for (int jj = 0; jj < ii+1; jj++)
										if (ret[ii][jj] > 0) 
										{
											distcount[ii][jj] ++;
											double delta = ret[ii][jj] - distavg[ii][jj];
											distavg[ii][jj] = distavg[ii][jj] + delta/distcount[ii][jj];
											distvar[ii][jj] = distvar[ii][jj] + delta*(ret[ii][jj] - distavg[ii][jj]);
										}
						}
						// writing average file: i,j,distance,err,cov
						for (int ii = 0; ii < mkt.assets.size(); ii++)
							for (int jj = 0; jj < ii+1; jj++)
								if (distcount[ii][jj] > 1)
								{
									avgbest.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+distvar[ii][jj]/(distcount[ii][jj]-1)+","+pop.T.correlation[ii][jj]+"\n");
								}
								else if (distcount[ii][jj] == 1)
								{
									avgbest.write(ii+","+jj+","+distcount[ii][jj]+","+distavg[ii][jj]+","+0+","+pop.T.correlation[ii][jj]+"\n");
								}
						avgbest.close();
					} // finish best-indiv file

				
				}	

				System.out.println(" done");

			} 
		}
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
	}
	
	static void TEC_experiment(String[] args){
		Parameter param = Parameter.getInstance(); 
		try {
			param.load(args[0]);		
		}
		catch (Exception e) {
			System.err.print(e.getMessage());
			System.exit(1);
		} 
		
		String p = param.getParam("type");

		switch(Integer.parseInt(p))
		{
		case 0:
			TEC_memetic(args);
			break;
		case 1:
			TEC_static(args);
			break;
		case 2:
			TEC_motion(args);
			break;
		}
	}

	

	static void TEC_memetic(String[] args) {
		
		Parameter param = Parameter.getInstance(); 
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
						
		try
		{
	    
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	

				MemePopulation pop = new MemePopulation(null);
				pop.initPopulation(c.getTime());	
			
				for (int i = 0; i < pop.ngens; i++)
				{
					if ((i+1)%20 == 0)
						System.out.print(".");

					pop.runGeneration();
					pop.updateStatus();
					
					fitnessfile.write(i + " " +
							pop.max_fitness[i] + " " +
							pop.avg_fitness[i] + " " +
							pop.diversity[i] + " 0 0 0" +
							"\n");
				}	

				System.out.println(" done");

				fitnessfile.close();
	      			
				finalfile.write(pop.individual.get(0).fitness + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + "\n");

				System.out.println("Totals: "+pop.mutcount+" "+
						pop.memecount+" "+pop.crosscount);
			} 
			finalfile.close();
		}
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
		
	}

	static void TEC_static(String[] args) {
		
		Parameter param = Parameter.getInstance(); 
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
						
		try
		{
	    
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	

				StbmaPopulation pop = new StbmaPopulation();
				pop.initPopulation(c.getTime());	
			
				for (int i = 0; i < pop.ngens; i++)
				{
					if ((i+1)%20 == 0)
						System.out.print(".");

					pop.runGeneration();
					pop.updateStatus();
					
					fitnessfile.write(i + " " +
							pop.max_fitness[i] + " " +
							pop.avg_fitness[i] + " " +
							pop.diversity[i] + " 0 0 0" +
							"\n");
				}	

				System.out.println(" done");

				fitnessfile.close();
	      			
				finalfile.write(pop.individual.get(0).fitness + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + "\n");

				System.out.println("Totals: "+pop.mutcount+" "+
						pop.memecount+" "+pop.crosscount);

			}
			finalfile.close();
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
		
		
	}

	static void TEC_motion(String[] args) {
		
		Parameter param = Parameter.getInstance(); 
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
						
		try
		{
	    
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
				BufferedWriter terrainfit = new BufferedWriter(new FileWriter(args[0] + ".terfit_" + curr));	
				BufferedWriter terrainpop = new BufferedWriter(new FileWriter(args[0] + ".terpop_" + curr));	

				MtbmaPopulation pop = new MtbmaPopulation();
				pop.initPopulation(c.getTime());	
			
				for (int i = 0; i < pop.ngens; i++)
				{
					if ((i+1)%20 == 0)
						System.out.print(".");

					pop.runGeneration();
					pop.updateStatus();
					
					fitnessfile.write(i + " " +
							pop.max_fitness[i] + " " +
							pop.avg_fitness[i] + " " +
							pop.diversity[i] + " " +
							pop.citytotal[i] + " " +
							pop.memevalue[i][0] + " " + pop.memevalue[i][1] +
							"\n");
					for (int ii = 0; ii < pop.max_Y; ii++)
					{
						for (int jj = 0; jj < pop.max_X; jj++)
						{
							terrainfit.write(pop.cityfitness[i][jj][ii] + " ");
							terrainpop.write(pop.citypop[i][jj][ii] + " ");
						}
						terrainfit.write("\n");
						terrainpop.write("\n");
					}
					terrainfit.write("\n\n");
					terrainpop.write("\n\n");
				}	

				System.out.println(" done");

				fitnessfile.close();
	      		terrainfit.close();	
	      		terrainpop.close();
				
				finalfile.write(pop.individual.get(0).fitness + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + "\n");

				System.out.println("Totals: "+pop.mutcount+" "+
						pop.memecount+" "+pop.crosscount);

			}
			finalfile.close();
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
		
		
	}
	
	
	static void Simple_simulation(String[] args) {

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
		
		System.out.println("Starting Evolution...");

	    
		MtbmaPopulation pop = new MtbmaPopulation();
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
						//pop.nodes[i] + " " +
						//pop.introns[i] + 
						"\n");
				
				
				System.out.print(".");
	    		}	

			
			
			fitnessfile.close();
	    	}
	    	catch(IOException e)
	    	{
	    		System.err.print(e.getMessage());
	    		System.exit(1);
	    	}
	    	try{
	    		BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	
	    			    		
	    		finalfile.write("\nSharpe, Return and Risk:\n\n");
	    		finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
							pop.individual.get(0).Ereturn + " " +
							pop.individual.get(0).Risk + "\n");
	    		finalfile.write("\nPortfolio Structure\n");
	    		//Portfolio P = pop.individual.get(0).generatePortfolio();
	    		//finalfile.write(P.dump(0.0));
	    		finalfile.close();
				}	
	    	catch(IOException e)
	    	{
	    		System.err.print(e.getMessage());
	    	}
	    
	}    
	
	static void tbma_experiment(String args[])
	{
		
		long totalTime = 0;
		
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
				
		//init time counter.
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		
		try
		{
	    
			// Global Files
			BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port"));	
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
				long initTime = bean.getCurrentThreadUserTime( );

				StbmaPopulation pop = new StbmaPopulation();
				pop.initPopulation(c.getTime());	
			
				for (int i = 0; i < pop.ngens; i++)
				{
					if ((i+1)%20 == 0)
						System.out.print(".");

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

				System.out.println(" done" + pop.mutcount + " " + pop.memecount);

				totalTime = bean.getCurrentThreadUserTime() - initTime;
				fitnessfile.close();
	      			
				finalfile.write(pop.individual.get(0).fitness + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + " " + totalTime + "\n");

				Portfolio P = pop.individual.get(0).generatePortfolio();
				portfile.write(P.dump(0.0) + "\n");
			}
			finalfile.close();
			portfile.close();
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
	}
	
	/*
	 * Experimento simples para tentar rodar o Differential Evolution.
	 * Runs Differential Evolutionary experiments N times.
	 */
	static void DE_experiment(String args[])
	{
		
		long totalTime = 0;
		
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
				
		//init time counter.
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		
		try
		{
	    
			// Global Files
			BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port"));	
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
			{	      		
				System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
				long initTime = bean.getCurrentThreadUserTime( );

				DEPopulation pop = new DEPopulation(null);
				pop.initPopulation(c.getTime());	
			
				for (int i = 0; i < pop.ngens; i++)
				{
					if ((i+1)%20 == 0)
						System.out.print(".");

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

				System.out.println(" done");

				totalTime = bean.getCurrentThreadUserTime() - initTime;
				fitnessfile.close();
	      			
				finalfile.write(pop.individual.get(0).fitness + " " + 
						pop.individual.get(0).Ereturn + " " +
						pop.individual.get(0).Risk + " " + totalTime + "\n");

				Portfolio P = new Portfolio();
				P.setWeights(pop.individual.get(0).getNormalWeights());
				portfile.write(P.dump(0.0) + "\n");
			}
			finalfile.close();
			portfile.close();
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}
		
		
		

		
	}
	
	/* 
	 * Experiment for GECCO 2009
	 * 
	 * Evolves a portfolio for the initial time, then rebalances that
	 * portfolio for one year. Reports the evolved portfolio, 
	 * risk/return/sharpe values, and portfolio distance 
	 * (euclidian distance + min change cost) in different files.
	 * 
	 * Does the above using local optimization and re-evolution.
	 */
	static void experiment_jan_rebalancing(String args[])
	{
		//long totalTime = 0;
		
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

		Portfolio P = new Portfolio();
		Portfolio oldP = new Portfolio();
		
		// Calculating the Time spent in my code
		// use CPU time for total time (including system calls)
	    //ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
	    //long initTime = bean.getCurrentThreadUserTime( );

		System.out.println("\nStarting Memetic Run...");		
		
		parameter = param.getParam("date");
		date = parameter.split("-");
		c.set(Integer.parseInt(date[0]),
			  Integer.parseInt(date[1]),
			  Integer.parseInt(date[2]));
		P = new Portfolio();
		
		try{
			
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end1"));	
			BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port1"));
			BufferedWriter difffile = new BufferedWriter(new FileWriter(args[0] + ".diff1"));


			MemePopulation pop = new MemePopulation(null);
			pop.initPopulation(c.getTime());	
		
			for (int j = 0; j < pop.ngens; j++)
			{
				pop.runGeneration();
				pop.updateStatus();
				if (j%10 == 0)
					System.out.printf(".");
			}

			/*
			 * If this is inside the loop, then the memetic optimization
			 * always happens from the original portfolio.
			 * If this is outside the loop, the memetic optimization
			 * happens from the last good portfolio. 
			 * 
			 * Check both.
			 */
			pop.eval();
			//MemeNode sol = pop.individual.get(0);

			
			for (int i = 0; i < 12; i++)
			{
				MemeNode sol = pop.individual.get(0).copy();
				Trainer T = new Trainer(c.getTime());
		
				sol.memetize(T);
				sol.eval(T);
				
				oldP = P;
				P = sol.generatePortfolio();

				double portDiff = 0;
				if (i != 0)
				{
					for (int j = 0; j < mkt.assets.size(); j++)
					{
						double a = Math.sqrt(Math.pow((oldP.getWeightByIndex(j) - P.getWeightByIndex(j)),2));
						if (a > 0 && a < 0.03)
							a = 0.03;
						portDiff += a;
					}
				}
									
				finalfile.write(sol.Sharpe + "\t" + sol.Ereturn + "\t" +
						sol.Risk + "\n");			
				portfile.write("Month: " + i + "\n" + P.dump(0.0)+"\n");			
				difffile.write(i + " " + portDiff + "\n");
				c.add(Calendar.MONTH, 1);						
			}	
			finalfile.close();
			portfile.close();
			difffile.close();
		}
		catch(IOException e)
		{
				System.err.print(e.getMessage());
		}

		
		System.out.println("\nStarting Evolutive run...");
		
		parameter = param.getParam("date");
		date = parameter.split("-");
		c.set(Integer.parseInt(date[0]),
			  Integer.parseInt(date[1]),
			  Integer.parseInt(date[2]));
		P = new Portfolio();		
		
		try{
			
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end0"));	
			BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port0"));
			BufferedWriter difffile = new BufferedWriter(new FileWriter(args[0] + ".diff0"));

			
			for (int i = 0; i < 12; i++)
			{
				MemePopulation pop = new MemePopulation(null);
				pop.initPopulation(c.getTime());	
			
				for (int j = 0; j < pop.ngens; j++)
				{
					pop.runGeneration();
					pop.updateStatus();
					if (j%10 == 0)
						System.out.printf(".");
				}
				MemeNode sol = pop.individual.get(0);
				Trainer T = new Trainer(c.getTime());
		

				sol.eval(T);
				
				oldP = P;
				P = sol.generatePortfolio();

				double portDiff = 0;
				if (i != 0)
				{
					for (int j = 0; j < mkt.assets.size(); j++)
					{
						double a = Math.sqrt(Math.pow((oldP.getWeightByIndex(j) - P.getWeightByIndex(j)),2));
						if (a > 0 && a < 0.03)
							a = 0.03;
						portDiff += a;
					}
				}
									
				finalfile.write(sol.Sharpe + "\t" + sol.Ereturn + "\t" +
						sol.Risk + "\n");			
				portfile.write("Month: " + i + "\n" + P.dump(0.0)+"\n");			
				difffile.write(i + " " + portDiff + "\n");
				c.add(Calendar.MONTH, 1);						
			}	
			finalfile.close();
			portfile.close();
			difffile.close();
		}
		catch(IOException e)
		{
				System.err.print(e.getMessage());
		}


		
		
	}
	
	/*
	 * Repeats one simulation N times, and reports all data from it.
	 * 
	 * This experiments execute the static simulation for a one month scenario, and 
	 * reports the results. It must automagically remove bad data from the dataset,
	 * repeat the experiment a number of times indicated by the "experiment_repeat" 
	 * parameter, and print everything to different files indicated by the 
	 * parameter file name.
	 */
	static void experiment_sim_fulldata(String args[])
	{
		long totalTime = 0;
		
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
		
		parameter = param.getParam("Method");
		
		//init time counter.
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		
		try
		{
	    
			if (parameter.compareToIgnoreCase("mtga") == 0)
			{
				
				// Global Files
	      		BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port"));	
	      		BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	
	      		
	      		for (int curr = 0; curr < maxrepeat; curr++)
	      		{	      		
	      			System.out.print("Starting Repetition " + curr + " ");
	      			BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
	      			long initTime = bean.getCurrentThreadUserTime( );
	      			MemePopulation pop = new MemePopulation(null);
	      			pop.initPopulation(c.getTime());
	      			
	      			for (int i = 0; i < pop.ngens; i++)
	      			{
	      				if ((i+1)%20 == 0)
	      					System.out.print(".");
	      					
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

	      			System.out.println(" done");
	      			pop.eval();
	      			
	      			totalTime = bean.getCurrentThreadUserTime() - initTime;
	      			fitnessfile.close();						      			
	      			
	      			finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
	      					pop.individual.get(0).Ereturn + " " +
	      					pop.individual.get(0).Risk + " " + totalTime + "\n");

	      			Portfolio P = pop.individual.get(0).generatePortfolio();
	      			portfile.write(P.dump(0.0) + "\n");
	      		}
	    		finalfile.close();
	    		portfile.close();
			}
	    
			if (parameter.compareToIgnoreCase("ga") == 0)
			{
				// Global Files
				BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port"));	
				BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

				for (int curr = 0; curr < maxrepeat; curr++)
	      		{	      		
	      			System.out.print("Starting Repetition " + curr + " ");
					BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
	      			long initTime = bean.getCurrentThreadUserTime( );

	      			GAPopulation pop = new GAPopulation(null);
	      			pop.initPopulation(c.getTime());	
			
	      			for (int i = 0; i < pop.ngens; i++)
	      			{
	      				if ((i+1)%20 == 0)
	      					System.out.print(".");

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

	      			System.out.println(" done");

	      			totalTime = bean.getCurrentThreadUserTime() - initTime;
	      			fitnessfile.close();
	      			
	      			finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
	      					pop.individual.get(0).Ereturn + " " +
	      					pop.individual.get(0).Risk + " " + totalTime + "\n");

	      			Portfolio P = new Portfolio();
	      			P.setWeights(pop.individual.get(0).getNormalWeights());
	      			portfile.write(P.dump(0.0) + "\n");
	      		}
	      		finalfile.close();
	      		portfile.close();
			}
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}

	
	}
	
	/*
	 * Runs a simulation N times, using the SPEA algorithm.
	 */
	static void experiment_spea(String args[])
	{
		long totalTime = 0;
		
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
		
		parameter = param.getParam("Repeat Experiment");
		int maxrepeat = Integer.parseInt(parameter);
		
		parameter = param.getParam("Method");
		
		//init time counter.
		ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
		
		try
		{
    		// Global Files
			BufferedWriter portfile = new BufferedWriter(new FileWriter(args[0] + ".port"));	
			BufferedWriter finalfile = new BufferedWriter(new FileWriter(args[0] + ".end"));	

			for (int curr = 0; curr < maxrepeat; curr++)
      		{	      		
      			System.out.print("Starting Repetition " + curr + " ");
				BufferedWriter fitnessfile = new BufferedWriter(new FileWriter(args[0] + ".fitness_" + curr));	
      			long initTime = bean.getCurrentThreadUserTime( );

      			SpeaPopulation pop = new SpeaPopulation(null);
      			pop.initPopulation(c.getTime());	
			
      			for (int i = 0; i < pop.ngens; i++)
      			{
      				if ((i+1)%20 == 0)
      					System.out.print(".");
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

      			System.out.println(" done");

      			totalTime = bean.getCurrentThreadUserTime() - initTime;
      			fitnessfile.close();
	      			
      			finalfile.write(pop.max_fitness[pop.ngens-1] + " " + 
      					pop.individual.get(0).Ereturn + " " +
      					pop.individual.get(0).Risk + " " + totalTime + "\n");
	      			Portfolio P = new Portfolio();
	      			P.setWeights(pop.individual.get(0).getNormalWeights());
	      			portfile.write(P.dump(0.0) + "\n");
	      		}
	      		finalfile.close();
	      		portfile.close();
	    }
    	catch(IOException e)
    	{
    		System.err.print(e.getMessage());
    		System.exit(1);
    	}

	
	}
	
	/**
	 * Prints Sharpe, Return for a number of assets.
	 */
	static void print_index(String args[])
	{
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
		
		parameter = param.getParam("data directory");
		mkt.loadDir(parameter);	
		mkt.loadDirIndex(parameter);
				
		parameter = param.getParam("date");
		String[] date = parameter.split("-");
		c.set(Integer.parseInt(date[0]),
			  Integer.parseInt(date[1]),
			  Integer.parseInt(date[2]));
		
		
		double[] price;
		Double[] profit;
		double ereturn = 0;
		double risk = 0;
		int in;
		price = new double[13];
		profit = new Double[12];
		
		// 06-02
		c.set(2005, 0, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);
		
		
		// 06-11
		c.set(2005, 9, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);

		// 07-02
		c.set(2006, 0, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);

		// 07-11
		c.set(2006, 9, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);

		// 08-02
		c.set(2007, 0, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);

		// 08-11
		c.set(2007, 9, 5);
		in = mkt.getIndexPosByDate(c.getTime());
		for (int i = 0; i < 13; i++)
		{
			price[i] = mkt.index.get(in+i).p;
			if (i > 0)
			{
				profit[i-1] = Math.log(price[i]/price[i-1]);
				ereturn += profit[i-1];
			}
		}		
		ereturn /= 12;
		risk = Math.sqrt(MathFun.variance(profit));		
		System.out.println(ereturn + " " + (ereturn - 0.03)/risk);
		
		
	}
	
	
}