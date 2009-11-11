package ghirl.graph;

import java.util.*;
import ghirl.util.Distribution;

/** The core operations from a graph that are necessary to do a lazy
 * walk on the graph. */

public interface Walkable
{
    /** Return an ordered array of link labels that lead out from any
     * node.  GraphUtil contains a default implementation of this
     * method which loops over all nodes.  For large graphs it useful
     * to cache the information separately.
     */
    public String[] getOrderedEdgeLabels();

    /** Return all link labels that lead out from this node. */
    public Set getEdgeLabels(GraphId from);

    /** Return Set of ids of all linked-to nodes */
    public Set followLink(GraphId from,String linkLabel);

    /** Similar to followLink, but returns a Distribution of all ids
     * originating from this node, for any label. */
    public Distribution walk1(GraphId from);

    /** Similar to followLink, but returns a Distribution of ids
     * originating from this node, for the linklabel. */
    public Distribution walk1(GraphId from,String linkLabel);

    /** Get a string corresponding to the node's "content". 
     * If there is no such thing, then the shortName of the
     * id should be returned.
     */
    public String getTextContent(GraphId id);

    /** Convert a string to a distribution over nodes, e.g. by
     * constructing a distribution of all nodes that partially match
     * the string. This is a convenience method, which is especially
     * convenient for TextGraph's to have.
     */
    public Distribution asQueryDistribution(String queryString);
}
