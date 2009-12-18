package ghirl.graph;

import ghirl.persistance.Hexastore;
import ghirl.persistance.IGraphStore;
import ghirl.persistance.Memstore;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

public class PersistantGraphHexastore extends PersistantGraph {
	private static final Logger logger = Logger.getLogger(PersistantGraphHexastore.class);
	IGraphStore hs;  // the interface to the C code
	
	public PersistantGraphHexastore(String dbName,char mode)
	{ 
		if (dbName.equals(""))
			hs = new Memstore(dbName);
		else {
			// Hexastore uses two different index files which start with the 
			// provided name.  For compliance with TextGraph's method of
			// wiping databases, we need to wrap these in an enclosing
			// directory.
			File enclosing = new File(dbName);
			if (!enclosing.exists()) enclosing.mkdir();
			String[] parts = dbName.split(File.separator);
			hs = new Hexastore(dbName+File.separatorChar+parts[parts.length-1]);
		}
		if ('r'==mode) freeze();
	}
	
	public void freeze() {
		if(!this.isFrozen) {
			logger.debug("Cache size before writing: "+(null != this.edgeCache ? this.edgeCache.size() : "null"));
			hs.writeToDB();
			logger.debug("Cache size after  writing: "+(null != this.edgeCache ? this.edgeCache.size() : "null"));
		}
		super.freeze();
	}
	
	public void checkMelted() {
		super.checkMelted();
	}
	
	@Override
	public void addEdge(String linkLabel, GraphId from, GraphId to) {
		logger.debug("Creating edge "+linkLabel);
		checkMelted();
		hs.add_Edge(from.toString(), linkLabel, to.toString());
		cacheEdgeLabel(linkLabel);
	}

	@Override
	public boolean contains(GraphId id) {
		return hs.contains_Node(makeKey(id.getFlavor(),id.getShortName()));
	}

	@Override
	public GraphId createNode(String flavor, String shortName) {
		logger.debug("Creating node "+shortName);
		checkMelted();
		GraphId id = new GraphId(flavor,shortName);
		hs.add_Node(makeKey(id), id.toString());
		cacheNodeId(id);
		return id;
	}

	@Override
	public Set followLink(GraphId from, String linkLabel) {
		return hs.getResultSet(from.toString(), linkLabel);
	}

	@Override
	public Set getEdgeLabels(GraphId from) {
		Set s = hs.get_Labels(makeKey(from));
		if (s == null)
			return Collections.EMPTY_SET;
		return s;
	}

	@Override
	public GraphId getNodeId(String flavor, String shortName) {
		String s = hs.get_Node(makeKey(flavor,shortName));
		return s==null ? null : GraphId.fromString(s);
	}

	@Override
	public Iterator getNodeIterator() {
		return hs.getNodesIterator();
	}

	@Override
	public String getProperty(GraphId id, String prop) {
		String s = hs.get_Prop(id.toString(), prop);
		return s;
	}

	@Override
	public void setProperty(GraphId id, String prop, String val) {
		checkMelted();
		hs.add_Prop(id.toString(), prop, val);
	}

}
