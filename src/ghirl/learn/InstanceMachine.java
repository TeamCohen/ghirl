package ghirl.learn;

import ghirl.graph.*;
import ghirl.util.Distribution;
import edu.cmu.minorthird.classify.*;


public interface InstanceMachine
{
    public Distribution generateCandidates(Graph graph,Distribution initDist,NodeFilter filter);

    public Instance getInstanceFor(String subpop, GraphId id);

    public void setWalker(Walker walker);
    public Walker getWalker();

    public void setNumToScore(int k);
}
