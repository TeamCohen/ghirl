package ghirl.learn;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.*;

public class TrainGraphSearch
{
    private Graph graph = null;
    private Walker walker = null;
    private GraphSearchLearner learner = new ClassifierRerankerLearner();
    private ClassifierLearner reranker = null;
    private InstanceMachine fe = new ClassifierRerankerLearner.WalkerInstanceMachine();
    private GraphSearchDataset trainExamples = new GraphSearchDataset();
    //walker parameters
    private int walker_numLevels = 12;
    private double walker_stayProb = 0.5;
    private int walker_numSteps = 1000;
    private boolean walker_stayVersion = false;
    private boolean walker_sampling = true;
    private String walker_weights = "uniform"; // other values: "random", file-name
    //admin
    private String saveFile = null;
    private boolean printResult = false;
    private boolean printEval = false;
    private boolean doTrain = true; // if false, then this will evaluate initial walker performance for the training dir.


    public class MyCLP extends BasicCommandLineProcessor {
        public void graph(String s) { try { graph = new TextGraph(s); } catch(IOException e) { throw new IllegalStateException("Couldn't open graph "+s,e); }}
        public void cache(String s) { graph = new CachingGraph(graph,StringUtil.atoi(s)); }
        public void annotate(String s)
        {
            graph = AnnotatableGraph.addAnnotator( graph, (GraphAnnotator)BshUtil.toObject(s,GraphAnnotator.class) );
        }
        public void learner(String s) { learner = (GraphSearchLearner)BshUtil.toObject(s,GraphSearchLearner.class); }
        public void reranker(String s) { reranker = (ClassifierLearner)BshUtil.toObject(s,ClassifierLearner.class); }
        public void walker(String s) { walker = (Walker)BshUtil.toObject(s,Walker.class); }
        public void fe(String s) { fe = (InstanceMachine)BshUtil.toObject(s,InstanceMachine.class); }
        public void levels(String s) { walker_numLevels =StringUtil.atoi(s); }
        public void steps(String s) { walker_numSteps = StringUtil.atoi(s); }        public void stayProb(String s) { walker_stayProb = (new Double(s)).doubleValue(); }
        public void stay() { walker_stayVersion = true; }
        public void sample(String s) { if (s.toLowerCase().startsWith("y")) walker_sampling = true; else walker_sampling=false; }
        public void weights(String s) { walker_weights = s.toLowerCase(); }
        public void example(String s) { trainExamples.add(new GraphSearchExample(s)); }
        public void dir(String s) {
            String[] files = new File(s).list();
            for (int i=0; i<files.length; i++)
                trainExamples.add(new GraphSearchExample(s+"\\"+files[i]));
        }
        public void noTrain() { doTrain = false; }
        public void saveAs(String s) { saveFile=s; }
        public void printResult() { printResult=true; }
        public void printEval() { printEval=true; }
        public CommandLineProcessor learnerOpt() { return tryToGetCLP(learner); }
        public void usage()
        {
            super.usage();
            System.out.println(" GraphSearchExample files are specified either by the dir option, " +
                                "and/or by specifying each example separately using the example argument.");
        }
    }

    public void processArguments(String[] args)
    {
        int i=0;
        while (i<args.length){
            int argp = new MyCLP().consumeArguments(args,i);
            if (argp==0){ //allow specifying an example as a default arg.
                argp=1;
                try{
                    trainExamples.add(new GraphSearchExample(args[i]));
                }catch(Exception e){ ;
                }
            }
            i += argp;
        }
    }

    public GraphSearcher train()
    {
        return learner.batchTrain(trainExamples);
    }

    static public void main(String[] args) throws IOException
    {
        TrainGraphSearch t = new TrainGraphSearch();
        t.processArguments(args);
        t.learner.setGraph( t.graph );

        // set the feature extractor
        t.learner.setInstanceMachine( t.fe );

        // make adjustments to the walker
        Walker walker;
        if (t.walker==null) walker=t.learner.getWalker();
        else walker = t.walker;

        walker.setGraph( t.graph) ;
        walker.setNumLevels(t.walker_numLevels);
        walker.setNumSteps(t.walker_numSteps);
        walker.setProbRemainAtNode(t.walker_stayProb);
        if (t.walker_stayVersion){ walker.setStayWalkVersion(true); }
        walker.setSamplingPolicy(t.walker_sampling);
        try{
            File weightsFile = new File(t.walker_weights);
            ((WeightedWalker)walker).readEdgeWeights(weightsFile);
        } catch (Exception e){
            if (t.walker_weights.equals("uniform")) walker.setUniformEdgeWeights();
            else if (t.walker_weights.equals("random")) walker.setRandomEdgeWeights();
        }
        t.learner.getInstanceMachine().setWalker(walker);


        //make adjustments to learner
        if (t.reranker!=null) try{
            ((ClassifierRerankerLearner)t.learner).setClassifierLearner(t.reranker);
        } catch (Exception e){ System.out.println("The re-ranker specified can not be applied " +
                "(either the learner does not require re-ranker, or bad specification.)");

        }

        // print parameters
        System.out.println("learner: "+t.learner);

        if (t.printEval){
            GraphSearchEval eval = new GraphSearchEval();
            eval.extend(t.graph, t.learner.getWalker(), t.trainExamples);
            System.out.println("Initial MAP: " + eval.meanAveragePrecision());
        }

        // train
        if (t.doTrain){
            GraphSearcher searcher = t.train();

            if (t.printResult){
                System.out.println("learned graphsearcher: "+searcher);
            }
            if (t.printEval){
                GraphSearchEval eval = new GraphSearchEval();
                eval.extend(t.graph, searcher, t.trainExamples);
                System.out.println("Final MAP: " + eval.meanAveragePrecision());
	            for (GraphSearchExample.Looper i=t.trainExamples.iterator(); i.hasNext(); ) {
	                GraphSearchExample example = i.nextExample();
                    Distribution dist = eval.getRanking(example.getSubpopulationId());
                    System.out.println("final list: ");
                    for (Iterator it=dist.orderedIterator();it.hasNext();){
                        System.out.println(it.next().toString());
                    }
                }
            }
            if (t.saveFile!=null) {
                if (!IOUtil.saveSomehow(searcher,new File(t.saveFile))) {
                    System.out.println("can't save "+searcher);
                    System.out.println("serialization error encountered is below:");
                    try {
                        IOUtil.saveSerialized((Serializable)searcher,new File(t.saveFile));
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
    }
}
