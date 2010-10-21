package ghirl.PRA.util;

/**
 * Manages time elapsed in a certain module.
  * @author ni lao
 */


import java.io.Serializable;
import java.text.Format;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StopWatch implements Serializable {

	public static final long serialVersionUID = 20061207L; // YYYYMMDD

	private Map<String, Long> startTime;
	private Map<String, Long> elapsedTime;
	
	public StopWatch() {
		startTime   = new LinkedHashMap<String, Long>();
		elapsedTime = new LinkedHashMap<String, Long>();
		start();
	}
	
	/**
	 * Get a set of label names
	 * @return set of label names
	 */
	public Set<String> keySet() {
		return elapsedTime.keySet();
	}
	public void start() {
		start( "");
	}
	public long stop() {		
		return stop( "");	
	}
	
	/**
	 * Set the current time as start time
	 * @param key
	 */
	public void start( String key ) {
		startTime.put( key, System.currentTimeMillis() );
	}

	/**
	 * Set the current time as end time
	 * @param key
	 */
	public long stop( String key ) {
		long d=System.currentTimeMillis() -startTime.get(key);
		elapsedTime.put( key, d );
		return d;
	}
	
	/**
	 * Get elapsed time (in milli second) of a specific event, specified by a key.
	 * @param key elapsed time of a key
	 */
	public long getMilSec( String key ) {
		if ( elapsedTime.get( key ) > 0 ) {
			return elapsedTime.get( key );
		} else {
			return -1L;
		}
	}
	
	/**
	 * Get elapsed time (in second) of a specific event, specified by a key.
	 * @param key
	 * @return elapsed time in second
	 */
	public double getSec( String key ) {
		return elapsedTime.get( key ) / 1000.0f;
	}


	/**
	 * Get the summary report
	 * @return summary report 
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for ( String key : elapsedTime.keySet() )
			sb.append( key + "\t" +  getSec( key )  + "\n" );
		return sb.toString(); 
	}
	
	/**
	 * Get the summary report in tab separated form (Excel-friendly)
	 * @return summary report in tab separated value
	 */
	public String print() {
		StringBuilder sb = new StringBuilder();
		for ( String key : elapsedTime.keySet() ) 
			sb.append( key + "\t" + getSec( key ) + "\t" );
		return sb.toString(); 
	}
	
	/**
	 * Get time epression in <code>mm:ss</code> 
	 * @param sec
	 * @return mm:ss
	 */
	public String sec2string( long time ) {
/*		int ss = (int)( sec %60); 	sec/=60;
		int mm = (int)( sec % 60 ); sec/=60;
		int hh = (int)( sec  );
		return hh+":"+mm+":"+ss;
	*/	
		//return formatter.format(new java.util.Date(time));
		return String.format("%.1fs", ((float)time)/1000);
	}
	public Format formatter=new java.text.SimpleDateFormat("hh:mm:ss");
	
	
	public String getTime(){
		return sec2string(stop());
	}
	public void printElapsedTime(){
		System.out.println("Time Elapsed= "+getTime());
	}
	public static void main(String[] args) {
		StopWatch sw= new StopWatch();
		//FSystem.sleep(1000);
		System.out.print("Total time= "+sw.getTime());

		
	}
}
