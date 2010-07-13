package ghirl.test;
import static org.junit.Assert.*;

import java.util.Iterator;

import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.util.Distribution;


public class GhirlTestSupport {

	public Distribution assertPresent(Graph g, String query) {
		return assertPresent(g,query,-1);
	}
	
	public Distribution assertPresent(Graph g, String query, int exactNumberOfMatches) {
		Distribution d = g.asQueryDistribution(query);
		assertTrue("Query "+query+" must return non-null", d!=null);
		if (exactNumberOfMatches < 0)
			assertTrue("Query "+query+" must return nonzero results", d.size()>0);
		else
			assertEquals("Query "+query+" must return "+exactNumberOfMatches+" results", exactNumberOfMatches,d.size());
		for (Iterator it=d.iterator(); it.hasNext();) {
			GraphId id = (GraphId) it.next();
			System.out.println("Found: "+id.toString());
		}
		return d;
	}
	
	public void assertFlavor(Graph g, String query, String flavor) {
		Distribution d = g.asQueryDistribution(query);
		assertTrue("Query "+query+" must return non-null", d!=null);
		boolean hazFlavor=false;
		for (Iterator it=d.iterator(); it.hasNext();) {
			GraphId id = (GraphId) it.next();
			if (hazFlavor = id.getFlavor().equals(flavor)) break;
		}
		assertTrue("At least one of the nodes returned from the query must have flavor "+flavor, hazFlavor);
	}
	
	public void assertAbsent(Graph g, String query) {
		Distribution d = g.asQueryDistribution(query);
		if (d != null)
			assertEquals("Query "+query+" must be absent",0,d.size());
	}
	
	public void assertNumberOfEntities(Graph g, String query, int number) {
		Distribution e = g.asQueryDistribution(query);
		assertTrue("Query "+query+" must exist as a type in the graph",e!=null);
		assertTrue("Query "+query+" must have at least one presence in the graph",
				e.size()>0);
		Distribution d = g.walk1(
				(GraphId) (e.iterator().next()),
				"isaInverse");
		assertEquals("Query "+query+" isaInverse should generate "+number+" results",
				number, d.size());
	}
}
