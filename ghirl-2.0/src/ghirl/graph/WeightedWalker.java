package ghirl.graph;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import ghirl.util.*;

import org.apache.log4j.*;

/**
 * Emulates a lazy random walk - like pagerank, but with fixed probability of
 * stopping at any point.
 *
 * This WeightedWalker version assigns a fixed weight per edge type (including
 * the option of uniform weights) and computes an edge probability as the weight
 * of the relevant edge over the total outgoing probability from the parent node.
 *
 **/

public class WeightedWalker extends BasicWalker implements CommandLineProcessor.Configurable
{
	protected static Logger log = Logger.getLogger( WeightedWalker.class );

	private Map edgeWeights = new HashMap();
	private Distribution nodeSample;
	private Random rand;
	private boolean randomEdgeWeights = true;
	public Distribution[] historicNodeSamples;

	private boolean sample = false;
	private boolean stayWalkVersion = false;
	private String weightsSource = "";

	public WeightedWalker() {
		edgeStopList.add("isa");
		edgeStopList.add("isaInverse");
	}

	public void setGraph(Graph graph){
		this.graph = graph;
		if (edgeWeights.isEmpty()){
			if (randomEdgeWeights) setRandomEdgeWeights();
			else setUniformEdgeWeights();
		}
	}

	public Graph getGraph(){
		return graph;
	}


	/**
	 * Edge weights' operations
	 **/

	final public void setRandomEdgeWeights(){
		randomEdgeWeights = true;
		edgeWeights.clear();
		String[] edgeLabels = graph.getOrderedEdgeLabels();
		Random r = new Random(System.currentTimeMillis());
		for (int i=0; i<edgeLabels.length; i++){
			double w = r.nextDouble();
			edgeWeights.put(edgeLabels[i],new Double(w));
		}
		weightsSource="random";
	}

	final public void setUniformEdgeWeights(){
		randomEdgeWeights = false;
		edgeWeights.clear();
		String[] edgeLabels = graph.getOrderedEdgeLabels();
		for (int i=0; i<edgeLabels.length;i++)
			edgeWeights.put(edgeLabels[i],new Double(1));
		weightsSource="uniform";
	}

	final public void setWeights(Map weights){
		edgeWeights.putAll(weights);
	}

	public void readEdgeWeights(File file) throws IOException{
		Set alteredEdgeWeights = new HashSet();
		BufferedReader br = new BufferedReader(new FileReader(file));
		while (br.ready()){
			String line = br.readLine();
			if (line.length()>0){
				String[] toks = line.split(" ");
				if (!edgeWeights.containsKey(toks[0])) System.out.println("adding new edge type: " + toks[0]);
				edgeWeights.put(toks[0],new Double(toks[2]));
				alteredEdgeWeights.add(toks[0]);
			}
		}
		for (Iterator it=edgeWeights.keySet().iterator(); it.hasNext();){
			String edge = (String)it.next();
			if (!alteredEdgeWeights.contains(edge)) System.out.println(edge + " : weight not in input file.");
		}
		weightsSource=file.toString();
	}

	public void setEdgeWeight(String edge, double w){
		edgeWeights.put(edge,new Double(w));
	}

	public double getEdgeWeight(String edge){
		return ((Double)edgeWeights.get(edge)).doubleValue();
	}

	public Map getWeights(){
		return this.edgeWeights;
	}

	public String printWeights(){
		String str = new String();
		for (Iterator it=edgeWeights.keySet().iterator();it.hasNext();){
			String edge = (String)it.next();
			str = str.concat(edge + " " + edgeWeights.get(edge) + " \n");
		}
		return str;
	}

	public String edgeWeightsToString(){
		String str = new String();
		for (Iterator it=edgeWeights.keySet().iterator();it.hasNext();){
			String edgeType = (String)it.next();
			double weight = ((Double)edgeWeights.get(edgeType)).doubleValue();
			str = str.concat(edgeType+ " " + weight + "\n");
		}
		return str.trim();
	}


	final public void setInitialDistribution(Distribution dist)
	{
		this.startDist = dist;
		if (startDist.getTotalWeight()==0) {
			throw new IllegalArgumentException("start distribution can't be empty!");
		}
		reset();
	}
	final public Distribution getInitialDistribution() { return startDist; }
	final public void setNumLevels(int n) { this.numLevels = n; }
	final public int getNumLevels() { return numLevels; }
	final public void setProbRemainAtNode(double p) { probRemainAtNode=p; }
	final public double getProbRemainAtNode() { return probRemainAtNode; }
	// node version
	public void addToNodeStopList(GraphId id) { nodeStopList.add(id); }
	// edge version
	public void addToEdgeStopList(String edgeName) { edgeStopList.add(edgeName); }
	final public int getNumSteps() { return steps; }
	final public void setNumSteps(int n) { steps=n; }
	final public void setSamplingPolicy(boolean sample) { this.sample = sample ; }
	// Set an experimental type of walk
	final public void setStayWalkVersion(boolean b) { this.stayWalkVersion = b ; }

	public String toString()
	{
		return
		"[WeightedWalker: numLevels="+numLevels+" probRemain="+probRemainAtNode+" numSteps="+steps+
		" weights-source=" + weightsSource + " sampling="+sample+" stay-version="+stayWalkVersion+"]";
	}

	public Distribution getNodeSample()
	{
		return nodeSample;
	}

	public double getWeightNodeLevel(GraphId id, int level)
	{
		return historicNodeSamples[level].getWeight(id);
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
		if (stayWalkVersion) new MyWeightedWalker().walkStayVersion(false);
		else new MyWeightedWalker().walk(sample);
	}

	public double getOutgoingWeight(GraphId fromId){
		double outgoingWeight = 0;
		Set linkLabelSet = graph.getEdgeLabels(fromId);
		pruneStopList( linkLabelSet );
		for (Iterator j=linkLabelSet.iterator(); j.hasNext(); ){
			String linkLabel = (String)j.next();
			int count = graph.walk1(fromId,linkLabel).size();
			double edgeWeight = getEdgeWeight(linkLabel);
			//System.out.println(fromId + " " + linkLabel + " " + count);
			outgoingWeight += edgeWeight*count;
		}
		return outgoingWeight;
	}


	/** An innerclass that does the actual lazy walk.  All local state for this
	 * class is in protected variables to make it easy to subclass and modify.
	 */
	protected class MyWeightedWalker
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
		final public void walk(boolean sample)
		{
			nodeSample = new TreeDistribution();
			historicNodeSamples = new TreeDistribution[numLevels+1];
			probReachCurrentLevel = 1.0;
			currentDist = new TreeDistribution();
			currentDist.addAll(1.0, startDist);
			pruneStopList(currentDist);
			doStartWalkHook();

			ProgressCounter pc = null;
			if (sample){pc = new ProgressCounter("lazyRandomWalk","step",steps); }
			for (currentLevel=0; currentLevel<=numLevels && currentDist.size()>0; currentLevel++) {

				if (log.isInfoEnabled()) log.info("level "+currentLevel+" of "+numLevels);
				doStartLevelHook();

				// update nodeSample for cases where we stop here
				nodeSample.addAll( probReachCurrentLevel*probRemainAtNode, currentDist );
				//System.out.println("currentDist " + currentDist.toString());
				//System.out.println("node sample " + nodeSample.toString());

				historicNodeSamples[currentLevel] = new TreeDistribution();
				historicNodeSamples[currentLevel].addAll( probReachCurrentLevel*(1-probRemainAtNode), currentDist);

				if (currentLevel>0) log.info("Expanded level " + currentLevel + ", " + nodeSample.size() + " nodes");

				if (currentLevel<numLevels) {
					// there will be transitions from this level to the next
					nextDist = new TreeDistribution();
					//historicNodeSamples[currentLevel]=currentDist.copy();

					Distribution subsample = new TreeDistribution();

					// sample m nodes from the current distribution
					if (sample){
						int m = (int)Math.round(steps/numLevels + 0.5);
						for (int i=0; i<m; i++) {
							Object rId = currentDist.sample(rand);
							if (log.isInfoEnabled()) log.info(" sample "+(i+1)+"/"+m+" is "+rId);
							subsample.add( currentDist.getWeight(rId), rId );
						}
					} else {
						subsample = currentDist;
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
						double outgoingWeight = getOutgoingWeight(fromId);
						for (Iterator j=linkLabelSet.iterator(); j.hasNext(); ) {
							linkLabel = (String)j.next();
							double edgeWeight = ((Double)edgeWeights.get(linkLabel)).doubleValue();
							linkLabelWeight = edgeWeight / outgoingWeight;
							walk = graph.walk1(fromId,linkLabel);
							pruneStopList(walk);
							doWalkHook();
							nextDist.addAll( fromIdWeight*linkLabelWeight, walk );
						}
						if (pc!=null) pc.progress();
					}

					doEndLevelHook();

					currentDist = nextDist;
					probReachCurrentLevel *= (1-probRemainAtNode);

				} // if there will be a next level
			}
			doEndWalkHook();
			if (pc!=null) pc.finished();
		}


		/** In this version of walk, rather than emitting probabilities, the stay prob. flows
		 * in a self-cycle to the node, and the reminder mass is propagated to neighbors.
		 */
		final public void walkStayVersion(boolean sample)
		{
			nodeSample = new TreeDistribution();
			currentDist = new TreeDistribution();
			currentDist.addAll(1.0, startDist);
			pruneStopList(currentDist);
			doStartWalkHook();

			ProgressCounter pc = null;
			if (sample){pc = new ProgressCounter("lazyRandomWalk","step",steps); }
			for (currentLevel=0; currentLevel<numLevels && currentDist.size()>0; currentLevel++) {

				if (log.isInfoEnabled()) log.info("level "+currentLevel+" of "+numLevels);
				doStartLevelHook();

				// there will be transitions from this level to the next
				nextDist = new TreeDistribution();
				//historicNodeSamples[currentLevel]=currentDist.copy();

				// walk away from the current distribution
				for (Iterator i=currentDist.iterator(); i.hasNext(); ) {
					fromId = (GraphId)i.next();
					if (fromId==null) {
						System.err.println("warning - null fromId??");
						continue;
					}
					fromIdWeight = currentDist.getWeight(fromId);
					linkLabelSet = graph.getEdgeLabels(fromId);
					pruneStopList( linkLabelSet );
					double outgoingWeight = getOutgoingWeight(fromId);
					for (Iterator j=linkLabelSet.iterator(); j.hasNext(); ) {
						linkLabel = (String)j.next();
						double edgeWeight = ((Double)edgeWeights.get(linkLabel)).doubleValue();
						linkLabelWeight = edgeWeight / outgoingWeight;
						walk = graph.walk1(fromId,linkLabel);
						linkLabelWeight = linkLabelWeight*walk.size();
						pruneStopList(walk);
						doWalkHook();

						nextDist.addAll((1-probRemainAtNode)*fromIdWeight*linkLabelWeight, walk );
					}

					nextDist.add( probRemainAtNode*fromIdWeight, fromId);
					if (pc!=null) pc.progress();
				}
				doEndLevelHook();

				System.out.println("Expanded level " + (currentLevel+1) + ", " +  nextDist.size() + " nodes");
				currentDist = nextDist;
			}
			// output the result
			nodeSample = currentDist;

			doEndWalkHook();
			if (pc!=null) pc.finished();
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

	} // class MyWalker


	public void pruneStopList(Set edges)
	{
		for (Iterator i=edges.iterator(); i.hasNext(); ) {
			String linkLabel = (String)i.next();
			if (edgeStopList.contains(linkLabel)) {
				i.remove();
			}
		}
	}


	public void pruneStopList(Distribution d)
	{
		for (Iterator i=nodeStopList.iterator(); i.hasNext(); ) {
			Object obj = i.next();
			d.remove(obj);
			if (log.isDebugEnabled()) log.debug("removed "+obj);
		}
	}
}
