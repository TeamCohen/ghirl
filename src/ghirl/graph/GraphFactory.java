package ghirl.graph;

import ghirl.util.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.TreeMap;

import ml.options.OptionSet;
import ml.options.Options;
import ml.options.Options.Multiplicity;
import ml.options.Options.Separator;

import org.apache.log4j.Logger;

/**
 * Flexible graph creation methods using commandline-compatible property specification.
 * 
 * Available options:<ul>
 * <li><b>-memorygraph</b> Keeps nodes and edges in memory using a BasicGraph.
 * <li><b>-textgraph graphName</b> Indexes text as well as nodes and edges using
 *  a TextGraph or MutableTextGraph.  Currently all textgraphs are assumed to be
 *  disk-backed.  Specify the disk persistance for nodes and edges using 
 *  -Dghirl.persistanceClass as documented in TextGraph.  Specify the location of
 *  the graph using -Dghirl.dbDir.
 * <li><b>-bshgraph bshFile</b> Creates a graph according to a Beanshell file.
 *  This is the most flexible option, but since beanshells are interpreted, they
 *  can be difficult to debug.  Resulting graph is of Graph or MutableGraph type.
 * <li><b>-graph graphNameOrBshFile<b> Simulate the behavior of CommandLineUtil,
 *  which tries to detect whether the argument is a graph or a beanshell, attempts
 *  to create a graph accordingly, and if that fails, tries the other option.
 *  Resulting graph is of Graph or MutableGraph type.
 * <li><b>-r</b> Create a read-only graph. This graph must already exist on disk.
 * <li><b>-w</b> Create a writeable graph, overwriting any previous contents at
 *  this location.
 * <li><b>-a</b> Create an appendable graph, editing a pre-existing graph or 
 *  creating a new one if nothing exists at this location yet.
 * <li><b>-load file1,file2,...</b> After the graph has been created, use a
 *  GraphLoader to load each file in the comma-separated list.  These files
 *  and any FILE nodes they reference may be in the current directory or in 
 *  ghirl.dbDir.
 * </ul>
 * 
 * A table showing all options is below:
 * <table>
 * <tr>
 * <td>-memorygraph</td><td></td><td>[-load file1,file2,file3,...]</td>
 * </tr><tr>
 * <td>-textgraph graphName</td><td>{-r|-w|-a}</td><td>[-load file1,file2,file3,...]</td>
 * </tr><tr>
 * <td>-bshgraph bshFile</td><td>{-r|-w|-a}</td><td>[-load file1,file2,file3,...]</td>
 * </tr><tr>
 * <td>-graph graphNameOrBshFile</td><td>{-r|-w|-a}</td><td>[-load file1,file2,file3,...]</td>
 * </tr>
 * </table>
 * 
 * <h2>Examples</h2>
 * 
 * Create a TokyoCabinet-backed TextGraph called "tokyo" in the current directory.
 * Replace any graph which is already there with a fresh empty copy, then load
 * the file FBrf_20k.ghirl into the graph.
 * <pre>
 *  $ java -cp ghirl.jar -Dghirl.dbDir=. -Djava.library.path=/usr0/local/lib -Dghirl.persistanceClass=ghirl.graph.PersistantGraphTokyoCabinet ghirl.graph.GraphFactory -textgraph tokyo -w -load FBrf_20k.ghirl 
 * </pre>
 * The argument breakdown:<ul>
 * <li><b>TokyoCabinet-backed:</b> -Dghirl.persistanceClass=ghirl.graph.PersistantGraphTokyoCabinet -Djava.library.path=/usr0/local/lib
 * <li><b>TextGraph</b> called <b>"tokyo":</b> -textgraph tokyo
 * <li>in the <b>current directory:</b> -Dghirl.dbDir=.
 * <li><b>Replace</b> any graph currently there: -w
 * <li><b>load the file FBrf_20k.ghirl</b>: -load FBrf_20k.ghirl
 * </ul>
 * 
 * @author katie
 *
 */
public class GraphFactory {
	private static final Logger logger= Logger.getLogger(GraphFactory.class);
	private static final String
		GRAPH="graph",
		TEXTGRAPH="textgraph",
		MEMORYGRAPH="memorygraph",
		BSHGRAPH="bshgraph",
		COMPACTGRAPH="compactgraph";
	private static final String USAGE = "GraphFactory Usage:"
		+"\n\t-memorygraph [-load file1,file2,...]"
		+"\n\t-textgraph graphName {-r|-w|-a} [-load file1,file2,...]"
		+"\n\t-bshgraph bshFile {-r|-w|-a} [-load file1,file2,...]"
		+"\n\t-graph graphNameOrBshFile {-r|-w|-a} [-load file1,file2,...]";
	private static TreeMap<String,GraphHelper> helpers = new TreeMap<String,GraphHelper>();
	static {
		helpers.put(TEXTGRAPH, new TextGraphBuilder());
		helpers.put(MEMORYGRAPH, new MemoryGraphBuilder());
		helpers.put(BSHGRAPH, new BshGraphBuilder());
		helpers.put(COMPACTGRAPH, new CompactGraphBuilder());
	}
	private static final GraphFactory instance = new GraphFactory();
	public static Graph makeGraph(String ... args) {
		return instance.fromOptions(args);
	}
	private Graph fromOptions(String ... args) {
		Options options = new Options(args,0);
		OptionSet s;
		s = options.addSet(GRAPH,0) // backwards-compatible
			.addOption(GRAPH,Separator.BLANK); addMode(s);
		s = options.addSet(TEXTGRAPH,0)
			.addOption(TEXTGRAPH,Separator.BLANK); addMode(s);
		s = options.addSet(BSHGRAPH,0)
			.addOption(BSHGRAPH,Separator.BLANK); addMode(s);
		options.addSet(COMPACTGRAPH,0)
			.addOption(COMPACTGRAPH);
		options.addSet(MEMORYGRAPH,0)
			.addOption(MEMORYGRAPH);
		options.addOptionAllSets("load", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		OptionSet set;
		if ((set = options.getMatchingSet()) == null || hasBadModeSetting(set)) {
			StringBuilder optionstring = new StringBuilder();
			for(String st : args) optionstring.append("\t").append(st).append("\n");
			throw new IllegalArgumentException(
					USAGE
					+"\n\n"+options.getCheckErrors()
					+"\n\nOn the following options:\n"+optionstring);
		}
		GraphHelper gh = null;
		
		if (set.getSetName().equals(GRAPH)) {
			try {
				return helpers.get(TEXTGRAPH).make(set, TEXTGRAPH);
			} catch(IOException e) {
				try {
					return helpers.get(BSHGRAPH).make(set, BSHGRAPH);
				} catch (IOException e1) {
					System.err.println("Tried textgraph and bshgraph; both failed:");
					e.printStackTrace();
					e1.printStackTrace();
				}
			}
		}  else {
			try {
				return helpers.get(set.getSetName()).make(set, set.getSetName());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		throw new IllegalStateException("Could not create graph");
	}
	private void addMode(OptionSet set) {
		set
			.addOption("r",Multiplicity.ZERO_OR_ONE)
			.addOption("w",Multiplicity.ZERO_OR_ONE)
			.addOption("a",Multiplicity.ZERO_OR_ONE);
	}
	private boolean hasBadModeSetting(OptionSet set) {
		String name = set.getSetName();
		if (   name.equals(TEXTGRAPH) 
			|| name.equals(BSHGRAPH)) {
			return ( (set.isSet("r") ? 1 : 0)
				    +(set.isSet("w") ? 1 : 0)
				    +(set.isSet("a") ? 1 : 0) ) > 1;
		}
		return false;
	}
	private static char getMode(OptionSet set) {
		String name = set.getSetName();
		if (! name.equals(MEMORYGRAPH)) {
			if (set.isSet("r")) return 'r';
			if (set.isSet("w")) return 'w';
			if (set.isSet("a")) return 'a';
		}
		return 'r';
	}
	
	public static void main(String[] args) {
		Graph g = makeGraph(args);
		logger.info("Closing graph...");
		if (g instanceof Closable) ((Closable)g).close();
		logger.info("Closed.");
	}
	
	/** Graph helpers: Each helper knows how to construct and load a particular kind of graph.
	 * Since Text, Bsh, and Memory graphs all load similarly, we can extract that behavior to 
	 * the abstract class.
	 * @author krivard
	 *
	 */
	public abstract static class GraphHelper {
		public abstract Graph constructGraph(OptionSet set, String pseudonym) throws IOException;
		public void loadGraph(Graph g, OptionSet set) throws IOException {
			if (set.isSet("load") && getMode(set) != 'r') {
				GraphLoader loader = new GraphLoader((MutableGraph)g);
				for (String filename : set.getOption("load").getResultValue(0).split(",")) {
					File file = new File(filename);
					if (!file.exists()) {
						file = new File(Config.getProperty(Config.DBDIR)
								        +File.separator+filename);
						if (!file.exists()) 
							throw new FileNotFoundException("Could not load file "+filename+"; file not found.");
					}
					((MutableGraph)g).melt();
					loader.load(file);
				}
				((MutableGraph)g).freeze();
			}
		}
		public Graph make(OptionSet set, String pseudonym) throws IOException {
			Graph g = constructGraph(set, pseudonym);
			loadGraph(g, set);
			return g;
		}
	}
	public static class TextGraphBuilder extends GraphHelper {
		public Graph constructGraph(OptionSet set, String pseudonym) throws IOException {
			String graphName = set.getOption(set.getSetName()).getResultValue(0);
			char mode = getMode(set);
			if ('r'==mode) return new TextGraph(graphName);
			else		   return new MutableTextGraph(graphName,mode);
		}
	}
	public static class BshGraphBuilder extends GraphHelper {
		public Graph constructGraph(OptionSet set, String pseudonym) throws IOException {
			String bshname = set.getOption(set.getSetName()).getResultValue(0);
			char mode = getMode(set);
			if ('r'==mode) return BshUtil.toObject(bshname, Graph.class);
			else           return BshUtil.toObject(bshname, MutableGraph.class);
		}
	}
	public static class MemoryGraphBuilder extends GraphHelper {
		public Graph constructGraph(OptionSet set, String pseudonym) throws IOException {
			return new BasicGraph();
		}
	}
	
	/** CompactGraphBuilder has a special loading procedure that doesn't use a GraphLoader. */
	public static class CompactGraphBuilder extends GraphHelper {
		public Graph constructGraph(OptionSet set, String pseudonym) throws IOException {
			return new CompactGraph();
		}
		public void loadGraph(Graph g, OptionSet set) throws IOException {
			if (!set.isSet("load")) return;
			if (!(g instanceof ICompact)) throw new IllegalStateException("Graph passed to CompactGraphBuilder.loadGraph() not of type ICompact (really messed up)");
			ICompact c = (ICompact) g;
			String loadOption = set.getOption("load").getResultValue(0);
			String[] parts = loadOption.split(",");
			if (parts.length == 1) {
				c.load(parts[0]);
			} else if (parts.length == 4) {
				c.load( new File(parts[0]),
						new File(parts[1]),
						new File(parts[2]),
						new File(parts[3]));
			} else throw new IllegalStateException(USAGE+"\n\n-load option for compact graphs must take one or four comma-separated files; you had "+parts.length+":"
					+"\n\t"+loadOption);
		}
	}
}
