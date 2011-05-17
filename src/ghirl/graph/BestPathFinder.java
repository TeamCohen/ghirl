package ghirl.graph;

import ghirl.util.*;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;
import java.util.*;

/**
 * Finds best path in a graph from a start node to a set of end points.
 */

public class BestPathFinder 
{
    private final AStarState NULL_STATE = new AStarState(null,null,0);

    private WeakHashMap cache = new WeakHashMap();
    private int maxNodesExplored = 100;
    private Walkable graph;
    private Distribution targetNodes;

    public BestPathFinder(Walkable graph, Distribution targetNodes)
    {
	this(graph,targetNodes,100);
    }

    public BestPathFinder(Walkable graph, Distribution targetNodes, int maxNodesExplored)
    { 
	this.graph=graph; this.targetNodes=targetNodes;
	this.maxNodesExplored=maxNodesExplored; 
    }

    public GraphId[] bestPath(GraphId start)
    {
	return bestPathState(start).path;
    }
    
    public String[] bestEdgeLabelPath(GraphId start)
    {
	return bestPathState(start).edgeLabelsForPath;
    }

    public AStarState bestPathState(GraphId start)
    {
	AStarState best = (AStarState)cache.get(start);
	if (best==null) cache.put(start, (best=computeBestPathState(start)));
	return best;
    }

    public AStarState computeBestPathState(GraphId start)
    {
	SortedSet heap = new TreeSet();
	heap.add( new AStarState(new GraphId[]{start}, new String[]{"_start_"}, 0) );
	ProgressCounter pc = new ProgressCounter("finding connecting path","step",maxNodesExplored);
	int n=0;
	//System.out.println("searching from "+start+" to "+targetNodes);
	while (heap.size()>0) {
	    AStarState best = (AStarState)heap.first();
	    heap.remove( best );
	    //System.out.println("best ="+ best);
	    if (best.isGoalState()) return best;
	    else if (n++ > maxNodesExplored) return NULL_STATE;
	    else {
		heap.addAll(best.expand());
	    }
	    pc.progress();
	}
	pc.finished();
	return null;
    }

    private class AStarState implements Comparable 
    {  
	double cost; 
	GraphId[] path; 
	String[] edgeLabelsForPath; 
	boolean isGoal;
	GraphId endPoint;

	public AStarState(GraphId[] p,String[] edgeLabelsForPath,double c) {
	    this.cost=c; this.path=p; this.edgeLabelsForPath=edgeLabelsForPath;
	    if (p==null) return;
	    endPoint = p[p.length-1];
	    this.isGoal = false;
	    double w = targetNodes.getProbability(endPoint);
	    if (w>0) {
		cost -= Math.log(w);
		isGoal = true;
	    }
	}
	public boolean isGoalState() { 
	    return isGoal; 
	}
	public int compareTo(Object o) {
	    AStarState b = (AStarState)o;
	    double d = cost - b.cost;
	    if (d!=0) return d>0 ? +1 : -1;
	    else return path.length - b.path.length;
	}
	public String toString() { 
	    return "state("+cost+","+StringUtil.toString(path)+(isGoal?"*":"");
	}
	public Set expand() 
	{
	    //System.out.println("expanding "+this);
	    Set accum = new TreeSet();
	    for (Iterator i=graph.getEdgeLabels(endPoint).iterator(); i.hasNext(); ) {
		String linkLabel = (String)i.next();
		Distribution dist = graph.walk1(endPoint,linkLabel);
		for (Iterator j=dist.iterator(); j.hasNext(); ) {
		    GraphId newEndpoint = (GraphId)j.next();
		    double newEdgeCost = dist.getLastProbability();
		    GraphId[] newPath = append1(path,newEndpoint);
		    String[] newEdgeLabelsForPath = append1(edgeLabelsForPath,linkLabel);
		    AStarState child = new AStarState(newPath,newEdgeLabelsForPath,cost-Math.log(newEdgeCost));
		    //System.out.println(" edge cost "+newEdgeCost+", expanding to "+child);
		    accum.add( child );
		}
	    }
	    return accum;
	}
	public GraphId[] append1(GraphId[] path,GraphId id) {
	    GraphId[] newPath = new GraphId[path.length+1];
	    for (int j=0; j<path.length; j++) newPath[j]=path[j];
	    newPath[path.length] = id;
	    return newPath;
	}
	public String[] append1(String[] labels,String lab) {
	    String[] newLabels = new String[labels.length+1];
	    for (int j=0; j<labels.length; j++) newLabels[j]=labels[j];
	    newLabels[labels.length] = lab;
	    return newLabels;
	}
    }

}

