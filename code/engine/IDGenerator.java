package engine;

public class IDGenerator {

		private static IDGenerator instance;
		private long cur = 0;		
		
		protected IDGenerator () 
		{
		}
				
		public static synchronized IDGenerator getInstance() 
		{
			if(instance == null) {
				instance = new IDGenerator();
		      }	
			return instance;
		}	
		
		public long newID()
		{
			cur++;
			return cur;
		}
	
}
