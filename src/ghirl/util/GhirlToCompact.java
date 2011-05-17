package ghirl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import edu.cmu.minorthird.util.ProgressCounter;
/** 
 * Executable to convert a regular GHIRL text file:
 * <blockquote>
 * node 
 * @author katie
 *
 */
public class GhirlToCompact {
	public static final String PROP_SAFETYFACTOR_NODEARRAY="ghirl.safetyfactor.nodearray",
		PROP_SAFETYFACTOR_ROWS="ghirl.safetyfactor.rows",
		PROP_SAFETYFACTOR_SORT="ghirl.safetyfactor.sort",
		PROP_SAFETYFACTOR_OPENFILES="ghirl.safetyfactor.openfiles";
	private static final String PROP_PLAINTEXT_TMPFILES = "ghirl.use-plaintext-tmpfiles";
	private static final int DEFAULT_NODEARRAY = 96, 
	                         DEFAULT_ROWS = 1000,
	                         DEFAULT_OPENFILES = 200000;
	private static final double DEFAULT_SORT = 0.3;
	public static final String FILE_EXTENSION_GZIP=".gz";
	public static final FileFilter isGZIP = new FileFilter() {
		public boolean accept(File pathname) {
			return pathname.getName().endsWith(FILE_EXTENSION_GZIP);
		}};
	private static int safetyfactor_nodearray; // ~bytes per node
	private static int safetyfactor_rows; // ~ maximum expected bytes needed per line of tmp
	private static double safetyfactor_sort;
	private static boolean debug_tmpfile;
	private static int SORTBUFFER_SIZE = 500000;

	private static String join(String delim, String ... parts) {
		StringBuilder sb = new StringBuilder();		
		for (int i=0; i< (parts.length-1); i++) {
			sb.append(parts[i]);
			sb.append(delim);
		}
		sb.append(parts[parts.length-1]);
		return sb.toString();
	}
	public static boolean maxMemoryOkayPortion(double safetyfactor) {
		Runtime r = Runtime.getRuntime();
		return (r.freeMemory() + (r.maxMemory()-r.totalMemory())) > r.maxMemory()*safetyfactor;
	}
	public static boolean maxMemoryOkayAbsolute(double ammt) {
		Runtime r = Runtime.getRuntime();
		return (r.freeMemory() + (r.maxMemory()-r.totalMemory())) > ammt;
	}
	public static boolean currentMemoryOkay(double safetyfactor) {
		Runtime r = Runtime.getRuntime();
		return r.freeMemory() > r.totalMemory()*safetyfactor;
	}
	
	/**
	 * @param args
	 */
	public static void main(String ... args) {
		if (args.length < 6) {
			System.err.println("Usage:\n" +
					"sizeFile linkFile nodeFile rowFile tmpFile graphFile1.ghirl [graphFile2.ghirl ...]\n" +
					String.format("You may also place a ghirl.properties file in the classpath to define custom values for the following memory tuning parameters:\n\t"+
							"%s (default %d) average bytes per node name\n\t" +
							"%s (default %d) maximum expected bytes to process one line of the tmp file\n\t" +
							"%s (default %f) when free memory drops below this portion of maximum heap space while sorting, flush the current chunk and start a new one", 
							PROP_SAFETYFACTOR_NODEARRAY,DEFAULT_NODEARRAY,
							PROP_SAFETYFACTOR_ROWS,DEFAULT_ROWS,
							PROP_SAFETYFACTOR_SORT,DEFAULT_SORT));
			System.exit(0);
		}
		
		safetyfactor_nodearray = Integer.parseInt(Config.getProperty(PROP_SAFETYFACTOR_NODEARRAY,
																	 String.valueOf(DEFAULT_NODEARRAY)));
		safetyfactor_rows = Integer.parseInt(Config.getProperty(PROP_SAFETYFACTOR_ROWS,
																String.valueOf(DEFAULT_ROWS)));
		safetyfactor_sort = Double.parseDouble(Config.getProperty(PROP_SAFETYFACTOR_SORT,
																  String.valueOf(DEFAULT_SORT)));
		debug_tmpfile = Boolean.parseBoolean(Config.getProperty(PROP_PLAINTEXT_TMPFILES, "false"));
		/*System.out.println(String.format("Memory tuning preferences:\n\t" +
				"%s = %d (default %d)\n\t" +
				"%s = %d (default %d)\n\t" +
				"%s = %f (default %f)\n\t" +
				PROP_SAFETYFACTOR_NODEARRAY,safetyfactor_nodearray,DEFAULT_NODEARRAY,
				PROP_SAFETYFACTOR_ROWS,safetyfactor_rows,DEFAULT_ROWS,
				PROP_SAFETYFACTOR_SORT,safetyfactor_sort,DEFAULT_SORT));*/
		Runtime runtime = Runtime.getRuntime();
		long free = runtime.freeMemory();
		System.out.println(free+" bytes memory free");
		File size, link, node, row, tmp;
		Writer sizewriter, linkwriter, nodewriter, rowwriter, tmpwriter=null;
		size = new File(args[0]);
		link = new File(args[1]);
		node = new File(args[2]);
		row  = new File(args[3]);
		tmp  = new File(args[4]);
		int tmpLines=0;

		try {
			if (debug_tmpfile) {
				tmpwriter = new BufferedWriter(new FileWriter(tmp));
			} else {
				tmp = new File(tmp.getPath()+FILE_EXTENSION_GZIP);
				tmpwriter = new GzipWriter(tmp);
			}
		} catch (IOException e1) {
			e1.printStackTrace();
			System.exit(0);
		}
		
		DiskSet nodenames  = new DiskSet((int) free/200, new File(node.getName()+"_tmp"));
		TreeSet<String> labelnames = new TreeSet<String>();
		try {
			ProgressCounter progress = new ProgressCounter("load ghirlfile","line");
			// record node and link names,
			// write each edge (and its inverse) to the tmpfile
			//REFACTOR: send the link and node file, too, for
			//preparation of tempfiles that can be merged later
			tmpLines += loadGhirlFiles(args, 5, nodenames, labelnames, tmpwriter, progress);
			tmpwriter.close();
			
			int i;
			
			// this is where we'd need to merge the link files
			System.out.println("done pass 1, sorting links");
			linkwriter = new BufferedWriter(new FileWriter(link));
			i=1;
			for (String label : labelnames){
				linkwriter.write(label+"\n");
			}
			linkwriter.close();
			int nlabels=labelnames.size();
			labelnames = null; // free memory

			// this is where we'd need to merge the node files
			System.out.println("done sorting links, sorting nodes");
			nodenames.write(node);
			int nnodes=nodenames.size(); // we have to write the nodes file to determine the #unique nodes
			// (due to merges of duplicates that only happens during write)
			nodenames = null; // free memory
			
			// write size of the graph to a special file
			sizewriter = new BufferedWriter(new FileWriter(size));
			String sizestr = nlabels+" "+nnodes+"\n";
			sizewriter.write(sizestr);
			sizewriter.close();
			System.out.println("size: "+sizestr);
			
			// determine how many lines of the tmpfile we can handle in memory
			System.gc();
			long left = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
//			System.out.println(left);
			SORTBUFFER_SIZE = (int) left / 200;
			System.out.println("Estimated chunk size: "+SORTBUFFER_SIZE+"\n" +
					"Number of chunks: "+(tmpLines/SORTBUFFER_SIZE));
			
			sortFile(tmp);
			
			System.gc();
			left = runtime.maxMemory() - runtime.totalMemory() + runtime.freeMemory();
			if (!maxMemoryOkayAbsolute(nnodes*safetyfactor_nodearray)) {
				throw new IllegalStateException(left+" is not enough memory for "+nnodes+" nodes. Adjust -Xmx upwards and try again.");
			} else {
				System.out.println(left+" memory available (max) > "+nnodes*safetyfactor_nodearray+" estimated memory required; OK to continue");
			}
			
			// grab the list of nodes wholesale
			System.out.println("allocating node list");
			String[] allnodes = new String[nnodes];
			BufferedReader nodereader = new BufferedReader(new FileReader(node));
			i=0; progress = new ProgressCounter("loading node list", "line");
			for(String line; (line=nodereader.readLine()) != null; i++) {
				progress.progress();
				allnodes[i]=line;
			}
			nodereader.close();
			
			// grab the list of labels wholesale
			String[] alllabels = new String[nlabels];
			BufferedReader linkreader = new BufferedReader(new FileReader(link));
			i=0;
			for(String line; (line=linkreader.readLine()) != null; i++) alllabels[i]=line;
			linkreader.close();
			
			writeRowFile(tmp, row, allnodes, alllabels);
			
			System.out.println("conversion complete.");
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static int loadGhirlFiles(String[] args, int offset,
			DiskSet nodenames, TreeSet<String> labelnames,
			Writer tmpwriter, ProgressCounter progress) throws IOException {

		int tmpLines=0;
		for (int i=offset; i<args.length; i++) {
			File file = new File(args[i]);
			System.out.println("reading "+file.getName());
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for(String line; (line = reader.readLine()) != null;) {
				line = line.trim();
				if (line.startsWith("node")) {
					String nodename = line.split("\\s+")[1];
					nodenames.add(normalizeNodeName(nodename));//,null);
				} else if (line.startsWith("edge")) {
					String[] parts = line.split("\\s+");
					String nn1 = normalizeNodeName(parts[2]);
					String nn2 = normalizeNodeName(parts[3]);
					String label = parts[1];
					String count = "1";
					if (parts[0].indexOf(":") >= 0) count = parts[0].split(":")[1];
					nodenames.add(nn1);//,null);
					nodenames.add(nn2);//,null);
					labelnames.add(label);//,null);
					labelnames.add(label+"Inverse");//,null);
					tmpwriter.write(join(" ",nn1,label,count,nn2)+"\n");
					tmpwriter.write(join(" ",nn2,label+"Inverse",count,nn1)+"\n");
					tmpLines += 2;
				}
				progress.progress();
			}
			reader.close();
		}
		return tmpLines;
	}
	private static void writeRowFile(File tmp, File row, String[] allnodes,
			String[] alllabels) throws IOException {

		RandomAccessFile raf = new RandomAccessFile(row, "rwd"); 
		raf.setLength(raf.getFilePointer());
		long sizepointer=-1, endpointer=-1;
		String nformat = "% "+(int) Math.ceil(Math.log10(allnodes.length))+"d";
		
		String lastSource="", lastLabel="";
		int destCount=0;
		int rowLineCount=0;
		StringBuilder destString=null;
		BufferedReader tmpreader = null;
		try {
			if (isGZIP.accept(tmp)) {
				tmpreader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(tmp))));
			} else {
				tmpreader = new BufferedReader(new FileReader(tmp));
			}
		} catch (EOFException e) {
			throw new IllegalStateException("Couldn't open temp file "+tmp.getPath()+" for input",e);
		}
		Writer rowwriter = new RandomAccessWriter(raf);
		ProgressCounter progress = new ProgressCounter("convert temp to row","line");
		for(String line; (line=tmpreader.readLine()) != null;) {
			line=line.trim(); if (line.isEmpty()) break;
			String[] parts = line.split("\\s+");
			if (parts.length<4) {
				System.out.println("Bad line? \""+line+"\"");
				break;
			}
			String source = parts[0],
				   label=parts[1],
				   count=parts[2],
				   dest=parts[3];
			if(!lastSource.equals(source) || !lastLabel.equals(label)) {
				if(destString != null) { 
					if (sizepointer < 0) {
						rowwriter.write(String.format(nformat+" %s\n",
								destCount,destString.toString()));
					} else {
						rowwriter.write(destString.toString()+"\n");
						endpointer = raf.getFilePointer();
						raf.seek(sizepointer);
						rowwriter.write(String.format(nformat,destCount));
						raf.seek(endpointer);
						sizepointer = endpointer = -1;
					}
				}
				destString = new StringBuilder();
				destCount=0;
				// this next line might be improved -- srcid should occur in sorted order
				int sourceId = Arrays.binarySearch(allnodes, source)+1;
				int linkId = Arrays.binarySearch(alllabels, label)+1;
				rowwriter.write(join(" ",String.valueOf(sourceId),String.valueOf(linkId))+" ");
				rowLineCount++;
			}
			lastSource=source;
			lastLabel=label;
			int destId = Arrays.binarySearch(allnodes,dest)+1;
			destString.append(" "+destId+":"+count);
			destCount++;
			progress.progress();
			
			if (!maxMemoryOkayAbsolute(safetyfactor_rows)) {
				// flush cache
				System.out.println("flushing cache to free memory; row line "+rowLineCount);
				if (sizepointer < 0) {
					sizepointer = raf.getFilePointer();
					rowwriter.write(String.format(nformat,0));	
				}
				rowwriter.write(destString.toString());
				destString = new StringBuilder();
				System.gc();
			}
		}
		tmpreader.close();
		if (destString != null)
			rowwriter.write(" "+destCount+destString.toString()+"\n");
		else
			System.out.println("WARNING: no rows written. Something wrong?");
		rowwriter.close();
	}

	/**
	 * 
	 * @param chunk List of strings in the chunk to be written
	 * @param part ID of the chunk
	 * @param tmpdir Directory where the chunk file should reside
	 * @param tmpFile Temp file that the chunk came out of (useful for detecting zip state)
	 * @throws IOException
	 */
	private static void writeChunk(ArrayList<String> chunk, int part, File tmpdir, File tmpFile) throws IOException {
		Collections.sort(chunk);
		if (!tmpdir.exists()) tmpdir.mkdir();
		File chunkfile = new File(tmpdir,String.format("%05d", part));
		Writer writer = null;
		if (isGZIP.accept(tmpFile)) {
			chunkfile = new File(chunkfile.getPath()+FILE_EXTENSION_GZIP);
			writer = new GzipWriter(chunkfile);
		} else {
			writer = new FileWriter(chunkfile);
		}
		for (String chunkline : chunk) writer.write(chunkline+"\n");
		writer.close();
		chunk.clear();
		System.gc();
	}
	
	/**
	 * Sorts the specified file, chunking and merging if necessary.
	 * @param tmp the file to sort; replaced by the sorted version. If the file is
	 * too big to sort in memory, chunk files will be written to a directly {tmp.getPath()}+"_parts".
	 * @throws IOException
	 */
	private static void sortFile(File tmp) throws IOException {
		
		// first read the file in big chunks, sorting each chunk and then writing to a file.
		BufferedReader reader = null;
		if (isGZIP.accept(tmp)) {
			reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(tmp))));
		} else {
			reader = new BufferedReader(new FileReader(tmp));
		}
		int nlines=0,part=0;
		File tmpdir = new File(tmp.getPath()+"_parts");
		if(tmpdir.exists()) FilesystemUtil.rm_r(tmpdir);
		ArrayList<String> chunk = new ArrayList<String>(SORTBUFFER_SIZE);
		try {
			for (String line; (line = reader.readLine()) != null; nlines++) {
				if (nlines > SORTBUFFER_SIZE || !maxMemoryOkayPortion(safetyfactor_sort)) {
					System.out.println("chunk "+(part+1)+": "+nlines+" lines");
					writeChunk(chunk, part, tmpdir, tmp);
					part++; nlines=0;
				}
				chunk.add(line);
			}
		}catch(OutOfMemoryError e) {
			System.out.println(Runtime.getRuntime().freeMemory());
			System.out.println(Runtime.getRuntime().maxMemory());
			System.out.println(Runtime.getRuntime().totalMemory());
			throw (e);
		}
		System.out.println("chunk "+(part+1)+": "+nlines+" lines");
		reader.close();
		// nuke the original tmp file
		FilesystemUtil.rm_r(tmp);
		
		// if we only ever used one chunk, write it to the final temp file
		if (part == 0) {
			Collections.sort(chunk);
			Writer writer = null;
			if (isGZIP.accept(tmp)) {
				writer = new GzipWriter(tmp);
			} else {
				writer = new FileWriter(tmp);
			}
			for (String chunkline : chunk) writer.write(chunkline+"\n");
			writer.close();
			chunk.clear();
			return;
		}
		
		// otherwise we have to first write the most recent chunk,
		writeChunk(chunk, part, tmpdir, tmp);
		
		// and then merge all the parts back together
		mergeFiles(tmpdir, tmp);
		System.out.println("sortFile completed");
	}

	
	private static int mergeFiles(File tmpdir, File tmp) throws IOException {
		int maxNumOpenFiles = Integer.parseInt(Config.getProperty(PROP_SAFETYFACTOR_OPENFILES,
				String.valueOf(DEFAULT_OPENFILES)));
		System.out.println("tmpdir :"+tmpdir.getPath().toString());
		File[] tmpfilearray = tmpdir.listFiles();
		
		// case 1: #tempfiles < max # open files
		System.out.println("merging "+tmpfilearray.length+" files.");
		if (tmpfilearray.length <= maxNumOpenFiles) {
			return mergeFileSubset(tmpfilearray,0,tmpfilearray.length,tmp);
		}
		
		// case 2: two-phase merge
		File[] mergedfilearray = new File[(int)Math.ceil(tmpfilearray.length/maxNumOpenFiles + 0.5)];
		System.out.println("using "+mergedfilearray.length+" intermediate files");
		
		int numFilesProcessed = 0;
		int index = 0;
		while (numFilesProcessed < tmpfilearray.length) {
			/*
			 * Big change here: Instead of copying around a big array of files, 
			 * we'll just keep track of the start point (numFilesProcessed)
			 * and the number of files to process this round (maxFilesOpen or 
			 * to the end of the file array, whichever comes first).
			 * Since Java is pass-by-reference, this should reduce complexity,
			 * be faster, and take up less space. 
			 */
			int length = Math.min( maxNumOpenFiles,
						           tmpfilearray.length - numFilesProcessed); 
			File intermFile = new File(new String("interm_temp"+index));
			
			System.out.println("merging "+length+" files into "+intermFile.getName());
			mergeFileSubset(tmpfilearray, numFilesProcessed, length, intermFile);
			
			mergedfilearray[index] = intermFile;
			index++;
			numFilesProcessed += length;
		}
		System.out.println("FINAL mergeFileSubset(intermfilearray, intermFile) called : " + mergedfilearray.length);
		return mergeFileSubset(mergedfilearray,0,mergedfilearray.length,tmp);
	}
	
	private static int mergeFileSubset(File[] tmpfilearray, int start, int length, File tmpFile) throws IOException {
		
		TreeMap<String,BufferedReader> tmpfileReaders = new TreeMap<String,BufferedReader>();
		
		// fill the collection of readers
		int nmerged=0,nout=0;
		for(File f:tmpfilearray) {
			BufferedReader reader = null;
			if (isGZIP.accept(f)) {
				reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(f))));
			} else {
				reader = new BufferedReader(new FileReader(f));
			}
			// increment this reader to the first line we haven't seen yet,
			// and add the reader to our collection
			nmerged += putNextUnusedLine(tmpfileReaders, reader);
		}
		System.out.println("Using "+tmpfileReaders.size()+" of "+tmpfilearray.length+" files.");
		
		// now write the lowest string to the sortfile, and increment that reader
		// drop readers that become empty
		// stop when we have no more readers
		Writer writer = null;
		if (isGZIP.accept(tmpFile)) {
			writer = new GzipWriter(tmpFile);
		} else {
			writer = new FileWriter(tmpFile);
		}
		Entry<String,BufferedReader> entry;
		while ( (entry = tmpfileReaders.pollFirstEntry()) != null) {
			// note that pollFirstEntry is like pop() -- it removes as well as returns
			writer.write(entry.getKey()+"\n");
			nout++;
			nmerged += putNextUnusedLine(tmpfileReaders, entry.getValue());
		}
		writer.close();
		System.out.println(nmerged+" lines merged in "+tmpfilearray.length+" files.\nLines in final file: "+nout);
		return nout;
	}

	private static int putNextUnusedLine(Map<String,BufferedReader> readers, BufferedReader reader) throws IOException {
		int nlines=0;
		// increment this reader until we find a line we haven't seen yet,
		// then add the reader to the collection.
		// close the reader if it runs out of lines before we find one.
		for (boolean done=false; !done;) {
			String line = reader.readLine(); 
			if (line != null) { nlines++;
				if (!readers.containsKey(line)) {
					readers.put(line, reader);
					done=true;
				}
			} else {
				System.out.println("Closing exhausted file.");
				reader.close(); // clean up the dead reader
				done=true;
			}
		}
		return nlines;
	}

	private static String normalizeNodeName(String nodename) {
		if (nodename.contains("$")) return nodename;
		return "$"+nodename;
	}

	/** 
	 * Encapsulates a GZIPOutputStream in a Writer-type object. Hides
	 * the additional GZIP-related tasks during close(), to make a GZIP writer
	 * behave like any other writer.
	 * @author katie
	 *
	 */
	public static class GzipWriter extends Writer {
		GZIPOutputStream zipper;
		BufferedWriter writer;
		public GzipWriter(File f) throws FileNotFoundException, IOException {
			zipper = new GZIPOutputStream(new FileOutputStream(f));
			writer = new BufferedWriter(new OutputStreamWriter(zipper));
		}
		@Override
		public void close() throws IOException {
			writer.flush();
			zipper.finish();
			writer.close();
		}

		@Override
		public void flush() throws IOException { writer.flush(); }

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {}
		
		@Override
		public void write(String s) throws IOException {
			writer.write(s);
		}
	}

	public static class RandomAccessWriter extends Writer {
		RandomAccessFile raf;
		public RandomAccessWriter(RandomAccessFile f) {
			raf=f;
		}
		@Override
		public void close() throws IOException {
			raf.close();
		}

		@Override
		public void flush() throws IOException {}

		@Override
		public void write(char[] cbuf, int off, int len) throws IOException {}
		
		@Override
		public void write(String s) throws IOException {
			raf.writeBytes(s);
		}
	}

	public static class DiskSet {
		private int maxsize;
		private TreeSet<String> internalSet;
		private File tmpDir;
		private int internalSetSize, totalSetSize;
		private int tmpFileNo;
		public DiskSet() {
			this(Integer.MAX_VALUE,null);
		}
		public DiskSet(int maxsize, File tmpdir) {
			this.maxsize=maxsize;
			this.internalSet=new TreeSet<String>();
			this.internalSetSize=0;
			this.tmpFileNo=0;
			this.tmpDir=tmpdir;
			if(tmpdir.exists()) FilesystemUtil.rm_r(tmpdir);
		}
		public int size() { return totalSetSize; }
		public void add(String s) throws IOException {
			internalSet.add(s);
			totalSetSize++;
			if (++internalSetSize > maxsize) flush();
		}
		private void flush() throws IOException {
			if (!tmpDir.exists()) tmpDir.mkdir();
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					new File(tmpDir,String.format("%05d",tmpFileNo++))));
			for (String s : internalSet) {
				writer.write(s);
				writer.write("\n");
			}
			writer.close();
			internalSet = new TreeSet<String>();
			internalSetSize=0;
		}
		public void write(File file) throws IOException {
			this.flush();
			int N=mergeFiles(this.tmpDir, file);
			if (N>=0) this.totalSetSize = N;
		}
	}
}
