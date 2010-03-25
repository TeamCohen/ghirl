package ghirl.test;


import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.PersistantGraph;
import ghirl.graph.PersistantGraphHexastore;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

public class SimpleTestPersistantGraphHexastore {
	private static final Logger logger = Logger.getLogger(SimpleTestPersistantGraphHexastore.class);
	private static final String DBDIR="tests/testPersistantGraphHexastore";
	MutableGraph graph;
	
	private void setuplogger() {
		Logger.getRootLogger().removeAllAppenders();
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.DEBUG); 
		Logger.getLogger("ghirl.graph.GraphLoader").setLevel(Level.INFO);
		logger.debug("Set up logger.");
	}
	
	private void setupgraph() {
		logger.info("System java.library.path: "+System.getProperty("java.library.path"));
		graph = new PersistantGraphHexastore(DBDIR,'w');
	}
	
	private void loadgraph() {
		logger.debug("created graph; loading stuff...");
		GraphLoader loader = new GraphLoader(graph);
		loader.invertLinks = false; // only put in what we tell it
		logger.debug("Adding an edge");
		loader.loadLine("edge isa  puppy pet");
		loader.loadLine("edge eats puppy dogfood");
	}
	@Test
	public void testPersistantGraphWrite() {
		setuplogger();
		setupgraph();
		loadgraph();
		graph.freeze();
	}
	
	@Test
	public void testPersistantGraphWriteClose() {

		setuplogger();
		setupgraph();
		loadgraph();
		graph.freeze();
		((PersistantGraph) graph).close();
	}

	
	@Test
	public void testPersistantGraphRead() {
		logger.info("System java.library.path: "+System.getProperty("java.library.path"));
		logger.info("Opening graph...");
		MutableGraph graph = new PersistantGraphHexastore(DBDIR,'r');
		
	}
}
