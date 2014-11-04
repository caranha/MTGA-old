/* This Class includes some calculations often used */

package engine;

import java.util.*;

public class Utils {
	
	
	/* Calculates how many months exists between date "start" and 
	 * date "end". Months are counted as changes in the "months" field. 
	 * 0 if in the same month.
	 */
	public static int calcMonths(Date start, Date end)
	{
		Calendar cs = Calendar.getInstance();
		Calendar ce = Calendar.getInstance();
		
		cs.setTime(start);
		ce.setTime(end);
		
		int ys = cs.get(Calendar.YEAR);
		int ye = ce.get(Calendar.YEAR);
		int ms = cs.get(Calendar.MONTH);
		int me = ce.get(Calendar.MONTH);
		
		int ret = 12 * (ye - ys);
		ret += me - ms;

		
		return ret;
	}

}
