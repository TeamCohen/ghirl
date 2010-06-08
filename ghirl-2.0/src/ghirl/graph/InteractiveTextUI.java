package ghirl.graph;

import java.io.Console;
import java.io.IOException;
import java.util.Set;

import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.TextGraph;
import ghirl.util.Config;
import ghirl.util.Distribution;

public class InteractiveTextUI {
	/* Static Methods */
	public static void main(String args[]) {
		String graphName=null;
		for (int i=0; i<args.length; i++) {
			if ("-graph".equals(args[i])) {
				i++;
				if (i>=args.length) { error("-graph must take an argument"); return; }
				graphName = args[i];
			} else { help(); return; }
		}
		if (graphName == null) { help(); return; }
		new InteractiveTextUI().runConsole(graphName);
	}
	private static void error(String erstring) {
		System.err.println(erstring);
	}
	private static void help() {
		System.err.println("Usage:\n"+
				"\t-graph [graphName]");
	}
	
	/* Instance Methods */
	private String makeGraphIdDesc(GraphId destination, Graph graph) {
		String isa = "";
		Set<GraphId> isaSet = graph.followLink(destination, "isa");
		if (isaSet.size() > 0) isa = isaSet.iterator().next().getShortName() + " "; 
		if (destination.getFlavor().equals("TEXT")) {
			return isa
				+ destination.toString() 
				+ " \"" + graph.getTextContent(destination) + "\"";
		}
		return isa + destination.toString();
	}
	public void runConsole(String graphName) {
		System.out.println("Opening "+graphName+" in "+Config.getProperty("ghirl.dbDir")+"...");
		TextGraph graph=null;
		try {
			graph = new TextGraph(graphName);
		} catch (IOException e) {
			System.err.println("Couldn't create graph!  Check your spelling.");
			e.printStackTrace();
			System.exit(0);
		}
		
		Console console = System.console(); String input;
		while ( !(input = console.readLine("> ")).equals("q")) {
			if (input.equals("?")) {
				console.printf("\t?\tThis help screen\n"
						+"\tq\tQuit\n"
						+"\t[node name]\tQuery the node.  If it exists, print outgoing edges and a summary of destination nodes.\n");
			} else {
				Distribution d = graph.asQueryDistribution(input);
				if (d.size() == 0) {
					console.printf("%s not found as a graph node.\n", input);
					continue;
				}
				GraphId node = (GraphId) d.iterator().next();
				Set<String> edges = graph.getEdgeLabels(node);
				for (String edge : edges) {
					Set<GraphId> destinations = graph.followLink(node, edge);
					int ndests = destinations.size();
					if (ndests == 1) {
						GraphId destination = destinations.iterator().next();
						console.printf("%s: %s\n",edge,makeGraphIdDesc(destination, graph));
						continue;
					}
					console.printf("%s: %d nodes\n", edge,ndests);
					int i=0;
					for (GraphId destination : destinations) {
						if (i>=10) {
							console.printf("\t...and %d others\n",ndests-10);
							break;
						}
						console.printf("\t%s\n",makeGraphIdDesc(destination, graph));
						i++;
					}	 
				}
			}
		}
		console.printf("Quitting...\n");
		graph.close();	
	}
}
