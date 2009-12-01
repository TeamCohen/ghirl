package ghirl.graph;

import java.io.*;
import java.util.*;

import com.sleepycat.bind.serial.*;
import com.sleepycat.bind.tuple.*;
import com.sleepycat.je.*;

import ghirl.util.*;

/**
 * A persistant version of a graph.  
 */

public class PersistantGraph extends SleepycatDB implements MutableGraph, Serializable 
{
	private Database 
	nodeMap,  // maps nodeId -> uniq nodeName
	propMap,  // maps makeKey(nodeName,prop) -> uniq value
	edgeMap,  // maps makeKey(nodeName,label) -> multiple GraphId
	labelSet; // maps maps nodeName -> multiple labels
	private boolean 
	isFrozen=false;

	public void freeze() 
	{ 
		super.sync(); 
		isFrozen=true; 
	}

	public void melt() 
	{ 
		isFrozen=false; 
	}

	private void checkMelted()
	{
		if (isFrozen) throw new IllegalStateException("graph is frozen!");
	}

	private void handleDBError(DatabaseException ex) 
	{
		System.err.println("db error "+ex);
	}

	// TODO: Trapping startup errors in this way makes it impossible 
	// to tell whether the DB started up properly or not.  At least add
	// an "initialized" flag, or something. :(
	public PersistantGraph(String dbName,char mode)
	{ 
		try { 
			initDBs(dbName,mode); 
			nodeMap = openDB("_node");
			propMap = openDB("_prop");
			edgeMap = openDupDB("_edges");
			labelSet = openDupDB("_labels");
		} catch (DatabaseException ex) {
			handleDBError(ex);
		}
	}

	public GraphId createNode(String flavor,String shortName,Object obj)
	{
		checkMelted();
		return createNode(flavor,shortName);
	}

	public GraphId createNode(String flavor,String shortName)
	{
		checkMelted();
		try {
			GraphId id = new GraphId(flavor,shortName);
			putDB( nodeMap, makeKey(id), id.toString() );
			return id;
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return new GraphId(GraphId.DEFAULT_FLAVOR,"error");
		}
	}


	public boolean contains(GraphId id) 
	{ 
		return getNodeId(id.getFlavor(),id.getShortName())!=null; 
	}


	public GraphId getNodeId(String flavor,String shortName)
	{ 
		try {
			String s = getFirstDB(nodeMap,makeKey(flavor,shortName));
			return s==null ? null : GraphId.fromString(s);
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return new GraphId(GraphId.DEFAULT_FLAVOR,"error");
		}
	}

	public Iterator getNodeIterator() 
	{ 
		try {
			return new MyKeyIteratorAdaptor(new KeyIteratorDB( nodeMap ));
		} catch (DatabaseException ex) {
			handleDBError(ex);
			throw new IllegalStateException("can't continue from db error");
		}
	}
	private class MyKeyIteratorAdaptor implements Iterator
	{
		private Iterator it;
		public MyKeyIteratorAdaptor(Iterator it) { this.it=it; }
		public void remove() { it.remove(); }
		public boolean hasNext() { return it.hasNext(); }
		public Object next() { return GraphId.fromString((String)it.next()); }
	}

	public String getProperty(GraphId id,String prop)
	{
		try {
			return getFirstDB(propMap, makeKey(id,prop) );
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return "";
		}
	}

	public void setProperty(GraphId id,String prop,String val)
	{
		checkMelted();
		try {
			String key = makeKey(id,prop);
			putDB(propMap,key,val);
		} catch (DatabaseException ex) {
			handleDBError(ex);
		}
	}

	public String getTextContent(GraphId id) 
	{
		return id.getShortName();
	}

	public void addEdge(String linkLabel,GraphId from,GraphId to)
	{
		checkMelted();
		try {
			putDB(edgeMap,makeKey(from,linkLabel),to.toString());
			putDB(labelSet,makeKey(from),linkLabel);
		} catch (DatabaseException ex) {
			handleDBError(ex);
		} //catch (ClassCastException ex) { ex.printStackTrace(); }
	}

	public Set followLink(GraphId from,String linkLabel)
	{
		try {
			Set idStrings = getDB( edgeMap, makeKey(from,linkLabel) ); 
			Set accum = new HashSet();
			for (Iterator i = idStrings.iterator(); i.hasNext(); ) {
				String s = (String)i.next();
				accum.add( GraphId.fromString(s) );
			}
			return accum;
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return Collections.EMPTY_SET;
		}
	}

	public Set getEdgeLabels(GraphId from)
	{
		try {
			return getDB(labelSet,makeKey(from));
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return Collections.EMPTY_SET;
		}
	}

	public Distribution walk1(GraphId from, String linkLabel)
	{
		Distribution dist = new TreeDistribution();
		for (Iterator j=followLink(from,linkLabel).iterator(); j.hasNext(); ) {
			GraphId id = (GraphId)j.next();
			dist.add( 1.0, id );
		}
		return dist;
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

	/** Warning: this implementation is very slow */
	public String[] getOrderedEdgeLabels() { return GraphUtil.getOrderedEdgeLabels(this); }

	/** Warning: this implementation is very slow */
	public GraphId[] getOrderedIds() { return GraphUtil.getOrderedIds(this); }

	//
	//
	//

	private String makeKey(String flavor,String shortName,String label)
	{
		//return makeKey(new GraphId(flavor,shortName),label);
		return GraphId.toString(flavor,shortName)+"#"+label;
	}

	private String makeKey(GraphId from,String label) 
	{ 
		return from.toString()+"#"+label;
	}

	private String makeKey(String flavor,String shortName)
	{
		//return new GraphId(flavor,shortName).toString();
		return GraphId.toString(flavor,shortName);
	}

	private String makeKey(GraphId from) 
	{ 
		return from.toString();
	}
}
