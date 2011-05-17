package ghirl.graph;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import ghirl.util.*;

import org.apache.log4j.*;

public class DotProductSearcher implements GraphSearcher, CommandLineProcessor.Configurable
{
	private static Logger log = Logger.getLogger( DotProductSearcher.class );
	private WeakHashMap cache = new WeakHashMap();
	protected Graph graph;
	private Graph noncachingVersionOfCurrentGraph = null;

	private GraphSearcher forwardSearcher = new BasicWalker();
	private GraphSearcher backwardSearcher = new BasicWalker();
	private int numCandidatesToRescore = 100;
	private int numBackwardsCandidatesToRetain = 0;

	public DotProductSearcher(int numToRescore)
	{
		this(new BasicWalker(),numToRescore);
	}

	public DotProductSearcher(GraphSearcher searcher)
	{
		this(searcher,100);
	}


	public DotProductSearcher()
	{
		this(new BasicWalker(),100);
	}

	public DotProductSearcher(GraphSearcher searcher,int numToRescore)
	{
		this.forwardSearcher = this.backwardSearcher = searcher;
		this.numCandidatesToRescore = numToRescore;
	}

	//
	//
	//
	public class MyCLP extends BasicCommandLineProcessor
	{
		public void numToRescore(String s) { 
	    numCandidatesToRescore = StringUtil.atoi(s); 
		}
		public void pruneBack(String s)	{
			numBackwardsCandidatesToRetain = StringUtil.atoi(s);
		}
		public void forwardSearcher(String s) {
	    forwardSearcher = (GraphSearcher)BshUtil.toObject(s,GraphSearcher.class);
		}
		public void backwardSearcher(String s) {
	    backwardSearcher = (GraphSearcher)BshUtil.toObject(s,GraphSearcher.class);
		}
		public CommandLineProcessor forwardOpt() {
	    return tryToGetCLP( forwardSearcher );
		}
		public CommandLineProcessor backwardOpt() {
	    return tryToGetCLP( backwardSearcher );
		}

	}
	public CommandLineProcessor getCLP() { return new MyCLP(); }


	/** Define the graph over which the walk will take place. */
	public void setGraph(Graph graph) 
	{ 
		if (graph!=noncachingVersionOfCurrentGraph) {
	    noncachingVersionOfCurrentGraph = graph;
	    this.graph=new CachingGraph(graph); 
	    cache = new WeakHashMap();
	    forwardSearcher.setGraph(graph);
	    backwardSearcher.setGraph(graph);
		}
	}
	public Graph getGraph() 
	{ 
		return graph; 
	}


	public Distribution search(GraphId id)
	{
		return search(new TreeDistribution(id), null);
	}

	public Distribution search(GraphId id, NodeFilter nodeFilter)
	{
		return search(new TreeDistribution(id), nodeFilter);
	}

	public Distribution search(Distribution queryDistribution)
	{
		return search(queryDistribution,null);
	}

	public Distribution search(Distribution queryDistribution, NodeFilter filter)
	{
		Distribution queryResult = forwardSearcher.search(queryDistribution);
		Distribution tmp = queryResult.copy();
		if (filter!=null) tmp = filter.filter( graph, queryResult );
		Distribution candidateDist = tmp.copyTopN(numCandidatesToRescore);
		return rescoreCandidates(candidateDist,queryResult);
	}

	// rescore candidates based on dot-product similarity of their
	// search result to the query result
	private Distribution rescoreCandidates(Distribution candidateDist, Distribution queryResult)
	{
		//System.out.println("rescoring these candidates:\n"+candidateDist.format());
		Distribution rescoredDist = new TreeDistribution();
		ProgressCounter pc = new ProgressCounter("rescoring candidates","candidate",candidateDist.size());
		for (Iterator i = candidateDist.orderedIterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    Distribution idResult = cacheSearch(id);
	    double weight = dotProduct( idResult, queryResult );
	    rescoredDist.add( weight, id );
	    pc.progress();
		}
		pc.finished();
		return rescoredDist;
	}

	static public double dotProduct( Distribution x, Distribution y )
	{
		double sum = 0;
		for (Iterator i=x.iterator(); i.hasNext(); ) {
	    Object obj = i.next();
	    double w = x.getLastProbability();
	    sum += w * y.getWeight( obj );
		}
		return sum;
	}

	public Distribution cacheSearch(GraphId id)
	{
		Distribution result = (Distribution)cache.get(id);
		if (result==null) {
			result = backwardSearcher.search(id);
			if (numBackwardsCandidatesToRetain>0) 
				result = result.copyTopN(numBackwardsCandidatesToRetain);
			cache.put( id, result );
		}
		return result;
	}

	public String toString() 
	{ 
		return "[DotProdSearcher: numRescore="+numCandidatesToRescore+" forward="+forwardSearcher+"]";
	}
}
