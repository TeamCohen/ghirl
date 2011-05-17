package ghirl.graph;

import edu.cmu.minorthird.util.*;

/** An edge in a graph. */

public class Edge implements Comparable
{
    final GraphId start,end;
    final String label;

    public Edge(GraphId start,GraphId end) {
	this(start,end,"?unknown?");
    }

    public Edge(GraphId start,GraphId end,String label) { 
	this.start=start; this.end=end; this.label = label;
	if (start==null || end==null) throw new IllegalArgumentException("bad edge!");
    }
    public GraphId getStart() { return start; }
    public GraphId getEnd() { return end; }
    public String getLabel() { return label; }
    public String toString() { return "[edge "+start+"-->"+end+"]"; }

    public int hashCode() {
	return start.hashCode() ^ end.hashCode();
    }

    public int compareTo(Object o) {
	Edge b = (Edge)o;
	int cmp = start.compareTo(b.start);
	if (cmp!=0) return cmp;
	else return end.compareTo(b.end);
    }
}
