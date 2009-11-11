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

public class ClassifierRerankerLearner implements GraphSearchLearner, CommandLineProcessor.Configurable
{
	protected ClassifierLearner classifierLearner = new RankingPerceptronLearner();
	protected Graph graph;
	protected InstanceMachine machine = new WalkerInstanceMachine();
	private String saveDataFile = null;

	public CommandLineProcessor getCLP() { return new MyCLP(); }
	public class MyCLP extends BasicCommandLineProcessor 
	{
		public void fe(String s) {
            try{
	            ((FEWalker)machine.getWalker()).setFE( (LinkFeatureExtractor)BshUtil.toObject(s,LinkFeatureExtractor.class) );
            }catch (Exception e){
                System.out.println("Walker not compatible with FE " + e.toString());
            }
		}
		public void learner(String s) { 
	        classifierLearner = (ClassifierLearner)BshUtil.toObject(s,ClassifierLearner.class);
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

    public void setClassifierLearner(ClassifierLearner cl){
        this.classifierLearner = cl;
    }

	public void setGraph(Graph graph)
	{ 
		this.graph = graph;
        this.machine.getWalker().setGraph(graph);
	}

    public void setInstanceMachine(InstanceMachine machine){
        this.machine = machine;
    }

    public InstanceMachine getInstanceMachine(){
        return machine;
    }

    public Walker getWalker(){
        return machine.getWalker();
    }

	public GraphSearcher batchTrain(GraphSearchDataset searchData)
	{
		Dataset classData = new BasicDataset();
		for (GraphSearchExample.Looper i=searchData.iterator(); i.hasNext(); ) {
            GraphSearchExample example = i.nextExample();
            // build instances for each node retrieved by the walker
            System.out.println("Generating examples from "+example);
            Distribution exampleInitDist = graph.asQueryDistribution(example.getQueryString());
            Distribution ranking = machine.generateCandidates(graph,exampleInitDist,example.getNodeFilter() );
            // save the instances for the final ranked answers
            ProgressCounter pc = new ProgressCounter("building instances","candidate",ranking.size());

            int counter=0;
            for (Iterator j = ranking.orderedIterator(); j.hasNext(); ) {
                GraphId id = (GraphId)j.next();
                Instance instance = machine.getInstanceFor( example.getQueryString(), id);
                Example e = new Example(instance, example.candidateLabel(id));
                classData.add(e,false);

                pc.progress();
            }
            pc.finished();
		}

		if (saveDataFile!=null) {
            if (!IOUtil.saveSomehow( classData, new File(saveDataFile) )) {
                    System.out.println("can't save datafile?");
            } else {
                    System.out.println("saved "+classData.size()+" examples to "+saveDataFile);
            }
		}

		DatasetClassifierTeacher teacher = new DatasetClassifierTeacher(classData);
		System.out.println("training on "+classData.size()+" examples from "+searchData.size()+" searches");
		Classifier classifier = teacher.train(classifierLearner);
		new ViewerFrame("classifier", new SmartVanillaViewer(classifier));
		new ViewerFrame("dataset", new SmartVanillaViewer(classData));
		return new ClassifierRerankerGraphSearcher(machine,classifier);
	}

	static class WalkerInstanceMachine implements InstanceMachine,Serializable
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;

		private FEWalker walker1 = new FEWalker();
		public int numToScore = 100;

        public void setWalker(Walker walker){ ;}
        public Walker getWalker(){ return walker1; }
        public void setNumToScore(int k){ numToScore=k; }

		public Distribution generateCandidates(Graph graph,Distribution initDist,NodeFilter filter)
		{
            Distribution tmp = walker1.search( initDist );
            if (filter!=null) tmp = filter.filter( graph, tmp );
            return tmp.copyTopN(numToScore);
		}

		public Instance getInstanceFor(String subpop, GraphId id)
		{
	        return walker1.getInstance( id, subpop );
		}

        public String toString(){
            return("[Feature Extractor: WalkerInstanceMachine]");
        }

	}

	/** A utility: sort a dataset by subpopulation.  Returns a map so
	 * that map.get(subpop) is a List of examples.
	 */
	static public Map sortBySubpop(Dataset data)
	{
		Map map = new HashMap();
		for (Example.Looper i=data.iterator(); i.hasNext(); ) {
	    Example ex = i.nextExample();
	    List list = (List)map.get( ex.getSubpopulationId() );
	    if (list==null) map.put( ex.getSubpopulationId(), (list = new ArrayList()) );
	    list.add( ex );
		}
		return map;
	}

	static class ClassifierRerankerGraphSearcher extends AbstractGraphSearcher implements Serializable, Visible
	{
		static private final long serialVersionUID = 1;
		private final int CURRENT_VERSION_NUMBER = 1;
		private Classifier classifier;
		private InstanceMachine machine;

		public ClassifierRerankerGraphSearcher(InstanceMachine machine,Classifier classifier)
		{
            this.machine = machine;
            this.classifier = classifier;
		}
		public Distribution search(GraphId id) { return search(id,null); }
		public Distribution search(Distribution queryDistribution) { return search(queryDistribution,null); }
		public Distribution search(GraphId id, NodeFilter filter) 
		{
	        return search(new TreeDistribution(id), filter);
		}
		public Distribution search(Distribution queryDistribution, NodeFilter filter)
		{
	        return rerankWithClassifier( machine.generateCandidates(graph,queryDistribution, filter) );
		}
		// for now...should include the walker params as well
		public Viewer toGUI() { return new SmartVanillaViewer(classifier); }

		private Distribution rerankWithClassifier(Distribution ranking)
		{
            ArrayList accum = new ArrayList();
            for (Iterator i=ranking.iterator(); i.hasNext(); ) {
                GraphId id = (GraphId)i.next();
                Instance instance = machine.getInstanceFor(null,id);
                double score = classifier.classification(instance).posWeight();
                accum.add( new ScoredId(score,id) );
            }
            Collections.sort( accum );
            Distribution reranking = new TreeDistribution();
            for (int i=0; i<accum.size(); i++) {
                    reranking.add( accum.size()-i, ((ScoredId)accum.get(i)).id );
            }
            return reranking;
		}

		// help class for re-ranking
		private static class ScoredId implements Comparable
		{ 
            double score; GraphId id;
            public ScoredId(double score,GraphId id) { this.score=score; this.id=id; }
            public int compareTo(Object o) {
                    ScoredId b = (ScoredId)o;
                    double diff = b.score - score;
                    return diff<0 ? -1 : (diff>0 ? +1 : 0);
            }
		}

		public String toString() { return "[ClsRerankGS "+machine.getWalker()+" "+classifier+" "+machine+"]"; }
	}

	public String toString() { return "[ClassifierRerankerLearner: "+machine.getWalker()+" "+classifierLearner+" "+machine+"]"; }


	public static void main(String[] args) throws IOException
	{
		ClassifierRerankerLearner crl = new ClassifierRerankerLearner();
		int argp = crl.getCLP().consumeArguments(args,0);
		if (args.length-argp!=2) 
	    throw new IllegalArgumentException("usage: [options] datafile graphSearcherFile");
		Dataset classData = DatasetLoader.loadFile(new File(args[argp]));
		DatasetClassifierTeacher teacher = new DatasetClassifierTeacher(classData);
		Classifier classifier = teacher.train(crl.classifierLearner);
		GraphSearcher gs = new ClassifierRerankerGraphSearcher(crl.machine,classifier);
		try {
	    IOUtil.saveSerialized((Serializable)gs, new File(args[argp+1]));
		} catch (Exception ex) {
	    ex.printStackTrace();
		}
	}
}
