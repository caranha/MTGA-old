package engine;
/* For now I'm using this only for 
 * ordering portfolio weigths for output.
 */

public class PortWeight implements Comparable<PortWeight>{
	
	String name;
	int index;
	Double weight;
	
	
	public PortWeight(String n, int idx, Double w)
	{
		name = n;
		weight = w;
		index = idx;
	}
	
	public int compareTo(PortWeight pw)
	{
		if (pw.weight < this.weight)
			return -1;
		if (pw.weight > this.weight)
			return 1;
		return 0;			
	}

}
