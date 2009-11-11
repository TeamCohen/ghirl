package ghirl.graph;

import java.util.*;
import java.io.*;
import ghirl.util.*;
import edu.cmu.minorthird.text.*;
import edu.cmu.minorthird.util.*;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/** Uses a SecondString "soft dictionary" and a Minorthird entity
 * extractor as steps in fixed search in which extracted entities are
 * matched to a soft dictionary of synonyms for a normalized
 * identifier.  This is a baseline method for entity normalization
 * problems.
 */

public class SoftDictEntitySearcherV1 extends ProgrammableSearcher
{
    private String doc2Labels = TextGraph.ANNOTATES_TEXT_EDGE_LABEL+"Inverse";
    private String type2Instance = "isaInverse";
    private SoftDictionary softDict = new SoftDictionary(new SimpleTokenizer(true,true));
    private Map normalIdMap = new HashMap();
    private String spanType;

    /**
     * The soft dictionary is formed from a 'synonym list', which is a
     * set of normalized identifiers, each of which is mapped to
     * 'synonyms' for those identifiers, as defined by the parameters
     * graph, normalizedIdType, and normalizedId2Synonym.  Once it is
     * built, the the SoftDictEntitySearcherV1 searches on this path:
     *
     * FILE$someFile -doc2Labels-> LABEL$someMinorthirdLabels
     * -hasSpanType-> TEXT$someEntityName -softDictionary->
     * $normalizedIdOfBestMatchToEntity
     *
     * @param graph the graph to build the soft dictionary from
     * @param normalizedIdType the type associated the normalized
     * identifiers -- eg for each GraphId x for a normalized
     * identifier, there is an edge "x isa normalizedIdType".
     * @param normalizedIdType for each GraphId x that is a normalized
     * identifier, there are some edges "x normalizedId2Synonym yi"
     * where yi is a synonym for x.
     *
     */
    public SoftDictEntitySearcherV1(TextGraph graph,String spanType,String normalizedIdType,String normalizedId2Synonym)
    {
	this.spanType = spanType;
	String labels2Span = "has" + spanType.substring(0,1).toUpperCase() + spanType.substring(1);
	System.out.println("path="+doc2Labels+" "+labels2Span+" softTFIDF ");

	// build the dictionary
	buildDictionary(graph,normalizedIdType,normalizedId2Synonym);
	// create the pipeline of search steps
	setSteps( buildSteps(labels2Span) );
	//setTrace(true);
    }

    public SoftDictEntitySearcherV1(String spanType,String synonymListFileName)
    {
	this.spanType = spanType;
	String labels2Span = "has" + spanType.substring(0,1).toUpperCase() + spanType.substring(1);
	System.out.println("path="+doc2Labels+" "+labels2Span+" softTFIDF ");
	// build the dictionary
	try {
	    buildDictionary(synonymListFileName);
	} catch (IOException ex) {
	    throw new IllegalArgumentException("can't open "+synonymListFileName+": "+ex);
	}
	// create the pipeline of search steps
	setSteps( buildSteps(labels2Span) );
	//setTrace(true);
    }

    private ProgrammableSearcher.SearchStep[] buildSteps(String labels2Span)
    {
	return
	    new ProgrammableSearcher.SearchStep[]{
		new ProgrammableSearcher.LinkStep( doc2Labels ),
		new ProgrammableSearcher.LinkStep( labels2Span ),
		new SoftDictionaryStep( softDict, normalIdMap )
		//was: ,new ProgrammableSearcher.LinkStep( normalizedId2Synonym+"Inverse" )
	    };
    }

    private void buildDictionary(String synonymListFileName) throws IOException,FileNotFoundException
    {
	System.err.println("loading synonyms from "+synonymListFileName+"...");
	LineNumberReader in = new LineNumberReader(new FileReader(new File(synonymListFileName)));
	String line = null;
	ProgressCounter pc = new ProgressCounter("loading "+synonymListFileName,"lines");
	while ((line = in.readLine())!=null) {
	    //System.out.println("read: "+line);
	    String[] parts = line.split("\\t+");
	    String normalIdName = parts[0].replace(':','_');
	    for (int i=1; i<parts.length; i++) {
		softDict.put( parts[i], null );
		normalIdMap.put( parts[i], normalIdName );
	    }
	    pc.progress();
	}
	pc.finished();
	in.close();
    }

    private void buildDictionary(TextGraph graph,String normalizedIdType,String normalizedId2Synonym)
    {
	PathSearcher normalizedIdFinder = new PathSearcher(type2Instance);
	PathSearcher synonymFinder = new PathSearcher(normalizedId2Synonym);
	setGraph(graph);
	normalizedIdFinder.setGraph(graph);
	synonymFinder.setGraph(graph);
	GraphId normalizedIdTypeId = GraphId.fromString(normalizedIdType);
	System.out.println("finding normalized ids...");
	Distribution normalizedIdDist = normalizedIdFinder.search(normalizedIdTypeId);
	System.out.println("building dictionary...");
	ProgressCounter pc = new ProgressCounter("building soft dictionary","graph node");
	for (Iterator i=normalizedIdDist.iterator(); i.hasNext(); ) {
	    GraphId normalizedId = (GraphId)i.next();
	    Distribution synonynmDist = synonymFinder.search(normalizedId);
	    for (Iterator j=synonynmDist.iterator(); j.hasNext(); ) {
		GraphId synonymId = (GraphId)j.next();
		String content = graph.getTextContent(synonymId);
		softDict.put( content, null );
		//was: normalIdMap.put( content, synonymId );
		normalIdMap.put( content, normalizedId );
	    }
	    pc.progress();
	}
	pc.finished();
	System.out.println("softDict has "+softDict.size()+" entries");
	
    }

    public class SoftDictionaryStep extends ProgrammableSearcher.SearchStep
    {
	private SoftDictionary softDict;
	private Map normalIdMap;
	/**
	 * @param the normalIdMap maps strings that are entries in the softDict
	 * to either GraphId's, or strings which are the shortName() of 
	 * a GraphId.
	 */
	public SoftDictionaryStep(SoftDictionary softDict, Map normalIdMap) 
	{ 
	    this.softDict=softDict; 
	    this.normalIdMap = normalIdMap;
	}
	public Distribution takeStep(GraphId id) 
	{
	    String content = graph.getTextContent(id);
	    StringWrapper w = (StringWrapper)softDict.lookup( content );
	    if (w==null) {
		//System.out.println("softDict: "+id+"="+content+" => NULL");
		return TreeDistribution.EMPTY_DISTRIBUTION;
	    }  else {
		Object normalIdHandle = normalIdMap.get(w.unwrap());
		//System.out.println("softDict: "+id+"="+content+" => "+ normalIdHandle);
		GraphId normalId = null;
		if (normalIdHandle instanceof GraphId) {
		    normalId = (GraphId)normalIdHandle;
		    //System.out.println("handle was a graphId = " + normalId);
		} else if (normalIdHandle instanceof String) {
		    normalId = graph.getNodeId( GraphId.DEFAULT_FLAVOR, (String)normalIdHandle );
		    //System.out.println("handle's graphid is "+normalId);
		} else {
		    throw new IllegalArgumentException("unexpected normalId type "+normalId.getClass());
		}
		if (normalId==null) return TreeDistribution.EMPTY_DISTRIBUTION;
		else return new TreeDistribution(normalId);
	    }
	}
    }

    public Set normalizedIdsFor(TextLabels labels)
    {
	Set result = new HashSet();
	for (Span.Looper i = labels.instanceIterator(spanType); i.hasNext(); ) {
	    Span span = i.nextSpan();
	    String entity = span.asString();
	    StringWrapper w = (StringWrapper)softDict.lookup( entity );
	    GraphId nid = (GraphId)normalIdMap.get(w.unwrap());
	    if (w!=null) result.add( nid.getShortName() );
	}
	return result;
    }


    public static void main(String[] args) throws IOException
    {
	if (args.length<5) {
	    System.out.println("usage: graphName spanType normalType normal2synLink textFile textLabelsFile");
	    System.out.println("example: lite-mouse likelyProtein geneId synonym foo.txt foo.labels");
	    System.out.println("         where the docIds in foo.labels are 'someFile'");
	    System.exit(0);
	}

	TextGraph g = new TextGraph(args[0],'r');
	SoftDictEntitySearcherV1 s = new SoftDictEntitySearcherV1(g,args[1],args[2],args[3]);
	String textFileContents = IOUtil.readFile(new File(args[4]));
	BasicTextBase textBase = new BasicTextBase();
	textBase.loadDocument( "someFile", textFileContents );
	TextLabels textLabels = new TextLabelsLoader().loadOps( textBase, new File(args[5]) );
	Set normalizedIds = s.normalizedIdsFor( textLabels );
	System.out.println("normalized: "+normalizedIds);
    }
}
