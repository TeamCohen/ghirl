package ghirl.graph;

import ghirl.persistance.SleepycatStore;
import ghirl.util.Config;

import java.io.File;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;

public class PersistantGraphSleepycat extends PersistantGraph
{
	private static final Logger logger = Logger.getLogger(PersistantGraphSleepycat.class);
	SleepycatStore persistance;

	public PersistantGraphSleepycat(String dbName,char mode)
	{ 

		String basedir = Config.getProperty("ghirl.dbDir");
		if (basedir == null) throw new IllegalArgumentException("The property ghirl.dbDir must be defined!");
		String dbpath = basedir + File.separatorChar + dbName;
		logger.info("Creating new PersistantGraphSleepycat '"+dbpath+"'");
		persistance = new SleepycatStore(dbpath, mode);
		if ('r'==mode) freeze();
	}
	
	public void freeze() 
	{
		super.freeze();
		persistance.sync();
	}
	
	public void close() { this.persistance.close(); }

	public GraphId createNode(String flavor,String shortName,Object obj)
	{
		checkMelted();
		return createNode(flavor,shortName);
	}

	public GraphId createNode(String flavor,String shortName)
	{
		checkMelted();
		GraphId id = new GraphId(flavor,shortName);
		persistance.add_Node(makeKey(id), id.toString());
		logger.debug("Created new node "+id.toString());
		cacheNodeId(id);
		return id;
		// TODO: WARNING! this used to return an error ID if the node add failed.  see SleepycatStore.
	}


	public boolean contains(GraphId id) 
	{ 
		return persistance.contains_Node(makeKey(id));//getNodeId(id.getFlavor(),id.getShortName())!=null; 
	}


	public GraphId getNodeId(String flavor,String shortName)
	{ 
		String s = persistance.get_Node(makeKey(flavor,shortName));
		return s==null ? null : GraphId.fromString(s);
	}

	public Iterator getNodeIterator() 
	{ 
		return persistance.getNodesIterator();
	}

	public String getProperty(GraphId id,String prop)
	{
		return persistance.get_Prop(makeKey(id,prop), null);
	}

	public void setProperty(GraphId id,String prop,String val)
	{
		checkMelted();
		persistance.add_Prop(makeKey(id,prop), null, val);
	}

	public void addEdge(String linkLabel,GraphId from,GraphId to)
	{
		checkMelted();
		// DANGER SleepycatStore does its own makeKey(from,linklabel) calculation inside the below:
		persistance.add_Edge(makeKey(from), linkLabel, makeKey(to)); 
		logger.debug("Created new edge "+linkLabel);
		//catch (ClassCastException ex) { ex.printStackTrace(); }
		cacheEdgeLabel(linkLabel);
	}

	public Set followLink(GraphId from,String linkLabel)
	{
			Set idStrings = persistance.getResultSet(makeKey(from, linkLabel), null); 
			Set accum = new HashSet();
			for (Iterator i = idStrings.iterator(); i.hasNext(); ) {
				String s = (String)i.next();
				accum.add( GraphId.fromString(s) );
			}
			return accum;
	}

	
	public Set getEdgeLabels(GraphId from)
	{
		return persistance.get_Labels(makeKey(from));
	}
}
