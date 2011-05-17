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

public class ClassifierRerankerExpt
{
	private String dataFileName = null;
	private ClassifierLearner classifierLearner = new RankingPerceptronLearner();
	private Splitter splitter = new CrossValSplitter();
	private String saveFile = null;
	private StringEncoder encoder = new StringEncoder('%',"$ \t\n");

	public CommandLineProcessor getCLP() { return new MyCLP(); }
	public class MyCLP extends BasicCommandLineProcessor 
	{
		public void data(String s) { dataFileName = s; }
		public void splitter(String s) { splitter = Expt.toSplitter(s);	}
		public void saveAs(String s) { saveFile=s; }
		public void learner(String s) { 
	    classifierLearner = (ClassifierLearner)BshUtil.toObject(s,ClassifierLearner.class);
		} 
	}

	private GraphSearchEval doExpt() throws IOException,NumberFormatException
	{
		Dataset data = DatasetLoader.loadFile(new File(dataFileName));
		System.out.println("loaded "+data.size()+" examples");

		GraphSearchEval eval = new GraphSearchEval();
		Dataset.Split split = data.split(splitter);
		ProgressCounter pc = new ProgressCounter("train/test", "fold", split.getNumPartitions());
		for (int k=0; k<split.getNumPartitions(); k++) {
	    Dataset train = split.getTrain(k);
	    Dataset test = split.getTest(k);
	    DatasetClassifierTeacher teacher = new DatasetClassifierTeacher(data);
	    Classifier classifier = teacher.train(classifierLearner);
	    doTest( classifier, test, eval );
	    pc.progress();
		}
		pc.finished();
		return eval;
	}

	private void doTest( Classifier classifier, Dataset test, GraphSearchEval eval)
	{
		Map bySubpopMap = ClassifierRerankerLearner.sortBySubpop(test);
		for (Iterator i=bySubpopMap.keySet().iterator(); i.hasNext(); ) {
            String subpop = (String)i.next();
            List subdata = (List)bySubpopMap.get(subpop);
            // find minimal score, so we can make them all greater than zero
            double minScore = Double.MAX_VALUE;
            for (Iterator j=subdata.iterator(); j.hasNext(); ) {
                    Example ex = (Example)j.next();
                    double prediction = classifier.classification( ex.asInstance() ).posWeight();
                    minScore = Math.min( minScore, prediction );
            }
            // now create a ranking and set of positive examples */
            Distribution ranking = new TreeDistribution();
            Set posIds = new TreeSet();
            for (Iterator j=subdata.iterator(); j.hasNext(); ) {
                Example ex = (Example)j.next();
                double prediction = classifier.classification( ex.asInstance() ).posWeight();
                ClassLabel predictedLabel = ClassLabel.positiveLabel(prediction-minScore+1.0);
                GraphId exId = GraphId.fromString( encoder.encode(ex.getSource().toString()) );
                ranking.add( prediction-minScore+1, exId );
                if (ex.getLabel().isPositive()) posIds.add( exId );
                // einat - fix the below, query nodes and uninteresting nodes should be excluded
                eval.extend( subpop, ranking, posIds, null, null );
            }
		}
	}

	public static void main(String[] args) throws IOException,NumberFormatException
	{
		ClassifierRerankerExpt x = new ClassifierRerankerExpt();
		x.getCLP().processArguments(args);
		GraphSearchEval eval = x.doExpt();
		System.out.println(eval.toTable());
		//new ViewerFrame("result", eval.toGUI());
		if (x.saveFile!=null) 
	    IOUtil.saveSomehow(eval, new File(x.saveFile),true);
	}
}
