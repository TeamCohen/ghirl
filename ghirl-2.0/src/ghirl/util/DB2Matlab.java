package ghirl.util;

import edu.cmu.minorthird.util.IOUtil;

import java.io.*;
import java.util.*;

import ghirl.graph.Graph;
import ghirl.graph.TextGraph;
import ghirl.graph.GraphId;

/**
 * iterate over the graph, and output:
 * 1. data file, that can be uploaded as a (sparse)matrix in Matlab
 * 2. index file, converting from numerical ids to graph ids
 *
 * Further advice by Hanghang Tong:
 * a. Load the data file in matlab by "load": load myG.txt
 * b. By a, you will have a variable "myG" (|E|x3) in matlab. convert "myG"
   to a sparse marix (W) and you are done: W = spconvert(myG);
 *
 * Note: the weights below are calculated in a default way -- you may want
 * update this.
 *
 * @Author: Einat Minkov
 */

public class DB2Matlab
  {
    Graph graph;
    Set stopEdges = new HashSet();
    Map graphId2index = new HashMap();
    Map index2GraphId = new HashMap();

    int nodeIndex;

    public DB2Matlab(Graph graph) {
        this.graph = graph;
        nodeIndex=0;
        stopEdges.add("isa");
        stopEdges.add("isaInverse");
    }

    public void graph2Files() throws IOException {

        BufferedWriter bw = new BufferedWriter(new FileWriter(new File("matrix.txt")));

        int edgeCount = 0;
        int nodeCount = 0;

        for (Iterator i=graph.getNodeIterator(); i.hasNext(); i.next() ) {
            GraphId from = (GraphId)i.next();
            int fromIndex = getIndex(from.toString());

            Set edgeLabels = graph.getEdgeLabels(from);
            int outgoingEdgeCount = graph.walk1(from).size();
            for (Iterator edgeIt = edgeLabels.iterator(); edgeIt.hasNext();){
                String label = (String)edgeIt.next();
                if (!stopEdges.contains(label)){
                    Distribution dist = graph.walk1(from,label);
                    edgeCount += outgoingEdgeCount;

                    for (Iterator it=dist.iterator();it.hasNext();){
                        GraphId to = (GraphId)it.next();
                        int toIndex = getIndex(to.toString());
                        bw.write(fromIndex+" "+toIndex+" "+1/(double)outgoingEdgeCount);
                        bw.newLine();
                    }
                }
            }
            nodeCount++;

            if (nodeCount%1000==0) System.out.println(nodeCount + " " + edgeCount);
        }
        bw.close();

        bw = new BufferedWriter(new FileWriter(new File("index2GraphId.txt")));
        for (Iterator it=index2GraphId.keySet().iterator();it.hasNext();){
            Integer index = (Integer)it.next();
            String id = (String)index2GraphId.get(index);
            bw.write(index.intValue() + " " + id);
            bw.newLine();
        }
        bw.close();
    }

    public int getIndex(String id){
        if (graphId2index.containsKey(id))
            return ((Integer)graphId2index.get(id)).intValue();
        else {
            graphId2index.put(id,new Integer(nodeIndex));
            index2GraphId.put(new Integer(nodeIndex),id);
            nodeIndex++;
            return (nodeIndex-1);
        }
    }



    public static void main(String[] args) throws IOException{
        Graph graph = new TextGraph(args[0],'r');
        DB2Matlab dbm = new DB2Matlab(graph);

        System.out.println("=======================================================");
        System.out.println(" Writing graph weighted edges to matrix.txt"            );
        System.out.println(" and the Matlab-ghirl inverse key, to index2GraphId.txt ");
        System.out.println("=======================================================");
        System.out.println();

        dbm.graph2Files();

    }
}
