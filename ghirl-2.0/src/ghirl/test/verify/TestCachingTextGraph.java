package ghirl.test.verify;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import ghirl.graph.CachingGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.PersistantGraph;
import ghirl.graph.PersistantGraphSleepycat;
import ghirl.graph.TextGraph;
import ghirl.test.GoldStandard;
import ghirl.util.Config;
import ghirl.util.Distribution;
import ghirl.util.FilesystemUtil;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestCachingTextGraph {
	protected static String DBNAME="toy/toy";
	protected static String CLEANUPDIR="tests/toy";
	protected static String DBDIR ="tests";
	protected TextGraph graph;
	protected static File testhome;
	@BeforeClass
	public static void setAllUp() {
		Config.setProperty("ghirl.dbDir", DBDIR);
		testhome = new File(CLEANUPDIR);
		if (testhome.exists()) {
			FilesystemUtil.rm_r(testhome);
			fail("Test home wasn't cleaned up -- run again.");
		}
		testhome.mkdir();
	}
	@After
	public void tearDown() {
		if (graph != null) graph.close();
	}

	@AfterClass
	public static void tearAllDown() {
		FilesystemUtil.rm_r(testhome);
	}
	@Test
	public void testCachingGraph() throws FileNotFoundException, IOException {
		GraphLoader loader = new GraphLoader(new MutableTextGraph(DBNAME,'w'));
		loader.load(new File("tests/graph.txt"));
		loader.getGraph().freeze();
		((TextGraph)loader.getGraph()).close();
		PersistantGraph innerGraph = new PersistantGraphSleepycat(DBNAME+"_db",'r');
		innerGraph.loadCache();
		graph = new TextGraph(DBNAME, innerGraph);
		Graph cgraph = new CachingGraph(graph);
		
		GoldStandard gold = new GoldStandard();
		Distribution resdist = gold.queryGraph(cgraph);
		
		BufferedReader testreader, goldreader;
		testreader = new BufferedReader(new StringReader(resdist.format()));
		goldreader = new BufferedReader(new StringReader(gold.getFormattedGoldStandard()));
		String t,g;
		while (true) {
			t = testreader.readLine(); g = goldreader.readLine();
			assertEquals(g,t);
			if (t==null && g==null) break;
		}
	}
}
