package mtbma;

import java.util.*;

import data.Market;

import memetic.MemeNode;
import engine.Portfolio;
import engine.Trainer;
import engine.Parameter;
import engine.RNG;

/**
 * Implements the "TERRAIN" and "CITIES" of mTBMA as described in the GECCO 09 article of 
 * Carlos Azevedo and Scott Gordon (747--754), adapted to MTGA
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

public class MtbmaPopulation {

	/* Evolutionary parameters: */
	public int size; // size of the population
	public int ngens; // total number of generations
	public int currgen; // current generation

	public int mutcount;
	public int memecount;
	public int crosscount;
	
	
	/* Crossover parameters */
	int tournamentK; // size of tournament (Used in city evolution)
	double mutrate; // chance that a mutation will occur
	double xoverrate; // chance that the xover will occur
	double meme_chance; // chance of using memetic eval
	
	/* GP Parameters */
	double treedensity; //chance of a tree being full
	int maxdepth; // maximum depth of tree
	
	/* mTBMA Parameters */
	int citySize; // Maximum number of individuals in the same cell;
	double mig_chance; // Chance of an individual migrating even if in a good city;
	
	/* Terrain parametres */
	// FIXME: Right now, this has to be done by hand
	public int max_X = 6;
	public int max_Y = 9;
	int adaptive;
	
	/* Terrain Parameter Values */
	String yparameter[] = {"10","8","6","4","3","5","7","9","11"};
	String yparamname = "anne pop";
	String xparameter[] = {"0.3","0.5","0.7","0.6","0.4","0.2"}; 
	String xparamname = "anne accel";
	
	
	/* Containers */
	public ArrayList<MemeNode> individual; // Global Individual List (Main list)
	public Object terrain[][]; // City lists;	
		// declarin terrain as "Object" because java will not compile if I declare it as 
		// an "Arraylist" - how nasty is this???
	Trainer T; // This object carries out the fitness evaluations;
		
	/* Progress data */
	public double[] max_fitness;
	public double[] avg_fitness;
	public double[] terminals; // average total number of terminals
	public double[] bigterminals; // average total number of sig. terminals
	public double[] nodes; // average total number of nodes
	public double[] introns; // average total number of introns
	public double[] diversity; // centroid+inertia diversity measure;
	public double[][] memevalue; // average meme values per generation;
	public int[] citytotal;
	public int[][][] citypop;
	public double[][][] cityfitness;

	/**
	 * Initialize and load parameters.
	 */
	public MtbmaPopulation()
	{
		individual = new ArrayList<MemeNode>();
		terrain = new Object[max_X][max_Y];
		
		
		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				terrain[i][j] = new ArrayList<MemeNode>();
			}
		
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

		paramval = param.getParam("city size");
		if (paramval != null)
			citySize = Integer.valueOf(paramval);
		else
		{
			System.err.println("\"city size\" not defined on parameter file.");
			citySize = size/2;
		}
		
		paramval = param.getParam("migration chance");
		if (paramval != null)
			mig_chance = Double.valueOf(paramval);
		else
		{
			System.err.println("\"migration chance\" not defined on parameter file.");
			mig_chance = 0.2;
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
	 * - Build XxY terrain - Distribute population randomly in the terrain
	 * - The best individual of each cell is declared the "Mayor" (this can be done later)
	 * 
	 * @param startd
	 */
	public void initPopulation(Date startd)
	{
		T = new Trainer(startd);
		RNG dice = RNG.getInstance();
		
		mutcount = 0;
		memecount = 0;
		crosscount = 0;
		
		currgen = 0;
		for (int i = 0; i < size; i++)
		{
			MemeNode n = new MemeNode();
			int px = dice.nextInt(max_X);
			int py = dice.nextInt(max_Y);
			
			n.generateTree(maxdepth, treedensity);

			n.eval(T); 
			ArrayList<MemeNode> m = (ArrayList<MemeNode>)terrain[px][py];

			individual.add(n); // adding to the global list.
			m.add(n); // adding to the city list
		}
		
				
		max_fitness = new double[ngens];
		avg_fitness = new double[ngens];
		terminals = new double[ngens];
		bigterminals = new double[ngens];
		nodes = new double[ngens];
		introns = new double[ngens];
		diversity = new double[ngens];
		memevalue = new double[ngens][2];
		citytotal = new int[ngens];
		citypop = new int[ngens][max_X][max_Y];
		cityfitness = new double[ngens][max_X][max_Y];
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
		memevalue[currgen-1][0] = 0;
		memevalue[currgen-1][1] = 0;
		citytotal[currgen-1] = 0;
		
		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				ArrayList<MemeNode> m = (ArrayList<MemeNode>)terrain[i][j];
				memevalue[currgen-1][0] += m.size()*Double.parseDouble(xparameter[i]);
				memevalue[currgen-1][1] += m.size()*Double.parseDouble(yparameter[j]);
				citypop[currgen-1][i][j] = m.size();
				cityfitness[currgen-1][i][j] = 0;
				for(int k = 0; k < m.size(); k++)
				{
					cityfitness[currgen-1][i][j]+= m.get(k).fitness / m.size();
				}
				if (m.size() > 0) citytotal[currgen-1]++;
				
			}
		
		for (int i = 0; i < size; i++)
		{
			// calculating centroid;
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
		memevalue[currgen-1][0] /= size;
		memevalue[currgen-1][1] /= size;
	
	}
	
	
	public void runGeneration()
	{	
		processSquares();
		processMayors();
		processMigrations();
		clearmigflag();
		currgen++;
		
	}

	public void clearmigflag()
	{
		for (int i = 0; i < individual.size(); i++)
			individual.get(i).migflag = false;
	}
	
	/*
	 * helper function to make one individual do a random walk. If the 
	 * individual tries to walk into an invalid square (full city), it fails.
	 * 
	 * returns true if the random walk succeeded, false if it fails.
	 */
	boolean randomWalk(MemeNode n, int x, int y)
	{
		RNG dice = RNG.getInstance();
		int dx,dy;

		
		// calculating movement;
		dx = dice.nextInt(3) - 1;
		if (dx == 0)
			dy = (dice.nextInt(2)==0?1:-1);
		else 
			dy = (dice.nextInt(3) - 1);

		dx = (dx + x + max_X) % max_X;
		dy = (dy + y + max_Y) % max_Y;
		
		
		ArrayList<MemeNode> dest = (ArrayList) terrain[dx][dy];
		if (dest.size() >= citySize)
			return false;
		
		ArrayList<MemeNode> orig = (ArrayList) terrain[x][y];

		// Testing if ArrayList.remove is working correctly
		if(orig.remove(n)==false)
			System.err.println("ERROR - Randomwalk: Removing element from city - city says no such element exists.");				
		dest.add(n);
			
		
		Collections.sort(dest);
		
		return true;
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
	public MemeNode Tournament(ArrayList<MemeNode> city)
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
			int draw = d.nextInt(city.size());
			list[i] = city.get(draw);
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
			{
				return list[i];
			}	
		}

		// should never get here
		System.err.println(ticket + " + " + sum);
		System.err.println("Warning: MemeTournament - reached unreachable line");
		return list[0];
	}
	
	
	/**
	 * For each square: 
	 * -- If the city is isolated, mutation + local search + random walk
	 * -- if the city is not isolated, do "Pop - 1" iterations of 
	 *    tournament + crossover + selection
	 */
	public void processSquares()
	{
		RNG dice = RNG.getInstance();
		ArrayList<MemeNode> city;
		
		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				city = (ArrayList) terrain[i][j];
				if (city.size() == 1 && city.get(0).migflag == false)
				{
					city.get(0).migflag = true;
					// Only one person in the city -- mutate and walk
					if (dice.nextDouble() < mutrate )
					{
						mutcount++;
						memecount++;
						city.get(0).mutation(maxdepth);

						/* Setting meemtic Parameters */
						if (adaptive == 1)
						{
							Parameter P = Parameter.getInstance();
							P.setParam(yparamname, yparameter[j]);
							P.setParam(xparamname, xparameter[i]);
						}					

						city.get(0).memetize(T);

						
						
					}
					randomWalk(city.get(0),i,j);
				}
				else
				{
					// More than one person in the city -- tournament crossover
					for (int k = 0; k < city.size()-1; k++)
					{
						MemeNode p1 = null, p2 = null;
						int i1, i2;
						p1 = Tournament(city);
						p2 = Tournament(city);
		
						i1 = city.indexOf(p1);
						i2 = city.indexOf(p2);
						
						p1 = p1.copy();
						p2 = p2.copy();
						
						p1.crossoverSimple(p2,maxdepth);
						p1.eval(T);
						crosscount++;
						//According to the paper, the offspring replaces the closest
						//parent. But it is hard (?) to calculate genetic distance with
						//MTGA, so I'll just replace the worst parent.
						
						//A problem with this is that since tournament selection chooses 
						//best parents to breed, lower individuals who are not selected
						//by tournament may never be replaced (until they migrate to a 
						//"bad" city.
						
						
						if (dice.nextDouble() < meme_chance)
						{
							p1.memetize(T);
							memecount++;
						}
						
						if (dice.nextDouble() < mutrate)
						{
							p1.mutation(maxdepth);
							mutcount++;
						}

						if ((i1 == i2))
						{
							// Special case when both parents are the mayor, 
							// Will only replace if the new individual is better than
							// the mayor. 
							if (p1.fitness > city.get(i1).fitness)
							{
								MemeNode oldguy = city.get(i1);
								city.remove(oldguy);
								individual.remove(oldguy);
								city.add(p1);
								individual.add(p1);
							}
						}
						else  // Parents are different.
						{
							if (city.get(i1).fitness > city.get(i2).fitness)
							{
								MemeNode oldguy = city.get(i2);
								city.remove(oldguy);
								individual.remove(oldguy);

								city.add(p1);
								individual.add(p1);
							}
							else
							{
								MemeNode oldguy = city.get(i1);

								city.remove(oldguy);
								individual.remove(oldguy);
							
								city.add(p1);
								individual.add(p1);
							}
						}
					}
					Collections.sort(city);
				}
			}
		
	}
	
	/*
	 * If "processSquares" was implemented correctly, all cities should be 
	 * sorted by now, so I just need to thank the first element of each city
	 * to have the mayors.
	 */
	public void processMayors()
	{
		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				ArrayList<MemeNode> c = (ArrayList) terrain[i][j];
				if (c.size() > 0)
				{
					MemeNode mayor = c.get(0).copy();
					
					// checking 4 neighbors.
					ArrayList<MemeNode> cn;
					MemeNode cnbreed = null;
					
					cn = (ArrayList) terrain [i][(j+1+max_Y)%max_Y];
					if (cn.size() > 0)
						cnbreed = cn.get(0).copy();
					
					cn = (ArrayList) terrain [i][(j-1+max_Y)%max_Y];
					if ((cn.size() > 0)&&
						((cnbreed == null) || (cn.get(0).fitness > cnbreed.fitness)))
						cnbreed = cn.get(0).copy();
					
					cn = (ArrayList) terrain [(i+1+max_X)%max_X][j];
					if ((cn.size() > 0)&&
						((cnbreed == null) || (cn.get(0).fitness > cnbreed.fitness)))
						cnbreed = cn.get(0).copy();
					
					cn = (ArrayList) terrain [(i-1+max_X)%max_X][j];
					if ((cn.size() > 0)&&
						((cnbreed == null) || (cn.get(0).fitness > cnbreed.fitness)))
						cnbreed = cn.get(0).copy();
					
					// breeding.
					if (cnbreed != null)
					{
						crosscount++;
						mayor.crossoverSimple(cnbreed, maxdepth);
					
						RNG dice = RNG.getInstance();
						if (dice.nextDouble() < mutrate)
						{
							mutcount++;
							memecount++;
							mayor.mutation(maxdepth);
							
							/* Setting meemtic Parameters */
							if (adaptive == 1)
							{
								Parameter P = Parameter.getInstance();
								P.setParam(yparamname, yparameter[j]);
								P.setParam(xparamname, xparameter[i]);
							}					
							
							mayor.memetize(T);
						}
					
						mayor.eval(T);
						if (mayor.fitness > c.get(0).fitness)
						{
							MemeNode oldguy = c.get(0);
							c.remove(oldguy);
							individual.remove(oldguy);
						
							c.add(0, mayor);
							individual.add(mayor);
						}
					}
					
				}
				
			}	
	}

	
	/* Processing rules 1 and 2 for post-crossover migration */
	public void processMigrations()
	{

		for (int i = 0; i < max_X; i++)
			for (int j = 0; j < max_Y; j++)
			{
				int k = 0; // citizen count
				ArrayList<MemeNode> c = (ArrayList) terrain[i][j];
				while (k < c.size())
				{
					
					MemeNode migrant = c.get(k);
					if (individual.indexOf(migrant) == -1)
						System.out.println(c.size()+ " " + k + migrant.dumptree(false));
					
					if (migrant.migflag == true)
					{
						k++;
					}
					else
					{
						// first time we're evaluating this individual.
						migrant.migflag = true;
						ArrayList<MemeNode> neighborcity = null;
						double target = migrant.fitness;
						int dx = 0, dy = 0;
						
						// Rule 1 - check for a better looking neighbor
						for (int ii = -1; ii < 2; ii++)
							for (int jj = -1; jj < 2; jj++)
								if ((ii == 0 && jj == 0) == false)
								{
									ArrayList<MemeNode> nc = (ArrayList)terrain[(i + ii + max_X) % max_X][(j + jj + max_Y)%max_Y];
									if ((nc.size() > 0) &&
											(nc.get(0).fitness > target) &&
											(nc.size() < citySize))
									{
										neighborcity = nc;
										target = nc.get(0).fitness;
										dx = (i + ii + max_X) % max_X;
										dy = (j + jj + max_Y) % max_Y;
									}										
								}							
						//there is a better neighbor with space, migrate there
						if (neighborcity != null)
						{
							c.remove(migrant);
							
							neighborcity.add(migrant);
							// don't add to k because the city just got smaller
						}
						else
						{
							// random chance of migration away from best city
							RNG dice = RNG.getInstance();
							if (dice.nextDouble() < mig_chance)
							{
								if (randomWalk(migrant, i,j) == false)
									k++; // random Walk failed
							}
							else
							{
								// nothing happens, next individual
								k++;
							}
						}					
					
					}// if migdone == true
				}
			}
		
	}
	
	
	
	
}
