/**
 * 
 */
package ghirl.test;

import static org.junit.Assert.*;

import java.io.File;

import ghirl.graph.PersistantGraphHexastore;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

/**
 * @author katie
 *
 */
public class TestPersistantGraphHexastore extends BasicGraphTest {
	private static final Logger logger = Logger.getLogger(TestPersistantGraphHexastore.class);
	protected static String DBDIR = "tests/testPersistantGraphHexastore";
	
	@AfterClass
	public static void tearAllDown() {
		logger.info("Cleaning up test directory "+DBDIR+"...");
		rm_r(new File(DBDIR));
	}


	
	@Before
	public void setUp() {
		logger.info("System java.library.path: "+System.getProperty("java.library.path"));
		graph = new PersistantGraphHexastore(DBDIR,'w');
		logger.debug("created graph; loading stuff...");
		loadGraph();
	}

	/**
	 * Test method for {@link ghirl.graph.PersistantGraph#getOrderedEdgeLabels()}.
	 */
	@Test
	public void testGetOrderedEdgeLabels() {
		super.testGetOrderedEdgeLabels();
		graph = null;
	}

	/**
	 * Test method for {@link ghirl.graph.PersistantGraph#getOrderedIds()}.
	 */
	@Test
	public void testGetOrderedIds() {
		super.testGetOrderedIds();
		graph = null;
	}

	@Override
	public void reset() {
		graph = new PersistantGraphHexastore(DBDIR,'r');
	}

}
