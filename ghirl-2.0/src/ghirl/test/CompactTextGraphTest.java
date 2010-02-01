/**
 * 
 */
package ghirl.test;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import ghirl.graph.CommandLineUtil;
import ghirl.graph.CompactGraph;
import ghirl.graph.Graph;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.NullGraph;
import ghirl.graph.TextGraph;
import ghirl.graph.WeightedTextGraph;
import ghirl.graph.WeightedWalker;
import ghirl.util.Distribution;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;

/**
 * NB: You must run this test from the tests/ directory, not from the project root.
 * @author katie
 *
 */
public class CompactTextGraphTest {
	protected static String DBDIR = "testCompactGraph";
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		ghirl.util.Config.setProperty("ghirl.dbDir", DBDIR);
	}
	

	/**
	 * Test method for {@link ghirl.graph.CompactGraph#load(java.io.File, java.io.File, java.io.File, java.io.File)}.
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	@Test
	public void testLoad() throws Exception {
		Logger.getRootLogger().setLevel(Level.INFO);
		try {
			((MutableGraph) CommandLineUtil.makeGraph("compact-loader.bsh")).freeze();
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		File link, node, walk, size;
		link = new File(DBDIR+"/graphLink.pct");
		node = new File(DBDIR+"/graphNode.pct");
		walk = new File(DBDIR+"/graphRow.pct");
		size = new File(DBDIR+"/graphSize.pct");
		
		CompactGraph cgraph = new CompactGraph();
		cgraph.load(size, link, node, walk);
		Graph graph = new TextGraph(DBDIR, cgraph);
		
		GoldStandard gold = new GoldStandard();
		Distribution resdist = gold.queryGraph(graph);
		String test = resdist.copyTopN(20).format();
		assertTrue(resdist.size() >= 20);
		System.err.println("Top 20:\n"+test);
		
		weightTest(test,gold);
//		this.lineByLineTest(test, gold);
	}
	
	public void weightTest(String test, GoldStandard gold) throws IOException {
		HashMap<String,String> answers = new HashMap<String,String>();
		BufferedReader goldreader = new BufferedReader(new StringReader(gold.getGoldStandard()));
		String g;
		while ( (g=goldreader.readLine()) != null) {
			String[] part = g.split("\\s+");
			answers.put(part[1], part[0]);
		}
		

		BufferedReader testreader = new BufferedReader(new StringReader(test));
		String t;
		while ( (t = testreader.readLine()) != null) {
			String[] part = t.split("\\s+");
			assertEquals(answers.get(part[1]), part[0]);
		}
	}
	
	public void lineByLineTest(String test, GoldStandard gold) throws IOException {
		BufferedReader testreader, goldreader;
		testreader = new BufferedReader(new StringReader(test));
		goldreader = new BufferedReader(new StringReader(gold.getGoldStandard()));
		String t,g;
		while (true) {
			t = testreader.readLine(); g = goldreader.readLine();
			assertEquals(g,t);
			if (t==null && g==null) break;
		}
	}

}
