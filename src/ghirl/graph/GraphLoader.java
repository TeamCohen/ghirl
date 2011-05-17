package ghirl.graph;


import java.io.*;
import java.util.*;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.util.*;
import ghirl.util.Config;

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
 * <p>By default, GraphLoader will not link a node and its flavor.  If you would
 * like GraphLoader to do so, set the following property:
 * <pre>ghirl.isaFlavorLinks=true</pre>
 * With this property enabled, GraphLoader will, for the example "TEXT$foo", 
 * add an implicit "edge isa TEXT$foo TEXT".  This represents old GHIRL behavior
 * that was phased out in March 2010.</p>
 *  
 * @author William Cohen
 */



public class GraphLoader
{
	private static final Logger log = Logger.getLogger(GraphLoader.class);
	public static final StringEncoder ENCODER = new StringEncoder('%',"\t\n ");

	private Map isaRules; // applied defineIsa rules, as given in the header of graph file.
	protected MutableGraph graph; // graph that is loaded

	// customizable behaviour

	/** If true, automatically provide inverse link for every edge (Default true). */
	public boolean invertLinks = true; 
	/** If true, throw an error for illegal lines (Default false). */
	public boolean throwErrors = false;
	/** If true, print warnings for illegal lines (Default true). */
	public boolean printWarnings = true;
	/** If non-zero, print status after each N lines (Default 10K). */
	public int linesBetweenStatusMessage = 10000;
	/** If true, add isa edges between a node and its flavor (Default false).
	 * This can also be set in a properties file using the "ghirl.isaFlavorLinks" property. */
	public boolean addIsaFlavorLinks;
	
	protected GraphLoader() {}

	/** Create a new loader for this graph */
	public GraphLoader(MutableGraph graph) 
	{ 
		this.graph = graph;
		this.isaRules = new HashMap();
		log.info("created GraphLoader for "+graph);
		addIsaFlavorLinks = Boolean.parseBoolean(Config.getProperty(Config.ISAFLAVORLINKS,"false"));
	}

	/** Return the graph */
	public MutableGraph getGraph() { return this.graph; }

	/** Load some stuff from a file */
	public void load(File file) throws IOException, FileNotFoundException
	{
		if (!file.exists()) file = new File(Config.getProperty(Config.DBDIR)
				+ File.separatorChar
				+ file.getPath());
		log.info("loading graph from "+file+"...");
		LineNumberReader in = new LineNumberReader(new FileReader(file));
		String line = null;
		int numLines = 0;
		ProgressCounter pc = new ProgressCounter("loading "+file,"lines");
		while ((line = in.readLine())!=null) {
			numLines++;
			if (linesBetweenStatusMessage>0 && numLines%linesBetweenStatusMessage==0) 
				log.info("loaded "+numLines+" lines");
			boolean status = loadLine(line);
			if (!status) {
				if (printWarnings) log.warn("illegal line "+in.getLineNumber()+": "+line);
				if (throwErrors) throw new IllegalArgumentException("illegal line "+in.getLineNumber()+": "+line);
			}
			pc.progress();
		}
		pc.finished();
	}

	/**
	 * Process a single line from the file.
	 * @param line
	 * @return True if the line was a valid graph statement, false otherwise.
	 */
	public boolean loadLine(String line)
	{
		if (line.startsWith("#") || line.trim().length()==0) return true;
		// This is kindof dumb -- what we actually want is
		// 1: split first token on "node" or "edge"
		// node: lookup second token with remainder of string
		// edge: 
		String[] parts = line.split("\\s+");
		if ("node".equals(parts[0]) && parts.length>=2) {
			lookupNode(parts[1],textIdShortCut(parts));
			return true;
		} 
		
		if ("edge".equals(parts[0]) && parts.length>=4) {
			// syntax "edge r x y" == "x r y", eg "isa william person"
			String linkLabel = parts[1];
			log.debug("Looking up field 1 "+parts[2]);
			GraphId from = lookupNode(parts[2]);  // id of field 1
			log.debug("Looking up field 2 "+parts[3]);
			GraphId to = lookupNode(parts[3]);    // id of field 2
			log.debug("Adding edge");
			addEdge(linkLabel, from, to);
			return true;
		}
		
		return false;
	}
	
	protected void addEdge(String linkLabel, GraphId from, GraphId to) {
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

	protected GraphId lookupNode(String s)
	{
		return lookupNode(s,"");
	}

	protected GraphId lookupNode(String s,String content) 
	{
		GraphId id = GraphId.fromString(s);
		log.debug("Checking containment of "+s);
		if (graph.contains(id)) {
			//System.err.println("looking up node "+id);
			return id;
		} else {
			log.debug("creating node "+id.toString());
			id = createNode(id,content);
			return id;
		}
	}
	
	protected GraphId createNode(GraphId id,String content) {
		GraphId ret = graph.createNode(id.getFlavor(),id.getShortName(),content);
		if (this.addIsaFlavorLinks && id.getFlavor().length() > 0){
			/* 29 Mar 2010 Katie Rivard
			 * This code used to provide an ISA edge for 
			 * nodes named "$foo" i.e. "edge isa $foo "
			 * which was deemed incorrect.
			 * 
			 * Now it only provides ISA edges for nodes with 
			 * a flavor e.g."TEXT$foo".
			 */
			log.debug("specifying node type");
			graph.addEdge("isa",id,lookupNode(id.getFlavor()));
		}
		return ret;
	}

	static public void main(String[] args) throws IOException
	{
		if (args.length<2) {
			throw new IllegalArgumentException("usage: INDEXFILE GRAPHFILE1 [GRAPHFILE2...] ");
		}

		new MutableTextGraph(args[0],'w').freeze(); // clear args[0]
		for (int i=1; i<args.length; i++) {
			MutableGraph g = new MutableTextGraph(args[0],'a');
			GraphLoader loader = new GraphLoader(g);
			loader.load(new File(args[i]));
			g.freeze();
		}
	}
}
