package ghirl.test.verify;


import java.util.Iterator;
import java.util.Random;

import ghirl.graph.GraphId;
import ghirl.util.CompactImmutableArrayDistribution;
import ghirl.util.CompactImmutableDistribution;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestCompactImmutableDistribution {
	CompactImmutableDistribution cid;
	GraphId[] alphaGraph;
	int[] vowels = {1,5,9,15,21};
	float[] weights = {1,2,4,8,16};
	float[] accumWeights = new float[weights.length];
	@Before
	public void setUp() throws Exception {
		String alpha = "0ABCDEFGHIJKLMNOPQRSTUVWXYZ";
		alphaGraph = new GraphId[26];
		for (int i=0; i<alphaGraph.length; i++) {
			alphaGraph[i] = GraphId.fromString("$"+alpha.charAt(i));
		}
		accumWeights[0] = weights[0];
		for (int i=1; i<weights.length; i++) accumWeights[i] = accumWeights[i-1]+weights[i];
		cid = new CompactImmutableArrayDistribution(vowels,accumWeights,alphaGraph);
	}
	
	@Test
	public void testGetWeight() {
		assertEquals("Object not in distribution should have 0 weight",
				0,cid.getWeight(alphaGraph[2]),0.1);
		for (int i=0; i<vowels.length; i++) {
			assertEquals(alphaGraph[vowels[i]].toString(),
					weights[i],cid.getWeight(alphaGraph[vowels[i]]),0.1);
		}
	}
	
	@Test
	public void testSample() {
		for (int i=0; i<weights.length; i++) {
			Random r = new TestingRandom(i);
			assertEquals(alphaGraph[vowels[i]].toString()+" with random "+r.nextDouble(),
					alphaGraph[vowels[i]], cid.sample(r));
		}
	}
	
	@Test
	public void testIterator() {
		Iterator it=cid.iterator();
		for (int i=0; i<weights.length; i++) {
			assertEquals(alphaGraph[vowels[i]],it.next());
		}
	}
	
	public class TestingRandom extends Random {
		public TestingRandom(int i) { this.i=i; }
		private int i;
		public double nextDouble() {
			return (double) accumWeights[i]/31;
		}
	}
}
