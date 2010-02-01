package ghirl.graph;

import java.util.*;

import org.apache.log4j.Logger;

import ghirl.util.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

/** A TextGraph wrapper that includes an in-memory cache. 
 */

public class CachingGraph implements Graph
{
	private static final Logger log = Logger.getLogger(CachingGraph.class);
    private static final Distribution EMPTY_DIST = new TreeDistribution();

    private Graph innerGraph;
    private int cacheSize;

    /** ordered list of all graph ids */
    private GraphId[] graphIds;
    private int graphIdIndex(GraphId id) { return safeLookup( graphIds,id,"graphId"); }

    /** ordered list of all link labels */
    private String[] linkLabels;
    private int linkLabelIndex(String label) { return safeLookup( linkLabels,label,"link label"); }

    /** cached walk for potentially every graph id, indexed by graph in */
    private CompactImmutableDistribution[][] walkInfo;

    /** queue of graphIds, in order they are added to walkInfo */
    private int[] lruQueue = null;
    int lruQueuePtr;

    /** total size of all walks, in bytes */
    private int walkInfoSize;

    public CachingGraph(Graph innerGraph)
    {
        this(innerGraph,0);
    }

    public CachingGraph(Graph innerGraph,int cacheSize) 
    { 
        log.info("creating new CachingGraph: innerGraph="+innerGraph.getClass());
        this.innerGraph = innerGraph; 
        this.cacheSize = cacheSize;
        graphIds = innerGraph.getOrderedIds();
        linkLabels = innerGraph.getOrderedEdgeLabels();
        log.debug("for CachingGraph, "+graphIds.length+" ids, linkLabels="+StringUtil.toString(linkLabels));
        walkInfo = new CompactImmutableDistribution[ graphIds.length ][ linkLabels.length ];
        if (cacheSize>0) {
	    lruQueue = new int[ cacheSize ];
	    lruQueuePtr = 0;
        }
        walkInfoSize = graphIds.length*linkLabels.length*4;
    }

    //////////////////////////////////////////////////////////////////////////////
    // for size estimates and size manipulation
    //////////////////////////////////////////////////////////////////////////////

    public int sizeInBytesForGraphIds()
    {
        int s = 0;
        for (int i=0; i<graphIds.length; i++) {
	    s += graphIds[i].sizeInBytes();
        }
        s += graphIds.length*4;
        return s;
    }

    public int sizeInBytesForLinkLabels() 
    {
        int s = 0;
        for (int i=0; i<linkLabels.length; i++) {
	    s += linkLabels.length;
        }
        s += linkLabels.length*4;
        return s;
    }

    public int sizeInBytesForWalks()
    {
        return walkInfoSize;
    }

    public int sizeInNodes()
    {
        return graphIds.length;
    }

    public int sizeInBytes()
    {
        return sizeInBytesForWalks() + sizeInBytesForLinkLabels() + sizeInBytesForGraphIds();
    }

    public void clearCache()
    {
        /*
          for (int i=0; i<lruQueuePtr; i++) {
          int idIndex = lruQueue[i];
          if (idIndex>0) {
          for (int j=0; j<linkLabels.length; j++) {
          walkInfo[idIndex][j] = null;
          }
          lruQueue[i] = 0;
          }
          }
          lruQueuePtr = 0;
        */
    }

    //
    //

    public Graph getInnerGraph() { return innerGraph; } 

    final private int safeLookup( Object[] array, Object o, String whatItIs)
    {
        int k = Arrays.binarySearch( array, o );
        if (k<0) {
	    log.debug("searching: "+StringUtil.toString(array));
	    throw new IllegalStateException(whatItIs+": "+o+" not found (would be at "+k+")");
        }
        return k;
    }

    final private Distribution getCachedDist(int fromIndex,int linkIndex)
    {
    	CompactImmutableDistribution cd = walkInfo[fromIndex][linkIndex];
    	if (cd==null) {
    		if (lruQueue!=null) {
    			// a non-zero entry means the ptr has wrapped around once,
    			// and the current entry was added a long time ago..
    			int oldestGraphId = lruQueue[ lruQueuePtr ];
    			if (oldestGraphId!=0) {
    				// clear all information about this ancient cache entry
    				//System.out.println("erasing walks for "+graphIds[oldestGraphId]);
    				for (int i=0; i<linkLabels.length; i++) {
    					walkInfo[ oldestGraphId ][i] = null;
    				}
    			}
    			// record that we will add info about this graph index
    			lruQueue[ lruQueuePtr++ ] = fromIndex;
    			// if the ptr points beyond the queue, reset it
    			if (lruQueuePtr>=lruQueue.length) lruQueuePtr = 0;
    		}
    		// add all information about this index to the cache
    		//System.out.println("caching walks for "+graphIds[fromIndex]);
    		for (int i=0; i<linkLabels.length; i++) {
    			Distribution d = innerGraph.walk1(graphIds[fromIndex],linkLabels[i]);
    			//if (d.size()>0) System.out.println("inner dist for "+graphIds[fromIndex]+","+linkLabels[i]+" = "+d);
    			cd = new CompactImmutableDistribution(d, graphIds);
    			//if (cd.size()>0) System.out.println("compact dist = "+cd);
    			walkInfo[fromIndex][i] = cd;
    			walkInfoSize += cd.sizeInBytes();
    		}
    	}
    	return walkInfo[fromIndex][linkIndex];
    }

    //
    // external interface
    // 

    public boolean contains(GraphId id)
    {
        int k = Arrays.binarySearch( graphIds, id );
        return k>=0;
    }
	
    public GraphId getNodeId(String flavor,String shortNodeName)
    {
        GraphId id = new GraphId(flavor,shortNodeName);
        int k = Arrays.binarySearch( graphIds, id );
        return k>=0 ? graphIds[k] : null;
    }

    public Iterator getNodeIterator()
    {
        return new MyIterator();
    }

    private class MyIterator implements Iterator
    {
        int index = 0;
        public boolean hasNext() { return index < graphIds.length; }
        public Object next() { return graphIds[index++]; }
        public void remove() { throw new UnsupportedOperationException("can't remove"); }
    }

    public Set getEdgeLabels()
    {
        Set accum = new HashSet();
        for (int i=0; i<linkLabels.length; i++) {
	    accum.add(linkLabels[i]);
        }
        return accum;
    }

    public Set getEdgeLabels(GraphId from)
    {
        Set accum = new HashSet();
        int fromIndex = graphIdIndex(from);
        for (int i=0; i<linkLabels.length; i++) {
	    if (getCachedDist(fromIndex,i).size()>0) {
                accum.add( linkLabels[i] );
	    }
        }
        return accum;
    }

    public Set followLink(GraphId from,String linkLabel)
    {
        int fromIndex = graphIdIndex(from);
        int linkIndex = linkLabelIndex(linkLabel);
        Distribution d = getCachedDist(fromIndex,linkIndex);
        Set accum = new HashSet();
        for (Iterator i=d.iterator(); i.hasNext(); ) {
	    accum.add( i.next() );
        }
        return accum;
    }

    public Distribution walk1(GraphId from,String linkLabel)
    {
        int fromIndex = graphIdIndex(from);
        int linkIndex = linkLabelIndex(linkLabel);
        return getCachedDist( fromIndex, linkIndex );
    }

    public Distribution walk1(GraphId from)
    {
        Distribution accum = new TreeDistribution();
        int fromIndex = graphIdIndex(from);
        for (int i=0; i<linkLabels.length; i++) {
	    Distribution d = getCachedDist(fromIndex,i);
	    if (d.size()>0) accum.addAll( 1.0, d );
        }
        return accum;
    }

    public String getProperty(GraphId from,String prop)
    {
        return innerGraph.getProperty(from,prop);
    }

    public String getTextContent(GraphId id)
    {
        return innerGraph.getTextContent(id);
    }
    

    public void printStatus()
    {
        System.out.println("nodes: "+sizeInNodes() + "\n" + 
                           "memory for graphIds:   "+sizeInBytesForGraphIds()/1024.0 + "k\n" + 
                           "memory for linkLabels: "+sizeInBytesForLinkLabels()/1024.0 + "k\n" + 
                           "memory for walks:      "+sizeInBytesForWalks()/1024.0 + "k\n" + 
                           "total memory:          "+(sizeInBytes()/1024.0) / sizeInNodes() + "k/node");
        if (lruQueue!=null) {
	    System.out.println("lru queue size: "+lruQueue.length+" ptr: "+lruQueuePtr);
        } else {
	    System.out.println("no lru queue in effect");
        }
    }

    public String[] getOrderedEdgeLabels() { return innerGraph.getOrderedEdgeLabels(); }

    public GraphId[] getOrderedIds() { return innerGraph.getOrderedIds(); }

    public Distribution asQueryDistribution(String queryString)
    {
        //printStatus();
        return innerGraph.asQueryDistribution(queryString);
    }


    static public void main(String[] args)
    {
        Graph graph = new TextGraph(args[0]);
        CachingGraph cg = new CachingGraph(graph,1000);
        QueryGUI gui = new QueryGUI(cg);
        new ViewerFrame("QueryGUI", gui );
    }
}
