package ghirl.graph;

import edu.cmu.pra.CTag;
import edu.cmu.pra.ergraph.ERGraph;
import edu.cmu.pra.ergraph.Entity;
import edu.cmu.pra.ergraph.Section;
import edu.cmu.pra.etgraph.ETGraph;
import edu.cmu.pra.etgraph.EntType;
import edu.cmu.pra.etgraph.Relation;
import ghirl.util.Distribution;

import java.util.Iterator;
import java.util.Set;

public class PRAGraph implements MutableGraph{
	private ERGraph graph;
	private ETGraph net;
  ////////////////////////////////////////////////////////////
  /////////////////////  Walkable  ////////////////////////////
  ////////////////////////////////////////////////////////////
	
  /** Return an ordered array of link labels that lead out from any
   * node.  GraphUtil contains a default implementation of this
   * method which loops over all nodes.  For large graphs it useful
   * to cache the information separately.
   */
  public String[] getOrderedEdgeLabels(){
  	return net.vRel.getVS(CTag.name).toArray();
  }

  /** Return all link labels that lead out from this node. */
  public Set getEdgeLabels(GraphId from){
  	EntType et=net.getEntType(from.getFlavor());  	
  	return et.vRelationTo.getVS(CTag.name).toSet();
  }

  /** Return Set of ids of all linked-to nodes */
  public Set followLink(GraphId from,String linkLabel){
  	Entity e=graph.getSection(from.getFlavor()).getEntity(from.getShortName());
  	Relation r= net.getRelation(linkLabel);
  	return e.mvRelEnt.get(r.id).toSet();
  }

  /** Similar to followLink, but returns a Distribution of all ids
   * originating from this node, for any label. */
  public Distribution walk1(GraphId from){
  	return null;
  }

  /** Similar to followLink, but returns a Distribution of ids
   * originating from this node, for the linklabel. */
  public Distribution walk1(GraphId from,String linkLabel){
  	return null;
  }

  /** Get a string corresponding to the node's "content". 
   * If there is no such thing, then the shortName of the
   * id should be returned.
   */
  public String getTextContent(GraphId id){
  	return null;
  }

  /** Convert a string to a distribution over nodes, e.g. by
   * constructing a distribution of all nodes that partially match
   * the string. This is a convenience method, which is especially
   * convenient for TextGraph's to have.
   */
  public Distribution asQueryDistribution(String queryString){
  	return null;
  }

  
  ////////////////////////////////////////////////////////////
  /////////////////////  Graph  ////////////////////////////
  ////////////////////////////////////////////////////////////
  /** Check for a vertex. */
  public boolean contains(GraphId id){
  	Section sec=graph.getSection(id.getFlavor());  
  	if (sec==null)
  		return false;
  	return sec.getEntity(id.getShortName())!=null;
  }

  /** Get the id in the graph corresponding to a flavor and name.
   * Returns null if the graph doesn't contain this node. */
  public GraphId getNodeId(String flavor,String shortNodeName){
  	return null;
  }

  /** Get all node ids one by one. */
  public Iterator getNodeIterator(){
  	return null;
  }

  /** Return an ordered array of all GraphId's that would be
   * returned by the iterator() method. GraphUtil contains a default
   * implementation of this method.  For large persistant graphs it
   * useful to cache the information separately to reduce disk
   * access.
   */
  public GraphId[] getOrderedIds(){
  	return null;
  }
  
  /** Get a property of a node. */
  public String getProperty(GraphId from,String prop){
  	return null;
  }
  
  
  ////////////////////////////////////////////////////////////
  /////////////////////  MutableGraph  ////////////////////////////
  ////////////////////////////////////////////////////////////
  
  /** Create a new node that lives in this Graph. */
  public GraphId createNode(String flavor,String shortName){
  	return null;
  }

  /** Create a new node that lives in this Graph. */
  public GraphId createNode(String flavor,String shortName,Object content){
  	return null;
  }

  /** Add an edge to the graph. */
  public void addEdge(String label,GraphId from,GraphId to){
  	return ;
  }

  /** Set a property of a node. */
  public void setProperty(GraphId from,String prop,String val){
  	return;
  }

  /** Freeze the graph to no more write operations are possible
      until a 'melt()' operation is called.  This also syncs the
      graph to disk, if necessary.  */
  public void freeze(){
  	return;
  };

  /** Reverse a 'freeze' operation.  */
  public void melt(){
  	return ;
  }
}
