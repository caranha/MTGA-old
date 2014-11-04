package diffevo;
/**
 * DEPopulation
 * 
 * Evolves the Solution using the Differential Evolution
 * Framework described in:
 * "An Enhanced Memetic Differential Evolution in Filter 
 * Design for Defect Detection in Paper Production"
 * Tirronem et. al. 2008, Evolutionary Computation.
 * 
 *  
 *  Basic Algorithm: Steady state, with new individual an
 *  created as: a1 + K(a2 - a3), and an replaces a4 if 
 *  f(an) > f(a4) (with a smidge of crossover between an and 
 *  a4.
 *  
 *  This DE is based on array genomes.
 */

import data.Market;
import engine.*;
import java.util.*;
import ga.Genome;

public class DEPopulation {

	/* Evolutionary parameters: */
	/* Steady state -> No generations per se, but every 
	 * "generation", SIZE new solutions are generated.
	 */ 
	public int size; // size of the population
	public int ngens; // total number of generations
	public int currgen; // current generation
	int init_flag; // Indicates whether to initialize a special population for DE.
	int SPX_flag; // Indicates whether to use the SPX operator
	
	/* Crossover parameters */
	/* None needed - xoverrate is the xover of aN and a4 */
	//int tournamentK; 
	//int elite;
	//int immigrant;
	double DE_K; // parameter for new genome generation in DE
	double DE_C; // parameter for switch with a4 in DE
	double Psigma; // Control parameter for SPX crossover from Noman
	int Pnp; //SPX neighborhood parameter.	
	
	/* Containers */
	public ArrayList<Genome> individual; 
	int a1, a2, a3, a4; // parents are replaced by indexes;
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
	public DEPopulation(Genome comp)
	{
		individual = new ArrayList<Genome>();

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
				
		paramval = param.getParam("DE K value");
		if (paramval != null)
			DE_K = Double.valueOf(paramval);
		else
		{
			System.err.println("\"DE K value\" not defined on parameter file.");
			DE_K = 0.9;
		}

		paramval = param.getParam("DE C value");
		if (paramval != null)
			DE_C = Double.valueOf(paramval);
		else
		{
			System.err.println("\"DE Crossover value\" not defined on parameter file.");
			DE_C = 0.9;
		}
		
		paramval = param.getParam("Noman Control Parameter");
		if (paramval != null)
			Psigma = Double.valueOf(paramval);
		else
		{
			System.err.println("\"Noman Control Parameter\" not defined on parameter file.");
			Psigma = 1.0;
		}

		paramval = param.getParam("Init DE Population");
		if (paramval != null)
			init_flag = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"Init DE Population\" not defined on parameter file.");
			init_flag = 0;
		}
		
		paramval = param.getParam("Use SPX");
		if (paramval != null)
			SPX_flag = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"Use SPX\" not defined on parameter file.");
			SPX_flag = 1;
		}
		
		paramval = param.getParam("SPX neighborhood parameter");
		if (paramval != null)
			Pnp = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"SPX neighborhood parameter\" not defined on parameter file.");
			Pnp = 10;
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
		Market mkt = Market.getInstance();


		for (int i = 0; i < size; i++)
		{
			Genome n = new Genome();

			// conditions for special initialization
			// 1- index is within the number of assets (each asset has a "1.0" individual)
			// 2- asset is valid for this experiment (badnees) 3- init_flag is on (off by default)
			if (i < mkt.assets.size() && mkt.assets.get(i).badness == false && init_flag == 1)
			{
				n.init_empty();
				n.weight[i] = 1;
				n.index[i] = true;
			}
			else
			{
				n.init();
			}			
			
			n.eval(T);
			individual.add(n);
		}
		
		
		// +1 is N generation + the initial population;
		max_fitness = new double[ngens+1];
		avg_fitness = new double[ngens+1];
		terminals = new double[ngens+1];
		bigterminals = new double[ngens+1];
		
		updateStatus();
	}
	
	/**
	 * Runs one generation loop
	 *
	 * In EMDE, there are no generations per se (steady-state). In Noman's DE, there are 
	 * generations, and each individual generates one offspring. I'll go with the second
	 * for now.
	 *
	 */
	public void runGeneration()
	{
		currgen++;
		Genome tmp;
		
		// Step 1 - local crossover on the best individual		
		tmp = individual.remove(0);
		individual.add(SPX(tmp,Pnp));
		
		// Step 2 - for each individual, generate a possible offspring
		for (int i = 0; i < size; i++)
		{
			genNewIndividual(i);
		}
		
		Collections.sort(individual);
		
		updateStatus();
	}
	
	public void genNewIndividual(int parent)
	{
		Genome child;
		Genome tmp;
		Genome tmp2;
		
		RNG die = RNG.getInstance();
		a1 = die.nextInt(individual.size());
		a2 = die.nextInt(individual.size());
		a3 = die.nextInt(individual.size());
		a4 = parent;
		
		/* child = a1 + Xover(a2 - a3) */
		
		child = individual.get(a1).copy();
		tmp = individual.get(a2);
		tmp2 = individual.get(a3);
		
		// FIXME: cludge to prevent negative weight values.
		// this messes with the relative weights :-/
		double baseline = 0;
		
		for (int i = 0; i < child.index.length; i++)
		{
			child.weight[i] += DE_K*(tmp.weight[i] - tmp2.weight[i]);
			child.index[i] = child.index[i] || tmp.index[i] || tmp2.index[i];
			if (child.weight[i] < baseline)
				baseline = child.weight[i];	
		}
		
		// FIXME: cludge to prevent negative weight values.
		// this messes with the relative weigths
		if (baseline < 0)
			for (int i = 0; i < child.index.length; i++)
				child.weight[i] -= baseline;
		
		// exchange with a4
		for (int i = 0; i < child.index.length; i++)
			if (die.nextDouble() < DE_C)
			{
				child.weight[i] = individual.get(a4).weight[i];
				child.index[i] = individual.get(a4).index[i];
			}	
		
		child.eval(T);
		
		if (child.fitness > individual.get(a4).fitness)
		{
			individual.remove(a4);
			individual.add(child);
		}
		
	}
	
	/*
	 * SPX local crossover optimization operator
	 * by Noman (Noman and IBA, 2005)
	 * "Accelerating Differential Evolution using an Adaptive Local Search"
	 * 
	 * I is the Genome to be optimized, np is the number of parents for the 
	 * crossover.
	 */
	public Genome SPX(Genome I, int np)
	{
		RNG dice = RNG.getInstance();
		
		Genome n; // new genome
		Genome f; // final genome
		
		f = I.copy();
		
		Genome s[] = new Genome[np]; // parent vector
		s[0] = I;
		for (int i = 1; i < np; i++)
		{
			int k = dice.nextInt(individual.size());
			s[i] = individual.get(k);
		}
		
		boolean done = false;
		
		while (!done) //running the crossover
		{

			// generating parent's center of mass
			double[] O = new double[s[0].weight.length];			
			for (int i = 0; i < O.length; i++)
				for (int j = 0; j < np; j++)
				{
					O[i] += s[j].weight[i]/np;
				}

			// generating random rates (r[np] is not used)
			double[] r = new double[np];
			for (int i = 0; i < np; i++)
			{
				r[i] = Math.pow(dice.nextDouble(),1/(i+2.0)); //+2 because the index begins at 0.
			}
			
			// generating the offspring;
			double [] Yn, Yo, Cn, Co;
			
			// initialization
			Yn = new double[O.length];
			Cn = new double[O.length];
			for (int i=0; i<Yn.length; i++)
			{
				Yn[i] = O[i] + Psigma * (s[0].weight[i] - O[i]);
				Cn[i] = 0;
			}
			
			// generating yi Ci for i = 1 to np
			for (int j = 1; j < np; j++)
			{
				Yo = Yn.clone();
				Co = Cn.clone();
				
				for (int i=0; i<Yn.length; i++)
				{
					Yn[i] = O[i] + Psigma * (s[j].weight[i] - O[i]);
					Cn[i] = r[j-1]*(Yo[i] - Yn[i] + Co[i]);
				}
				
			}
			
			// Making the resulting child
			n = new Genome();
			n.init_empty();
			double offset = 0;
			
			for (int i = 0; i < n.weight.length; i++)
			{
				n.weight[i] = Yn[i] + Cn[i];
				if (n.weight[i] < offset);
					offset = n.weight[i];
			}
			
			// TODO: Fixing possible negative weights - I need to move this into 
			// the Genome itself later - or maybe into the eval routine?
			if (offset < 0)
			{
				for (int i = 0; i < n.weight.length; i++)
					n.weight[i] += offset;
			}
			
			n.eval(T);
		
			if (n.fitness > f.fitness)
			{
				f = n.copy();
			}
			else
			{
				done = true;
			}
				
		}
				
		return (f);
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
		
		avg_fitness[currgen] = 0;
		terminals[currgen] = 0;
		bigterminals[currgen] = 0;

		double bigfit = 0;
		
		for (int i = 0; i < individual.size(); i++)
		{
			avg_fitness[currgen] += individual.get(i).Sharpe;			
			if (individual.get(i).Sharpe > bigfit);
			bigfit = individual.get(i).Sharpe;
			terminals[currgen] += individual.get(i).countAsset(0.0);
			bigterminals[currgen] += individual.get(i).countAsset(tresh);
		}
		
		max_fitness[currgen] = bigfit;
		avg_fitness[currgen] /= size;
		terminals[currgen] /= size;
		bigterminals[currgen] /= size;
	
	}
	
		
	
	
	
}

