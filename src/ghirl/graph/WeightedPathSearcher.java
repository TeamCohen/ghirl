package ghirl.graph;

import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import java.util.Iterator;

public class WeightedPathSearcher extends ProgrammableSearcher {
	public WeightedPathSearcher(String path,boolean traceFlag)
	{
		this(path);
		setTrace(traceFlag);
	}
	public WeightedPathSearcher(String path, Graph graph) {
		this(path);
		setGraph(graph);
	}
	
	public class WeightedLinkStep extends ProgrammableSearcher.LinkStep {

		public WeightedLinkStep(String linkLabel) { super(linkLabel); }
		public Distribution takeStep(Distribution dist)
		{
			Distribution accum = new TreeDistribution();
			for (Iterator i=dist.iterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				accum.addAll(dist.getLastWeight(), takeStep(id));
			}
			return accum;
		}
	}
	

	/**
	 * Create a new PathSearcher for the specified path.
	 * @param path is a space-separated list of edge labels.
	 */
	public WeightedPathSearcher(String path)
	{
		String[] linkLabels = path.split("\\s+");
		ProgrammableSearcher.SearchStep[] steps = new ProgrammableSearcher.SearchStep[linkLabels.length];
		for (int i=0; i<linkLabels.length; i++) {
			steps[i] = new WeightedLinkStep(linkLabels[i]);
		}
		setSteps(steps);
	}
}
