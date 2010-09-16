package ghirl.test.tdd;


import ghirl.util.Config;
import ghirl.util.GhirlToCompact;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.io.Writer;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class TestGhirlToCompact {
	File testdir;
	String sourcetext = "Mary had a little lamb\n" +
			"Its fleece was white as snow\n" +
			"And everywhere that Mary went\n" +
			"Her lamb was sure to go.\n";
	
	@Before
	public void setUp() throws Exception {
		testdir = new File("tests/GhirlToCompact");
		if (!testdir.exists()) testdir.mkdir();
	}

	@Test
	public void test() throws IOException {
		Config.setProperty(GhirlToCompact.PROP_SAFETYFACTOR_OPENFILES, String.valueOf(3)); 
		File size = new File(testdir,"size"),
			 link = new File(testdir,"link"),
			 node = new File(testdir,"node"),
		     row  = new File(testdir,"row"),
		     tmp  = new File(testdir,"tmp"),
	     	 ghirl= new File("tests/graph.txt");
		GhirlToCompact.main(size.getPath(),link.getPath(),node.getPath(),row.getPath(),tmp.getPath(),ghirl.getPath());
		File standardRow = new File("sample-graphs/compact/rows.txt");
		BufferedReader standard = new BufferedReader(new FileReader(standardRow));
		BufferedReader experimental = new BufferedReader(new FileReader(row));
		int i=0;
		Set<String> standardLines = new TreeSet<String>();
		for(String sline; (sline = standard.readLine()) != null;) {
			standardLines.add(sline.replaceAll("\\s+", " "));
		}
		for (String eline; (eline = experimental.readLine()) != null; i++) {
			assertTrue("Too many lines in experimental file", standardLines.size() > 0);
			assertTrue("No record of line "+i+"; remaining lines are: "+getRemainingLines(standardLines),standardLines.remove(eline.replaceAll("\\s+"," ")));
		}
		standard.close();
		experimental.close();
	}
	
	public String getRemainingLines(Set<String> set) {
		StringBuilder sb = new StringBuilder();
		for (String s : set) sb.append(s).append("\n");
		return sb.toString();
	}
	
	@Test
	public void testGZIP() throws FileNotFoundException, IOException {
		File gz = new File(testdir,"test.gz");
		BufferedReader sourceReader = new BufferedReader(new StringReader(sourcetext));
		GZIPOutputStream zipper = new GZIPOutputStream(new FileOutputStream(gz));
		Writer writer = new BufferedWriter(new OutputStreamWriter(zipper));
		for(String line; (line = sourceReader.readLine()) != null;) {
			writer.write(line);
			writer.write("\n");
		}
		writer.flush();
		zipper.finish();
		writer.close();
		
		StringBuilder rebuilder = new StringBuilder();
		BufferedReader fileReader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(gz))));
		for(String line; (line = fileReader.readLine()) != null;) {
			rebuilder.append(line);
			rebuilder.append("\n");
		}
		fileReader.close();
		assertEquals(sourcetext,rebuilder.toString());
	}
}
