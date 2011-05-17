package ghirl.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import edu.cmu.minorthird.util.UnionIterator;
import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

public class NestedGraph implements MutableGraph {
	    private MutableGraph outer;
	    private Graph inner;
	    
	    public NestedGraph(Graph inner) {
	    	this(inner,new BasicGraph());
	    }
	    public NestedGraph(Graph inner, MutableGraph outer) {
	        this.inner = inner;
	        // "outer" graph is memory-resident
	        this.outer = new BasicGraph();
	        outer.melt();
	    }
	    public String toString() 
	    { 
	        return "[NestedGraph inner:"+inner+" outer: "+outer+"]"; 
	    }
	    public void close() {
	    	if (inner instanceof Closable) ((Closable) this.inner).close();
	    }

	    public void freeze() 
	    { 
	        System.out.println("freezing "+this);
	        outer.freeze(); 
	        if (inner instanceof MutableGraph) ((MutableGraph)inner).freeze();
	    }
	    public void melt() 
	    { 
	        System.out.println("melting "+this+" => melting "+outer);
	        outer.melt(); 
	    }

	    public GraphId createNode(String flavor,String shortName,Object obj)
	    {
	        GraphId id = inner.getNodeId(flavor,shortName);
	        if (id!=null) return id;
	        else {
	            return outer.createNode(flavor,shortName,obj);
	        }
	    }

	    public GraphId createNode(String flavor,String shortName)
	    {
	        return createNode(flavor,shortName,null);
	    }

	    public boolean contains(GraphId id) 
	    { 
	        return outer.contains(id) || inner.contains(id);
	    }

	    public GraphId getNodeId(String flavor,String shortName)
	    {
	        GraphId id = outer.getNodeId(flavor,shortName);
	        if (id!=null) return id;
	        return inner.getNodeId(flavor,shortName);
	    } 

	    public Iterator getNodeIterator() 
	    { 
	        return new UnionIterator( outer.getNodeIterator(), inner.getNodeIterator() );
	    }

	    public String getProperty(GraphId id,String prop)
	    {
	        String value = outer.getProperty(id,prop);
	        if (value!=null) return value;
	        else return inner.getProperty(id,prop);
	    }

	    public void setProperty(GraphId id,String prop,String val)
	    {
	        outer.setProperty(id,prop,val);
	    }

	    public String getTextContent(GraphId id) 
	    {
		String content = outer.getTextContent(id);
	        if (content!=null) return content;
	        else return inner.getTextContent(id);
	    }

	    public void addEdge(String linkLabel,GraphId from,GraphId to)
	    {
	        outer.addEdge(linkLabel,from,to);
	    }

	    public Set followLink(GraphId from,String linkLabel) 
	    {
	        return mergeSet( outer.followLink(from,linkLabel),  inner.followLink(from,linkLabel) );
	    }

	    public Set getEdgeLabels(GraphId from)
	    {
	        return mergeSet( outer.getEdgeLabels(from),  inner.getEdgeLabels(from) );
	    }

	    public Set mergeSet(Set sOuter,Set sInner)
	    {
	        Set result = new HashSet();
	        result.addAll( sOuter );
	        result.addAll( sInner );
	        return result;
	    }
	 
	    public Distribution asQueryDistribution(String queryString)
	    {
	        return CommandLineUtil.parseNodeOrNodeSet(queryString,this);
	    }

	    public Distribution walk1(GraphId from)
	    {
	        return mergeDist( outer.walk1(from), inner.walk1(from) );
	    }

	    public Distribution walk1(GraphId from,String linkLabel)
	    {
	        return mergeDist( outer.walk1(from,linkLabel), inner.walk1(from,linkLabel) );
	    }

	    protected Distribution mergeDist(Distribution dOuter, Distribution dInner)
	    {
	        Distribution result = new TreeDistribution();
	        for (Iterator i=dInner.iterator(); i.hasNext(); ) {
	            Object o = (Object)i.next();
	            double w = dInner.getLastWeight();
	            result.add(w, o);
	        }
	        for (Iterator i=dOuter.iterator(); i.hasNext(); ) {
	            Object o = (Object)i.next();
	            double w = dOuter.getLastWeight();
	            result.add(w, o);
	        }
	        return result;
	    }

	    public String[] getOrderedEdgeLabels() { return GraphUtil.getOrderedEdgeLabels(this); }
	    public GraphId[] getOrderedIds() { return GraphUtil.getOrderedIds(this); }

}
