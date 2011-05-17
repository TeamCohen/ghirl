package ghirl.graph;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

public class TextNodeFilter extends NodeFilter {
	/**Expression is of the form: linkLabel=target. A node V passes
	 * the filter if some node whose text content matches the target 
	 * is reachable by an edge from V with the given linkLabel.  
	 * The target might be:
	 * <ul>
	 * <li>The contents of the document
	 * <li>A regex enclosed in forward slashes, eg '/.*@cs.cmu.edu/'
	 * <li>An asterisk, which matches anything
	 * </ul>
	 * Also, the linklabel might be ".", which means to apply
	 * the pattern to the text contents of the node itself.
	 * @param expression
	 */
	public TextNodeFilter(String expression) {
		super(expression);
		if ("*".equals(targetValue)) isany=true;
		else if (targetValue.startsWith("/") && targetValue.endsWith("/")) {
			isregex=true;
			pattern = targetValue.substring(1,targetValue.length()-1);
		}
	}
	
	protected boolean isany=false;
	protected boolean isregex=false;
	protected String pattern;

	@Override
	protected boolean matchesTarget(Graph graph, GraphId toId)
	{
		if (isany) return true;
		else if (isregex) {
			//System.out.println("matching '"+toId.toString()+"' to /"+pattern+"/");
			return graph.getTextContent(toId).matches(pattern);
		} else {
			return graph.getTextContent(toId).equals(targetValue);
		}
	}

}
