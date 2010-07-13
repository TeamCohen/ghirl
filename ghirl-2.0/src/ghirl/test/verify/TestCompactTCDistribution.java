package ghirl.test.verify;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import ghirl.test.GoldStandard;
import ghirl.util.CompactTCDistribution;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import tokyocabinet.BDB;

public class TestCompactTCDistribution {

	TestableCompactTCDistribution ctd;
	@Before
	public void setUp() throws Exception {
		int [] oi = {1,2};
		float [] tw = {1,3};
		ctd = new TestableCompactTCDistribution(oi,tw,null,false);
	}
	@Test
	public void testSerialization() throws IOException {
		int i = (int) Math.random()*1000;
		float f = (float) Math.random();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ctd.serializeInt(i, baos, 4);
		ctd.serializeInt(Float.floatToRawIntBits(f), baos, 4);
		baos.close();
		ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
		int rei = ctd.deserializeInt(bais, 4);
		int refbits = ctd.deserializeInt(bais, 4);
		float ref = Float.intBitsToFloat(refbits);
		assertEquals("int",i,rei);
		assertEquals("float",f,ref,GoldStandard.TOLERANCE);
	}

	
	public class TestableCompactTCDistribution extends CompactTCDistribution {
		public void serializeInt(int allbits, ByteArrayOutputStream baos, int bytesPerInt) {
			super.serializeInt(allbits, baos, bytesPerInt);
		}
		public int deserializeInt(ByteArrayInputStream bais, int bytesPerInt) {
			return super.deserializeInt(bais, bytesPerInt);
		}
		public TestableCompactTCDistribution(int[] objectIndex, float[] totalWeightSoFar, BDB graphIds, boolean ordered) {
			super(objectIndex,totalWeightSoFar,graphIds,ordered);
		}
		
	}
}
