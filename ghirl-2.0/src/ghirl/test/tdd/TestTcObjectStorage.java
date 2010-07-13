package ghirl.test.tdd;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Iterator;

import ghirl.graph.GraphId;
import ghirl.persistance.TokyoCabinetPersistance;
import ghirl.test.GoldStandard;
import ghirl.util.Distribution;
import ghirl.util.TreeDistribution;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import tokyocabinet.BDB;
import tokyocabinet.Util;


public class TestTcObjectStorage {
	private static final String TEST_DIRECTORY= "tests/TestTcObjectStorage";
	private Distribution makeDummyDistribution() {
		Distribution d = new TreeDistribution();
		d.add(1, GraphId.fromString("$foo"));
		d.add(2, GraphId.fromString("$bar"));
		d.add(4, GraphId.fromString("$baz"));
		return d;
	}
	@Before
	public void setup() {
		File dbdir = new File(TEST_DIRECTORY);
		if (!dbdir.exists()) dbdir.mkdir();
	}
	@Test
	public void test() throws IOException {
		TokyoCabinetPersistance tc = new TokyoCabinetPersistance();
		tc.mode = 'w'; int imode = tc.MODES.get(tc.mode);
		BDB foo = tc.initDB(TEST_DIRECTORY+"/db", imode);
		byte[] key = "d".getBytes();
		try {
			ByteArrayOutputStream baos =new ByteArrayOutputStream(); 
			ObjectOutputStream oos = new ObjectOutputStream(baos);
			oos.writeObject(makeDummyDistribution());
			foo.put(key, baos.toByteArray());
			
			ObjectInputStream ios = 
				new ObjectInputStream(
						new ByteArrayInputStream(foo.get(key)));
			Distribution f = (Distribution) ios.readObject();
			assertEquals(3,f.size());
			assertEquals(7,f.getTotalWeight(),0.1);
			for (Iterator it = makeDummyDistribution().iterator(); it.hasNext(); ) {
				GraphId node = (GraphId) it.next();
				assertNotNull(node.toString()+" must be in retrieved distribution!", f.remove(node));
			}
		} catch(Exception e) {
			foo.close();
		}
	}
	
	@Test
	public void testBytes() throws IOException {
		System.out.println(Double.SIZE);
		System.out.println(Float.SIZE);
		System.out.println(Integer.SIZE);
		System.out.println(Byte.SIZE);
		
		float flot = 0.578525641025641f;// (float) Math.random();//*Float.MAX_VALUE;
		System.out.println("Testing "+flot);
		byte[] bytes;
		float reflot;
		
		double doub = (double) flot;
		long ustart = System.currentTimeMillis();
		bytes = Util.packdouble(doub);
		long uend = System.currentTimeMillis();
		double redoub = Util.unpackdouble(bytes);
		reflot = (float) redoub;
		assertEquals(flot,reflot,GoldStandard.TOLERANCE);
		
		long start = System.currentTimeMillis();
		int bits = Float.floatToIntBits(flot);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		for (int i=0; i<(Integer.SIZE/Byte.SIZE); i++) {
			int shift = Byte.SIZE*i;
			System.out.println("Shifting "+shift+" bits");
			baos.write(bits >> shift);
		}
		baos.close();
		bytes = baos.toByteArray();
		long end = System.currentTimeMillis();
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		int rebits = 0; int r;
		for (int i=0; i<(Integer.SIZE/Byte.SIZE) && ((r=bais.read()) != -1); i++) {
			int shift = Byte.SIZE*i;
			System.out.println("Shifting "+shift+" bits");
			rebits = rebits |  (r << shift);
		}
		bais.close();
		reflot = Float.intBitsToFloat(rebits);
		assertEquals(bits, rebits);
		assertEquals(flot, reflot, 0.1);
		
		
		System.out.println("By hand: "+(end-start)+"\nBy util: "+(uend-ustart));
	}

}
