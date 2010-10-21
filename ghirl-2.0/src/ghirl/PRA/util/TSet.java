/**
 * 
 */
package ghirl.PRA.util;

import edu.cmu.minorthird.classify.Feature;
import edu.cmu.minorthird.classify.MutableInstance;
import ghirl.PRA.util.Interfaces.IGetIntByString;
import ghirl.PRA.util.Interfaces.IRead;
import ghirl.PRA.util.Interfaces.IWrite;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.PRA.util.TVector.VectorS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * @author nlao
 *This class is an extension to Set&lt;V&gt;
 *
 */
public class TSet<K>  extends  TreeSet<K> implements 
	IGetIntByString, IWrite, IRead , Serializable{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public Class c=Object.class;
	
	public Integer getInt(String name){
		if (name.equals(CTag.size)) return size();
		return null;
	}
	
	public TSet<K> newInstance(){
		return new TSet<K>(c);
	}
	public TSet<K> copy(){
		//return new TMap<K, V>(this);
		TSet<K> m = newInstance();
		m.addAll(this);
		return m;
	}
	public TSet<K> removeOn(Collection<K> c){
		removeAll(c);
		return this;
	}
	
	public K newKey(){//needed for primitive classes, silly java//weakness of Java template
		try{
			return (K) c.newInstance();
		}
		catch (Exception e){
			System.out.println(e.getClass().getName());
			e.printStackTrace();			
		}
		return null;
	}
	
	public TSet(Class c){
		this.c = c; 
	}
	// will pre allocate memory
	public TSet(Class c, Collection<K> v){
		this.c =c;// v.iterator().next().getClass();
		addAll(v);
	}
	// will incrementally allocate memory
	public TSet(Class c,Iterable<K> v){
		this.c =c;// v.iterator().next().getClass();
		addIterable(v);
	}
	public TSet(K[] v){
		this.c = v.getClass();
		addAll(v);
	}
	

	public TSet<K>  addOn(Set<K> m){
		for ( K e : m) 	add(e);
		return this;
	}
	// What's this... clip counted version?

	public TSet<K>  and(Collection<K> v){
		TSet<K> m= newInstance();
		for ( K x : v) 
			if (contains(x))
				m.add(x);		
		return m;
	}	
	public TSet<K>  and(K[] v){
		TSet<K> m= newInstance();
		for ( K x : v) {
			if (contains(x))
				m.add(x);
		}
		return m;
	}	
	public TSet<K>  andSet(Set<K> m){
		TSet<K> m1= newInstance();
		for ( K e : this) 
			if (m.contains(e))
				m1.add(e);		
		return m1;
	}
	
	public TSet<K> addAll (K[] v){
		for (K x : v)
			add(x);		
		return this;
	}
	public TSet<K> addIterable (Iterable<K> v){
		for (K x : v)
			add(x);		
		return this;
	}
	public TSet<K> load (K[] v){
		clear();
		return addAll(v);
	}
	public TSet<K> load (Collection<K> v){
		clear();
		addAll(v);
		return this;
	}	
/*	public TSet<K> fromMapKey<V> (TMap<K,V> m){
		for ( Map.Entry<K, V> e : m.entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			this.put(k, v);
		}	
		return this;
	}	*/	
	
	public String join(String c) {
		StringBuffer sb = new StringBuffer();
		int first=1;
		for ( K e : this) {
			if (first==1)	first=0;			
			else	sb.append(c);			
			sb.append(e);
		}
		return (sb.toString());
	}	
	public String toString() {
		return join( ", ");
	}
	public BufferedWriter write(BufferedWriter writer){// throws IOException {
		for ( K e : this) {
			// writer.write(x);
			//((IWrite) x).write(writer);
		}
		return writer;
	}
	public BufferedReader read(BufferedReader reader) {//throws IOException {
		for ( K e : this){
			// writer.write(x);
			//((IRead) x).read(reader);
		}
		return reader;
	}
	public TVector<K> newVector(){
		return new TVector<K>(c);
	}
	
	public TVector<K> toVector(){
		TVector<K> v= newVector();
		v.ensureCapacity(size());
		for ( K e : this){
		//for (int i=0; i< size(); ++i)
			v.add(e);
		}
		return v;
	}

	public boolean in(TSet<K> m){
		for ( K x : this ) {
			if (!m.contains(x)) return false;
		}
		return true;
	}		


	public TSet<K> addOn(K x){
		this.add(x);
		return this;
	}

	public boolean save(String fn){
		return	FFile.save(this,fn,null,"\n");
	}	
	public boolean save(String fn, String sep){
		return	FFile.save(this,fn,null,sep);
	}	
	public boolean saveT(String fn,String title){
		return	FFile.save(this,fn,title,"\n");
	}	
	public K parseLine(String k){		
		return null;
	}
	

	public boolean loadLine(String x, String sep) {
		this.clear();
		for (String s: x.split(sep))
			this.add(parseLine(s));
		return true;
	}
	/**
	 * @author nlao
	 *This class is an extension to TSet&lt;Integer&gt;
	 */
	public static class SetI extends TSet<Integer>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public SetI newInstance(){
			return new SetI();
		}	
		public TVector<Integer >newVector(){
			return new VectorI();		
		}
		public Integer newKey(){//needed for primitive classes, silly java
			return 0;
		}	
		public SetI(){super(Integer.class);}
		public SetI(Iterable<Integer> v){super(Integer.class,v);}
		
		public MutableInstance toM3rdInstance(){
			MutableInstance instance = new MutableInstance();
			for ( int f : this ) 
				instance.addBinary( new Feature( f+"" ) );
			return instance;
		}


		
		public Integer parseLine(String k){		
			return Integer.parseInt(k);
		}
		public VectorI toVector(){
			return (VectorI) super.toVector();
		}
		
		public static SetI  fromFile(String fn){
			return new SetI(FTable.enuLines(fn).toSeqI());
		}
		public SetI  andSet(Set<Integer> m){
			return (SetI) super.andSet(m);
		}
		public SetI  and(Set<Integer> m){
			return (SetI) super.andSet(m);
		}
	}
	/**
	 * @author nlao
	 *This class is an extension to TSet&lt;String&gt;
	 */
	public static class SetS extends TSet<String>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public SetS newInstance(){
			return new SetS();
		}	
		public VectorS newVector(){
			return new VectorS();
		}
		
		public String newKey(){//needed for primitive classes, silly java
			return null;
		}
		public SetS(){
			super(String.class); 
		}
		public VectorS toVector(){
			return (VectorS) (new VectorS()).load(this);
		}		
		
		public SetS(Iterable<String> v){
			super(String.class, v);
		}
		public SetS(String[] v){
			super(v);
		}
		public SetS(String s){
			super(s.split(" "));
		}
		public SetS addAll (String s){
			addAll(s.split(" "));
			return this;
		}


		public String match(String txt){
			for (String s: this)
				if (txt.indexOf(s)>=0)
					return s;
			return null;
		}
		
		//public void load(String fn){addAll(FTable.enuLines(fn));	}
		public String parseLine(String k){		
			return k;
		}
				

	}
}
