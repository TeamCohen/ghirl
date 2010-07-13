package ghirl.graph;

import ghirl.util.Config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

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
		BSHGRAPH="bshgraph";
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
		options.addSet(MEMORYGRAPH,0)
			.addOption(MEMORYGRAPH);
		options.addOptionAllSets("load", Separator.BLANK, Multiplicity.ZERO_OR_ONE);
		OptionSet set;
		if ((set = options.getMatchingSet()) == null || hasBadModeSetting(set)) {
			throw new IllegalArgumentException(
					"GraphFactory Usage:"
					+"\n\t-memorygraph [-load file1,file2,...]"
					+"\n\t-textgraph graphName {-r|-w|-a} [-load file1,file2,...]"
					+"\n\t-bshgraph bshFile {-r|-w|-a} [-load file1,file2,...]"
					+"\n\t-graph graphNameOrBshFile {-r|-w|-a} [-load file1,file2,...]"
					+"\n\n"+options.getCheckErrors());
		}
		
		if (set.getSetName().equals(GRAPH)) {
			try {
				Graph g = makeGraphHelper(set,TEXTGRAPH);
				return g;
			} catch(IOException e) {
				try {
					Graph g = makeGraphHelper(set,BSHGRAPH);
					return g;
				} catch (IOException e1) {
					System.err.println("Tried textgraph and bshgraph; both failed:");
					e.printStackTrace();
					e1.printStackTrace();
				}
			}
		} else {
			try {
				return makeGraphHelper(set,set.getSetName());
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
	private char getMode(OptionSet set) {
		String name = set.getSetName();
		if (! name.equals(MEMORYGRAPH)) {
			if (set.isSet("r")) return 'r';
			if (set.isSet("w")) return 'w';
			if (set.isSet("a")) return 'a';
		}
		return 'r';
	}
	
	private Graph makeGraphHelper(OptionSet set, String pseudonym) throws IOException {
		Graph g=null; char mode = 'w';
		if (TEXTGRAPH.equals(pseudonym)) {
			String graphName = set.getOption(set.getSetName()).getResultValue(0);
			mode = getMode(set);
			if ('r'==mode) g = new TextGraph(graphName);
			else           g = new MutableTextGraph(graphName,mode);
		} else if (BSHGRAPH.equals(pseudonym)) {
			String bshname = set.getOption(set.getSetName()).getResultValue(0);
			mode = getMode(set);
			if ('r'==mode) g = BshUtil.toObject(bshname, Graph.class);
			else           g = BshUtil.toObject(bshname, MutableGraph.class);
		} else if (MEMORYGRAPH.equals(pseudonym)) {
			g = new BasicGraph();
		}
		if (g == null) throw new IllegalStateException("Specified options did not result in a graph?");
		
		if (set.isSet("load") && mode != 'r') {
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
		return g;
	}
	
	public static void main(String[] args) {
		Graph g = makeGraph(args);
		if (g instanceof Closable) ((Closable)g).close();
	}
}
