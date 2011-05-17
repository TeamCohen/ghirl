package ghirl.learn;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import ghirl.graph.*;
import ghirl.util.*;

import org.apache.log4j.*;

/** 
 * BasicWalker, extended to generate features based on edges.
 *
 * Specifically, the Basicwalker computes this:
 * <pre>
 * Pr(z|s) = Pr(start at s, lazy walk to z, with gamma as prob of continuing vs stopping)
 *         = (1-gamma) sum_d gamma^d Pr(s ->^{d} z)
 * Pr(s ->^{d} z) = Pr(start at s, walk to z in d steps)
 *                 = sum_y Pr(s ->^{d-1} z) * sum_a Pr(a|y) * Pr(z|y,a)
 * </pre>
 * The FEWalker computes a feature vector F as follows:
 * <pre>
 * F(z|s) = feature vector for starting at s, lazy walk to z, with gamma as prob of continuing vs stopping
 *        = (1-gamma) sum_d gamma^d F(s ->^{d} z)
 * F(s ->^{d} z) = feature vector for start at s, walk to z in d steps
 *               = sum_y Pr(s ->^{d-1} z) * sum_a Pr(a|y) * Pr(z|y,a) * f(y->^a z)
 * </pre>
 * 
 * The 'primitive' feature f, which depends only on the source node y,
 * the destination node z, and the edge label a, is determined by the LinkFeatureExtractor
 * set by linkFE.
 *
 **/

public class FEWalker extends BasicWalker implements Serializable
{
	static private final long serialVersionUID = 1;
	private final int CURRENT_VERSION_NUMBER = 1;

	private static Logger log = Logger.getLogger( FEWalker.class );

	//private LinkFeatureExtractor linkFE = new LinkFEs.RootedEdgeLabelFE();
	private LinkFeatureExtractor linkFE = new LinkFEs.EdgeLabelFE();


	// map a node Id to Distributions of all features for that nodeId
	// ie F(z|s)---i.e., instanceMap.get(z) = Distribution d so that
	// d.getWeight(f) = F(z|s)
	transient private Map instanceMap;
	// similar to instanceMap, but for F(z|s,currentLevel) 
	transient private Map currentLevelInstanceMap;    
	// similar to instanceMap, but for F(z|s,currentLevel+1) 
	transient private Map nextLevelInstanceMap;    

	private int level;


	public void setFE(LinkFeatureExtractor fe) { this.linkFE = fe; }

	public void walk() { new MyWalker().walk(); }

	protected class MyWalker extends BasicWalker.MyBasicWalker
	{
		public void doStartWalkHook()
		{
	    instanceMap = new HashMap();
	    currentLevelInstanceMap = new HashMap();
	    nextLevelInstanceMap = new HashMap();
	    linkFE.setGraph( graph );
	    linkFE.setInitialDistribution( getInitialDistribution() );
			level = 0;
	    //System.out.println("FEWalker.MyWalker.doStartWalkHook");
		}

		public void doEndWalkHook()
		{
	    // add overall score to each node as a feature
	    Feature walkerScoreFeature = new Feature(new String[]{"walkerScore"});
	    for (Iterator i=getNodeSample().iterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				double idScore = getNodeSample().getLastProbability();
				Distribution d = (Distribution)instanceMap.get(id);
				if (d!=null) d.add( idScore, walkerScoreFeature );
	    }
		}
	
		public void doStartLevelHook() 
		{
			linkFE.setWalkLevel(++level);
	    for (Iterator i=nextLevelInstanceMap.keySet().iterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				Distribution nextLevelFeaturesForId = (Distribution)nextLevelInstanceMap.get( id );
				Distribution totalFeaturesForId = (Distribution)instanceMap.get( id );
				if (totalFeaturesForId==null) instanceMap.put(id, (totalFeaturesForId=new TreeDistribution()));
				totalFeaturesForId.addAll( probReachCurrentLevel*probRemainAtNode, nextLevelFeaturesForId);
	    }
	    //System.out.println("FEWalker.MyWalker.doStartLevelHook, level = "+currentLevel);
	    nextLevelInstanceMap = new HashMap();
		}
		public void doEndLevelHook()
		{
	    currentLevelInstanceMap = nextLevelInstanceMap;
		}

		public void doWalkHook() 
		{
	    Distribution linkFeatures = linkFE.toFeatures(fromId,linkLabel);
	    Distribution fromIdFeatures = (Distribution)currentLevelInstanceMap.get( fromId );
	    if (fromIdFeatures==null) currentLevelInstanceMap.put( fromId, (fromIdFeatures=new TreeDistribution()) );
	    //System.out.println("linkFeatures "+fromId+","+linkLabel+" = "+linkFeatures);
	    for (Iterator i=walk.iterator(); i.hasNext(); ) {
				GraphId toId = (GraphId)i.next();
				double toIdWeight = walk.getLastProbability();
				// combine destination-independent features and destination-dependent ones
				Distribution toIdFeatures = new TreeDistribution();
				toIdFeatures.addAll( 1.0, linkFeatures ); 
				toIdFeatures.addAll( toIdWeight, linkFE.toFeatures(fromId,linkLabel,toId) );
				// retrieve and initialize the current feature set
				Distribution nextLevelFeaturesForToId = (Distribution)nextLevelInstanceMap.get( toId );		
				if (nextLevelFeaturesForToId==null) {
					nextLevelInstanceMap.put( toId, (nextLevelFeaturesForToId=new TreeDistribution()) );
				}
				// add in the new features 
				nextLevelFeaturesForToId.addAll( fromIdWeight*linkLabelWeight, fromIdFeatures );
				nextLevelFeaturesForToId.addAll( fromIdWeight*linkLabelWeight, toIdFeatures );
				//System.out.println("f("+fromId+" -"+linkLabel+"-> "+toId+") = "+toIdFeatures);
				//System.out.println("F[->("+(currentLevel+1)+"),"+toId+" ==> "+nextLevelFeaturesForToId);
	    }
		}
	}
    
	/** Return the instance associated with this id after the walk.
	 * This is defined as the weighted sum of linkFe(v,label) over all
	 * (v,label) pairs that lead to id, where the weight is the weight
	 * of that transition in the walk.
	 */
	public Instance getInstance(GraphId id,String subpop) 
	{
		Distribution totalFeaturesForId = getInstanceDistribution( id );
        if (totalFeaturesForId==null) {
	    return new DistributionInstance(id, subpop, TreeDistribution.EMPTY_DISTRIBUTION);
		} else {
	    return new DistributionInstance(id, subpop, totalFeaturesForId );
		}
	}

	public Distribution getInstanceDistribution(GraphId id)
	{
		return (Distribution)instanceMap.get(id);
	}

}
