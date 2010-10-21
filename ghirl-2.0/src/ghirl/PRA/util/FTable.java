package ghirl.PRA.util;

import ghirl.PRA.util.TSequence.Pipe;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.PRA.util.TVector.VectorS;
import ghirl.PRA.util.TVector.VectorVectorS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;


// this class should be simplified with lambda expressions
public  class FTable {

	
	
  // group the rows of a stream by the value of a column
  // assuming the stream is already sorted by this column
  //string[][]
	
	/*
	public static Iterable<Vector<String[]>> groupByCol
  (Iterable<String[]> rows,int iCol) {
  List<string[]> vRow=null;
  string lastValue=null;
  foreach (string[] row in rows) {
    if (row[iCol]!=lastValue) {
      if (vRow!=null)
        yield return vRow;
      vRow=new List<string[]>();
    }
    vRow.Add(row);
    lastValue=row[iCol];
  }
  if (vRow!=null)
    yield return vRow;
}
  
*/
	
  //enumerate lines of a file, grouped by a perticular column
	

  public static SequenceS enuACol(String fn , int iCol) {
    return new SequenceS(enuRows(fn), new PipeVSS(iCol));
  }
  
  //enumerate lines of a file, splited into columns
  public static TSequence<String[]> enuRows(String fn) {
  	return enuRows(fn,"\t");  	
  }
    public static TSequence<String[]> enuRows(String fn, String sep) {
  	return new TSequence<String[]>(null, enuLines(fn), new PipeSVS(sep));
  }
	public static class PipeVSS  implements Pipe<String[],String>{
		int iCol;
		public PipeVSS(int iCol ){
			this.iCol=iCol;
		}
		public String transform(String[] v){
			return v[iCol];
		}
	}
	
	public static class PipeSVS  implements Pipe<String,String[]>{
		String sep= "\t";
		public PipeSVS(){	
		}
		public PipeSVS(String sep){
			this.sep=sep;
		}
		public String[] transform(String s){
			return  s.split(sep);
		}
	}
  
  public static SequenceS enuLines(String fn){
  	return enuLines(fn,false);
  }
  public static SequenceS enuLines(String fn, boolean bSkipTitle){
  	return new SequenceS(new LineSequence(fn,bSkipTitle));
  }// elevate from Iterable to TSequence
  
	public static class LineSequence implements Iterable <String>, Iterator<String>{
		
		private BufferedReader br=null;
		public LineSequence( String fn){
			this(fn,false);
		}
		public LineSequence( String fn, boolean bSkipTitle) {//Class c,
			//this.c = c;
	 		br=FFile.bufferedReader(fn);
	 		if (bSkipTitle) FFile.readLine(br);
		}
		String line=null;
	  public boolean hasNext()  {
			if (br==null)  return false;
	  	line=FFile.readLine(br);
	  	if (line==null){
	  		FFile.close(br);
	  		br=null;
	  		return false;
	  	}
			return true;
	  }
	  public String next() throws NoSuchElementException {    
	    return line;
	 }
	  public void remove()  {  }
	  public Iterator<String> iterator(){ 	return this; }
	}

	
	public static VectorS  loadLines(String fn){
		return loadLines(fn,false);
	}

	public static VectorS  loadLines(String fn, boolean skipTitle){
		return loadLines(fn,skipTitle,false);
	}

	public static VectorS  loadLines(String fn,boolean skipTitle, boolean bBreakSharp){
		VectorS  vs = new VectorS();	//	vs.load(fn);
		
		for (String line: enuLines(fn)){
			if (skipTitle) {
				skipTitle=false;
				continue;
			}
			if (bBreakSharp)
				if (line.startsWith("#"))
					break;
			vs.add(line);
		}
		return vs;
	}
	//static int nLine=0;

	
	public static boolean splitByProb(String fn, double p) {
		return splitByProb(fn,p, fn+".p"+p,fn+".p"+(1-p));
	}
	public static boolean splitByProb(String fn, double p, String fn1, String fn2) {
		BufferedReader br = FFile.bufferedReader(fn);	
		BufferedWriter bw1 = FFile.bufferedWriter(fn1);	
		BufferedWriter bw2 = FFile.bufferedWriter(fn2);	
		if (br==null) 
			return false;
		if (bw1==null) 
			return false;
		if (bw2==null) 
			return false;
		
		String line = null;
		while ((line = FFile.readLine(br)) != null) 
			if (FRand.drawBoolean(p))
				FFile.writeln(bw1,line);
			else
				FFile.writeln(bw2,line);
		
		
		FFile.close(br);
		FFile.close(bw1);
		FFile.close(bw2);
		return true;
	}
	
	public static boolean countNonempltyCells(String fn) {
		BufferedReader br = FFile.bufferedReader(fn);	
		if (br==null) return false;
		String line = null;
		//VectorI vC=new VectorI();
		int vC[]=new int[10000]; 
		int nCol=0;
		while ((line = FFile.readLine(br)) != null) {
			String vs[]=line.split("\t");
			if (vs.length>nCol) nCol=vs.length;
			//vC.extend(vs.length);
			for (int i=0;i<vs.length;++i)
				if (vs[i].length()>0)
					++vC[i];
					//vC.set(i,vC.g)
		}
		FFile.close(br);
		
		BufferedWriter bw = FFile.bufferedWriter(fn+".count");	
		if (bw==null) return false;
		for (int i=0;i<nCol;++i)
			FFile.write(bw,vC[i]+"\t");
		
		FFile.close(bw);
		return true;
	}
	public static boolean filterByFreq(String fn, int iCol, int th) {
		return filterByFreq(fn,iCol,th, String.format(
			"%s.c%dfq%d", fn,iCol,th));
	}	
	public static boolean filterByFreq(String fn, int iCol, int th, String fn1) {
		BufferedReader br = FFile.bufferedReader(fn);	
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (br==null) return false;
		if (bw==null) return false;
		String line = null;
		while ((line = FFile.readLine(br)) != null){
			String vs[]= line.split("\t");
			if (vs.length>iCol)
				if (Integer.parseInt(vs[iCol])>=th)
					FFile.writeln(bw,line);
		}
		FFile.close(br);
		FFile.close(bw);
		return true;
	}
	public static VectorS loadAColumn(String fn, int iCol) {
		return loadAColumn(fn,iCol,"\t");
	}
	public static VectorS loadAColumn(String fn, int iCol, String c) {
		VectorS vs= new VectorS();

		BufferedReader br = FFile.bufferedReader(fn);	
		if (br==null) return null;
		String line = null;
		while ((line = FFile.readLine(br)) != null){ 
			String v[]=line.split(c);
			if (v.length>iCol)
				vs.add(v[iCol]);
		}
		
		FFile.close(br);
		return vs;
	}
	public static boolean filterByColumn(String fn, int iCol, Set<String> mKeep) {
		return filterByColumn(fn,fn+".f"+iCol, iCol,mKeep);
	}
	public static boolean filterByColumn(String fn, String fn1, int iCol, Set<String> mKeep) {
		BufferedReader br = FFile.bufferedReader(fn);	
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (br==null) return false;
		if (bw==null) return false;
		String line = null;
		while ((line = FFile.readLine(br)) != null) 
			if (mKeep.contains(FString.split(line,"\t")[iCol]))
				FFile.writeln(bw,line);
		
		FFile.close(br);
		FFile.close(bw);
		return true;
	}
	
	public static boolean splitItems(String fn, String fn1) {
		return splitItems(fn,fn1," ");
	}
	public static boolean splitItems(String fn, String fn1, String cSep) {
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (bw==null) return false;
		for (String line: enuLines(fn))
			for (String item: FString.split(line,cSep))
				FFile.writeln(bw,item);
		
		FFile.close(bw);
		return true;
	}

	public static boolean subColumns(String fn, VectorI vKeep) {
		return subColumns(fn,vKeep,fn+".cols");
	}
	public static boolean subColumns(String fn, VectorI vKeep, String fn1) {
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (bw==null) return false;
		for (String line: enuLines(fn)){
			VectorS vs= FString.splitVS(line, "\t");
			if (vs.firstElement().length()==0) continue;
			vs=vs.sub(vKeep);
			FFile.writeln(bw,vs.join("\t"));
		}
		FFile.close(bw);
		return true;
	}

	
	public static boolean mergeColumns(String fn, VectorI vKeep, String fn1) {
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (bw==null) return false;
		for (String line: enuLines(fn)){
			VectorS vs= FString.splitVS(line, "\t");
			if (vs.firstElement().length()==0) continue;
			for (String cell: vs.sub(vKeep))
				for (String item: cell.split(" "))
					if (item.length()>0)
						FFile.writeln(bw,item);
		}
		FFile.close(bw);
		return true;
	}

	public static boolean subColumn(String fn, int iCol) {
		return subColumn(fn,iCol,fn+".col"+iCol);
	}
	public static boolean subColumn(String fn, int iCol, String fn1) {
		return subColumn(fn,iCol,fn1,"\t");
	}
	public static boolean subColumn(String fn, int iCol, String fn1, String c) {
		
	/*	new ILineTransformer() {
			public String transform(String line){
				return FString.split(line,c)[iCol]);
			}
		}
		*/
		BufferedWriter bw = FFile.bufferedWriter(fn1);	
		if (bw==null) return false;
		for (String line: enuLines(fn))
			FFile.writeln(bw,FString.split(line,c)[iCol]);
		FFile.close(bw);
		return true;
	}
	public static interface ILineTransformer {
		String transform(String s);
	}
}
