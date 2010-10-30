package ghirl.graph;

import java.util.Collection;

import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.util.Distribution;


/** should put this in Walkable? but let's not do it for now */
public interface ICompact extends Graph{
  /** want a more efficient interface 
   * Usually I prefer abstract class then interface
   * since I can implement a default behavior for all the subclasses.
   * Decide not to touch this now --Ni*/
  public Distribution walk1(int from,int linkLabel);
	public SetI walk2(int from,int linkLabel);
 
  public int getNodeIdx(GraphId from);
	public SetI getNodeIdx( String flavor, String[] vs);
  
  public String getNodeName(int idx);
  public String[] getNodeName(Collection<Integer> vi);

	
}
