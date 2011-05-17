package ghirl.graph;

import ghirl.util.*;
import java.util.*;
/**
 * Find a subset of connecting paths in a graph (up to length K)
 * from a start node to a single end point.
 * Paths that include cycles (going back and forth between two nodes) may be
 * ignored (controlled by 'allowCycles' policy).
 *
 * To be efficient - walk concurrently from the source and target nodes,
 * up to meeting point.
 *
 * Author: Einat
 */

public class ConnectingPathsFinder
{
    private int maxPathLength = 2;
    private Graph graph;
    private Set edgeStopList;
    private boolean allowCycles = true;

    public ConnectingPathsFinder(Graph graph, Set edgeStopList, boolean allowCycles)
    {
	    this.graph = graph;
        this.edgeStopList = edgeStopList;
        this.allowCycles = allowCycles;
    }

    public ConnectingPathsFinder(Graph graph, Set edgeStopList)
    {
	    this.graph = graph;
        this.edgeStopList = edgeStopList;
    }

    public void allowCycles(boolean policy){
        this.allowCycles = policy;
    }


    // aggregate by edges-only paths (sum up probability of all relevant specific paths)
    public Set aggregate(Set paths){
        Set edgePaths = new HashSet();
        for (Iterator it=paths.iterator();it.hasNext();){
            Path path = ((Path)it.next());
             if (!edgePaths.contains(path)) edgePaths.add(path.getEdgeSeq());
        }
        return edgePaths;
    }


    public Set getConnectingPaths(Distribution query, GraphId target, int maxPathLength){
        Set paths = new HashSet();
        for (Iterator it=query.iterator();it.hasNext();){
            paths.addAll(getConnectingPaths((GraphId)it.next(),target,maxPathLength));
        }
        return paths;
    }


    public Set getConnectingPaths(GraphId source, GraphId target, int maxPathLength){
        this.maxPathLength = maxPathLength;
        return getConnectingPaths(source,target);
    }

    private Set getConnectingPaths(GraphId source, GraphId target){
        Set connectingPaths = new HashSet();
        int backwardLevels = maxPathLength/2;
        int forwardLevels = maxPathLength - backwardLevels;

        // Store the current paths walking from source and target,
        // indexed by the node that is furthest from the origion (either source or target).
        Set sourcePaths = new TreeSet(), targetPaths = new TreeSet();
        Map sourcePathsMap = new TreeMap(), targetPathsMap = new TreeMap();

        sourcePaths.add(new Path(source));
        targetPaths.add(new Path(target));
        sourcePathsMap.put(source,sourcePaths);
        targetPathsMap.put(target,targetPaths);

        // expand partial paths
        for (int i=0; i<forwardLevels; i++)
            sourcePathsMap = expandPathsMap(sourcePathsMap,i,true);

        for (int j=0; j<backwardLevels; j++)
            targetPathsMap = expandPathsMap(targetPathsMap,j,false);

        //System.out.println("source map : " + sourcePathsMap.toString());
        //System.out.println("target map : " + targetPathsMap.toString());

        // find matches between the paths
        for (Iterator it=sourcePathsMap.keySet().iterator();it.hasNext();){
            GraphId endPoint = (GraphId)it.next();
            //System.out.println("end point: " + endPoint.toString());
            if (targetPathsMap.containsKey(endPoint)){
                //System.out.println("END POINT FOUND :-) ");
                Set startPaths = (Set)sourcePathsMap.get(endPoint);
                Set endPaths = (Set)targetPathsMap.get(endPoint);
                for (Iterator it2=endPaths.iterator();it2.hasNext();){
                    Path pEnd = (Path)it2.next();
                    for (Iterator it3=startPaths.iterator();it3.hasNext();){
                        Path pStart = (Path)it3.next();
                        Path concatenated = concatPaths(pStart,pEnd);
                        if (concatenated != null) connectingPaths.add(concatenated);
                    }
                }
            }
        }
        return connectingPaths;
    }

    // expand those paths that are of maximal current length, and add them to the pool of paths
    // the paths are contained in a map, indexed by the end node
    private Map expandPathsMap(Map paths, int length, boolean forwardMode){
        Map pathsNew = new HashMap();
        pathsNew.putAll(paths);
        for (Iterator it=paths.keySet().iterator();it.hasNext();){
            GraphId endNode = (GraphId)it.next();
            //System.out.println(endNode.toString());
            Set curPaths = (Set)paths.get(endNode);
            pathsNew.put(endNode,curPaths);
            for (Iterator it2=curPaths.iterator();it2.hasNext();){
                Path p = (Path)it2.next();
                if (p.getSize()==length){
                    GraphId endPoint = p.getEndPoint();
                    for (Iterator i=graph.getEdgeLabels(endPoint).iterator(); i.hasNext(); ){
                       String linkLabel = (String)i.next();
                       if (!edgeStopList.contains(linkLabel)){
                           Distribution dist = graph.walk1(endPoint,linkLabel);
                           for (Iterator j=dist.iterator(); j.hasNext(); ){
                               GraphId newEndPoint = (GraphId)j.next();
                               if (allowCycles || !p.includesNode(newEndPoint)){
                                    Path pNew = (Path)p.clone();
                                    String effLinkLabel = linkLabel;
                                    if (!forwardMode) effLinkLabel = invLabel(linkLabel);
                                    pNew.append(effLinkLabel,newEndPoint);
                                    Set newPathsNode = new HashSet();
                                    if (pathsNew.keySet().contains(newEndPoint))
                                        newPathsNode = (Set)pathsNew.get(newEndPoint);
                                    newPathsNode.add(pNew);
                                    pathsNew.put(newEndPoint,newPathsNode);
                               }
                           }
                       }
                    }
                }
            }
         }
        //System.out.println("current forward / backward paths: " + pathsNew.toString());
        return pathsNew;
    }


    private Path concatPaths(Path forward, Path backward){

        //System.out.println("  forward: " + forward.toString());
        //System.out.println("  backward: " + backward.toString());

        if (!forward.getEndPoint().equals(backward.getEndPoint())) return null;
        Path newPath = (Path)forward.clone();
        int size = backward.getSize();
        LinkedList nodes = backward.getNodes();
        LinkedList edges = backward.getEdges();
        for (int i=(size-1); i>=0; i--)
            newPath.append((String)edges.get(i),(GraphId)nodes.get(i));

        //System.out.println("result: " + newPath.toString());
        //System.out.println(" =-=-=-=-=-=-=-=-=-=-");

        if (!allowCycles && newPath.hasCycles()) return null;
        return newPath;
    }

    private String invLabel(String linkLabel){
        if (linkLabel.equals("_hasTerm")) return "_inFile";
        if (linkLabel.equals("_inFile")) return "_hasTerm";
        if (linkLabel.endsWith("Inverse")) linkLabel = linkLabel.replaceAll("Inverse","");
        else linkLabel = linkLabel+"Inverse";
        return linkLabel;
    }

    //identify parent nodes and connecting edges, exploiting edge symmetry
    public Map getIncomingNodeEdges(GraphId node){
        Map nodesEdges = new HashMap();
        for (Iterator j=graph.getEdgeLabels(node).iterator(); j.hasNext(); ) {
            String outLabel = (String)j.next();
            for (Iterator it=graph.followLink(node,outLabel).iterator();it.hasNext();) {
                GraphId parent = (GraphId)it.next();
                String label = invLabel(outLabel);
                HashSet labels = new HashSet();
                if (nodesEdges.containsKey(parent)) { labels = (HashSet)nodesEdges.get(parent); }
                labels.add(label);
                nodesEdges.put(parent,labels);
            }
        }
        return nodesEdges;
    }


    public static void main(String[] args){
        Graph graph = null;
        Distribution sourceDist=null, targetDist = null;
        int maxPathLen = 0;
        boolean allowCycles = false;
        ConnectingPathsFinder cpf = null;
        try{
            //graph = new CachingGraph(new TextGraph(args[0],'r'));
            graph = new TextGraph(args[0]);
            cpf = new ConnectingPathsFinder(graph,new HashSet());
            sourceDist = CommandLineUtil.parseNodeOrNodeSet(args[1],graph);
            targetDist = CommandLineUtil.parseNodeOrNodeSet(args[2],graph);
            maxPathLen = (new Integer(args[3])).intValue();
        }catch(Exception e){
            System.out.println("USAGE: graph sourceNode targeNode maximal-path-length");
        }
        GraphId sourceNode=null, targetNode=null;
        for (Iterator it=sourceDist.iterator();it.hasNext();)
            sourceNode = (GraphId)it.next();
        for (Iterator it=targetDist.iterator();it.hasNext();)
            targetNode = (GraphId)it.next();
        Set paths = cpf.getConnectingPaths(sourceNode,targetNode,maxPathLen);
        System.out.println("Found " + paths.size() + " paths:");
        for (Iterator it=paths.iterator();it.hasNext();){
            Path p = (Path)it.next();
            System.out.println(p.toString() + " " + p.getSize());
        }
    }
}
