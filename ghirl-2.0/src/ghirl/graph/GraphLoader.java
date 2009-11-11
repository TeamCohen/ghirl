package ghirl.graph;


import java.io.*;
import java.util.*;

import edu.cmu.minorthird.util.*;

/**
 * Loads nodes into a MutableGraph, from a file-based storage.  
 * 
 * <p>
 *
 * Each line in the file loaded by GraphLoader must either be
 * <ol>
 *
 * <li>A blank line, or anything starting with a "#".  These
 * are comments. 
 *
 * <li>A line of the form "edge relation id1 id2".  This creates an
 * edge labeled 'relation' between the node labeled 'id1' and 'id2'.
 * If nodes id1 or id2 have not yet been created they will be created
 * <i>iff they are primitive nodes.</i> FILE and TEXT nodes need to be
 * explicitly declared with one of commands below.
 *
 * <li>A declaration of a FILE node, which is of the form
 * "node FILE$location".  Here "location" should be the name
 * of a valid {@link java.io.File}.  As always with java, if 
 * you're on Windows it's safer to use the forward slash than 
 * the backward slash as a separator.  FILE nodes are 
 * indexed by Lucene, so there will be implicit edges between
 * the FILE node and the terms it contains.
 *
 * <li>A declaration of a TEXT node, which is of the form
 * "node TEXT$id word1 word2 ... wordn".  A TEXT node is
 * also indexed by Lucene, just as if it were a FILE node
 * with the contents "word1 ... wordn".
 *
 * <li>A declaration of a LABELS node, which is of the form "node
 * LABELS$location fileId".  Here location should be the name of a
 * {@link java.io.File} that contains a Minorthird labels file, and
 * fileId is the node identifier of a previously-declared FILE node.
 * All spans in the Minorthird labels file should refer to the
 * documents in that file, and should use the special document
 * identifier "someFile".
 *
 * </ol>
 *
 * <p>If a node declared already exists, the old definition is used and any new content (for a TEXT node, e.g.) is <i>discarded</i>.
 *
 * @author William Cohen
 */



public class GraphLoader
{
	public static final StringEncoder ENCODER = new StringEncoder('%',"\t\n ");

	private Map isaRules; // applied defineIsa rules, as given in the header of graph file.
	private MutableGraph graph; // graph that is loaded

	// customizable behaviour

	/** If true, automatically provide inverse link for every edge. */
	public boolean invertLinks = true; 
	/** If true, throw an error for illegal lines. */
	public boolean throwErrors = false;
	/** If true, print warnings for illegal lines. */
	public boolean printWarnings = true;
	/** If non-zero, print status after each N lines. */
	public int linesBetweenStatusMessage = 10000;

	/** Create a new loader for this graph */
	public GraphLoader(MutableGraph graph) 
	{ 
		this.graph = graph;
		this.isaRules = new HashMap();
		System.out.println("created GraphLoader for "+graph);
	}

	/** Return the graph */
	public Graph getGraph() { return this.graph; }

	/** Load some stuff from a file */
	public void load(File file) throws IOException, FileNotFoundException
	{
		System.err.println("loading graph from "+file+"...");
		LineNumberReader in = new LineNumberReader(new FileReader(file));
		String line = null;
		int numLines = 0;
		ProgressCounter pc = new ProgressCounter("loading "+file,"lines");
		while ((line = in.readLine())!=null) {
			numLines++;
			if (linesBetweenStatusMessage>0 && numLines%linesBetweenStatusMessage==0) 
				System.err.println("loaded "+numLines+" lines");
			if (!line.startsWith("#") && line.trim().length()>0) {
				boolean status = loadLine(line);
				if (!status) {
					if (printWarnings) System.err.println("illegal line "+in.getLineNumber()+": "+line);
					if (throwErrors) throw new IllegalArgumentException("illegal line "+in.getLineNumber()+": "+line);
				}
			}
			pc.progress();
		}
		pc.finished();
	}

	/** Process a single line from the file. */ 
	public boolean loadLine(String line)
	{
		// This is kindof dumb -- what we actually want is
		// 1: split first token on "node" or "edge"
		// node: lookup second token with remainder of string
		// edge: 
		String[] parts = line.split("\\s+");
		if ("node".equals(parts[0]) && parts.length>=2) {
			lookupNode(parts[1],textIdShortCut(parts));
			return true;
		} else if ("edge".equals(parts[0]) && parts.length>=4) {
			// syntax "edge r x y" == "x r y", eg "isa william person"
			String linkLabel = parts[1];
			GraphId from = lookupNode(parts[2]);  // id of field 1
			GraphId to = lookupNode(parts[3]);    // id of field 2
			graph.addEdge( linkLabel, from, to );
			if (invertLinks) graph.addEdge( linkLabel+"Inverse", to, from );
			// also, check if an 'isa' rule is applied
			if (isaRules.keySet().contains(linkLabel)){
				Map indexType = (Map)isaRules.get(linkLabel);
				for (Iterator i=indexType.keySet().iterator(); i.hasNext();){
					Integer index = (Integer)i.next();
					String type = (String)indexType.get(index);
					GraphId node = index.intValue()==1? from : to;
					GraphId typeNode = lookupNode(type);
					graph.addEdge ( "isa", node, typeNode);
					if (invertLinks) graph.addEdge( "isa"+"Inverse", typeNode, node );
				}
			}
			return true;
		}
		else {
			return false;
		}
	}

	private String textIdShortCut(String[] parts)
	{
		if (parts.length<3) return "";
		StringBuffer buf = new StringBuffer("");
		buf.append(parts[2]);
		for (int i=3; i<parts.length; i++) {
			buf.append(" ");
			buf.append(parts[i]);
		}
		return buf.toString();
	}

	private GraphId lookupNode(String s)
	{
		return lookupNode(s,"");
	}

	private GraphId lookupNode(String s,String content) 
	{
		GraphId id = GraphId.fromString(s);
		if (graph.contains(id)) {
			//System.err.println("looking up node "+id);
			return id;
		} else {
			id = graph.createNode(id.getFlavor(),id.getShortName(),content);
			if ((s.split("\\$")).length>1){
				graph.addEdge("isa",id,GraphId.fromString(id.getFlavor()));
			}
			return id;
		}
	}

	static public void main(String[] args) throws IOException
	{
		if (args.length<2) {
			throw new IllegalArgumentException("usage: INDEXFILE GRAPHFILE1 [GRAPHFILE2...] ");
		}

		new TextGraph(args[0],'w').freeze(); // clear args[0]
		for (int i=1; i<args.length; i++) {
			TextGraph g = new TextGraph(args[0],'a');
			GraphLoader loader = new GraphLoader(g);
			loader.load(new File(args[i]));
			g.freeze();
		}
	}
}
