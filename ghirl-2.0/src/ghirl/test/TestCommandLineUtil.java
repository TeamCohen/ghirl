package ghirl.test;


import ghirl.graph.CommandLineUtil;
import ghirl.graph.Graph;
import ghirl.util.Config;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
public class TestCommandLineUtil {

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testCLU() {
		Config.setProperty("ghirl.dbDir", "tests");
		Graph g = CommandLineUtil.makeGraph("fqcache-toy.bsh");
		assertTrue( g != null );
		
	}
}
