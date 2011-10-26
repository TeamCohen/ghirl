package ghirl.graph;

import ghirl.util.*;

import java.util.*;
import java.util.regex.Pattern;
import java.io.*;

/**
 * Filter out nodes that pass some test.
 */

public class NodeFilter
{

	protected String linkLabel,targetValue;
	protected Pattern pattern=null;

	/** Expression is of the form: linkLabel=target. A node V passed
	 * the filter if some node matching the target is reachable by an
	 * edge from V with the given linkLabel.  The target might be either
	 * <ul>
	 * <li>A node id, eg '$email' or 'TERM$meeting'
	 * <li>A regex enclosed in forward slashes, eg '/.*@cs.cmu.edu/'
	 * <li>An asterisk, which matches anything
	 * </ul>
	 * Also, the linklabel might be ".", which means to apply
	 * the pattern to the node itself.
	 */
	public NodeFilter(String expression) 
	{
		String parts[] = expression.split("\\s*=\\s*");
		if (parts.length!=2) throw new IllegalArgumentException("bad filter expression: "+expression);
		linkLabel = parts[0];
		targetValue = parts[1];
		if (targetValue.startsWith("/") && targetValue.endsWith("/")) {
			pattern=Pattern.compile(targetValue.substring(1,targetValue.length()-1));
		}
	}

	public boolean accept(Graph graph,GraphId id) 
	{
		Set toSet = ".".equals(linkLabel) ? Collections.singleton(id) : graph.followLink( id, linkLabel );
		for (Iterator j=toSet.iterator(); j.hasNext(); ) {
			GraphId toId = (GraphId)j.next();
			if (matchesTarget(graph,toId)) return true;
		}
		return false;
	}

	protected boolean matchesTarget(Graph graph,GraphId toId)
	{
		if ("*".equals(targetValue)) return true;
		else if (pattern != null) {
			return pattern.matcher(toId.toString()).matches();
		/*else if (targetValue.startsWith("/") && targetValue.endsWith("/")) {
			String pattern = targetValue.substring(1,targetValue.length()-1);
			//System.out.println("matching '"+toId.toString()+"' to /"+pattern+"/");
			return toId.toString().matches(pattern);*/
		} else {
			return toId.toString().equals(targetValue);
		}
	}

	public Distribution filter(Graph graph,Distribution nodeDist)
	{
		Distribution result = new TreeDistribution();
		for (Iterator i=nodeDist.iterator(); i.hasNext(); ) {
			GraphId fromId = (GraphId)i.next();
			double w = nodeDist.getLastWeight();
			if (accept(graph,fromId)) result.add( w, fromId );
		}
		return result;
	}
	
	public Distribution filterTop(Graph graph,Distribution nodeDist, int max) {
		Distribution result = new TreeDistribution();
		int n=0;
		for (Iterator i=nodeDist.orderedIterator(true); i.hasNext() && n < max; n++) {
			GraphId fromId = (GraphId)i.next();
			double w = nodeDist.getLastWeight();
			if (accept(graph,fromId)) result.add( w, fromId );
		}
		return result;
	}
}

