package ghirl.learn;

import java.util.*;
import java.io.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.Distribution;

/** 
 * The analog of a labeled example for graph searches.  It consists of
 * a query, and optionally a NodeFilter, which will produce a set of
 * responses from a GraphSearcher; plus a complete listing of all
 * valid responses to the GraphSearcher, ie, all nodes that are
 * appropriately similar to the query, and also satisfy the
 * NodeFilter.  
 *
 * <p>Additionally, valid-response items can be tagged with a
 * 'uniqId', eg a person identifier for emails associated with the
 * same person. Some additional measurements can be made based
 * these uniqId's.
 * 
 * <p>The file format for a GraphSearchExample is as follows.  Each
 * non-blank line should either start with '#', or else should contain
 * a GraphId, in a format readable by GraphId.fromString, optionally
 * followed (after some whitespace) with a uniqId for that GraphId.
 * Lines starting "#!query:", "#!graph:", and "#!filter" specify the
 * initial query, the associated graph, and the filter, respectively.
 * For instance:
 *
 * <pre>
 * #Members of the SLIF project group as of 2002.
 * #!query: slif
 * #!filter: isa=email
 * #!graph: William's meeting-related email
 *
 * $Mitchell@Cs tom
 * $Tom.Mitchell@cmu.edu tom
 * $Tom.Mitchell@cs.cmu.edu tom
 * $William.Cohen@cs.cmu.edu
 * $atyu@andrew.cmu.edu
 * $juchangh@andrew.cmu.edu
 * $khuang@andrew.cmu.edu
 * $mjoffe@andrew.cmu.edu
 * $murphy@andrew.cmu.edu bob
 * $murphy@cmu.edu bob
 * $woomy@cs.cmu.edu zk
 * $zkou@andrew.cmu.edu zk
 * </pre>
 */

public class GraphSearchExample implements HasSubpopulationId
{
    private String subPop = null;
    private String queryString = null; 
    private String graphName = null;
    private NodeFilter nodeFilter = null;
    private Set posLabelSet = new HashSet();
    private Set knownLabelSet = new HashSet();  // constructed for the case of predicting new entities, then we'd like to ignore known entities in evaluation.
    private Map uniqCodeMap = new HashMap();

    private static final String QUERY_PREFIX = "#!query:";
    private static final String GRAPH_PREFIX = "#!graph:";
    private static final String FILTER_PREFIX = "#!filter:";

    //
    // getters
    //
    public String getQueryString() { return queryString; }
    public NodeFilter getNodeFilter() { return nodeFilter; }
    public Set getCorrectAnswerSet() { return posLabelSet; }
    public String getSubpopulationId() { return subPop; }

    public String getUniqCode(GraphId id) { return (String)uniqCodeMap.get(id); }

    //
    // constructor
    //

    public GraphSearchExample(String exampleFile)
    {
	try {
	    subPop = exampleFile;
	    loadLabelFile(exampleFile);
	} catch (IOException ex) {
	    throw new IllegalStateException("error loading "+exampleFile+": "+ex);
	}
    }
    private void loadLabelFile(String filename) throws IOException
    {
	posLabelSet = new HashSet();
	uniqCodeMap = new HashMap();
	FileInputStream is = new FileInputStream(new File(filename));
	LineNumberReader in = new LineNumberReader(new InputStreamReader(is));
	String line;
	while ((line = in.readLine())!=null) {
	    if (!processSpecialLine(line)) {
        if (line.startsWith("@"))
            knownLabelSet.add(GraphId.fromString(line.substring(1).trim()));
		String[] parts = line.split("\\s+");
		GraphId id = GraphId.fromString(parts[0]);
		posLabelSet.add( id );
		String uniqCode = parts.length>1 ? parts[1] : parts[0];
		uniqCodeMap.put( id, uniqCode );
	    }
	}
	in.close();
    }
    private boolean processSpecialLine(String line)
    {
        if (line.startsWith(QUERY_PREFIX)) {
            queryString = line.substring(QUERY_PREFIX.length()).trim();
            return true;
        } else if (line.startsWith(GRAPH_PREFIX)) {
            graphName = line.substring(GRAPH_PREFIX.length()).trim();
            return true;
        } else if (line.startsWith(FILTER_PREFIX)) {
            nodeFilter = new NodeFilter(line.substring(FILTER_PREFIX.length()).trim());
            return true;
        } else if (line.startsWith("#") || line.trim().length()==0) {
            return true;
        } else {
            return false;
        }
    }

    //
    // evaluation methods
    //

    /** Evaluate the output of a GraphSearcher, returning an Evaluation object. */
    public Evaluation evaluateSearchResult(Distribution dist, boolean ignoreDuplicates)
    {
	Evaluation eval = new Evaluation(ExampleSchema.BINARY_EXAMPLE_SCHEMA);
	evaluateSearchResult(eval,dist,ignoreDuplicates);
	return eval;
    }

    /** Evaluate the output of a GraphSearcher, returning an Evaluation object. */
    public Evaluation evaluateSearchResult(Distribution dist)
    {
	return evaluateSearchResult(dist,false);
    }

    /** Evaluate the output of a GraphSearcher, appending it to an existing Evaluation. */
    public void evaluateSearchResult(Evaluation eval, Distribution dist)
    {
	evaluateSearchResult(eval,dist,false);
    }

    /** Evaluate the output of a GraphSearcher, appending it to an existing Evaluation. */
    public void evaluateSearchResult(Evaluation eval, Distribution dist, boolean ignoreDuplicates)
    {
	Set uniqCodeOfIdsAlreadyRanked = new HashSet();
	for (Iterator i = dist.orderedIterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    double w = dist.getLastWeight()/dist.getTotalWeight();
	    String uniqCode = (String)uniqCodeMap.get(id);
	    if (uniqCode==null) uniqCode = id.toString();
	    boolean codeAlreadyRanked = uniqCodeOfIdsAlreadyRanked.contains(uniqCode);
	    if (!codeAlreadyRanked || !ignoreDuplicates) {
		ClassLabel trueLabel = candidateLabel(id);
		eval.extend( ClassLabel.positiveLabel(w), new Example(new MutableInstance(id),trueLabel), 0 );
	    }
	    uniqCodeOfIdsAlreadyRanked.add(uniqCode);
	}
	for (Iterator i=posLabelSet.iterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    if (dist.getWeight(id)==0) {
		// a false negative
		String uniqCode = (String)uniqCodeMap.get(id);
		if (uniqCode==null) uniqCode = id.toString();
		boolean codeAlreadyRanked = uniqCodeOfIdsAlreadyRanked.contains(uniqCode);
		if (!codeAlreadyRanked || !ignoreDuplicates) {
		    ClassLabel trueLabel = ClassLabel.binaryLabel(+1);
		    eval.extend( ClassLabel.binaryLabel(-1), 
				 new Example(new MutableInstance(id),trueLabel), 0 );
		}
	    }
	}
    }
    /** generate a label for a candidate pair */
    public ClassLabel candidateLabel(GraphId candidate) 
    {
	return ClassLabel.binaryLabel( (posLabelSet!=null && posLabelSet.contains(candidate)) ? +1 : -1 );
    }

    /**  Get the set of positive labels/aka relevant GraphIds. */
    public Set getPositiveIds()
    {
	return posLabelSet;
    }

    /**  Get the set of known labels/aka, that will be discarded from final ranking and evaluation
     * (this is useful if one is interested in retrieving new entities, in addition to a known set */
    public Set getKnownIds()
    {
        return knownLabelSet;
    }

    //
    // execution methods
    //

    /**
     * Use the provided searcher to execute the query, and filter it.
     */
    public Distribution doQuery(GraphSearcher searcher,Graph graph)
    {
	if (queryString==null || queryString.length()==0) 
	    throw new IllegalArgumentException("null query string");
	Distribution initDist = graph.asQueryDistribution(queryString);
	if (initDist==null || initDist.size()==0)
	    throw new IllegalArgumentException("nothing found for '"+queryString+"'");
	searcher.setGraph(graph);
	Distribution result = searcher.search( initDist, nodeFilter );
	return result;
    }

    //
    // output utilities
    //

    public String formatRankedList(Distribution dist,int numToShowAllEntries)
    {
	StringBuffer buf = new StringBuffer("top "+numToShowAllEntries+" entries:\n");
	java.text.DecimalFormat fmt = new java.text.DecimalFormat("0.0000");
	Set uniqCodeOfIdsAlreadyRanked = new HashSet();
	int k=0;
	for (Iterator i=dist.orderedIterator(); i.hasNext(); ) {
	    ++k;
	    GraphId id = (GraphId)i.next();
	    double w = dist.getLastWeight()/dist.getTotalWeight();
	    String tag = "?";
	    if (posLabelSet!=null) tag = posLabelSet.contains(id) ? "+" : "-";
	    // figure out if this has been ranked before 
	    String uniqCode = null;
	    if (uniqCodeMap!=null) uniqCode = (String)uniqCodeMap.get(id);
	    if (uniqCode==null) uniqCode = id.toString();
	    if (uniqCodeOfIdsAlreadyRanked.contains(uniqCode)) tag = tag+"Dup";
	    uniqCodeOfIdsAlreadyRanked.add(uniqCode);
	    // print the entry if it's positive, or if it's near the top
	    if (k<numToShowAllEntries || tag.startsWith("+")) {
		buf.append(k+"\t"+fmt.format(w)+"\t"+tag+"\t"+id+"\n");
		// if (showExplanations) buf.append(walker.explain(id));
	    }
	}
	// now print the false negatives - ie the unranked positives
	if (posLabelSet!=null) {
	    for (Iterator i=posLabelSet.iterator(); i.hasNext(); ) {
		GraphId id = (GraphId)i.next();
		if (dist.getWeight(id)==0) {
		    String tag = "+";
		    String uniqCode = (String)uniqCodeMap.get(id);
		    if (uniqCode==null) uniqCode = id.toString();
		    if (uniqCodeOfIdsAlreadyRanked.contains(uniqCode)) tag = tag+"Dup";
		    buf.append(">"+k+"\t0\t"+tag+"\t"+id+"\n");
		}
	    }
	}
	return buf.toString();
    }


    public String summarize(Distribution ranking)
    {
	return summarize(ranking,false);
    }

    /**
     * Summarize a potential answer to a query, with a printout of
     * average precision and max F1.  If 'alsoShowWithoutDups' is true
     * then show with and without dups.
     */
    public String summarize(Distribution ranking,boolean alsoShowWithoutDups)
    {
	StringBuffer buf = new StringBuffer();
	Evaluation withDups = evaluateSearchResult( ranking, false );
	String tag = alsoShowWithoutDups?"/+dups":"";
	buf.append(summarize(withDups,tag));
	if (alsoShowWithoutDups) {
	    Evaluation noDups = evaluateSearchResult( ranking, true );
	    buf.append(summarize(noDups,"/-dups"));
	}
	return buf.toString();
    }

    public static String summarize(Evaluation eval, String tag)
    {
	StringBuffer buf = new StringBuffer();
	buf.append("avgPrec"+tag+":   "+eval.averagePrecision()+"\n");
	buf.append("ap*recall"+tag+": "+adjustedAveragePrecision(eval)+"\n");
	buf.append("maxF1"+tag+":     "+eval.maxF1(0)+"\n");
	return buf.toString();
    }

    public static double adjustedAveragePrecision(Evaluation eval)
    {
	return eval.averagePrecision()*eval.recall();
    }

    public String toString() 
    {
	return "[GSExample: "+queryString+"]"; 
    }

    //
    // iterator for GraphSearchExample's.
    //
    public static class Looper extends AbstractLooper {
	public Looper(Iterator i) { super(i); }
	public Looper(Collection c) { super(c); }
	public GraphSearchExample nextExample() { return (GraphSearchExample)next(); }
    }
}
