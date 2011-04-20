package ghirl.graph;

import java.io.*;
import java.util.*;

import ghirl.util.*;
import edu.cmu.minorthird.util.UnionIterator;
import edu.cmu.minorthird.text.TextLabels;

/** A TextGraph which is defined by two "nested" TextGraph's.
 *
 * <p> Operationally, queries to the NestedTextGraph are answered as
 * if they were made to the union of the two text graphs.  Updates to
 * the graph are applied to the 'outer' TextGraph.
 *
 * <p>Pragmatically, this means that if you create a NestedTextGraph
 * from an innerGraph, it will initially look like innerGraph.  But if
 * you modify it, the innerGraph will not be changed, so you can at
 * any point easily revert to the old innerGraph.
 *
 * @author William Cohen
*/


public class NestedTextGraph extends NestedGraph implements TextGraphExtensions,Closable
{
    private MutableTextGraph outer;
    private Graph inner;
    public NestedTextGraph(Graph inner)
    {
    	super(inner);
        if (inner instanceof TextGraph) TextGraph.mergeDocFrequencies((TextGraph) inner,outer);
    }
    public String toString() 
    { 
        return "[NestedTextGraph inner:"+inner+" outer: "+outer+"]"; 
    }
    /** Create a new node in the graph of flavor LABELs with short
     * name labelIdShortName based on the TextLabels object textLabels
     * which annotates the content of the node named by textFileIdName.
     */
    public GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,String textFileIdName)
    {
	return outer.createLabelsNode(textLabels,labelIdShortName,textFileIdName);
    }

    public Distribution textQuery(String queryString)
    {
    	if (inner instanceof TextGraph) 
    		return mergeDist( ((TextGraph)inner).textQuery(queryString), outer.textQuery(queryString) );
    	return outer.textQuery(queryString);
    }

    static public void main(String[] args) throws IOException
    {
        MutableTextGraph g1 = new MutableTextGraph(args[0]);
        g1.freeze();
        NestedTextGraph g2 = new NestedTextGraph(g1);
        GraphLoader loader = new GraphLoader(g2);
        loader.load(new File(args[1]));
	//QueryGUI gui = new QueryGUI(g2);
	//new edu.cmu.minorthird.util.gui.ViewerFrame("QueryGUI", gui );
        GraphId id = GraphId.fromString(args[2]);
        if (!g2.contains(id)) {
            System.out.println(id+" not in graph "+args[0]);
        } else {
            System.out.println(g2.walk1(id));
        }
    }
}
