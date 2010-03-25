/**
 * 
 */
package ghirl.test;

import static org.junit.Assert.*;

import java.io.File;
import java.util.Iterator;


import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.PersistantGraph;
import ghirl.graph.PersistantGraphSleepycat;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestPersistantGraphSleepycat extends BasicGraphTest {
	protected static String DBDIR = "tests/testPersistantGraphSleepycat";
	
	@Before
	public void setUp() {
		graph = new PersistantGraphSleepycat(DBDIR,'w');
		loadGraph();
	}
	
	public void reset() {
		((PersistantGraph) graph).close();
		graph = new PersistantGraphSleepycat(DBDIR,'r');
		((PersistantGraph) graph).loadCache();
	}
	
	@Test
	public void testImproperSleepycatCaching() {
		// graph already exists and has been loaded
		// open a new one on top
		((PersistantGraph)graph).close();
		graph = new PersistantGraphSleepycat(DBDIR,'w');
		int i=0;
		for(Iterator it=graph.getNodeIterator(); it.hasNext(); it.next()) i++;
		
		assertEquals("Freshly-opened 'w' mode graph should be empty:",0,i);
		
		
	}
	
	/**
	 * Test method for {@link ghirl.graph.PersistantGraph#getOrderedEdgeLabels()}.
	 */
	@Test 
	public void testGetOrderedEdgeLabels() {
		super.testGetOrderedEdgeLabels();
	}

	/**
	 * Test method for {@link ghirl.graph.PersistantGraph#getOrderedIds()}.
	 */
	@Test 
	public void testGetOrderedIds() {
		super.testGetOrderedIds();
	}
	
	@AfterClass
	public static void tearAllDown() {
		Logger.getLogger(BasicGraphTest.class).info("Cleaning up test directory "+DBDIR+"...");
		rm_r(new File(DBDIR));
	}
	
	@After
	public void tearDown() {
		((PersistantGraph)graph).close();
	}
}
