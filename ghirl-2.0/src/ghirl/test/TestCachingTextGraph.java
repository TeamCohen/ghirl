package ghirl.test;


import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;

import ghirl.graph.CachingGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.TextGraph;
import ghirl.util.Distribution;

import org.junit.Before;
import org.junit.Test;

public class TestCachingTextGraph {
	protected static String DBDIR="toy";
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ghirl.util.Config.setProperty("ghirl.dbDir", ".");
	}
	@Test
	public void testCachingGraph() throws FileNotFoundException, IOException {
		GraphLoader loader = new GraphLoader(new MutableTextGraph(DBDIR,'w'));
		loader.load(new File("graph.txt"));
		loader.getGraph().freeze();
		Graph graph = new CachingGraph(new TextGraph(DBDIR));
		
		GoldStandard gold = new GoldStandard();
		Distribution resdist = gold.queryGraph(graph);
		
		BufferedReader testreader, goldreader;
		testreader = new BufferedReader(new StringReader(resdist.format()));
		goldreader = new BufferedReader(new StringReader(gold.getGoldStandard()));
		String t,g;
		while (true) {
			t = testreader.readLine(); g = goldreader.readLine();
			assertEquals(g,t);
			if (t==null && g==null) break;
		}
	}
	

}
