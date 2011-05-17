package ghirl.test.verify;

import java.util.Iterator;

import ghirl.graph.BasicGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.util.Config;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Verifies fix of the following bug:<pre>
 * """
In addition to generating and isa edge from TEXT$foo to TEXT ghirl
also generates an edge from $foo to $ (a normal node with an empty
name), which seems like even more of a problem.  Here's an example

bash-3.2$ cat test-ghirl.txt
edge related $this $that
bash-3.2$ java ghirl.graph.GraphLoader test test-ghirl.txt
bash-3.2$ java ghirl.graph.TextUI -graph test -query 'this'
[0.0/0.0sec] converting 'this' to initDist...
[0.0/0.0sec] converted
[0.0/0.0sec] preparing walker...
[0.0/0.0sec] executing an exhaustive walk...
[0.0/0.0sec] returning WeightedTextGraph...
[0.0/0.0sec] search 'this' time: 0.0080 sec
62.500  $this
25.000  $that

bash-3.2$ java ghirl.graph.TextUI -graph test -dump
[0.0/0.0sec] iterating over graph nodes
[0.0/0.0sec] $: [TreeDist:]
[0.0/0.0sec] $that: [TreeDist: $this:1.0 $:1.0]
[0.0/0.0sec] $this: [TreeDist: $that:1.0 $:1.0]
usage: warning - no query specified

Clearly there should be no edge between $this and $.  There be no edge between 
TEXT$this and TEXT by default, but we should have a ghirl.properties option that
switches from the old semantics to a simpler one.
 * """</pre>
 * 
 * @author katie
 *
 */
public class TestGraphLoaderFlavorBug {
	@AfterClass
	public static void tearAllDown() {
		Config.setProperty(Config.ISAFLAVORLINKS, "");
		Config.getProperties().remove(Config.ISAFLAVORLINKS);
	}
	@Test
	public void testFlavorIsaDefaults() {
		TestableGraphLoader loader = new TestableGraphLoader(new BasicGraph());
		Graph g = loader.getGraph();
		
		int n = lookupAndGetCount("TEXT$foo",g,loader);
		assertEquals("Wrong number of nodes in the graph: ", 1, n);
		
		n = lookupAndGetCount("$bar",g,loader);
		assertEquals("Wrong number of nodes in the graph: ", 2, n);
	}
	@Test
	public void testFlavorIsaOn() {
		Config.setProperty(Config.ISAFLAVORLINKS, "true");
		TestableGraphLoader loader = new TestableGraphLoader(new BasicGraph());
		Graph g = loader.getGraph();
		
		int n = lookupAndGetCount("TEXT$foo",g,loader);
		assertTrue("Must contain flavor root 'TEXT'", g.contains(GraphId.fromString("TEXT")));
		assertEquals("Wrong number of nodes in the graph: ", 2, n);
		
		n = lookupAndGetCount("$bar",g,loader);
		assertEquals("Wrong number of nodes in the graph: ", 3, n);
	}
	@Test
	public void testLoadLine() {

		TestableGraphLoader loader = new TestableGraphLoader(new BasicGraph());
		Graph g = loader.getGraph();
		assertTrue(loader.loadLine("edge isa 3280976 paper"));
	}
	private int lookupAndGetCount(String node, Graph g, TestableGraphLoader loader) {
		GraphId id = loader.lookupNode(node);
		assertTrue(g.contains(id));
		int n=0;
		for(Iterator it = g.getNodeIterator(); it.hasNext(); ) { it.next(); n++; }
		return n;
	}
	
	private class TestableGraphLoader extends GraphLoader {
		public TestableGraphLoader(MutableGraph g) {super(g);}
		@Override public GraphId lookupNode(String s) { return super.lookupNode(s); }
	}
 }
