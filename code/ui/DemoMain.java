package ui;
import javax.swing.*;        
import data.*;
import engine.*;

public class DemoMain {

	static Market mkt;
	static Parameter param;
	
	/**
	 * Runs the rountines needed to load the program's data
	 */
	static void initData()
	{
		mkt = Market.getInstance();
			
		mkt.setName(Parameter.getInstance().getParam("data name"));
		mkt.loadDir(Parameter.getInstance().getParam("data directory"));
		mkt.loadDirIndex(Parameter.getInstance().getParam("data directory"));
	}

	/**
	 * Initial configuration of the program. 
	 */
	static void initConfig()
	{
		param = Parameter.getInstance(); // creating parameter object
		
		try
		{
			param.load("parameter.par");
		} catch (Exception e)
		{
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}
	
	/**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event-dispatching thread.
     */	
    static void createAndShowGUI() {
        //Create and set up the window.
        //JFrame frame = new JFrame("HelloWorldSwing");
        JFrame frame = new JFrame("EvoPortfolio Demo");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        MainTabbedPane p1 = new MainTabbedPane();               

        frame.add(p1.getPanel());
        frame.pack();	

        //Display the window.
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.

    	initConfig();
    	initData();
    	
    //    javax.swing.SwingUtilities.invokeLater(new Runnable() {
    //        public void run() {
                createAndShowGUI();
                
     //       }
     //   });
    }
}


