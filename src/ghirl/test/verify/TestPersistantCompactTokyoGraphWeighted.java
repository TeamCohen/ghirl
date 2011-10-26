package ghirl.test.verify;

import java.util.Iterator;

import ghirl.graph.GraphId;
import ghirl.graph.PersistantCompactTokyoGraph;
import ghirl.util.Config;
import ghirl.util.Distribution;

import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestPersistantCompactTokyoGraphWeighted extends
		TestPersistantCompactTokyoGraph {
	
	@BeforeClass
	public static void setUp() throws Exception {
		ghirl.util.Config.setProperty(Config.DBDIR,TESTROOT);
		graph = load(new TestPersistantCompactTokyoGraphWeighted());
	}
	
	protected String getCompactDir() {
		return "tests/TestCompactTextGraph_weightedEdges";
	}
	
	@Test
	public void testWeightedEdges() {
		Distribution d = graph.walk1(GraphId.fromString("$william"), "advises");
		for (Iterator it=d.iterator(); it.hasNext(); ) {
			GraphId node = (GraphId) it.next();
			double wt = d.getLastWeight();
			assertTrue( "Expected value near 0.5 for $william advises "+node.toString()+", found "+wt, Math.abs(wt - 0.5) < 0.00001);
		}
		
	}
}
