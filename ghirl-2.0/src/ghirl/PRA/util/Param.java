/**
 * automatically load corresponding .conf file on construction
 */
package ghirl.PRA.util;

import ghirl.PRA.util.TMap.MapSS;
import ghirl.PRA.util.TVector.VectorS;

import java.io.FileInputStream;
import java.io.Serializable;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is a generic parameter class.
 * 
 * By default it load the configuration file 
 * <project path>/conf/<derived class name>
 * 
 * If you call overwriteFrom(String confFile)
 * all the parameters for all classes will be overwritten 
 * by the parameters in confFile
 * 
 * @author nlao
 *
 */
public class Param implements Serializable{//extends Properties  {
	
	//private static Logger log = Logger.getLogger( Param.class );
	
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	//actually we could have just extends MapSS here	
	public static String path_proj;	
	public String path_conf;
	public String path_data;
	public String path_cache;
	//public Param() {this(getClass());	}
	
	public String getBaseCode(){//String hyperParam){
		String s= getString("vDParam","");
		if (s.equals("")) return "";
		
		VectorS vDParam= FString.toVS(s,",");
		String code="";
		for (String dp: vDParam)
			code+= String.format("_%s=%s", dp, getString(dp));
		return code;
	}
	//are system properties useful?
	//http://java.sun.com/docs/books/tutorial/essential/environment/sysprop.html
	static {
		try {
			String cp=FFile.getClassPath(Param.class);
			path_proj=FFile.getCanonicalPath( cp+"../");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private Properties p = new Properties();
	
	public void parse(Properties p){
		this.p = p;
		parse();
	}
	public void parse(){}	
	
	public Integer getInt(String key, Integer dft){
		//if (dft==null) return null;
		String x=p.getProperty(key);//, dft+"");// p.getProperty(key);
		if (x==null) return dft;
		return Integer.parseInt(x.trim());
	}
	public Double getDouble(String key, Double dft){
		//if (dft==null) return null;
		String x= p.getProperty(key);//, dft+"");
		if (x==null) return dft;
		return Double.parseDouble(x.trim());
	}
	public Boolean getBoolean(String key, Boolean dft){
		//if (dft==null) return null;
		String x= p.getProperty(key);//, dft+"");
		if (x==null) return dft;
		return Boolean.parseBoolean(x.trim());
	}
	public char getChar(String key, char dft){
		String x= p.getProperty(key);//, dft+"");
		if (x==null) return dft;
		return x.charAt(0);
	}
	
	public String getString(String key, String dft){
		//if (dft==null) return null;
		String x= p.getProperty(key);//, dft);
		if (x==null) return dft;
		return x.trim();
	}	
	
	/**Force to provide parameter*/
	public String getString(String key) {
		try {
			return p.getProperty(key).trim();
		} catch ( Exception e ) {
			e.printStackTrace();
			//log.warn( "Configuration \""+key+"\" is missing" );
			//System.exit(-1);
			return "";
		}
	}
	
	public Param(Class c) {//throws IOException{
		//String fn=null;
		//fn =FFile.getCanonicalPath( path_conf + c.getName() + ".conf");
		//load(FFile.bufferedReader(fn));
		String cn = c.getName();
		//path_conf= FFile.getCanonicalPath(path_proj	+ "conf/" +cn);
		//path_data= FFile.getCanonicalPath(path_proj	+ "data/" +cn);
		//path_cache= FFile.getCanonicalPath(path_proj	+ "cache/" +cn);

		path_conf= path_proj	+ "/conf/"+cn;
		path_data= path_proj	+ "/data/"+cn;//+"/";
		path_cache= path_proj	+ "/cache/"+cn;//+"/";

		try {
			p.load(new FileInputStream(path_conf));
		} catch (Exception e) {
			//log.warn( "Could not load .conf for "+ cn );
			//e.printStackTrace();
		}
		p.putAll(ms);
	}
	
	//to ovewrite properties of any class that uses Param
	public static MapSS ms= new MapSS();
	public static final Pattern pParam= Pattern.compile("(.*?)=(.*?)");    

	
	//beware, this only happens after all static things are initialized
	/**
	 * All the parameters for all classes will be overwritten 
	 * by the parameters defined in args
	 * @param args 
	 */
	public static void overwrite(String[] args){
		for (String s: args){
			Matcher ma = pParam.matcher(s);
			if (!ma.matches()){
				//log.warn("parse paramater failed " + s +"\n");
				continue;
			}
			ms.put(ma.group(1), ma.group(2));
		}
	}
	public static void overwrite(Param p){
		for (Object s: p.p.keySet())
			ms.put((String) s, (String) p.p.get(s));		
	}
	
	
	/**
	 * All the parameters for all classes will be overwritten 
	 * by the parameters defined in arguments
	 * @param confFile ,-splitted paramters
	 */
	public static void overwrite(String arguments){
		overwrite(arguments.split(","));
	}
	
	/**
	 * All the parameters for all classes will be overwritten 
	 * by the parameters in conf
	 * @param confFile
	 */
	public static void overwriteFrom1(String confFile){
		Properties p = new Properties();
		try {
			p.load(new FileInputStream(confFile));
		} catch (Exception e) {
			//log.warn( "Could not load .conf at "+ conf );
			//e.printStackTrace();
		}
		for (Object s: p.keySet())
			ms.put((String) s, (String) p.get(s));		
	}
	public static void overwriteFrom(String confFile){
		for (String line: FTable.enuLines(confFile)){
			if (line.startsWith("#"))
				continue;
			int i=line.indexOf('=');
			if (i<=0) 
				continue;
			String key= line.substring(0,i);
			String value=line.substring(i+1);
			ms.put(key, value);		
		}
	}

}

