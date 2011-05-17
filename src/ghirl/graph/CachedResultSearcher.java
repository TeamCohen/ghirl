package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** Emulates the GraphSearcher interface using results previously
 * saved in a SearchResultCache.
 */

public class CachedResultSearcher implements GraphSearcher
{
	private Graph graph;
	private SearchResultCache cache;
	private String dbName;
	
	public void setGraph(Graph graph) { this.graph=graph;	}
	public Graph getGraph() { return graph; }

	public CachedResultSearcher(String dbName)
	{
		this.dbName = dbName;
		this.cache = new SearchResultCache(dbName,'r'); 
	}

	public Distribution search(GraphId id)
	{
		Distribution result = cache.get(id);
		if (result==null) throw new IllegalStateException("no cached result available for "+id+" in dbName");
		return result;
	}

	public Distribution search(GraphId id, NodeFilter nodeFilter)
	{
		Distribution ranking = search(id);
		if (nodeFilter==null || ranking==null) return ranking;
		else return nodeFilter.filter(graph,ranking);
	}

	public Distribution search(Distribution queryDistribution,NodeFilter nodeFilter)
	{
		Distribution accum = new TreeDistribution();
		for (Iterator i=queryDistribution.iterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			accum.add( queryDistribution.getLastWeight(), search(id,nodeFilter) );
		}
		return accum;
	}

	public Distribution search(Distribution queryDistribution)
	{
		return search(queryDistribution,null);
	}
}
