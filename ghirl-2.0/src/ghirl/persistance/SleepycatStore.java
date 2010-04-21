package ghirl.persistance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseException;

public class SleepycatStore extends SleepycatDB implements IGraphStore {
	private static final Logger logger = Logger.getLogger(SleepycatStore.class);
	private Database 
	nodeMap,  // maps nodeId -> uniq nodeName
	propMap,  // maps makeKey(nodeName,prop) -> uniq value
	edgeMap,  // maps makeKey(nodeName,label) -> multiple GraphId
	labelSet; // maps maps nodeName -> multiple labels
	private static final String NAME_NODEMAP="_node",
	                            NAME_PROPMAP="_prop",
	                            NAME_EDGEMAP="_edges",
	                            NAME_LABELSET="_labels";
	
	public SleepycatStore(String dbName,char mode) {
		try { 
			initDBs(dbName,mode); 
			nodeMap = openDB(NAME_NODEMAP);
			propMap = openDB(NAME_PROPMAP);
			edgeMap = openDupDB(NAME_EDGEMAP);
			labelSet = openDupDB(NAME_LABELSET);
		} catch (DatabaseException ex) {
			handleDBError(ex);
		}
	}
	
	protected List<String> getDatabaseNames() { 
		List<String> ret = new ArrayList<String>();
		Collections.addAll(ret, NAME_NODEMAP, NAME_PROPMAP, NAME_EDGEMAP, NAME_LABELSET);
		return ret;
	}

	protected void handleDBError(DatabaseException ex) 
	{
		logger.error("db error ",ex);
		throw new IllegalStateException(ex);
	}
	
	@Override
	public void add_Edge(String from, String linkLabel, String to) {
		try {
//			putDB(edgeMap,makeKey(from,linkLabel),to.toString());
//			putDB(labelSet,makeKey(from),linkLabel);

			putDB(edgeMap,from+"#"+linkLabel,to);
			putDB(labelSet,from,linkLabel);
		} catch (DatabaseException ex) {
			handleDBError(ex);
		}
	}

	@Override
	public void add_Node(String key, String node) {
		try {
			putDB( nodeMap, key, node);
		} catch (DatabaseException ex) {
			handleDBError(ex);
//			return new GraphId(GraphId.DEFAULT_FLAVOR,"error");
		}
	}

	@Override
	/**
	 * @param a the key for this id with this propname
	 * @param b unused
	 * @param c the value for the property
	 */
	public void add_Prop(String keyproperty, String unused, String value) {
		try {
			putDB(propMap,keyproperty,value);
		} catch (DatabaseException ex) {
			handleDBError(ex);
		}
	}

	@Override
	public boolean contains_Node(String key) {
		return get_Node(key) != null;
	}

	@Override
	public String[] getNodesArray() {
		// hack!  returning a String[] for a huge database is a dumb idea!
		// but that's how hexastore works!  help!
		throw new UnsupportedOperationException("Not legal to get array from a SleepycatStore.");
	}
	
	public Iterator getNodesIterator() {
		try {
			return new MyKeyIteratorAdaptor(new KeyIteratorDB( nodeMap ));
		} catch (DatabaseException ex) {
			handleDBError(ex);
			throw new IllegalStateException("can't continue from db error");
		}
	}

	@Override
	public String[] getResultSetArray(String node, String linkLabel) {
		// hack!  returning a String[] for a huge database is a dumb idea!
		// but that's how hexastore works!  help!
		throw new UnsupportedOperationException("Not legal to get array from a SleepycatStore.");
	}
	
	public Set getResultSet(String key, String linkLabel) {
		try {
			return getDB( edgeMap, key );
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return Collections.EMPTY_SET;
		}
	}

	@Override
	public Set get_Labels(String key) {
		try {
			return getDB(labelSet,key);
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return Collections.EMPTY_SET;
		}
	}

	@Override
	public String get_Node(String key) {
		try {
			String s = getFirstDB(nodeMap,key);
			return s;//==null ? null : GraphId.fromString(s);
		} catch (DatabaseException ex) {
			handleDBError(ex);
//			return new GraphId(GraphId.DEFAULT_FLAVOR,"error");
		}
		return null;
	}

	@Override
	/**
	 * @param a the key generated by the id and the property name
	 * @param b unused
	 */
	public String get_Prop(String keyproperty, String unused) {
		try {
			return getFirstDB(propMap, keyproperty );
		} catch (DatabaseException ex) {
			handleDBError(ex);
			return "";
		}
	}

	@Override
	public void writeToDB() {
		this.sync();
	}
	
	public void close() {
		try {
			this.closeDBs();
		} catch (DatabaseException ex) { throw new RuntimeException(ex); }
	}
}
