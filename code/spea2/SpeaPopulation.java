package spea2;
/**
 * GAPopulation
 * 
 * - Runs one population with array based individuals. This is 
 * based on Memepop, and runs only one scenario. Multiple 
 * scenarios should be run by tying this together on GASolver.
 *  
 */

import engine.*;
import java.util.*;
import ga.*;

public class SpeaPopulation {

	/* Evolutionary parameters: */
	public int size; // size of the population
	public int ngens; // total number of generations
	public int currgen; // current generation

	/* Crossover parameters */
	int tournamentK; // size of tournament
	int elite; // size of elite
	int immigrant; // number of new random individuals
	double mutrate; // chance that a mutation will occur
	double xoverrate; // chance that the xover will occur
		
	/* Containers */
	public ArrayList<Genome> individual;
	public ArrayList<Genome> archive;
	Genome parent; 
	Trainer T;
		
	/* Progress data */
	public double[] max_fitness;
	public double[] avg_fitness;
	public double[] terminals; // average total number of terminals
	public double[] bigterminals; // average total number of sig. terminals

	/**
	 * Initialize and load parameters.
	 * Parameter comp is a node from a previous 
	 * scenario, which is used for distance calculations.
	 */
	public SpeaPopulation(Genome comp)
	{
		individual = new ArrayList<Genome>();
		archive = new ArrayList<Genome>();
		
		parent = comp;

		// reading parameters
		
		Parameter param = Parameter.getInstance();		
		String paramval;
		
		paramval = param.getParam("population size");
		if (paramval != null)
			size = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"population size\" not defined on parameter file.");
			size = 10;
		}

		paramval = param.getParam("generation number");
		if (paramval != null)
			ngens = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"generation number\" not defined on parameter file.");
			ngens = 10;
		}
		
		paramval = param.getParam("tournament K");
		if (paramval != null)
			tournamentK = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"tournament K\" not defined on parameter file.");
			tournamentK = 5;
		}

		paramval = param.getParam("elite size");
		if (paramval != null)
			elite = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"elite size\" not defined on parameter file.");
			elite = 1;
		}
				
		paramval = param.getParam("immigrant size");
		if (paramval != null)
			immigrant = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"immigrant size\" not defined on parameter file.");
			immigrant = 0;;
		}
		
		paramval = param.getParam("mutation rate");
		if (paramval != null)
			mutrate = Double.valueOf(paramval);
		else
		{
			System.err.println("\"mutation rate\" not defined on parameter file.");
			mutrate = 0.01;
		}
		
		paramval = param.getParam("crossover rate");
		if (paramval != null)
			xoverrate = Double.valueOf(paramval);
		else
		{
			System.err.println("\"crossover rate\" not defined on parameter file.");
			xoverrate = 0.9;
		}
	
	}

	/**
	 * Initialize the new population and the local
	 * variables. Startd is the target date for the 
	 * @param startd
	 */
	public void initPopulation(Date startd)
	{
		T = new Trainer(startd);
		currgen = 0;
		for (int i = 0; i < size; i++)
		{
			Genome n = new Genome();
			n.init();
			individual.add(n);
			archive.add(n);
		}
		
		max_fitness = new double[ngens];
		avg_fitness = new double[ngens];
		terminals = new double[ngens];
		bigterminals = new double[ngens];
		
		/* Start archive is the same size as the population */
		
	}
	
	/**
	 * Runs one generation loop
	 *
	 */
	public void runGeneration()
	{
		/* Modified for SPEA */

		archiveMix(); // -- copies old archive into the population, and clears it.
		eval(); // -- Calculates Complex fitness, and makes new archive;
		archiveTrim(); // -- remove or add individuals until archive is right
		breed(); // -- Breed individuals from archive into population.
		currgen++;
	}
	
	public void archiveMix()
	{
		for (int i = 0; i < archive.size(); i++)
		{
			individual.add(archive.get(i));
		}
		archive.clear();
	}
	
	public void archiveTrim()
	{
		if (archive.size() == size)
			return;

		int diff = size - archive.size();
		if (diff > 0)
			// too few individuals
			for (int i = 0; i < diff; i++)
			{
				archive.add(individual.get(individual.size()-1));
				individual.remove(individual.size()-1);
			}
		else
			// too many individuals
			// I'm just removing the extra ones, I should be 
			// calculating their distance.
			for (int i = 0; i < diff*-1; i++)
			{
				archive.remove(archive.size());
			}
	}
	
	
	/**
	 * update the values of the maxfitness/avg fitness/etc
	 * public arrays;
	 */
	public void updateStatus()
	{
		Parameter p = Parameter.getInstance();
		String param = p.getParam("asset treshold");
		double tresh = Double.valueOf(param);
		
		avg_fitness[currgen-1] = 0;
		terminals[currgen-1] = 0;
		bigterminals[currgen-1] = 0;

		double bigfit = 0;
		
		for (int i = 0; i < archive.size(); i++)
		{
			avg_fitness[currgen-1] += archive.get(i).Sharpe;			
			if (archive.get(i).Sharpe > bigfit)
				bigfit = avg_fitness[currgen -1];
			terminals[currgen-1] += archive.get(i).countAsset(0.0);
			bigterminals[currgen-1] += archive.get(i).countAsset(tresh);
		}
		
		max_fitness[currgen-1] = bigfit;
		avg_fitness[currgen-1] /= size;
		terminals[currgen-1] /= size;
		bigterminals[currgen-1] /= size;
	
	}
	
	
	/* Returns 1 if g1 dominates g2, -2 if g2 dominates g1, and 0
	 * if neither dominates the other.
	 */
	int dominates(Genome g1, Genome g2)
	{
		if ((g1.Ereturn > g2.Ereturn) && (g1.Risk <= g2.Risk))
			return 1;
		if ((g1.Ereturn >= g2.Ereturn) && (g1.Risk < g2.Risk))
			return 1;
		if ((g1.Ereturn < g2.Ereturn) && (g1.Risk >= g2.Risk))
			return -1;
		if ((g1.Ereturn <= g2.Ereturn) && (g1.Risk > g2.Risk))
			return -1;
		
		return 0;
	}
	
	/**
	 * Calculates the fitness value for each individual
	 * in the population.
	 */
	public void eval()
	{
		// Calculate complete SPEA2 fitness
		
		
		// Step one: Real fitness (Risk and return)
		for (int i = 0; i < size; i++)
		{
			individual.get(i).eval(T);
			individual.get(i).fitness = 0.0; 
			// fitness is not sharpe ration anymore
		}
		
		
		int dom;
		int[] S;
		S = new int[individual.size()];
		
		// Step Two: Strength value
		for (int i = 0; i < individual.size(); i++)
			for(int j = i+1; j < individual.size(); j++)
			{
				dom = dominates (individual.get(i), individual.get(j));
				if (dom > 0)
					S[i] += 1;
				if (dom < 0)
					S[j] += 1;
			}
		
		// Step Three: RAW fitness
		for (int i = 0; i < individual.size(); i++)
			for (int j = i+1; j < individual.size(); j++)
			{
				dom = dominates (individual.get(i), individual.get(j));
				if (dom > 0)
					individual.get(j).fitness += S[i];
				if (dom < 0)
					individual.get(i).fitness += S[j];
			}

		
		// Step 4: Distance
		ArrayList<Double> distance;
		distance = new ArrayList<Double>();
		long k = Math.round(Math.pow(size, 0.5));
		
		for (int i = 0; i < individual.size(); i++)
		{
			for (int j = 0; j < individual.size(); j++)
				if (i!=j)
				{
					double dist = 0;
					dist += Math.pow(individual.get(i).Ereturn - individual.get(j).Ereturn,2) 
							+ Math.pow(individual.get(i).Risk - individual.get(j).Risk, 2);
					dist = Math.pow(dist, 0.5);
					distance.add(dist);
				}
			Collections.sort(distance);
			individual.get(i).fitness += 1/(2+distance.get((int) k));
		}

	
		// Step 5: Make new Archive
		Collections.sort(individual);
		while (individual.get(individual.size()-1).fitness < 0 && (individual.size() > 0));
		{
			archive.add(individual.get(individual.size()-1));
			individual.remove(individual.size()-1);
		}
		
	}
	
	/**
	 * Perform selection, crossover, mutation in 
	 * order to create a new population.
	 * 
	 * Assumes the eval function has already been
	 * performed.
	 *
	 */
	public void breed()
	{
		// for SPEA, the new individuals are bred from the archive.
		RNG d = RNG.getInstance();
		ArrayList<Genome> nextGen = new ArrayList<Genome>();
		Genome p1,p2;
		
		// crossover:
		for (int i = 0; i < size; i+=2)
		{
			// selection - the selection function should 
			// return copies already.
			p1 = Tournament();
			p2 = Tournament();
			
			// rolls for xover
			if (d.nextDouble() < xoverrate)
			{
				p1.crossover(p2);
			}				
			
			// rolls for mutation
			if (d.nextDouble() < mutrate)
				p1.mutation();
			if (d.nextDouble() < mutrate)
				p2.mutation();
			
			nextGen.add(p1);
			nextGen.add(p2);
			
		}
		
		individual = nextGen;		
	}
	
	/**
	 * Select one parent from the population by using
	 * fitness-proportional tournament selection 
	 * (each candidate has a chance proportional to his 
	 * fitness of being chosen).
	 * 
	 * The function copy the chosen candidate and send
	 * him back.
	 * @return
	 */
	public Genome Tournament()
	{
		// EDIT: Tournament selection in SPEA is done by binary
		// deterministic tournament.
		
		RNG d = RNG.getInstance();

		Genome g1 = archive.get(d.nextInt(archive.size())).copy();
		Genome g2 = archive.get(d.nextInt(archive.size())).copy();
		if (g1.fitness > g2.fitness)
			return g2;
		else
			return g1;
		
	}
	
}

