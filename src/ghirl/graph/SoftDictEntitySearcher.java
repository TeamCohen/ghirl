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

public class SoftDictEntitySearcher extends ProgrammableSearcher
{
    private final static double DEFAULT_MINSCORE = 0.5;
    private final static String DOC2LABELS = TextGraph.ANNOTATES_TEXT_EDGE_LABEL+"Inverse";

    public SoftDictEntitySearcher(String spanType,String softDictFileName) throws IOException
    {
        this(spanType,softDictFileName,DEFAULT_MINSCORE);
    }

    public SoftDictEntitySearcher(String spanType,String softDictFileName,double minScore) throws IOException
    {
	String labels2Span = "has" + spanType.substring(0,1).toUpperCase() + spanType.substring(1);
	setSteps(new ProgrammableSearcher.SearchStep[]{
            new ProgrammableSearcher.LinkStep( DOC2LABELS ),
            new ProgrammableSearcher.LinkStep( labels2Span ),
            new ProgrammableSearcher.GraphSearcherStep( new SoftDictSearcher(softDictFileName, minScore) ),
        });
    }
}
