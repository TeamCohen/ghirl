package ghirl.test.verify;


import java.io.File;
import java.io.IOException;
import java.util.Set;

import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.TextGraph;
import ghirl.util.Config;
import ghirl.util.Distribution;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import tokyocabinet.BDB;
import static org.junit.Assert.*;
public class TestPersistantGraphTokyoCabinet {
	private static Logger logger = Logger.getRootLogger();

	TextGraph graph;
	
	@BeforeClass
	public static void setUp() throws Exception {
		File testdir = new File("tests/TestPersistantGraphTokyoCabinet");
		if (!testdir.exists()) testdir.mkdir();
		Config.setProperty(Config.DBDIR,testdir.getPath());
		Config.setProperty(TextGraph.CONFIG_PERSISTANCE_PROPERTY,"ghirl.graph.PersistantGraphTokyoCabinet");
	}
	public static void checkLogging() {
		logger.debug("Debug");
		logger.info("Info");
		logger.warn("Warn");
		logger.error("Error");
		logger.fatal("Fatal");
	}
	
	@After
	public void tearDown() {
		if (graph != null) graph.close();
	}
	
	@Test
	public void testWrite() throws IOException {
		logger.info("****************** TEST WRITE ***********************");
		mutableTest('w');
	}
	@Test
 	public void testRead() throws IOException {
		logger.info("****************** TEST READ ***********************");
		immutableTest();
	}
	@Test
	public void testAppend() throws IOException {
		logger.info("****************** TEST APPND ***********************");
		mutableTest('a');
	}
	@Test
	public void testReadAfterAppend() throws IOException {
		logger.info("*************** TEST APPND-READ *********************");
		immutableTest();
	}
	@Test
	public void testOverWrite() throws IOException {
		logger.info("**************** TEST OVERWRITE *********************");
		mutableTest('w');
	}
	@Test
	public void testReadAfterOverWrite() throws IOException {
		logger.info("*************** TEST OVWRT-READ *********************");
		immutableTest();
	}

	public void checkLibaryPath() {
		assertTrue("You must set java.library.path for TokyoCabinet to run this test.",System.getProperty("java.library.path")!=null);
	}
	private void mutableTest(char mode) throws IOException {
		checkLibaryPath();
		graph = new MutableTextGraph("db",mode);
		MutableGraph m = (MutableGraph) graph;
		GraphLoader loader = new GraphLoader(m);
		loader.loadLine("node cat");
		loader.loadLine("edge has cat fleas");
		loader.loadLine("edge has dog fleas");
		m.freeze();
	}
	
	private void immutableTest() throws IOException {
		checkLibaryPath();
		graph = new TextGraph("db");
		Distribution d = graph.asQueryDistribution("cat");
		assertEquals(1,d.size());
		GraphId cat = (GraphId) d.iterator().next();
		assertEquals("$cat",cat.toString());
		Set s = graph.getEdgeLabels(cat);
		assertEquals(1,s.size());
		String has = (String) s.iterator().next();
		assertEquals("has",has);
		Set t = graph.followLink(cat, has);
		assertEquals(1,t.size());
		GraphId fleas = (GraphId) t.iterator().next();
		Distribution f = graph.walk1(fleas,"hasInverse");
		assertEquals(2,f.size());
	}
}
