package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** A graph structure that can be 'annotated' by adding new edge types
 * defined by GraphSearchers.
 */

public class AnnotatableGraph implements Graph
{
    private Graph innerGraph;
    private Map annotatorMap = new HashMap();
    private boolean allowMoreAnnotations = true;

    public AnnotatableGraph(Graph innerGraph) { this.innerGraph=innerGraph; }

    public boolean contains(GraphId id) { return innerGraph.contains(id); }

    public GraphId getNodeId(String flavor,String shortNodeName) { return innerGraph.getNodeId(flavor,shortNodeName); }

    public Iterator getNodeIterator() { return innerGraph.getNodeIterator(); }

    /** Insert a new annotator */
    public void addAnnotator(GraphAnnotator annotator)
    {
	annotatorMap.put(annotator.getLinkLabel(),annotator);
    }

    public Set followLink(GraphId from,String linkLabel)
    {
	GraphAnnotator annotator = (GraphAnnotator)annotatorMap.get(linkLabel);
	if (annotator==null) return innerGraph.followLink(from,linkLabel);
	else {
	    Distribution dist = annotator.walk1(innerGraph,from);
	    Set result = new HashSet();
	    for (Iterator i=dist.iterator(); i.hasNext(); ) {
		result.add( i.next() );
	    }
	    return result;
	}
    }

    public Set getEdgeLabels(GraphId from)
    {
	Set result = new HashSet();
	result.addAll( innerGraph.getEdgeLabels(from) );
	for (Iterator i=annotatorMap.keySet().iterator(); i.hasNext(); ) {
	    String linkLabel = (String)i.next();
	    GraphAnnotator annotator = (GraphAnnotator) annotatorMap.get(linkLabel);
	    if (annotator.getPrecondition().accept(innerGraph,from)) result.add(linkLabel);
	}
	return result;
    }

    public Distribution walk1(GraphId from)
    {
	Distribution dist = new TreeDistribution();
	for (Iterator i=getEdgeLabels(from).iterator(); i.hasNext(); ) {
	    String linkLabel = (String)i.next();
	    for (Iterator j=followLink(from,linkLabel).iterator(); j.hasNext(); ) {
		GraphId id = (GraphId)j.next();
		dist.add( 1.0, id );
	    }
	}
	return dist;
    }

    public Distribution walk1(GraphId from,String linkLabel)
    {
	GraphAnnotator annotator = (GraphAnnotator)annotatorMap.get(linkLabel);
	if (annotator==null) return innerGraph.walk1(from,linkLabel);
	else return annotator.walk1(innerGraph,from);
    }

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
	return innerGraph.asQueryDistribution(queryString);
    }

    public String[] getOrderedEdgeLabels() 
    { 
	TreeSet set = new TreeSet();
	set.addAll( annotatorMap.keySet() );
	String[] innerEdges = innerGraph.getOrderedEdgeLabels();
	for (int j=0; j<innerEdges.length; j++) {
	    set.add(innerEdges[j]);
	}
	return (String[]) set.toArray(new String[set.size()]);
    }

    public GraphId[] getOrderedIds() { return innerGraph.getOrderedIds(); }

    public static Graph setAllowMoreAnnotations(Graph graph,boolean flag) 
    { 
	if (graph instanceof AnnotatableGraph) {
	    ((AnnotatableGraph) graph).allowMoreAnnotations = flag;
	    return graph;
	} else {
            return graph;
        } 
    }

    /**
     * Return a version of the graph that includes the annotator.  If
     * necessary, this will create a new AnnotatableGraph enclosing
     * the graph that was passed in.
     */
    public static Graph addAnnotator(Graph graph,GraphAnnotator annotator)
    {
	if ((graph instanceof AnnotatableGraph) && (((AnnotatableGraph)graph).allowMoreAnnotations)) {
	    ((AnnotatableGraph) graph).addAnnotator(annotator);
	    return graph;
	} else {
	    AnnotatableGraph result = new AnnotatableGraph(graph);
	    result.addAnnotator(annotator);
	    return result;
	}
    }
}
