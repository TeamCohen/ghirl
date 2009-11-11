package ghirl.graph;

import java.util.*;
import java.io.*;
import ghirl.util.*;
import edu.cmu.minorthird.util.ProgressCounter;
import edu.cmu.minorthird.text.*;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;


public class TrieAnnotator extends GraphAnnotator
{
    /** Uses the SoftTFIDF measure as the StringDistanceLearner,
     * and trains it on the supplied synonym-list file.
     *
     * @param synonymListFile contains "training data" (sample
     * strings to compute TF stats) for the SoftDictionary.  Each line
     * is a tab-separated list of sample strings, the first of which
     * is an identifier (which is ignored).
     */
    public TrieAnnotator(String linkLabel,String precondition,File synonymListFile)
    {
	super(linkLabel,precondition,new TrieSearcher(synonymListFile));
    }

    private static class TrieSearcher extends AbstractGraphSearcher
    {
	private Trie trie;
	private Graph graph;

	public TrieSearcher(File synonymListFile)
	{
	    try {
		trie = buildTrie(synonymListFile);
	    } catch (IOException ex) {
		throw new IllegalArgumentException("can't open file "+synonymListFile+": "+ex);
	    }
	}

	private Trie buildTrie(File synonymListFile) throws IOException
	{
	    BasicTextBase textBase = new BasicTextBase();
	    System.err.println("loading synonyms from "+synonymListFile+"...");
	    LineNumberReader in = new LineNumberReader(new FileReader(synonymListFile));
	    String line = null;
	    ProgressCounter pc = new ProgressCounter("loading "+synonymListFile,"lines");
	    while ((line = in.readLine())!=null) {
		String[] parts = line.split("\\t+");
		String normalIdName = parts[0].replace(':','_');
		for (int i=1; i<parts.length; i++) {
		    String[] tokens = textBase.getTokenizer().splitIntoTokens(parts[i]);
		    trie.addWords( normalIdName, tokens );
		}
		pc.progress();
	    }
	    pc.finished();
	    in.close();
	    return trie;
	}

	public void setGraph(Graph graph) { this.graph = graph; }
	public Graph getGraph() { return graph; }

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

	// search for strings in a file that match a trie
	public Distribution search(GraphId fromId)
	{
	    BasicTextLabels labels = new BasicTextLabels( graph.getTextContent(fromId) );
	    Span contentSpan = labels.getTextBase().documentSpanIterator().nextSpan();
	    Distribution dist = new TreeDistribution();
	    for (Trie.ResultLooper i = trie.lookup(contentSpan); i.hasNext(); ) {
		Span match = i.nextSpan();
		List ids = i.getAssociatedIds();
		for (Iterator j=ids.iterator(); j.hasNext(); ) {
		    String toIdName = (String)j.next();  
		    dist.add( 1.0, GraphId.fromString(toIdName) );
		}
	    }
	    return dist;
	}
    }
}
