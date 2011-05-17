package ghirl.test.tdd;

import ghirl.util.GhirlToCompact.RandomAccessWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Writer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestRandomAccessFiles {
	RandomAccessFile raf;
	Writer rowwriter;
	@Before
	public void setUp() throws Exception {
		raf = new RandomAccessFile(new File("tests/testRandomAccessFile"), "rw");
		raf.setLength(raf.getFilePointer());
		rowwriter = new RandomAccessWriter(raf);
	}
	
	@Test
	public void test() throws IOException {
		long sizepointer=-1, endpointer=-1;
		String nformat = "% "+(int) Math.ceil(Math.log10(12345))+"d";
		assertEquals("% 5d",nformat);
		
		rowwriter.write(String.format("node link "));
		sizepointer=raf.getFilePointer();
		assertEquals(10,sizepointer);
		rowwriter.write(String.format(nformat+" ",0));
		rowwriter.write("dest1, dest2, dest3\n");
		endpointer=raf.getFilePointer();
		assertEquals(26+sizepointer,endpointer);
		raf.seek(sizepointer-1);
		rowwriter.write(String.format(nformat,12345));
		raf.seek(endpointer);
		rowwriter.write("done");
		
		BufferedReader reader = new BufferedReader(new FileReader("tests/testRandomAccessFile"));
		String line1 = reader.readLine();
		assertEquals("node link 12345 dest1, dest2, dest3",line1);
		String line2 = reader.readLine();
		assertEquals("done",line2);
		reader.close();
	}
	
	@After
	public void shutdown() throws IOException {
		rowwriter.close();
	}

}
