package ghirl.graph;

import java.util.*;
import java.io.*;
import ghirl.util.*;
import com.wcohen.ss.*;
import com.wcohen.ss.lookup.*;
import com.wcohen.ss.tokens.*;
import com.wcohen.ss.api.*;

/** Computes a new edge in a graph based on a "soft dictionary".
 */

public class SoftDictSearcher extends AbstractGraphSearcher
{
    private static double DEFAULT_MINSCORE = 0.80;

    private SoftTFIDFDictionary softDict;
    private double minScore;

    public SoftDictSearcher(String fileName) throws IOException
    {
        this(fileName,DEFAULT_MINSCORE);
    }

    public SoftDictSearcher(String fileName,double minScore) throws IOException
    {
        this(SoftTFIDFDictionary.restore(new File(fileName)),minScore);
    }

    public SoftDictSearcher(SoftTFIDFDictionary softDict)
    {
        this(softDict,DEFAULT_MINSCORE);
    }

    public SoftDictSearcher(SoftTFIDFDictionary softDict,double minScore)
    {
        this.softDict = softDict;
        this.minScore = minScore;
    }

    public Distribution search(Distribution queryDistribution)
    {
	Distribution result = new TreeDistribution();
	for (Iterator i=queryDistribution.iterator(); i.hasNext(); ) {
	    GraphId qId = (GraphId)i.next();
            double probOfQId = queryDistribution.getLastProbability();
	    String content = graph.getTextContent(qId);
            int n = softDict.lookup(minScore, content);
            //System.out.println("search on '"+content+"' with score "+probOfQId+" gives "+n+" matches"); 
            for (int j=0; j<n; j++) {
                String simString = (String)softDict.getValue(j);
                double score = softDict.getScore(j);
                GraphId simId = GraphId.fromString( simString );
                if (graph.contains(simId)) {
                    //System.out.println("adding to result "+simId+" sim="+score+" total score="+probOfQId*score);
                    result.add( probOfQId*score, simId );
                } else {
                    System.out.println("Warning: unknown graph id '"+simString+"'");
                }
            }
	}
	return result;
    }
}
