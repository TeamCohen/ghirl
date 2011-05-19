package ghirl.graph;

import java.io.*;
import java.util.*;

import ghirl.util.*;

public class BasicGraph implements MutableGraph 
{
	/** Set of nodes */
	protected Set<GraphId> nodeSet = new HashSet<GraphId>();  // set of nodes
	/** Maps id to edge labels */
	protected Map<GraphId,Set<String>> labelMap = new HashMap<GraphId,Set<String>>(); // maps id => edge labels
	/** Maps edgeKey(id,label) to destinations */
	protected Map<String,Set<GraphId>> edgeMap = new HashMap<String,Set<GraphId>>();  // maps edgeKey(id,label)=>HashSet<destinations>
	/** Maps edgeKey(node prop) to properties */
	protected Map<GraphId,Properties> nodeProps = new HashMap<GraphId,Properties>(); // maps edgeKey(node,prop)=>properties
	protected boolean isFrozen = false;
	
	public void freeze() 
	{ 
		isFrozen=true;
	}

	public void melt() 
	{ 
		isFrozen=false; 
	}

	protected void checkMelted()
	{
		if (isFrozen) throw new IllegalStateException("graph is frozen!");
	}

	public GraphId createNode(String flavor,String shortName,Object obj)
	{
		checkMelted();
		return createNode(flavor,shortName);
	}

	public GraphId createNode(String flavor,String shortName)
	{
		checkMelted();
		GraphId id = new GraphId(flavor,shortName);
		nodeSet.add(id);
		return id;
	}

	public boolean contains(GraphId id) { return nodeSet.contains(id); }

	public GraphId getNodeId(String flavor,String shortName)
	{
		GraphId id = new GraphId(flavor,shortName);
		if (contains(id)) return id;
		else return null;
	} 

	public Iterator<GraphId> getNodeIterator() { return nodeSet.iterator(); }


	public Properties getProperties(GraphId id){
		return (Properties)nodeProps.get(id);
	}

	public String getProperty(GraphId id,String prop)
	{
		Properties props = (Properties)nodeProps.get(id);
		if (props==null) return null;
		else return props.getProperty(prop);
	}

	public void setProperty(GraphId id,String prop,String val)
	{
		checkMelted();
		Properties props = (Properties)nodeProps.get(id);
		if (props==null) nodeProps.put(id,(props=new Properties()));
		props.setProperty(prop,val);
	}

	public String getTextContent(GraphId id) 
	{
		return id.getShortName();
	}

	public void addEdge(String linkLabel,GraphId from,GraphId to)
	{
		checkMelted();
		String key = edgeKey(from,linkLabel);
		Set<GraphId> oldSet = edgeMap.get(key);
		if (oldSet==null) edgeMap.put(key,(oldSet = new HashSet<GraphId>()));
		oldSet.add(to);
		Set<String> oldLabs = labelMap.get(from);
		if (oldLabs==null) labelMap.put(from,(oldLabs = new HashSet<String>()));
		oldLabs.add( linkLabel );
	}

	public Set followLink(GraphId from,String linkLabel) 
	{
		return safeSet(edgeMap.get(edgeKey(from,linkLabel)));
	}

	public Set getEdgeLabels(GraphId from)
	{
		return safeSet(labelMap.get(from));
	}

	public Distribution walk1(GraphId from, String linkLabel)
	{
		Distribution d = new TreeDistribution();
		for (Iterator i=followLink(from,linkLabel).iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			d.add( 1.0, id );
		}
		return d;
	}

	public Distribution walk1(GraphId from)
	{
		Distribution dist = new TreeDistribution();
		for (Iterator i=getEdgeLabels(from).iterator(); i.hasNext(); ) {
			String linkLabel = (String)i.next();
			for (Iterator j=followLink(from,linkLabel).iterator(); j.hasNext(); ) {
				GraphId id = (GraphId)j.next();
				dist.add( 1.0, id );
			}
		}
		return dist;
	}

	public Distribution asQueryDistribution(String queryString)
	{
		return CommandLineUtil.parseNodeOrNodeSet(queryString,this);
	}

	public String[] getOrderedEdgeLabels() { 
		Set<String> edges = new HashSet<String>();
		for(Set<String> valset: this.labelMap.values()) edges.addAll(valset);
		String[] edgearray = (String[]) edges.toArray(new String[edges.size()]);
		Arrays.sort(edgearray);
		return edgearray;
		//	return GraphUtil.getOrderedEdgeLabels(this); 
	}
	public GraphId[] getOrderedIds()       {
		GraphId[] idarray = (GraphId[]) nodeSet.toArray(new GraphId[nodeSet.size()]);
		Arrays.sort(idarray);
		return idarray;
		// return GraphUtil.getOrderedIds(this); 
	}

	//
	// for handling edges
	//

	protected String edgeKey(GraphId from,String linkLabel) 
	{ 
		return from.getFlavor()+":"+from.getShortName()+"#"+linkLabel; 
	}

	protected Set safeSet(Set set) { return set==null ? Collections.EMPTY_SET : set; }

	@Override
	public void addEdge(String label, GraphId from, GraphId to, double wt) {
		throw new UnsupportedOperationException(this.getClass().getName()+" does not support weighted edges.");
	}

}
