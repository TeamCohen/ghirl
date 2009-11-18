package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** Search by following a fixed series of SearchSteps.
 */

public class ProgrammableSearcher extends AbstractGraphSearcher
{
	private SearchStep[] steps;
	private boolean trace = false;

	public ProgrammableSearcher() { this.steps = null; }
	public ProgrammableSearcher(SearchStep[] steps) { this.steps=steps; }

	public void setSteps(SearchStep[] steps) { this.steps=steps; }
	public void setTrace(boolean flag) { this.trace=flag; }

	public Distribution search(Distribution queryDistribution)
	{
		//System.out.println("trace="+trace+" on search from "+queryDistribution);
		Distribution currentDist = queryDistribution;
		if (trace) System.out.println("from: "+queryDistribution);
		for (int i=0; i<steps.length; i++) {
			Distribution nextDist = steps[i].takeStep(currentDist);
			if (trace) System.out.println("after "+steps[i]+": "+nextDist);
			currentDist = nextDist;
		}
		return currentDist;
	}

	public abstract class SearchStep 
	{
		/** Find all the nodes reachable by following this step in the programmed search. */
		abstract public Distribution takeStep(GraphId id);

		public Distribution takeStep(Distribution dist)
		{
			Distribution accum = new TreeDistribution();
			for (Iterator i=dist.iterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				accum.addAll(1.0, takeStep(id));
			}
			return accum;
		}
	}

	public class LinkStep extends SearchStep
	{
		private String linkLabel;
		public LinkStep(String linkLabel) { this.linkLabel=linkLabel; }
		public Distribution takeStep(GraphId id) { return graph.walk1(id,linkLabel); }
		public String toString() { return "[LinkStep "+linkLabel+"]"; }
	}

	public class GraphSearcherStep extends SearchStep
	{
		public GraphSearcher searcher;
		public GraphSearcherStep(GraphSearcher searcher) { this.searcher=searcher; }
		public Distribution takeStep(GraphId id) { searcher.setGraph(graph); return searcher.search(id); }
		public String toString() { return "[SearcherStep "+searcher+"]"; }
	}
}
