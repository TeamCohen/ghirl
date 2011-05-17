package ghirl.graph;

import java.util.*;
import ghirl.util.*;
import edu.cmu.minorthird.util.ProgressCounter;

/** Samples by walking around a graph. */
public abstract class Walker extends AbstractGraphSearcher implements GraphSearcher
{
    abstract public void setRandomEdgeWeights();
    abstract public void setUniformEdgeWeights();
    abstract public Set getEdgeStopList();

    /** Search for nodes similar to one or all of the nodes in the
     * distribution. */
    public Distribution search(Distribution queryDistribution)
    {
	setInitialDistribution(queryDistribution);
	walk();
	return getNodeSample();
    }

    /** Walk the graph to find nodes similar to the initial
     * distribution.
     */
    abstract public void walk();

    /** Assert that some node should be ignored in walks on the graph.
     */
    abstract public void addToNodeStopList(GraphId id);

    /** Assert that some edges should be ignored in walks on the graph.
     */
    abstract public void addToEdgeStopList(String edgeName);

    /** Return the current sample of nodes that were reached. */
    abstract public Distribution getNodeSample();

    /** Define the start distribution for this walk. */
    abstract public void setInitialDistribution(Distribution dist);
    abstract public Distribution getInitialDistribution();

    /** Start the walk over again from the current initial
     * distribution. */
    abstract public void reset();

    /** Define the probability that the walker will output the current node. */
    abstract public double getProbRemainAtNode();
    abstract public void setProbRemainAtNode(double d);

    /** Define the maximum distance across the graph the walk will go. */
    abstract public int getNumLevels();
    abstract public void setNumLevels(int n);

    /** Limit the amount of resources that will be used in the walk. */
    abstract public int getNumSteps();
    abstract public void setNumSteps(int n);

    /** Set samlping on/off  */
    abstract public void setSamplingPolicy(boolean sample);

    /** Set samlping on/off  */
    abstract public void setStayWalkVersion(boolean stay);

}
