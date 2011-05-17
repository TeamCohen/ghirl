package ghirl.graph;

import ghirl.util.Distribution;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/** A graph structure that includes a fixed-size, in-memory cache. 
 */

public class CachingGraphV1 implements Graph
{
    static private int CACHE_MAX_SIZE = 20000;

    private Graph innerGraph;
    private LRUMap containsCache = new LRUMap();
    private LRUMap followCache = new LRUMap();
    private LRUMap followNumCache = new LRUMap();
    private LRUMap edgeLabelCache = new LRUMap();
    private LRUMap walkCache = new LRUMap();
    private LRUMap walkNumCache = new LRUMap();
    private LRUMap walkLinkCache = new LRUMap();
    private LRUMap propCache = new LRUMap();
    private int numMisses,numAccesses;

    public CachingGraphV1(Graph innerGraph) 
    { 
	this.innerGraph=innerGraph; 
    }

    public Graph getInnerGraph() { return innerGraph; } 

    // report statistics
    public void startCounting() { numAccesses = numMisses = 0; }
    public double hitRate() { return numAccesses==0? 0 : 1.0 - ((double)numMisses)/numAccesses; }
    // clear the cache
    public void clearCache()
    {
	containsCache.clear();
	followCache.clear();
	followNumCache.clear();
	edgeLabelCache.clear();
	walkCache.clear();
	walkNumCache.clear();
	walkLinkCache.clear();
	propCache.clear();
    }

    public boolean contains(GraphId id) 
    {
	numAccesses++;
	Boolean result = (Boolean)containsCache.get(id);
	if (result==null) {
	    numMisses++;
	    result = new Boolean(innerGraph.contains(id));
	    containsCache.put(id,result);
	}
	return result.booleanValue();
    }

    public GraphId getNodeId(String flavor,String shortNodeName) 
    {
	GraphId id = new GraphId(flavor,shortNodeName);
	return contains(id) ? id : null;
    }

    public Iterator getNodeIterator() 
    {
	return innerGraph.getNodeIterator();
    }

    public String getTextContent(GraphId id) 
    {
	return innerGraph.getTextContent(id);
    }

    public Set followLink(GraphId from,String linkLabel)
    {
	String key = edgeKey(from,linkLabel);
	numAccesses++;
	Set result = (Set)followCache.get(key);
	if (result==null) {
	    numMisses++;
	    result = innerGraph.followLink(from,linkLabel);
	    followCache.put(key,result);
	}
	return result;
    }

    public Set getEdgeLabels(GraphId from)
    {
	numAccesses++;
	Set result = (Set)edgeLabelCache.get(from);
	if (result==null) {
	    numMisses++;
	    result = innerGraph.getEdgeLabels(from);
	    edgeLabelCache.put(from,result);
	}
	return result;
    }

    public Distribution walk1(GraphId from,String linkLabel)
    {
	numAccesses++;
	String key = edgeKey(from,linkLabel);
	Distribution result = (Distribution)walkLinkCache.get(key);
	if (result==null) {
	    numMisses++;
	    result = innerGraph.walk1(from,linkLabel);
	    walkLinkCache.put(key,result);
	}
	return result;
    }

    public Distribution walk1(GraphId from)
    {
	numAccesses++;
	Distribution result = (Distribution)walkCache.get(from);
	if (result==null) {
	    numMisses++;
	    result = innerGraph.walk1(from);
	    walkCache.put(from,result);
	}
	return result;
    }

    public String getProperty(GraphId from,String prop)
    {
	numAccesses++;
	String key = edgeKey(from,prop);
	String result = (String)propCache.get(key);
	if (result==null) {
	    numMisses++;
	    result = innerGraph.getProperty(from,prop);
	    propCache.put(key,result);
	}
	return result;
    }

    public Distribution asQueryDistribution(String queryString)
    {
	return innerGraph.asQueryDistribution(queryString);
    }

    private String edgeKey(GraphId from,String linkLabel) 
    { 
	return from.getFlavor()+":"+from.getShortName()+"#"+linkLabel; 
    }

    public String[] getOrderedEdgeLabels() { return innerGraph.getOrderedEdgeLabels(); }

    public GraphId[] getOrderedIds() { return innerGraph.getOrderedIds(); }

    final private static class LRUMap 
    {
	private HashMap oldMap = new HashMap();
	private HashMap newMap = new HashMap();
	public void clear() 
	{ 
	    newMap.clear(); 
	    oldMap.clear();
	}
	public void put(Object key,Object val)
	{
	    newMap.put(key,val);
	    if (newMap.size()>CACHE_MAX_SIZE/2) {
		System.out.println("reducing cache size from "+(newMap.size()+oldMap.size()));
		oldMap.clear();
		HashMap tmp = oldMap;
		oldMap = newMap;
		newMap = tmp;
		System.out.println("reduced cache size to "+(newMap.size()+oldMap.size()));
	    }
	}
	public Object get(Object key)
	{
	    Object obj = newMap.get(key);
	    if (obj!=null) return obj;
	    else return oldMap.get(key);
	}
    }

}
