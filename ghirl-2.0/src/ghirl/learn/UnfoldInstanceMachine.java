package ghirl.learn;

import java.util.*;
import java.io.Serializable;

import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.util.gui.*;
import ghirl.graph.*;
import ghirl.util.*;

/**
 * A feature extractor, based on the connecting paths information.
 * Processed post the walk, and generates binary features
 */

public class UnfoldInstanceMachine implements InstanceMachine,Serializable
{
    static private final long serialVersionUID = 1;
    private final int CURRENT_VERSION_NUMBER = 1;

    // Features used
    private boolean EDGE_TYPE = false;
    private boolean EDGE_BIGRAMS = true;
    private boolean PATH_COUNT = false;
    private boolean SOURCE_COUNT = true;

    private Walker walker1 = new WeightedWalker();
    private Distribution walkedDist;
    public int numToScore = 100;

    public void setWalker(Walker walker){;}
    public Walker getWalker(){ return walker1; }
    public void setNumToScore(int k){ numToScore=k; }

    public UnfoldInstanceMachine() {; }

    public Distribution generateCandidates(Graph graph,Distribution initDist,NodeFilter filter)
    {
        walker1.setSamplingPolicy(false);
        walker1.setNumLevels(2);
        walkedDist = walker1.search( initDist );
        if (filter!=null) walkedDist = filter.filter( graph, walkedDist );
        for (Iterator it=initDist.iterator();it.hasNext();)
            walkedDist.remove(it.next());
        return walkedDist.copyTopN(numToScore);
    }

    public Instance getInstanceFor(String subpop, GraphId id)
    {
        MutableInstance instance = new MutableInstance(id, subpop);

        // add overall score to each node as a feature
	    Feature walkerScoreFeature = new Feature("walkerScore");
	    double idScore = walkedDist.getWeight(id);
        instance.addNumeric( walkerScoreFeature, Math.log(idScore) );

        // get the set of connecting paths for this node
        ConnectingPathsFinder cpf = new ConnectingPathsFinder(walker1.getGraph(),walker1.getEdgeStopList(),false);
        Set paths = cpf.aggregate(cpf.getConnectingPaths(walker1.getInitialDistribution(),id,walker1.getNumLevels()));

        // add features
        instance = addBinaryFeatures(instance,paths);
        return instance;
    }


    private MutableInstance addBinaryFeatures(MutableInstance instance, Set paths){
        // may want to add: path diversity (aggregate) ...

        Set pathsSourceCount = new HashSet();
        for (Iterator it=paths.iterator();it.hasNext();){
            Path p = (Path)it.next();
            pathsSourceCount.add(p.getNodes().get(0));
            for (int i=0; i<p.getSize();i++){
                String edge = (String)p.getEdges().get(i);
                if (EDGE_TYPE){
                    instance.addBinary(new Feature(new String[]{"edgeType",edge}) );
                }
                if (EDGE_BIGRAMS){
                    if (i>0)
                     instance.addBinary(new Feature(new String[]{"edgeBigram",(String)p.getEdges().get(i-1),edge}));
                }
            }
        }
        if (PATH_COUNT){
            instance.addBinary( new Feature(new String[]{"pathCount",new Integer(paths.size()).toString()}));
        }
        if (SOURCE_COUNT){
            //FOR THIS FEATURE, NEED TO LOOK AT THE *ORIGINAL* FEATURES
            instance.addBinary( new Feature(new String[]{"sourceCount",new Integer(pathsSourceCount.size()).toString()}));
        }
        return instance;
    }

    public String toString(){
        return("[Feature Extractor: UnfoldInstanceMachine]");
    }

}
