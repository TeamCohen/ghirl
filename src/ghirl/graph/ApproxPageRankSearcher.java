package ghirl.graph;

import java.util.Iterator;

import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

public class ApproxPageRankSearcher implements GraphSearcher {
    double epsilon;
    double alpha;
    Graph graph;

    public ApproxPageRankSearcher() {}
    public ApproxPageRankSearcher(double alpha, double epsilon, Graph graph) {
        this.setAlpha(alpha);
        this.setEpsilon(epsilon);
        this.setGraph(graph);
    }
    
    public void setEpsilon(double e) { epsilon = e; }
    public void setAlpha(double a)   { alpha   = a; }

    /** Search for nodes similar to the id. */
    public Distribution search(GraphId id) {
        Distribution pageRank = apr(alpha,epsilon,new TreeDistribution(id),null);
        return pageRank; //lowConductanceSubgraph(pageRank, id);
    }
    
    public Distribution search(GraphId id, Distribution residual) {
        Distribution seed = new TreeDistribution(id);
        residual.addAll(1.0, seed);
        Distribution pageRank = apr(alpha,epsilon,seed,residual);
        return pageRank;
    }

    private Distribution apr(double alpha, double epsilon, Distribution seed, Distribution residual) {
        if (residual == null) residual = seed.copy();
        Distribution p = new TreeDistribution();

        int nhits = 0;
        int niters = 0;
        for (;niters == 0 || nhits > 0; niters++) {
            nhits=0;
            for(Iterator it = graph.getNodeIterator(); it.hasNext();) { 
                GraphId u = (GraphId) it.next();
                if (residual.getWeight(u) / graph.walk1(u).size() > epsilon) {
                    nhits++;
                    push(alpha,u,p,residual);
                }
            }
        }
        return p;
    }

    private void push(double alpha, GraphId u, Distribution p, Distribution residual) {
        // NB: getWeight() is not available for some distribution types
        double ru = residual.getWeight(u);
        p.add(alpha * ru, u);

        // adjust weights for the 0.5*D^{-1}A portion of W
        Distribution uNeighbors = graph.walk1(u);
        double wn = uNeighbors.size() > 0 ? 1.0/uNeighbors.size() : 0;
        for(Iterator it = uNeighbors.iterator(); it.hasNext();) {
            GraphId neighbor = (GraphId) it.next();
            double rn = residual.getWeight(neighbor);
            residual.remove(neighbor);
            residual.add(rn + (1-alpha)*ru*wn*0.5, neighbor);
        }

        // adjust weights for the 0.5*I portion of W
        residual.remove(u);
        residual.add((1-alpha)*ru*0.5,u);
    }

    /* This is problematic ***********/
    /*
    private Distribution lowConductanceSubgraph(Distribution pr, GraphId v0) {
        Distribution S = new TreeDistribution(v0);
        Distribution star = S.copy();
        double vol_S    = graph.walk1(v0).size();
        double vol_star = vol_S;
        double boundary_S    = vol_S;
        double boundary_star = boundary_S;
        for(Iterator it = pr.orderedIterator(); it.hasNext();) {
            GraphId u = (GraphId) it.next();
            S.add(1.0,u);
            vol_S += graph.walk1(u).size();
            boundary_S -= 0;//#edges from any node in S to u -- no way to get this
            boundary_S += 0;//#edges from u to any node not in S
            if (boundary_S / vol_S > boundary_star / vol_star) {
                star.add(1.0,u); // wait, is this right?
                vol_star = 0;
                boundary_star = 0;
            } else {
                //what happens if S doesn't win?
            }
        }
        return star;
    }
    */

    /************ Other stuff required by GraphSearcher interface ****************/

    /** Search for nodes similar to one or all of the nodes in the
     * distribution. */
    public Distribution search(Distribution queryDistribution) {return null;}

    /** Search for nodes similar to the id that match the filter. */
    public Distribution search(GraphId id, NodeFilter filter) {return null;}

    /** Search for nodes that match the filter which are similar to
     * one or all of the nodes in the distribution. */
    public Distribution search(Distribution queryDistribution, NodeFilter filter) {return null;}

    /** Define the graph to be searched. */
    public void setGraph(Graph graph) {
        this.graph = graph;
    }
}