package ghirl.graph;

import edu.cmu.minorthird.util.IOUtil;
import ghirl.util.*;
import java.io.*;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

public class CommandLineUtil
{
	private static Logger log = Logger.getLogger(CommandLineUtil.class);
	/**
	 * 
	 * @param s Graph name or beanshell filename
	 * @return
	 * @deprecated Please consider using GraphFactory.makeGraph(String[] args), which is more flexible and robust.
	 */
	@Deprecated
	public static Graph makeGraph(String s)
    {
		log.info("in make graph: " + s);
    	
        if (s.endsWith(".bsh")) {
            try {
                return (Graph)BshUtil.toObject(s,Graph.class);
            } catch (Exception ex) {
                System.out.println("can't parse bsh script '"+s+"': will try to lookup existing graph named "+s);
                try {
                	return new TextGraph(s);
                } catch(IOException e) { 
                	throw new IllegalStateException("Couldn't make graph",e);
                }
            }
        } else {
            try {
            	log.info("trying new: " + s);
                return new TextGraph(s);
            } catch (Exception ex) {
            	log.info("couldn't find: " + ex.getMessage());
                System.out.println("can't find existing graph '"+s+"': will try using bean shell");
                return (Graph)BshUtil.toObject(s,Graph.class);
            }
        }
    }
    public static Graph annotateGraph(Graph graph,String s)
    {
        return AnnotatableGraph.addAnnotator( graph, (GraphAnnotator)BshUtil.toObject(s,GraphAnnotator.class) );
    }
    public static GraphSearcher loadSearcher(String s)
    {
        try {
            return (GraphSearcher)IOUtil.loadSerialized(new File(s)); 
        } catch (Exception ex) {
            throw new IllegalArgumentException("can't load from '"+s+"'");
        }
    }
    /**
     * Convert a query string to a distribution.  Valid examples:
     * "{foo,bar}" is the uniform distribution over the nodes '$foo'
     * and '$bar'; "{foo, TEXT$bar, FILE$bat}" is uniform distribution
     * over the nodes '$foo' 'TEXT$bar', and 'FILE$bat'; "foo" is
     * equivalent to "{$foo}".
     */
    public static Distribution parseNodeOrNodeSet(String queryString,Graph graph)
    {
        if (queryString.startsWith("{")) {
            int k = queryString.indexOf("}");
            if (k>0) {
                Distribution result = new TreeDistribution();
                String[] setElements = queryString.substring(1,k).split("\\s*,\\s*");
                for (int i=0; i<setElements.length; i++) {
                    GraphId id = GraphId.fromString(setElements[i]);
                    if (!graph.contains(id)) {
                        id = new GraphId(GraphId.DEFAULT_FLAVOR,queryString);
                        if (!graph.contains(id)) {
                            throw new IllegalArgumentException("illegal set element '"+setElements[i]+"'"); 
                        }
                    }
                    result.add(1.0, id);
                }
                return result;
            } else {
                throw new IllegalArgumentException("illegal set '"+queryString+"'");
            }
        } else {
            GraphId id = GraphId.fromString(queryString);
            if (graph.contains(id)) return new TreeDistribution(id);

            id = new GraphId(GraphId.DEFAULT_FLAVOR,queryString);
            if (graph.contains(id)) return new TreeDistribution(id);	

            return null;
        }
    }
}
