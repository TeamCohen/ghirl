package ghirl.learn;

import ghirl.graph.*;
import ghirl.util.*;
import edu.cmu.minorthird.classify.*;
import java.io.*;
import java.util.*;

/** Sample feature extractors. */

public class LinkFEs
{
    public static final Distribution EMPTY_DISTRIBUTION = TreeDistribution.EMPTY_DISTRIBUTION;

    /** A base class with useful defaults. */
    public static class NullFE implements LinkFeatureExtractor, Serializable
    {
        static private final long serialVersionUID = 1;
        transient protected Distribution initDist;
        transient protected Walkable graph;
        transient protected int level;
        public void setInitialDistribution(Distribution initDist) { this.initDist=initDist; }
        public void setGraph(Walkable graph) { this.graph=graph; }
        public void setWalkLevel(int level) { this.level=level; }
        public Distribution toFeatures(GraphId fromId,String linkLabel,GraphId toId) { return EMPTY_DISTRIBUTION; }
        public Distribution toFeatures(GraphId fromId,String linkLabel) { return EMPTY_DISTRIBUTION; }
    }

    /** Outputs features "term.FOO" for edges entering nodes TERM$foo.
     */
    final public static class TermFE extends NullFE
    {
        public Distribution toInstance(GraphId fromId,String linkLabel,GraphId toId) {
	    if (toId.getFlavor().equals(TextGraph.TERM_TYPE)) 
                return new TreeDistribution( new Feature(new String[]{"term",toId.getShortName()}) ) ;
	    else 
                return EMPTY_DISTRIBUTION;
        }
    }

    /** Outputs features 'edge.EDGELABEL' for edges with the label EDGELABEL.
     */
    final public static class EdgeLabelFE extends NullFE
    {
        public Distribution toFeatures(GraphId fromId,String linkLabel) {
	    return new TreeDistribution( new Feature(new String[]{"edge",linkLabel}) );
        }
    }

    /** Outputs a variety of edge-related features. */
    final public static class TypedEdgeLabelFE extends NullFE
    {
        private Boolean useLevel=null,useFrom=null,useVia=null,useTo=null;

        /** Defaults to using only the 'fromVia' features. 
         */
        public TypedEdgeLabelFE() { this(Boolean.FALSE,Boolean.TRUE,Boolean.TRUE,Boolean.FALSE); }

        /**
         */
        public TypedEdgeLabelFE(Boolean level,Boolean from,Boolean via,Boolean to)
        {
	    this.useLevel=level;
	    this.useFrom=from;
	    this.useVia=via;
	    this.useTo=to;
        }
        public Distribution toFeatures(GraphId fromId,String linkLabel,GraphId toId) 
        {
	    Distribution result = new TreeDistribution();
            List accum = new ArrayList();
            accum.add( new ArrayList() );
            accum = appendFeatures( accum, useLevel, "level", Integer.toString(level) );
            accum = appendFeatures( accum, useFrom, "from", getType(fromId) );
            accum = appendFeatures( accum, useVia, "via", linkLabel );
            accum = appendFeatures( accum, useTo, "to", getType(toId) );
            for (Iterator i=accum.iterator(); i.hasNext(); ) {
                List protoFeature = (List)i.next();
                String[] protoFeaturePath = (String[])protoFeature.toArray( new String[protoFeature.size()] );
                result.add(1.0, new Feature(protoFeaturePath));
            }
	    return result;
        }
        private List appendFeatures(List list,Boolean flag,String key,String value)
        {
            List result = new ArrayList();
            for (Iterator i=list.iterator(); i.hasNext(); ) {
                List protoFeature = (List)i.next();
                List copy = new ArrayList(); 
                if (flag==null || flag.booleanValue()) {
                    copy.addAll( protoFeature ); copy.add(key); copy.add(value);
                    result.add( copy );
                }
                if (flag==null || !flag.booleanValue()) {
                    result.add( protoFeature );
                }
            }
            return result;
        }
        private String getType(GraphId id)
        {
	    Set typeIds = graph.followLink(id,"isa");
	    if (typeIds==null || typeIds.size()==0) {
                if (!id.getFlavor().equals(GraphId.DEFAULT_FLAVOR)) return "a"+id.getFlavor();
                else return "noType";
            } else {
                String rawType = typeIds.iterator().next().toString();
                return "a"+rawType.substring(0,1).toUpperCase()+rawType.substring(1);
	    }
        }
    }

    /** Outputs features of the form
     * 'EDGELABEL1.EDGELABEL2.....EDGELABELk' for edges labeled
     * EDGELABELk from nodes reachable from the initial distribution
     * via a path of links labeled
     * EDGELABEL1.EDGELABEL2.....EDGELABEL[k-1].  For efficiency, the
     * path is searched for with a bounded cost search, and if no path
     * is found, then EDGELABEL1.EDGELABEL2.....EDGELABEL[k-1] is
     * replaced with '_someNode'.
     */
    public static class RootedEdgeLabelFE extends NullFE
    {
        transient private BestPathFinder bestPathFinder;
        static private final int MAX_STEPS = 50;

        public void setInitialDistribution(Distribution initDist) { this.initDist=initDist; reinit(); }
        public void setGraph(Graph graph) { this.graph=graph; reinit(); }
        private void reinit() { bestPathFinder = null; }

        public Distribution toFeatures(GraphId fromId,String linkLabel) 
        {
	    if (bestPathFinder==null) bestPathFinder = new BestPathFinder(graph,initDist,MAX_STEPS);
	    String[] path = bestPathFinder.bestEdgeLabelPath(fromId);
	    if (path==null) {
                return new TreeDistribution( new Feature(new String[]{"_someNode",linkLabel}) );
	    } else {
                String[] s = new String[path.length+1];
                for (int i=0; i<path.length; i++) s[i] = path[i];
                s[path.length] = linkLabel;
                return new TreeDistribution( new Feature(s) );
	    }
        }
    }

}
