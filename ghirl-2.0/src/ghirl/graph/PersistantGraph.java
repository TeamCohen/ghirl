package ghirl.graph;

import java.util.*;

import org.apache.log4j.Logger;

import com.sleepycat.je.*;

import edu.cmu.minorthird.util.StringUtil;

import ghirl.util.*;

/**
 * A persistant version of a graph. 
 * 
 *  Dev note: Currently there is no way to get the ordered edges or nodes without
 *  caching all the desired items into memory first.  This could be problematic for large
 *  graphs which might benefit from an alternate solution.
 */

public abstract class PersistantGraph implements MutableGraph, Closable 
{
	private static final Logger logger = Logger.getLogger(PersistantGraph.class);
	protected boolean isFrozen=false;
	protected Set<String>    edgeCache;
	protected List<GraphId>  nodeCache;

	public abstract GraphId createNode(String flavor,String shortName);
	public abstract boolean contains(GraphId id);
	public abstract GraphId getNodeId(String flavor,String shortName);
	public abstract Iterator getNodeIterator();
	public abstract String getProperty(GraphId id,String prop);
	public abstract void setProperty(GraphId id,String prop,String val);
	public abstract void addEdge(String linkLabel,GraphId from,GraphId to);
	public abstract Set followLink(GraphId from,String linkLabel);
	public abstract Set getEdgeLabels(GraphId from);
	public abstract void close();

	private boolean throwAFit=false;
	/**
	 * Ensures the existence of an edgeCache.
	 * @return True if an edgeCache was created, false if it already existed.
	 */
	private boolean ensureEdgeCache() {
		if(throwAFit) throw new IllegalStateException("Not supposed to happen!");
		if (edgeCache != null) return false;
		logger.debug("Created edge cache");
		edgeCache = new HashSet<String>();
		return true;	
	}
	/** 
	 * Adds edge label to the cache used to execute getOrderedEdgeLabels.
	 * @param s The edge label
	 */
	protected void cacheEdgeLabel(String s) { 
		ensureEdgeCache();
		edgeCache.add(s); 
		logger.debug("Cached edge "+s);
	}
	/**
	 * Ensures the existence of a nodeCache.
	 * @return True if a nodeCache was created, false if it already existed.
	 */
	private boolean ensureNodeCache() {
		if (nodeCache != null) return false;
		logger.debug("Created node cache");
		nodeCache = new ArrayList<GraphId>();
		return true;
	}
	/**
	 * Adds node to the cache used to execute getOrderedNodeIds.
	 * @param id The graphId for the node.
	 */
	protected void cacheNodeId(GraphId id) {
		ensureNodeCache();
		nodeCache.add(id);
		logger.debug("Cached node "+id.toString());
	}
	
	public void freeze()  { 
		if (isFrozen) return;
		isFrozen=true;
	}
	
	/** Warning: This method should ONLY be run on small graphs i.e. those which fit in memory. **/
	public void loadCache() {
		logger.debug("Cache size before freezing: "+(null == this.edgeCache ? "null" : this.edgeCache.size()));
		logger.debug("Freezing...");
		boolean doEdges = ensureEdgeCache();
		boolean doNodes = ensureNodeCache();
		logger.debug("Edgecache is "+(doEdges ? "" : "not ")+"new");
		logger.debug("Nodecache is "+(doNodes ? "" : "not ")+"new");
//		throwAFit=true;
		if (doEdges || doNodes) {
			for (Iterator i=getNodeIterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				if (doEdges) edgeCache.addAll( getEdgeLabels(id) );
				if (doNodes) nodeCache.add( id );
			}
			logger.debug("Updated cache: "+edgeCache.size()+" edges and "+nodeCache.size()+" nodes.");
		}
	}
	

	public void melt() { isFrozen=false; }

	protected void checkMelted()
	{
		if (isFrozen) throw new IllegalStateException("graph is frozen!");
	}

	protected void handleDBError(DatabaseException ex) 
	{
		System.err.println("db error "+ex);
	}

	public GraphId createNode(String flavor,String shortName,Object obj)
	{
		checkMelted();
		return createNode(flavor,shortName);
	}

	public String getTextContent(GraphId id) 
	{
		return id.getShortName();
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

	public String[] getOrderedEdgeLabels() {
		if(ensureEdgeCache()) logger.warn("Edge cache empty -- call loadCache() before getOrderedEdgeLabels()");
		String[] labels = edgeCache.toArray(new String[0]);
		Arrays.sort(labels);

	    logger.debug("returning edge labels: "+StringUtil.toString(labels));
		return labels;
	}

	public GraphId[] getOrderedIds() {
		if(ensureNodeCache()) logger.warn("Node cache empty -- call loadCache() before getOrderedIds()");
		GraphId[] result = nodeCache.toArray(new GraphId[nodeCache.size()]);
		Arrays.sort(result);
		return result;
	}
	
	protected String makeKey(String flavor,String shortName,String label)
	{
		//return makeKey(new GraphId(flavor,shortName),label);
		return GraphId.toString(flavor,shortName)+"#"+label;
	}

	protected String makeKey(GraphId from,String label) 
	{ 
		return from.toString()+"#"+label;
	}

	protected String makeKey(String flavor,String shortName)
	{
		//return new GraphId(flavor,shortName).toString();
		return GraphId.toString(flavor,shortName);
	}

	protected String makeKey(GraphId from) 
	{ 
		return from.toString();
	}


	protected class MyKeyIteratorAdaptor implements Iterator
	{
		private Iterator it;
		public MyKeyIteratorAdaptor(Iterator it) { this.it=it; }
		public void remove() { it.remove(); }
		public boolean hasNext() { return it.hasNext(); }
		public Object next() { return GraphId.fromString((String)it.next()); }
	}
}
