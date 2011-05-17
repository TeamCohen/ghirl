/**
 * 
 */
package ghirl.test.verify;


import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import ghirl.graph.Closable;
import ghirl.graph.CompactGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.GraphId;
import ghirl.graph.MutableGraph;
import ghirl.graph.SparseCompactGraph;
import ghirl.graph.TextGraph;
import ghirl.test.GoldStandard;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestSparseCompactGraph {

	protected static String DBNAME = "testSparseCompactGraph";
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
		TextGraph graph = (TextGraph) GraphFactory.makeGraph("-bshgraph","tests/compact-sparse-loader.bsh");
		
		((MutableGraph)graph).freeze();
		((TextGraph)graph).close();
		
		File link, node, walk, size;
		link = new File(enDir("graphLink.pct"));
		node = new File(enDir("graphNode.pct"));
		walk = new File(enDir("graphRow.pct"));
		size = new File(enDir("graphSize.pct"));
		
		CompactGraph cgraph = new SparseCompactGraph();
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

}
