package ghirl.graph;

import java.util.*;
import java.util.LinkedList;

/** Object that stores a path in a graph.
*/
public class Path implements Comparable
{
    private LinkedList nodes;
    private LinkedList edges;
    // a placeholder for node weights (depending on the graph properties)


    public Path(GraphId sourceNode){
        this.nodes = new LinkedList();
        this.edges = new LinkedList();
        if (sourceNode!=null) nodes.add(sourceNode);
    }

    public Path(){
        this.nodes = new LinkedList();
        this.edges = new LinkedList();
    }

    public void append(String edge, GraphId node){
        nodes.add(node);
        edges.add(edge);
    }

    public GraphId getSourceNode(){
        return (GraphId)nodes.get(0);
    }

    public GraphId getEndPoint(){
        return (GraphId)nodes.get(nodes.size()-1);
    }

    public LinkedList getNodes(){
        return nodes;
    }

    public LinkedList getEdges(){
        return edges;
    }

    public GraphId getNode(int index){
        return (GraphId)nodes.get(index);
    }

    public int getSize(){
        return edges.size();
    }

    public String toString(){
        String str = "x ";
        if (nodes.get(0) != null) str = nodes.get(0).toString();
        for (int i=1; i<nodes.size();i++){
            str = str.concat(" -> " + edges.get(i-1) + " -> ");
            GraphId node = (GraphId)nodes.get(i);
            if (node != null)
            str = str.concat((nodes.get(i)).toString());
            else str = str.concat(" x ");
        }
        return str;
    }

    public String getEdgeSequence(){
        String str = (String)edges.get(0);
        for (int i=1; i<edges.size();i++){
            str = str.concat(" -> " + edges.get(i));
        }
        return str;
    }

    // allows a preliminary check, to avoid cyclic paths
    public boolean includesNode(GraphId node){
        for (int i=0; i<nodes.size(); i++)
            if (nodes.get(i).toString().equals(node.toString())) return true;
        return false;
    }

    public boolean hasCycles(){
        Set nodes = new HashSet();
        for (int i=0; i<this.nodes.size(); i++){
            String node = ((GraphId)this.nodes.get(i)).toString();
            if (nodes.contains(node)) return true;
            else nodes.add(node);
        }
        return false;
    }

    public Path getEdgeSeq(){
        Path path = (Path)this.clone();
        path.nodes = new LinkedList();
        for (int i=0; i<(path.edges.size()+1); i++)
            path.nodes.addLast(null);
        return path;
    }


    public Object clone(){
        Path p = new Path();
        p.nodes.add(nodes.get(0));
        for (int i=1; i<nodes.size(); i++){
            p.nodes.add(nodes.get(i));
            p.edges.add(edges.get(i-1));
        }
        return p;
    }

    public int compareTo(Object o){
        Path p = (Path)o;
        if (this.hashCode()>p.hashCode()) return 1;
        else if (this.hashCode()<p.hashCode()) return -1;
        else return 0;
    }

    public boolean equals(Object o){
        Path p = (Path)o;
        if (this.nodes.size()!=p.nodes.size()) return false;
        for (int i=0; i<nodes.size(); i++){
            GraphId id = (GraphId)nodes.get(i);
            GraphId pId = (GraphId)p.nodes.get(i);
            if (id != null && pId!=null)
                if (!id.equals(pId)) return false;
            else if (id== null || pId==null) return false;
        }
        for (int i=1; i<this.edges.size();i++)
            if (!this.edges.get(i-1).equals(p.edges.get(i-1))) return false;
        return true;
    }

    public int hashCode(){
        int hash = 7;
        for (int i=0; i< nodes.size();i++)
            if (nodes.get(i)==null) hash += 1;
            else hash+= i*nodes.get(i).hashCode();
        for (int i=1; i<edges.size();i++)
            hash+= i*edges.get(i).hashCode();
        return hash;
    }

}
