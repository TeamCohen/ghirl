/**
 * 
 */
package ghirl.test;

import static org.junit.Assert.*;

import java.io.File;

import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.TextGraph;
import ghirl.util.Config;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestTextGraph extends BasicGraphTest {

	protected static String DBDIR = "tests/testTextGraph";
	
	public void loadGraph() {
		GraphLoader loader = new GraphLoader((MutableGraph)graph);
		loader.invertLinks = false; // only put in what we tell it
		loader.loadLine("node TEXT$loremipsum lorem ipsum dolor sit amet");
		super.loadGraph();
	}
	
	@Before 
	public void setUp() {
		Config.setProperty("ghirl.dbDir", DBDIR);
		new File(DBDIR).mkdir();
		graph = new MutableTextGraph(DBDIR.split("/")[1],'w');
		loadGraph();
	}
	
	public void reset() {
		graph = new TextGraph(DBDIR.split("/")[1]);
	}

	/**
	 * Test method for {@link ghirl.graph.TextGraph#getOrderedIds()}.
	 */
	@Test
	public void testGetOrderedIds() {

		GraphId[] nodes = graph.getOrderedIds();
		for(int i=0; i<nodes.length; i++) {
			System.err.println(nodes[i].toString());
		}
		super.testGetOrderedIds(DEFAULT_IDS + 6);
	}

	/**
	 * Test method for {@link ghirl.graph.TextGraph#getOrderedEdgeLabels()}.
	 */
	@Test
	public void testGetOrderedEdgeLabels() {
		super.testGetOrderedEdgeLabels(DEFAULT_EDGES + 5);
	}
	
	@AfterClass
	public static void tearAllDown() {
		Logger.getLogger(BasicGraphTest.class).info("Cleaning up test directory "+DBDIR+"...");
		rm_r(new File(DBDIR));
	}
}
