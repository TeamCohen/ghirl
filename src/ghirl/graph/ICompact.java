package ghirl.graph;

import edu.cmu.pra.graph.IGraph;
import ghirl.util.Distribution;


/** should put this in Walkable? but let's not do it for now */
public interface ICompact extends Graph, IGraph{
  /** want a more efficient interface 
   * Usually I prefer abstract class then interface
   * since I can implement a default behavior for all the subclasses.
   * Decide not to touch this now --Ni*/
  public Distribution walk1(int from,int linkLabel);
  
  
/*	public SetI walk2(int from,int linkLabel);
 
	public SetI getNodeIdx( String flavor, String[] vs);
  
  public String getNodeName(int idx);
  public String[] getNodeName(Collection<Integer> vi);
*/
  public int getNodeIdx(GraphId from);	
  public GraphId[] getGraphIds();
}
