package ghirl.util;

import java.io.*;
import java.util.*;
import org.apache.log4j.Logger;

/**
 * Access properties specified in System.getProperties() or ghirl.properties, in that order.
 */
public class Config
{
  private static final Logger log = Logger.getLogger(Config.class);
    private static Properties props = new Properties();
    public static final String DBDIR = "ghirl.dbDir";
    public static final String GRAPHNAME = "ghirl.graphName";
    public static final String ISAFLAVORLINKS = "ghirl.isaFlavorLinks";
    static {
	try {
	    InputStream in = Config.class.getClassLoader().getResourceAsStream("ghirl.properties");
	    if (in != null) props.load(in);
            log.info("Loaded ghirl.properties");
	} catch (IOException e) {
	    throw new IllegalStateException("error getting ghirl.properties: "+e);
	}
	// override properties with command line, if a flag is present
	for (Enumeration i=System.getProperties().propertyNames(); i.hasMoreElements(); ) {
	    String p = (String)i.nextElement();
	    props.setProperty(p, System.getProperty(p));
	}
    }
    public static Properties getProperties() { return props; }
    public static String     getProperty(String prop) { return props.getProperty(prop); }
    public static void       setProperty(String prop, String val) { props.setProperty(prop, val); }
    public static String     getProperty(String prop,String def) { return props.getProperty(prop,def); }
}
