package ghirl.graph;

import ghirl.util.*;

import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.*;

import java.util.*;
import java.io.*;
import java.text.DecimalFormat;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;


/** A weighted version of a text graph that includes a bunch of
 * visualization techniques.
*/

public class WeightedTextGraph implements Visible
{
	private static final boolean SHOW_PATHS_TO_SOURCE = false;

	private Distribution initDist,nodeDist,edgeDist;
	private Graph graph;
	private boolean showIntermediateNodes=true;
	private static final String LINKS_FROM_TAG = " links from "; // constant phrase in table
	private static final int TEXT_SUMMARY_LENGTH = 60; // length of summary of content displayed

	public WeightedTextGraph(Graph graph)
	{
		this(new TreeDistribution(),new TreeDistribution(),new TreeDistribution(),graph);
	}

	public WeightedTextGraph(Distribution nodeDist,Graph graph)
	{
		this(null,nodeDist,new TreeDistribution(),graph);
	}

	public WeightedTextGraph(Distribution initDist,Distribution nodeDist,Graph graph)
	{
		this(initDist,nodeDist,new TreeDistribution(),graph);
	}

	public WeightedTextGraph(Distribution initDist,Distribution nodeDist, Distribution edgeDist, Graph graph)
	{
		this.initDist = initDist;
		this.nodeDist = nodeDist;
		this.edgeDist = edgeDist;
		this.graph = graph;
		//System.out.println("created WeightedTextGraph, initDist="+initDist);
	}

	public Distribution getNodeDist() { return nodeDist; }
	public Distribution getEdgeDist() { return edgeDist; }
	public Graph getGraph() { return graph; }



	/** If set to 'true', then when the graph is visualized, all nodes
	 * that are incident on any edge in getEdgeDist() will be shown,
	 * even if their weight is zero.  Otherwise, these nodes (and
	 * edges incident on them) will not be shown.
	 */
	public void setShowIntermediateNodes(boolean flag) { showIntermediateNodes=flag; }
	public boolean getShowIntermediateNodes() { return showIntermediateNodes; }

	public Viewer toGUI() 
	{ 
		Viewer listView =  new ZoomedViewer(new NodeListViewer(), new NodeViewer());
		ParallelViewer v = new ParallelViewer();
		v.addSubView("Ranked List", listView );
		v.setContent(this);
		return v;
	}

	/** Build a Viewer for a particular node. 
	 */
	public Viewer toGUI(GraphId id) 
	{
		Viewer v = new NodeViewer();
		v.setContent(id);
		return v;
	}

	/**
	 * Combines several plausible views of a node
	 */
	public class NodeViewer extends ParallelViewer
	{
		public NodeViewer()
		{
	    super();
	    addSubView("Properties", new SimpleNodeViewer());
	    addSubView("Text Content", new NodeTextContentViewer());
	    if (initDist!=null && SHOW_PATHS_TO_SOURCE) {
				addSubView("Path to Query", 
									 new ZoomedViewer(new NodePathToInitDistViewer(),new NodeTextContentViewer()));
	    }
		}
		protected boolean canHandle(int signal,Object argument,ArrayList senders) {
	    return (signal==OBJECT_SELECTED) && (argument instanceof GraphId);
		}
		protected void handle(int signal,Object argument,ArrayList senders) {
	    GraphId id = (GraphId)argument;
	    setContent(id); 
	    repaint(10); // force display update
		}
	}

	/**
	 * View the text for a node in the graph
	 */
	public class NodeTextContentViewer extends ComponentViewer
	{
		public JComponent componentFor(Object o)
		{
	    GraphId id = (GraphId)o;
	    return new VanillaViewer(graph.getTextContent(id));
		}
	}

	/**
	 * View a path from the node to the initDist
	 */
	public class NodePathToInitDistViewer extends ComponentViewer 
	{
		public JComponent componentFor(Object o) 
		{
	    GraphId id = (GraphId)o;
	    BestPathFinder bpf = new BestPathFinder(graph,initDist);
	    GraphId[] path = bpf.bestPath(id);
	    if (path==null) return new JLabel("No path found!");
	    Object[][] tableData = new Object[path.length][2];
	    for (int i=0; i<path.length; i++) {
				tableData[i][0] = new Integer(i);
				tableData[i][1] = path[i];
	    }
	    String[] colNames = {"Dist from "+id, "Graph Id"};
	    JTable table = new JTable(tableData,colNames);
	    // allows user to select the graph id
	    monitorSelections(table,1); 
	    return new JScrollPane(table);
		}
	}

	/**
	 * View basic properties of a single node in the graph.
	 */
	public class SimpleNodeViewer extends ComponentViewer
	{
		public JComponent componentFor(Object o)
		{
	    final GraphId id = (GraphId)o;
	    Set edgeLabels = graph.getEdgeLabels(id);
	    Distribution neighbors = graph.walk1(id).copyTopN(100);
	    Object[][] tableData = new Object[edgeLabels.size() + neighbors.size() + 1][3];
	    int row = 0;
	    tableData[row][0] = "Id:";
	    tableData[row][1] = id.toString();
	    String textContentSummary = graph.getTextContent(id);
	    if (textContentSummary.length()>TEXT_SUMMARY_LENGTH) {
				textContentSummary = textContentSummary.substring(0,TEXT_SUMMARY_LENGTH-3)+"...";
	    }
	    tableData[row][2] = textContentSummary;
	    row++;
	    String tag = "Edges:";
	    for (Iterator i=edgeLabels.iterator(); i.hasNext(); ) {
				String linkLabel = (String)i.next();
				tableData[row][0] = tag;
				tableData[row][1] = linkLabel+" links:";
				Set outlinks = graph.followLink(id,linkLabel);
				while (outlinks.size()>100) outlinks.remove( outlinks.iterator().next() );
				if (outlinks.size()<3) {
					tableData[row][2] = outlinks;
				} else {
					tableData[row][2] = outlinks.size()+LINKS_FROM_TAG+linkLabel;
				}
				tag = "";
				row++;
	    }
	    tag = "Neighbors:";
	    for (Iterator i=neighbors.orderedIterator(); i.hasNext(); ) {
				GraphId	nbr = (GraphId)i.next();
				tableData[row][0] = tag;
				tableData[row][1] = new Double(100*neighbors.getLastWeight()/neighbors.getTotalWeight());
				tableData[row][2] = nbr;
				tag = "";
				row++;
	    }
	    JTable table = new JTable(tableData,
																new String[]{"Property","Link/Weight","Summary/Dest ID"});
	    table.getColumnModel().getColumn(0).setPreferredWidth(10);
	    table.getColumnModel().getColumn(0).setPreferredWidth(20);
	    table.getColumnModel().getColumn(2).setPreferredWidth(TEXT_SUMMARY_LENGTH);
	    monitorSelections(table,2,new Transform() {
					// transform the object in column 2 of the selected row of the table
					// to a graphId. returning an non-Graphid will result in no action;
					// a graphId will be loaded by the parent viewer
					public Object transform(Object obj) {
						// transform a GraphId to itself
						if (obj instanceof GraphId) return obj;
						else if ((obj instanceof Set)) {
							Set outlinks = (Set)obj;
							// transform a singletop set to its only element
							if (outlinks.size()==1) return outlinks.iterator().next();
							// transform a real set with a popup menu
							else return popup(outlinks);
						}
						else if ((obj instanceof String) && ((String)obj).length()>0) {
							// this would be a string of the form "12 links from foo"
							String countString = (String)obj;
							int k = countString.indexOf(LINKS_FROM_TAG);
							if (k>=0) {
								String linkLabel = countString.substring(k+LINKS_FROM_TAG.length());
								// transform using a popup menu
								return popup(graph.followLink(id,linkLabel));
							} else return "transform rejected"; 
						} 
						return "transform rejected";
					}
					// ask the user which of a set he means
					private GraphId popup(Set set) { 
						String title = "Pick a node";
						JComboBox box = new JComboBox();
						for (Iterator i=set.iterator(); i.hasNext(); ) {
							box.addItem( i.next() );
						}
						JOptionPane optionPane = new JOptionPane(new Object[]{title,box});
						JDialog dialog = optionPane.createDialog(SimpleNodeViewer.this,title);
						dialog.setVisible(true);
						//System.out.println("selected "+box.getSelectedItem());
						return (GraphId)box.getSelectedItem();
					}
				});
	    return new JScrollPane(table);
		}
	}

	/**
	 * View the nodes with non-zero weight as a list
	 */
	public class NodeListViewer extends ComponentViewer
	{
		private class WeightedObj implements Comparable {
	    double w;
	    Object o;
	    public WeightedObj(double w,Object o) { this.w=w; this.o=o; }
	    public int compareTo(Object b) {
				return MathUtil.sign( ((WeightedObj)b).w - w );
	    }
		}

		public JComponent componentFor(Object o)
		{
	    final WeightedTextGraph wg = (WeightedTextGraph)o;
	    int numRows = wg.getNodeDist().size();
	    //System.out.println("setting content to wg with "+numRows+" nodes");
	    Object[][] tableData = new Object[numRows][2];
	    int k=0;
	    for (Iterator i=wg.getNodeDist().orderedIterator(); i.hasNext(); ) {
				GraphId id = (GraphId)i.next();
				tableData[k][0] = new Double(wg.getNodeDist().getLastWeight());
				tableData[k][1] = id;
				k++;
	    }
	    String[] columnNames = {"Weight", "Graph Id"};
	    JTable table = new JTable(tableData,columnNames);
	    // allows user to select the graph id
	    monitorSelections(table,1); 
	    return new JScrollPane(table);
		}
	}

}

