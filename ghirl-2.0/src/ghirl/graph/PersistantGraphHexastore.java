package ghirl.graph;

import ghirl.persistance.Hexastore;
import ghirl.persistance.IGraphStore;
import ghirl.persistance.Memstore;
import ghirl.util.Config;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

public class PersistantGraphHexastore extends PersistantGraph {
	private static final Logger logger = Logger.getLogger(PersistantGraphHexastore.class);
	IGraphStore hs;  // the interface to the C code
	boolean canWrite;
	
	public PersistantGraphHexastore(String dbName,char mode)
	{ 
		canWrite=true;
		if (dbName.equals(""))
			hs = new Memstore(dbName);
		else {
			// Hexastore uses two different index files which start with the 
			// provided name.  For compliance with TextGraph's method of
			// wiping databases, we need to wrap these in an enclosing
			// directory.

			String basedir = Config.getProperty("ghirl.dbDir");
			if (basedir == null) throw new IllegalArgumentException("The property ghirl.dbDir must be defined!");
			String dbpath = basedir + File.separatorChar + dbName;
			File enclosing = new File(dbpath);
			if (!enclosing.exists()) {
				logger.warn("Hexastore directory \""+dbpath+"\" not present -- creating...");
				enclosing.mkdir();
			}
			String[] parts = dbpath.split(File.separator);
			hs = new Hexastore(dbpath+File.separatorChar+parts[parts.length-1],mode);
		}
		if ('r'==mode) {
			canWrite = false;
			freeze();
		}
	}
	
	public void freeze() {
		if(canWrite && !this.isFrozen) {
			if (this.edgeCache != null) { 
				logger.debug("Writing "+this.edgeCache.size()+" elts to the hexastore...");
				hs.writeToDB();
			} else logger.debug("No cache write necessary.");
		}
		super.freeze();
	}
	
	public void close() {
		hs.close();
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
