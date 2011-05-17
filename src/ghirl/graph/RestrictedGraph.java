package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** A graph with certain edges/nodes removed */

public class RestrictedGraph implements Graph
{
    private NodeTest nodeTest;
    private EdgeTest edgeTest;
    private Graph innerGraph;

    //
    // how to build restrictions
    //
    
    /** A test to apply to nodes in the graph. */
    public interface NodeTest {
	public boolean accept(GraphId id);
    }
    /** A test to apply to edges in the graph. */
    public interface EdgeTest {
	public boolean accept(String edgeLabel,GraphId from,GraphId to);
    }
    
    /** A primitive test to apply to nodes. 
     */
    static public class BasicNodeTest implements NodeTest
    {
	private NodeFilter filter;
	private Graph graph;
	public BasicNodeTest(Graph graph,NodeFilter filter) { 
	    this.graph = graph;
	    this.filter=filter; 
	}
	public boolean accept(GraphId id) 
	{
	    return filter.accept(graph,id);
	}
    }

    public NodeTest makeNodeTest(String nodeFilterSpec)
    {
	return new BasicNodeTest(innerGraph,new NodeFilter(nodeFilterSpec));
    }

    /** A primitive test to apply to edges.  An edge is accepted if
     * its label matches the edgeLabel string, and the from and to
     * nodes match their associated filters.  Null filters or
     * edgeLabel's in an EdgeTest match anything.
     */
    static public class BasicEdgeTest implements EdgeTest {
	public String edgeLabel;
	public NodeTest fromTest, toTest;
	public BasicEdgeTest(String edgeLabel,NodeTest fromTest,NodeTest toTest) {
	    this.edgeLabel=edgeLabel;
	    this.fromTest=fromTest;
	    this.toTest=toTest;
	}
	public boolean accept(String label,GraphId from,GraphId to)
	{
	    return 
		(edgeLabel==null || edgeLabel.equals(label))
		&& (fromTest==null || fromTest.accept(from))
		&& (toTest==null || toTest.accept(to));
	}
    }
    
    /** Negate a NodeTest. */
    static public class InvertNodeTest implements NodeTest {
	private NodeTest test;
	public InvertNodeTest(NodeTest test) { this.test=test; }
	public boolean accept(GraphId id) { return !test.accept(id); }
    }

    /** Union of many NodeTests */
    static public class OrNodeTest implements NodeTest {
	private NodeTest[] tests;
	public OrNodeTest(NodeTest[] tests) { this.tests=tests; }
	public boolean accept(GraphId id) { 
	    for (int i=0; i<tests.length; i++) 
		if (tests[i].accept(id)) return true;
	    return false;
	}
    }

    /** Intersection of many NodeTests */
    static public class AndNodeTest implements NodeTest {
	private NodeTest[] tests;
	public AndNodeTest(NodeTest[] tests) { this.tests=tests; }
	public boolean accept(GraphId id) { 
	    for (int i=0; i<tests.length; i++) 
		if (!tests[i].accept(id)) return false;
	    return true;
	}
    }

    /** Negate a EdgeTest. */
    static public class InvertEdgeTest implements EdgeTest {
	private EdgeTest test;
	public InvertEdgeTest(EdgeTest test) { this.test=test; }
	public boolean accept(String edgeLabel,GraphId from,GraphId to) { 
	    return !test.accept(edgeLabel,from,to); 
	}
    }

    /** Union of many EdgeTests */
    static public class OrEdgeTest implements EdgeTest {
	private EdgeTest[] tests;
	public OrEdgeTest(EdgeTest[] tests) { this.tests=tests; }
	public boolean accept(String edgeLabel,GraphId from,GraphId to) { 
	    for (int i=0; i<tests.length; i++) {
		if (tests[i].accept(edgeLabel,from,to)) return true;
	    }
	    return false;
	}
    }

    /** Intersection of many EdgeTests */
    static public class AndEdgeTest implements EdgeTest {
	private EdgeTest[] tests;
	public AndEdgeTest(EdgeTest[] tests) { this.tests=tests; }
	public boolean accept(String edgeLabel,GraphId from,GraphId to) { 
	    for (int i=0; i<tests.length; i++) {
		if (!tests[i].accept(edgeLabel,from,to)) return false;
	    }
	    return true;
	}
    }

    //
    // constructors
    //

    public RestrictedGraph(Graph innerGraph,NodeTest nodeTest,EdgeTest edgeTest)
    {
	this.innerGraph = innerGraph;
	this.nodeTest = nodeTest;
	this.edgeTest = edgeTest;
    }
    public RestrictedGraph(Graph innerGraph)
    {
	this(innerGraph,null,null);
    }
    public void setNodeTest(NodeTest nodeTest)
    {
	this.nodeTest = nodeTest;
    }
    public void setEdgeTest(EdgeTest edgeTest)
    {
	this.edgeTest = edgeTest;
    }

    //
    // definitions
    //

    public boolean contains(GraphId id)
    {
	return innerGraph.contains(id) && (nodeTest==null || nodeTest.accept(id));
    }

    public boolean containsEdge(String linkLabel,GraphId from,GraphId to)
    {
	return contains(from) && contains(to) && (edgeTest==null || edgeTest.accept(linkLabel,from,to));
    }

    public GraphId getNodeId(String flavor,String shortNodeName)
    {
	GraphId id = innerGraph.getNodeId(flavor,shortNodeName);
	if (id!=null && contains(id)) return id;
	else return null;
    }

    public Iterator getNodeIterator()
    {
	List accum = new ArrayList();
	for (Iterator i=innerGraph.getNodeIterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    if (contains(id)) accum.add(id);
	}
	return accum.iterator();
    }


    public Set followLink(GraphId from,String linkLabel)
    {
	if (!contains(from)) return Collections.EMPTY_SET;
	else {
	    Set result = new HashSet();
	    Set toSet = innerGraph.followLink(from,linkLabel);
	    for (Iterator i=toSet.iterator(); i.hasNext(); ) {
		GraphId to = (GraphId)i.next();
		if (containsEdge(linkLabel,from,to)) {
		    result.add(to);
		}
	    }
	    return result;
	}
    }

    public Set getEdgeLabels(GraphId from)
    {
	return innerGraph.getEdgeLabels(from);
    }

    public Distribution walk1(GraphId from)
    {
	Distribution dist = new TreeDistribution();
	for (Iterator i=getEdgeLabels(from).iterator(); i.hasNext(); ) {
	    String linkLabel = (String)i.next();
	    Distribution linkDist = walk1(from,linkLabel);
	    for (Iterator j=linkDist.iterator(); j.hasNext(); ) {
		GraphId to = (GraphId)j.next();
		double w = linkDist.getLastWeight();
		if (containsEdge(linkLabel,from,to)) {
		    dist.add( w, to );
		}
	    }
	}
	return dist;
    }

    public Distribution walk1(GraphId from,String linkLabel)
    {
	Distribution dist = innerGraph.walk1(from,linkLabel);
	for (Iterator i=dist.iterator(); i.hasNext(); ) {
	    GraphId to = (GraphId)i.next();
	    if (!containsEdge(linkLabel,from,to)) dist.remove( to );
	}
	return dist;
    }

    public String[] getOrderedEdgeLabels() { return innerGraph.getOrderedEdgeLabels(); }

    public GraphId[] getOrderedIds() { return innerGraph.getOrderedIds(); }

    public String getProperty(GraphId from,String prop)
    {
	return innerGraph.getProperty(from,prop);
    }

    public String getTextContent(GraphId id)
    {
	return innerGraph.getTextContent(id);
    }

    public Distribution asQueryDistribution(String queryString)
    {
	Distribution dist = innerGraph.asQueryDistribution(queryString);
	for (Iterator i=dist.iterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    if (!contains(id)) dist.remove( id );
	}
	return dist;
    }
}
