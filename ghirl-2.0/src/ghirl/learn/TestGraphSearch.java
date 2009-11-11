package ghirl.learn;

import java.util.*;
import java.io.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.*;

/** Evaluate a GraphSearcher.
 */

public class TestGraphSearch
{
    private Graph graph = null;
    private GraphSearcher searcher = new BasicWalker();
    private GraphSearchDataset testExamples = new GraphSearchDataset();
    private boolean showRankingFlag = false;
    private int numTop = 20;
    private boolean removeDups = false;
    private String saveFile = null;
    private boolean showResultFlag = false;

    public class MyCLP extends BasicCommandLineProcessor 
    {
        public void graph(String s) { graph = CommandLineUtil.makeGraph(s);	}
        public void annotate(String s) { graph = CommandLineUtil.annotateGraph(graph, s); }
        public void cache(String s) { graph = new CachingGraph(graph,StringUtil.atoi(s)); }
        public void loadFrom(String s) { searcher = CommandLineUtil.loadSearcher(s); }
        public void searcher(String s) { searcher = (GraphSearcher)BshUtil.toObject(s,GraphSearcher.class); }
        public void example(String s) { testExamples.add(new GraphSearchExample(s)); }
        public void numTop(String s) { numTop = StringUtil.atoi(s); }
        public void showRanking() { showRankingFlag=true; }
        public void removeDups() { removeDups=true; }
        public void saveAs(String s) { saveFile=s; }
        public void showResult() { showResultFlag=true; }
        public CommandLineProcessor searcherOpt() { return tryToGetCLP(searcher); }
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
	    if ( args[i].startsWith("-") ) {
                System.err.println("unknown option "+args[i]);
	    } else {
                testExamples.add( new GraphSearchExample(args[i]) );
	    }
        }
    }

    public GraphSearchEval test()
    {
        GraphSearchEval eval = new GraphSearchEval();
        test(eval);
        return eval;
    }

    public void test(GraphSearchEval eval)
    {
        eval.extend( graph, searcher, testExamples );
        if (showRankingFlag) showRankings(graph, testExamples, searcher, numTop);
    }

    static public void showRankings(Graph graph,GraphSearchDataset test,GraphSearcher searcher,int numTopShown)
    {
        for (GraphSearchExample.Looper i=test.iterator(); i.hasNext(); ) {
	    GraphSearchExample example = i.nextExample();
	    Distribution ranking = example.doQuery(searcher, graph);
	    System.out.println(example + "\n" + example.formatRankedList(ranking, numTopShown));
        }
    }


    static public void main(String[] args) 
    {
        TestGraphSearch t = new TestGraphSearch();
        t.processArguments(args);
        System.out.println("searcher: "+t.searcher);
        GraphSearchEval eval = t.test();
        //if (t.removeDups) System.out.println(eval.toDetails());
        //else System.out.println(eval.toDetails(false));
        System.out.println(eval.averageRecallAsFunctionOfK());
        System.out.println(eval.toTable());
        if (t.showResultFlag) {
	    eval.setGraph( t.graph );
	    new ViewerFrame("Result", eval.toGUI());
        }
        if (t.saveFile!=null) {
	    try {
                String format = eval.getFormatNames()[0];
                eval.saveAs( new File(t.saveFile+eval.getExtensionFor(format)), format );
	    } catch (Exception ex) {
                ex.printStackTrace();
	    }
        }
    }
}
