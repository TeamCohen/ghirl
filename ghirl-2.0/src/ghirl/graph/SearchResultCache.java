package ghirl.graph;

import java.util.*;
import java.io.*;
import java.util.zip.*;

import com.sleepycat.bind.serial.*;
import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

import edu.cmu.minorthird.util.*;
import ghirl.persistance.SleepycatDB;
import ghirl.util.*;

/** Save results of a GraphSearcher.
 */

public class SearchResultCache extends SleepycatDB
{
    private Database id2ResultMap;

    public SearchResultCache(String dbName,char mode)
    {
	try {
	    initDBs(dbName,mode);
	    id2ResultMap = openDB("_searchResult");
	} catch (Exception ex) {
	    handleException(ex);
	}
    }

    public void clear(GraphId id)
    {
	try {
	    clearDB( id2ResultMap, id.toString() );
	} catch (Exception ex) {
	    handleException(ex);
	}
    }

    public void put(GraphId id,Distribution d)
    {
	try {
	    //System.out.println("storing "+d+" for "+id);
	    putDB( id2ResultMap, id.toString().getBytes("UTF-8"), toBytes(d) );
	    //System.out.println("retrieval should give "+get(id));
	} catch (Exception ex) {
	    handleException(ex);
	}
    }

    public Distribution get(GraphId id)
    {
	try {
	    byte[] bytes = getFirstDB(id2ResultMap, id.toString().getBytes("UTF-8"));
	    if (bytes==null) return null;
	    else return fromBytes( bytes );
	} catch (Exception ex) {
	    handleException(ex);
	    return null;
	}
    }

    private void handleException(Exception ex)
    {
	ex.printStackTrace();
	throw new IllegalStateException("unexpected error in SearchResultCache: "+ex);
    }


    /** Convert a distribution over graphids's into an array of bytes. */
    static private byte[] toBytes(Distribution d) throws IOException
    {
	ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
	PrintStream out = new PrintStream(new GZIPOutputStream(byteArrayStream));
	//PrintStream out = new PrintStream(byteArrayStream);
	for (Iterator i=d.iterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    out.println(id.toString());
	    double w = d.getLastWeight();
	    out.println(w);
	}
	out.close();
	return byteArrayStream.toByteArray();
    }

    /** Recover a Distribution from a byte array produced by toBytes() */
    static private Distribution fromBytes(byte[] bytes) throws IOException
    {
	ByteArrayInputStream byteArrayStream = new ByteArrayInputStream(bytes);
	LineNumberReader in = new LineNumberReader(new InputStreamReader(new GZIPInputStream(byteArrayStream)));
	//LineNumberReader in = new LineNumberReader(new InputStreamReader(byteArrayStream));
	Distribution d = new TreeDistribution();
	String line = null;
	while ((line = in.readLine())!=null) {
	    GraphId id = GraphId.fromString(line);
	    String weightString = in.readLine();
	    d.add( StringUtil.atof(weightString), id );
	}
	in.close();
	return d;
    }

    // for testing purposes
    private static void testMain(String[] args) throws IOException
    {
	if (args.length<3) {
	    System.out.println("cache mode keyId [id1 ... idk]");
	    System.exit(0);
	}
	SearchResultCache cache = new SearchResultCache(args[0], args[1].charAt(0));
	GraphId key = GraphId.fromString(args[2]);
	Distribution d = new TreeDistribution();
	for (int i=3; i<args.length; i++) {
	    d.add( 1.0, GraphId.fromString(args[i]) );
	}
	if (d.size()>0) {
	    cache.put( key, d );				
	} else {
	    d = cache.get( key );
	    System.out.println("Result for "+key+":\n" + d);
	    System.out.println("Formatted Result for "+key+":\n" + d.format());
	}
	cache.sync();
    }

    public static void cacheGeneIdSearchMain(String[] args) throws IOException
    {
	Graph graph = new TextGraph("mouse",'r');
	BasicWalker walker = new BasicWalker();
	walker.setGraph( graph );
	//walker.setNumSteps(3000);
	String synonymListFileName = "data/mouse/mouse_synonyms.list";
	SearchResultCache cache = new SearchResultCache("mouseGeneId-top1000",'w');

	Runtime rt = Runtime.getRuntime();

	int n = 0;
	LineNumberReader in = new LineNumberReader(new FileReader(new File(synonymListFileName)));
	String line = null;
	ProgressCounter pc = new ProgressCounter("loading "+synonymListFileName,"lines");
	while ((line = in.readLine())!=null) {
	    n++;
	    if (n<50000) continue;
	    //System.out.println("read: "+line);
	    String[] parts = line.split("\\t+");
	    String normalIdName = parts[0].replace(':','_');
	    GraphId normalId = GraphId.fromString(normalIdName);
	    Distribution c = cache.get( normalId );
	    if (c!=null) {
		//System.out.println("result found for "+normalId);
	    } else {
		Distribution d = walker.search( normalId ).copyTopN( 1000 );
		cache.put( normalId, d );
		System.out.println("stored distribution of "+d.size()+" for "+normalId);
		if (n%10==0) cache.sync();
		//if (n%10==0) walker.clearCache();
		System.out.println("Memory usage after "+n+" lines: "+((double)rt.totalMemory()/(1024*1024))+"Mb");
	    }
	    pc.progress();
	}
	pc.finished();
	in.close();
	cache.sync();
    }
}
