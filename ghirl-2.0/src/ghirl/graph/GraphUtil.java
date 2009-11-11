package ghirl.graph;

import java.io.*;
import java.util.*;

import ghirl.util.*;

/** Contains default implementations of some Graph operations. */

class GraphUtil
{
    static GraphId[] getOrderedIds(Graph g)
    {
	List idList = new ArrayList();
	for (Iterator i=g.getNodeIterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    idList.add( id.toString() );
	}
	GraphId[] result = (GraphId[]) idList.toArray(new GraphId[idList.size()]);
	Arrays.sort(result);
	return result;
    }

    static String[] getOrderedEdgeLabels(Graph g)
    {
	Set labelSet = new HashSet();
	for (Iterator i=g.getNodeIterator(); i.hasNext(); ) {
	    GraphId id = (GraphId)i.next();
	    labelSet.addAll( g.getEdgeLabels(id) );
	}
	String[] result = (String[]) labelSet.toArray(new GraphId[labelSet.size()]);
	Arrays.sort(result);
	return result;
    }
}
