/**
 * 
 */
package ghirl.test.verify;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import ghirl.graph.Closable;
import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.PersistantGraph;
import ghirl.graph.PersistantGraphSleepycat;
import ghirl.graph.TextGraph;
import ghirl.util.Config;
import ghirl.util.FilesystemUtil;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestTextGraph { //extends BasicGraphTest {
	protected static final Logger logger= Logger.getLogger(TestTextGraph.class);
	protected static String DBDIR = "tests";
	protected static String DBNAME = "testTextGraph/testTextGraph";
	protected static String CLEANUPDIR = "tests/testTextGraph";
	protected static int N_EDGE_LABELS = 7;
	protected static int N_NODES = 10;
	// Node number calculation:
	// 1 TEXT$loremipsom
	// 2 TERM$lorem
	// 3 TERM$ipsum
	// 4 TERM$dolor
	// 5 TERM$sit
	// 6 TERM$amet
	// 7 TERM$loremipsum
	// 8 $puppy
	// 9 $pet
	//10 $dogfood
	protected Graph graph;
	public static File testhome;
	
	public static void setUpLogger() {
		Logger.getRootLogger().removeAllAppenders();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG); 
		Logger.getLogger("ghirl.graph.GraphLoader").setLevel(Level.INFO);
		logger.debug("Set up logger.");
	}
	
	public void loadGraphText(MutableGraph g) {
		logger.info("TestTextGraph loader:");
		GraphLoader loader = new GraphLoader(g);
		loader.invertLinks = false; // only put in what we tell it
		loader.loadLine("node TEXT$loremipsum lorem ipsum dolor sit amet");
		loadGraphGhirl(g);
	}
		
	public void loadGraphGhirl(MutableGraph g) {	
		GraphLoader loader = new GraphLoader(g);
		loader.invertLinks = false; // only put in what we tell it
		logger.debug("Adding an edge");
		loader.loadLine("edge isa  puppy pet");
		loader.loadLine("edge eats puppy dogfood");
	}
	
	@BeforeClass
	public static void setAllUp() {
		setUpLogger();
		Config.setProperty("ghirl.dbDir", DBDIR);
		testhome = new File(CLEANUPDIR);
		if (testhome.exists()) {
			FilesystemUtil.rm_r(testhome);
			fail("Test home wasn't cleaned up -- run again.");
		}
		testhome.mkdir();
	}
	@AfterClass
	public static void tearAllDown() {
		logger.info("*********** TEARING DOWN...");
		FilesystemUtil.rm_r(testhome);
	}

	@After
	public void tearDown() {
		logger.info("********** CLOSING GRAPH...");
		((Closable)graph).close();
	}
	
	@Test
	public void writeableTest() {
		logger.info("*************************** STARTING W-MODE TEST");
		graph = new MutableTextGraph(DBNAME,'w');
		loadGraphText(((MutableGraph)graph));
		((MutableGraph)graph).freeze();
		((Closable)graph).close();
		graph = new TextGraph(DBNAME);
	}
	
	@Test
	public void appendableTest() {
		logger.info("*************************** STARTING A-MODE TEST");
		graph = new MutableTextGraph(DBNAME,'a');
		((Closable)graph).close();
		graph = new TextGraph(DBNAME);
	}
	
	@Test
	public void readableTest() {
		logger.info("*************************** STARTING R-MODE TEST");
		graph = new MutableTextGraph(DBNAME,'r');
		((Closable)graph).close();
		graph = new TextGraph(DBNAME);
	}
	
	@Test
	public void overwriteTest() {
		logger.info("*************************** STARTING OVERWRITE TEST");
		graph = new MutableTextGraph(DBNAME,'w');
		loadGraphText(((MutableGraph)graph));
		((MutableGraph)graph).freeze();
		((Closable)graph).close();
		graph = new TextGraph(DBNAME);
	}
	
	@Test
	public void memorygraphTest() {
		logger.info("*************************** STARTING MEMORYGRAPH TEST");
		graph = new MutableTextGraph();
		loadGraphText(((MutableGraph)graph));
		((MutableGraph)graph).freeze();
		checkNumbers("written in memory:");
		((Closable)graph).close();
	}
	
	/** Tests getNodeIterator(), getOrderedIds(), getOrderedEdgeLabels(),
	 * PersistantGraphSleepycat.loadCache()
	 * 
	 */
	@Test
	public void verifyContents() {
		logger.info("*************************** VERIFYING GRAPH CONTENTS");
		graph = new MutableTextGraph(DBNAME,'w');
		assertEquals("Overwritten graph should be empty:",0,getIteratorNodecount(graph));
		((MutableGraph)graph).melt();
		loadGraphText(((MutableGraph)graph));
		((MutableGraph)graph).freeze();
		checkNumbers("written:");
		
		PersistantGraph innerGraph;
		String innerGraphName = DBNAME+"_db";
		
		// check they're still there in a TextGraph (basic way to read a graph)
		((Closable)graph).close();
		innerGraph = new PersistantGraphSleepycat(innerGraphName,'r');
		innerGraph.loadCache();
		graph = new TextGraph(DBNAME, innerGraph);
		checkNumbers("remaining after close and reopen:");
		
		// check they're still there in a read-only Mutable
		((Closable)graph).close();
		innerGraph = new PersistantGraphSleepycat(innerGraphName,'r');
		innerGraph.loadCache();
		graph = new MutableTextGraph(DBNAME,'r', innerGraph);
		checkNumbers("remaining after close and reopen:");
		
		// check they're still there in an appendable Mutable
		((Closable)graph).close();
		innerGraph = new PersistantGraphSleepycat(innerGraphName,'a');
		innerGraph.loadCache();
		graph = new MutableTextGraph(DBNAME,'a', innerGraph);
		checkNumbers("remaining after close and reopen:");
	}
	
	public void checkNumbers(String msg) {

		assertEquals("Nodes (iterator) "+msg, N_NODES, getIteratorNodecount(graph));
		assertEquals("Nodes (ordered)  "+msg, N_NODES, getOrderedNodecount(graph));
		assertEquals("Edges labels "+msg, N_EDGE_LABELS, graph.getOrderedEdgeLabels().length);
	}
	
	@Test
	public void verifyContentsInDetail() {
		Set<String> masterNodeList = new HashSet<String>();
		Collections.addAll(masterNodeList,
									   "$puppy",
									   "$pet",
									   "$dogfood",
									   "TERM$lorem",
									   "TERM$ipsum",
									   "TERM$dolor",
									   "TERM$sit",
									   "TERM$amet",
									   "TEXT$loremipsum",
									   "TERM$loremipsum");
		logger.info("*************************** VERIFYING GRAPH CONTENTS");
		
		Set<String> masterEdgeList = new HashSet<String>();
		Collections.addAll(masterEdgeList, "isa",
				                            "eats",
				                            "_annotates",
				                            "_hasSpan",
				                            "_hasSpanType",
				                            "_hasTerm",
				                            "_inFile");
		graph = new MutableTextGraph(DBNAME,'w');
		assertEquals("Overwritten graph should be empty:",0,getIteratorNodecount(graph));
		((MutableGraph)graph).melt();
		loadGraphText(((MutableGraph)graph));
		((MutableGraph)graph).freeze();
		// check nodes
		int i=0;
		for (Iterator it=graph.getNodeIterator(); it.hasNext(); i++) {
			String s= it.next().toString();
			assertTrue("Graph contains illegal node "+s,masterNodeList.contains(s));
		}
		assertEquals(N_NODES,i);
		// check edges
		i=0; System.out.println("Labels:");
		for (String label : graph.getOrderedEdgeLabels()) {
			i++;
			assertTrue("Graph contains illegal edge "+label,masterEdgeList.contains(label));
		}
		assertEquals(N_EDGE_LABELS,i);

		PersistantGraph innerGraph;
		String innerGraphName = DBNAME+"_db";
		
		((Closable)graph).close();
		innerGraph = new PersistantGraphSleepycat(innerGraphName,'a');
		innerGraph.loadCache();
		graph = new TextGraph(DBNAME, innerGraph);
		for (GraphId id: graph.getOrderedIds()) {
			assertTrue("After reopen, graph should NOT contain "+id.toString(),masterNodeList.remove(id.toString()));
		}
		for (String s: masterNodeList) System.out.println(s);
		assertEquals("Nodes missing from graph after reopen:",0,masterNodeList.size());
		assertEquals("Nodes in graph after close and reopen:", N_NODES, getIteratorNodecount(graph));
		assertEquals("Edges labels after close and reopen:", N_EDGE_LABELS, graph.getOrderedEdgeLabels().length);
	}
	

	public int getIteratorNodecount(Graph g) {
		int i=0;
		for(Iterator it=g.getNodeIterator(); it.hasNext(); it.next()) i++;
		return i;
	}
	public int getOrderedNodecount(Graph g) {
		return g.getOrderedIds().length;
	}
}
