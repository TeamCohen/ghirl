/**
 * 
 */
package ghirl.test.verify;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import ghirl.graph.Closable;
import ghirl.graph.CommandLineUtil;
import ghirl.graph.CompactGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.NullGraph;
import ghirl.graph.TextGraph;
import ghirl.graph.WeightedTextGraph;
import ghirl.graph.WeightedWalker;
import ghirl.test.GoldStandard;
import ghirl.util.Distribution;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * NB: You must run this test from the tests/ directory, not from the project root.
 * @author katie
 *
 */
public class TestCompactTextGraph {
	protected static String DBNAME = "testCompactGraph";
	protected static String TESTROOT = "tests";
	protected static String COMPACTSRC = "tests/TestCompactTextGraph";
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ghirl.util.Config.setProperty("ghirl.dbDir", TESTROOT);
	}
	
	Graph graph;
	@After
	public void shutDown() {
		if (graph != null) ((Closable) graph).close();
	}
	
	public String enDir(String name) {
		return COMPACTSRC + File.separatorChar + name;
	}
	
	public Graph load() throws Exception {
		Logger.getRootLogger().setLevel(Level.INFO);
		TextGraph graph = (TextGraph) GraphFactory.makeGraph("-bshgraph","tests/compact-loader.bsh");
		
		((MutableGraph)graph).freeze();
		((TextGraph)graph).close();
		
		File link, node, walk, size;
		link = new File(enDir("graphLink.pct"));
		node = new File(enDir("graphNode.pct"));
		walk = new File(enDir("graphRow.pct"));
		size = new File(enDir("graphSize.pct"));
		
		CompactGraph cgraph = new CompactGraph();
		cgraph.load(size, link, node, walk);
		graph = new TextGraph(DBNAME, cgraph);
		return graph;
	}

	/**
	 * Test method for {@link ghirl.graph.CompactGraph#load(java.io.File, java.io.File, java.io.File, java.io.File)}.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testLoad() throws Exception {
		graph = load();
		
		GoldStandard gold = new GoldStandard(GoldStandard.GRAPH);
		gold.assertMatchesGoldStandard(gold.queryGraph(graph));
//		Distribution resdist = gold.queryGraph(graph);
//		String test = resdist.copyTopN(20).format();
//		assertTrue("Must have at least 20 items; only has "+resdist.size(),resdist.size() >= 20);
//		System.err.println("Top 20:\n"+test);
		
		//weightTest(test,gold);
		//this.lineByLineTest(test, gold);
	}

	@Test
	public void testNNodes() throws Exception {
		graph = load();
		int i=0;
		for(Iterator it=graph.getNodeIterator(); it.hasNext(); ) {
			System.out.println(it.next()); 
			i++;
		}
		assertEquals("modules for inferring names and ontological relationships etc",
				graph.getTextContent(GraphId.fromString("TEXT$m3ac")));
	}
	/*
	public void weightTest(String test, GoldStandard gold) throws IOException {
		HashMap<String,Double> answers = new HashMap<String,Double>();
		BufferedReader goldreader = new BufferedReader(new StringReader(gold.getFormattedGoldStandard()));
		String g;
		while ( (g=goldreader.readLine()) != null) {
			String[] part = g.split("\\s+");
			answers.put(part[1], new Double(Double.parseDouble(part[0])));
		}
		BufferedReader testreader = new BufferedReader(new StringReader(test));
		String t;
		while ( (t = testreader.readLine()) != null) {
			String[] part = t.split("\\s+");
                        double w = Double.parseDouble(part[0]);
                        assertEquals(w, answers.get(part[1]).doubleValue(), 0.05);
		}
	}
	
	public void lineByLineTest(String test, GoldStandard gold) throws IOException {
		BufferedReader testreader, goldreader;
		testreader = new BufferedReader(new StringReader(test));
		goldreader = new BufferedReader(new StringReader(gold.getFormattedGoldStandard()));
		String t,g;
		while (true) {
			t = testreader.readLine(); g = goldreader.readLine();
			assertEquals(g,t);
			if (t==null && g==null) break;
		}
	}*/

}
