package ghirl.graph;

import java.util.*;
import ghirl.util.*;

/** Search along a specified path of links. 
 */

public class PathSearcher extends ProgrammableSearcher
{
    public PathSearcher(String path,boolean traceFlag)
    {
	this(path);
	setTrace(traceFlag);
    }

    /** @options path is a space-separated list of edge labels.
     */
    public PathSearcher(String path)
    {
	String[] linkLabels = path.split("\\s+");
	ProgrammableSearcher.SearchStep[] steps = new ProgrammableSearcher.SearchStep[linkLabels.length];
	for (int i=0; i<linkLabels.length; i++) {
	    steps[i] = new ProgrammableSearcher.LinkStep(linkLabels[i]);
	}
	setSteps(steps);
    }
}
