/* Market is a collection of assets.
 * For example, Nikkei, Nasdaq, etc.
 * 
 * A market is characterized by all assets being inside the same directory.
 */

package data;

import java.util.*;
import engine.*;
import java.io.*;
import java.text.*;

public class Market {

	private static Market instance;
	
	public String name;
	public Vector<Asset> assets;
	public Vector<Price> index;
	
	protected Market() 
	{
		name = null;
		assets = null;
		index = null;
	}
	
	public static synchronized Market getInstance() 
	{
		if(instance == null) {
			instance = new Market();
	      }	
		return instance;
	}	
	
	
	// Set up a name for the market (for reloading data)
	// not sure if this will be useful yet.
	public void setName(String n)
	{
		name = n;
	}
		
	// reads asset data from a directory
	public void loadDir(String dirname)
	{
		assets = new Vector<Asset>();
		File dir = new File (dirname);
		
		// DEBUG: use this to see where the data is being loaded from.
		// System.err.println(dir.getAbsolutePath());
		
		System.out.println("Loading Asset Data on " + dir.getAbsolutePath());
		
		File[] files = dir.listFiles(new ExtensionFilter(".csv"));
		if (files == null || files.length == 0)
		{
			System.out.println("Error: invalid data path on parameter file");
			System.exit(0);
		}
		
		
		for (int i = 0; i < files.length; i++)
		{
			if (files[i].length() > 0)
				assets.add(new Asset(files[i]));
			else
				System.out.println("Skipping " + files[i].getName() + ": file empty");
		}
				
		Collections.sort(assets);
		
		
	}
	
	public void loadDirIndex(String dirname)
	{
		File dir = new File (dirname);
		System.out.println("Loading index...");
		loadIndex(dir.listFiles(new ExtensionFilter(".idx")));
	}
	
	
	
	
	public void loadIndex(File f[])
	{
		index = new Vector<Price>();
		Parameter param = Parameter.getInstance();
		
		String datefmt = param.getParam("index date format");
		if (datefmt == null)
		{
			System.err.println("ERROR: Please specify date format");
			System.exit(1);
		}	
		String separator = param.getParam("index separator");
		if (separator == null)
		{
			System.err.println("ERROR: Please specify data separator");
			System.exit(1);
		}
		if (separator.compareToIgnoreCase("space") == 0)
			separator = " ";
		String pripos = param.getParam("index price position");
		if (pripos == null)
		{
			System.err.println("ERROR: Please specify price position");
			System.exit(1);
		}
		String volpos = param.getParam("index volume position");
		if (volpos == null)
		{
			System.err.println("ERROR: Please specify volume position");
			System.exit(1);
		}
		
		
		
		BufferedReader reader;	
		String line = null;	

       try
       {
    	   reader =
    		   new BufferedReader(new FileReader(f[0]));

    	   // skipping first line
    	   line = reader.readLine();
    	   
    	   while ((line = reader.readLine()) != null)
       		{
    		   String[] input = line.split(separator);
    		   
    		   
    		   SimpleDateFormat dateparser = new SimpleDateFormat(datefmt, new Locale("en"));
    		   Date d = dateparser.parse(input[0]);

    		   index.add(new Price(d, 
    				   Double.valueOf(input[Integer.valueOf(pripos)]),
    				   Double.valueOf(input[Integer.valueOf(volpos)])));
       		}
    	   
    	   Collections.sort(index);

    	   //DEBUG -- prints read data
    	   //for (int i = 0; i < index.size(); i++)
    	   //{
    	   //   	System.out.println(index.get(i).p);
    	   //}
    	   
    	   
       } catch (Exception e)
       {
    	   System.err.print("Error opening file: " + f[0].getAbsolutePath() + "\n");
    	   System.err.print("Message: " + e.getMessage() + "\n\n");
     	   System.exit(1);
       }
       	       	
	}
	
	// remove all asset data
	public void makeEmpty()
	{
		if (assets != null)
			assets.clear();
	}

	
	public Asset getAssetFromName(String n)
	{
		for (int i = 0; i < assets.size(); i++)
			if (assets.get(i).name.equals(n))
				return assets.get(i);
		return null;
	}

	public int getIndexPosByDate(Date d)
	{			
		for (int i = 0; i < index.size(); i++)
		{
			if (Utils.calcMonths(d, index.get(i).d) == 0)
				return i;
		}
		return -1;	
		
	}
	
	public void resetBadness()
	{
		for (int i=0; i < assets.size(); i++)
		{
			assets.get(i).badness = false;
		}
	}
}	


