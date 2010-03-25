package ghirl.test;

import static org.junit.Assert.assertTrue;
import ghirl.graph.Graph;
import ghirl.graph.WeightedTextGraph;
import ghirl.graph.WeightedWalker;
import ghirl.util.Distribution;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class GoldStandard {
	private static final String QUERY = "william";

	public String getGoldStandard() throws IOException {
		StringBuilder b = new StringBuilder();
		BufferedReader in = new BufferedReader(new FileReader("tests/output-goldstandard.txt"));
		String read;
		while ( (read=in.readLine()) != null) {
			b.append("\n");
			b.append(read);
		}
		return b.substring(1);
	}
	
	public Distribution queryGraph(Graph graph) {
		WeightedWalker walker = new WeightedWalker();
		Distribution dist = graph.asQueryDistribution(QUERY);
		assertTrue(dist != null && dist.size() > 0);
		walker.setGraph(graph);
		walker.setInitialDistribution(dist);
		walker.setUniformEdgeWeights();
		walker.reset();
		walker.setNumSteps(100);
		walker.setSamplingPolicy(false);
		walker.setStayWalkVersion(false);
		walker.walk();
		return new WeightedTextGraph(dist, walker.getNodeSample(), graph).getNodeDist();
	}
}
