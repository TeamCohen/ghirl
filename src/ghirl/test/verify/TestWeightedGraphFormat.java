package ghirl.test.verify;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ghirl.graph.BasicWeightedGraph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.util.Distribution;

import org.junit.Test;


public class TestWeightedGraphFormat {
	private static final double EPSILON = 0.0001;

	@Test
	public void test() throws FileNotFoundException, IOException {
		BasicWeightedGraph g = new BasicWeightedGraph();
		GraphLoader loader = new GraphLoader(g);
		loader.load(new File("tests/weighted-graph.txt"));
		
		Distribution einat = g.walk1(GraphId.fromString("$einat"));
		assertEquals(3,einat.size());
		assertTrue(Math.abs(2.6-einat.getTotalWeight()) < EPSILON);
		assertTrue(Math.abs(1.1-einat.getWeight(GraphId.fromString("$person"))) < EPSILON);
		assertTrue(Math.abs(0.5-einat.getWeight(GraphId.fromString("$william"))) < EPSILON);
		
	}
}
