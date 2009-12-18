/**
 * 
 */
package ghirl.test;

import static org.junit.Assert.*;

import java.io.File;


import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.PersistantGraphSleepycat;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
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
		graph = new PersistantGraphSleepycat(DBDIR,'r');
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
}
