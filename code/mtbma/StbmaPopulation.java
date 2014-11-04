package mtbma;

import java.util.*;

import data.Market;

import memetic.MemeNode;
import engine.Portfolio;
import engine.Trainer;
import engine.Parameter;
import engine.RNG;

/**
 * Implements the STBMA, where individuals do not migrate
 * 
 * Adaptations: 
 *  -- GAKM is replaced with two generations of tournament selection and steady station breeding
 *  -- Local search is performed when migrating
 *  -- Because Intron pruning is a binary quality, the Terrain is 4x10 (where 10 is the range
 *  	of tree depth)
 * 
 * @author caranha
 *
 */

public class StbmaPopulation {

	/* Evolutionary parameters: */
	public int size; // size of the population
	public int ngens; // total number of generations
	public int currgen; // current generation

	public int mutcount;
	public int memecount;
	public int crosscount;
	
	
	/* Crossover parameters */
	double mutrate; // chance that a mutation will occur
	double meme_chance; // chance of using memetic eval
	
	/* GP Parameters */
	double treedensity; //chance of a tree being full
	int maxdepth;
	
	/* FIXME: Right now, TBMA parameters have to be set by hand -
	 * Need to find a way to put this all in the parameter file.
	 */
	
	/* mTBMA Parameters */
	int max_X = 6;
	int max_Y = 9;
	int adaptive;
	
	/* Terrain Parameter Values */
	String yparameter[] = {"10","8","6","4","3","5","7","9","11"};
	String yparamname = "anne pop";
	String xparameter[] = {"0.3","0.5","0.7","0.6","0.4","0.2"}; 
	String xparamname = "anne accel";
	
	
	
	/* Containers */
	public ArrayList<MemeNode> individual; // Global Individual List (Main list)
	public MemeNode terrain[][]; 
	Trainer T; // This object carries out the fitness evaluations;
		
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
	 */
	public StbmaPopulation()
	{
		individual = new ArrayList<MemeNode>();
		terrain = new MemeNode[max_X][max_Y];
				
		// reading parameters
		
		Parameter param = Parameter.getInstance();		
		String paramval;

		size = max_X*max_Y; // Population size is fixed for the size of the Terrain in STBMA

		paramval = param.getParam("generation number");
		if (paramval != null)
			ngens = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"generation number\" not defined on parameter file.");
			ngens = 10;
		}
				
		paramval = param.getParam("mutation rate");
		if (paramval != null)
			mutrate = Double.valueOf(paramval);
		else
		{
			System.err.println("\"mutation rate\" not defined on parameter file.");
			mutrate = 0.01;
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
		
		paramval = param.getParam("Tree Depth");
		if (paramval != null)
			maxdepth = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"Tree Depth\" not defined on parameter file.");
			maxdepth = 7;
		}
		
		paramval = param.getParam("Adaptive");
		if (paramval != null)
			adaptive = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"Adaptive\" not defined on parameter file.");
			adaptive = 0;
		}
	
	}
	
	
	/**
	 * Initialize the new population and the local
	 * variables. Startd is the target date for the 
	 * simulation.
	 * 
	 * MTBMA:
	 * - Build XxY terrain - One individual per cell
	 * 
	 * @param startd
	 */
	public void initPopulation(Date startd)
	{
		T = new Trainer(startd);
		
		mutcount = 0;
		memecount = 0;
		crosscount = 0;
		
		currgen = 0;
		
		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				MemeNode n = new MemeNode();	
				n.generateTree(maxdepth, treedensity);
				n.eval(T);
				terrain[i][j] = n;				
				individual.add(n); // adding to the global list.
		}
		
				
		max_fitness = new double[ngens];
		avg_fitness = new double[ngens];
		terminals = new double[ngens];
		bigterminals = new double[ngens];
		nodes = new double[ngens];
		introns = new double[ngens];
		diversity = new double[ngens];
	}
	
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
		//TODO: Make sure that sorting global list does not mess up local lists!
		
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
	
	
	public void runGeneration()
	{	
		processSquares();
		currgen++;
	}
	
	/**
	 * For each square: 
	 * Find the best neighbor
	 *  - Crossover with that neighbor
	 *  - Mutate, and Memetize (with probability)	
	 *  - If kid is better than parent, substitute parent.
	 *
	 */
	public void processSquares()
	{

		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				MemeNode original = terrain[i][j].copy();
				MemeNode cnbreed = null;
				
				cnbreed = terrain [i][(j+1+max_Y)%max_Y];
					
				if (terrain [i][(j-1+max_Y)%max_Y].fitness > cnbreed.fitness)
					cnbreed = terrain [i][(j-1+max_Y)%max_Y];

				if (terrain [(i+1+max_X)%max_X][j].fitness > cnbreed.fitness)
					cnbreed = terrain [(i+1+max_X)%max_X][j];
					
				if (terrain [(i-1+max_X)%max_X][j].fitness > cnbreed.fitness)
					cnbreed = terrain [(i-1+max_X)%max_X][j];
					
				crosscount++;
				cnbreed = cnbreed.copy();
				original.crossoverSimple(cnbreed, maxdepth);
								
				RNG dice = RNG.getInstance();
				if (dice.nextDouble() < mutrate)
				{
					mutcount++;
					memecount++;
					original.mutation(maxdepth);
				
					/* Setting meemtic Parameters */
					if (adaptive == 1)
					{
						Parameter P = Parameter.getInstance();
						P.setParam(yparamname, yparameter[j]);
						P.setParam(xparamname, xparameter[i]);
					}					
					original.memetize(T);
				}
					
				original.eval(T);
				if (original.fitness > terrain[i][j].fitness)
				{
					individual.remove(terrain[i][j]);
					individual.add(original);
					terrain[i][j] = original;
				}					
				
			}	// End double for
	}

	
	
	
	
	
}
