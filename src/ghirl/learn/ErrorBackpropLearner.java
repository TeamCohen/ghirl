package ghirl.learn;

import ghirl.util.*;
import ghirl.graph.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import edu.cmu.minorthird.classify.algorithms.linear.*;

import java.util.*;
import java.io.*;

/**
 * Learn from examples a GraphSearcher that re-ranks examples based on
 * scores from a learned classifier.
 */

public class ErrorBackpropLearner implements GraphSearchLearner, CommandLineProcessor.Configurable
{
	protected Graph graph;
	protected BackPropInstanceMachine machine = new BackPropInstanceMachine();
	private String saveDataFile = null;
    int maxNumNodesToTrainOnPerExample = 2;
    protected Map trainNodes = new HashMap();

	public CommandLineProcessor getCLP() { return new MyCLP(); }
	public class MyCLP extends BasicCommandLineProcessor 
	{
		public void learner(String s) {
		} 
		public CommandLineProcessor walkerOpt() {
	    return tryToGetCLP(machine.getWalker());
		}
		public void numToRescore(String s) { 
	    machine.setNumToScore(StringUtil.atoi(s));
		}
		public void saveData(String s) { 
	    saveDataFile = s;
		}
	}

    public String toString() { return "[ErrorBackpropLearner "+machine.getWalker()+"]"; }

	public void setGraph(Graph graph){
		this.graph = graph;
        this.machine.getWalker().setGraph(graph);
	}

    public void setInstanceMachine(InstanceMachine machine){
        System.out.println("ErrorBackpropLearner uses an internal InstanceMachine (does not take fe)."); }
    public InstanceMachine getInstanceMachine(){ return machine; }


    public Walker getWalker(){
        return machine.getWalker();
    }


    // this basic gradient descent routine is to be enhanced
    public GraphSearcher batchTrain(GraphSearchDataset searchData){
        double stepSize = 1;
        int maxIteations = 10000;
        double minimalWeight = 0.01;

        //initialize
        try{
         machine.getWalker().setNumLevels(2);
         //((WeightedWalker)machine.getWalker()).readEdgeWeights(new File("c:/weights3.txt"));
         machine.getWalker().setRandomEdgeWeights();
        } catch (Exception e){;}
        int trainSize = constructTrainSet(searchData);
        System.out.println("Learning based on " + trainNodes.size() + " examples, and " + trainSize + " nodes.");

        // iterate
        double meanAvgPrec = 0;
        for (int j=0; j<maxIteations && meanAvgPrec<1; j++){
            GraphSearchEval eval = new GraphSearchEval();
            eval.extend(graph,machine.getWalker(),searchData);
            meanAvgPrec = eval.meanAveragePrecision();
            //System.out.println(machine.walker1.getWeights().toString());
            System.out.println("Iteration " + j + ": MAP " + eval.meanAveragePrecision());

             Map derivatives = iterate(searchData, eval);
            // Originally: divide by number of examples. but the learning ratio accounts for that anyway.
            ((WeightedWalker)machine.getWalker()).setWeights(updateWeights(derivatives,stepSize,minimalWeight));
        }
        return machine.getWalker();
    }


    private int constructTrainSet(GraphSearchDataset searchData){
        int trainNumNodes = 0;
        for (GraphSearchExample.Looper i=searchData.iterator(); i.hasNext(); ) {
            GraphSearchExample example = i.nextExample();
            Distribution exampleInitDist = graph.asQueryDistribution(example.getQueryString());
            Distribution ranking = machine.generateCandidates(graph,exampleInitDist,example.getNodeFilter() );

            // Construct the training set
            Set correctAnswers = example.getCorrectAnswerSet();
            Set trainNodesEx = new HashSet();
            int numNodes = Math.min(maxNumNodesToTrainOnPerExample,correctAnswers.size());
            for (Iterator it=ranking.orderedIterator();it.hasNext() & trainNodesEx.size()<numNodes;){
                GraphId node = (GraphId)it.next();
                if (correctAnswers.contains(node)) trainNodesEx.add(node);
            }
            /**   ADD THIS BACK
            for (Iterator it=ranking.orderedIterator();it.hasNext() & trainNodesEx.size()<2*numNodes;){
                GraphId node = (GraphId)it.next();
                if (!correctAnswers.contains(node)) trainNodesEx.add(node);
            }  **/
            trainNodes.put(example,trainNodesEx);
            trainNumNodes += trainNodesEx.size();

        }
        return trainNumNodes;
    }


    public GraphSearchEval eval(GraphSearchDataset searchData){
        GraphSearchEval eval = new GraphSearchEval();

		for (GraphSearchExample.Looper i=searchData.iterator(); i.hasNext(); ) {
            GraphSearchExample example = i.nextExample();
            Distribution exampleInitDist = graph.asQueryDistribution(example.getQueryString());
            Distribution ranking = machine.generateCandidates(graph,exampleInitDist,example.getNodeFilter() );
            eval.extend(example.toString(),ranking,example.getCorrectAnswerSet(),example.getKnownIds(),exampleInitDist);
        }

         return eval;
    }

    private Map updateWeights(Map derivatives, double stepSize, double minimalWeightAllowed){
        Map edgeWeights = ((WeightedWalker)machine.getWalker()).getWeights();
        for (Iterator it=derivatives.keySet().iterator();it.hasNext();){
            String edgeType = (String)it.next();
            double weight = ((WeightedWalker)machine.getWalker()).getEdgeWeight(edgeType);
            double deriv = ((Double)derivatives.get(edgeType)).doubleValue();
            double newWeight = Math.max(minimalWeightAllowed,weight-deriv/trainNodes.values().size()*stepSize);
            edgeWeights.put(edgeType,new Double(newWeight));
            //System.out.println("weight " + edgeType + " " + weight+ " " + deriv/trainSize*stepSize);
        }
        return edgeWeights;
    }


    // updates edgeWeights, and returns evaluation of performance prior to the update
	public Map iterate(GraphSearchDataset searchData,GraphSearchEval eval)
	{
        Map derivatives = new HashMap();
        double cost = 0;

		for (GraphSearchExample.Looper i=searchData.iterator(); i.hasNext(); ) {
            GraphSearchExample example = i.nextExample();
            Distribution exampleInitDist = graph.asQueryDistribution(example.getQueryString());

            // updated derivatives
            for (Iterator j = ((Set)trainNodes.get(example)).iterator(); j.hasNext(); ) {
                GraphId id = (GraphId)j.next();
                double optimalProb = example.getCorrectAnswerSet().contains(id)? 1.0: 0 ;
                double exampleError = eval.getRanking(example.getSubpopulationId()).getWeight(id)-optimalProb;

                cost += 0.5*Math.pow(exampleError,2);
                System.out.println(id.toString() + " -- weight: " + eval.getRanking(example.getSubpopulationId()).getWeight(id));
                Map exampleDerivatives = machine.getDerivatives(graph,exampleInitDist,id);
                //System.out.println("ex derivs: " + exampleDerivatives.toString());
                machine.joinDerivs(exampleDerivatives,exampleError,derivatives);
            }
            //System.out.println("derivative: " + derivatives.toString());
		}

        System.out.println("ERROR: " + cost);

        return derivatives;
	}


	static class BackPropInstanceMachine implements InstanceMachine
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
        Distribution[] distTimeT;;

        public int numToScore = 100;
		public WeightedWalker walker1 = new WeightedWalker();

        public Walker getWalker(){ return walker1; }
        public void setWalker(Walker walker){
            try{
                walker1=(WeightedWalker)walker;
            }catch(Exception e){
                System.out.println("BackPropInstanceMachine requires WeightedWalker.");
            }
        }
        public void setNumToScore(int k){ ;}

        public Instance getInstanceFor(String str, GraphId id){ return null; }

        public Map getDerivatives(Graph graph,Distribution initDist,GraphId id){
            Map nodeDerivatives = new HashMap();
            boolean includeCyclicPaths = true;

            // first, recover the set of traversed paths
            Set paths = new HashSet();
            ConnectingPathsFinder cpf = new ConnectingPathsFinder(graph,walker1.getEdgeStopList(),includeCyclicPaths);

            for (Iterator it=initDist.iterator();it.hasNext();){

                Set connectingPaths = cpf.getConnectingPaths((GraphId)it.next(),id,walker1.getNumLevels());
                paths.addAll(connectingPaths);

                for (Iterator itPaths=paths.iterator();itPaths.hasNext();){
                    Path path = (Path)itPaths.next();
                    LinkedList pathNodes = path.getNodes();
                    double[] nodeProbs = new double[path.getNodes().size()];
                    nodeProbs[0] = initDist.getWeight(path.getNode(0))*(1-walker1.getProbRemainAtNode());

                    for (int i=1; i<pathNodes.size();i++){
                        GraphId node = (GraphId)pathNodes.get(i);
                             //System.out.println("probReach: " + probReachTarget);
                        Map stepDerivatives = getNodeDerivatives(node,path,i,nodeProbs);

                        //System.out.println("step derivs: " + stepDerivatives.toString());
                        nodeDerivatives = joinDerivs(stepDerivatives,1,nodeDerivatives);
                    }
                }
            }
            return nodeDerivatives;
        }

        // a local utility
        public Map joinDerivs(Map delta,double factor,Map all){
            for (Iterator it=delta.keySet().iterator();it.hasNext();){
                Object type = it.next();
                double val = ((Double)delta.get(type)).doubleValue()*factor;
                //System.out.println("val: " + val);
                double curVal = 0;
                if (all.keySet().contains(type))
                    curVal = ((Double)all.get(type)).doubleValue();
                all.put(type,new Double(curVal+val));
            }
            return all;
        }

		public Distribution generateCandidates(Graph graph,Distribution initDist,NodeFilter filter)
		{
            distTimeT = new Distribution[walker1.getNumLevels()+1];
            Distribution tmp = walker1.search( initDist );

            if (filter!=null) tmp = filter.filter( graph, tmp );
            return tmp.copyTopN(numToScore);
		}

        public double getProbReachTarget(Path path,int nodeIdx)
        {
            double probReach=1-walker1.getProbRemainAtNode();
            for (int i=nodeIdx; i<(path.getNodes().size()-1); i++){
                GraphId node = (GraphId)path.getNodes().get(i);
                double outgoingProb = walker1.getOutgoingWeight(node);
                double edgeProb = walker1.getEdgeWeight((String)path.getEdges().get(i));
                probReach *= (edgeProb/outgoingProb)*walker1.getProbRemainAtNode();
            }
            return probReach;
        }


        public Map getNodeDerivatives(GraphId node, Path path, int timeStep, double[] nodeProbs){

            Map derivs = new HashMap();
            GraphId parentNode = (GraphId)path.getNodes().get(timeStep-1);

            double probParent = nodeProbs[timeStep-1];
            double probReachTarget = getProbReachTarget(path,timeStep);

            double outgoingWeight = walker1.getOutgoingWeight(parentNode);
            Set outgoingEdgeTypes = walker1.getGraph().getEdgeLabels(parentNode);

            // compute total weight of edges leading from x to y
            double connectingWeight = 0;
            for (Iterator edgeIt=outgoingEdgeTypes.iterator(); edgeIt.hasNext();){
                String edgeType = (String)edgeIt.next();
                if (walker1.getGraph().followLink(parentNode,edgeType).contains(node)){
                   connectingWeight += walker1.getEdgeWeight(edgeType);
                }
            }
            //modify this - actually consider one edge type per path
            nodeProbs[timeStep]=connectingWeight/outgoingWeight*probParent*(1-walker1.getProbRemainAtNode());

            // compute derivative per edge type
            for (Iterator edgeIt=outgoingEdgeTypes.iterator();edgeIt.hasNext();){
                String edgeType = (String)edgeIt.next();
                if (!walker1.getEdgeStopList().contains(edgeType)){
                    Set childrenOverEdgeType = walker1.getGraph().followLink(parentNode,edgeType);
                    int countEdgeParentNode = childrenOverEdgeType.contains(node)? 1: 0;
                    int countEdgeParentAllChildren = childrenOverEdgeType.size();
                    double derivEdgeType = (countEdgeParentNode*outgoingWeight
                            -countEdgeParentAllChildren*connectingWeight)/Math.pow(outgoingWeight,2);
                    //System.out.println(countEdgeParentNode + " " + outgoingWeight);
                    //System.out.println(countEdgeParentAllChildren + " " + connectingWeight);


                    double deriv = 0;
                    if (derivs.keySet().contains(edgeType))
                        deriv=((Double)derivs.get(edgeType)).doubleValue();
                    /**
                    System.out.println(" node: " + node.toString() +
                                       " parent: " + parentNode.toString() +
                                       " probParent: " + probParent +
                                       " prob reach target: " + probReachTarget +
                                       " edgeType: " + edgeType + " deriv: " + derivEdgeType);
                     **/
                    derivs.put(edgeType,new Double(deriv + probParent*derivEdgeType*probReachTarget));
                 }
            }
            return derivs;
        }

        public String toString()
	    {
		return "BackPropInstanceMachine";
	    }
    }


    	public static void main(String[] args) throws IOException
	{
		ErrorBackpropLearner ebl = new ErrorBackpropLearner();
		int argp = ebl.getCLP().consumeArguments(args,0);
		if (args.length-argp!=2)
	    throw new IllegalArgumentException("usage: [options] datafile graphSearcherFile");
	}

}
