package memetic;

import data.*;
import engine.*;


public class MemeNode implements Comparable<MemeNode> {

	/* node internal variables */
	Boolean isTerminal;	
	public Long uniqueID;
	int mydepth; // depth from root.
	int treedepth; // depth of this subtree.
	
	/* Node Contents */
	public MemeNode leftChild;
	public MemeNode rightChild;
	Double weight; // weight of the children, if node
	Integer assetIndex; // index of the asset, if terminal
	
	/* Fitness Values */	
	public Double fitness = 0.0;
	public Double Ereturn = 0.0; // estimated return
	public Double Risk    = 0.0; // risk measure
	public Double Sharpe  = 0.0; // sharpe ratio
	
	public boolean migflag = false; // used to guarantee that one individual does not mig twice.
	
	/**
	 * Empty constructor - generates an empty node.
	 *
	 */
	public MemeNode()
	{		
		IDGenerator i = IDGenerator.getInstance();
		uniqueID = i.newID();
		mydepth = -1; //this node is uninitiated;
				
	}

	/**
	 * Copy tree function - recursively copies a tree and send 
	 * it back
	 */
	public MemeNode copy()
	{
		IDGenerator i = IDGenerator.getInstance();
		if (isTerminal)
		{
			MemeNode newn = new MemeNode();
			newn.mydepth = mydepth;
			newn.treedepth = treedepth;
			newn.isTerminal = true;
			newn.assetIndex = assetIndex;
			newn.uniqueID = i.newID();
			newn.fitness = fitness;
			return newn;
		}
		else
		{
			MemeNode newn = new MemeNode();
			newn.mydepth = mydepth;
			newn.treedepth = treedepth;
			newn.isTerminal = false;
			newn.weight = weight;
			newn.leftChild = leftChild.copy();
			newn.rightChild = rightChild.copy();
			newn.uniqueID = i.newID();
			newn.fitness = fitness;
			return newn;
		}
	}
	
	/**
	 * Generate a random tree starting from this node. 
	 * Recursive Function. A tree with only the root 
	 * has depth = 0. The correct depth of the node is 
	 * supposed to be already set when entering this 
	 * function (-1 if empty tree).
	 * 
	 * prob is the probability that each node will 
	 * not be a terminal node. If prob = 1, then the 
	 * tree is a full tree.
	 * 
	 */
	public void generateTree(int maxdepth, double prob)
	{
		RNG dice = RNG.getInstance();
		if (mydepth == -1)
			mydepth = 0;
			
		if ((maxdepth <= mydepth+1)||
				(dice.nextDouble() > prob))
		{
			// terminal case
			Market mkt = Market.getInstance();
			treedepth = 1;
			isTerminal = true;
			
			/* Avoiding bad assets */
			int oops = 0;
			do {				
				assetIndex = dice.nextInt(mkt.assets.size());			
				oops++;
				if (oops > 500)
				{
					System.err.println("Infinite loop trying to pick non-bad asset");
					System.exit(1);
				}
			} while(mkt.assets.get(assetIndex).badness == true);

		}
		else
		{
			// node case
			weight = dice.nextDouble();
			isTerminal = false;
			leftChild = new MemeNode();
			leftChild.mydepth = mydepth + 1;
			leftChild.generateTree(maxdepth,prob);
			
			rightChild = new MemeNode();
			rightChild.mydepth = mydepth + 1;
			rightChild.generateTree(maxdepth,prob);	
			
			treedepth = (leftChild.treedepth > rightChild.treedepth ?
					leftChild.treedepth:rightChild.treedepth);
			treedepth+=1;
		}
	}
	
	/**
	 * Crossover this tree with another tree at random
	 * points. Depth of the cut is decided with Normal
	 * probability for both trees, and the actual node 
	 * is chosen by visiting the tree.
	 * 
	 * If each resulting tree is deeper than maximum 
	 * depth, the tree is pruned.
	 */
	public void crossoverSimple(MemeNode tree)
	{
		crossoverSimple(tree, -1);
	}

	public void crossoverSimple(MemeNode tree, int maxdepth)
	{
		boolean myleft, hisleft; /* if we're switching the left nodes */
		MemeNode mypnode, hispnode; /* the parent nodes */
		MemeNode mycnode, hiscnode; /* the nodes being xovered */
		int mycut, hiscut, i; /* cut depths */
		
		RNG die = RNG.getInstance();
		
		// do not crossover unitary trees
		if (this.treedepth <= 1 || tree.treedepth <= 1)
			return;

		// ugly code to avoid special first case
		mycnode = this;
		hiscnode = tree;

		mycut = 1 + die.nextInt(this.treedepth-1);
		hiscut = 1 + die.nextInt(tree.treedepth-1);
		
		// choose cutpoints
		
		i = 0;
		do {
			mypnode = mycnode;
			myleft = die.nextBoolean();			
			mycnode = (myleft?mypnode.leftChild:mypnode.rightChild);			
			i++;
		} while ((i < mycut) && (mycnode.isTerminal==false));

		i = 0;
		do {
			hispnode = hiscnode;
			hisleft = die.nextBoolean();
			hiscnode = (hisleft?hispnode.leftChild:hispnode.rightChild);			
			i++;
		} while ((i < hiscut) && (hiscnode.isTerminal==false));
		
		
		// transplant subtrees.
		
		if (myleft) 
			mypnode.leftChild = hiscnode;
		else
			mypnode.rightChild = hiscnode;
		
		if (hisleft)
			hispnode.leftChild = mycnode;
		else
			hispnode.rightChild = mycnode;	
		
		tree.mydepth = 0;
		this.mydepth = 0;
		
		if (!(this.insanity_Check() && tree.insanity_Check()))
		{
			System.err.println("insane before pruning");
		}
		
		tree.prune(maxdepth);
		this.prune(maxdepth);

		
	}

	
	/**
	 * Mutation chooses a node at random in the tree, 
	 * and generates a new tree based on that node.
	 *
	 */
	public void mutation()
	{
		mutation(-1);
	}
	public void mutation(int depth)
	{
		RNG die = RNG.getInstance();
		int maxdepth;
		Parameter p = Parameter.getInstance();
		String param;
		
		if (depth == -1)
		{
			param = p.getParam("tree depth");
			if (param == null)
			{
				System.err.println("Error in Mutation: No tree depth parameter defined");
				System.exit(1);
			}
			maxdepth = Integer.parseInt(param);	
		}
		else maxdepth = depth;
		
		param = p.getParam("tree density");
		double dense = Double.parseDouble(param);
			
		if (treedepth < 1)
			treedepth = 1;
		
		int cutdepth = die.nextInt(treedepth); // may mutate at root
		MemeNode n = this;
		
		for (int i = 0; i < cutdepth; i++)
		{
			if (n.isTerminal)
				break;
			if (die.nextBoolean())
				n = n.leftChild;
			else 
				n = n.rightChild;
		}
		
		
		/* TODO: maybe I should delete the tree first? */
		n.generateTree(maxdepth, dense);
		prune(maxdepth);
	}
	
	/**
	 * This function recursively removes introns from the tree. 
	 * 
	 * Since it is too troublesome to change the local variable (this), this 
	 * function RETURNS the corrected tree - so that you must call if from outside the 
	 * individual and reassign the variable to the result of this function.
	 * 
	 * Yeah, this is quite clunky :-/
	 * TODO: make this function more elegant.
	 *
	 */	
	public MemeNode removeIntrons()
	{		
		if (isTerminal == false && weight == 0)
		{
			rightChild = rightChild.removeIntrons();
			return rightChild;
		}
		
		if (isTerminal == false && weight == 1)
		{
			leftChild = leftChild.removeIntrons();
			return leftChild;
		}
		
		if (isTerminal == false)
		{
			rightChild = rightChild.removeIntrons();
			leftChild = leftChild.removeIntrons();
		}
		
		// Either this node is a terminal, or its weight is not at the limit
		return (this);			
	}
	
	
	
	/**
	 * Walks through the trees, correcting the 
	 * mydepth values for the children. If mydepth
	 * reaches the maxdepth parameter value, prunes 
	 * the tree.
	 * 
	 * TODO: This function is inneficient, as 
	 * it runs through the whole tree. I need to 
	 * fix this object as to only visit the 
	 * nodes that were changed in a tree.
	 * 
	 */
	public void prune(int depth)
	{
		int maxdepth;
		if (depth == -1)
		{
			Parameter p = Parameter.getInstance();
			maxdepth = Integer.parseInt(p.getParam("tree depth"));
		}
		else
			maxdepth = depth;
		
		// If this node is a terminal, do nothing.
		if (isTerminal == true)
		{
			treedepth = 1;
			return;
			
		}
			
		if (isTerminal == false && mydepth+1 < maxdepth)
		{
			// Non terminal, and below depth limit.
			// Just call this function recursively.
			leftChild.mydepth = mydepth + 1;
			leftChild.prune(maxdepth);
			rightChild.mydepth = mydepth + 1;
			rightChild.prune(maxdepth);
			
			// fix sanity and tree depths.
			treedepth = (leftChild.treedepth > rightChild.treedepth ?
					leftChild.treedepth:rightChild.treedepth);
			treedepth+=1;
			
		}
		else
		{
			/* PRUNING HAPPENS HERE.
			 * 
			 * Pruning policy checks the highest weighted assed
			 * of this subtree and transforms it into a 
			 * terminal of that asset.
			 */
			
			
			Portfolio P = this.generatePortfolio();
			
			
			treedepth = 0;
			
			isTerminal = true;
			assetIndex = P.getMaxWeightIndex();
			
			/* TODO: maybe I should finalize these? */
			leftChild = null;
			rightChild = null;
			
			
		}
	}
	
	/**
	 * Runs recursively through the tree, 
	 * optimizing each non-terminal weight value.
	 */
	public void memetize(Trainer T)
	{
		// stop condition
		if (isTerminal)
		{
			Portfolio port = generatePortfolio();
			calculateLocalFitness(port, T);
			return;
		}
		
		// recursive
		leftChild.memetize(T);
		rightChild.memetize(T);

		// These guys are used to calculate the different portfolios
		Portfolio lport = leftChild.generatePortfolio();
		Portfolio rport = rightChild.generatePortfolio();

		// Getting all the memetic parameters;
		Parameter p = Parameter.getInstance();
		String param;
		
		int method = 1;
		param = p.getParam("meme method");
		if (param != null)
			method = Integer.parseInt(param);				

		int maxeval = 10;
		int eval_c = 0;
		param = p.getParam("meme maxeval");
		if (param != null)
			maxeval = Integer.parseInt(param);
		
		double meme_speed = 0.1;
		param = p.getParam("hill speed");
		if (param != null)
			meme_speed = Double.parseDouble(param);

		double meme_delta = 0.333;
		param = p.getParam("hill delta");
		if (param != null)
			meme_delta = Double.parseDouble(param);

		int SA_pop = 4;
		param = p.getParam("anne pop");
		if (param != null)
			SA_pop = Integer.parseInt(param);
		
		double SA_accel = 0.5;
		param = p.getParam("anne accel");
		if (param != null)
			SA_accel = Double.parseDouble(param);

		Portfolio port = new Portfolio();
		port.Merge(lport, rport, weight);			
		calculateLocalFitness(port, T); // initial evaluation;
		eval_c = 1;

		
		switch (method)
		{
		case 0: 
			/*
			 * Hill Climbing:
			 * 
			 * Moves the weight by "direction", until the fitness starts to fall, 
			 * then invert "direction" and divides it by "meme_accel";
			 */			
			int direction;
			if (weight > 0.5)
				direction = -1;
			else
				direction = 1;		

			
			while (eval_c < maxeval)
			{
				double old_fit = fitness;
				weight = weight + (meme_speed*direction);

				port.Merge(lport, rport, weight);			
				calculateLocalFitness(port, T);
				eval_c++;

				// switch directions
				if (fitness <= old_fit)
				{
					direction = direction * -1;
					meme_speed = meme_speed * meme_delta;
				}
				
				if (weight > 1.0)
				{
					weight = 1.0;				
					calculateLocalFitness(lport, T);		
					eval_c++;
					break;
				}
				if (weight < 0.0)
				{
					weight = 0.0;
					calculateLocalFitness(rport, T);		
					eval_c++;
					break;
				}
			}
			
			
			break;
		case 1: 
			/*
			 * Simulated annealing:
			 * 
			 * Generates POP candidates in the search area. Choose the best 
			 * candidate and reduce the search area by "SA Accel". Repeat.
			 */
			double Smin = 0, Smax = 1, Range = 1;
			double maxfit = fitness;
			eval_c++; // adding this because of the final evaluation;
			while (eval_c < maxeval)
			{
				double tmp;				
				RNG die = RNG.getInstance();
				if (maxeval - eval_c < SA_pop)
					SA_pop = maxeval - eval_c; // guarantees that wont go above maxeval
				for (int i = 0; i < SA_pop; i++)
				{
					tmp = die.nextDouble()*(Smax - Smin) + Smin;

					port.Merge(lport, rport, tmp);			
					calculateLocalFitness(port, T);
					eval_c++;

					if (fitness > maxfit)
					{
						maxfit = fitness; weight = tmp;
					}					
				}
				Range = Range*SA_accel;
				Smin = weight - Range/2;
				Smax = weight + Range/2;
				if (Smin < 0) Smin = 0;
				if (Smax > 1) Smax = 1;
				
			} // end while for Sim-Anne
			
			port.Merge(lport, rport, weight);
			calculateLocalFitness(port,T); // calculate correct fitness
			
			break;
		}		
			
		
	}
	
	public void eval(Trainer T)
	{
		Portfolio port = generatePortfolio();
		calculateLocalFitness(port, T);		
	}
	
	/**
	 * This function calculates the local fitness and 
	 * is called both by eval and by memetize
	 * @param T
	 */		
	public void calculateLocalFitness(Portfolio port, Trainer T)
	{
		Parameter p = Parameter.getInstance();
		String param;
				
		param = p.getParam("risk type");
		if (param == null)
		{
			System.err.println("Error. Parameter -risk type- not set");
			System.exit(0);
		}
		int running = param.compareToIgnoreCase("running");
		
		param = p.getParam("riskless return");
		if (param == null)
		{
			System.err.println("Error. Parameter \"riskless return\" not set");
			System.exit(0);
		}
		double r0 = Double.parseDouble(param);
		
		// calculate return and risk;
		Ereturn = T.getExpectedReturn(port);
		if (running == 0)
			Risk = T.getRunningRisk(port);
		else
			Risk = T.getCovarRisk(port);
		Sharpe = (Ereturn - r0)/Risk;
		
		fitness = Sharpe;
	}
	
	/**
	 *  Generates a weight list for this tree 
	 */
	public Portfolio generatePortfolio()
	{
		Portfolio result = new Portfolio();
		
		if (isTerminal)
			{
				result.setWeight(assetIndex, 1.0);
			}
		else
		{
			Portfolio lport = leftChild.generatePortfolio();
			Portfolio rport = rightChild.generatePortfolio();
			
			result.Merge(lport, rport, weight);	
		}
		
		return result;
	}
		
	
	
	/**
	 *  implements comparable. Higher fitness is better.
     */
    public int compareTo(MemeNode gt)
    {
    	if (fitness > gt.fitness)
    		return -1;
    	if (fitness < gt.fitness)
    		return 1;
    	return 0;
    }
    
    /**
     * Counts the number of assets in this tree. Possibly
     * limited by a weight treshold.
     * 
     */
    public int countAsset(double tresh)
    {
    	int ret = 0;
    	Portfolio W = generatePortfolio();
    	W.getMaxWeightIndex();
    	
    	
       	for (int i = 0; i < W.getAssetSize(); i++)
    		if (W.getWeightByPos(i) > tresh) 
    			ret++;
    		else return ret;
    	return ret;    		
    }

    
    /**
     * Returns a text string that contains an ASCII 
     * representation of this tree. verbose, if set 
     * to true, prints each node's contents.
     */
    public String dumptree(boolean verbose)
    {
    	return dumptree(0,verbose);
    }
    public String dumptree(int tab, boolean verbose)
    {
    	Market mkt = Market.getInstance();
    	
    	String ret = "";
    	String tabulation = "";
    	for (int i = 0; i < tab; i++)
    		tabulation = tabulation + "  ";
    	
    	ret = ret + tabulation + "(Node " + uniqueID + ")";
    	if (isTerminal)
    	{
    		ret = ret + " Terminal: " + mkt.assets.get(assetIndex).name;
    	}
    	else
    	{
    		ret = ret + " Non-Term: " + weight;
    	}

    	if (verbose)
    	{
    		ret = ret + "  Fitness: " + fitness;
    	}
    	
    	ret = ret + "\n";
    	
    	if (!isTerminal)
    	{
    		ret = ret + tabulation + leftChild.dumptree(tab + 1, verbose);
    		ret = ret + tabulation + rightChild.dumptree(tab + 1, verbose);
    	}
    		
    	return ret;
    }
	
    /**
     * Count the number of nodes and introns in this 
     * tree. Recursive. Introns are defined as the number 
     * of nodes that don't contribute to the final answer, 
     * because they are under zero weight.
     *  
     * @return
     * ret[0] is the total number of nodes, and 
     * ret[1] is the total number of introns;
     */
    public int[] countNodes()
    {
    	int ret[] = new int[2];
    	
    	if (isTerminal)
    	{
    		ret[0] = 1;
    		ret[1] = 0;
    	}
    	else
    	{
    		int lret[] = leftChild.countNodes();
    		int rret[] = rightChild.countNodes();
    		
    		ret[0] = lret[0] + rret[0] + 1;
    		
    		if (weight == 1.0)
    		{
    			ret[1] = lret[1] + rret[0];
    		}
    		else if (weight == 0.0)
    		{
    			ret[1] = lret[0] + rret[1];
    		}
    		else 
    		{
    			ret[1] = lret[1] + rret[1];
    		}
    		
    	}
    	
    	return ret;
    }
    
    public boolean insanity_Check()
    {
    	boolean ret = true;
    	
    	if (isTerminal)
    	{
    		return true;
    	}
    	
    	if (leftChild == null || rightChild == null)
    	{
    		return false;
    	}
    	
    	ret = leftChild.insanity_Check() && ret; 
    	ret = rightChild.insanity_Check() && ret;
    	
    	return ret;
    }

    /**
     * Calculate a distance matrix for all the terminals in a tree
     * where the distance is the minimum number of intermediate nodes 
     * between two indexes.
     * 
     * rootdist is an assistant variable, which is null when called
     * from the root node, and passed to children
     * 
     */
    public int[][] calculateDistanceMatrix(int[] rootdist)
    {
    	Market m = Market.getInstance();
    	int[][] ret = null;

    	// Do nothing case
    	if (isTerminal)
    		return ret;
    	
    	// Four cases (two mirrored): 2 terminals, 1 terminal, no terminals.
    	
    	// Two terminal case: Make Ret with two terminals, create rootdist.
    	if (leftChild.isTerminal && rightChild.isTerminal)
    	{
    		ret = new int[m.assets.size()][m.assets.size()];
	    		
    		rootdist[leftChild.assetIndex] = 1;
    		rootdist[rightChild.assetIndex] = 1;
    		ret[leftChild.assetIndex][rightChild.assetIndex] = 1;
    		ret[rightChild.assetIndex][leftChild.assetIndex] = 1;
    		return ret;
    	} // end two terminal case

    	// Two non-terminal case: Merge rets, and create a new Rootdist.
    	if (!leftChild.isTerminal && !rightChild.isTerminal)
    	{
    		int[] rootdist2 = new int[m.assets.size()];
    		int [][] ret2;
    	
    		ret = leftChild.calculateDistanceMatrix(rootdist);
    		ret2 = rightChild.calculateDistanceMatrix(rootdist2);
    		
    		// Then Merge the Rets. Existing smaller ret is included, then 
    		// Rootdist distances are compared.
    		for (int i = 0; i < rootdist.length; i++)
    		{
    			for (int j = i; j < rootdist.length; j++)
    			{
    				// merging rets
    				if (ret[i][j] == 0)
    				{
    					if (ret2[i][j] > 0)
    					{
    						ret[i][j] = ret2[i][j];
    						ret[j][i] = ret2[j][i];
    					}
    				}
    				else if ((ret2[i][j] > 0) && (ret2[i][j] < ret[i][j]))
    				{
    					ret[i][j] = ret2[i][j];
						ret[j][i] = ret2[j][i];
    				}
    				
    				// Checking distances from Root:
    				int ij = 0;
    				int ji = 0;
    				
    				if (rootdist[i] > 0 && rootdist2[j] > 0)
    					ij = rootdist[i] + rootdist2[j] + 1;
    				if (rootdist[j] > 0 && rootdist2[i] > 0)
    					ji = rootdist[j] + rootdist2[i] + 1;
    			
    				if ((ret[i][j] == 0) || ((ij > 0) && (ij < ret[i][j])))
    				{
    					ret[i][j] = ij;
    					ret[j][i] = ij;
    				}
    				if ((ret[i][j] == 0) || ((ji > 0) && (ji < ret[i][j])))
    				{
    					ret[i][j] = ji;
    					ret[j][i] = ji;
    				}

    				
    			}
    			// updating rootdist[i] now that we won't use it anymore
    			if ((rootdist[i] == 0) || ((rootdist2[i] > 0) && (rootdist2[i] < rootdist[i])))
    				rootdist[i] = rootdist2[i];
    			if (rootdist[i] > 0)
    				rootdist[i]++;
    		}    	
    	} // end two non-terminal case
    	
    	// Case Terminal - Not-Terminal
    	if (leftChild.isTerminal && (!rightChild.isTerminal))
    	{
    		ret = rightChild.calculateDistanceMatrix(rootdist);
    		// if things are working, this has updated both ret and rootdist
    		
    		if (rootdist[leftChild.assetIndex] == 0) // left terminal not in right.
    		{
    			for (int i = 0; i < rootdist.length; i++)
    				if (rootdist[i] > 0)
    				{
    					rootdist[i]++; // increase distance from root;
    					ret[i][leftChild.assetIndex] = rootdist[i]; // create links to new asset
    					ret[leftChild.assetIndex][i] = rootdist[i];
    				}
    			rootdist[leftChild.assetIndex]=1;
    		}
    		else // left terminal exists in right
    			for (int i = 0; i < rootdist.length; i++)
    				if (rootdist[i] > 0)
    				{
    					rootdist[i]++; // increase the basic distance of rtree terms
    					if ((ret[i][leftChild.assetIndex] > rootdist[i])||
    						(ret[i][leftChild.assetIndex] == 0))
    					{
    						ret[i][leftChild.assetIndex] = rootdist[i];
    						ret[leftChild.assetIndex][i] = rootdist[i];
    					}
    					if (i == leftChild.assetIndex)
    						rootdist[i] = 1;    					
    				}
    		return ret;
    	} //left terminal - right intermediate case
    	
    	// Non-terminal, Terminal Case
    	if (rightChild.isTerminal && (!leftChild.isTerminal))
    	{
    		ret = leftChild.calculateDistanceMatrix(rootdist);
    		// if things are working, this has updated both ret and rootdist
    		
    		if (rootdist[rightChild.assetIndex] == 0) // right terminal not in left.
    		{
    			for (int i = 0; i < rootdist.length; i++)
    				if (rootdist[i] > 0)
    				{
    					rootdist[i]++;
    					ret[i][rightChild.assetIndex] = rootdist[i];
    					ret[rightChild.assetIndex][i] = rootdist[i];
    				}
    			rootdist[rightChild.assetIndex]=1;
    		}
    		else
    			for (int i = 0; i < rootdist.length; i++)
    				if (rootdist[i] > 0)
    				{
    					rootdist[i]++; // increase the basic distance of rtree terms
    					if ((ret[i][rightChild.assetIndex] > rootdist[i])||
    						(ret[i][rightChild.assetIndex] == 0))
    					{
    						ret[i][rightChild.assetIndex] = rootdist[i];
    						ret[rightChild.assetIndex][i] = rootdist[i];
    					}
    					if (i == rightChild.assetIndex)
    						rootdist[i] = 1;
    				}
    		
    		return ret;
    	} // right terminal - left intermediate case

    	
    	
    	return ret;
    	
    }
    
    
}
