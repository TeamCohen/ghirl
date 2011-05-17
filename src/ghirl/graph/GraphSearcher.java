package ghirl.graph;

import java.util.*;
import ghirl.util.Distribution;

/** 
 * A GraphSearcher embodies a similarity metric over GraphIds.  The
 * basic operation is: given a GraphId, return ranked set of similar
 * GraphId's.  The ranked set is encoded as a distribution over
 * GraphId's.
 */

public interface GraphSearcher
{
    /** Search for nodes similar to the id. */
    public Distribution search(GraphId id);

    /** Search for nodes similar to one or all of the nodes in the
     * distribution. */
    public Distribution search(Distribution queryDistribution);

    /** Search for nodes similar to the id that match the filter. */
    public Distribution search(GraphId id, NodeFilter filter);

    /** Search for nodes that match the filter which are similar to
     * one or all of the nodes in the distribution. */
    public Distribution search(Distribution queryDistribution, NodeFilter filter);

    /** Define the graph to be searched. */
    public void setGraph(Graph graph);
    //public Graph getGraph();
}
