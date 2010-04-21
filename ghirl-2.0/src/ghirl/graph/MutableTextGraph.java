package ghirl.graph;

import java.util.Iterator;

import org.apache.lucene.document.*;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;


import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.Span;
import ghirl.util.Config;
import ghirl.util.FilesystemUtil;

import java.io.*;
import org.apache.log4j.*;

/** A graph structure that can contain "text nodes".  There are implicit
 * links from text nodes to terms, and terms to text nodes, which are
 * maintained by using a lucene index. After creating a MutableTextGraph
 * either by hand or from a .ghirl file, it can be re-opened as a read-only copy
 * using ghirl.graph.TextGraph.
 *
 * <h2>Declaring Graphs</h2>
 * <p>Special nodes maintained by this class, along with the GraphLoader line that
 * creates them:</p>
 *
 * <ul>
 * <li><code>node FILE$filename</code> - a text node containing all the text in the file.
 * <li><code>node TEXT$id word1 ... wordK</code> - a text node containing the text "word1 ... wordK".
 * <li><code>node LABELS$file1 FILE$file2</code> - creates a new "labels" node.  
 * <code>file1</code> is a minorthird <code>.labels</code>
 * file which annotates <code>file2</code> (and no other file). 
 * The document ids in <code>file1</code> should all
 * be "someFile". <i>[Do you mean the literal string, or that the ids should all be the same in
 * any given <code>file1</code>, or that every id in <code>file1</code> should 
 * be a valid file id elsewhere in the graph? -ed]</i>
 *
 * <p>If <code>LABELS$file1</code> asserts that there is a span of spanType <i>type1</i>
 * containing the text <code>TEXT$text1 = "span text 1"</code>, then the relations created are
 * as follows: <ul>
 * <li><code>LABELS$file1 _annotates FILE$file2</code>
 * <li><code> LABELS$file1 _hasSpanType file2//type1</code>
 * <li><code> file2//type1 _hasSpan text1</code>
 * <li><code>LABELS$file1 hasType1 text1</code>
 * </ul>
 * <i>(This should be checked to make sure it's still true!)</i>
 *</ul>
 *
 * <h2>Configuration</h2>
 * <p>Properties read by this class include:</p><ul>
 * <li><code>ghirl.dbDir</code> - REQUIRED - the absolute or relative path where the 
 * persistance should be stored (unused for a memory-resident MutableTextGraph).
 * <li><code>ghirl.persistanceClass</code> - OPTIONAL - the fully-qualified classname
 * to use for the <code>innerGraph</code>.  Defaults to PersistantGraphSleepycat at present.
 * </ul>
 *
 * <h2>For MutableTextGraph developers:</h2>
 * <p>This class was created by splitting the original TextGraph into 
 * read-only and writable components.  When in doubt, I left a method or
 * member variable in TextGraph rather than crossporting it here.  Future
 * edits of this class may need to dive into TextGraph as well to pick apart
 * code. <i>--Katie</i></p>
 * 
 * <p><s>MutableTextGraph maintains a list of all valid GraphId's and all valid
 * edge labels.  Each of these are stored in two places: a file,
 * and a TreeSet.  Things are added to the TreeSet when they are
 * created, and inserted in the file after a freeze().</s>   TextGraph node and
 * edge caching for the <code>getOrderedEdgeLabels</code> and <code>
 * getOrderedIds</code> methods is handled by its <code>innerGraph</code>.  
 * TextGraph-specific edges are appended at request time.</p>
 * 
 * <p>The methods <code>freeze()</code> and <code>melt()</code> are used to make 
 * sure that everything is added to the
 * index before it's ever accessed.  The first access forces a
 * 'freeze' operation, and after that no text files can be added
 * without throwing an error.</p>
 */
public class MutableTextGraph extends TextGraph implements MutableGraph {
	private static final Logger log = Logger.getLogger(MutableTextGraph.class);
	protected MutableGraph innerGraph;
	/** Specified by Lucene. */
	private IndexWriter writer = null;
	/** Used instead of indexFileName if writer is memory-resident */
	private RAMDirectory writerDirectory = null;
	
	/** Create a text graph.  Data will be stored under the directory named by
	 * the property ghirl.dbDir (in the properties file ghirl.properties).
	 *
	 * @param mode 'w' for write, 'r' for read, 'a' for append.
	 */
	public MutableTextGraph(String fileStem, char mode) {
		this(fileStem, mode, null);
	}
	
	/** Create a text graph with a pre-initialized innerGraph.
	 * 
	 * @param graph The innerGraph this text graph should use.
	 * @param mode One of 'r' read, 'a' append, 'w' write.  Write overwrites 
	 * existing graphs.  Append creates a new one if one doesn't exist already.
	 */
	public MutableTextGraph(String fileStem, char mode, MutableGraph graph)
	{
		this.innerGraph = graph;
		
		setupLuceneSettings(fileStem);
		
//		 check that the mode is valid
		if ("war".indexOf((int) mode) < 0) {
			throw new IllegalArgumentException("mode must be 'r' 'a' or 'w'");
		}
		
//		 open the db, optionally clearing beforehand
		if ('w'==mode) {
			//FilesystemUtil.rm_r(new File(dbFileName));
			FilesystemUtil.rm_r(new File(indexFileName));
		}
		setupInnerGraph(mode, MutableGraph.class);

		if (mode=='w') {
			// re-open the text index
			setupWriteableIndex(true);
		} else if (mode=='a') {
			innerGraph.melt();
			// only create the index if it does not exist yet
			setupWriteableIndex( !(new File(indexFileName).exists()) );
		} else if (mode=='r') {
			setupReadonlyIndex();
			freeze();
		}
	}
	

	/** Create a memory-resident text graph.
	 */
	public MutableTextGraph()
	{
		indexFileName = dbFileName = null;
		innerGraph = new BasicGraph();
		try {
			writerDirectory = new RAMDirectory();
			writer = new IndexWriter(writerDirectory, analyzer, true);
			log.info("memory-based writer = "+writer);
		} catch (IOException ex) {
			throw new IllegalStateException("can't open memory-resident writer: "+ex);
		}
	}

	/** Create a memory-resident text graph and initialize it with data
	 * from files readable by GraphLoader.
	 */
	public MutableTextGraph(String graphFileName)
	{
		this(new String[]{graphFileName});
	}

	/** Create a memory-resident text graph and initialize it with data
	 * from files readable by GraphLoader
	 */
	public MutableTextGraph(String[] graphFileNames)
	{
		this();
		GraphLoader loader = new GraphLoader(this);
		for (int i=0; i<graphFileNames.length; i++) {
			try {
				log.info("loading "+graphFileNames[i]);
				loader.load(new File(graphFileNames[i]));
			} catch (IOException ex) {
				throw new IllegalArgumentException("can't load '"+graphFileNames[i]+"': "+ex);
			}
		}
		this.freeze();
	}
	

	protected void setupWriteableIndex(boolean create) {
		try {
			writer = new IndexWriter(indexFileName, analyzer, create);
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't create lucene writer: "+ex);
		}
	}
	
	protected void setInnerGraph(Graph g) { this.innerGraph = (MutableGraph) g; }
	protected Graph getInnerGraph() { return this.innerGraph; }
	
	public String toString() 
	{ 
		if (indexFileName!=null) return super.toString();
		else return "[TextGraph RAMDir:"+writerDirectory+"]";
	} 
	
	
	@Override
	public boolean isFrozen() { return frozenFlag; }
	
	@Override
	/**
	 * these are used to make sure that everything is added to the
	 * index before it's ever accessed.  The first access forces a
	 * 'freeze' operation, and after that no text files can be added
	 * to the unmelted graph without throwing an error.
	 */
	protected void checkFrozen() { if (!frozenFlag) freeze(); }

	public void setProperty(GraphId id,String prop,String val) { innerGraph.setProperty(id,prop,val);  }

	/** Stop querying graph, and set it up for extending. */
	public void melt()    
	{
		if (!frozenFlag) return;
		innerGraph.melt();
		if (writer==null) {
			try {
				if (indexFileName!=null) {
					writer = new IndexWriter(indexFileName, analyzer, false);
				} else {
					log.info("opening memory-resident index for directory "+writerDirectory);
					writer = new IndexWriter(writerDirectory, analyzer, false);
				}
			} catch (IOException ex) {
				throw new IllegalArgumentException("can't create lucene writer: "+ex);
			}
			index = null;
			searcher = null;
		}
		frozenFlag=false;
	}
	
	/** Stop extending graph, and set it up for querying */
	public void freeze()
	{
		if (frozenFlag) return;
		log.info("freezing "+this);
		innerGraph.freeze();
		try {
			// optimize lucene index, and prepare for reading
			if (writer!=null) {
				writer.optimize();
				writer.close();
				writer = null;
			}
			if (indexFileName!=null) {
				index = IndexReader.open(indexFileName); 
			} else {
				log.info("opening index for memory-resident index");
				index = IndexReader.open(writerDirectory);
			}
			searcher = new IndexSearcher(index);
			frozenFlag = true;
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't open file",ex);
		}
	}
	
	 /**
	 * ? Disallow creation of term nodes, and make sure file nodes are indexed.
	 */
	public GraphId createNode(String flavor,String shortName)
	{
		return createNode(flavor,shortName,"");
	}

	/**
	 * Create a new node in the graph.  There are special cases for
	 * nodes with flavor TEXT, FILE and LABELS.  For FILE nodes, the
	 * shortName should be a file, which will be read in and indexed
	 * by Lucene.  For TEXT nodes, the 'obj' parameter should be a
	 * string, which will be indexed by Lucene.  For LABELS nodes, the
	 * shortName should be the name of a Minorthird .labels and the
	 * 'obj' parameter should be the name of a text file which is
	 * annotated by the .labels file.
	 */
	public GraphId createNode(String flavor,String shortName,Object obj)
	{
		//System.out.println("creating node "+flavor+"$"+shortName);

		if (TERM_TYPE.equals(flavor)) 
			throw new IllegalArgumentException("can't create "+TERM_TYPE+" nodes");
		if (frozenFlag)
			throw new IllegalStateException("index is frozen - no new text files can be added!");

		GraphId id = new GraphId(flavor,shortName);
		if (contains(id)) {
			//System.out.println(id+" exists");
			return id;
		} else {
			if (FILE_TYPE.equals(flavor)) {
//				try {
//					indexDocument(flavor,shortName,IOUtil.readFile(new File(shortName)));
//				} catch (IOException ex) {
					try {
						indexDocument(flavor,shortName,IOUtil.readFile(disambiguateFile(shortName, true)));
					} catch (IOException ex2) {
						//throw new IllegalArgumentException("can't read file "+shortName);
						log.error("can't read file "+shortName+" in "
								+new File(shortName).getAbsolutePath()
								+" or "
								+new File(inBaseDir(shortName)).getAbsolutePath());
						indexDocument(flavor,shortName,"file error:"+ex2.toString());
					}
//				}
				//System.out.println("indexed file "+shortName);
			} else if (TEXT_TYPE.equals(flavor)) {
				//System.out.println("indexing TEXT "+obj);
				if (((String)obj).trim().equals("")) {
					log.warn(flavor+"$"+shortName+" is defined as an empty string");
				}
				indexDocument(flavor,shortName,(String)obj);
			} else if (LABELS_TYPE.equals(flavor)) {
				//System.err.println("labels node "+shortName+" "+obj);
				createLabelsNode(shortName,(String)obj);
			}
			return innerGraph.createNode(flavor,shortName);
		}
	}
	
	private File disambiguateFile(String name, boolean throwException) {
		File f = new File(name);
		if (!f.exists()) f = new File(inBaseDir(name));
		if (!f.exists()) {
			String err = "No file exists for "+name+" in JVM direcotry or in ghirl.dbDir!";
			if (throwException) throw new IllegalArgumentException(err);
			log.error(err);
		}
		return f;
	}
	
	private GraphId createLabelsNode(String shortName,String textFileIdString)
	{
		//System.err.println("createLabelsNode '"+shortName+"' '"+textFileIdString+"'");


		// the spec is the id of the document being annotated. note
		// that the file corresponding to textFileIdString may not
		// have been indexed yet - the second line ensures it will be
		GraphId tmp = GraphId.fromString( textFileIdString ); 
		//System.err.println("tmp id: "+tmp.getFlavor()+" $ "+tmp.getShortName());
		if (!FILE_TYPE.equals(tmp.getFlavor())) {
			log.error("warning: LABELS should refer to a file: LABELS$"+shortName+" textFileIdString");
		}
		GraphId textFileId  = createNode( FILE_TYPE, tmp.getShortName() );
		//System.err.println("textFileId: '"+textFileId+"'");
		// get fileContents from the file itself, as
		// getTextContent(id) requires the index to be complete...

		// create the textbase/labels
		TextLabels textLabels = null;
		TextBase textBase = null;
//		try {
//			textBase = textBaseFor( new File(textFileId.getShortName()) );
//		} catch (IOException ex) {
			try {
				textBase = textBaseFor( disambiguateFile(textFileId.getShortName(), true));
			} catch (IOException ex2) {
				throw new IllegalArgumentException("can't read file "+textFileId.getShortName()+" in "
						+new File("").getAbsolutePath()
						+" or "
						+new File(inBaseDir("")).getAbsolutePath());
			}
//		}
//		try {
//			textLabels = new TextLabelsLoader().loadOps( textBase, new File(shortName) );
//		} catch (IOException ex) {
			try {
				textLabels = new TextLabelsLoader().loadOps( textBase, disambiguateFile(shortName, true) );
			} catch (IOException ex2) {
				throw new IllegalArgumentException("can't read file "+shortName+" in "
						+new File("").getAbsolutePath()
						+" or "
						+new File(inBaseDir("")).getAbsolutePath());
			}
//		}
		return createLabelsNode(textLabels,shortName,textFileId);
	}
	
	public void addEdge(String linkLabel,GraphId from,GraphId to)
	{
		if (isFrozen()) {
			throw new IllegalStateException("adding edge "+linkLabel+" "+from+" -> "+to+" to frozen graph "+this);
		}
		innerGraph.addEdge(linkLabel,from,to);
	}

	/** Create a new node in the graph of flavor LABELs with short
	 * name labelIdShortName based on the TextLabels object textLabels
	 * which annotates the content of textFileId.
	 */
	private GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,GraphId textFileId)
	{
		String docName = textFileId.getShortName();

		// link the textFile to the labelFile
		//System.err.println("creating inner node "+LABELS_TYPE+" $ "+labelIdShortName);
		GraphId labelFileId =  innerGraph.createNode(LABELS_TYPE,labelIdShortName);

		// edge labels defined here should also be
		// cached out to disk in the 'freeze' routine
		
		//System.err.println("labels node "+labelFileId+" "+textFileId+": adding edges");
		addEdge(ANNOTATES_TEXT_EDGE_LABEL, labelFileId, textFileId );
		addEdge(ANNOTATES_TEXT_EDGE_LABEL+"Inverse", textFileId, labelFileId );

		// now, create text nodes for each span in this file and link them
		// to the textFileId
		for (String type : textLabels.getTypes()) {
			String typeIdShortName = docName+"//"+type;
			String typeEdgeLabel = "has"+type.substring(0,1).toUpperCase()+type.substring(1);
			GraphId typeId = innerGraph.createNode( GraphId.DEFAULT_FLAVOR, typeIdShortName );
			addEdge(HASSPANTYPE_EDGE_LABEL, labelFileId, typeId );
			addEdge(HASSPANTYPE_EDGE_LABEL+"Inverse", typeId, labelFileId );
			//System.err.println(" - adding type "+type);
			for (Iterator<Span> j=textLabels.instanceIterator(type); j.hasNext(); ) {
				Span span = j.next();
				int lo = span.documentSpanStartIndex();
				int hi = lo+span.size();
				String spanIdShortName = docName+"//"+lo+"/"+hi;
				//System.err.println(" -- adding span "+span.asString());
				GraphId spanId = getNodeId( TEXT_TYPE, spanIdShortName );
				if (spanId==null) {
					//System.err.println(" -- creating node");
					spanId = createNode( TEXT_TYPE, spanIdShortName, span.asString() );
				} 
				//System.err.println(" -- adding edges");
				addEdge(HASSPAN_EDGE_LABEL, typeId, spanId);
				addEdge(HASSPAN_EDGE_LABEL+"Inverse", spanId, typeId);
				// short cut which actually includes the name of the type
				addEdge(typeEdgeLabel, labelFileId, spanId );
				addEdge(typeEdgeLabel+"Inverse", spanId, labelFileId );
			}
		}
		return labelFileId;
	}
	
	/** Create a new node in the graph of flavor LABELs with short
	 * name labelIdShortName based on the TextLabels object textLabels
	 * which annotates the content of the node named by textFileIdName.
	 */
	public GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,String textFileIdName)
	{
		GraphId id = GraphId.fromString(textFileIdName);
		return createLabelsNode(textLabels,labelIdShortName,id);
	}
	
	
	 /** 
	  * Create and index a document (if the flavor is 'FILE_TYPE') or
	  * a literal string that has been encoded (if the flavor is 'TEXT_TYPE') 
	  * @param flavor Flavor of the node
	  * @param shortName Name of the node
	  * @param content Text content of a literal node; filename for a file node.
	 */
	protected void indexDocument(String flavor,String shortName,String content)
	{
		try {
			writer.addDocument( textDocument(flavor,shortName,content) );
		} catch (IOException ex) {
			throw new IllegalStateException("index error: "+ex);
		}
	}
	
	/**
	 * Create and index a document for the file named in the node's text contents.
	 * @param flavor
	 * @param textHandle
	 * @param content
	 * @return
	 */
	private Document textDocument(String flavor,String textHandle,String content)
	{
		Document doc = new Document();
		doc.add(Field.Keyword(TEXT_HANDLE_FIELD, textHandle));
		doc.add(Field.Keyword(FLAVOR_FIELD, flavor));
		// Add the contents of the file a field named "contents".  
		// tokenize and store the term vector as well
		doc.add(new Field(CONTENTS_FIELD, new String(content), true, true, true, true)); 
		return doc;
	}
	
	/** convenience method - summarize the graph, optionally display some nodes
	 * @param args
	 */
	static public void main(String[] args)
	{
		//TextGraph g = new TextGraph(args[0],'r') ;
		MutableTextGraph g = new MutableTextGraph(args[0]);
		boolean gui = false;
		int argp = 1;
		while (argp<args.length) {
			if ("-gui".equals(args[argp])) {
				gui = true;
			} else if ("-tokens".equals(args[argp])) {
				for (Iterator i=g.getNodeIterator(); i.hasNext(); ) {
					GraphId id = (GraphId)i.next();
					if (TERM_TYPE.equals(id.getFlavor())) {
						System.out.println(id.getShortName());
					}
				}
			}  else if ("-walk".equals(args[argp])) {
				GraphId id = GraphId.fromString(args[++argp]);		
				if (!g.contains(id)) {
					System.out.println(id+" not in graph "+args[0]);
				} else {
					System.out.println(g.walk1(id));
				}
			}  else if ("-cacheWalk".equals(args[argp])) {
				GraphId id = GraphId.fromString(args[++argp]);		
				if (!g.contains(id)) {
					System.out.println(id+" not in graph "+args[0]);
				} else {
					System.out.println(new CachingGraph(g).walk1(id));
				}
			} else if ("-edges".equals(args[argp])) {
				GraphId id = GraphId.fromString(args[++argp]);		
				if (!g.contains(id)) {
					System.out.println(id+" not in graph "+args[0]);
				} else {
					System.out.println(g.getEdgeLabels(id));
				}
			} else if ("-cont".equals(args[argp]) || "-content".equals(args[argp])) {
				boolean summarize = "-cont".equals(args[argp]);
				GraphId id = GraphId.fromString(args[++argp]);
				if (!g.contains(id)) {
					System.out.println(id+" not in graph "+args[0]);
				} else {
					String content = g.getTextContent(id);
					int len = content.length();
					if (summarize) {
						len = Math.min(len,60);
						content = content.replaceAll("\n"," ");
					}
					System.out.println(id+" = "+content.substring(0,len));
				}
			} else {
				System.out.println("unknown argument "+args[argp]);
			}
			argp++;
		}
	}
}
