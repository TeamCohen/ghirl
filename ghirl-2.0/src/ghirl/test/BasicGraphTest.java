package ghirl.test;

import static org.junit.Assert.assertEquals;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;

import java.io.File;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;

/**
 * Wrapper for testing Graph implementations including default setup, teardown, and method-testing.
 * 
 * Augh -- and I don't mean (BasicGraph) (Test), I mean (Basic) (GraphTest), sorry.
 * @author katie
 *
 */
public abstract class BasicGraphTest {
	private static final Logger logger = Logger.getLogger(BasicGraphTest.class);
	protected static String DBDIR;
	protected static final int DEFAULT_IDS = 3;
	protected static final int DEFAULT_EDGES = 2;
	MutableGraph graph;
	
	/** Initialize graph for writing to disk */
	public abstract void setUp(); 
	/** Re-initialize graph for reading from disk */
	public abstract void reset();
	
	@BeforeClass
	public static void setUpBeforeClass() {
		Logger.getRootLogger().removeAllAppenders();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG); 
		Logger.getLogger("ghirl.graph.GraphLoader").setLevel(Level.INFO);
		logger.debug("Set up logger.");
	}

	/**
	 * Adds a standardized set of nodes and edges which match the numbers 
	 * DEFAULT_IDS and DEFAULT_EDGES.
	 */
	public void loadGraph() {
		GraphLoader loader = new GraphLoader(graph);
		loader.invertLinks = false; // only put in what we tell it
		logger.debug("Adding an edge");
		loader.loadLine("edge isa  puppy pet");
		loader.loadLine("edge eats puppy dogfood");
		graph.freeze();
	}
	
	/**
	 * Recursively delete files in a directory until the directory is empty, 
	 * then delete it.  Call this in your @AfterClass teardown method.
	 * @param f File to delete
	 */
	public static void rm_r(File f) {
		if (f.isDirectory())
			for (File g : f.listFiles()) rm_r(g);
		f.delete();
	}
	
	/** 
	 * Test method for {@link ghirl.graph.Graph#getOrderedEdgeLabels} for both a
	 *  freshly written and a freshly loaded graph, asserting the default number
	 *  of edge labels. 
	 */
	public void testGetOrderedEdgeLabels() { testGetOrderedEdgeLabels(2); }
	/**
	 * Test method for {@link ghirl.graph.Graph#getOrderedEdgeLabels} for both a
	 *  freshly written and a freshly loaded graph, asserting the specified 
	 *  number of edge labels.
	 * @param nlabels
	 */
	public void testGetOrderedEdgeLabels(int nlabels) {
		// test cached edges
		String[] labs = graph.getOrderedEdgeLabels();
		assertEquals(nlabels,labs.length);
		reset();
		// test read-from-graph edges
		labs = graph.getOrderedEdgeLabels();
		if (nlabels != labs.length) {
			System.out.println("Labels read: ");
			for (String lab : labs) System.out.println(labs);
		}
		assertEquals(nlabels,labs.length);
	}
	/** 
	 * Test method for {@link ghirl.graph.Graph#getOrderedIds} for both a
	 *  freshly written and a freshly loaded graph, asserting the default number
	 *  of ids. 
	 */
	public void testGetOrderedIds() { testGetOrderedIds(3); }
	/**
	 * Test method for {@link ghirl.graph.Graph#getOrderedIds} for both a
	 *  freshly written and a freshly loaded graph, asserting the specified
	 *  number of ids. 
	 * @param nnodes
	 */
	public void testGetOrderedIds(int nnodes) {
		// test cached nodes
		GraphId[] nodes = graph.getOrderedIds();
		assertEquals(nnodes,nodes.length);
		reset();
		// test read-from-graph nodes
		nodes = graph.getOrderedIds();
		assertEquals(nnodes,nodes.length);
	}

}
