package ghirl.graph;

import ghirl.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.ProgressCounter;

import java.util.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.text.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import edu.cmu.minorthird.util.*;


/**
 * A query interface to the graph.
 */

public class QueryGUI extends MessageViewer 
{
	private Graph graph;
	private QueryControls controls;
	private ControlledWeightedGraphViewer graphViewer;

	/**
	 * Constructs a read-only textgraph with the specified name.
	 * @param graphName
	 * @throws IOException
	 */
	public QueryGUI(String graphName) throws IOException 
	{ 
		this(new TextGraph(graphName)); 
	}
	public QueryGUI(Graph graph) 
	{ 
		this.graph = graph; 
		ControlledWeightedGraphViewer graphViewer = new ControlledWeightedGraphViewer();
		controls = new QueryControls();
		setSubView( new ControlledViewer(graphViewer,controls) );
	}

	public void setStopList(String s) { controls.setStopList(s); }

	private class ControlledWeightedGraphViewer extends ComponentViewer implements Controllable
	{
		public JComponent componentFor(Object o)
		{
			WeightedTextGraph wg = (WeightedTextGraph)o;
			return wg.toGUI();
		}
		public void applyControls(ViewerControls c)
		{
			QueryControls controls = (QueryControls)c;
			//System.out.println("ok, applying controls");
			WeightedTextGraph wg = controls.doQuery();
			if (wg!=null) { 
				setContent( wg );
				revalidate();
			}
		}
		public void forwardMessage(String s)
		{
			sendSignal(TEXT_MESSAGE,s);
		}
	}

	// bundles together the parameters for the Walker
	// used by the QueryControls
	public static class WalkerParams
	{
		int tot = 100;
		String stoplist = "";
		Walker walker = new BasicWalker();
		public int getTotalSteps() { return tot; }
		public void setTotalSteps(int i) { tot=i; }
		public void setWalker(Walker walker) { this.walker=walker; }
		public Walker getWalker() { return walker; }
		public String getStopList() { return stoplist; }
		public void setStopList(String s) { 
			this.stoplist=s; 
			String[] ids = this.stoplist.split("\\s*,\\s*");
			for (int i=0; i<ids.length; i++) {
				this.walker.addToNodeStopList( GraphId.fromString(ids[i]) );
			}
		}
		public WalkerParams() {;}
	}

	// bundles together other controls
	public static class QueryOutputParams
	{
		int topNodes=50, topEdges=50;
		public int getNumTopNodes() { return topNodes; }
		public void setNumTopNodes(int n) { topNodes=n; }
		//public int getNumTopEdges() { return topEdges; }
		//public void setNumTopEdges(int n) { topEdges=n; }
	}

	private class QueryControls extends ViewerControls
	{
		WalkerParams walkerControls;
		QueryOutputParams outputParams;
		JTextField queryField,filterField;
		JPanel outputPanel,mainPanel;

		public void setStopList(String s) {
			walkerControls.setStopList(s);
		}

		// put this on top of the pane 
		public int preferredLocation() { return ViewerControls.TOP; }

		private void statusMessage(String s) 
		{
			ControlledWeightedGraphViewer v = (ControlledWeightedGraphViewer)getControlledViewer();
			v.forwardMessage(s);
			System.err.println("QueryGUIControls: "+s);
		}

		// layout
		public void initialize()
		{
			mainPanel = new JPanel();
			mainPanel.setLayout(new GridBagLayout());

			int x=0,y=0,maxX=3;
			GridBagConstraints agbc;

			walkerControls = new WalkerParams();
			Class[] allowed = new Class[]{WalkerParams.class,
					BasicWalker.class};
			TypeSelector ts1 = 
				new TypeSelector( allowed, walkerControls.getClass() );
			ts1.setContent( walkerControls );
			agbc = gbc(0,y++,false); agbc.gridwidth = maxX;
			mainPanel.add( ts1, agbc );

			outputParams = new QueryOutputParams();
			TypeSelector ts2 = new TypeSelector(new Class[]{outputParams.getClass()},
					outputParams.getClass());
			ts2.setContent( outputParams );
			agbc = gbc(0,y++,false); agbc.gridwidth = maxX;
			mainPanel.add( ts2, agbc );

			x=0; // start next row
			mainPanel.add( new JLabel("Query:"), gbc(x++,y,false) );
			queryField = new JTextField(40);
			// kludge to pass in 'this' to action below
			final ViewerControls theseViewerControls = this; 
			AbstractAction queryAction = new AbstractAction("Go") {
				public void actionPerformed(ActionEvent e) { 
					getControlledViewer().applyControls(theseViewerControls);
				}				
			};
			mainPanel.add( queryField, gbc(x++,y,false) );	    
			queryField.addActionListener( queryAction ); // re-run query on newline here
			mainPanel.add( new JButton(queryAction), gbc(x++,y,false) );
			x=0; y++; // start next row

			filterField = new JTextField(40);
			filterField.addActionListener( queryAction ); // re-run query on newline here
			mainPanel.add( new JLabel("Filter:"), gbc(x++,y,false) );
			mainPanel.add( filterField, gbc(x++,y,false) );
			mainPanel.add( new JLabel("Examples: isa=person, _inFile=*, _hasTerm=*"), gbc(x++,y,false) );
			x=0; y++;

			// progress counters
			JProgressBar progressBar1 = new JProgressBar();
			JProgressBar progressBar2 = new JProgressBar();
			ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar1, progressBar2});
			agbc = gbc(0,y++,false); agbc.gridwidth = maxX;
			mainPanel.add( progressBar1, agbc );
			agbc = gbc(0,y++,false); agbc.gridwidth = maxX;
			mainPanel.add( progressBar2, agbc );

			add(mainPanel);
		}
		private GridBagConstraints gbc(int x,int y,boolean fill)
		{
			GridBagConstraints result = fillerGBC(); result.gridx = x; result.gridy = y;
			result.weightx = result.weighty = 0;
			if (fill) { result.weightx++; result.weighty++; }
			return result;
		}

		public WeightedTextGraph doQuery()
		{
			String searchString = queryField.getText();
			if (searchString.length()==0) {
				statusMessage("null search string");
				return null;
			}
			statusMessage("searching for '"+searchString+"'....");
			Distribution initDist = graph.asQueryDistribution(searchString);
			walkerControls.walker.setGraph(graph);

			//System.out.println("initDist="+initDist);
			if (initDist==null || initDist.size()==0) {
				statusMessage("nothing found for '"+searchString+"'");
				return null;
			} 
			walkerControls.walker.setInitialDistribution( initDist );
			//walkerControls.walker.setUniformEdgeWeights();
			walkerControls.walker.reset();
			long startTime = System.currentTimeMillis();
			WalkerThread thread = new WalkerThread(walkerControls);
			try {
				statusMessage("walking...");
				thread.start();
				thread.join();
			} catch (InterruptedException ex) {
				ex.printStackTrace();
				statusMessage("interrupted");
			}
			Distribution result = thread.getResult();
			if (result==null) {
				statusMessage("walker failed?");
				return null;
			}

			Distribution filteredNodes = 
				filterNodes( graph, walkerControls.walker.getNodeSample(), filterField.getText() );
			if (filteredNodes.size()==0) {
				statusMessage("search '"+searchString+"' finds nothing in "
						+(System.currentTimeMillis()-startTime)/1000.0+" sec");
				return null;
			}
			//System.out.println("before filtering, "+filteredNodes.size()+" nodes");
			filteredNodes = filteredNodes.copyTopN( outputParams.topNodes );
			WeightedTextGraph output = new WeightedTextGraph( initDist, filteredNodes, graph );
			statusMessage("search '"+searchString+"' complete in "
					+(System.currentTimeMillis()-startTime)/1000.0+" sec");
			return output;
		}

		private class WalkerThread extends Thread {
			Distribution result = null;  
			WalkerParams wp;
			public WalkerThread(WalkerParams wp) { this.wp=wp; }
			public void run() { wp.walker.setNumSteps(wp.tot); wp.walker.walk(); }
			public Distribution getResult() { return wp.walker.getNodeSample(); }
		}

		// remove nodes that don't match the 'filter string'
		private Distribution filterNodes(Graph graph, Distribution nodeDist, String filterString)
		{
			filterString = filterString.trim();
			if (filterString.length()==0) return nodeDist;
			NodeFilter filter = null;
			try { 
				filter = new NodeFilter(filterString);
			} catch (Exception ex) {
				statusMessage("the filter '"+filterString+"' is invalid: "+ex);
				return nodeDist;
			}
			return filter.filter(graph,nodeDist);
		}
	}

	public static void main(String[] args)
	{
		if (args.length==0) {
			System.out.println("usage: GRAPH ANNOTATOR1 ...");
			System.exit(0);
		}
		Graph graph = CommandLineUtil.makeGraph(args[0]);
		for (int i=1; i<args.length; i++) {
			graph = CommandLineUtil.annotateGraph(graph,args[i]);
		}
		QueryGUI gui = new QueryGUI(graph);
		new ViewerFrame("QueryGUI", gui );
	}
}


