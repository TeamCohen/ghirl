package ghirl.test.verify;

import java.util.Iterator;

import ghirl.graph.ApproxPageRankSearcher;
import ghirl.graph.BasicGraph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import org.junit.Test;
import static org.junit.Assert.*;

public class ApproxPageRankSearcherTest {

    @Test
    public void test() {
        BasicGraph g = new BasicGraph();
        GraphLoader loader = new GraphLoader(g);
        loader.loadLine("edge likes $cat $toymouse");
        loader.loadLine("edge likes $cat $catnip");
        loader.loadLine("edge filledWith $toymouse $catnip");
        loader.loadLine("edge hates $cat $water");
        
        double alpha = 0.3;
        double eps = 0.3;
        ApproxPageRankSearcher searcher = new ApproxPageRankSearcher(alpha, eps, g);
        Distribution residual = new TreeDistribution();
        Distribution result = searcher.search(GraphId.fromString("$toymouse"), residual);
        
        StringBuilder pr = new StringBuilder("pr contents:\n");
        for (Iterator it=result.iterator(); it.hasNext(); ) {
            GraphId node = (GraphId) it.next();
            pr.append(String.format("%f\t%s", result.getLastWeight(),node)).append("\n");
        }
        
        StringBuilder r = new StringBuilder("residual contents:\n");
        for (Iterator it=residual.iterator(); it.hasNext(); ) {
            GraphId node = (GraphId) it.next();
            r.append(String.format("%f\t%s", residual.getLastWeight(),node)).append("\n");
        }
        
        assertEquals("There should only be one node in pr. "+pr.toString(),1,result.size());
        assertEquals(pr.toString(),"toymouse",((GraphId)result.iterator().next()).getShortName());
        assertTrue("Expected weight 0.3 for $toymouse; got "+result.getLastWeight(),
                Math.abs(result.getLastWeight() - 0.3) < 1e-6);
        
        assertEquals("There should be 3 nodes in the residual. "+r.toString(), 3, residual.size());
        for (Iterator it=residual.iterator(); it.hasNext();) {
            String nodename = ((GraphId) it.next()).getShortName();
            assertTrue("Bad residual node name "+nodename,"toymouse cat catnip".indexOf(nodename) >= 0);
            double expectedweight = 0.0;
            if (nodename.equals("toymouse")) { expectedweight = 0.35; }
            else if (nodename.equals("cat")) { expectedweight = 0.175; }
            else if (nodename.equals("catnip")) { expectedweight = 0.175; }
            assertTrue(String.format("Expected weight %f for residual $%s; got %f", expectedweight,nodename,residual.getLastWeight()), 
                    Math.abs(residual.getLastWeight() - expectedweight) < 1e-6);
        }
    }
}
