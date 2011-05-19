package ghirl.graph;

import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class BasicWeightedGraph extends BasicGraph {
	protected Map<String,Distribution> weightedEdgeMap = new HashMap<String,Distribution>();
	
	@Override
	public void addEdge(String linkLabel,GraphId from,GraphId to) {
		this.addEdge(linkLabel,from,to,1.0);
	}
	@Override
	public void addEdge(String linkLabel,GraphId from,GraphId to,double wt) {
		super.addEdge(linkLabel, from, to);
		String key = edgeKey(from,linkLabel);
		Distribution oldSet = weightedEdgeMap.get(key);
		if (oldSet==null) weightedEdgeMap.put(key,(oldSet = new TreeDistribution()));
		oldSet.add(wt, to);
	}
	@Override
	public Distribution walk1(GraphId from, String linkLabel)
	{
		return weightedEdgeMap.get(edgeKey(from,linkLabel)).copy();
	}

	@Override
	public Distribution walk1(GraphId from)
	{
		Distribution walkResult = new TreeDistribution();
		for (Iterator i=getEdgeLabels(from).iterator(); i.hasNext(); ) {
			String linkLabel = (String)i.next();
			Distribution edgeResult = weightedEdgeMap.get(edgeKey(from,linkLabel));
			walkResult.addAll(edgeResult.getTotalWeight(),edgeResult);
		}
		return walkResult;
	}
}
