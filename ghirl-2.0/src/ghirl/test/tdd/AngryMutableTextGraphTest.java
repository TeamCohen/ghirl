package ghirl.test.tdd;


import java.io.IOException;

import ghirl.graph.MutableTextGraph;
import ghirl.util.Config;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class AngryMutableTextGraphTest {
	MutableTextGraph graph;
	@BeforeClass
	public static void setAllUp() {
		Config.setProperty(Config.DBDIR, "tests");
	}
	@After
	public void setUp() throws Exception {
		graph.close();
	}
	@Test
	public void test1() throws IOException {
		graph = new MutableTextGraph(Config.getProperty(Config.GRAPHNAME),'w');
	}
	@Test
	public void test2() throws IOException {
		graph = new MutableTextGraph(Config.getProperty(Config.GRAPHNAME),'w');
	}

}
