package ghirl.learn;

import java.util.*;
import java.io.*;

import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.*;

public class TrainTestGraphSearch
{
    private Graph graph = null;
    private GraphSearchLearner learner = new ClassifierRerankerLearner();
    private GraphSearchDataset trainExamples = new GraphSearchDataset();
	private GraphSearchDataset testExamples = new GraphSearchDataset();
    private String saveFile = null;
    private Splitter splitter = new RandomSplitter();
    private boolean showRankingFlag = false;
    private boolean removeDups = false;

    public class MyCLP extends BasicCommandLineProcessor {
        public void graph(String s) { try { graph = new TextGraph(s);  } catch(IOException e) { throw new IllegalStateException("Couldn't open graph "+s,e); }}
        public void annotate(String s)
        {
            graph = AnnotatableGraph.addAnnotator( graph, (GraphAnnotator)BshUtil.toObject(s,GraphAnnotator.class) );
        }
        public void splitter(String s) { splitter = Expt.toSplitter(s); }
        public void learner(String s) { learner = (GraphSearchLearner)BshUtil.toObject(s,GraphSearchLearner.class); }
        public void example(String s) { trainExamples.add(new GraphSearchExample(s)); }
        public void dir(String s) {
            String[] files = new File(s).list();
            for (int i=0; i<files.length; i++)
                trainExamples.add(new GraphSearchExample(s+"\\"+files[i]));
        }
        public void trainDir(String s) { dir(s); }
        public void testDir(String s) {
            String[] files = new File(s).list();
            for (int i=0; i<files.length; i++)
                testExamples.add(new GraphSearchExample(s+"\\"+files[i]));
        }
        public void fe(String s) {
            if ("unfold".equals(s.toLowerCase())) learner.setInstanceMachine(new UnfoldInstanceMachine());
        }
        public void saveAs(String s) { saveFile=s; }
        public void showRanking() { showRankingFlag=true; }
        public void removeDups() { removeDups=true; }
        public CommandLineProcessor learnerOpt() { return tryToGetCLP(learner); }
        public void usage()
        {
            super.usage();
            System.out.println(" remaining arguments are GraphSearchExample files");
        }
    }

    public void processArguments(String[] args)
    {
        int argp = new MyCLP().consumeArguments(args,0);
        // treat unprocessed examples as
        for (int i=argp; i<args.length; i++) {
            //System.out.println("loading example from "+args[i]);
            trainExamples.add( new GraphSearchExample(args[i]) );
        }
    }

    public GraphSearchEval trainTest()
    {
	learner.setGraph( graph );
	GraphSearchEval eval = new GraphSearchEval();
    // apply CV in case test examples not exlicitly specified
    if (testExamples.size()==0)
    {
	    GraphSearchDataset.Split split = trainExamples.split(splitter);
	    ProgressCounter pc = new ProgressCounter("train/test", "fold", split.getNumPartitions());
        for (int k=0; k<split.getNumPartitions(); k++) {
            GraphSearchDataset train = split.getTrain(k);
            GraphSearchDataset test = split.getTest(k);
            GraphSearcher searcher = learner.batchTrain(train);
            eval.extend(graph,searcher,test);
            pc.progress();
        }
	pc.finished();
    }
    else{
        GraphSearcher searcher = learner.batchTrain(trainExamples);
        eval.extend(graph,searcher,trainExamples);
        System.out.println("Train MAP: " + eval.meanAveragePrecision());
        eval = new GraphSearchEval();
        eval.extend(graph,searcher,testExamples);
        System.out.println("Test MAP: " + eval.meanAveragePrecision());
    }
	return eval;
    }

    static public void main(String[] args) throws IOException
    {
	TrainTestGraphSearch t = new TrainTestGraphSearch();
	t.processArguments(args);
	System.out.println("learner: "+t.learner);
	GraphSearchEval eval = t.trainTest();
	//if (t.removeDups) System.out.println(eval.toDetails());
	//else System.out.println(eval.toDetails(false));
	System.out.println(eval.toTable());
	if (t.saveFile!=null) {
	    if (!IOUtil.saveSomehow(eval,new File(t.saveFile))) {
		System.out.println("warning: can't save eval to "+t.saveFile);
	    }
	}
    }
}
