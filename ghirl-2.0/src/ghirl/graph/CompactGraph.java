package ghirl.graph;

import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import ghirl.util.*;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;

/** A compact non-text graph that can be loaded
 * from three files:
 *<ul>
 * <li> <code>nodeNameFile</code>: each line is the name of a node, where the 'integer
 *  id' of a node is line number, starting from one.  Lines are sorted
 *  lexigraphically.</li>
 * 
 * <li><code>linkNameFile</code>: each line is the name of a link label, where the
 *  'integer id' is the line number.</li>
 *
 * <li><code>walkFile</code>: each line has the form: 
 *<pre>
 *   srcId <space> linkId <space> numDestinations <space> destId1:w1 <space> ... <space> destIdN:wN"
 *</pre>
 * where the id's are all appropriate integers, and wi is a number.  The numbers need not be 
 * normalized to sum to one.  In <code>scripts/cvt_graph.pl</code>, this file is referred to
 * as the "row" file.</li>
 *
 * <li><code>sizeFile</code>: the number of lines in the first two files.</li>
 * </ul>
 */
public class CompactGraph implements Graph
{
    private static final Logger log = Logger.getLogger(CompactGraph.class);

    protected static final Distribution EMPTY_DIST = new TreeDistribution();

    /** ordered list of all graph ids */
    protected GraphId[] graphIds;
    protected int graphIdIndex(GraphId id) { return safeLookup( graphIds,id,"graphId"); }

    /** ordered list of all link labels */
    protected String[] linkLabels;
    protected int linkLabelIndex(String label) { return safeLookup( linkLabels,label,"link label"); }

    /** cached walk for potentially every graph id, indexed by graph in */
    protected CompactImmutableDistribution[][] walkInfo;

    public void load(File sizeFileName,File linkFileName,File nodeFileName,File walkFileName)
        throws IOException, FileNotFoundException 
    { 
        String line;
        String[] parts;

        // read the statistics on the array sizes
        LineNumberReader sizeIn = new LineNumberReader(new FileReader(sizeFileName));
        line = sizeIn.readLine();
        parts = line.split(" ");
        int numLinks = StringUtil.atoi(parts[0]);
        int numNodes = StringUtil.atoi(parts[1]);
        sizeIn.close();
        
        log.info("creating compact graph with "+numLinks+" links and "+numNodes+" nodes");
        linkLabels = new String[numLinks + 1];
        graphIds = new GraphId[numNodes + 1];

        LineNumberReader linkIn = new LineNumberReader(new FileReader(linkFileName));
        int id = 0;
        linkLabels[0] = "";   //null id
        while ((line = linkIn.readLine())!=null) {
            linkLabels[++id] = line;
        }
        linkIn.close();

        ProgressCounter npc = new ProgressCounter("loading "+nodeFileName,"lines");
        LineNumberReader nodeIn = new LineNumberReader(new FileReader(nodeFileName));
        id = 0;
        graphIds[0] = GraphId.fromString("");   //for null id
        while ((line = nodeIn.readLine())!=null) {
            graphIds[++id] = GraphId.fromString(line);
            npc.progress();
        }
        npc.finished();
        nodeIn.close();

        ProgressCounter wpc = new ProgressCounter("loading "+walkFileName,"lines");
        LineNumberReader walkIn = new LineNumberReader(new FileReader(walkFileName));
        initWalkInfo(); // template call
        while ((line = walkIn.readLine())!=null) {        
            parts = line.split(" ");
            int srcId = StringUtil.atoi(parts[0]);
            int linkId = StringUtil.atoi(parts[1]);
            int numDest = StringUtil.atoi(parts[2]);
            int[] destId = new int[numDest];
            float[] totalWeightSoFar = new float[numDest];
            float tw = (float)0.0;
            int k = 0;
            for (int i = 3; i < parts.length; i++) {
                String[] destWeightParts = parts[i].split(":");
                destId[k] = StringUtil.atoi(destWeightParts[0]); 
                tw += StringUtil.atof(destWeightParts[1]);
                totalWeightSoFar[k] = tw;
                k++;
            }
            initWalkInfoCell(srcId,linkId,destId,totalWeightSoFar); // template call
            wpc.progress();
        }
        finishWalkInfo(); // template call
        wpc.finished();
        walkIn.close();
    }
    /** Finish initialization of immutable walk info cache */
    protected void finishWalkInfo() {}
    /** Initialize walk info cache */
    protected void initWalkInfo() {
        walkInfo = new CompactImmutableDistribution[ graphIds.length ][ linkLabels.length ];
    }
    /** Add one node:link record to the walk info cache */
    protected void initWalkInfoCell(int srcId, int linkId, int[]destId, float[] totalWeightSoFar) {
        walkInfo[srcId][linkId] = 
            new CompactImmutableArrayDistribution(destId,totalWeightSoFar,graphIds);
    }

    final protected int safeLookup( Object[] array, Object o, String whatItIs)
    {
        int k = Arrays.binarySearch( array, o );
//        if (k<0) {
//	    throw new IllegalStateException(whatItIs+": "+o+" not found");
//        }
        return k;
    }

    // change 6/22 kmr: this method was marked final, but I don't think
    // that's what we meant to do -- "final" just means the method cannot
    // be overridden in a subclass, not that the method is inlined or
    // the resulting object is immutable.
    protected Distribution getStoredDist(int fromIndex,int linkIndex)
    {
        Distribution result = walkInfo[fromIndex][linkIndex];
        if (result == null) {
            return EMPTY_DIST;
        } else {
            return result;
        }
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
        for (int i=1; i<linkLabels.length; i++) {
	    accum.add(linkLabels[i]);
        }
        return accum;
    }

    public Set getEdgeLabels(GraphId from)
    {
        Set accum = new HashSet();
        int fromIndex = graphIdIndex(from); if (fromIndex<0) return Collections.EMPTY_SET;
        for (int i=1; i<linkLabels.length; i++) {
	    if (getStoredDist(fromIndex,i).size()>0) {
                accum.add( linkLabels[i] );
	    }
        }
        return accum;
    }

    public Set followLink(GraphId from,String linkLabel)
    {
        int fromIndex = graphIdIndex(from); if (fromIndex<0) return Collections.EMPTY_SET;
        int linkIndex = linkLabelIndex(linkLabel); if (linkIndex<0) return Collections.EMPTY_SET;
        Distribution d = getStoredDist(fromIndex,linkIndex);
        Set accum = new HashSet();
        for (Iterator i=d.iterator(); i.hasNext(); ) {
	    accum.add( i.next() );
        }
        return accum;
    }

    public Distribution walk1(GraphId from,String linkLabel)
    {
        int fromIndex = graphIdIndex(from); if (fromIndex<0) return TreeDistribution.EMPTY_DISTRIBUTION;
        int linkIndex = linkLabelIndex(linkLabel);  if (linkIndex<0) return TreeDistribution.EMPTY_DISTRIBUTION;
        return getStoredDist( fromIndex, linkIndex );
    }

    public Distribution walk1(GraphId from)
    {
        Distribution accum = new TreeDistribution();
        int fromIndex = graphIdIndex(from); if (fromIndex<0) return TreeDistribution.EMPTY_DISTRIBUTION;
        for (int i=0; i<linkLabels.length; i++) {
	    Distribution d = getStoredDist(fromIndex,i);
	    if (d.size()>0) accum.addAll( 1.0, d );
        }
        return accum;
    }

    public String getProperty(GraphId from,String prop)
    {
        return null;
    }

    public String getTextContent(GraphId id)
    {
	return id.getShortName();
    }
    
    public String[] getOrderedEdgeLabels() { return linkLabels; }

    public GraphId[] getOrderedIds() { return graphIds; }

    public Distribution asQueryDistribution(String queryString)
    {
        return CommandLineUtil.parseNodeOrNodeSet(queryString,this);
    }

    static public void main(String[] args)
        throws IOException, FileNotFoundException 
    {
        CompactGraph cg = new CompactGraph();
        if (args.length!=4) {
            System.out.println("usage: sizeFile linkFile nodeFile rowFile");
        } else {
            cg.load(new File(args[0]), new File(args[1]), new File(args[2]), new File(args[3]));
            QueryGUI gui = new QueryGUI(cg);
            new ViewerFrame("QueryGUI", gui );
        }
    }
}
