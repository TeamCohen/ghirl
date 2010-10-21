/**
 * 
 */
package ghirl.PRA.util;

import ghirl.PRA.util.Interfaces.IRead;
import ghirl.PRA.util.TVector.VectorS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;


/**
 * @author nlao
 *
 */
public class FFile {
	public static String getFilePath(String path){
		int i=path.lastIndexOf("/");
		return path.substring(0, i);
	}
	public static boolean mkdirsTrimFile(String path){		
		return mkdirs(getFilePath(path));
	}
	
  public static void renameFile(String file, String toFile) {
    File f1 = new File(file);
    if (!f1.exists() || f1.isDirectory()) {
        System.out.println("File does not exist: " + file);
        return;
    }
    File newFile = new File(toFile);
    if (!f1.renameTo(newFile)) 
        System.out.println("Error renmaing file="+file);
  }
  
	public static synchronized boolean mkdirs(String path){
		if (FFile.exist(path))
			return false;
		
		boolean b= new File(path).mkdirs();
		if (!b){
			b= new File(path).mkdirs();
			
			/*for(int i=0; (i=path.indexOf('/',i))>=0; ++i){
				String fd= path.substring(0,i);
				if (FFile.exist(fd))
					continue;
				//b=new File(fd).mkdir();
				//if (!b)	break;//not working
				FSystem.cmd("mkdir "+fd);
				if (FFile.exist(fd))
					continue;				
			}*/
			if (!b){
				System.err.print("failed to create dir="+path);
				System.exit(-1);
			}
		}
		return b;
	}
	public static int readReminder=100000;
	public static int writeReminder=100000;
	public static int readCount=0;
	public static int writeCount=0;
	//public static int reminder=-1;
	//public static int count=0;
	
	//should mostly called by... never appear...... 
	// should not be called directly by containers
	public static String readLine(BufferedReader br){
		++readCount;
		if (readReminder>0)
			if (readCount % readReminder==0)
				System.out.print("r");
		try{
			return br.readLine();
	  } catch ( Exception e ) {
			e.printStackTrace();
			return null;
		} 
	}	
	
	public static void writeln(BufferedWriter bw, String str){
		write(bw,str);
		write(bw,"\n");
	}
	public static void write(BufferedWriter bw, String str){
		if (bw==null) return;
		++writeCount;
		if (writeReminder>0)
			if (writeCount % writeReminder==0)
				System.out.print("w");
		try{
			bw.write(str);
	  } catch ( Exception e ) {
			e.printStackTrace();
			System.exit(-1);
		} 
	}		
	public static void write(BufferedWriter bw,String format, Object ... args){
		write(bw, String.format(format, args));//(Object[])
	}	
	public static void close(BufferedReader br){
		try{
			br.close();
	  } catch ( Exception e ) {
			e.printStackTrace();
			return;
		} 
	}	
	public static void close(BufferedWriter bw){
		if (bw==null) return;
		try{
			bw.flush();
			bw.close();
	  } catch ( Exception e ) {
			e.printStackTrace();
			return;
		} 
	}	
	public static void flush(BufferedWriter bw){
		try{
			bw.flush();
	  } catch ( Exception e ) {
			e.printStackTrace();
		} 
	}	

	/**
	 * Write file content to the end of a file
	 * @param fileContent
	 * @param filePath
	 */
	public static void appendToFile(String fileContent, String filePath) {
		try {
			FileOutputStream fos = new FileOutputStream(filePath, true);
			OutputStreamWriter osw = new OutputStreamWriter(fos, "UTF-8");
			BufferedWriter bw = new BufferedWriter(osw);
			bw.write(fileContent);
			bw.close();
			osw.close();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		} 
	}
	public static BufferedWriter bufferedWriter(String fn){
		return bufferedWriter(fn,  false);
	}
	
	public static BufferedWriter bufferedWriter(String fn, boolean bAppend){
		return bufferedWriter(fn, bAppend, "UTF-8");
	}
	
	// create append
	public static BufferedWriter bufferedWriterCA(String fn){
		return bufferedWriter(fn, FFile.exist(fn));
	}

	public static BufferedWriter bufferedWriter(
			String fn, boolean bAppend, String encoding){
		//System.out.println("Open write "+fn);
		try{
			return new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(fn,bAppend), encoding));
	  } catch ( Exception e ) {
			e.printStackTrace();
			return null;
		}  
	}

	public static ObjectOutputStream objectOutputStream(String fn) throws IOException {
		return new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(fn)));
	}

	public static ObjectInputStream objectInputStream(String fn) throws IOException {
		if (!exist(fn)) return null;
		return new ObjectInputStream(new GZIPInputStream(new FileInputStream(fn)));
	}

	/*public static void save(IWrite x, String fn) throws IOException {
		BufferedWriter w = bufferedWriter(fn);
		x.write(w);
		return;
	}*/

	public static <T> boolean save(Iterable<T> v, String fn, String sep){
		return save(v,fn,sep,null);
	}
	public static <T> boolean save(Iterable<T> v, String fn, String sep,String title){
		BufferedWriter bw  = FFile.bufferedWriter(fn);
		if (title!=null)
			FFile.writeln(bw, title);
		for ( T k:v ){ 
			FFile.write(bw, k.toString());
			if (sep!=null)
				FFile.write(bw, sep);
		}
		FFile.flush(bw);	
		FFile.close(bw);
		return true;
	}	

	public static void load(IRead x, String fn) throws IOException {
		BufferedReader w = bufferedReader(fn);
		x.read(w);
		return;
	}
	
	

	public static String enterFileName() {
		// Create a file dialog to query the user for a filename.
		// FileDialog f = new FileDialog(frame, "Load Scribble", FileDialog.LOAD);
		// FileDialog f = new FileDialog(frame, "Save Scribble", FileDialog.SAVE);
		// f.show();                        // Display the dialog and block.
		// String filename = f.getFile();   // Get the user's response
		return null;
	}

	public static void saveObject(Object x,String fn) {
		try{
			ObjectOutputStream out = objectOutputStream(fn);
			out.writeObject(x); // Write the entire Vector of scribbles
			out.flush(); // Always flush the output.
			out.close(); // And close the stream.
		}
		catch(Exception e){
			e.printStackTrace();
		}
		
	}

	public static Object loadObject(String fn)  {
		try{
			ObjectInputStream in = objectInputStream(fn.trim());
			if (in==null) return null;
			Object x = in.readObject();
			in.close();
			return x;
		}
		catch(Exception e){
			//e.printStackTrace();
			System.out.println(e);
		}
		return null;
	}

	public static BufferedReader bufferedReader(String fn) {
		return bufferedReader(fn, "UTF-8");
	}
	public static BufferedReader bufferedReaderOrDie(String fn) {
		BufferedReader br= bufferedReader(fn, "UTF-8");
		if (br==null) 
			System.exit(-1);	
		return br;
	}
	public static BufferedReader bufferedReader(	String fn, String encoding) {
		try {
			return new BufferedReader(new InputStreamReader(
					new FileInputStream(fn), encoding));
		} catch (Exception e) {
			System.err.println(
				"cannot open read file="+fn);
			return null;
		}
	}
	/** Recursively delete files anchored at <code>f</code>	 * 
	 * @param f Directory or file to delete.
	 */
	public static void rmRecur(File f) {
		if (f.isDirectory()) {
			File[] g = f.listFiles();
			for (int i = 0; i < g.length; i++)
				rmRecur(g[i]);
		}
		boolean deleted = f.delete();
		// logger.info("deleted: "+f+" "+(deleted?"-done":
		// "-not present!"));
	}


	//TODO: it somehow does not work
	public static boolean exist(String fn) {
		return (new File(fn)).exists();
	}
	public static String getFileName(String filePath) {
		return (new File(filePath)).getName();
	}
	public static String getCanonicalPath(String fn) {
		try {
			return (new File(fn)).getCanonicalPath();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String cwd()  {
		try {
			return getCanonicalPath(".");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String parentFolder()  {
		try {
			return getCanonicalPath("..");
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}


	// Gets a list of String filenames present in a directory.
	// Returns an empty list if the directory is empty, if the
	// filename is not a directory, or if an exception occurred.
	public static List<String> ls(String dir) {
		List<String> files = new ArrayList<String>();
		File d = null;
		try {
			d = new File(dir);
			if (d.isDirectory()) {
				File contents[] = d.listFiles();
				for (int i = 0; i < contents.length; i++) 
					files.add(contents[i].getCanonicalPath());
			}
		} catch (Exception e) {
			System.err
					.println("Caught exception listing contents of " + dir + ":" + e.getMessage());
			return Collections.emptyList(); // Exception caught.
		}
		Collections.sort(files);
		return files;
	}

	// Takes a list of string filenames, and returns one as well.
	public static List<String> lsRecur(List<String> input) throws IOException {
		List<String> output = new ArrayList<String>();
		for (Iterator<String> i = input.iterator(); i.hasNext();) {
			try {
				String filename = i.next();
				File f = new File(filename);
				if (f.isDirectory()) {
					output.addAll(lsRecur(ls(filename)));
				} else {
					// System.err.println("adding:"+filename);
					output.add(filename);
				}
			} catch (Exception e) {
				System.err.println("(lsRecur)" + e.getMessage());
			}
		}
		return output;
	}

	public static String getClassPath(Class c) {
		URL url = c.getProtectionDomain().getCodeSource().getLocation();
		//Workaround for windows' problematic paths 
		//such as "C:\Documents%20and%20Settings\"
		return url.getPath().replaceAll("%20", " ");
	}

	public static String getClassPath2(Class cls) {
		if (cls == null) return null;
		try {
			String name = cls.getName().replace('.', '/');
			URL loc = cls.getResource("/" + name + ".class");
			File f = new File(loc.getFile());
			// Class file is inside a jar file.
			if (f.getPath().startsWith("file:")) {
				String s = f.getPath();
				int index = s.indexOf('!');
				// It confirm it is a jar file
				if (index != -1) {
					f = new File(s.substring(5).replace('!', File.separatorChar));
					return f.getPath();
				}
			}
			f = f.getCanonicalFile();
			return f.getPath();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return null;
		}
	}


	public static byte[] loadByte(String file) {
		try {
		   FileInputStream fis = new FileInputStream(file);
		   FileChannel fc = fis.getChannel();
		   byte[] data = new byte[(int)(fc.size())];   
		   // fc.size returns the size of the file which backs the channel
		   ByteBuffer bb = ByteBuffer.wrap(data);
		   fc.read(bb);
		   return data;
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return null;
	}

	public static boolean saveByte(byte[] vb,String file){
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(vb);
			fos.flush();
			fos.close();
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}			
		return false;
	}

	

		
	public static ArrayList<File> getSortedFiles( String dirPath ) throws Exception {
		File dir = new File(dirPath);
        if (!dir.exists()) {
        	throw new Exception("Directory doesn't exist: "+dir.getAbsolutePath());
    	}
        
        File[] fileArray = dir.listFiles();
        
        ArrayList<File> files = new ArrayList<File>(fileArray.length);
        for (File f : fileArray) {
        	files.add(f);
        }
        Collections.sort(files);
        return files;
	}
	
	public static ArrayList<File>[] getSortedFiles( String dirPath1, String dirPath2 ) throws Exception {
		File dir1 = new File(dirPath1);
        if (!dir1.exists()) {
        	throw new Exception("Directory doesn't exist: "+dir1.getAbsolutePath());
    	}
        File dir2 = new File(dirPath2);
        if (!dir2.exists()) {
        	throw new Exception("Directory doesn't exist: "+dir2.getAbsolutePath());
    	}
        
        File[] fileArray1 = dir1.listFiles();
        File[] fileArray2 = dir2.listFiles();
        
        ArrayList<String> fileNameList1 = new ArrayList<String>(fileArray1.length);
        ArrayList<String> fileNameList2 = new ArrayList<String>(fileArray2.length);
        
        HashSet<String> set = new HashSet<String>(fileArray1.length+fileArray2.length);
        for (File f : fileArray1) {
        	fileNameList1.add(f.getName()); 
        	set.add(f.getName());
        }
        for (File f : fileArray2){
        	fileNameList2.add(f.getName()); 
        	set.add(f.getName());
        }
        
        HashSet<String> setUpdated = new HashSet<String>(set.size());
        
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
        	String fileName = it.next();
        	if (fileNameList1.contains(fileName) && fileNameList2.contains(fileName)) {
        		setUpdated.add(fileName);
        	}
        }
        
        ArrayList<File> fileList1 = new ArrayList<File>(setUpdated.size());
        ArrayList<File> fileList2 = new ArrayList<File>(setUpdated.size());
        
        it = setUpdated.iterator();
        while (it.hasNext()) {
        	String fileName = it.next();
        	fileList1.add(new File(dirPath1+"/"+fileName));
        	fileList2.add(new File(dirPath2+"/"+fileName));
        }
        Collections.sort(fileList1);
        Collections.sort(fileList2);
        
        ArrayList<File>[] sortedFileLists = new ArrayList[2];
        sortedFileLists[0] = fileList1;
        sortedFileLists[1] = fileList2;
        
        return sortedFileLists;
	}

	public static void printProgress( PrintStream out, int i ) {
		out.print("*"+(i%10==9?" ":"")+(i%50==49?"\n":""));
	}
	/**
	 * Recursively browses a directory and its subdirectories for files.
	 * 
	 * @param dir a directory
	 */
	private static void getFilesRec(File dir, ArrayList<File> files) {
		File[] filesOrDirs = dir.listFiles();
		for (File fileOrDir : filesOrDirs)
			if (fileOrDir.isFile()) files.add(fileOrDir);  // add normal files
			else getFilesRec(fileOrDir, files);  // browse subdirectories
	}
	
	/**
	 * Returns the files in the given directory 
	 * (only normal files, no subdirectories).	  
	 * @param dir a directory
	 * @return files in the directory
	 */
	public static VectorS  getFiles(String dir) {
		return getFiles(dir, ".*");
	}
	public static VectorS  getFiles(String dir, String regex) {
		//TVector<File> files = new TVector<File>(File.class);
		VectorS files = new VectorS();		
		// only return normal files, no subdirectories
		File[] filesOrDirs = new File(dir).listFiles();
		for (File fileOrDir : filesOrDirs){
			if (!fileOrDir.isFile()) continue;
			if (!fileOrDir.getName().matches(regex)) continue;
			files.add(fileOrDir.getName());
		}
		return files;
	}
	public static VectorS  getFolders(String dir) {
		VectorS files = new VectorS();
		
		// only return normal files, no subdirectories
		File[] filesOrDirs = new File(dir).listFiles();
		for (File fileOrDir : filesOrDirs)
			if (!fileOrDir.isFile()) files.add(fileOrDir.getName());		
		return files;
	}
	/**
	 * Returns the files in the given directory and its subdirectories.
	 * 
	 * @param dir a directory
	 * @return files in the directory and subdirectories
	 */
	public static File[] getFilesRec(String dir) {
		ArrayList<File> files = new ArrayList<File>();
		
		// recursively browse directories
		getFilesRec(new File(dir), files);
		
		return files.toArray(new File[files.size()]);
	}
	
	/**
	 * Returns the subdirectories of the given directory.
	 * 
	 * @param dir a directory
	 * @return subdirectories
	 */
	public static File[] getSubdirs(String dir) {
		File[] filesOrDirs = new File(dir).listFiles();
		ArrayList<File> subdirs = new ArrayList<File>();
		
		// only return subdirectories, no files
		for (File fileOrDir : filesOrDirs)
			if (!fileOrDir.isFile()) subdirs.add(fileOrDir);
		
		return subdirs.toArray(new File[subdirs.size()]);
	}
	public static String loadString(String fileName){
		return loadString(fileName, "UTF-8");
	}
	public static String loadLastLine(String fileName){
		//String txt = loadString(fileName);
		try{
			BufferedReader br=new BufferedReader(
				new InputStreamReader(new FileInputStream(fileName)	));
			StringBuffer sb=new StringBuffer();
			String rlt=null;
			for(String line;(line=br.readLine())!=null;)
				rlt=line;			
			br.close();
			return rlt;
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
/*	public static String loadString(String filePath) {
		StringBuilder fileContent = new StringBuilder();
		try {
			FileInputStream fis = new FileInputStream(filePath);
			InputStreamReader isr = new InputStreamReader(fis, "UTF-8");
			BufferedReader br = new BufferedReader(isr);
			
			String line;
			while ((line=br.readLine())!=null) {
				fileContent.append(line+"\n");
			}
			br.close();
			isr.close();
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}	
		return fileContent.toString();
	}*/
	/**
	 * Reads the entire contents of the file with the specified name, using the 
     * specified encoding.
	 * 
	 * @param fileName the name of the file to read
	 * @param encoding the encoding to use
	 * @return the contents of the file in a String
	 */
	public static String loadString(String fileName,String encoding){
		try{
			BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(fileName),encoding));
			StringBuffer sb=new StringBuffer();
			for(String nextLine;(nextLine=br.readLine())!=null;){
				sb.append(nextLine+"\n");
			}
			br.close();
			return sb.toString();
		}
		catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}
	/**
	 * Writes the specified String to a file with the specified name, using the
     * specified encoding.
	 * 
	 * @param data the String to write to the file
	 * @param fileName the name of the file to write to
	 * @param encoding the encoding to use
	 */
	public static void saveString(String fileName,String data,String encoding){
		try{
			BufferedReader reader=new BufferedReader(new StringReader(data));
			PrintWriter writer=new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName),encoding),true);
			for(String nextLine;(nextLine=reader.readLine())!=null;){
				writer.println(nextLine);
			}
			reader.close();
			writer.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}	
	/**
	 * Write file content to a file
	 * @param txt
	 * @param filePath
	 */
	//public static void saveString(String filePath, String txt){
		//saveString(filePath,txt, "UTF-8");	}
	public static void saveString(String filePath, String txt, Object ... args){
		saveString(filePath, String.format(txt, args),"UTF-8");
	}	
	
  public static void copy(String in, String out) {
		try{
	    FileInputStream fis  = new FileInputStream(in);
	    FileOutputStream fos = new FileOutputStream(out);
	    byte[] buf = new byte[1024];
	    int i = 0;
	    while((i=fis.read(buf))!=-1) {
	        fos.write(buf, 0, i);
	    }
	    fis.close();
	    fos.close();
		}
		catch(Exception e){
			e.printStackTrace();
		}
  }
	public static boolean delete(String fn){
		return (new File(fn)).delete();
  }
	public static void mergeFiles(
			String folder,String regex, String fn){
		mergeFiles(folder, FFile.getFiles(folder, regex),fn);
	}	
	public static void mergeFiles(String folder,VectorS vf, String fn){
		BufferedWriter bw=FFile.bufferedWriter(fn	);
		for (String fn1: vf){
			//System.out.println("merge file "+fn1);
			//if (!FFile.exist(fn1))				continue;
			FFile.write(bw,FFile.loadString(folder+"/"+fn1));
		}
		FFile.close(bw);
		System.out.println(vf.size()+" files merged");		
	}	
	public static boolean move(String srcFile, String destPath) {
			// File (or directory) to be moved
		File file = new File(srcFile);
		// Destination directory
		File dir = new File(destPath);
		// Move file to new directory
		boolean success = file.renameTo(new File(dir, file.getName()));
		return success;
	}
	 public static void copyFile(String oldPath, String newPath) {
		try {
			int bytesum = 0;
			int byteread = 0;
			File oldfile = new File(oldPath);
			if (oldfile.exists()) {
				InputStream inStream = new FileInputStream(oldPath);
				FileOutputStream fs = new FileOutputStream(newPath);
				byte[] buffer = new byte[1444];
				int length;
				while ((byteread = inStream.read(buffer)) != -1) {
					bytesum += byteread;
					System.out.println(bytesum);
					fs.write(buffer, 0, byteread);
				}
				inStream.close();
			}
		} catch (Exception e) {
			System.out.println("error copying file ");
			e.printStackTrace();
		}
	} 
  /** Deletes all files and subdirectories under dir.
	// Returns true if all deletions were successful.
	// If a deletion fails, the method stops attempting to delete and returns
	// false.*/
	public static boolean deleteDir(String dir1) {
		File dir =new File(dir1);
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDir(dir+"/"+ children[i]);
				if (!success) {
					return false;
				}
			}
		}
		// The directory is now empty so delete it
		return dir.delete();
	}
	

}
