package ghirl.test;

import static org.junit.Assert.*;
import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.WeightedTextGraph;
import ghirl.graph.WeightedWalker;
import ghirl.util.Config;
import ghirl.util.Distribution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/** This is a utility class for checking graph query results against the 
 * gold standard graph (MutableTextGraph/BasicGraph) or against the
 * the standard resultset which is saved in tests/output-goldstandard.txt.
 * 
 * To use, create a GoldStandard based on either a live graph or a textfile,
 * then use matchesGoldStandard(Graph g) with your test graph to see if it
 * matches.
 * 
 * More complex use cases are possible by breaking apart the gold standard
 * query process or by checking the gold standard answers directly as a Map or
 * as formatted text.
 * 
 * @author katie
 *
 */
public class GoldStandard {
	private static final String QUERY = "william";
	public static final int TEXTFILE=0;
	public static final int GRAPH=1;
	public static final double TOLERANCE = 0.0001d;
	public static final String FILENAME_GOLD_NOINV="tests/output-goldstandard.txt";
	public static final String FILENAME_GOLD_INV="tests/output-goldstandard-inv.txt";
	
	private String formattedAnswers;
	private HashMap<String,Double> answers;
	
	/** Create a Gold Standard object backed by either a textfile or a MutableTextGraph. */
	public GoldStandard() { this(TEXTFILE); }
	public GoldStandard(int standard) {
		answers = new HashMap<String,Double>();
		try {
			switch(standard) {
			case TEXTFILE: initText(); break;
			case GRAPH:   initGraph(); break;
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void initText() throws IOException {
		StringBuilder b = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader(FILENAME_GOLD_NOINV));
		String read;
		while ( (read=in.readLine()) != null) {
			b.append("\n");
			b.append(read); if ( (read=read.trim()) .equals("")) continue;
			String[] part = read.split("\\s+");
			answers.put(part[1], new Double(Double.parseDouble(part[0])));
		}
		formattedAnswers = b.substring(1);
	}
	private void initGraph() throws IOException {
		String oldDbDir = Config.getProperty(Config.DBDIR);
		Config.setProperty(Config.DBDIR, "tests");
		GraphLoader loader = new GraphLoader(new MutableTextGraph());
		loader.load(new File("tests/graph.txt"));
		Distribution d = queryGraph(loader.getGraph());
		formattedAnswers = d.copyTopN(20).format();
		for(Iterator it=d.iterator(); it.hasNext();) {
			GraphId node = (GraphId) it.next();
			answers.put(node.toString(),d.getLastWeight());
		}
	}

	/** Get the gold standard as a formatted string of the top 20 results.  
	 * If you want to check against this string yourself, use 
	 * <pre>
	 * resultsDistribution.copyTopN(20).format();
	 * </pre>
	 * to generate the string for the test graph.
	 * @return
	 */
	public String getFormattedGoldStandard() {
		return formattedAnswers;
	}
	
	/** Get the gold standard as a map from node names (node.toString()) to 
	 * weights (encoded as double)
	 * @return
	 */
	public Map<String,Double> getMapGoldStandard() {
		return answers;
	}
	/** Return whether the test graph matches the gold standard graph (or recorded results)
	 * for the gold standard query.
	 * @param g The graph to be tested.
	 * @return
	 */
	public boolean matchesGoldStandard(Graph g) {
		return matchesGoldStandard(queryGraph(g));
	}
	/** Return whether the distribution of results matches the gold standard.
	 * 
	 * @param d Distirubtion of gold standard query results, as returned from GoldStandard.queryGraph()
	 * @return
	 */
	public boolean matchesGoldStandard(Distribution d) {
		return matchesGoldStandard(d,false);
	}
	/** Test the distribution of results, and fail with an assertion error
	 * specifying details if the distribution does not match the gold standard.
	 * @see matchesGoldStandard(Distribution d)
	 * @param d
	 * @return
	 */
	public boolean assertMatchesGoldStandard(Distribution d) {
		return matchesGoldStandard(d,true);
	}
	private boolean matchesGoldStandard(Distribution d, boolean assertIt) {
		HashSet<String> nodes = new HashSet<String>();
		for(Iterator it=d.iterator(); it.hasNext();) {
			String node = ((GraphId) it.next()).toString();
			nodes.add(node);
			Double goldWeight = answers.get(node);
			Double testWeight = d.getLastWeight();
			if ( Math.abs(goldWeight - testWeight) > TOLERANCE ) {
				if (assertIt) {
					assertEquals("Weight of "+node+" is outside tolerance +/-"+TOLERANCE,
							 goldWeight, testWeight, TOLERANCE);
					return false;
				} else return false;
			}
		}
		// we now know that all the test set's nodes are in the answer set;
		// now we make sure that none of the answer set's nodes are missing from the test set:
		Set<String> goldNodes = answers.keySet();
		if (!nodes.containsAll(goldNodes)) {
			if (!assertIt) return false;
			for (String goldnode : goldNodes) {
				assertTrue("Must contain "+goldnode,nodes.contains(goldnode));
			}
			assertTrue("Unknown missing node!",false);
			return false;
		}
		return true;
	}
	
	/** Run the gold standard query and graphwalk on the specified graph.  The 
	 * settings used are the same as for the recorded results in output-goldstandard.txt,
	 * and the resulting distribution is valid for use in the matchesGoldStandard()
	 * methods.
	 * @param graph The graph to be tested
	 * @return
	 */
	public Distribution queryGraph(Graph graph) {
		WeightedWalker walker = new WeightedWalker();
		Distribution dist = graph.asQueryDistribution(QUERY);
		assertTrue("Must have nonzero results for query "+QUERY, dist != null && dist.size() > 0);
		walker.setGraph(graph);
		walker.setInitialDistribution(dist);
		walker.setUniformEdgeWeights();
		walker.reset();
		walker.setNumSteps(100);
		walker.setSamplingPolicy(false);
		walker.setStayWalkVersion(false);
		walker.walk();
		return new WeightedTextGraph(dist, walker.getNodeSample(), graph).getNodeDist();
	}
}
