package ghirl.test.verify;


import java.io.File;


import ghirl.graph.Closable;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.TextGraph;
import ghirl.util.Config;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGraphFactory {
	private static final Logger logger = Logger.getRootLogger();
	Graph g;
	
	@BeforeClass
	public static void setAllUp() {
		Config.setProperty(Config.DBDIR, "tests/TestGraphFactory");
		Config.setProperty(TextGraph.CONFIG_PERSISTANCE_PROPERTY, "ghirl.graph.PersistantGraphTokyoCabinet");
	}
	
	@After
	public void tearDown() {
		if (g != null && g instanceof Closable) ((Closable) g).close();
	}
	
	public void checkLibaryPath() {
		assertTrue("You must set java.library.path for TokyoCabinet to run this test.",System.getProperty("java.library.path")!=null);
	}
	
	@Test
	public void memoryGraph() {
		logger.info("****************** TEST MEMORYGRAPH ***********************");
		g = GraphFactory.makeGraph("-memorygraph");
		g = GraphFactory.makeGraph("-memorygraph -load graphFactory.ghirl".split(" "));
	}
	
	@Test
	public void retroText() {
		checkLibaryPath();
		logger.info("****************** TEST GRAPH (TEXT) ***********************");
		g = GraphFactory.makeGraph("-graph db -w -load graphFactory.ghirl".split(" "));
		((Closable) g).close();
		g = GraphFactory.makeGraph("-graph db -r".split(" "));
	}
	
	@Test
	public void retroBsh() {
		checkLibaryPath();
		logger.info("****************** TEST GRAPH (BSH) ***********************");
		g = GraphFactory.makeGraph("-graph graphFactory.bsh".split(" "));
	}
	
	@Test
	public void bshGraph() {
		checkLibaryPath();
		logger.info("****************** TEST BSHGRAPH ***********************");
		g = GraphFactory.makeGraph("-bshgraph graphFactory.bsh -r".split(" "));
		((Closable)g).close();
		g = GraphFactory.makeGraph("-bshgraph graphFactory.bsh -a -load tag.ghirl".split(" "));
		assertTrue(g != null);
		assertTrue(g instanceof Closable);
	}
}
