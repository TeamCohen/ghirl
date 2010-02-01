package ghirl.graph;

import ghirl.util.*;
import edu.cmu.minorthird.util.gui.*;
import edu.cmu.minorthird.util.StringUtil;
import edu.cmu.minorthird.util.ProgressCounter;

import java.util.*;
import java.io.*;
import javax.swing.*;

import javax.swing.event.*;

import javax.swing.text.*;

import javax.swing.border.*;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.awt.*;

import java.awt.event.*;



/** Very simple text query interface.
 */



public class TextUI extends MessageViewer
{
	private long startTime = System.currentTimeMillis();
	private long timeOfLastStatusMessage = startTime;
	private java.text.DecimalFormat timeFmt = new java.text.DecimalFormat("0.0");
	private Graph graph;
	private Walker walker = new WeightedWalker();
	private String weightsInit = "u"; // can be changed via command line to 'r' for random
	private int totalSteps = 100; // initialized through args or the selected walker
	private boolean sampleWalk = false;
	private boolean stayWalkVersion = false; // an experimental type of walk

	public TextUI(String file)
	{
		this(new CachingGraph(new TextGraph(file)));
	}

	public TextUI(Graph graph)
	{
		this.graph = graph;
	}


	public WeightedTextGraph doQuery(String searchString)
	{
		if (searchString.length()==0) {
			statusMessage("null search string");
			return null;
		}
		statusMessage("converting '"+searchString+"' to initDist...");
		Distribution initDist = graph.asQueryDistribution(searchString);
		statusMessage("converted");
		if (initDist==null || initDist.size()==0) {
			statusMessage("nothing found for '"+searchString+"'");
			return null;
		}
		statusMessage("preparing walker...");
		walker.setGraph(graph);
		walker.setInitialDistribution( initDist );
		if (weightsInit.equals("r")) { walker.setRandomEdgeWeights(); }
		else walker.setUniformEdgeWeights();
		walker.reset();
		if (sampleWalk) statusMessage("walking "+totalSteps+" steps...");
		else statusMessage("executing an exhaustive walk...");
		long startTime = System.currentTimeMillis();
		walker.setNumSteps(totalSteps);
		walker.setSamplingPolicy(sampleWalk);
		walker.setStayWalkVersion(stayWalkVersion);
		if (stayWalkVersion) statusMessage("Performing an experimental STAY walk version");
		walker.walk();
		statusMessage("returning WeightedTextGraph...");
		return new WeightedTextGraph(initDist,walker.getNodeSample(),graph);
	}


	public void statusMessage(String msg)
	{
		long now = System.currentTimeMillis();
		double elapsed = (now-timeOfLastStatusMessage)/1000.0;
		double cumulative = (now-startTime)/1000.0;
		System.out.println("["+timeFmt.format(cumulative)+"/"+timeFmt.format(elapsed)+"sec] "+msg);
		timeOfLastStatusMessage = now;
	}



	public void dump()
	{
		statusMessage("iterating over graph nodes");
		for (Iterator i=graph.getNodeIterator(); i.hasNext(); ) {
			GraphId id = (GraphId)i.next();
			statusMessage(id+": "+graph.walk1(id));
		}
	}


	public void setWalker(String s){
		try{
			walker = (Walker)BshUtil.toObject(s,Walker.class);
		}catch(Exception e){
			System.out.println("WALKER SET TO BasicWalker, OTHERWISE SPECIFY e.g. \"new ghirl.graph.WeightedWalker()\"");
		}

	}


	public static void main(String[] args)
	{
		// log4j setup if we don't have a log4j.properties
		if (!Logger.getRootLogger().getAllAppenders().hasMoreElements()) {
			System.err.println("Configuring a default Log4J system...");
			BasicConfigurator.configure(); // go to the console
			Logger.getRootLogger().setLevel(Level.INFO); // only print warnings or worse
		}
		// end log4j setup

		TextUI gui = null;
		String query = null;
		NodeFilter filter = null;
		int numTop = 20, numRepeat = 1;
		int argp = 0;
		int steps = -1;
		boolean reportSize = false;
		while (argp<args.length) {
			if ("-dump".equals(args[argp])) {
				gui.dump();
			} else if ("-graph".equals(args[argp])) {
				//System.err.println("creating TextUI object...");
				Graph g = CommandLineUtil.makeGraph(args[++argp]);
				gui = new TextUI(g);
				//System.err.println("created.");
			} else if ("-steps".equals(args[argp])) {
				if (gui!=null) gui.totalSteps = StringUtil.atoi(args[++argp]);
				else throw new IllegalArgumentException("-db INDEX must precede other options");
			} else if ("-levels".equals(args[argp])) {
				if (gui!=null) gui.walker.setNumLevels(StringUtil.atoi(args[++argp]));
				else throw new IllegalArgumentException("-db INDEX must precede other options");
			} else if ("-top".equals(args[argp])) {
				numTop = StringUtil.atoi(args[++argp]);
			} else if ("-walker".equals(args[argp])) {
				if (gui!=null) gui.setWalker(args[++argp]);
			} else if ("-sample".equals(args[argp])) {
				if (gui!=null) gui.sampleWalk = true;
			} else if ("-stay".equals(args[argp])) {
				if (gui!=null) gui.stayWalkVersion = true;
			} else if ("-repeat".equals(args[argp])) {
				numRepeat = StringUtil.atoi(args[++argp]);
			} else if ("-weights".equals(args[argp])) {
				if (gui!=null) {
					String val = args[++argp];
					if (val.equals("r")) gui.weightsInit="r";
					else if (!val.equals("u"))
						throw new IllegalArgumentException("-weights takes values of 'u' (uniform) or 'r' (random)");
				}
			} else if ("-query".equals(args[argp])) {
				query = args[++argp];
			} else if ("-reportSize".equals(args[argp])) {
				reportSize = true;
			} else if ("-filter".equals(args[argp])) {
				filter = new NodeFilter(args[++argp]);
			} else if ("-edgeStopList".equals(args[argp])) {
				String[] ids = args[++argp].split("\\s*,\\s*");
				for (int i=0; i<ids.length; i++) {
					gui.walker.addToEdgeStopList(ids[i]);
				}
			} else if ("-nodeStopList".equals(args[argp])) {
				String[] ids = args[++argp].split("\\s*,\\s*");
				for (int i=0; i<ids.length; i++) {
					gui.walker.addToNodeStopList( GraphId.fromString(ids[i]) );
				}
			} else {
				System.out.println("unknown option '"+args[argp]+"'");
			}
			argp++;
		}

		if (gui==null) {
			System.out.println("usage: -graph INDEX [-dump] [-steps N] [-top K] [-repeat N] [-filter F] [-weights W] [-sample] -query Q");
			System.out.println(" -dump: dumps entire index, a very large output!");
			System.out.println(" -repeat N: runs the query N times, for performance tests");
			System.out.println(" -weights W: sets the graph edge weights to uniform ('u', default) or random ('r')");
			System.out.println(" -filter F: outputs only nodes that pass some filter F");
			System.out.println("\n Note: for multi-node queries (e.g. a&b) use curly brackets (e.g., -query {a,b}");
			System.exit(0);
		}

		if (query==null) {
			System.out.println("usage: warning - no query specified");
			System.exit(0);
		}

		WeightedTextGraph wg;
		long startTime,endTime;
		String timeMsg;
		if (numRepeat==1) {
			startTime = System.currentTimeMillis();
			wg = gui.doQuery(query);
			endTime = System.currentTimeMillis();
			timeMsg = "search '"+query+"' time";
		} else {
			wg = gui.doQuery(query);
			startTime = System.currentTimeMillis();
			for (int i=1; i<numRepeat; i++) {
				wg = gui.doQuery(query);
			}
			endTime = System.currentTimeMillis();
			timeMsg = "search '"+query+"' avg warm-cache time";
		}

		gui.statusMessage(timeMsg+": "+(endTime-startTime)/(numRepeat*1000.0)+" sec");

		if (wg!=null) {
			Distribution tmp = wg.getNodeDist();
			if (filter!=null) tmp = filter.filter(gui.graph,tmp);
			System.out.println( tmp.copyTopN(numTop).format() );
		}

		if (reportSize) {
			System.err.println("Processing graph statistics...");
			int nodeCount = 0;
			int edgeCount = 0;
			for (Iterator i=gui.graph.getNodeIterator(); i.hasNext(); i.next() ) {
				GraphId node = (GraphId)i.next();
				nodeCount++;
				if (nodeCount%1000==0) System.out.println(nodeCount + " " + edgeCount);
				Set edgeLabels = gui.graph.getEdgeLabels(node);

				for (Iterator edgeIt = edgeLabels.iterator(); edgeIt.hasNext();){
					String label = (String)edgeIt.next();
					if (!label.equals("isa"))
						edgeCount += gui.graph.followLink(node,label).size();
				}
			}
			System.out.println("The graph has "+nodeCount+" nodes and "+edgeCount + " edges.");
		}
	}
}
