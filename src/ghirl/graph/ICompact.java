package ghirl.graph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import edu.cmu.pra.graph.IGraph;
import ghirl.util.Distribution;


/** should put this in Walkable? but let's not do it for now */
public interface ICompact extends Graph, IGraph{
  /** want a more efficient interface 
   * Usually I prefer abstract class then interface
   * since I can implement a default behavior for all the subclasses.
   * Decide not to touch this now --Ni*/
  public Distribution walk1(int from,int linkLabel);
  
  public int getNodeIdx(GraphId from);	
  public GraphId[] getGraphIds();
  
  /** Loads the graph from the compact format files specified. See CompactGraph for format details. */
  public void load(File size, File link, File node, File row) 
  	throws IOException, FileNotFoundException;
  
  public void load(String dir) 
	throws IOException, FileNotFoundException;
}
