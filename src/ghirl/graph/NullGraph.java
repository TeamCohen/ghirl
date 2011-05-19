package ghirl.graph;

import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

/** 
 * Placeholder graph which remembers nothing.
 * Methods return non-null, empty values for everything except GraphIds.
 * 
 * @author katie
 *
 */
public class NullGraph implements Graph, MutableGraph {

	@Override
	public boolean contains(GraphId id) { return false; }

	@Override
	public GraphId getNodeId(String flavor, String shortNodeName) { return null; }

	@Override
	public Iterator getNodeIterator() { return new ArrayList().iterator(); }

	@Override
	public GraphId[] getOrderedIds() { return new GraphId[0]; }

	@Override
	public String getProperty(GraphId from, String prop) { return ""; }

	@Override
	public Distribution asQueryDistribution(String queryString) { return null; /* new TreeDistribution(); */ }

	@Override
	public Set followLink(GraphId from, String linkLabel) { return new TreeSet(); }

	@Override
	public Set getEdgeLabels(GraphId from) { return new TreeSet(); }

	@Override
	public String[] getOrderedEdgeLabels() { return new String[0]; }

	@Override
	public String getTextContent(GraphId id) { return null; }

	@Override
	public Distribution walk1(GraphId from) { return new TreeDistribution(); }

	@Override
	public Distribution walk1(GraphId from, String linkLabel) { return new TreeDistribution(); }

	@Override
	public void addEdge(String label, GraphId from, GraphId to) { }
	@Override
	public void addEdge(String label, GraphId from, GraphId to, double wt) { }

	@Override
	public GraphId createNode(String flavor, String shortName) { return new GraphId(flavor,shortName); }

	@Override
	public GraphId createNode(String flavor, String shortName, Object content) { return new GraphId(flavor,shortName); }

	@Override
	public void freeze() { }

	@Override
	public void melt() { }

	@Override
	public void setProperty(GraphId from, String prop, String val) { }
}
