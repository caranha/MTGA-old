package memetic;

import data.Market;
import engine.*;

import java.util.*;




public class MemePopulation {

	/* Evolutionary parameters: */
	public int size; // size of the population
	public int ngens; // total number of generations
	public int currgen; // current generation
	
	public int mutcount;
	public int memecount;
	public int crosscount;

	/* Crossover parameters */
	int tournamentK; // size of tournament
	int elite; // size of elite
	int immigrant; // number of new random individuals
	double mutrate; // chance that a mutation will occur
	double xoverrate; // chance that the xover will occur
	double meme_chance; // chance of using memetic eval
	
	/* GP Parameters */
	int maxdepth; // Maximum depth for the tree. should be 2xlog(number of assets)
	double treedensity; //chance of a tree being full
	
	/* Containers */
	public ArrayList<MemeNode> individual;
	MemeNode parent; 
	public Trainer T;
		
	/* Progress data */
	public double[] max_fitness;
	public double[] avg_fitness;
	public double[] terminals; // average total number of terminals
	public double[] bigterminals; // average total number of sig. terminals
	public double[] nodes; // average total number of nodes
	public double[] introns; // average total number of introns
	public double[] diversity;

	/**
	 * Initialize and load parameters.
	 * Parameter comp is a node from a previous 
	 * scenario, which is used for distance calculations.
	 */
	public MemePopulation(MemeNode comp)
	{
		individual = new ArrayList<MemeNode>();
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

		paramval = param.getParam("Tree Depth");
		if (paramval != null)
			maxdepth = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"Tree Depth\" not defined on parameter file.");
			maxdepth = 7;
		}
		
		paramval = param.getParam("Tree density");
		if (paramval != null)
			treedensity = Double.parseDouble(paramval);
		else
		{
			System.err.println("\"Tree Density\" not defined on parameter file.");
			treedensity = 0.9;
		}
						
		paramval = param.getParam("meme chance");
		if (paramval != null)
			meme_chance = Double.valueOf(paramval);
		else
		{
			System.err.println("\"meme chance\" not defined on parameter file.");
			meme_chance = 0.8;
		}
	
	}

	/**
	 * Initialize the new population and the local
	 * variables. Startd is the target date for the 
	 * @param startd
	 */
	public void initPopulation(Date startd)
	{
		mutcount = 0;
		memecount = 0;
		crosscount = 0;
		
		T = new Trainer(startd);
		currgen = 0;
		for (int i = 0; i < size; i++)
		{
			MemeNode n = new MemeNode();
			n.generateTree(maxdepth, treedensity);
			individual.add(n);
		}
		
		max_fitness = new double[ngens];
		avg_fitness = new double[ngens];
		terminals = new double[ngens];
		bigterminals = new double[ngens];
		nodes = new double[ngens];
		introns = new double[ngens];
		diversity = new double[ngens];
	}
	
	/**
	 * Runs one generation loop
	 *
	 */
	public void runGeneration()
	{
		eval();
		breed();
		currgen++;
	}
	
	/**
	 * update the values of the maxfitness/avg fitness/etc
	 * public arrays;
	 */
	public void updateStatus()
	{
		Market mkt = Market.getInstance();
		Parameter p = Parameter.getInstance();
		String param = p.getParam("asset treshold");
		double tresh = Double.valueOf(param);
		int[] n = new int[2];
		double[][] list = new double[size][mkt.assets.size()];
		double[] centroid = new double[mkt.assets.size()];		
		
		Collections.sort(individual);
		
		avg_fitness[currgen-1] = 0;
		terminals[currgen-1] = 0;
		bigterminals[currgen-1] = 0;
		nodes[currgen-1] = 0;
		introns[currgen-1] = 0;
		diversity[currgen-1] = 0;
		
		for (int i = 0; i < size; i++)
		{
			Portfolio port = individual.get(i).generatePortfolio();				
			for (int j = 0; j < centroid.length; j++)
			{
				list[i][j] = port.getWeightByIndex(j);
				centroid[j] += list[i][j]/size;
			}
			
			avg_fitness[currgen-1] += individual.get(i).fitness;
			terminals[currgen-1] += individual.get(i).countAsset(0.0);
			bigterminals[currgen-1] += individual.get(i).countAsset(tresh);
			n = individual.get(i).countNodes();
			nodes[currgen-1] += n[0];
			introns[currgen-1] += n[1];
		}
		
		diversity[currgen-1] = 0;
		for(int i = 0; i < size; i++)
			for(int j = 0; j < centroid.length; j++)
			{
				diversity[currgen-1] += Math.pow(centroid[j] - list[i][j],2);
			}		
		
		max_fitness[currgen-1] = individual.get(0).fitness;
		avg_fitness[currgen-1] /= size;
		terminals[currgen-1] /= size;
		bigterminals[currgen-1] /= size;
		nodes[currgen-1] /= size;
		introns[currgen-1] /= size;
	
	}
	
	/**
	 * Calculates the fitness value for each individual
	 * in the population.
	 */
	public void eval()
	{
		RNG d = RNG.getInstance();
	
		
		for (int i = 0; i < size; i++)
		{
	
			if (d.nextDouble() < meme_chance)
			{
				individual.get(i).memetize(T);
				memecount++;
			}
			else
				individual.get(i).eval(T);
		}
		Collections.sort(individual);
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
		RNG d = RNG.getInstance();
		ArrayList<MemeNode> nextGen = new ArrayList<MemeNode>();
		MemeNode p1,p2;
		
		// elite: (few copied individuals)
		for (int i = 0; i < elite; i++)
		{
			nextGen.add(individual.get(i).copy());
		}
		// immigrant: (usually 0)
		for (int i = 0; i < immigrant; i++)
		{
			MemeNode n = new MemeNode();
			n.generateTree(maxdepth, treedensity);
			nextGen.add(n);
		}

		// crossover:
		for (int i = 0; i < size - (immigrant + elite); i+=2)
		{
			// selection - the selection function should 
			// return copies already.
			p1 = Tournament();
			p2 = Tournament();
			
			// rolls for xover
			if (d.nextDouble() < xoverrate)
			{
				p1.crossoverSimple(p2);
				crosscount++;
			}				
			
			// rolls for mutation
			if (d.nextDouble() < mutrate)
			{
				p1.mutation();
				mutcount++;
			}
			if (d.nextDouble() < mutrate)
			{
				p2.mutation();
				mutcount++;
			}
			
			nextGen.add(p1);
			nextGen.add(p2);
			
		}
		
		individual = nextGen;		
	}
	
	/**
	 * Select one parent from the population by using
	 * fitness-proportional tournament selection 
	 * (eat candidate has a chance proportional to his 
	 * fitness of being chosen).
	 * 
	 * The function copy the chosen candidate and send
	 * him back.
	 * 
	 * @return
	 */
	public MemeNode Tournament()
	{
		RNG d = RNG.getInstance();
		MemeNode[] list = new MemeNode[tournamentK];
		double[] rank = new double[tournamentK];
		double sum = 0.0;
		double ticket = 0.0;
		double min = 0.0;
		
		/* Selects individuals and removes negative fitness */
		for (int i = 0; i < tournamentK; i++)
		{
			list[i] = individual.get(d.nextInt(size));
			if (list[i].fitness < min)
				min = list[i].fitness;
		}
		
		/* I'm not sure if this is the best way to 
		 * make the proportion between the fitnesses. 
		 * Some sort of scaling factor should be put here 
		 * to avoit high fitnesses from superdominating.
		 * 
		 * But maybe the tournament proccess already guarantees this?
		 */
		for (int i = 0; i < tournamentK; i++)
		{
			sum += list[i].fitness - min;
			rank[i] = sum;
		}
		
		ticket = d.nextDouble()*sum;
		for (int i = 0; i < tournamentK; i++)
		{
			if ((ticket) <= rank[i])
				return list[i].copy();
		}

		// should never get here
		System.err.println(ticket + " + " + sum);
		System.err.println("Warning: MemeTournament - reached unreachable line");
		return list[0].copy(); 
	}
	
}
