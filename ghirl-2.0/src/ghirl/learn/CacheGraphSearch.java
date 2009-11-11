package ghirl.learn;

import java.util.*;
import java.io.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.*;

/** 
 */

public class CacheGraphSearch
{
    private Graph graph = null;
    private GraphSearcher searcher = new BasicWalker();
    private GraphSearchDataset testExamples = new GraphSearchDataset();
    private SearchResultCache cache = null;
    private int numToSave = 1000;

    public class MyCLP extends BasicCommandLineProcessor {
	public void graph(String s) { graph = new TextGraph(s,'r'); }
	public void cache(String s) { cache = new SearchResultCache(s,'w'); }
	public void numToSave(String s) { numToSave = StringUtil.atoi(s); }
	public void annotate(String s) 
	{ 
	    GraphAnnotator ann = (GraphAnnotator)BshUtil.toObject(s,GraphAnnotator.class);
	    graph = AnnotatableGraph.addAnnotator( graph, ann );
	}
	public void loadFrom(String s) 
	{ 
	    try {
		searcher = (GraphSearcher)IOUtil.loadSerialized(new File(s)); 
	    } catch (Exception ex) {
		throw new IllegalArgumentException("can't load from '"+s+"'");
	    }
	}
	public void example(String s) { testExamples.add(new GraphSearchExample(s)); }
	public void searcher(String s) { searcher = (GraphSearcher)BshUtil.toObject(s,GraphSearcher.class); }
	public CommandLineProcessor searcherOpt() { return tryToGetCLP(searcher); }
	public void usage() 
	{ 
	    super.usage(); 
	    System.out.println(" remaining arguments are GraphSearchExample files"); 
	}
    }
    public void processArguments(String[] args) 
    {
	int argp = new MyCLP().consumeArguments(args,0);
	// treat unprocessed examples as 
	for (int i=argp; i<args.length; i++) {
	    //System.out.println("loading example from "+args[i]);
	    if ( args[i].startsWith("-") ) {
		System.err.println("unknown option "+args[i]);
	    } else {
		testExamples.add( new GraphSearchExample(args[i]) );
	    }
	}
    }

    public void cache()
    {
	searcher.setGraph( graph );
	int n=0;
	ProgressCounter pc = new ProgressCounter("caching searches","search",testExamples.size());
	for (GraphSearchExample.Looper i=testExamples.iterator(); i.hasNext(); ) {
	    GraphSearchExample example = i.nextExample();
	    GraphId id = GraphId.fromString(example.getQueryString());
	    if (!graph.contains(id)) System.out.println("can't cache query '"+example.getQueryString()+"'");
	    else {
		Distribution c = cache.get( id );
		if (c!=null) System.out.println("result found for "+id);
		else {
		    Distribution d = searcher.search( id ).copyTopN( numToSave );
		    cache.put( id, d );
		}
		n++;
		if (n%10==0) cache.sync();
	    }
	    pc.progress();
	}
	pc.finished();
    }


    static public void main(String[] args) 
    {
	CacheGraphSearch c = new CacheGraphSearch();
	c.processArguments(args);
	System.out.println("searcher: "+c.searcher);
	c.cache();
    }
}
