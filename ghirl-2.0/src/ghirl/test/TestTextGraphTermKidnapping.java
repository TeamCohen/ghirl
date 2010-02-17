package ghirl.test;

import static org.junit.Assert.assertTrue;
import ghirl.graph.Graph;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.util.Config;
import ghirl.util.Distribution;

import org.junit.Before;
import org.junit.Test;

/**
 * This class tests a bug whereby melting and refreezing a graph 
 * unintentionally removed the lucene index of terms -- but not the 
 * innerGraph record of TEXT nodes -- from the graph.
 * 
 * Guarantees:
 * - Presence of text nodes and terms after freezing
 * - Presence of text nodes and terms after melting and refreezing
 * - Presence of text nodes and terms after re-adding a copy of one text node
 *   (same shortname, same terms)
 * @author katie
 *
 */
public class TestTextGraphTermKidnapping {
	protected static String DBDIR = "tests/testTermKidnapping";
	public void assertInGraph(String s, Graph g) {
		Distribution d = g.asQueryDistribution(s);
		assertTrue(d != null);
		assertTrue(s+" should be in the graph!", d.size() > 0);
	}
	
	@Before
	public void setup() {
		Config.setProperty("ghirl.dbDir", DBDIR);
	}
	
	@Test
	public void testMemoryGraph() {
		MutableGraph g = new MutableTextGraph();
		testTKforGraph(g);
	}
	
	@Test
	public void testPersistantGraph() {
		MutableGraph g = new MutableTextGraph("db",'w');
	}
	
	public void testTKforGraph(MutableGraph g) {
		GraphLoader loader = new GraphLoader(g);
		loader.loadLine("node TEXT$foo a b c");
		loader.loadLine("node TEXT$bar d e f");
		loader.loadLine("edge isa TEXT$foo metasyntactic");
		loader.loadLine("edge isa TEXT$bar metasyntactic");
		loader.loadLine("edge hasnext TEXT$foo TEXT$bar");
		g.freeze();
		assertInGraph("TEXT$foo",g);
		assertInGraph("b",g);
		assertInGraph("TEXT$bar",g);
		assertInGraph("f",g);
		g.melt();
		g.freeze();
		assertInGraph("TEXT$foo",g);
		assertInGraph("b",g); // fail?!
		assertInGraph("TEXT$bar",g);
		assertInGraph("f",g);
		g.melt();
		loader.loadLine("node TEXT$foo a b c");
		g.freeze();
		assertInGraph("TEXT$bar",g);
		assertInGraph("f",g);
		assertInGraph("TEXT$foo",g);
		assertInGraph("b",g);
	}
}
