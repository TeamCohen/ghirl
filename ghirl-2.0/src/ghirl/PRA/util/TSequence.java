package ghirl.PRA.util;

import ghirl.PRA.util.FTable.PipeSVS;
import ghirl.PRA.util.Interfaces.IGetStringByString;
import ghirl.PRA.util.Interfaces.IParseLine;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * this is a sequence of objects 
 * which can be deserialized from a text file
 * with one object per line
 * 
 * fun(this XXX xxx, YYY yyy) is helpful
 * it acts like a pipe, and glues two types of things together
 * 
//class and member variable pointers are needed 
// because the lack of yield return
 * 
//don't want to have many versions, mess things up
sequence from file-->FileSequencer
sequence from Iterable -->TSequence
	sequence from collection
	sequence from sequence

 * @author nlao
 *
 * @param <T>
 * http://www.artima.com/weblogs/viewpost.jsp?thread=208860
 */
public class TSequence<T> implements Iterable <T>, Iterator<T>{
	
	//public static interface IWhere {boolean extract(T o);}
	
	//public TSequence<T> Where(IWhere f){	}
	
	public Class c=null;//stupic java	
	public T newValue(){	
		return null;//(T) FClass.newValue(c);
	}
	
	private Class getArgClass1(){
		try{
			Type t=getClass().getGenericSuperclass();
			return (Class)((ParameterizedType)t)
				.getActualTypeArguments()[0];
		}
		catch(Exception e){
			
		}
		return null;
	}
	private Class getClass(Type type){
		//Type type=getClass();//.getGenericSuperclass();
    if (type instanceof Class) {
      return (Class) type;
    }
    else if (type instanceof ParameterizedType) {
      return getClass(((ParameterizedType) type).getRawType());
    }
    else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      Class<?> componentClass = getClass(componentType);
      if (componentClass != null ) {
        return Array.newInstance(componentClass, 0).getClass();
      }
      else {
        return null;
      }
    }
    else {
      return null;
    }
	}
	private void _init(){
		//FSystem.

	}
	public Pipe pipe=null;

	private Iterator<T> it=null;
	public TSequence(Class c, Iterable<T> v ) {
		this (c, v,null);
	}
	public <T1> TSequence(Class c,Iterable v, Pipe<T1,T> f) {
		this.pipe=f;
		it=v.iterator();
		//_init();
		this.c=c;
		//c=getArgClass1();// getClass(getClass().getGenericSuperclass());
 	  nTot=0;
 	  nFailure=0;
	}
	public TSequence(String fn ) {
		this (String.class, FTable.enuLines(fn), new PipeSX<T>());
	}

	
	/*
	* Transform an objects in T1 into T2 
	* 	//this is really lame. 
	//shouldn't Integer.parseInt be the function pointer we need?
	//hope java have F pointer
	* @author nlao
	*
	* @param <T1,T2>
	*/
	public static interface Pipe<T1,T2>{// implements Iterable <T2>, Iterator<T2>{
		public T2 transform(T1 x);
	}
	
	// convert string to anything that implements IParseLine
	public static class PipeSX<T2>  implements Pipe<String,T2>{
		public Class c=null;//Object.class;	

		public PipeSX(){
			c=(Class)((ParameterizedType)this.getClass(). 
	       getGenericSuperclass()).getActualTypeArguments()[0];
		}
		public T2 transform(String s){
			T2 x=(T2)FClass.newValue(c);
			if (! ((IParseLine)x).parseLine(s))
				x=null;			
			return  x;
		}
	}
	

	public static class PipeSI  implements Pipe<String,Integer>{
		public Integer transform(String s){
			return Integer.parseInt(s);
		}
	}
	public static class PipeSD  implements Pipe<String,Double>{
		public Double transform(String s){
			return Double.parseDouble(s);
		}
	}

	// convert anything that implements IGetStringByString to string
	public static class PipeXS<T1>  implements Pipe<T1, String>{
		public String attributeName;	
		public PipeXS(String attributeName){
			this.attributeName=attributeName;
		}
		public String transform(T1 x){
			return  ((IGetStringByString)x).getString(attributeName);
		}
	}
	 
	public void save(String fn){
		FFile.save(this,fn,"\n",null);
	}
	public void save(String fn, String sep){
		FFile.save(this,fn,sep,null);
	}

  public TSequence<String[]> enuRows(String sep) {
  	return new TSequence<String[]>(null, this, new PipeSVS(sep));
  }
  
	public TSequence<T> goupBy(Pipe f){
		return null;//new TSequence<Vector<T> >(this, )
		/*
		 * 	public static Iterable<Vector<String[]>> groupByCol
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
		 */
	}
	


  public Iterator<T> iterator(){
  	return this;
  }
  

  public void remove()  {
  }
  //int nSucc=0;
  int nTot=0;
  int nFailure=0;
  T x=null;
  public boolean hasNext()  {
  	if (it==null) return false;
		if (!it.hasNext()) return false;
		if (pipe==null)
			x=it.next();
		else
			x= (T) pipe.transform(it.next());
		return true;
  }
  
  
  public T next() throws NoSuchElementException {
     if ( x==null )
        throw new NoSuchElementException();
     return x;
  }
  
  
  
  public static void main ( String[] args )   {
 /*  for ( PMAbsInfor abs: 
  	 new TSequence<PMAbsInfor>("pmid.sgd.crawl.ex") )
     System.out.println( abs );*/
     
  }
}
