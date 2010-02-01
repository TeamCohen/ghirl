package ghirl.learn;

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import edu.cmu.minorthird.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.classify.*;
import edu.cmu.minorthird.classify.experiments.*;
import ghirl.graph.*;
import ghirl.util.*;

import javax.swing.*;
import java.awt.*;


/** Evaluate a GraphSearcher on a GraphSearchDataset
 */

public class GraphSearchEval implements Visible, Saveable
{
    private final static int GRAPHS_PER_PAGE = 10;
    private final static int NUM_TOP_TO_SHOW = 50;
    private TreeMap rankedListMap = new TreeMap();
    private TreeMap positiveIdMap = new TreeMap();
    private TreeMap initialNodesMap = new TreeMap();
    private Graph graph = null;
    private boolean guiFlag = false;
    private String loadedFile = null;

    public void setGraph(Graph graph) { this.graph = graph; }

    public void extend(Graph graph,GraphSearcher searcher,GraphSearchDataset dataset)
    {
	ProgressCounter pc = new ProgressCounter("running searcher", "example", dataset.size());
	for (GraphSearchExample.Looper i=dataset.iterator(); i.hasNext(); ) {
	    GraphSearchExample example = i.nextExample();
	    extend(graph, searcher, example);
	    pc.progress();
	}
	pc.finished();
    }

    public void extend(Graph graph,GraphSearcher searcher,GraphSearchExample example)
    {
        Distribution rankedList = example.doQuery(searcher,graph);
        Set posIds = example.getPositiveIds();
        Set knownIds = example.getKnownIds();
        Distribution ranking = rankedList.copy();
        for (Iterator it=knownIds.iterator(); it.hasNext();)
            ranking.remove(it.next());
        Distribution initialIds = CommandLineUtil.parseNodeOrNodeSet(example.getQueryString(),graph);
        rankedListMap.put( example.getSubpopulationId(), ranking );
        positiveIdMap.put( example.getSubpopulationId(), posIds );
        initialNodesMap.put( example.getSubpopulationId(), initialIds);
    }

    /** Low-level access */

    public void extend(String exampleId, Distribution rankedList, Set posIds, Set knownIds, Distribution initialIds)
    {
        Distribution ranking = rankedList.copy();
        for (Iterator it=knownIds.iterator(); it.hasNext();)
            ranking.remove(it.next());
        rankedListMap.put( exampleId, ranking );
        positiveIdMap.put( exampleId, posIds );
        initialNodesMap.put( exampleId, initialIds );
    }

    //
    // accessors
    //

    public Distribution getRanking(String exampleId) { return (Distribution)rankedListMap.get(exampleId); }
    private Set getPosIds(String exampleId) {
        Set set = (Set)positiveIdMap.get(exampleId);
        if (set==null) positiveIdMap.put(exampleId, (set=Collections.EMPTY_SET));
        return set;
    }
    private Distribution getInitialDist(String exampleId){
        return (Distribution)initialNodesMap.get(exampleId);
    }
    private Iterator getExampleIterator() { return rankedListMap.keySet().iterator(); }
    private int numPosIds(String exampleId) { return getPosIds(exampleId).size(); }
    private boolean isPositive(String exampleId,GraphId id) { return getPosIds(exampleId).contains(id); }
    private int numExamples() { return rankedListMap.keySet().size(); }

    //
    // split the examples into groups of K
    //
    private String[][] exampleGroups(int groupSize)
    {
        int remainder = numExamples() % groupSize;
        int numRemainderGroups = remainder>0 ? 1 : 0;
        String[][] group = new String[(numExamples()/groupSize) + numRemainderGroups][];
        for (int i=0; i<group.length-numRemainderGroups; i++) {
            group[i] = new String[groupSize];
        }
        if (numRemainderGroups>0) {
            group[group.length-1] = new String[ remainder ];
        }
        int j=0, k=0;
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            group[j][k++] = name;
            if (k>=group[j].length) {
            j++;
            k=0;
            }
        }
        return group;
    }

    //
    // useful subroutine - returns an array a such that a[0] is recall
    // at each rank, and a[1] is precision at each rank.
    //
    private double[][] recallAndPrecisionForEachK(String exampleId)
    {
        Distribution ranking = getRanking(exampleId);
        int totalPos = numPosIds(exampleId);
        double[] recall = new double[ranking.size()+1];
        double[] precision = new double[ranking.size()+1];
        int rank=0;
        double numPosAboveRank=0;
        for (Iterator j=ranking.orderedIterator(); j.hasNext(); ) {
            GraphId id = (GraphId)j.next();
            rank++;
            if (isPositive(exampleId,id))  numPosAboveRank++;
            if (totalPos>0) {
            recall[rank] = numPosAboveRank/totalPos;
            precision[rank] = numPosAboveRank/rank;
            } else {
            recall[rank] = precision[rank] = 1.0;
            }
        }
        double[][] result = new double[2][];
        result[0] = recall;
        result[1] = precision;
        return result;
    }


    //
    // public functions
    //
    /** Non-interpolated average precision.
     */
    public double averagePrecision(String exampleId)
    {
        int numCorrect = numPosIds(exampleId);
        if (numCorrect==0) return 1.0;
        double numPosSoFar = 0, totPrec = 0;
        Distribution weights = getRanking(exampleId).copy();
        Distribution removeNodes = (Distribution)initialNodesMap.get(exampleId);
        for (Iterator it=removeNodes.iterator();it.hasNext();)
            weights.remove(it.next());
        Distribution ranks = weights.rankDistribution();
        for (Iterator i=ranks.orderedIterator(false); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            if (isPositive(exampleId,id)) {
                numPosSoFar++;
                totPrec += numPosSoFar/ranks.getWeight(id);
                System.out.println(exampleId.toString() + " " + id.toString() + " " + ranks.getWeight(id));
            }
            if (numPosSoFar==numCorrect)
                return totPrec/numCorrect;
        }
        return totPrec/numCorrect;
    }

    /** mean average precision (MAP)
     */
    public double meanAveragePrecision()
    {
        double averagePrecisionSum = 0;
        for (Iterator i=getExampleIterator(); i.hasNext(); )
            averagePrecisionSum += averagePrecision(i.next().toString());
        return averagePrecisionSum/numExamples();
    }

    /**
     * Precision at Rank 1
     */
    
    public int getPrecAtRank1(String exampleId)
    {
        if (numPosIds(exampleId)==0) return 0;
        Distribution ranking = getRanking(exampleId);
        for (Iterator i=ranking.orderedIterator(); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            if (isPositive(exampleId,id)) {
                return 1;
            }

            break;
        }
        return 0;
    }

    /** Max value of F1 over all possible thresholds.
     */
    public double maxF1(String exampleId)
    {
        if (numPosIds(exampleId)==0) return 1.0;
        double rank=0, numPosAboveRank=0, maxF1=0;
        Distribution ranking = getRanking(exampleId);
        for (Iterator i=ranking.orderedIterator(); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            rank++;
            if (isPositive(exampleId,id)) {
            numPosAboveRank++;
            }
            double precision = numPosAboveRank/rank;
            double recall = numPosAboveRank/numPosIds(exampleId);
            if (precision+recall>0) {
            double f1 = 2*precision*recall/(precision+recall);
            maxF1 = Math.max( maxF1, f1 );
            }
        }
        return maxF1;
    }

    public double maxRecall(String exampleId)
    {
        if (numPosIds(exampleId)==0) return 1.0;
        double numRanked = 0;
        Distribution ranking = getRanking(exampleId);
        for (Iterator i=getPosIds(exampleId).iterator(); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            if (ranking.getWeight(id)>0) numRanked++;
        }
        return numRanked/numPosIds(exampleId);
    }

    /** Interpolated precision at eleven recall levels, averaged over all examples. */
    public double[] averageElevenPointPrecision()
    {
        double[] averagePrecision = new double[11];
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            double[] precision = elevenPointPrecision(name);
            for (int j=0; j<=10; j++) {
            averagePrecision[j] += precision[j];
            }
        }
        for (int j=0; j<=10; j++) {
            averagePrecision[j] /= numExamples();
        }
        return averagePrecision;
    }
    

    /** Interpolated precision at eleven recall levels: 0.0, ... ,1.0 */
    public double[] elevenPointPrecision(String exampleId)
    {
        double[][] a = recallAndPrecisionForEachK(exampleId);
        double[] recall = a[0];
        double[] precision = a[1];
        double[] interpolatedPrecision = new double[11];
        for (int k=1; k<recall.length; k++) {
            double r = recall[k];
            double p = precision[k];
            for (int j=0; j<=10; j++) {
            if (r >= j/10.0) {
                interpolatedPrecision[j] = Math.max( interpolatedPrecision[j], p );
            }
            }
        }
        return interpolatedPrecision;
    }

    /** A summary table. Columns are: avgpr, the 
     * non-interpolated average precision of the ranking (the average
     * of this is thus mean average precision); maxF1, the maximum F1
     * value for the ranking; maxRec, the maximum recall achieved
     * (i.e., the fraction of relevant nodes appearing in the
     * ranking); and #pos, the number of positive/relevant nodes.
     */
    public String toTable()
    {
        if (positiveIdMap.keySet().size()==0) {
            return "no examples?\n";
        }
        StringBuffer buf = new StringBuffer();
        DecimalFormat fmt = new DecimalFormat("0.000");
        DecimalFormat fmt2 = new DecimalFormat("0.0");
        buf.append("avgPr\tmaxF1\tmaxRec\t#pos\tPrec@1\n");
        double totMaxF1=0, totAvgPrec=0, totPos=0, totMaxRec=0, totPrecAt1 =0;
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            double ap = averagePrecision(name);
            double maxf = maxF1(name);
            double maxr = maxRecall(name);
            int np = numPosIds(name);

            int precAt1 = getPrecAtRank1(name);
            buf.append(fmt.format(ap) + "\t");
            buf.append(fmt.format(maxf) + "\t");
            buf.append(fmt.format(maxr) + "\t");
            buf.append(np + "\t");

            buf.append(precAt1 + "\t");
            buf.append(name + "\n");
            totAvgPrec += ap;
            totMaxF1 += maxf;
            totMaxRec += maxr;
            totPos += np;

            totPrecAt1 += precAt1;
        }
        buf.append("\n");
        buf.append(fmt.format(totAvgPrec/numExamples())+"\t");
        buf.append(fmt.format(totMaxF1/numExamples())+"\t");
        buf.append(fmt.format(totMaxRec/numExamples())+"\t");
        buf.append(fmt2.format(totPos/numExamples())+"\t");

        buf.append(fmt.format(totPrecAt1/numExamples())+"\t");
        buf.append("average" + "\n");
        return buf.toString();
    }

    private double[] averageRecallAtEachK()
    {
        int longestRankedList = 0;
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            longestRankedList = Math.max( getRanking(name).size(), longestRankedList );
        }
        // first have recall[k] be total recall over all examples at rank k
        double[] recall = new double[longestRankedList+1];
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Distribution ranking = getRanking(name);
            int rank=0;
            double numPosAboveRank=0;
            for (Iterator j=ranking.orderedIterator(); j.hasNext(); ) {
            GraphId id = (GraphId)j.next();
            rank++;
            if (isPositive(name,id))  numPosAboveRank++;
            if (numPosIds(name)>0) {
                recall[rank] += numPosAboveRank/numPosIds(name);
            } else {
                recall[rank] += 1.0;
            }
            }
            // extend the last recorded recall level to the end of this list
            for (int k=rank+1; k<recall.length; k++) {
            recall[k] += recall[rank];
            }
        }
        // now scale recall to average recall at K
        for (int k=1; k<recall.length; k++) {
            recall[k] /= numExamples();
        }
        recall[0] = -1; // convenient
        return recall;
    }

    /** Recall as function of K, averaged over all examples. */
    public String averageRecallAsFunctionOfK()
    {
        DecimalFormat fmt = new DecimalFormat("0.000");
        StringBuffer buf = new StringBuffer("");
        buf.append("K\tAvgRecall\n");
        double[] recall = averageRecallAtEachK();
        for (int k=1; k<recall.length; k++) {
            if (recall[k]!=recall[k-1]) {
            buf.append(k+"\t"+fmt.format(recall[k])+"\n");
            }
        }
        return buf.toString();
    }


    public String toTable(String name,int numToShowAllEntries)
    {
        Distribution ranking = getRanking(name);
        StringBuffer buf = new StringBuffer();
        DecimalFormat fmt = new DecimalFormat("0.000");
        int rank = 0;
        for (Iterator i=ranking.orderedIterator(); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            ++rank;
            double score = ranking.getLastProbability();
            String tag = isPositive(name,id) ? "+" : "-";
            // print the entry if it's positive, or if it's near the top
            if (rank<numToShowAllEntries || tag.startsWith("+")) {
            buf.append(rank+"\t"+fmt.format(score)+"\t"+tag+"\t"+id+"\n");
            }
        }
        // now print the false negatives - ie the unranked positives
        for (Iterator i=getPosIds(name).iterator(); i.hasNext(); ) {
            GraphId id = (GraphId)i.next();
            if (ranking.getWeight(id)==0) {
            String tag = "+";
            buf.append(">"+rank+"\t0\t"+tag+"\t"+id+"\n");
            }
        }
        return buf.toString();
    }

    public Viewer toGUI()
    {
        ParallelViewer v = new ParallelViewer();
        v.addSubView( "Summary Table", new ComponentViewer() {
            public JComponent componentFor(Object o) {
                GraphSearchEval gsEval = (GraphSearchEval)o;
                return new VanillaViewer( gsEval.toTable() );
            }
            });
        ParallelViewer v2 = new ParallelViewer();
        v.addSubView( "11-Pt Precision", v2 );
        v2.addSubView( "Averaged", new ComponentViewer() {
            public JComponent componentFor(Object o) {
                GraphSearchEval gsEval = (GraphSearchEval)o;
                double[] avgPrec = gsEval.averageElevenPointPrecision();
                LineCharter lc = new LineCharter();
                lc.startCurve("11-Pt Avg Prec");
                for (int j=0; j<=10; j++) {
                lc.addPoint( j/10.0, avgPrec[j] );
                }
                return lc.getPanel("11-Pt Average Interpolated Precision", "Recall", "Precision");
            }
            });
        String[][] groups = exampleGroups(GRAPHS_PER_PAGE);
        for (int i=0; i<groups.length; i++) {
            final String tag = groups.length==1 ? "Details" : ("Details: Group "+(i+1));
            final String[] group = groups[i];
            v2.addSubView( tag, new ComponentViewer() {
                public JComponent componentFor(Object o) {
                GraphSearchEval gsEval = (GraphSearchEval)o;
                LineCharter lc = new LineCharter();
                for (int i=0; i<group.length; i++) {
                    String name = group[i];
                    double[] avgPrec = gsEval.elevenPointPrecision(name);
                    lc.startCurve(name);
                    for (int j=0; j<=10; j++) {
                    lc.addPoint( j/10.0, avgPrec[j] );
                    }
                }
                return lc.getPanel("11-Pt Interpolated Precision", "Recall", "Precision");
                }
            });

        }
        v.addSubView( "AvgRecall vs Rank", new ComponentViewer() {
            public JComponent componentFor(Object o) {
                GraphSearchEval gsEval = (GraphSearchEval)o;
                double[] avgRec = averageRecallAtEachK();
                LineCharter lc = new LineCharter();
                lc.startCurve("Recall vs Rank");
                for (int i=1; i<avgRec.length; i++) {
                lc.addPoint( i, avgRec[i] );
                }
                return lc.getPanel("AvgRecall vs Rank", "Rank", "AvgRecall");
            }
            });
        ParallelViewer v3 = new ParallelViewer();
        v3.putTabsOnLeft();
        v.addSubView( "Details", v3 );
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            final String name = (String)i.next();
            v3.addSubView( name, new ComponentViewer() {
                public JComponent componentFor(Object o) {
                GraphSearchEval gsEval = (GraphSearchEval)o;
                if (graph==null) {
                    return new VanillaViewer( toTable(name,NUM_TOP_TO_SHOW) );
                } else {
                    ParallelViewer v4 = new ParallelViewer();
                    v4.addSubView( "Labeled Ranking",
                           new TransformedViewer(new VanillaViewer()) {
                               public Object transform(Object o) {
                               return ((GraphSearchEval)o).toTable(name,NUM_TOP_TO_SHOW);
                               }
                           });
                    v4.addSubView( "Clickable Ranking",
                           new TransformedViewer(new SmartVanillaViewer()) {
                               public Object transform(Object o) {
                               GraphSearchEval e = (GraphSearchEval)o;
                               return new WeightedTextGraph( e.getRanking(name), e.graph );
                               }
                           });
                    v4.setContent( gsEval );
                    return v4;
                }
                }
            });
        }

        v.setContent(this);
        return v;
    }

    //
    // implement Saveable
    //
    final static private String EVAL_FORMAT_NAME = "Graph Searcher Evaluation";
    final static private String EVAL_EXT = ".gsev";
    public String[] getFormatNames() { return new String[]{EVAL_FORMAT_NAME}; }
    public String getExtensionFor(String format) { return EVAL_EXT; }
    public void saveAs(File file,String formatName) throws IOException { save(file);	}
    public Object restore(File file) throws IOException	{ return load(file); }

    final static private StringEncoder encoder = new StringEncoder('%',"/\\:;$ \t\n");
    final static private String evalExt = Evaluation.EVAL_EXT;

    private void save(File file) throws IOException 
    {
        PrintStream out = new PrintStream(new FileOutputStream(file));
        for (Iterator i=getExampleIterator(); i.hasNext(); ) {
            String name = (String)i.next();
            Distribution ranking = getRanking(name);
            int rank = 0;
            for (Iterator j=ranking.orderedIterator(); j.hasNext(); ) {
            GraphId id = (GraphId)j.next();
            rank++;
            double weight = ranking.getLastWeight();
            out.println(name +"\t"+ id +"\t"+ rank +"\t" + weight);
            }
            Set pos = getPosIds(name);
            for (Iterator j=pos.iterator(); j.hasNext(); ) {
            GraphId id = (GraphId)j.next();
            out.println(name +"\t" + id);
            }
        }
        out.close();
    }

    static public GraphSearchEval load(File file) throws IOException
    {
        GraphSearchEval eval = new GraphSearchEval();
        eval.loadFromFile(file);
        return eval;
    }

    private void loadFromFile(File file) throws IOException 
    { 
        LineNumberReader in = new LineNumberReader(new InputStreamReader(new FileInputStream(file)));
        String line = null;
        while ((line = in.readLine())!=null) {
            String[] parts = line.split("\t");
            if (parts.length==2) {
            // exampleId positiveGraphId
            Set pos = (Set)positiveIdMap.get(parts[0]);
            if (pos==null) positiveIdMap.put(parts[0], (pos = new TreeSet()));
            pos.add( GraphId.fromString(parts[1]) );
            } else if (parts.length==4) {
            // exampleId graphId rank weight
            Distribution ranking = (Distribution)rankedListMap.get(parts[0]);
            if (ranking==null) rankedListMap.put(parts[0], (ranking = new TreeDistribution()));
            ranking.add( StringUtil.atof(parts[3]), GraphId.fromString(parts[1]) );
            } else {
            throw new IllegalArgumentException(file+" line "+in.getLineNumber()+": illegal format");
            }
        }
    }

    //
    // test
    //

    public class MyCLP extends BasicCommandLineProcessor {
        public void graph(String s) { graph=new TextGraph(s); }
        public void gui() { guiFlag = true; }
        public void loadFrom(String s) {
            loadedFile=s;
            try { loadFromFile(new File(s)); } catch (IOException ex) { ex.printStackTrace(); }
        }
    }
    public void processArguments(String[] args) { new MyCLP().processArguments(args); }


    static public void main(String[] args) throws IOException
    {
        GraphSearchEval eval = new GraphSearchEval();
        eval.processArguments(args);
        if (eval.guiFlag) new ViewerFrame(eval.loadedFile, eval.toGUI());
        else System.out.println(eval.toTable());
    }
}
