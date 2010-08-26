package ghirl.test.verify;


import java.io.File;

import ghirl.graph.BatchQueryRunner;
import ghirl.graph.Closable;
import ghirl.graph.Graph;
import ghirl.graph.GraphFactory;
import ghirl.graph.MutableGraph;
import ghirl.graph.TextGraph;
import ghirl.util.Config;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/** I totally deleted these files by accident. :-/  FIXME */
@Ignore
public class TestBatchQueryRunner {
	static File testdir;
	BatchQueryRunner runner;
	
	@BeforeClass
	public static void setAllup() {
		testdir = new File("tests/TestBatchQueryRunner");
		Config.setProperty(Config.DBDIR, testdir.getPath());
		Config.setProperty(TextGraph.CONFIG_PERSISTANCE_PROPERTY,"ghirl.graph.PersistantGraphTokyoCabinet");
	}
	
	@Before
	public void setup() {
		runner = new BatchQueryRunner();
	}
	@After
	public void tearDown() {
		Graph g = runner.getGraph();
		if (g instanceof Closable) ((Closable) g).close();
	}
	
	@Test
	public void simpleTest() {
		runner.setQueryFile(new File(testdir,"query.txt"));
		runner.setOutFile(new File(testdir,"out.txt"));
		runner.setGraph(GraphFactory.makeGraph("-textgraph","db","-a","-load","tests/graph.txt"));
		runner.run();
	}
	
	@Test
	public void longerTest() {
		assertTrue("FBRF graph must be loaded into "+testdir.getPath()+" separately with stem 'fbrf'.", new File(testdir,"fbrf_lucene.index").exists());
		runner.setQueryFile(new File(testdir, "fbrfquery.txt"));
		runner.setOutFile(new File(testdir,"fbrfout.txt"));
		runner.setGraph(GraphFactory.makeGraph("-textgraph","fbrf","-r"));
		runner.run();
	}
	
	@Test
	public void walkTest() {
		runner.setQueryFile(new File(testdir, "fbrfquery.txt"));
		runner.setOutFile(new File(testdir,"fbrfout.txt"));
		runner.setResultsProcessorBshfile(new File(testdir, "fbrfprocessor.bsh"));
		runner.setGraph(GraphFactory.makeGraph("-textgraph","fbrf","-r"));
		runner.run();
	}
	
	@Test
	public void argsTest() {
		runner = new BatchQueryRunner(
				"-query","tests/TestBatchQueryRunner/query.txt",
				"-graph","\"-textgraph db -r\"");
		runner.run();
	}
	
	@Test
	public void verboseTest() {
		runner = new BatchQueryRunner(
				"-query", "tests/TestBatchQueryRunner/query.txt",
				"-graph", "\"-textgraph db -r\"",
				"-v", "tests/TestBatchQueryRunner/verboseResults.txt");
	}
}
