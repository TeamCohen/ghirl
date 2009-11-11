package ghirl.graph;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import ghirl.util.*;

import org.apache.log4j.*;

/**
 * Emulates a lazy random walk - like pagerank, but with fixed probability of
 * stopping at any point.
 **/

public class BasicWalker extends Walker implements CommandLineProcessor.Configurable
{
	protected static Logger log = Logger.getLogger( BasicWalker.class );

	protected Distribution startDist;
	protected double probRemainAtNode = 0.5;
	protected Set nodeStopList = new HashSet();
	protected Set edgeStopList = new HashSet();
	protected int numLevels = 2;
	protected int steps = 1000;

	private Distribution nodeSample;
	private Random rand;

	public BasicWalker() { edgeStopList.add("isa"); edgeStopList.add("isaInverse"); }

	public void setInitialDistribution(Distribution dist)
	{
		this.startDist = dist;
		if (startDist.getTotalWeight()==0) {
	    throw new IllegalArgumentException("start distribution can't be empty!");
		}
		reset();
	}
	public Distribution getInitialDistribution() { return startDist; }
	public void setNumLevels(int n) { this.numLevels = n; }
	public int getNumLevels() { return numLevels; }
	public void setProbRemainAtNode(double p) { probRemainAtNode=p; }
	public double getProbRemainAtNode() { return probRemainAtNode; }
	// node version
	public void addToNodeStopList(GraphId id) { nodeStopList.add(id); }
	// edge version
	public void addToEdgeStopList(String edgeName) { edgeStopList.add(edgeName); }
	public int getNumSteps() { return steps; }
	public void setNumSteps(int n) { steps=n; }
    public Set getEdgeStopList() {return edgeStopList; }
    public void setSamplingPolicy(boolean sample) {
        if (!sample) System.out.println("An exhaustive walk is not supported by BasicWalker. Sampling...");
    }
    public void setStayWalkVersion(boolean stay) {
        if (stay) System.out.println("The stayWalkVersion is not supported by BasicWalker. Applying random walks with restart.");
    }

    public void setRandomEdgeWeights() { System.out.println("Random weights not supported by BasicWalker");}
    public void setUniformEdgeWeights() {;}

	public String toString()
	{
		return
	    "[BasicWalker: numLevels="+numLevels+" probRemain="+probRemainAtNode+" numSteps="+steps+"]";
	}

    public Distribution getNodeSample()
	{
	 return nodeSample;
	 }

	// allow command-line configuration
	public class MyCLP extends BasicCommandLineProcessor
	{
		public void steps(String s) { setNumSteps(StringUtil.atoi(s)); }
		public void levels(String s) { setNumLevels(StringUtil.atoi(s)); }
        public void probRemain(String s) { setProbRemainAtNode(StringUtil.atof(s)); }
		public void nodeStopList(String s) {
	    String[] ids = s.split("\\s*,\\s*");
	    for (int i=0; i<ids.length; i++) addToNodeStopList( GraphId.fromString(ids[i]) );
		}
	}
	public CommandLineProcessor getCLP() { return new MyCLP(); }

	public void reset()
	{
		nodeSample = new TreeDistribution();
		rand = new Random(0);
	}


	public void walk()
	{
		new MyBasicWalker().walk();
	}

	/** An innerclass that does the actual lazy walk.  All local state for this
	 * class is in protected variables to make it easy to subclass and modify.
	 */
	protected class MyBasicWalker
	{
		// Level (ie distance from starting distribution) currently being
		// walked away from.
		protected int currentLevel;
		// Probability of reaching the current level from the starting
		// distribution.
		protected double probReachCurrentLevel;
		// Probability distribution over nodes in the current level
		// (ie the level being walked from)
		protected Distribution currentDist;
		// Probability distribution over nodes in the level being constructed,
		// (ie the level being walked to)
		protected Distribution nextDist;
		// Node being walked from
		protected GraphId fromId;
		// Set of possible links available from fromId
		protected Set linkLabelSet;
		// Edge label for the edge being followed from fromId
		protected String linkLabel;
		// Probability of fromId in the currentDist
		protected double fromIdWeight;
		// Probability of linkLabel given fromId
		protected double linkLabelWeight;
		// Nodes reachable from fromId using linkLabel, weighting
		// by Prob(id|fromId,linkLabel)
		protected Distribution walk;

		/** Walk, level-by-level, to a maximal distance of numLevels from the initDist,
		 * weighting the nodes found at each level appropriately, according to the
		 * lazy-walk stopping parameter of probRemain.
		 */

		final public void walk()
		{
	    nodeSample = new TreeDistribution();
	    probReachCurrentLevel = 1.0;
	    currentDist = new TreeDistribution();
	    currentDist.addAll(1.0, startDist);
	    pruneStopList(currentDist);
	    doStartWalkHook();

	    ProgressCounter pc = new ProgressCounter("lazyRandomWalk","step",steps);
	    for (currentLevel=0; currentLevel<=numLevels && currentDist.size()>0; currentLevel++) {

				if (log.isInfoEnabled()) log.info("level "+currentLevel+" of "+numLevels);
				doStartLevelHook();

				// update nodeSample for cases where we stop here
				nodeSample.addAll( probReachCurrentLevel*probRemainAtNode, currentDist );

				if (currentLevel<numLevels) {
					// there will be transitions from this level to the next
					nextDist = new TreeDistribution();

					// sample m nodes from the current distribution
					int m = (int)Math.round(steps/numLevels + 0.5);
					Distribution subsample = new TreeDistribution();
					for (int i=0; i<m; i++) {
						Object rId = currentDist.sample(rand);
						if (log.isInfoEnabled()) log.info(" sample "+(i+1)+"/"+m+" is "+rId);
						subsample.add( currentDist.getWeight(rId), rId );
					}

					// randomly walk away from the subsample
					for (Iterator i=subsample.iterator(); i.hasNext(); ) {
						fromId = (GraphId)i.next();
						if (fromId==null) {
							System.err.println("warning - null fromId??");
							continue;
						}
						fromIdWeight = currentDist.getProbability(fromId);
						linkLabelSet = graph.getEdgeLabels(fromId);
						pruneStopList( linkLabelSet );
						for (Iterator j=linkLabelSet.iterator(); j.hasNext(); ) {
							linkLabel = (String)j.next();
							linkLabelWeight = doComputeWeight();
							walk = graph.walk1(fromId,linkLabel);
							pruneStopList(walk);
							doWalkHook();
							nextDist.addAll( fromIdWeight*linkLabelWeight, walk );
						}
						pc.progress();
					}

					doEndLevelHook();

					currentDist = nextDist;
					probReachCurrentLevel *= (1-probRemainAtNode);

				} // if there will be a next level
	    }
	    doEndWalkHook();
	    pc.finished();
		}

		private void pruneStopList(Set edges)
		{
	    for (Iterator i=edges.iterator(); i.hasNext(); ) {
				String linkLabel = (String)i.next();
				if (edgeStopList.contains(linkLabel)) {
					i.remove();
				}
	    }
		}

		private void pruneStopList(Distribution d)
		{
	    for (Iterator i=nodeStopList.iterator(); i.hasNext(); ) {
				Object obj = i.next();
				d.remove(obj);
				if (log.isDebugEnabled()) log.debug("removed "+obj);
	    }
		}


		/** Called when starting a walk.
		 */
		/** Called when starting a walk */
		public void doStartWalkHook()
		{
	    ; // do nothing - just a hook for subclasses to use
		}

		/** Called when finishing a walk */
		public void doEndWalkHook()
		{
	    ; // do nothing - just a hook for subclasses to use
		}


		/** Called after starting a new level. */
		public void doStartLevelHook()
		{
	    ; // do nothing - just a hook for subclasses to use
		}

		/** Called after completing a level */
		public void doEndLevelHook()
		{
	    ; // do nothing - just a hook for subclasses to use
		}

		/** Called after walking in graph from id along a link labeled linkLabel,
		 * resulting in the distribibution 'walk'.
		 */
		public void doWalkHook()
		{
	    ; // do nothing - just a hook for subclasses to use
		}

		/** Return the probability of traversing a link with the given
		 * label from the node 'id'. This default implementation assigns
		 * equal probability to all labels.
		 */
		public double doComputeWeight()
		{
	    return 1.0/linkLabelSet.size();
		}
	} // class MyWalker
}
