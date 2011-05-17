package ghirl.graph;

/** A graph structure that can be extended. */
public interface MutableGraph extends Graph
{
    /** Create a new node that lives in this Graph. */
    public GraphId createNode(String flavor,String shortName);

    /** Create a new node that lives in this Graph. */
    public GraphId createNode(String flavor,String shortName,Object content);

    /** Add an edge to the graph. */
    public void addEdge(String label,GraphId from,GraphId to);

    /** Set a property of a node. */
    public void setProperty(GraphId from,String prop,String val);

    /** Freeze the graph to no more write operations are possible
        until a 'melt()' operation is called.  This also syncs the
        graph to disk, if necessary.  */
    public void freeze();

    /** Reverse a 'freeze' operation.  */
    public void melt();

}
