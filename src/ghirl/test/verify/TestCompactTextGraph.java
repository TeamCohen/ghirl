/**
 * 
 */
package ghirl.test.verify;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import ghirl.graph.Closable;
import ghirl.graph.CompactGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.GraphId;
import ghirl.graph.MutableGraph;
import ghirl.graph.TextGraph;
import ghirl.test.GoldStandard;
import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

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
	private static final Logger logger = Logger.getLogger(TestCompactTextGraph.class);
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
	
	@Test
	public void testAbsentNodes() throws Exception {
		graph = load();
		
		// first, the bug we saw: filtering the result of a random
		// walk sometimes makes you ask odd questions about term nodes
		// (term nodes are not stored in compact graphs)
		Distribution d = graph.asQueryDistribution("inferring");
		assertTrue(d.size()>0); 
		Distribution e = new TreeDistribution();
		for (Iterator it=d.iterator(); it.hasNext();) {
			GraphId node = (GraphId) it.next();
			e.addAll(1.0,graph.walk1(node));
		}assertTrue(e.size()>0);
		for (Iterator it=e.iterator(); it.hasNext();) {
			GraphId node = (GraphId) it.next();
			logger.info("Checking node "+node.toString());
			assertTrue(graph.contains(node));
			Set s = graph.followLink(node, "notalink");
			assertNotNull(s);
			assertEquals("Nobody should have a link 'notalink'",0,s.size());
		}
		
		// Now the exhaustive test of everything that could encounter an
		// invalid node or link:
		GraphId notanode = GraphId.fromString("$notanode");
		Set s = graph.getEdgeLabels(notanode);
		assertNotNull(s); assertEquals(0,s.size());
		s = graph.followLink(notanode, "anything");
		assertNotNull(s); assertEquals(0,s.size());
		d = graph.walk1(notanode);
		assertNotNull(d); assertEquals(0,d.size());
	}

}
