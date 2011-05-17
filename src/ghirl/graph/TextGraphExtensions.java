package ghirl.graph;

import ghirl.util.Distribution;
import edu.cmu.minorthird.text.TextLabels;

/** 
 * Extended interfaces for a graph that includes indexed text.
*/
public interface TextGraphExtensions
{
    /** Create a new node in the graph of flavor LABELs with short
     * name labelIdShortName based on the TextLabels object textLabels
     * which annotates the content of the node named by textFileIdName.
     */
    public GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,String textFileIdName);
    
    /**
     * Retrieve files using the underlying IR engine.
     */
    public Distribution textQuery(String queryString);    
}
