package ghirl.learn;

import ghirl.util.*;
import ghirl.graph.*;
import edu.cmu.minorthird.classify.*;


/** A feature extractor for links in a graph. */

public interface LinkFeatureExtractor
{
    public void setGraph(Walkable graph);
    public void setInitialDistribution(Distribution initDist);

    /** Pass in information about what level (distance from start) the
     * walk is currently at. */
    public void setWalkLevel(int level);

    /** A distribution over features associated with links fromId to
     * toId labeled linkLabel. */
    public Distribution toFeatures(GraphId fromId,String linkLabel,GraphId toId);

    /** A distribution over features associated with links fromId
     * labeled linkLabel, regardless of destination */
    public Distribution toFeatures(GraphId fromId,String linkLabel);
}
