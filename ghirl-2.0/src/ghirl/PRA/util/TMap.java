package ghirl.PRA.util;

import edu.cmu.minorthird.classify.ClassLabel;
import ghirl.PRA.util.Interfaces.ICloneable;
import ghirl.PRA.util.Interfaces.IGetDoubleByString;
import ghirl.PRA.util.Interfaces.IGetIntByString;
import ghirl.PRA.util.Interfaces.IMinusObjOn;
import ghirl.PRA.util.Interfaces.IMultiply;
import ghirl.PRA.util.Interfaces.IMultiplyOn;
import ghirl.PRA.util.Interfaces.IPlusObjOn;
import ghirl.PRA.util.Interfaces.IRead;
import ghirl.PRA.util.Interfaces.IWrite;
import ghirl.PRA.util.TSet.SetI;
import ghirl.PRA.util.TSet.SetS;
import ghirl.PRA.util.TVector.VectorD;
import ghirl.PRA.util.TVector.VectorI;
import ghirl.PRA.util.TVector.VectorS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * @author nlao
 *This class is an extension to Map&lt;K, V&gt;
 *
 */
public class TMap<K, V>  extends  TreeMap<K, V> 
	implements IPlusObjOn, IMinusObjOn, IWrite, IRead 
	, Serializable, Cloneable, ICloneable, IGetIntByString{//, ICopyable
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public Class ck=Object.class;
	public Class cv=Object.class;
	public Integer getInt(String name){
		if (name.equals(CTag.size)) return size();
		return null;
	}
	public int sum(String name){
		int n=0;
		for (V x: this.values())
			n+= ((IGetIntByString)x).getInt(name);
		return n;		
	}
	public TMap<K, V> copy(TMap<K, V> v) {
		clear();
		try{
			for ( Map.Entry<K, V> e : v.entrySet() ) {
				K k = e.getKey();
				V x = e.getValue();
				if (x!=null)
					put(k, (V) ((ICloneable)x).clone());
				else 
					put(k,null);
			}
		}
		catch(Exception e){
			putAll(v);
		}
		return this;
	}
	public TMap<K, V> clone() {
		return newInstance().copy(this);
	}	
	
	
	public TMap(Class ck, Class cv){
		this.ck = ck; 
		this.cv = cv;
	}
	public TMap(TMap<K, V> m){
		this.ck = m.ck; 
		this.cv = m.cv;
		this.putAll(m);
	}	
	public TMap<K, V> load(TMap<K, V> m){
		clear();
		putAll(m);
		return this;
	}	
	public TMap(TVector<K> vk, TVector<V> vx){
		this.ck = vk.c; 
		this.cv = vx.c;
		this.load(vk,vx);		
	}
	public TVector<K> newVectorKey(){
		return new TVector<K>(ck);
	}	
	public TVector<V> newVectorValue(){
		return new TVector<V>(cv);
	}	
	public TSet<K> newSetKey(){
		return new TSet<K>(ck);
	}	
	public TSet<V> newSetValue(){
		return new TSet<V>(cv);
	}	
	public TMap<K, V> line(){
		return new TMap<K, V>(ck, cv);
	}	
	public K newKey(){//needed for primitive classes, silly java//weakness of Java template
		try{
			Object o=ck.newInstance();
			return (K) (o);
		}
		catch (Exception e){
			System.out.println(e.getClass().getName());
			e.printStackTrace();			
		}
		return null;
	}
	public V newValue(){//weakness of Java template
		try{
			Object o=cv.newInstance();
			return (V) (o);
		}
		catch (Exception e){
			System.out.println(e.getClass().getName());
			e.printStackTrace();			
		}
		return null;
	}
	/*
	public TMap<K, V> updateIdx(VectorI vi){	
		TMap<K, V> m = newInstance();
		return m;
	}*/

	public TVector<K> toVectorKeySortedByValueDesc(){
		return toVectorKey().sub(toVectorValue().sortId().reverseOn());
	}
	
	public TVector<K> toVectorKeySortedByValue(){
		return toVectorKey().sub(toVectorValue().sortId());
	}
	
	/**
	 * @author nlao
	 *This class represents multi-map data structure, where each key can have more than one value
	 */
	public static class TMapVector<K, V>  extends TMap<K, TVector<V> >
		implements	Serializable{// IWrite, IRead , 
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		
		//public Class ck=Object.class;
		//public Class cv=Object.class;
		public TMapVector(Class ck, Class cv){
			//this.ck = ck; 
			//this.cv = cv;
			super(ck,cv);
		}
		public V get(K k, int i){
			return get(k).get(i);
		}
		public TMapVector<K,V> fromMapValueKey(TMap<V,K>v){
			this.clear();
			for ( Map.Entry<V,K> e : v.entrySet() ) {
				V x = e.getKey();
				K k = e.getValue();
				insert(k,x);
			}
			return this;
		}	
		public TMapVector<K,V> load(Vector<K> vk, Vector<V>  vx){
			if (vk.size()!= vx.size()){
				System.err.println("vk.size()!= vx.size()");
				System.exit(-1);
			}
			this.clear();
			for (int i=0; i<vk.size();++i) {
				get(vk.get(i)).add(vx.get(i));
			}
			return this;
		}	
		public TMapVector(TMap<K, V> m){
			super(m.ck,  (new TVector<V>(m.cv)).getClass());
			//this.ck = m.ck; 
			//this.cv = m.cv;
			for ( Map.Entry<K, V> e : m.entrySet() ) {
				K k = e.getKey();
				V x = e.getValue();
				insert(k,x);
			}	
		}
		public void insert(K k, V x){
			TVector<V> v=null;
			if (! containsKey(k)){
				v =newValue();//newVectorV();//new TVector<V>(cv);
				super.put(k, v);
			}
			else 
				v=this.get(k);
			v.add(x);
		}
		public V popBack(){
			if (this.isEmpty()) return null;
			TVector<V> v=this.lastValue();
			V x=v.pop();
			if (v.isEmpty())
				this.remove(this.lastKey());
			return x;
		}
		public TVector<V> newValue(){
			return new TVector<V>(cv);	
		}
		
		public TVector<V> toVectorV(){
			TVector<V> v1 = newValue();
			for ( Map.Entry<K, TVector<V> > e : entrySet() ) {
				K k = e.getKey();
				TVector<V>  v = e.getValue();
				v1.catOn(v);
			}			
			return v1;
		}

		public TVector<K> toVectorK(){
			TVector<K> v1= newVectorKey();
			
			for ( Map.Entry<K, TVector<V> > e : entrySet() ) {
				K k = e.getKey();
				TVector<V>  v = e.getValue();
				v1.catOn(new TVector<K>(v.size(), k) );
			}	
			return v1;
		}
		//TMap<K, TVector<V> >
		public  boolean loadColumn(String fn, int icKey, int icValue){
			//TMap<K, TVector<V> >  mv = newInstance();
			BufferedReader br = FFile.bufferedReader(fn);	
			if (br==null) 
				return false;
			String line = null;
			while ((line = FFile.readLine(br)) != null) {
				String vs[] = FString.split(line, "\t");
				K k=parseKey(vs[icKey]);
				V x=parseValueValue(vs[icValue]);
				getC(k).add(x);
			} 
			FFile.close(br);
			return true;
		}
		public V parseValueValue(String s){
			return null;
		}
	}
	public static class TMapVectorXI<K> extends TMapVector<K, Integer>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public TMapVectorXI(Class ck){
			super(ck, Integer.class);
		}
		public VectorI newVectorV(){
			return new VectorI();
		}
		public VectorI newValue(){
			return new VectorI();
		}
	}
	public V lastValue(){
		return get(this.lastKey());
	}


	public TSet<K> toSetKey(){
		TSet<K> v= newSetKey();
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			//V x = e.getValue();
			v.add(k);
		}
		return v;
	}
	


	public TSet<V> toSetValue(){
		TSet<V> v= newSetValue();
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V x = e.getValue();
			v.add(x);
		}
		return v;
	}
	
	public TVector<K> toVectorKey(){
		TVector<K> v= newVectorKey();
		v.ensureCapacity(size());
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			//V x = e.getValue();
			v.add(k);
		}
		return v;
	}

	
	public TVector<V> subV(Vector<K> vi, boolean bIgNull){
		TVector<V> v= newVectorValue();
		v.ensureCapacity(vi.size());
		for ( K k: vi) {
			V x=getN(k);
			if (bIgNull & x==null)
				continue;
			v.add(x);
		}
		return v;
	}	
	
	public TVector<V> subV(Vector<K> vi){
		return subV(vi,false);
	}	
	

	public TVector<V> toVectorValue(){
		TVector<V> v= newVectorValue();
		v.ensureCapacity(size());
		for ( Map.Entry<K, V> e : entrySet() ) {
			//K k = e.getKey();
			V x = e.getValue();
			v.add(x);
		}
		return v;
	}	

	public K idxMin(){
		V x=null;
		K i=null;
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (x!=null) 
				if ( ((Comparable) x).compareTo(v) <=0)
					continue;
			x=v;
			i=k;			
		}
		return i;
	}
	public K idxMax(){
		V x=null;
		K i=null;
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (x!=null) 
				if ( ((Comparable) x).compareTo(v) >=0)
					continue;
			x=v;
			i=k;			
		}
		return i;
	}
	public V min(){
		K i=this.firstKey();//  minKey();
		if (i!=null)	return get(i);
		return null;
	}
	public V max(){
		K i=this.lastKey();//  maxKey();
		if (i!=null)	return get(i);
		return null;
	}
		public TMap<K, V> sub(Collection<K> v){
		TMap<K, V> m = newInstance();
		for (K k: v)
			if (containsKey(k))
				m.put(k, get(k));		
		return m;
	}
	public TMap<K, V> subOn(Collection<K> v){
		TMap<K, V> m = sub(v);
		clear();
		putAll(m);
		return this;
	}
	
	public TMap<K, V> subSet(Set<K> v){
		TMap<K, V> m = newInstance();
		for (K k: this.keySet())
			if (v.contains(k))
				m.put(k, get(k));		
		return m;
	}	
	public TMap<K,V> removeAll(Collection<K> v){
		for (K k: v)
			remove(k);		
		return this;
	}	
	
	public TMap<K, V> sub(K[] v){
		TMap<K, V> m = newInstance();
		for (K k: v){
			if (containsKey(k))
				m.put(k, getN(k));
		}
		return m;
	}
	
	public TVector<V> subV(K[] vi){
		TVector<V> v= newVectorValue();
		v.ensureCapacity(vi.length);
		for ( K k: vi) 
			v.add(getN(k));		
		return v;
	}	
	public TVector<V> subVE(K[] vi){
		TVector<V> v= newVectorValue();
		v.ensureCapacity(vi.length);
		for ( K k: vi) {
			v.add(getN(k));
			if (v.lastElement()==null)
				System.err.println("unknown key="+k);
		}
		return v;
	}	
	
	public TSet<V> subS(K[] vi){
		TSet<V> v= this.newSetValue();
		for ( K k: vi)
			if (get(k)!=null)
				v.add(get(k));		
		return v;
	}	
	public TSet<V> subSE(K[] vi){
		TSet<V> v= this.newSetValue();
		for ( K k: vi)
			if (get(k)!=null)
				v.add(get(k));	
			//else		System.err.println("unknown key="+k);
		return v;
	}	

	public TMap<K, V> filterOn(Set<K> m){
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (! m.contains(k)) this.remove(k);
		}
		return this;
	}	
	
	//get and create if neccesary
	public V getC(K k){
		if (get(k)!=null) 
			return get(k);
		V x=newValue();
		put(k, x);
		return x;
	}	
	
	// accept null as key
	public V getN(K k){		
		if (k==null)
			return null;
		return get(k);
	}	
	
	/**get and create default value if necessary 
	 * @param k
	 * @return
	 */
	public V getD(K k){
		if (get(k)!=null) 
			return get(k);
		return newValue();
	}		
	public V getD(K k, V x){
		if (get(k)!=null) 
			return get(k);
		return x;
	}		
	
/*	public V getC(K k){
		if (this.containsKey(k)){
			return get(k);//TODO: how to create unknown instances?
		}		
		V v=null;//=new V();
		return v;
	}	*/


/*	public TMap<K,V> newInstance(){
		return new TMap<K,V>(ck,cv);
	}	*/

/*	public TMap(){
		super();
	}*/

	public TMap<K, V>  load(Vector<K> vk, Vector<V> vx){		
		for (int i=0; i<vk.size(); ++i) {
			put(vk.get(i), vx.get(i));
		}	
		return this;
	}	
	public void  to(Vector<K> vk, Vector<V> vx){
		vk.clear();vx.clear();
		vk.ensureCapacity(size());
		vx.ensureCapacity(size());
		for (Map.Entry<K, V> e: this.entrySet()) {
			vk.add(e.getKey());
			vx.add(e.getValue());
		}	
		return;
	}	

	/*
	public V sum() {
		V sum = newValue();
		for ( Map.Entry<K, V> e : entrySet() ) {
			//K k = e.getKey();
			V v = e.getValue();
			((IAddOn)sum).addOn(v) ;
		}
		return sum;
	}	
	public V inner(TMap<K, V> m1) {
		return multiply(m1).sum();
	}		
	public TMap<K, V> multiply(TMap<K, V> m1) {//kronecker product
		TMap<K, V> m = newInstance();
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (!m1.containsKey(k)) continue;
			Object o=((IMultiply)v).multiply(m1.get(k));
			put(k, (V) o) ;
		}
		return m;
	}			
	public TMap<K, V> add(K k, V x){
		if (this.containsKey(k)){
			Object o= ((IAdd)get(k)).add(x);
			put(k, (V) o);
		}
		else put(k,x);
		return this;
	}	
	*/
	public String joinValueKey(String cpair, String c) {
		return join( cpair,  c, true);
	}		
	public String join(String cpair, String c) {
		return join( cpair,  c, false);
	}
	public String join(String cpair, String c, boolean bValueKey) {
		StringBuffer sb = new StringBuffer();
		int first=1;
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (first==1)	first=0;			
			else	sb.append(c);				
			if (bValueKey)
				sb.append(FString.format(v)	+cpair+FString.format(k));			
			else
				sb.append(FString.format(k)	+cpair+FString.format(v));			
		}
		return (sb.toString());
	}	
	//in reversed order
/*	public String joinR(String cpair, String c) {
		StringBuffer sb = new StringBuffer();
		int first=1;
		for ( Map.Entry<K, V> e :entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (first==1)	first=0;			
			else	sb.append(c);				
			sb.append(FString.format(k)	+cpair+FString.format(v));			
		}
		return (sb.toString());
	}	*/
	
	public String toString() {
		return join( "="," ");
	}
	public BufferedWriter write(BufferedWriter bw){
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			FFile.write(bw, k+"\t"+v+"\n");
		}
		return bw;
	}
	public BufferedReader read(BufferedReader br){
		while (true){
			String line=FFile.readLine(br);
			if (line==null) break;
			
			
		}
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			// writer.write(x);
			//((IRead) x).read(reader);
		}
		return br;
	}
	public boolean contains(TSet<K> m){
		for ( K x : m) {
			if (!containsKey(x)) return false;
		}
		return true;
	}			
	public V inner(Map<K, Double> m) {
		//TMapXD<K> m1 = multiply(m);
		return multiply(m).sum();
	}		
	public V inner(Vector<K> m) {
		return sub(m).sum();
	}	
	public V inner(Set<K> m) {
		return sub(m).sum();
	}	
	public V sum(){
		V x = this.newValue();
		for ( Map.Entry<K, V> e : entrySet() ) {
			//K k = e.getKey();
			V v = e.getValue();
			if (v!= null)
				((IPlusObjOn)x).plusObjOn(v);
		}
		return x;
	}
	public V mean(){
		return (V) ((IMultiplyOn)sum()).multiplyOn(1.0/size());
	}
	
	//position-wise product 
	//not kronecker product
	public TMap<K,V> multiply(Map<K,Double> m) {
		TMap<K,V> m2 = newInstance();
		if (m==null) return m2;
		for ( Map.Entry<K, V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			if (!m.containsKey(k)) continue;
			m2.put(k,(V) ((IMultiply) v).multiply(m.get(k))  ) ;
		}
		return m2;
	}
	

	
/*	public TMap<K,V> multiplyOn(Map<K,Double> m) {//kronecker product
		//Set<Entry<K,V>>

		return this;
	}		*/
	//
	public TMap<K, V>  addOn(TMap<K, V> m){
		for ( Map.Entry<K, V> e : m.entrySet() ) {
			K k = e.getKey();
			V x = e.getValue();
			this.put(k, x);
		}	
		return this;
	}
	public TMap<K, V> addOn(K k, V x){
		this.put(k, x);
		return this;
	}
	
	/*	public TMap<K,V> addOn(TMap<K,V> m){
		for ( Map.Entry<K,V> e : m.entrySet() ) {
			K k = e.getKey();
			V x = e.getValue();
			((IAddOn) getC(k)).addOn(x);
		}			
		return this;
	}*/
	public TMap<K,V> plusObjOn(Object m){
		return plusOn((TMap<K,V>) m);
	}
	public TMap<K,V> minusObjOn(Object m){
		return minusOn((TMap<K,V>) m);
	}	
	public TMap<K,V> plusOn(TMap<K,V> m){
		for ( Map.Entry<K,V> e : m.entrySet() ) 
			plusOn(e.getKey(), e.getValue());
		return this;
	}
	
	public TMap<K,V> plusOn(K k, V x){
		((IPlusObjOn) getC(k)).plusObjOn(x);			
		return this;
	}
	public V plusOnGet(K k, V x){
		((IPlusObjOn) getC(k)).plusObjOn(x);			
		return get(k);
	}
	public TMap<K,V> minusOn(TMap<K,V> m){
		for ( Map.Entry<K,V> e : m.entrySet() ) {
			K k = e.getKey();
			V x = e.getValue();
			((IMinusObjOn) getC(k)).minusObjOn(x);
		}			
		return this;
	}	
	public boolean save(String fn){
		//System.out.println("saving dataset to text file "+fn);
		BufferedWriter bw  = FFile.bufferedWriter(fn);
		for ( Map.Entry<K,V> e : entrySet() ) {
			K k = e.getKey();
			V v = e.getValue();
			FFile.write(bw, k+"\t"+v+"\n");
		}
		FFile.flush(bw);	
		FFile.close(bw);
		return true;
	}	
	public K parseKey(String k){		
		return null;
	}
	public V parseValue(String v){		
		return null;
	}

	public void parseLine(String line) {
		String vs[] = FString.split(line,"\t");
		put(parseKey(vs[0]),parseValue(vs[1]));
	}
	
	public boolean loadFile(String fn, String sep){
		//System.out.println("loading dataset from text file"+fn);
		BufferedReader br = FFile.bufferedReader(fn);	
		if (br==null) 
			return false;
		String line = null;
		while ((line = FFile.readLine(br)) != null) {
			String vs[]= FString.split(line,sep);
			put(parseKey(vs[0]),parseValue(vs[1]));
		} 
		FFile.close(br);
		return true;
	}
	
	public boolean loadFile(String fn){
		//System.out.println("loading dataset from text file"+fn);
		BufferedReader br = FFile.bufferedReader(fn);	
		if (br==null) 
			return false;
		String line = null;
		while ((line = FFile.readLine(br)) != null) {
			if (line.startsWith("#")) continue;
			parseLine(line);
		} 
		FFile.close(br);
		return true;
	}
	
	public boolean loadFile(String fn, int col1, int col2, String sep){
		//System.out.println("loading dataset from text file"+fn);
		BufferedReader br = FFile.bufferedReader(fn);	
		if (br==null) 
			return false;
		String line = null;
		while ((line = FFile.readLine(br)) != null) {
			String vs[] = line.split(sep);
			put(parseKey(vs[col1]),parseValue(vs[col2]));
		} 
		FFile.close(br);
		return true;
	}
	public boolean loadFile(String fn, int col1, int col2){
		return loadFile(fn, col1, col2,"\t");
	}


	public TMap<K,V> newInstance(){
		return null;
	}
/*	public K parseKey(String k){
		return null;
	}
	public K parseKey(String k){
		return null;
	}*/

	public  TMap<K,V> loadLine(String line, String cPair, String cSep){
		//TMap<K,V> m = newInstance();		
		for (String item:line.split(cSep)){			
			String v[]= item.split(cPair);
			K k=parseKey(v[0]);
			V x=parseValue(v[1]);
			put(k, x);
		}
		return this;
	}
	
	
	public static class TMapXI<K> extends TMap<K, Integer>{ 
	

		public TMapXI<K> newInstance(){
			return new TMapXI<K>(ck);
		}
		public Integer newValue(){//weakness of Java template
			return 0;
		}


		public Integer parseValue(String v){		
			return Integer.parseInt(v);
		}
	

		public TMapXI(Class ck){
			super(ck, Integer.class);
		}
		public TMapXI(TMap<K, Integer> m){
			super(m);
		}
		public VectorI newVectorValue(){
			return new VectorI();
		}	
		public SetI newSetValue(){
			return new SetI();
		}	
		public TMapXI<K>  plusOn(TMapXI<K> m){
			if (m==null) return this;
			for ( Map.Entry<K, Integer> e : m.entrySet() ) {
				K k = e.getKey();
				Integer v = e.getValue();
				plusOn(k,v);
			}	
			return this;
		}	
		public TMapXI<K> plusOn(K k, Integer x){//TMapXD<K>
			if (this.containsKey(k)){
				put(k, get(k)+x);
			}
			else put(k,x);
			//return get(k);
			return this;
		}	
		public TMapXI<K> plusOn(K k){
			return plusOn(k, 1);		
		}		
	}

	/**
	 * @author nlao
	 *This class is an extension to TMap&lt;String, Integer&gt;
	 */
	public static class MapSI extends TMapXI<String>{//TMap<String, Integer> {//
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapSI newInstance(){
			return new MapSI();
		}
		
		public String newKey(){//needed for primitive classes, silly java
			return null;
		}	
		
		public MapSI(){
			super(String.class);
			//super(String.class, Integer.class);
		}	
		public VectorS newVectorKey(){
			return new VectorS();
		}
		//public MapVectorIS newMapVectorValueKey(){
		//	return new MapVectorIS();	}

		public String parseKey(String k){		
			return k;
		}
		
		
		//TODO: rewrite it in enumeration
		public boolean load(String fn, int icKey, int icValue, boolean b2lowerKey){
			for (String line: FTable.enuLines(fn)){
				String vs[] = line.split("\t");
				String key=vs[icKey];
				if (b2lowerKey)
					key = key.toLowerCase();
				put(key,parseValue(vs[icValue]));
			} 
			return true;
		}
		
		public VectorI subV(VectorS vs){
			return (VectorI) super.subV(vs);
		}
		public VectorI subVIgNull(VectorS vs){
			return (VectorI) super.subV(vs,true);
		}
	}

	

	/**
	 * @author nlao
	 * This class is an extension to TMap&lt;K, Double&gt;
	 */
	public static class TMapXD<K> extends TMap<K, Double>
		implements IPlusObjOn, IMinusObjOn, IGetDoubleByString//, IWrite, IRead 
		, Serializable, Cloneable, IMultiply, IMultiplyOn{//, ICopyable
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD


		public Double getDouble(String name){
			if (name.equals("sum")) return this.sum();
			return null;
		}
		public TMapXD<K> newInstance(){
			return new TMapXD<K>(ck);
		}
		
		public TMapXD(TVector<K> vk, VectorD vx){
			super(vk,vx);
		}
		public Double newValue(){//weakness of Java template
			return 0.0;
		}	

		public VectorD newVectorValue(){
			return new VectorD();
		}	
		
		public Double parseValue(String v){		
			return Double.parseDouble(v);
		}
		
		public TMapXD(Class ck){
			super(ck, Double.class);
		}
		public TMapXD(TMap<K, Double> m){
			super(m);
		}


		public Double sum() {
			Double sum = 0.0;
			for ( Map.Entry<K,Double> e : entrySet() ) {
				// Integer k = e.getKey();
				Double v = e.getValue();
				sum = sum+v;
			}
			return sum;
		}	
		public Double mean(){
			return sum()/size();
		}
		public TMapXD<K> absOn() {
			for ( Map.Entry<K,Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				put(k, Math.abs(v));
			}
			return this;
		}	
		public TMapXD<K> abs() {
			TMapXD<K> m = newInstance();
			for ( Map.Entry<K,Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				m.put(k, Math.abs(v));
			}
			return m;
		}
	/*
		public Double inner(Map<K, Double> m) {
			//TMapXD<K> m1 = multiply(m);
			return multiply(m).sum();
		}		*/
		public TMapXD<K> multiply(Map<K,Double> m) {//kronecker product
			TMapXD<K> m2 = newInstance();
			if (m==null) return m2;
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				if (!m.containsKey(k)) continue;
				m2.put(k, v*m.get(k)) ;
			}
			return m2;
		}		
		public TMapXD<K> devide(Map<K,Double> m) {//kronecker product
			TMapXD<K> m2 = newInstance();
			if (m==null) return m2;
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				if (!m.containsKey(k)) {
					System.err.println("devided by zero");
					continue;
				}
				m2.put(k, v/m.get(k)) ;
			}
			return m2;
		}	
		//
		public TMapXD<K> min(Map<K,Double> m) {
			TMapXD<K> m2 = newInstance();
			if (m==null) return m2;
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				if (!m.containsKey(k)) continue;
				m2.put(k,Math.min(v, m.get(k))) ;
			}
			return m2;
		}	
		//Double
		public TMapXD<K> plusOn(K k, Double x){//TMapXD<K>
			Double d= get(k);
			if (d!=null)
			//if (this.containsKey(k))
				put(k, d+x);		
			else 
				put(k,x);
			return this;
		}	
		
		public Double plusOnGet(K k, Double x){//TMapXD<K>
			Double d= get(k);
			if (d==null){
				put(k,x);
				return x;
			}
			d+=x;
			put(k, d);		
			return d;
		}	

		public TMapXD<K>plusOn(K k){
			return plusOn(k, 1.0);		
		}	
		public TMapXD<K> add(K k){
			put(k, 1.0);
			return this;
		}		
		public TMapXD<K> minusOn(K k, double x){
			if (this.containsKey(k)){
				put(k, get(k)-x);
			}
			else put(k,-x);
			return this;
		}		
		
		//assume x and get(k) be positive
		//return min(get(k), x);
		//minus x from get(k)
		public double truncateOn(K k, double x){
			Double d=get(k);
			if (d==null) return 0;
			if (d>x){
				put(k,d-x);
				return x;		
			}
			remove(k);
			if (d<=0)	return 0.0;
			return d;
		}

		public TMapXD<K> truncateMaxOn(double max){
			for ( Map.Entry<K,Double> e : entrySet() ) {
				double d =e.getValue();
				if (d> max)
					e.setValue(max);
			}
			return this;
		}	

		public TMapXD<K> minusOn(double x){
			for ( Map.Entry<K,Double> e : entrySet() ) 
				e.setValue(e.getValue()-x);		
			return this;
		}
		public TMapXD<K> minus(double x){
			TMapXD<K> m = (TMapXD<K>) this.clone();
			return m.minusOn(x);
		}	

		public TMapXD<K> normalize() {
			TMapXD<K> m= newInstance();
			Double sum = sum();
			for ( Map.Entry<K,Double> e : entrySet() ) 
				m.put(e.getKey(),e.getValue()/sum);		
			return m;
		}	

		public TMapXD<K> normalizeOn() {
			Double sum = sum();
			if (sum==0.0) return this;
			for ( Map.Entry<K,Double> e : entrySet() ) {
				e.setValue(e.getValue()/sum);
			}
			return this;
		}	
		public TMapXD<K> normalizeL1On() {
			Double sum = L1Norm();
			//if (sum==0) return this;
			for ( Map.Entry<K,Double> e : entrySet() ) 
				e.setValue(e.getValue()/sum);		
			return this;
		}	
		public TMapXD<K> normalizeL2On() {
			double d=L2Norm();
			for ( Map.Entry<K,Double> e : entrySet() ) 
				e.setValue(e.getValue()/d);		
			return this;
		}		
		public double L2Norm(){
			return Math.sqrt(mod2());
		}
		public double L1Norm(){
			double sum = 0;
			for ( Map.Entry<K,Double> e : entrySet() ) 
				sum +=Math.abs(e.getValue());		
			return sum;
		}	
	/*	public TMapXD<K> normalize() {
			TMapXD<K> m = newInstance();
			m.from(this);
			//(TMapXD<K>) this.clone();
			return m.normalizeOn();
		}*/
		public TMapXD<K>  plusOn(TMapXD<K> m){
			if (m==null) return this;
			for ( Map.Entry<K, Double> e : m.entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				plusOn(k,v);
			}	
			return this;
		}
		public TMapXD<K>  plusOn(TMapXD<K> m, double scale){
			if (m==null) return this;
			for ( Map.Entry<K, Double> e : m.entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				plusOn(k,v*scale);
			}	
			return this;
		}
		

		

		
		/*public TMapXD<K>  shrinkZeroOn(TMapXD<K> m){
			for ( Map.Entry<K, Double> e : m.entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				if (v==0.0) 
					remove(k);
			}	
			return this;
		}*/
		public TMapXD<K>  shrinkTowardsZero(double L1){
			if (L1<=0){
				System.err.print("L1 <0");
				return null;
			}
			TMapXD<K> m= this.newInstance();
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				if (v> L1)m.put(k,v-L1);
				else if (v<-L1)m.put(k,v+L1);
			}	
			return m;
		}
		public TMapXD<K>  shrinkTowardsZeroOn(double L1){
			if (L1==0.0)
				return this;
			TMapXD<K> m= shrinkTowardsZero(L1);
			this.clear();
			if (m!=null)
				putAll(m);
			return this;
		}
		
		public TMapXD<K>  shrinkTowardsZeroScale(double L1, double scale){
			TMapXD<K> m= this.newInstance();
			if (L1<=0)
				return m;		
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				if (v> L1)m.put(k,(v-L1)*scale);
				else if (v<-L1)m.put(k,(v+L1)*scale);
			}	
			return m;
		}
		public TMapXD<K>  shrinkTowardsZeroScaleOn(double L1, double scale){
			TMapXD<K> m= shrinkTowardsZeroScale(L1,scale);
			this.clear();
			this.putAll(m);
			return this;
		}
		public TMapXD<K> plusObjOn(Object m){
			return plusOn((TMapXD<K>) m);
		}
		public TMapXD<K> minusObjOn(Object m){
			return minusOn((TMapXD<K>) m);
		}	
		public TMapXD<K> plusOn( TVector<K> vk){
			plusOn(vk, 1.0);
			return this;
		}	
		public TMapXD<K> plusOn( Collection<K> v){
			plusOn(v, 1.0);
			return this;
		}	
		public TMapXD<K> minusOn( TVector<K> vk){
			minusOn(vk, 1.0);
			return this;
		}	
		/*public TMapXD<K> plusOn( TVector<K> vk, double d){
			for (K k: vk)
				plusOn(k, d);
			return this;
		}	*/
		public TMapXD<K> plusOn( Collection<K> v, double d){
			for (K k: v)
				plusOn(k, d);
			return this;
		}	
		
		/**
		 * plus on a subsequence. useful in efficient manipulation 
		 */
		public TMapXD<K> plusOn( Vector<K> v, int b, int e, double d){
			for (int i=b; i<e; ++i)
				plusOn(v.get(i),d);
			return this;
		}	
		
		public TMapXD<K> set( Collection<K> v, double d){
			for (K k: v)
				this.put(k, d);
			return this;
		}	
		public TMapXD<K> minusOn( TVector<K> vk, double d){
			for (K k: vk)
				minusOn(k, d);
			return this;
		}		
		public TMapXD<K>  minusOn(TMapXD<K> m){
			if (m==null) return this;
			for ( Map.Entry<K, Double> e : m.entrySet() ) {
				K k = e.getKey();
				double v = e.getValue();
				minusOn(k,v);
			}	
			return this;
		}	
		
		
		public TMapXD<K>  plus(TMapXD<K> m){
			TMapXD<K> m1 = (TMapXD<K>) clone();
			return m1.plusOn(m);	
		}
		public TMapXD<K>  minus(TMapXD<K> m){
			TMapXD<K> m1 = (TMapXD<K>) clone();
			return m1.minusOn(m);	
		}	
		public TMapXD<K>  multiply(Double x){
			TMapXD<K> m = (TMapXD<K>) clone();
			return m.multiplyOn(x);
		}	
		public TMapXD<K>  multiplyOn(Double x){
			for ( Map.Entry<K, Double> e : entrySet() ) 
				e.setValue(e.getValue()*x);		
			return this;
		}	
		public TMapXD<K>  plusOn(Double x){
			for ( Map.Entry<K, Double> e : entrySet() ) 
				e.setValue(e.getValue()+x);		
			return this;
		}	
		public TMapXD<K>  devide(Double x){
			TMapXD<K> m = (TMapXD<K>) clone();
			return m.devideOn(x);
		}	
		public TMapXD<K>  devideOn(Double x){
			for ( Map.Entry<K, Double> e : entrySet() ) 
				e.setValue(e.getValue()/x);		
			return this;
		}	


		double mod2()	{
			double m2=0.0;
			for (double d: values())			
				m2 += d*d;
			return m2;
		}
		public TMapXD<K>  from(Vector<K> vk){		
			for (int i=0; i<vk.size(); ++i) {
				plusOn(vk.get(i), 1.0);
			}	
			return this;
		}	

		public VectorD toVectorAbsValue(){
			VectorD v= newVectorValue();
			v.ensureCapacity(size());
			for ( Map.Entry<K, Double> e : entrySet() ) {
				//K k = e.getKey();			
				v.add(Math.abs(e.getValue()));
			}
			return v;
		}	
		public TMapXD<K>  subAbsLargerThan(Double x){
			return (TMapXD<K>) sub(idxAbsLargerThan(x));
		}	
		public TVector<K>  idxAbsLargerThan(Double x){
			TVector<K> v = this.newVectorKey();
			for ( Map.Entry<K,Double> e : entrySet() ) 
				if (Math.abs(e.getValue())>x)
					v.add(e.getKey()); 
			return v;
		}
		public TMapXD<K> negat(){
			TMapXD<K> m = this.newInstance();
			for ( Map.Entry<K,Double> e : entrySet() ) 
				m.put(e.getKey(), -e.getValue());
			return m;
		}
		public K idxAbsMax(){
			if (size()==0)	return null;		
			double x=-1;
			K i=null;
			for ( Map.Entry<K, Double> e : entrySet() ) {
				K k = e.getKey();
				double v =Math.abs( e.getValue());
				if (v<=x)
					continue;
				x=v;
				i=k;			
			}
			return i;
		}
		
		public boolean save3(String fn){
			//System.out.println("saving dataset to text file "+fn);
			BufferedWriter bw  = FFile.bufferedWriter(fn);
			for ( Map.Entry<K,Double> e : entrySet() ) {
				K k = e.getKey();
				Double v = e.getValue();
				FFile.write(bw, String.format("%s\t%.3e\n"	,k,v));
			}
			FFile.flush(bw);		
			FFile.close(bw);
			return true;
		}
		public String toStringE(String fmt){
			//String oldFmt= FString.fmtDouble;
			return joinE("=", ",", fmt);
		}
		public String joinE(String cpair, String c, String fmt) {
			FString.fmtDouble=fmt;
			return super.join(cpair, c);
		}


	}
	/**
	 * @author nlao
	 *This class is an extension to TMapXD&lt;String&gt;
	 */
	public static class MapSD extends TMapXD<String> implements IGetDoubleByString {
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapSD newInstance(){
			return new MapSD();
		}
		
		public String newKey(){//needed for primitive classes, silly java
			return null;
		}	
		public VectorS newVectorKey(){
			return new VectorS();
		}
		
		
		public MapSD(){
//			super(Integer.class, Double.class);
			super(String.class);
		}	
		public MapSD(TMap<String, Double> m){
			super(m);
		}
		public Double getDouble(String name){
			Double d = super.getDouble(name);
			if (d!=null) return d;
			return get(name);
		}
		public static MapSD fromM3rdLabel(ClassLabel label){
			MapSD m = new MapSD();
			for (String key: (Set<String>) label.possibleLabels())
				m.put(key, label.getWeight(key));
			return m;
		}
		
		/*public static MapSD parse(String line){
			MapSD m = new MapSD();
			m.loadLine(line, ",", "=");
			return m;//Double.parseDouble(v[1]));
		}*/
		public String parseKey(String v){		
			return v;
		}

		public static MapSD fromFile(String fn){
			MapSD m=new MapSD();
			m.loadFile(fn);
			return m;
		}
		public static MapSD fromLine(String line){
			return fromLine(line, "=", " ");
		}
		public static MapSD fromLine(String line, String cSep, String c){
			MapSD m=new MapSD();
			m.loadLine(line, cSep, c);
			return m;
		}
	}

	/**
	 * @author nlao
	 *This class is an extension to TMapXD&lt;Integer&gt;
	 */
	public static class MapID extends TMapXD<Integer>{// implements IUpdateIdx{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapID newInstance(){
			return new MapID();
		}
		
		// speedtup  version of replaceKey().join();
		public String joinReplaceKey(String cpair, String c, VectorS vKey) {
			StringBuffer sb = new StringBuffer();
			int first=1;
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double v = e.getValue();
				if (first==1)	first=0;			
				else	sb.append(c);				
				sb.append(String.format("%s%s%.3f"
					, vKey.get(k)	,cpair, v));///FString.format(v));			
			}
			return (sb.toString());
		}	

		public MapSD replaceKey(VectorS vs){
			MapSD m = new MapSD();
			for (Map.Entry<Integer, Double> e: entrySet())
				m.put(vs.get(e.getKey()), e.getValue());
			return m;
		}
		public Integer newKey(){//needed for primitive classes, silly java
			return 0;
		}	
		public Double newValue(){//weakness of Java template
			return 0.0;
		}
		public VectorI newVectorKey(){
			return new VectorI();
		}

		public Integer parseKey(String k){		
			return Integer.parseInt(k);
		}
		public MapID(){
//			super(Integer.class, Double.class);
			super(Integer.class);
		}	
		public MapID(VectorI vk, VectorD vx){
			super(vk,vx);
		}

		
		public MapID replaceIdx(VectorI vi){	
			MapID m = newInstance();
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				m.put(vi.get(k), x);
			}
			if (m.containsKey(-1)) 
				m.remove(-1);		
			return m;
		}
		public MapID  convolve(MapID m){
			MapID rlt = newInstance();
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				for ( Map.Entry<Integer, Double> e1 : m.entrySet() ) {
					Integer k1 = e1.getKey();
					Double x1 = e1.getValue();
					rlt.plusOn(k1+k,x*x1);
				}
			}	
			return rlt;
		}
		public MapID subSet(Set<Integer> v){
			return (MapID) super.subSet(v);
		}
		public MapID sub(Collection<Integer> v){
			return (MapID) super.sub(v);
		}
		public VectorD subV(Vector<Integer> vi){
			return (VectorD) super.subV(vi);
		}
		
		public Double inner(Vector< Double> m) {
			double d=0;
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double v = e.getValue();
				d+= v*m.get(k);
			}
			return d;
		}	
		public Double inner(double[] m) {
			double d=0;
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double v = e.getValue();
				d+= v*m[k];
			}
			return d;
		}	
		
		public MapID multiply(Vector<Double> m) {
			MapID m2 = newInstance();
			if (m==null) return m2;
			
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double v = e.getValue();
				if (m.get(k)==0) continue;
				m2.put(k, v * m.get(k)) ;
			}
			return m2;
		}
		public MapID  plus(MapID m){
			return (MapID) super.plus(m);	
		}
		public MapID  minus(MapID m){
			return (MapID) super.minus(m);	
		}	
		public MapID  multiply(Double x){
			return (MapID) super.multiply(x);	
		}	
		public MapID  multiplyOn(Double x){
			return (MapID) super.multiplyOn(x);	
		}	
		public MapID  multiplyOn(int x){
			return multiplyOn( (double) x);	
		}	
		public MapID  devideOn(Double x){
			return (MapID) super.devideOn(x);	
		}	
		public MapID  devide(Double x){
			return (MapID) super.devide(x);	
		}	
		public VectorD toVector(int len){
			//return new VectorD(super.toVector(len));
			VectorD v= new VectorD(len, 0.0);//newVectorValue();		v.reset(len);
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				v.set(k,x);
			}
			return v;
		}
		public void keepLargest(){
			if (size()<=1)return;
			int idHigh=idxMax();
			double maxHigh=get(idHigh);
			clear();
			put(idHigh, maxHigh);		
		}

		public void keepLargest2(){
			if (size()<=2)return;
			int id1st=-1;
			int id2nd=-1;
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				if (id2nd==-1)
					id2nd=k;
				else{
					if (get(id2nd)>= x) continue;
					id2nd=k;
				}
				
				if (id1st==-1){
					id1st=id2nd;
					id2nd=-1;
				}
				else{
					if (get(id1st)>=get(id2nd)) continue;
					k=id1st;
					id1st=id2nd;
					id2nd=k;
				}
			}
			
			double d1st=get(id1st);
			double d2nd=get(id2nd);
			clear();
			put(id1st, d1st);		
			put(id2nd, d2nd);		
		}

		
		
		public void addTo(double[] v, double scale ){
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double d= e.getValue();
				v[k]+= d*scale;
			}
			return;
		}
		/**
		 * plus on with id translations 
		 * @param m
		 * @param vidx	dictionary
		 * @param scale
		 * @return
		 */
		public MapID  plusOn(MapID m,VectorI vidx, double scale){
			if (m==null) return this;
			for ( Map.Entry<Integer, Double> e : m.entrySet() ) {
				Integer k = vidx.get(e.getKey());
				double v = e.getValue();
				plusOn(k,v*scale);
			}	
			return this;
		}
		public void disterbByID(double scale){
			for ( Map.Entry<Integer, Double> e : entrySet() ) {
				Integer k = e.getKey();
				Double v = e.getValue();
				e.setValue(v+k*scale);
			}
			return;
		}


		public static MapID fromFile(String fn){
			MapID m=new MapID();
			m.loadFile(fn);
			return m;
		}
		public static MapID fromLine(String line){
			return fromLine(line, "=", " ");
		}
		public static MapID fromLine(String line, String cSep, String c){
			MapID m=new MapID();
			m.loadLine(line, cSep, c);
			return m;
		}

	}
	public static class TMapIX <V>  extends TMap<Integer, V>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public TMapIX<V> newInstance(){
			return new TMapIX<V>(cv);
		}	
		public SetI newSetKey(){
			return new SetI();
		}
		public TMapIX(Class c){
			super(Integer.class,c);
		}
		//need to replace both index
		public TMapIX<V> replaceIdx(VectorI vi){//<Integer> vi){	
			TMapIX<V> m = newInstance();
			for ( Map.Entry<Integer, V> e2 : entrySet() ) {
				Integer k2 = e2.getKey();
				V x = e2.getValue();
				if (vi.get(k2)==-1)	continue;			
				//((IUpdateIdx)x).updateIdx(vi); //should recur??			
				m.put(vi.get(k2), x);
			}
			return m;
		}	
	
		public void updateIdx(VectorI vi){//<Integer> vi){	
			copy(replaceIdx(vi));
		}
		public MapID newMapXD(){
			return  new MapID();
		}
		public MapII newMapKeyI(){
			return  new MapII();
		}
		public Integer parseKey(String k){		
			return Integer.parseInt(k);
		}

		//public MapII newMapXI(){		return  new MapII();	}
	}


	public static class TMapSX<V>  extends TMap<String, V>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public TMapSX<V> newInstance(){
			return new TMapSX<V>(cv);
		}
		public String newKey(){//weakness of Java template
			return null;
		}
		public VectorS newVectorKey(){
			return new VectorS();
		}	
		public TMapSX(Class c){
			super(String.class,c);
		}
		public TMapSX<V>  addKeyPrefix(String prefix){
			TMapSX<V> m = newInstance();
			for ( Map.Entry<String, V> e : entrySet() ) {
				String k = e.getKey();
				V v = e.getValue();
				m.put(prefix + k, v);
			}	
			return m;
		}	

		public SetS newSetKey(){
			return new SetS();
		}	
		public MapSD newMapXD(){
			return  new MapSD();
		}
		public MapSI newMapKeyI(){		
			return  new MapSI();	
		}
		public String parseKey(String v){		
			return v;
		}

	}
	/**
	 * @author nlao
	 *This class is an extension to TMap&lt;Integer, Integer&gt;
	 *
	 */
	public static class MapII extends TMapIX<Integer>{
		//extends TMapXI<Integer> {
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapII newInstance(){
			return new MapII();
		}
		
		public MapII(){
			super(Integer.class);
			//super(Integer.class, Integer.class);
		}	
		public Integer newValue(){//weakness of Java template
			return 0;
		}

		public MapII plusOn(Integer k, Integer x){
			if (containsKey(k))
				put(k, get(k)+x);		
			else 
				put(k,x);
			return this;
		}	
		public MapII plusOn(Integer k){
			return plusOn(k,1);
		}	
		public MapII plusOn(Set<Integer> m){
			for ( Integer k : m ) 
				plusOn(k);
			return this;
		}

		//public Integer newValue(){
		//needed for primitive classes, silly java
			//return 0;	}	
		
		//public SetI newSetValue(){
			//return new SetI();	}
		public MapID toDouble(){
			MapID m = new MapID();
			for (Map.Entry<Integer, Integer>e: this.entrySet())
				m.put(e.getKey(),(double) e.getValue());
			return m;
		}
		public Integer parseValue(String k){
			try{
				return Integer.parseInt(k);
			}
			catch(Exception e){
			}
			return null;
		}
		
		public void  idxSmallerThan(Integer x, TVector<Integer> v){
			v.clear();	//use outside container to avoid creating object
			//v.removeAllElements();
			v.ensureCapacity(size());
			for ( Map.Entry<Integer, Integer> e : entrySet() ) {
				Integer k = e.getKey();
				if (e.getValue()< x)
					v.add(k);
			}
		}
		

		public static MapII fromFile(String fn){
			MapII m=new MapII();
			m.loadFile(fn);
			return m;
		}
		public static MapII fromLine(String line){
			return fromLine(line, "=", " ");
		}
		public static MapII fromLine(String line, String cSep, String c){
			MapII m=new MapII();
			m.loadLine(line, cSep, c);
			return m;
		}
	}

	public static class MapMapIID extends TMapIX<MapID>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapMapIID newInstance(){
			return new MapMapIID();
		}	
		public Integer newKey(){//needed for primitive classes, silly java
			return 0;
		}	
		public MapID newValue(){
			return new MapID();
		}	
		
		public MapMapIID(){
			super(MapID.class);
		}	
		
		public MapMapIID transpose(){
			MapMapIID mm=newInstance();
			for ( Map.Entry<Integer, MapID> e1 : entrySet() ) {
				Integer k1 = e1.getKey();
				MapID  m = e1.getValue();
				
				for ( Map.Entry<Integer, Double> e2 : m.entrySet() ) {
					Integer k2 = e2.getKey();
					Double x = e2.getValue();
					mm.getC(k2).put(k1, x);
				}
			}
			return mm;
		}
	}/*
	public static class MapMapIID extends TMapMapXXD<Integer, Integer>{
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapMapIID newInstance(){
			return new MapMapIID();
		}	
		public Integer newKey(){//needed for primitive classes, silly java
			return 0;
		}	
		public MapID newValue(){
			return new MapID();
		}	
		
		public MapMapIID(){
			super(Integer.class, Integer.class);
		}	
		//public void plusOn(int i, int j, Double x){		
		//getC(i).plusOn(j,x);	}		

		public MapMapIID plusOn(int i, TMapXD<Integer> m){
			getC(i).plusOn(m);
			return this;
		}	
		public MapMapIID minusOn(int i, TMapXD<Integer> m){
			getC(i).minusOn(m);
			return this;
		}		
		public MapMapIID plusOn( TMapXD<Integer> m, int j){
			if (m==null) return this;
			for ( Map.Entry<Integer, Double> e : m.entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				getC(k).plusOn(j,x);
			}		
			return this;
		}			
		public MapMapIID minusOn( TMapXD<Integer> m, int j){
			if (m==null) return this;
			for ( Map.Entry<Integer, Double> e : m.entrySet() ) {
				Integer k = e.getKey();
				Double x = e.getValue();
				getC(k).minusOn(j,x);
			}		
			return this;
		}			
	}*/
	public static class MapSS extends TMapSX< String> {
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		//extends TMap<String, String> {// implements IParseXML {
		//public MapSS clone(){	
		//return (MapSS) super.clone();	}

		public MapSS newInstance(){
			return new MapSS();
		}
		

		public SetS newSetValue(){
			return new SetS();
		}
		
		public  MapSS parseXMLAttrbuteContent(
				Element e, String tag, String attribute){
			clear();
			NodeList v = e.getElementsByTagName(tag);
			for (int i = 0; i < v.getLength(); i++) {
				Element e1 = (Element) v.item(i);
				String k= e1.getAttribute(attribute);
				String x =  e1.getTextContent().trim();
				this.put(k,x);
			}		
			return this;	
		}
		public MapSS(){
			super(String.class);
		}	
		public String parseValue(String v){		
			return v;
		}
		
		public static MapSS fromFile(String fn, int col1, int col2, String sep){
			MapSS m=new MapSS();
			m.loadFile(fn,col1,col2,sep);
			return m;
		}
		public static MapSS fromFile(String fn){
			return fromFile(fn,"\t");
		}

		public static MapSS fromFile(String fn, String sep){
			MapSS m=new MapSS();
			m.loadFile(fn,sep);
			return m;
		}
		public static MapSS fromLine(String line){
			return fromLine(line, "=", " ");
		}
		public static MapSS fromLine(String line, String cSep, String c){
			MapSS m=new MapSS();
			m.loadLine(line, cSep, c);
			return m;
		}
	}
	public static class TMapMapIIX<V>  extends TMapIX<TMapIX<V> >{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public Class cv1;
	public TMapMapIIX<V> newInstance(){
		return new TMapMapIIX<V>(cv1);
	}
	public TMapMapIIX(Class c){
		super((new TMapIX<V>(c)).getClass());
		this.cv1 = c;
	}
	public TMapIX<V>  newValue(){//weakness of Java template
		return new TMapIX<V>(cv1);
	}	
	public TMapMapIIX<V> transpose(){
		TMapMapIIX<V> mm=newInstance();
		for ( Map.Entry<Integer, TMapIX<V>> e1 : entrySet() ) {
			Integer k1 = e1.getKey();
			TMapIX<V>  m = e1.getValue();
			//if (k1==-1) continue;
			
			for ( Map.Entry<Integer, V> e2 : m.entrySet() ) {
				Integer k2 = e2.getKey();
				V x = e2.getValue();
				//if (k2==-1)	continue;
				//mm.put(k2,k1, x);
				mm.getC(k2).put(k1, x);
			}
		}
		return mm;
	}
	public boolean contains(Integer k1, Integer k2){
		if (!containsKey(k1)) return false;
		if (!get(k1).containsKey(k2)) return false;
		return true;
	}	
	public TMapMapIIX<V> put(Integer k1, Integer k2, V x){
		getC(k1).put(k2,x);		
		return this;
	}

	public TVector<V> getV() {
		TVector<V>  v = new TVector<V> (this.cv1);
		for (int id: this.keySet())
			v.addAll(this.get(id).values());
		return v;
	}
	public V get(int i, int j) {
		TMapIX<V> m = get(i);
		if (m==null)
			return null;
		return m.get(j);
	}
	public boolean containsKey(int i,int j){
		return get(i,j)!=null;		
	}
	public V getC(int i, int j) {
		return getC(i).getC(j);
	}

	public void updateIdx(VectorI vi){//<Integer> vi){	
		super.updateIdx(vi);
		for (TMapIX<V> m: this.values()){
			m.updateIdx(vi);
		}
	}

}

	public static class MapIIb extends TMapXI<Integer>{//TMap<Integer, Integer> {
		private static final long serialVersionUID = 2008042701L; // YYYYMMDD
		public MapIIb newInstance(){
			return new MapIIb();
		}
		
		public Integer newValue(){//needed for primitive classes, silly java
			return 0;
		}	
		
		public MapIIb(){
			super(Integer.class);
			//super(Integer.class, Integer.class);
		}	


	}

}
