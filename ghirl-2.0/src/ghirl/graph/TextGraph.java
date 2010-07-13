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
import org.apache.log4j.*;


/** Read-only version of {@link:ghirl.graph.MutableTextGraph}. */
public class TextGraph implements Graph, TextGraphExtensions, Closable
{
	private static Logger log = Logger.getLogger(TextGraph.class);
	
	/** Property name for setting the persistance class in ghirl.properties */
	public static final String CONFIG_PERSISTANCE_PROPERTY="ghirl.persistanceClass";
	
	/** Special node flavor for TextGraphs */
	public static final String FILE_TYPE = "FILE",
							   TEXT_TYPE = "TEXT",
							   LABELS_TYPE = "LABELS",
							   TERM_TYPE = "TERM";
	/** Special field for TextGraphs. */
	public static final String TEXT_HANDLE_FIELD = "fileName",
							   FLAVOR_FIELD = "flavor",
							   CONTENTS_FIELD = "fileContents";
	/** Special edge label for TextGraphs. */
	public static final String HASTERM_EDGE_LABEL = "_hasTerm",
							   INFILE_EDGE_LABEL = "_inFile",
							   HASSPANTYPE_EDGE_LABEL = "_hasSpanType",
							   HASSPAN_EDGE_LABEL = "_hasSpan",
							   ANNOTATES_TEXT_EDGE_LABEL = "_annotates";
	public static final int    N_TEXTGRAPH_LABELS = 5;

	private static final StringEncoder ENCODER = GraphLoader.ENCODER;
	/** Used to instantiate <code>innerGraph</code> if no 
	 * <code>ghirl.persistanceClass</code> property is specified. */
	private static final Class DEFAULT_PERSISTANTGRAPH = PersistantGraphSleepycat.class;

	boolean frozenFlag = false;
	/** Holds the path of a data file */
	protected String indexFileName, dbFileName;
	/** Specified by Lucene. */
	protected IndexReader index = null;
	/** Specified by Lucene. */
	protected Searcher searcher = null;

	//private Analyzer analyzer = new StandardAnalyzer();
	protected Analyzer analyzer = new TextGraphLexer();

	private int maxHits = 50; // max number of documents returned in a search
	private int maxWords = 50; // max number of words used from a document
	// if true, weight graph so that sum of all edges of each link type are uniform
	private boolean equalWeightLinkTypes = true;

	/** the graph that holds non-text links */
	protected Graph innerGraph; 
	
	protected String basedir;
	protected File basedirFile;

	private TFIDFWeighter weighter = 
		new TFIDFWeighter(new DocFreqFunction() {
			public int docFreq(Term term) throws IOException { 
				return index.docFreq(term); }
			public int numDocs() throws IOException {
				return index.numDocs();
			}
		});

	private int nTermNodes;

	/**
	 * Grabs the persistance classname from <code>ghirl.persistanceClass</code>.
	 * Checks that the specified class implements MutableGraph.
	 * Only applicable when using the persistant constructor TextGraph(name,mode).
	 * @return A Class object representing the class of the graph to be instantiated.
	 */
	protected static Class configurePersistantGraphClass(Class graphType ) {
		String pgraphclassname = Config.getProperty(CONFIG_PERSISTANCE_PROPERTY);
		if (null != pgraphclassname) {
			try {
				Class temp = Class.forName(pgraphclassname);
				if (graphType.isAssignableFrom(temp))
					return temp;
				else
					log.error("\""+pgraphclassname+"\" doesn't implement MutableGraph and is an invalid selection for "+CONFIG_PERSISTANCE_PROPERTY+". Using default.");
			} catch (ClassNotFoundException e1) {
				log.error("Couldn't get specified "+CONFIG_PERSISTANCE_PROPERTY+" \""+pgraphclassname+"\"; using default");
				return DEFAULT_PERSISTANTGRAPH;
			}
		} else log.info("No ghirl.persistanceClass specified.  Using default...");
		if (MutableGraph.class.isAssignableFrom(DEFAULT_PERSISTANTGRAPH))
			return DEFAULT_PERSISTANTGRAPH;
		log.error("Shoot the programmer.  The default PersistanceGraph class must implement MutableGraph.");
		throw new IllegalStateException("Can't make a PersistanceGraph");
	}
	
	/** Create a text graph.  Data must be located under the directory named by
	 * the property ghirl.dbDir (in the properties file ghirl.properties).
	 *
	 * @param fileStem Name of the graph (minus "_db" or "_lucene.index").
	 * @throws IOException if the named graph can't be found or can't be opened.
	 */
	public TextGraph(String fileStem) throws IOException { this(fileStem, null); }
	
	public TextGraph(String fileStem, Graph graph) throws IOException {
		this.innerGraph = graph;
		setupLuceneSettings(fileStem);
		if (this.innerGraph == null && !new File(inBaseDir(dbFileName)).exists())
			throw new FileNotFoundException("Database "+dbFileName+" must exist.");
		setupInnerGraph('r', Graph.class);
		setupReadonlyIndex();
		
		if (getInnerGraph() instanceof MutableGraph) ((MutableGraph)getInnerGraph()).freeze();
		
		try {
			index = IndexReader.open(indexFileName);
		} catch (IOException e) {
			throw new IOException("Couldn't open lucene index "+indexFileName,e);
		} 

		searcher = new IndexSearcher(index);
	}
	
	protected void setupLuceneSettings(String fileStem) throws IOException {
		basedir = Config.getProperty(Config.DBDIR);
		if (basedir == null) throw new IllegalArgumentException("The property ghirl.dbDir must be defined!");

		basedirFile = new File(basedir);
		if (!basedirFile.exists() || !basedirFile.isDirectory()) {
			throw new IOException("The directory "+basedirFile+" must exist");
		}

		indexFileName = inBaseDir(fileStem+"_lucene.index");
		// Inner databases are polymorphic and automatically place themselves
		// in the baseDir
		dbFileName    = fileStem+"_db";
	}
	protected void setupInnerGraph(char mode, Class graphType) {
		if (this.getInnerGraph() != null) return;
		try {
			Class pgraphclass = configurePersistantGraphClass(graphType); 
			log.info("Configured "+pgraphclass.getCanonicalName());
			this.setInnerGraph((Graph) pgraphclass.getConstructor(String.class, char.class)
						.newInstance(dbFileName,mode));
		} catch (InvocationTargetException e) {
			log.error("Problem occurred inside innerGraph constructor: ",e);
		} catch (Exception e) {
			log.error("Problem getting constructor for innerGraph: ",e);
		}
		if (null == getInnerGraph()) throw new IllegalStateException("Cannot proceed without innerGraph.");
	}
	protected void setupReadonlyIndex() throws FileNotFoundException {
		if (!new File(indexFileName).exists()) {
			throw new FileNotFoundException("Lucene index doesn't exist at "+indexFileName);
		}
	}
	
	protected void setInnerGraph(Graph g) { this.innerGraph = g; }
	protected Graph getInnerGraph() { return this.innerGraph; }

	protected TextGraph() {}


	protected String inBaseDir(String filename) {
		return basedir
		       +File.separatorChar
		       +filename;
	}
	
	public void close() { 
		Graph g = getInnerGraph();
		if (g instanceof PersistantGraph) {
			((PersistantGraph) g).close();
		}
		try {
			if(index != null) {
				index.close();
				searcher.close();
			}
		} catch (IOException e) {
			log.error("Trouble closing Lucene index for "+this.toString(),e);
		}
	}

	/** Set number of documents to look at when 'walking' from a term 
	 */
	public void setMaxHits(int n) { maxHits=n; }

	public int getMaxHits() { return maxHits; }

	/** Set number of documents to look at when 'walking' from a term 
	 */
	public void setMaxWords(int n) { maxWords=n; }

	public int getMaxWords() { return maxWords; }

	public boolean isFrozen() { return true; }

	protected void checkFrozen() { }

	public String getProperty(GraphId id,String prop) { return getInnerGraph().getProperty(id,prop); }

	public String toString() 
	{ 
		return "[TextGraph index:"+indexFileName+"]";
	} 
	
	private void addTextgraphLabels(Collection labelcoll) {
		labelcoll.add( INFILE_EDGE_LABEL );
		labelcoll.add( HASTERM_EDGE_LABEL );
		labelcoll.add( HASSPANTYPE_EDGE_LABEL );
		labelcoll.add( HASSPAN_EDGE_LABEL );
		labelcoll.add( ANNOTATES_TEXT_EDGE_LABEL );
	}
	
	// special cases for TEXT, FILE, and LABELS flavors

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
				// here we check for TERM nodes both in the contents and 
				// file handle fields, since those fields are the sources
				// of TERM nodes that come out of getNodeIterator()
				Term t = new Term(CONTENTS_FIELD,id.getShortName());
				Term ti = new Term(TEXT_HANDLE_FIELD,id.getShortName());
				return (index.docFreq(t) > 0) || (index.docFreq(ti) > 0);
			} catch (IOException ex) {
				throw new IllegalStateException("index error: "+ex);
			}
		} else {
			return getInnerGraph().contains(id);
		}
	}

	// includes special machinery to iterate over the implicit term nodes
	public Iterator getNodeIterator()
	{
		checkFrozen();
		return new UnionIterator( getInnerGraph().getNodeIterator(), new TermNodeIterator() );
	}

	public Set getEdgeLabels(GraphId from)
	{
		checkFrozen();
		String flavor = from.getFlavor();
		if (TERM_TYPE.equals(flavor)) return Collections.singleton( INFILE_EDGE_LABEL );
		else {
			Set accum = new HashSet();
			accum.addAll(getInnerGraph().getEdgeLabels(from));
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
			accum.addAll( getInnerGraph().followLink(from,linkLabel) );
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
		} else return getInnerGraph().walk1(from,linkLabel);
	}

	public Distribution walk1(GraphId from)
	{
		checkFrozen();
		if (equalWeightLinkTypes) {
			TreeDistribution accum = new TreeDistribution();
			for (Iterator i=getInnerGraph().getEdgeLabels(from).iterator(); i.hasNext(); ) {
				String linkLabel = (String)i.next();
				accum.addAll(1.0, getInnerGraph().walk1(from,linkLabel));
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
				Distribution accum = getInnerGraph().walk1(from);
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
		Distribution d = getInnerGraph().asQueryDistribution(queryString);
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


	/** Converts a distribution to a set
	 * 
	 * @param d A Distribution of nodes
	 * @return A Set containing those nodes
	 */
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

	/** Because this traverses the term iterator each time it is called, this 
	 * method can be incredibly slow.
	 */
	public GraphId[] getOrderedIds()
	{
		checkFrozen();
		GraphId[] innerids = getInnerGraph().getOrderedIds();
		ArrayList<GraphId> ids   = new ArrayList<GraphId>(innerids.length + this.nTermNodes);
		// copy ids
		int i=0;
		for (; i<innerids.length; i++) ids.add(innerids[i]);
		TermNodeIterator it = new TermNodeIterator();
		for (GraphId node=null; it.hasNext();) {
			ids.add((GraphId) it.next());
			i++;
		}
		this.nTermNodes = Math.max(nTermNodes,i);
		// switch to array and sort
		GraphId[] allids = ids.toArray(new GraphId[0]);
		Arrays.sort(allids);
		return allids;
	}

	public String[] getOrderedEdgeLabels()
	{
		// what a nightmare:
		checkFrozen();
		String[] innerLabels     = getInnerGraph().getOrderedEdgeLabels();
		ArrayList<String> labels = new ArrayList<String>(innerLabels.length + N_TEXTGRAPH_LABELS);
		// copy labels
		for(String label: innerLabels) labels.add(label);
		addTextgraphLabels(labels); 
		// switch to array and sort
		String[] allLabels       = labels.toArray(new String[0]);
		Arrays.sort(allLabels);
		return allLabels; 
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
			return getInnerGraph().getTextContent(id);
		}
	}

	//////////////////////////////////////////////////////////////////////////////
	// special operation for text graphs only!
	//////////////////////////////////////////////////////////////////////////////

	/** Not supported -- do not call! */
	public GraphId createLabelsNode(TextLabels textLabels,String labelIdShortName,String textFileIdName)
	{
		log.warn("TextGraph is immutable and does not support createLabelsNode(TextLabels,String,String)!");
		return null;
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
		private static final String FLAVORFIELD = "flavor";
		private TermEnum te;
		private Term term;
		private boolean hadNext;
		private boolean includeFlavorField=false;
		/** The default TermNodeIterator excludes flavor nodes such as TERM$TEXT.
		 * 
		 */
		public TermNodeIterator() 
		{ 
			try {
				te = index.terms(); 
			} catch (IOException ex) {
				throw new IllegalStateException("error: "+ex);
			}
			advance(); 
		}
		/** Use this constructor if you want the iterator to include flavor
		 * nodes like TERM$TEXT.
		 * 
		 * @param includeFlavorField (default false)
		 */
		public TermNodeIterator(boolean includeFlavorField) {
			this();
			this.includeFlavorField = includeFlavorField;
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
		public String field() { return term.field(); }
		private void advance() 
		{ 
			try {
				hadNext = te.next(); 
				if (hadNext) {
					term = te.term();
					if (!includeFlavorField && FLAVORFIELD.equals(term.field())) advance();
				} else te.close();
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


	/** return a set of labels suitable for annotating the text in the given file.
	 */
	static public MutableTextLabels emptyLabelsFor(File f) throws IOException
	{
		TextBase base = textBaseFor(f);
		return new BasicTextLabels(base);
	}
	static protected TextBase textBaseFor(File f) throws IOException
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

}
