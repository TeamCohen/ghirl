package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** Provides default implementations of all the search variants in
 * terms of the abstract method search(Distribution queryDistribution).
 */

abstract public class AbstractGraphSearcher implements GraphSearcher
{
    protected Graph graph;

    public void setGraph(Graph graph) 
    { 
        this.graph = graph;
    }

    public Graph getGraph(){
        return this.graph;
    }

    public Distribution search(GraphId id)
    {
        TreeDistribution dist = new TreeDistribution(id);
        return search(dist);
    }
    public Distribution search(GraphId id, NodeFilter nodeFilter)
    {
        Distribution ranking = search(id);
        if (nodeFilter==null || ranking==null) return ranking;
        else return nodeFilter.filter(graph,ranking);
    }
    public Distribution search(Distribution queryDistribution,NodeFilter nodeFilter)
    {
        Distribution ranking = search(queryDistribution);
        if (nodeFilter==null || ranking==null) return ranking;
        else return nodeFilter.filter(graph,ranking);
    }
    abstract public Distribution search(Distribution queryDistribution);
}
