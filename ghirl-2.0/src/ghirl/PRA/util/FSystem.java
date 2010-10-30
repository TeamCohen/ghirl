/**
 * 
 */
package ghirl.PRA.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author nlao
 *
 */
public class FSystem {
	public static String cmd(String cmd, String dir){
		return cmd(cmd, new File(dir),false, false);
	}
	
	public static String cmd(String cmd){
		return cmd(cmd, null,false, false);
	}
	public static String cmd(String cmd,boolean silent){
		return cmd(cmd,null, silent,false);
	}
	public static String cmd(String cmd,boolean silent, boolean silentE){
		return cmd(cmd,null, silent,silentE);
	}
	public static String cmd(String cmd,File dir
			, boolean silent, boolean silentE){
		if (!silent)
			System.out.print("cmd= "+cmd+"\n");
    try {
      Process child = Runtime.getRuntime().exec(cmd,null,dir);
      //InputStream is = child.getInputStream();
      
      String cout = FString.inputStream2String(child.getInputStream());
      
      //if (!silent)    	System.out.println(s);
      if (!silentE){
        InputStream ise=child.getErrorStream();        
        System.err.print(FString.inputStream2String(ise));
      }
      child.destroy();
      child=null;
      return cout;
	  }		
	  catch(Exception e){//IOException e
			System.err.println(e.getClass().getName());			
			e.printStackTrace();
			System.exit(-1);
			return e.getCause().getMessage();
		}
	}	
	public static BufferedReader cmdGetStream(String cmd){
    try {
      Process child = Runtime.getRuntime().exec(cmd,null,null);
      //InputStream is = child.getInputStream();
      return new BufferedReader(new InputStreamReader(child.getInputStream())); 
	  }		
	  catch(Exception e){//IOException e
			System.err.println(e.getClass().getName());			
			System.err.println(e.getCause().getMessage());
			e.printStackTrace();
			return null;
		}
	}	

	public static String methodName(){
		//return  new System.Diagnostics.StackTrace().GetFrame(0).GetMethod().Name;
		return null;
	}
	//extends String {
	//left()
	//right();
	
	public static String readLine() {
		String s=null;
		try {
			s = (new BufferedReader(new InputStreamReader(System.in)))
					.readLine();
		} catch (IOException ioe) {
			System.out.println("IO error trying to read from keyboard");
			System.exit(1);
		}
		return s;
	}
/**
 	
Format Pattern 	Result
"yyyy.MM.dd G 'at' hh:mm:ss z"	
1996.07.10 AD at 15:08:56 PDT

"EEE, MMM d, ''yy" 	
Wed, July 10, '96

"h:mm a" 	
12:08 PM

"hh 'o''''clock' a, zzzz" 	
12 o'clock PM, Pacific Daylight Time

"K:mm a, z" 	
0:00 PM, PST

"yyyyy.MMMMM.dd GGG hh:mm aaa"	
1996.July.10 AD 12:08 PM
 */	
	public static String formatDate1(Date date, String format) {
		SimpleDateFormat df	= new SimpleDateFormat(format);
		///usr2/local/jdk1.5.0_07/jre/lib/zi/US/Eastern
		//TimeZone tz = TimeZone.getTimeZone("ECT");
		//TimeZone tz = TimeZone.getTimeZone("GMT");
		return df.format(date);
	}
//DateFormat df =  (SimpleDateFormat)DateFormat.getDateTimeInstance(
//DateFormat.LONG,	 DateFormat.LONG, Locale.US);
//df.setTimeZone(tz);
	//numerical time format
	public static String currentTimeN() {
		return (new SimpleDateFormat(
				"yyyy.MMdd.HHmmss"))//"EEE MMM dd HH:mm:ss zzz yyyy"
			.format(new Date());
	}
	public static String currentTime() {
		return (new Date()).toString();
	}

	public static void printMemoryTime() {
		System.out.println(currentTime()+"\t"+memoryUsage());
	}
	public static String memoryUsage() {
/*
 		long free = Runtime.getRuntime().freeMemory();
		long total = Runtime.getRuntime().totalMemory();
		long max = Runtime.getRuntime().maxMemory();
		long used = total-free;
*/
		double free = Runtime.getRuntime().freeMemory()/1000000.0;
		double total = Runtime.getRuntime().totalMemory()/1000000.0;
		double max = Runtime.getRuntime().maxMemory()/1000000.0;
		double used = total-free;
		return String.format(
			"used(%.0fM)=total(%.0fM)-free(%.0fM), max(%.0fM)"
			,used,total,free,max);
	}
	
	public static String memoryUsageS() {

				double free = Runtime.getRuntime().freeMemory()/1000000.0;
				double total = Runtime.getRuntime().totalMemory()/1000000.0;
				double max = Runtime.getRuntime().maxMemory()/1000000.0;
				double used = total-free;
				return String.format(	"%.0fM/%.0fM"	,used,max);
			}
	public static double memoryRate() {

		double free = Runtime.getRuntime().freeMemory()/1000000.0;
		double total = Runtime.getRuntime().totalMemory()/1000000.0;
		double max = Runtime.getRuntime().maxMemory()/1000000.0;
		double used = total-free;
		return used/max;
	}
	public static long memoryUsed() {
		return (Runtime.getRuntime().totalMemory()
			- Runtime.getRuntime().freeMemory())/1000000;		
	}
	public static String formatTime(long sec) {
		long min=sec/60;
		long hour=min/60;
		long day=hour/24;
		return String.format("%ds=%dm=%dh=%dd"	
			,sec, min,hour,day);
	}
	public static void sleep(long milSec){
		try{
			Thread.sleep(milSec);
	  }catch(Exception ex){
	    ex.printStackTrace();
	  }
	}
	public static void die(String msg){
		System.err.println(msg);
		System.err.println ((new Exception()).getStackTrace());
		System.exit(-1);
	}

	public static void _tmp2() {
		//The following will get you a string that contains the full path
		//to the users working directory
		String wd = System.getProperty("user.dir");
		//The following will get you a string that contains the full path
		//to the users home directory
		String home = System.getProperty("user.home");
		//It is customary to put the config file in the users home directory.
		//Also, don't forget to use the platform independent separator
		//character which is "\" for windows and "/" for linux, unix
		//Here is how to get the separator
		String fs = System.getProperty("file.separator");
		//Or you can create a File object like so
		File configfile = new File(home, ".config");
		//where home is the string from above and ".config" is
		//the name of the config file you want to use		
	}
	public static BufferedReader getStdIn(){
		try {
			return new BufferedReader(new InputStreamReader(System.in,"UTF8"));
		} catch (Exception e) {
			System.err.println(
					"cannot open stdIn");
			return null;
		}
	}
}
