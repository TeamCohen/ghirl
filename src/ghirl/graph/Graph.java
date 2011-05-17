package ghirl.graph;

import java.util.*;
import ghirl.util.Distribution;

/** A graph structure. */

public interface Graph extends Walkable
{
    /** Check for a vertex. */
    public boolean contains(GraphId id);

    /** Get the id in the graph corresponding to a flavor and name.
     * Returns null if the graph doesn't contain this node. */
    public GraphId getNodeId(String flavor,String shortNodeName);

    /** Get all node ids one by one. */
    public Iterator getNodeIterator();

    /** Return an ordered array of all GraphId's that would be
     * returned by the iterator() method. GraphUtil contains a default
     * implementation of this method.  For large persistant graphs it
     * useful to cache the information separately to reduce disk
     * access.
     */
    public GraphId[] getOrderedIds();
    
    /** Get a property of a node. */
    public String getProperty(GraphId from,String prop);
}
