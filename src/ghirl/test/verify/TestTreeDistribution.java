package ghirl.test.verify;

import java.util.Iterator;

import ghirl.graph.GraphId;
import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTreeDistribution {
    
    public TreeDistribution buildDistribution() {
        TreeDistribution d = new TreeDistribution();
        assertEquals(0,d.size());
        d.add(1.0, GraphId.fromString("$foo"));
        assertEquals(1,d.size());
        d.add(2.0,GraphId.fromString("$bar"));
        assertEquals(2,d.size());
        d.add(2.0,GraphId.fromString("$baz"));
        assertEquals(3,d.size());
        return d;
    }

    @Test
    public void testAdds() {
        TreeDistribution d= buildDistribution();
      
      TreeDistribution f = new TreeDistribution();
      assertEquals(0,f.size());
      f.addAll(1.0,d);
      assertEquals(3,f.size());
      
      Distribution g = d.copy();
      assertEquals(3,g.size());
      
      StringBuilder pr = new StringBuilder("pr contents:\n");
      for (Iterator it=d.iterator(); it.hasNext(); ) {
          GraphId node = (GraphId) it.next();
          pr.append(String.format("%f\t%s", d.getLastWeight(),node)).append("\n");
      }
      assertEquals(3,d.size());
      
      d.remove(GraphId.fromString("$quux"));
      assertEquals(3,d.size());
    }
    
    @Test
    public void testRemoves() {
        TreeDistribution d = buildDistribution();
        
        Distribution f = d.copy();
        d.remove(GraphId.fromString("$foo"));
        assertEquals(2, d.size());
        
        Distribution g = f.copy();
        f.remove(GraphId.fromString("$bar"));
        assertEquals(2,f.size());
        
        g.remove(GraphId.fromString("$baz"));
        assertEquals(2,g.size());
    }
}
