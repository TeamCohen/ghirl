package ghirl.test.verify;

import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;

import ghirl.graph.Graph;
import ghirl.graph.GraphId;
import ghirl.graph.GraphLoader;
import ghirl.graph.MutableGraph;
import ghirl.graph.MutableTextGraph;
import ghirl.graph.NodeFilter;
import ghirl.graph.TextNodeFilter;
import ghirl.util.Distribution;

import org.junit.Test;
import static org.junit.Assert.*;

public class TestTextNodeFilter {
	@Test
	public void test() {
		MutableGraph g = new MutableTextGraph();
		GraphLoader el = new GraphLoader(g);
		// a paper has two features, differentiated by the text in their 'type' field.
		el.loadLine("node TEXT$genetype gene");
		el.loadLine("node TEXT$12345678 gene");
		el.loadLine("node TEXT$prottype protein");
		el.loadLine("edge hastype FOO TEXT$genetype");
		el.loadLine("edge hastype BAR TEXT$12345678");
		el.loadLine("edge hastype pFOO TEXT$prottype");
		el.loadLine("edge hasfeature paper FOO");
		el.loadLine("edge hasfeature paper BAR");
		el.loadLine("edge hasfeature paper pFOO");
		
		// show both features
		Distribution features = g.walk1(GraphId.fromString("paper"),"hasfeature");
		assertEquals(3,features.size());
		
		// filter for the nodes with "gene" as the text content of their type node
		NodeFilter isGene = new TextNodeFilter("hastype=gene");
		Distribution genes = isGene.filter(g, features);
		assertEquals(2,genes.size());
		TreeSet<GraphId> answers = new TreeSet<GraphId>();
		Collections.addAll(answers, GraphId.fromString("FOO"), GraphId.fromString("BAR"));
		for (Iterator it=genes.iterator(); it.hasNext();) {
			GraphId node = (GraphId) it.next();
			assertTrue(node.toString(),answers.remove(node));
		}
		for (GraphId remaining : answers) {
			System.err.println("Missing: "+remaining.toString());
		}
		assertEquals(0,answers.size());
	}
}
