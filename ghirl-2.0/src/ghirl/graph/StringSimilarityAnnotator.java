package ghirl.graph;


import java.util.*;
import java.io.*;
import ghirl.util.*;
import edu.cmu.minorthird.util.ProgressCounter;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


/*
 * Add links to a graph based on a SoftDictionary. 
 */

public class StringSimilarityAnnotator extends GraphAnnotator
{
    /** Create a variant of this annotator with a possibly different
     * edge label, precondition, and postcondition, but the same
     * similarity metric.  
     */
    public StringSimilarityAnnotator makeVariant(String linkLabel,String preconditionDef,String postconditionDef)
    {
        SimilarityRescoringSearcher simSearcher = (SimilarityRescoringSearcher)getEdgeDefinition();
        NodeFilter precondition = new NodeFilter(preconditionDef);
        NodeFilter postcondition = new NodeFilter(postconditionDef);

        StringSimilarityAnnotator variant = 
            new StringSimilarityAnnotator(linkLabel,
                                          precondition,
                                          simSearcher.makeVariant(precondition,postcondition));
		
        return variant;
    }
    private StringSimilarityAnnotator(String linkLabel,NodeFilter precondition,GraphSearcher edgeDef)
    {
        super(linkLabel,precondition,edgeDef);
    }



    /** Uses the SoftTFIDF measure as the StringDistanceLearner,
     * and trains it on the supplied synonym-list file.
     *
     * @param synonymListFile contains "training data" (sample
     * strings to compute TF stats) for the SoftDictionary.  Each line
     * is a tab-separated list of sample strings, the first of which
     * is an identifier (which is ignored).
     */
    public StringSimilarityAnnotator(String linkLabel,String precondition,String postcondition,
                                     File synonymListFile)
    {
        this(linkLabel,precondition,postcondition,new JaroWinklerTFIDF());
        SimilarityRescoringSearcher simSearcher = (SimilarityRescoringSearcher)getEdgeDefinition();
        simSearcher.train(synonymListFile);
    }


    /** Uses the SoftTFIDF measure as the StringDistanceLearner.
     */
    public StringSimilarityAnnotator(String linkLabel,String precondition,String postcondition)
    {
        this(linkLabel,precondition,postcondition,new JaroWinklerTFIDF());
    }
    

    /** @param distanceLearnerSpec specifies a StringDistanceLearner in the format used by
     * com.wcohen.ss.DistanceLearnerFactory.  For example, 
     * <p>
     * <code> 
     * SoftTFIDF[tokenMatchThreshold=0.9]/WinklerVariant/Jaro
     * </code>
     * <p>
     * builds the JaroWinklerTFIDF class.
     */
    public StringSimilarityAnnotator(String linkLabel,String precondition,String postcondition,
                                     String distanceLearnerSpec)
    {
        this(linkLabel,precondition,postcondition,DistanceLearnerFactory.build(distanceLearnerSpec));
    }

    /** As the default edge definition, search for paths of the form
     * precondition -hasTerm-> term -inFile-> postcondition 
     */
    public StringSimilarityAnnotator(String linkLabel,String precondition,String postcondition,
                                     StringDistanceLearner distanceLearner)
    {
        this(linkLabel,precondition,postcondition,
             new PathSearcher(TextGraph.HASTERM_EDGE_LABEL+" "+TextGraph.INFILE_EDGE_LABEL),
             distanceLearner);
    }


    /**
     * @param distanceLearner nodes found by using edgeDefinition will be rescored according to their
     * similarity to the start node according to this distance metric.
     * This should produce scores between 0 and 1.
     * @param postcondition the output of the edgeDefinition will be
     * filtered using the postcondition, and the distanceLearner will
     * be trained assuming that its arguments satisfy either the
     * precondition or postcondition.
     */
    public StringSimilarityAnnotator(String linkLabel,String precondition,String postcondition,
                                     GraphSearcher edgeDefinition, StringDistanceLearner distanceLearner)

    {
        super(linkLabel,
              precondition,
              new SimilarityRescoringSearcher(precondition,postcondition,distanceLearner,edgeDefinition));
    }
    
    private static class SimilarityRescoringSearcher implements GraphSearcher
    {
        private NodeFilter precondition, postcondition;
        private StringDistanceLearner distanceLearner;
        private StringDistance distance;
        private GraphSearcher innerSearcher;
        private boolean trainingForDistanceMeasureGiven = false;
        private Graph noncachingVersionOfCurrentGraph, graph;
        private List variantList = new ArrayList();

        public SimilarityRescoringSearcher makeVariant(NodeFilter pre,NodeFilter post)
        {
            SimilarityRescoringSearcher variant = new SimilarityRescoringSearcher();
            variant.innerSearcher = innerSearcher;
            variant.precondition = pre;
            variant.postcondition = post;
            variant.distance = distance;
            variant.variantList = null;
            variantList.add( variant );
            return variant;
        }
		

        public SimilarityRescoringSearcher() {;}

        public SimilarityRescoringSearcher(String precondition,String postcondition,
                                           StringDistanceLearner distanceLearner,
                                           GraphSearcher innerSearcher)
        {
	    this.distanceLearner = distanceLearner;
	    this.innerSearcher = innerSearcher;
	    this.precondition = new NodeFilter(precondition);
	    this.postcondition = new NodeFilter(postcondition);
	    this.noncachingVersionOfCurrentGraph = null;
        }

        public void train(File synonymListFile)
        {
	    distance = new MySynonynTeacher(synonymListFile).train( distanceLearner );
	    trainingForDistanceMeasureGiven = true;
            // broadcast changes to variants
            for (Iterator i=variantList.iterator(); i.hasNext(); ) {
                SimilarityRescoringSearcher variant = (SimilarityRescoringSearcher)i.next();
                variant.distance = distance;
            }
        }

        // keep track of when the graph changes
        public void setGraph(Graph graph) 
        { 
	    this.graph = graph;
	    innerSearcher.setGraph(graph);
	    if (trainingForDistanceMeasureGiven || variantList==null) return;
	    // otherwise, decide if we need to train
	    Graph innerGraph = graph; 
	    while (innerGraph instanceof CachingGraph) {
                innerGraph = ((CachingGraph)innerGraph).getInnerGraph();
	    }
	    if (noncachingVersionOfCurrentGraph!=innerGraph) {
                // a "real" change in the graph
                noncachingVersionOfCurrentGraph = innerGraph;
                System.out.println("training "+distanceLearner);
                distance = new MyGraphTeacher().train( distanceLearner );
	    }
        }
        public Graph getGraph() 
        { 
	    return graph; 
        }
        // we only expect one type of search to happen...
        public Distribution search(GraphId id, NodeFilter nodeFilter)
        {
	    throw new IllegalStateException("unexpected usage");
        }
        public Distribution search(Distribution queryDistribution,NodeFilter nodeFilter)
        {
	    throw new IllegalStateException("unexpected usage");
        }
        public Distribution search(Distribution queryDistribution)
        {
	    throw new IllegalStateException("unexpected usage");
        }
        // search for candidates and rescore
        public Distribution search(GraphId fromId)
        {
	    //System.out.println("SimilarityRescoringSearcher: from "+fromId);
	    Distribution original = innerSearcher.search(fromId,postcondition);
	    //System.out.println("innerSearcher finds "+original.size()+" nodes");
	    Distribution rescored = new TreeDistribution();
	    StringWrapper fromWrapper = distance.prepare( graph.getTextContent(fromId) );
	    for (Iterator i=original.iterator(); i.hasNext(); ) {
                GraphId toId = (GraphId)i.next();
                StringWrapper toWrapper = distance.prepare( graph.getTextContent(toId) );
                double score = distance.score( fromWrapper, toWrapper );
                rescored.add( score, toId );
	    }
	    return rescored;
        }

        //
        // for unsupervised training
        //
        private abstract class MyTeacher extends StringDistanceTeacher 
        {
	    protected DistanceInstanceIterator distanceInstancePool() {
                return new BasicDistanceInstanceIterator( Collections.EMPTY_SET.iterator() );
	    }
	    protected DistanceInstanceIterator distanceExamplePool() {
                return new BasicDistanceInstanceIterator( Collections.EMPTY_SET.iterator() );			
	    }
	    protected DistanceInstance labelInstance(DistanceInstance distanceInstance) {
                return null;
	    }
	    protected boolean hasAnswers() {
                return false;
	    }
        }
        private class MyGraphTeacher extends MyTeacher
        {
	    protected StringWrapperIterator stringWrapperIterator() { 
                return new MyGraphStringWrapperIterator(); 
	    }
        }
        private class MySynonynTeacher extends MyTeacher
        {
	    private List accum = new ArrayList();
	    public MySynonynTeacher(File synonymListFile)
	    {
                try {
                    LineNumberReader in = new LineNumberReader(new FileReader(synonymListFile));
                    String line = null;
                    ProgressCounter pc = new ProgressCounter("loading "+synonymListFile,"lines");
                    while ((line = in.readLine())!=null) {
                        String[] parts = line.split("\\t+");
                        for (int i=1; i<parts.length; i++) {
                            accum.add( new BasicStringWrapper(parts[i]) );
                        }
                        pc.progress();
                    }
                    pc.finished();
                    in.close();
                } catch (IOException ex) {
                    throw new IllegalStateException("error: "+ex);
                }
                System.out.println("loaded "+accum.size()+" training strings from "+synonymListFile);
	    }
	    protected StringWrapperIterator stringWrapperIterator() { 
                return new BasicStringWrapperIterator( accum.iterator() );
	    }
        }

        // create string wrappers for all nodes that match a pre/post condition and return them
        private class MyGraphStringWrapperIterator implements StringWrapperIterator
        {
	    private Iterator i;
	    private GraphId nextValidNode;
	    public MyGraphStringWrapperIterator()  
	    {	
                i = graph.getNodeIterator(); 
                nextValidNode = null; 
                advance();
	    }
	    public boolean hasNext() 
	    { 
                return nextValidNode!=null; 
	    }
	    public Object next() 
	    { 
                StringWrapper w = new BasicStringWrapper( graph.getTextContent(nextValidNode) );
                advance(); 
                return w;
	    }
	    public void remove()
	    {
                throw new UnsupportedOperationException("can't remove!");
	    }
	    public StringWrapper nextStringWrapper() 
	    { 
                return (StringWrapper)next(); 
	    }
	    private void advance()
	    {
                while (i.hasNext()) {
                    GraphId id = (GraphId)i.next();
                    if (postcondition.accept(graph,id) || precondition.accept(graph,id)) {
                        nextValidNode = id;
                        return;
                    }
                }
                nextValidNode = null;
	    }
        }
    }
}
