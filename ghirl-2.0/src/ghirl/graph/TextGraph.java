package ghirl.graph;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.document.*;
import org.apache.lucene.store.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.queryParser.*;


import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.text.TextBase;
import edu.cmu.minorthird.text.TextLabels;
import edu.cmu.minorthird.text.MutableTextLabels;
import edu.cmu.minorthird.text.BasicTextLabels;
import edu.cmu.minorthird.text.BasicTextBase;
import edu.cmu.minorthird.text.TextLabelsLoader;
import edu.cmu.minorthird.text.Span;
import ghirl.util.*;

import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.MalformedURLException;
import org.apache.log4j.*;

//
// notes: new IndexWriter(new RAMDirectory(), analyzer, true ) will
// create a memory-based index. Can be used with
// IndexReader.open(indexWriter.getDirectory()).
//
// status: memory-resident textgraph is now complete

/** A graph structure that can contain "text nodes".  There are implicit
 * links from text nodes to terms, and terms to text nodes, which are
 * maintained by using a lucene index.
 *
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
 * be "someFile". <i>(Do you mean the literal string, or that the ids should all be the same in
 * any given <code>file1</code>?)</i>
 *
 * <p>If <code>LABELS$file1</code> asserts that there is a span of spanType <i>type1</i>
 * containing the text <code>TEXT$text1 = "span text 1", then the relations created are
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
 * persistance should be stored (unused for a memory-resident TextGraph).
 * <li><code>ghirl.persistanceClass</code> - OPTIONAL - the fully-qualified classname
 * to use for the <code>innerGraph</code>.  Defaults to PersistantGraphSleepycat at present.
 * </ul>
 *
 * <h2>For TextGraph developers:</h2>
 * <p><s>TextGraph maintains a list of all valid GraphId's and all valid
 * edge labels.  Each of these are stored in two places: a file,
 * and a TreeSet.  Things are added to the TreeSet when they are
 * created, and inserted in the file after a freeze().</s> TextGraph node and
 * edge caching for the <code>getOrderedEdgeLabels</code> and <code>
 * getOrderedIds</code> methods is handled by its <code>innerGraph</code>.  
 * TextGraph-specific
 * edges are appended at request time.</p>
 * 
 * <p>The methods <code>freeze()</code> and <code>melt()</code> are used to make 
 * sure that everything is added to the
 * index before it's ever accessed.  The first access forces a
 * 'freeze' operation, and after that no text files can be added
 * without throwing an error.</p>
 */
public class TextGraph implements MutableGraph, TextGraphExtensions
{
	private static Logger log = Logger.getLogger(TextGraph.class);

	public static final String FILE_TYPE = "FILE";
	public static final String TEXT_TYPE = "TEXT";
	public static final String LABELS_TYPE = "LABELS";
	public static final String TEXT_HANDLE_FIELD = "fileName";
	public static final String FLAVOR_FIELD = "flavor";
	public static final String CONTENTS_FIELD = "fileContents";
	// edge labels defined here should also be
	// cached out to disk in the 'freeze' routine
	public static final String HASTERM_EDGE_LABEL = "_hasTerm";
	public static final String INFILE_EDGE_LABEL = "_inFile";
	public static final String HASSPANTYPE_EDGE_LABEL = "_hasSpanType";
	public static final String HASSPAN_EDGE_LABEL = "_hasSpan";
	public static final String ANNOTATES_TEXT_EDGE_LABEL = "_annotates";
	public static final String TERM_TYPE = "TERM";

	private static final StringEncoder ENCODER = GraphLoader.ENCODER;
	/** Used to instantiate <code>innerGraph</code> if no 
	 * <code>ghirl.persistanceClass</code> property is specified. */
	private static final Class DEFAULT_PERSISTANTGRAPH = PersistantGraphSleepycat.class;

	boolean frozenFlag = false;
	/** Holds the path of a data file */
	private String indexFileName, dbFileName;
	/** Specified by Lucene. */
	private IndexWriter writer = null;
	/** Specified by Lucene. */
	private IndexReader index = null;
	/** Specified by Lucene. */
	private Searcher searcher = null;
	/** Used instead of indexFileName if writer is memory-resident */
	private RAMDirectory writerDirectory = null;

	// TextGraph maintains a list of all valid GraphId's and all valid
	// edge labels.  Each of these are stored in two places: a file,
	// and a TreeSet.  Things are added to the TreeSet when they are
	// created, and inserted in the file after a freeze().

//	private Set newIds = new HashSet();//, newLabels = new HashSet();
//	private String idFileName, labelFileName;

	//private Analyzer analyzer = new StandardAnalyzer();
	private Analyzer analyzer = new TextGraphLexer();

	private int maxHits = 50; // max number of documents returned in a search
	private int maxWords = 50; // max number of words used from a document
	// if true, weight graph so that sum of all edges of each link type are uniform
	private boolean equalWeightLinkTypes = true;

	// the graph that holds non-text links
	private MutableGraph innerGraph; 

	private TFIDFWeighter weighter = 
		new TFIDFWeighter(new DocFreqFunction() {
			public int docFreq(Term term) throws IOException { 
				return index.docFreq(term); }
			public int numDocs() throws IOException {
				return index.numDocs();
			}
		});

	/**
	 * Grabs the persistance classname from <code>ghirl.persistanceClass</code>.
	 * Checks that the specified class implements MutableGraph.
	 * Only applicable when using the persistant constructor TextGraph(name,mode).
	 * @return A Class object representing the class of the graph to be instantiated.
	 */
	private Class configurePersistantGraphClass() {
		String pgraphclassname = Config.getProperty("ghirl.persistanceClass");
		if (null != pgraphclassname) {
			try {
				Class temp = Class.forName(pgraphclassname);
				if (MutableGraph.class.isAssignableFrom(temp))
					return temp;
				else
					log.error("\""+pgraphclassname+"\" doesn't implement MutableGraph and is an invalid selection for ghirl.persistanceClass. Using default.");
			} catch (ClassNotFoundException e1) {
				log.error("Couldn't get specified ghirl.persistanceClass \""+pgraphclassname+"\"; using default");
				return DEFAULT_PERSISTANTGRAPH;
			}
		} else log.info("No ghirl.persistanceClass specified.  Using default...");
		if (MutableGraph.class.isAssignableFrom(DEFAULT_PERSISTANTGRAPH))
			return DEFAULT_PERSISTANTGRAPH;
		log.error("Shoot the programmer.  The default PersistanceGraph class must implement MutableGraph.");
		throw new IllegalStateException("Can't make a PersistanceGraph");
	}
	
	/** Create a text graph.  Data wil be stored under the directory named by
	 * the property ghirl.dbDir (in the properties file ghirl.properties).
	 *
	 * @param mode 'w' for write, 'r' for read, 'a' for append.
	 */
	public TextGraph(String fileStem, char mode)
	{
		String baseDir = Config.getProperty("ghirl.dbDir");
		if (baseDir==null) throw new IllegalArgumentException("The property ghirl.dbDir must be defined!");
		
		Class pgraphclass = configurePersistantGraphClass(); 
		log.info("Configured "+pgraphclass.getCanonicalName());
		
		File baseDirFile = new File(baseDir);
		if (!baseDirFile.exists() || !baseDirFile.isDirectory()) {
			throw new IllegalArgumentException("The directory "+baseDirFile+" must exist");
		}

		indexFileName = baseDir + File.separatorChar + fileStem+"_lucene.index";
		dbFileName    = baseDir + File.separatorChar + fileStem+"_db";
//		idFileName    = baseDir + File.separatorChar + fileStem+"_nodeIds.txt.gz";
//		labelFileName = baseDir + File.separatorChar + fileStem+"_edgeLabels.txt.gz";

		// check that the mode is valid
		if ("war".indexOf((int) mode) < 0) {
			throw new IllegalArgumentException("mode must be 'r' 'a' or 'w'");
		}
		// open the db, optionally clearing beforehand
		if ('w'==mode) rm_r(new File(dbFileName));
		try {
			innerGraph = (MutableGraph) pgraphclass.getConstructor(String.class, char.class)
						.newInstance(dbFileName,mode);
		} catch (InvocationTargetException e) {
			log.error("Problem occurred inside PersistanceGraph constructor: ",e);
		} catch (Exception e) {
			log.error("Problem getting constructor for PersistanceGraph: ",e);
		}
		if (null == innerGraph) throw new IllegalStateException("Cannot proceed without PersistanceGraph.");

		if (mode=='w') {
			// clear the saved label and graphId files, and retouch
//			File idFile=new File(idFileName), labelFile=new File(labelFileName);

//			rm_r(idFile);    //idFile.createNewFile();
//			rm_r(labelFile); //labelFile.createNewFile();
			// clear and re-open the text index
			rm_r(new File(indexFileName));
			try {
				writer = new IndexWriter(indexFileName, analyzer, true);
			} catch (IOException ex) {
				throw new IllegalArgumentException("can't create lucene writer: "+ex);
			}
		} else if (mode=='a') {
			innerGraph.melt();
			try {
				writer = new IndexWriter(indexFileName, analyzer, false);
			} catch (IOException ex) {
				throw new IllegalArgumentException("can't create lucene writer: "+ex);
			}
		} else if (mode=='r') {
			if (!new File(indexFileName).exists()) {
				throw new IllegalArgumentException("lucene index doesn't exist");
			}
			freeze();
		}

	}

	/** Create a memory-resident text graph.
	 */
	public TextGraph()
	{
		indexFileName = dbFileName = /*idFileName = labelFileName =*/ null;
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
	public TextGraph(String graphFileName)
	{
		this(new String[]{graphFileName});
	}

	/** Create a memory-resident text graph and initialize it with data
	 * from files readable by GraphLoader
	 */
	public TextGraph(String[] graphFileNames)
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

	/** Set number of documents to look at when 'walking' from a term 
	 */
	public void setMaxHits(int n) { maxHits=n; }

	public int getMaxHits() { return maxHits; }

	/** Set number of documents to look at when 'walking' from a term 
	 */
	public void setMaxWords(int n) { maxWords=n; }

	public int getMaxWords() { return maxWords; }

	//
	// these are used to make sure that everything is added to the
	// index before it's ever accessed.  the first access forces a
	// 'freeze' operation, and after that no text files can be added
	// without throwing an error
	//

	public boolean isFrozen() { return frozenFlag; }

	private void checkFrozen() { if (!frozenFlag) freeze(); }

	public String getProperty(GraphId id,String prop) { return innerGraph.getProperty(id,prop); }

	public void setProperty(GraphId id,String prop,String val) { innerGraph.setProperty(id,prop,val);  }

	public String toString() 
	{ 
		if (indexFileName!=null) return "[TextGraph index:"+indexFileName+"]";
		else return "[TextGraph RAMDir:"+writerDirectory+"]";
	} 

	public void melt()    
	{
		if (!frozenFlag) return;
		innerGraph.melt();
		if (writer==null) {
			try {
				if (indexFileName!=null) {
					writer = new IndexWriter(indexFileName, analyzer, true);
				} else {
					log.info("opening memory-resident index for directory "+writerDirectory);
					writer = new IndexWriter(writerDirectory, analyzer, true);
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

//			if (idFileName==null || labelFileName==null) {
//				log.info("skipping caching of nodes and labels");
//				frozenFlag = true;
//				return;
//			}

			// cache out new list of nodes and labels
//			if (newIds.size()>0) { // || newLabels.size()>0) { 
//				// read out all the previously-stored ids, except the
//				// ones of the form TERM$glob
////				TreeSet idSet = new TreeSet();
////				File idFile = new File(idFileName);
////				if (idFile.exists()) {
////					List tmp = getListFromFile(idFile,true);
////					for (Iterator i=tmp.iterator(); i.hasNext(); ) {
////						GraphId id = (GraphId)i.next();
////						if (!TERM_TYPE.equals(id.getFlavor())) {
////							idSet.add( id );
////						}
////					}
////				}
//				// now add all TERM$xxx's, old or new
////				for (Iterator i=new TermNodeIterator(); i.hasNext(); ) {
////					GraphId id = (GraphId)i.next();
////					idSet.add( id );
////				}
////				// now add any new ids
////				idSet.addAll(newIds);
//
//				// read all previously-stored labels
////				Set labelSet = new TreeSet();
////				File labelFile = new File(labelFileName);
////				if (labelFile.exists()) {
////					List tmp = getListFromFile(labelFile,false);
////					labelSet.addAll( tmp );
////				}
////				// add the special ones for text graphs
////				addTextgraphLabels(labelSet);
////				// add the new edge labels
////				//System.out.println("adding "+newLabels+"to: "+labelSet);
////				labelSet.addAll( newLabels );
//
//				// write out the new sets
////				PrintStream out = new PrintStream(new GZIPOutputStream(new FileOutputStream(new File(idFileName))));
////				for (Iterator i = idSet.iterator(); i.hasNext(); ) {
////					GraphId id = (GraphId) i.next();
////					out.println( id.toString() );
////				}
////				out.close();
////				out = new PrintStream(new GZIPOutputStream(new FileOutputStream(new File(labelFileName))));
////				for (Iterator i = labelSet.iterator(); i.hasNext(); ) {
////					String s = (String)i.next();
////					out.println( s );
////				}
////				out.close();
//			}
			frozenFlag = true;
		} catch (IOException ex) {
//			ex.printStackTrace();
			throw new IllegalArgumentException("can't open file",ex);
		}
	}
	
	private void addTextgraphLabels(Set labelSet) {
		labelSet.add( INFILE_EDGE_LABEL );
		labelSet.add( HASTERM_EDGE_LABEL );
		labelSet.add( HASSPANTYPE_EDGE_LABEL );
		labelSet.add( HASSPAN_EDGE_LABEL );
		labelSet.add( ANNOTATES_TEXT_EDGE_LABEL );
	}
	

	// disallow creation of term nodes, and make sure file nodes are
	// indexed.
	public GraphId createNode(String flavor,String shortName)
	{
		return createNode(flavor,shortName,"");
	}

	// special cases for TEXT, FILE, and LABELS flavors

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
//			newIds.add( id );
			if (FILE_TYPE.equals(flavor)) {
				try {
					indexDocument(flavor,shortName,IOUtil.readFile(new File(shortName)));
				} catch (IOException ex) {
					//throw new IllegalArgumentException("can't read file "+shortName);
					log.error("can't read file "+shortName);
					indexDocument(flavor,shortName,"file error:"+ex.toString());
				}
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

	private GraphId createLabelsNode(String shortName,String textFileIdString)
	{
		//System.err.println("createLabelsNode '"+shortName+"' '"+textFileIdString+"'");

		// dec 2009 refactored use of labelFileName as a local variable to use 
		// the name from the method argument instead.  labelFileName is a
		// member variable and should not be used as a local name.
//		String labelFileName = shortName;

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
		try {
			TextBase textBase = textBaseFor( new File(textFileId.getShortName()) );
			textLabels = new TextLabelsLoader().loadOps( textBase, new File(shortName) );
		} catch (IOException ex) {
			throw new IllegalArgumentException("can't read file "+shortName);
		}
		return createLabelsNode(textLabels,shortName,textFileId);
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
//		newIds.add( labelFileId );

		//System.err.println("labels node "+labelFileId+" "+textFileId+": adding edges");
		addEdge(ANNOTATES_TEXT_EDGE_LABEL, labelFileId, textFileId );
		addEdge(ANNOTATES_TEXT_EDGE_LABEL+"Inverse", textFileId, labelFileId );

		// now, create text nodes for each span in this file and link them
		// to the textFileId
		for (String type : textLabels.getTypes()) {
			String typeIdShortName = docName+"//"+type;
			String typeEdgeLabel = "has"+type.substring(0,1).toUpperCase()+type.substring(1);
			GraphId typeId = innerGraph.createNode( GraphId.DEFAULT_FLAVOR, typeIdShortName );
//			newIds.add( typeId );
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
		//GraphId innerId = innerGraph.createNode("",labelIdShortName);
		//newIds.add( innerId );
		//return innerId;
		return labelFileId;
	}

	public GraphId getNodeId(String flavor,String shortName)
	{
		GraphId id = new GraphId(flavor,shortName);
		if (contains(id)) return id;
		else return null;
	}

	// includes special check for term nodes, which are only implicitly there
	public boolean contains(GraphId id) 
	{ 	
		String flavor = id.getFlavor();
		if (TERM_TYPE.equals(flavor)) {
			try {
				Term t = new Term(CONTENTS_FIELD,id.getShortName());
				return index.docFreq(t) > 0;
			} catch (IOException ex) {
				throw new IllegalStateException("index error: "+ex);
			}
		} else {
			return innerGraph.contains(id);
		}
	}

	// includes special machinery to iterate over the implicit term nodes
	public Iterator getNodeIterator()
	{
		checkFrozen();
		return new UnionIterator( innerGraph.getNodeIterator(), new TermNodeIterator() );
	}

	public void addEdge(String linkLabel,GraphId from,GraphId to)
	{
		if (isFrozen()) {
			throw new IllegalStateException("adding edge "+linkLabel+" "+from+" -> "+to+" to frozen graph "+this);
		}
//		newLabels.add( linkLabel );
		innerGraph.addEdge(linkLabel,from,to);
	}

	public Set getEdgeLabels(GraphId from)
	{
		checkFrozen();
		String flavor = from.getFlavor();
		if (TERM_TYPE.equals(flavor)) return Collections.singleton( INFILE_EDGE_LABEL );
		else {
			Set accum = new HashSet();
			accum.addAll(innerGraph.getEdgeLabels(from));
			if (FILE_TYPE.equals(flavor) || TEXT_TYPE.equals(flavor))
				accum.add( HASTERM_EDGE_LABEL );
			return accum;
		}
	}

	public Set followLink(GraphId from,String linkLabel)
	{
		checkFrozen();
		String flavor = from.getFlavor();
		if (TERM_TYPE.equals(flavor) && INFILE_EDGE_LABEL.equals(linkLabel)) {
			return toSet(retrieveFiles(from.getShortName(),maxHits));
		} else {
			Set accum = new HashSet();
			accum.addAll( innerGraph.followLink(from,linkLabel) );
			if ((FILE_TYPE.equals(flavor)||TEXT_TYPE.equals(flavor)) && HASTERM_EDGE_LABEL.equals(linkLabel)) {
				accum.addAll( toSet(extractTerms(from.getShortName())) );
				//System.out.println("from :"+from+" via: "+linkLabel+" => "+accum);
			}
			return accum;
		}
	}

	public Distribution walk1(GraphId from,String linkLabel)
	{
		checkFrozen();
		String flavor = from.getFlavor();
		if (TERM_TYPE.equals(flavor) && INFILE_EDGE_LABEL.equals(linkLabel)) {
			return retrieveFiles(from.getShortName());
		} else if ((FILE_TYPE.equals(flavor) || TEXT_TYPE.equals(flavor)) 
				&& HASTERM_EDGE_LABEL.equals(linkLabel)) {
			return extractTerms(from.getShortName());
		} else return innerGraph.walk1(from,linkLabel);
	}

	public Distribution walk1(GraphId from)
	{
		checkFrozen();
		if (equalWeightLinkTypes) {
			TreeDistribution accum = new TreeDistribution();
			for (Iterator i=innerGraph.getEdgeLabels(from).iterator(); i.hasNext(); ) {
				String linkLabel = (String)i.next();
				accum.addAll(1.0, innerGraph.walk1(from,linkLabel));
			}
			String flavor = from.getFlavor();
			if (TERM_TYPE.equals(flavor)) {
				accum.addAll(1.0, retrieveFiles(from.getShortName(), maxHits));
			} else if (FILE_TYPE.equals(flavor) || TEXT_TYPE.equals(flavor)) {
				accum.addAll(1.0, extractTerms(from.getShortName()));
			}
			return accum;
		} else {
			String flavor = from.getFlavor();
			if (TERM_TYPE.equals(flavor)) {
				return retrieveFiles(from.getShortName(), maxHits);
			} else {
				Distribution accum = innerGraph.walk1(from);
				if (FILE_TYPE.equals(flavor) || TEXT_TYPE.equals(flavor)) {
					accum.addAll( 1.0, extractTerms(from.getShortName()) );
					//System.out.println("from :"+from+" => "+accum);
				}
				return accum;
			}
		}
	}

	public Distribution asQueryDistribution(String queryString)
	{
		checkFrozen();
		Distribution d = innerGraph.asQueryDistribution(queryString);
		if (d!=null) return d;
		else {
			GraphId id = GraphId.fromString(queryString);
			if (this.contains(id)) {
				return new TreeDistribution(id);
			} else {
				return textQuery(queryString);
			}
		}
	}

	//
	// converts a distribution to a set
	//

	private static Set toSet(Distribution d) 
	{
		Set accum = new TreeSet();
		for (Iterator i=d.iterator(); i.hasNext(); ) {
			accum.add(i.next());
		}
		return accum;
	}

	// these results are cached on disk when the graph is frozen,
	// since pre-computing them using the inner PersistantGraph can be
	// incredibly slow.

	public GraphId[] getOrderedIds()
	{
		checkFrozen();
//		try {
//			if (idFileName != null) {
//				File idFile = new File(idFileName);
//				if (idFile.exists()) {
//					List idList = getListFromFile(idFile,true);
//					return (GraphId[]) idList.toArray(new GraphId[idList.size()]);
//				} else return new GraphId[0];
//			} else {
//				log.warn("No id file -- restricting IDs to inner graph!");
				return innerGraph.getOrderedIds();
//			}
//		} catch (IOException ex) {
//			throw new IllegalStateException("error "+ex);
//		}
	}

	public String[] getOrderedEdgeLabels()
	{
		checkFrozen();
//		try {
//			if (labelFileName != null) {
//				File labelFile = new File(labelFileName);
//				if (labelFile.exists()) {
//					List labelList = getListFromFile(labelFile,false);
//					return (String[]) labelList.toArray(new String[labelList.size()]);
//				} else return new String[0];
//			} else {
//				log.warn("No label file -- restricting labels to inner graph!");
				Set labels = new HashSet();
				for(String label: innerGraph.getOrderedEdgeLabels()) {
					labels.add(label);
				}
				addTextgraphLabels(labels);
				return (String[]) labels.toArray(new String[labels.size()]); 
//				String[] innerLabels = innerGraph.getOrderedEdgeLabels();
//				Set textlabelsSet = new HashSet(); addTextgraphLabels(textlabelsSet);
//				String[] allLabels = new String[innerLabels.length+textlabelsSet.size()];
//				int i=0;
//				for(; i<innerLabels.length; i++) allLabels[i]=innerLabels[i];
//				for(String label : (String[]) textlabelsSet.toArray(new String[textlabelsSet.size()])) {
//					allLabels[i++] = label;
//				}
				
//			}
//		} catch (IOException ex) {
//			throw new IllegalStateException("error "+ex);
//		} 
	}

	private List getListFromFile(File fileName,boolean convertToGraphId) throws IOException
	{
		LineNumberReader in = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(fileName))));
		List list = new ArrayList();
		String line = null;
		while ((line=in.readLine())!=null) {
			if (convertToGraphId) list.add(GraphId.fromString(line));
			else list.add( line );
		}
		return list;
	}


	public String getTextContent(String idAsString)
	{
		return getTextContent(GraphId.fromString(idAsString));
	}

	public String getTextContent(GraphId id)
	{
		if (TEXT_TYPE.equals(id.getFlavor()) || FILE_TYPE.equals(id.getFlavor())) {
			try {
				String fileName = id.getShortName();
				Term t = new Term(TEXT_HANDLE_FIELD,fileName);
				TermDocs termDocs = index.termDocs(t);
				if (!termDocs.next()) {
					throw new IllegalStateException("can't retrieve text: no documents for term "+t.text()); 
				}
				//return null; //TODO: illegal response -- throw illegalstateexception or something
				Document doc = index.document( termDocs.doc() );
				Field contentField = doc.getField(CONTENTS_FIELD);
				String tmp = contentField.stringValue();
				return tmp;
			} catch (IOException ex) {
				throw new IllegalStateException("can't retrieve text: "+ex);
			}
		} else if (TERM_TYPE.equals(id.getFlavor())) {
			return "\""+id.getShortName()+"\"";
		} else {
			return innerGraph.getTextContent(id);
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// special operation for text graphs only!
	//////////////////////////////////////////////////////////////////////////////

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
	 * Retrieve files using the underlying IR engine.
	 */
	public Distribution textQuery(String queryString)
	{
		return retrieveFiles(queryString);
	}

	//////////////////////////////////////////////////////////////////////////////
	// special lucene-specific stuff
	//////////////////////////////////////////////////////////////////////////////

	protected interface DocFreqFunction
	{
		/** The document frequency of the term in this collection. **/
		public int docFreq(Term term) throws IOException;
		/** The number of documents in a collection **/
		public int numDocs() throws IOException;
	}

	/** Let g2 behave as if its DF's include all the documents in g1 as
	 * well as g2. */
	static public void mergeDocFrequencies(final TextGraph g1,final TextGraph g2)
	{
		DocFreqFunction mergedDF = new DocFreqFunction() {
			public int docFreq(Term term) throws IOException { 
				return g1.index.docFreq(term) + g2.index.docFreq(term);
			}
			public int numDocs() throws IOException {
				return g1.index.numDocs() + g2.index.numDocs();
			}
		};
		TFIDFWeighter mergedWeighter = new TFIDFWeighter(mergedDF);
		g2.weighter = mergedWeighter;
	}

	//
	// create and index a document (if the flavor is 'FILE_TYPE') or
	// a literal string that has been encoded (if the flavor is 'TEXT_TYPE') 
	// 
	private void indexDocument(String flavor,String shortName,String content)
	{
		try {
			writer.addDocument( textDocument(flavor,shortName,content) );
		} catch (IOException ex) {
			throw new IllegalStateException("index error: "+ex);
		}
	}

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

	private Set warnedAbout = new HashSet(); // tracks 'no termFreqVector' warnings
	private static final int MAX_NO_TERMFREQVEC_WARNINGS = 5;

	// extract the terms from a file
	private Distribution extractTerms(String fileName)
	{
		try {
			Term t = new Term(TEXT_HANDLE_FIELD,fileName);
			TermDocs termDocs = index.termDocs(t);
			if (!termDocs.next()) return TreeDistribution.EMPTY_DISTRIBUTION;
			int docNum = termDocs.doc();
			TermFreqVector termFreqVector = index.getTermFreqVector(docNum,CONTENTS_FIELD);
			if (termFreqVector==null) {
				if (warnedAbout.size()<MAX_NO_TERMFREQVEC_WARNINGS && warnedAbout.add(fileName)) { 
					log.warn("no termFreqVector for "+fileName);
					if (warnedAbout.size()==MAX_NO_TERMFREQVEC_WARNINGS) {
						log.warn("I don't want to nag, so that's your last warning of this type");
					}
				}
				return new TreeDistribution();
			} else {
				return weighter.heaviestTerms( maxWords, termFreqVector );
			}
		} catch (IOException ex) {
			throw new IllegalStateException("error computing TFIDF: "+ex);
		}
	}

	private static class WeightedId implements Comparable {
		public double w;
		public GraphId id;
		public WeightedId(double w,GraphId id) { this.w=w; this.id=id; }
		public int compareTo(Object b) {
			double w2 = ((WeightedId)b).w;
			return (w2>w)?+1 : ((w2<w)?-1:0);
		}
	}

	private static class TFIDFWeighter
	{
		private double w[] = new double[1000];
		private int numWeights = 0;
		private DocFreqFunction df;

		public TFIDFWeighter(DocFreqFunction df) { this.df = df; }

		private void computeWeights( TermFreqVector termFreqVector ) throws IOException
		{
			int[] freqs = termFreqVector.getTermFrequencies();
			numWeights = freqs.length; 
			// allocate space for weights
			if (w.length<numWeights) w = new double[numWeights];
			// compute normalized tfidf weights
			String[] terms = termFreqVector.getTerms();
			double norm = 0.0;
			for (int i=0; i<numWeights; i++) {	
				Term term = new Term(CONTENTS_FIELD,terms[i]);
				w[i] = Math.log(freqs[i]+1.0) * ( Math.log(df.numDocs()+1.0) - Math.log(df.docFreq(term)) );
				norm += w[i]*w[i];
			}
			norm = Math.sqrt(norm);
			for (int i=0; i<numWeights; i++) {
				w[i] /= norm;
			}
		}
		public Distribution heaviestTerms( int k, TermFreqVector termFreqVector ) throws IOException
		{
			computeWeights( termFreqVector );
			WeightedId[] wids = new WeightedId[numWeights];
			String[] terms = termFreqVector.getTerms();
			for (int i=0; i<numWeights; i++) { 
				wids[i] = new WeightedId( w[i], new GraphId(TERM_TYPE,terms[i]) ); 
			}
			Arrays.sort(wids);
			Distribution dist = new TreeDistribution();
			for (int i=0; i<Math.min(k,numWeights); i++) {
				dist.add( wids[i].w, wids[i].id );
			}
			return dist;
		}
	} 

	// return a distribution of files containing this term
	private Distribution retrieveFiles(String termText)
	{
		return retrieveFiles(termText,maxHits);
	}

	private Distribution retrieveFiles(String termText, int maxNum)
	{
		try {
			checkFrozen();
			Query query = QueryParser.parse(termText, CONTENTS_FIELD, analyzer);
			Hits hits = searcher.search(query);
			//System.out.println(hits.length()+" hits on "+termText);
			Distribution dist = new TreeDistribution();
			for (int i=0; i < Math.min(hits.length(), maxNum); i++) {
				Document doc = hits.doc(i);
				double score = hits.score(i);
				GraphId id = getIdFromDocument(doc);
				dist.add( score, id );
			}
			return dist;
		} catch (ParseException ex) {
			throw new IllegalStateException("error parsing query '"+termText+"': "+ex);
		} catch (IOException ex) {
			throw new IllegalStateException("error: "+ex);
		} 
	}

	public static GraphId getIdFromDocument(Document doc) 
	{ 
		Field nameField = doc.getField( TEXT_HANDLE_FIELD );
		Field flavorField = doc.getField( FLAVOR_FIELD );
		return new GraphId( flavorField.stringValue(), nameField.stringValue() );
	}

	// iterate over terms 
	private class TermNodeIterator implements Iterator
	{
		private TermEnum te;
		private Term term;
		private boolean hadNext;
		public TermNodeIterator() 
		{ 
			try {
				te = index.terms(); 
			} catch (IOException ex) {
				throw new IllegalStateException("error: "+ex);
			}
			advance(); 
		}
		public Object next() 
		{ 
			Object id = new GraphId( TERM_TYPE, term.text() );
			advance();  
			return id; 
		}
		public void remove() 
		{ 
			throw new UnsupportedOperationException("can't remove"); 
		}
		public boolean hasNext() 
		{ 
			return hadNext; 
		}
		private void advance() 
		{ 
			try {
				hadNext = te.next(); 
				if (hadNext) term = te.term(); 
				else te.close();
			} catch (IOException ex) {
				throw new IllegalStateException("error: "+ex);
			}
		}
	}

	private class TextGraphLexer extends Analyzer
	{
		public final TokenStream tokenStream(String fieldName,Reader reader) 
		{
			return new StopFilter(new LowerCaseFilter(new StandardFilter(new AlphanumericTokenizer(reader))),
					StopAnalyzer.ENGLISH_STOP_WORDS);
		}
	}
	private class AlphanumericTokenizer extends CharTokenizer
	{
		public AlphanumericTokenizer(Reader reader) { super(reader); }
		public boolean isTokenChar(char c) { return Character.isLetterOrDigit(c); }
	};


	//
	// utilities
	//

	private static void rm_r(File f)
	{
		if (f.isDirectory()) {
			File[] g = f.listFiles();
			for (int i=0; i<g.length; i++) rm_r(g[i]);
		}
		boolean deleted = f.delete();
		log.info("deleting: "+f+" "+(deleted?"-done":"-not present!"));
	}

	/** return a set of labels suitable for annotating the text in the given file.
	 */
	static public MutableTextLabels emptyLabelsFor(File f) throws IOException
	{
		TextBase base = textBaseFor(f);
		return new BasicTextLabels(base);
	}
	static private TextBase textBaseFor(File f) throws IOException
	{
		String contents = IOUtil.readFile(f);
		BasicTextBase textBase = new BasicTextBase();
		textBase.loadDocument( "someFile", contents );
		return textBase;
	}


	static public Set limitToN(int maxNum,Set set)
	{
		if (set.size()<=maxNum) return set;
		Set accum = new HashSet();
		Iterator i=set.iterator(); 
		for (int n=0; n<maxNum; n++) {
			accum.add (i.next());
		}
		return accum;
	}

	//
	// convenience method - summarize the graph, optionally display some nodes
	//

	static public void main(String[] args)
	{
		//TextGraph g = new TextGraph(args[0],'r') ;
		TextGraph g = new TextGraph(args[0]);
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
