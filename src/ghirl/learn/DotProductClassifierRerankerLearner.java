package ghirl.learn;

import ghirl.util.*;
import ghirl.graph.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;
import java.io.*;

/** 
 * Learn from examples a GraphSearcher that re-ranks examples based on
 * scores from a learned classifier.
 */

public class DotProductClassifierRerankerLearner extends ClassifierRerankerLearner
{
    public DotProductClassifierRerankerLearner()
    {
	machine = new DotProdInstanceMachine();
    }

    public CommandLineProcessor getCLP() 
    { 
	return new 
	    JointCommandLineProcessor(new CommandLineProcessor[]{new MyCLP(), new MyDotProdCLP()});
    }
    public class MyDotProdCLP extends BasicCommandLineProcessor 
    {
	public void fe2(String s) { 
	    LinkFeatureExtractor fe = (LinkFeatureExtractor)BshUtil.toObject(s,LinkFeatureExtractor.class);
	    ((DotProdInstanceMachine)machine).walker2.setFE( fe ); 
	}
	public CommandLineProcessor walker2Opt() {
	    return tryToGetCLP( ((DotProdInstanceMachine)machine).walker2 );
	}
    }

    static class DotProdInstanceMachine extends ClassifierRerankerLearner.WalkerInstanceMachine
    {
	public FEWalker walker2 = new FEWalker();
	private Distribution fullQueryRanking;
	private WeakHashMap cache;
	private Graph nonCachingVersionOfLastGraph=null;

	public Distribution generateCandidates(Graph graph,Distribution initDist,NodeFilter filter)
	{
	    getWalker().setGraph(graph);
	    walker2.setGraph(graph);
	    if (differsFromLastGraph(graph)) cache = new WeakHashMap();
	    fullQueryRanking = getWalker().search( initDist );
	    //System.out.println("fullQueryRanking has "+fullQueryRanking.size()+" nodes");
	    Distribution tmp = fullQueryRanking.copy();
	    if (filter!=null) tmp = filter.filter( graph, tmp );
	    return tmp.copyTopN(numToScore);
	}
	public Instance getInstanceFor(String subpop, GraphId id)
	{
	    Distribution idRanking = walker2.search( id );
	    //caching version of idRanking = walker2.search( id );
	    //Distribution idRanking = (Distribution)cache.get(id);
	    //if (idRanking==null) cache.put(id, (idRanking=walker2.search(id)) );
	    return dotProductInstance( subpop, id, idRanking, fullQueryRanking );
	}
	private boolean differsFromLastGraph(Graph graph)
	{
	    Graph nonCachingVersionOfThisGraph = graph;
	    while (nonCachingVersionOfThisGraph instanceof CachingGraph) {
		nonCachingVersionOfThisGraph = ((CachingGraph)nonCachingVersionOfThisGraph).getInnerGraph();
	    }
	    if (nonCachingVersionOfThisGraph==nonCachingVersionOfLastGraph) {
		return false;
	    } else {
		nonCachingVersionOfLastGraph = nonCachingVersionOfThisGraph;
		return true;
	    }
	}
	// build a dot-product instance
	private Instance dotProductInstance( String subpop, GraphId xId, Distribution x, Distribution y)
	{
	    Distribution totalDist = new TreeDistribution();
	    for (Iterator i=x.iterator(); i.hasNext(); ) {
		GraphId id = (GraphId)i.next();
		double xWeight = x.getLastProbability();
		double yWeight = y.getWeight(id);
		double w = xWeight*yWeight;
		if (w>0) {
		    Distribution xInstanceDist = ((FEWalker)getWalker()).getInstanceDistribution(id);
		    Distribution yInstanceDist = walker2.getInstanceDistribution(id);
		    totalDist.addAll( w/2, mapFeatures("forward",xInstanceDist) );
		    totalDist.addAll( w/2, mapFeatures("backward",yInstanceDist) );
		}
	    }
	    return new DistributionInstance( xId, subpop, totalDist );
	}
	// add a prefix in front of every feature in a distribution
	private Distribution mapFeatures(String prefix,Distribution d)
	{
	    Distribution result = new TreeDistribution();
	    for (Iterator i=d.iterator(); i.hasNext(); ) {
		Feature f = (Feature)i.next();
		double wf = d.getLastProbability();
		String[] parts = f.getName();
		String[] newParts = new String[parts.length+1];
		newParts[0] = prefix;
		for (int j=0; j<parts.length; j++) {
		    newParts[j+1] = parts[j];
		}
		result.add( wf, new Feature(newParts) );
	    }
	    return result;
	}
    }
}
