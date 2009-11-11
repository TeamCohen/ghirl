package ghirl.graph;

import java.io.*;
import java.util.*;

import ghirl.util.*;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.text.TextLabels;

/** A TextGraph which is defined by two "nested" TextGraph's.
 *
 * <p> Operationally, queries to the NestedTextGraph are answered as
 * if they were made to the union of the two text graphs.  Updates to
 * the graph are applied to the 'outer' TextGraph.
 *
 * <p>Pragmatically, this means that if you create a NestedTextGraph
 * from an innerGraph, it will initially look like innerGraph.  But if
 * you modify it, the innerGraph will not be changed, so you can at
 * any point easily revert to the old innerGraph.
 *
 * @author William Cohen
*/


public class NestedTextGraph implements MutableGraph,TextGraphExtensions
{
    private TextGraph outer, inner;
    public NestedTextGraph(TextGraph inner)
    {
        this.inner = inner;
        // "outer" graph is memory-resident
        this.outer = new TextGraph();
        TextGraph.mergeDocFrequencies(inner,outer);
        outer.melt();
    }
    public String toString() 
    { 
        return "[NestedTextGraph inner:"+inner+" outer: "+outer+"]"; 
    }

    public void freeze() 
    { 
        System.out.println("freezing "+this);
        outer.freeze(); 
        inner.freeze();
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

    private Distribution mergeDist(Distribution dOuter, Distribution dInner)
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

    /** Create a new node in the graph of flavor LABELs with short
     * name labelIdShortName based on the TextLabels object textLabels
     * which annotates the content of the node named by textFileIdName.
     */
    public GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,String textFileIdName)
    {
	return outer.createLabelsNode(textLabels,labelIdShortName,textFileIdName);
    }

    public Distribution textQuery(String queryString)
    {
        return mergeDist( inner.textQuery(queryString), outer.textQuery(queryString) );
    }


    public String[] getOrderedEdgeLabels() { return GraphUtil.getOrderedEdgeLabels(this); }
    public GraphId[] getOrderedIds() { return GraphUtil.getOrderedIds(this); }

    static public void main(String[] args) throws IOException
    {
        TextGraph g1 = new TextGraph(args[0]);
        g1.freeze();
        NestedTextGraph g2 = new NestedTextGraph(g1);
        GraphLoader loader = new GraphLoader(g2);
        loader.load(new File(args[1]));
	//QueryGUI gui = new QueryGUI(g2);
	//new edu.cmu.minorthird.util.gui.ViewerFrame("QueryGUI", gui );
        GraphId id = GraphId.fromString(args[2]);
        if (!g2.contains(id)) {
            System.out.println(id+" not in graph "+args[0]);
        } else {
            System.out.println(g2.walk1(id));
        }
    }
}
