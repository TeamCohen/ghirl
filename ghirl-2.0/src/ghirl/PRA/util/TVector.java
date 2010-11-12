package ghirl.PRA.util;

import ghirl.PRA.util.Interfaces.ICloneable;
import ghirl.PRA.util.Interfaces.IGetDoubleByString;
import ghirl.PRA.util.Interfaces.IGetIntByString;
import ghirl.PRA.util.Interfaces.IGetObjByString;
import ghirl.PRA.util.Interfaces.IGetObjByStringInt;
import ghirl.PRA.util.Interfaces.IGetStringByInt;
import ghirl.PRA.util.Interfaces.IGetStringByString;
import ghirl.PRA.util.Interfaces.IMultiplyOn;
import ghirl.PRA.util.Interfaces.IParseXML;
import ghirl.PRA.util.Interfaces.IPlusObj;
import ghirl.PRA.util.Interfaces.IPlusObjOn;
import ghirl.PRA.util.Interfaces.IRead;
import ghirl.PRA.util.Interfaces.ISetDoubleByString;
import ghirl.PRA.util.Interfaces.IWrite;
import ghirl.PRA.util.TMap.MapID;
import ghirl.PRA.util.TMap.MapIIb;
import ghirl.PRA.util.TMap.MapSI;
import ghirl.PRA.util.TMap.TMapVectorXI;
import ghirl.PRA.util.TMap.TMapXD;
import ghirl.PRA.util.TMap.TMapXI;
import ghirl.PRA.util.TSequence.PipeXS;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


/**
 * This class is an extension to Vector&lt;V&gt;
 * @author nlao
 * 
 */
public class TVector<T> extends Vector<T> 
	implements IWrite, IRead, Serializable
	, Cloneable, ICloneable, IGetIntByString{
	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
	public Class c=Object.class;
	//public TVector() {}

	
	public TVector<T> newInstance(){
		return new TVector<T>(c);
	}
	public TVector<T> newInstance(int n){
		return new TVector<T>(n, c);
	}	
	
	public T newValue(){//weakness of Java template		
		try{
			Object o=c.newInstance();
			return (T) (o);
		}
		catch (Exception e){
			System.out.println(e.getClass().getName());
			e.printStackTrace();			
		}
		return null;
	}
	
	public TVector<T> plusOn(TVector<T> v){
		if (size()!=v.size())
			System.err.println("unmatched vector size");
		
		//Math.min(size(), m.size())
		for (int i=0; i<v.size(); ++i) 
			((IPlusObjOn) getE(i)).plusObjOn(v.get(i));	
		//	get(i).plusOn(m.get(i));
		return this;
	}
	public TVector<T> plus(TVector<T> v){
		if (size()!=v.size())
			System.err.println("unmatched vector size");
		
		TVector<T> v1= newInstance();
		v1.ensureCapacity(v.size());
		
		for (int i=0; i<v.size(); ++i){
			IPlusObj o=(IPlusObj) getE(i);
			if (o==null)
				v1.add(null);	//Might regret about this
			else
				v1.add(	(T)o.plusObj(v.get(i)));
		}
		//	get(i).plusOn(m.get(i));
		return v1;
	}
	
	
	public TVector<T> keepRight(int n){
		if (n>=this.size())
			return this;
		this.removeRange(0, size()-n);
		return this;
	}	
	public Integer getInt(String name){
		if (name.equals(CTag.size)) return size();
		return null;
	}
	public T getS(int i){//smart version of get
		if (i<0 || i>= size()) return null;
		return get(i);
	}
	public T getN(Integer i){//smart version of get
		if (i==null) return null;
		return get(i);
	}
	
	public T getC(int i){//get or create
		if (get(i)!=null) return get(i);
		return set(i, newValue());
	}
	public TVector<T> initAll(){
		for (int i=0; i<size();++i){
			this.set(i, (T)  newValue()) ;
		}		
		return this;
	}

	
	public TVector<T> initNull(){
		for (int i=0; i<size();++i){
			if (get(i)!=null) continue;
			set(i, (T)  newValue()) ;
		}		
		return this;
	}
	
	//get and extend with default value if not exist
	public T getE(int i){//TVector<V>
		return extend(i+1).get(i);
	}
	public TVector<T> setE(int i,T x){//TVector<V>
		extend(i+1);
		set(i,x);
		return this;
	}
	
	public TVector<T> setE(int i,T x, T dft){//TVector<V>
		extend(i+1, dft);
		set(i,x);
		return this;
	}
	
	public TVector<T> extend(int n){
		int k= size();
		if (k>=n) 
			return this;
		
		setSize(n);
		for (int j=k; j<n;++j)
			set(j, newValue()) ;
		return this;
	}
	public TVector<T> extend(int n, T x){//TVector<V>
		int k= size();
		if (k>=n) 
			return this;
		
		setSize(n);
		for (int j=k; j<n;++j)
			set(j, x) ;
		return this;
	}
	public TVector<T> addN(int n, T x){//TVector<V>
		int k= size();
		setSize(k+n);
		for (int j=k; j<k+n;++j)
			set(j, x) ;
		return this;
	}
	public TVector(Class c) {
		this.c = c;
	}
	// will pre allocate memory
	public TVector(Collection<T> v){
		this.c = v.iterator().next().getClass();
		addAll(v);
	}
	// will incrementally allocate memory
	public TVector(Iterable<T> v){
		this.c = v.iterator().next().getClass();
		addIterable(v);
	}
	public TVector(int k, Class c) {
		this.c = c;
		setSize(k);		
		//initAll();
	}	
	public TVector(T[] v) {
		this.c = v.getClass();
		addAll(v);
	}
	public TVector(T x) {
		this.c = x.getClass();
		add(x);
	}

	public TVector<T> load(Collection<T> v)	{
		clear();
		this.addAll(v);
		return this;
	}	
	public TVector<T> addAll(T[] v)	{
		this.ensureCapacity(size()+ v.length);
		for( T x: v) add(x);
		return this;
	}
/*	already implemented by java
 * public TVector<T> addCollection(Collection<T> v){
		this.ensureCapacity(size()+ v.size());
		for( T x: v) add(x);
		return this;
	}*/
	public TVector<T> addIterable (Iterable<T> v){
		for (T x : v)	add(x);		
		return this;
	}
	public TVector<T> setAll(T[] v)	{
		for (int i=0; i<v.length; ++i)
			this.set(i,v[i]);
		return this;
	}


	public TVector<T> addOn(T x)	{
		add(x);
		return this;
	}

	//copy without duplicate the content
	public TVector<T> from(T[] v)	{
		clear();	
		addAll(v);
		return this;
	}


	public TVector(int k, T x) {
		this.c = x.getClass();
		addAll(Collections.nCopies(k, x));
	}

	//copy and ducplicate the content
	public TVector<T> copyShallow(TVector<T> v) {
		clear();
		addAll(v);
		/*try{
			for (V x: v)
				add((V)  ((ICopyable)x).copy());
		}
		catch(Exception e){
			addAll(v);
		}*/
		return this;
	}
	public TVector<T> clone() {
		TVector<T> v = newInstance();
		v.ensureCapacity(size());
		try{
			for (T x: this)
				v.add((T)	((ICloneable)x)	.clone()	);
		}
		catch(Exception e){
			//e.printStackTrace();
			//System.exit(-1);
			v.clear();
			v.addAll(this);
		}
		return v;
		//return newInstance().copyShallow(this);
	}	
	public void init(int n){
		setSize(n);
		initAll();
	}
	public TSet<T> toSet (){
		TSet<T> m = newSetValue();
		for (T x : this)
			if (x!=null)
				m.add(x);		
		return m;
	}	

	public TVector<T> parseXML(Element e, String tag){
		clear();
		NodeList v = e.getElementsByTagName(tag);
		for (int i = 0; i < v.getLength(); i++) {
			T x = newValue();
			( (IParseXML) x).parseXML((Element) v.item(i));
			add( x);
		}		
		return this;
	}
	
	public int idxMin(){
		T x=null;
		int i=-1;
		for (int j=0; j<size(); ++j){
			T y = get(j);
			if (x!=null) 
				if ( ((Comparable) x).compareTo(y) <=0)
					continue;
			x=y;i=j;			
		}
		return i;
	}
	public int idxMax(){
		T x=null;
		int i=-1;
		for (int j=0; j<size(); ++j){
			T y = get(j);
			if (x!=null) 
				if ( ((Comparable) x).compareTo(y) >=0)
					continue;
			x=y;i=j;			
		}
		return i;
	}
	public T min(){
		int i=idxMin();
		if (i>=0)	return get(i);
		return null;
	}
	public T max(){
		int i=idxMax();
		if (i>=0)	return get(i);
		return null;
	}

	public VectorI  sortId()	{
		return (VectorI) toMapVectorValueId().toVectorV();
	}	
	public TMapVectorXI<T> newTMapVectorXI(){
		return  new TMapVectorXI<T>(c);
	}
	public TMapVectorXI<T> toMapVectorValueId(){
		TMapVectorXI<T> m =newTMapVectorXI();
		for (int i=0; i<size();++i)	
			m.insert(get(i), i);		
		return m;
	}
	public TMapXI<T> newMapValueId(){
		return new TMapXI<T>(c);
	}
	public TMapXI<T> toMapValueId(){
		TMapXI<T> m = newMapValueId();
		for (int i=0; i<size();++i)
			if (get(i)!=null)
				m.put(get(i), i);		
		return m;
	}
		
	public T clone(int i) {
		return (T) ((ICloneable) get(i)).clone();
	}
	public TVector<T> except(int i) {
		return except(i,i+1);
	}
	public TVector<T> except(int b,int e) {
		TVector<T> v = newInstance();
		for (int i=0; i<size(); ++i)
			if (i<b || i>=e)
				v.add(get(i));
		return v;
	}
	public TVector<T> sub(int b) {
		return sub(b,size());
	}
	public TVector<T> sub(int b, int e) {
		//if (e>size()) e= size();
		//if (b<0 || e<0|| e-b<0)		return null;
		
		TVector<T> v = newInstance();
		if ( e-b<0)		return v;
		b=Math.max(0,b);
		e= Math.min(size(), e);
		v.ensureCapacity(e - b);
		for (int i = b; i < e && i<size(); ++i)
			//v.set(i-b,get(i));
			v.add(get(i));// TODO: clone or get?
		return v;
	}
	public TVector<T> truncateOn(int n) {
		if (size()>n)
			this.setSize(n);
		return this;
	}
	
	public TVector<T> left(int n) {
		return sub(0,n);
	}
	public TVector<T> right(int n) {
		return mid(size()-n);
	}	
	public TVector<T> mid(int b) {
		return sub(b, size());
	}
	public TVector<T> mid(int b, int n) {
		return sub(b, b+n);
	}	
	public T pop() {
		if (size()==0) return null;
		T x = this.lastElement();
		trim(1);
		return x;
	}	
	public T popFrontOn() {
		if (size()==0) return null;
		T x = this.firstElement();
		for (int i=0; i<size()-1; ++i)
			this.set(i, get(i+1));
		trim(1);
		return x;
	}	
	public TVector<T> pop(int k) {
		TVector<T> v = right(k);
		trim(k);
		return v;
	}
	public VectorS toVectorS() {
		VectorS vs = new VectorS();//size());
		vs.ensureCapacity(size());
		for (T x :this)
			if (x==null)
				vs.add(null);
			else
				vs.add(x.toString());
		return vs;
	}	
	public TVector<T> trim(int k) {
		
		this.setSize( Math.max(0,size()-k));
		return this;
	}		
	public TVector<T> set(Collection<Integer> vi, T x) {
		for (int i:vi)
			set(i, x);		
		return this;
	}	
	public TVector<T> set(Vector<Integer> vi, Vector<T> vx) {
		if (vi.size()!=vx.size()){
			System.err.println("vi, vx size differ");
			System.exit(-1);
		}
		for (int i=0; i<vi.size();++i)
			set(vi.get(i), vx.get(i));		
		return this;
	}
	public TVector<T> set(TMap<Integer,T> m) {
		for ( Map.Entry<Integer, T> e : m.entrySet() ) 
			set(e.getKey(),e.getValue());		
		return this;
	}	
	
/*	public TVector<V> set(Collection<Integer> vi, Collection<V> vx) {
		for (int i=0; i<vi.size();++i)
			set(i, vx.(i));		
		return this;
	}	*/

	public TVector<T> setRange(int ib, int ie, T x) {
		for (int i=ib; i<ie; ++i)
			set(i, x);		
		return this;
	}	

	
	public TVector<T> setAll( T x) {
		for (int i=0; i<size(); ++i)
			set(i, x);		
		return this;
	}	
	public TVector<T> reset(T vx[]) {
		clear();
		addAll(vx);
		return this;
	}
	public TVector<T> reset(int n, T x) {
		this.setSize(n);
		setAll(x);
		return this;
	}	
	public TSet<T> newSetValue(){
		return new TSet<T>(c);
	}
	public TSet<T> subSet(Collection<Integer> vi) {
		TSet<T> v = newSetValue();
		for (int i : vi)	
			//if (get(i)>=0)
				v.add(get(i)); 
		return v;
	}

	public TVector<T> sub(Collection<Integer> vi) {
		TVector<T> v = newInstance();
		if (vi==null)
			return v;

		v.ensureCapacity(vi.size());

		for (Integer i : vi)		
			if (i!=null)
				v.add(get(i)); // TODO: clone or get?
			else
				v.add(null);
		return v;
	}

	public TVector<T>  intersect(Collection<T> m){
		TVector<T> v= newInstance();
		for ( T x : this) {
			if (m.contains(x))
				v.add(x);
		}
		return v;
	}	

	
	//TODO:?
	//public TMap<V, V1> sub(TMap<V, V1> m) {
/*	public TMap<V,Object> newMapValueObj(){
		
	}
	public TMap<V, Object> sub(TMap<Integer, Object> m) {
		TMap<V,Object> m1 = newMapValueObj();
		for ( Map.Entry<Integer, Object> e : m.entrySet() ) {
			Integer k = e.getKey();
			Object x = e.getValue();
			m1.put(k, ((IGetDoubleByInt)x).getDouble(id));
		}
		return m1; 
	}*/


	public TVector<T> subByMask(Vector<Integer> vi) {
		if (vi.size()!=size())
			System.err.print("unmatched dimension");
		TVector<T> v = newInstance();
		v.ensureCapacity(vi.size());
		for (int i=0; i<vi.size(); ++i)
			if (vi.get(i)>0)
				v.add(get(i));		
		return v;
	}
	public TVector<T> subRand(double p) {
		TVector<T> v = newInstance();
		v.ensureCapacity( (int)(size()*p)+10);
		for(T x: this)
			if (FRand.drawBoolean(p))
				v.add(x);
		return v;
	}

	
	//find a subsequence v 
	public int findSeq(Vector<T> v) {
		return findSeq(v,0);
	}
	public VectorI findAny(Set<T> v) {
		VectorI vi = new VectorI();
		for (int i=0; i<size(); ++i)
			if (v.contains(get(i)))
				vi.add(i);
		return vi;
	}
	//find a subsequence v starting from ib 
	public int findSeq(Vector<T> v, int ib) {
		for (int i=ib; i<size()-v.size(); ++i){
			int j=0;
			for (;j<v.size(); ++j){
				if (!get(i+j).equals( v.get(j)))
					break;
			}
			if (j!=v.size()) continue; //failed matching
			return i;
		}
		return -1;
	}

	public int findExact(T x){
		for (int i=0; i<size(); ++i){
			if (get(i).equals(x)) return i; 
		}
		return -1;
	}
	public int idxFirst(Set<T> v){
		for (int i=0; i<size(); ++i){
			if (v.contains(get(i))) return i; 
		}
		return -1;
	}
	public int idxFirst(T x){
		for (int i=0; i<size(); ++i){
			if (get(i).equals(x)) return i; 
		}
		return -1;
	}
	
	//almost the same as addAll(), except returning itself
	public TVector<T> catOn(Collection<T> v) {
		this.ensureCapacity(size() + v.size());
		this.addAll(v);
		return this;
	}
	public TVector<T> replace(T x, Vector<T> v)  {
		int id = findExact(x);
		if (id <0) 
			return null; 
		TVector<T> v1 =left(id).catOn(v).catOn(mid(id+1));
		return v1;
	}
	
	//replace all occurences of sequence with a single element
	public TVector<T> replace(Vector<T> v, T x )  {
		int idx=0;
		for(;true;){
			//to find
			idx= findSeq(v, idx);
			if (idx<0) break;			
			replace(idx, v.size(), x);
		}		
		return null;
	}

	//replace an subsequence with a single element
	public TVector<T> replace(int ib, int len, T x )  {
		//int id = findExact(x);
		//if (id <0)	return null; 
		return left(ib).pushBackOn(x).catOn(mid(ib+len));
	}
	
	public TVector<T> replaceOn(T x, T y)  {
		//int id = find(x);set(id, y);
		for (int i=0; i<size(); ++i){
			if (get(i).equals(x))set(i, y); 
		}		
		return this;
	}
	public TVector<T> replace(T x, T y)  {
		return ((TVector<T> )clone()).replaceOn(x, y);
	}
	public BufferedWriter write(BufferedWriter writer){// throws IOException {
		for (T x : this) {			
			//writer.write(x.toString());
			((IWrite) x).write(writer);
		}
		return writer;
	}
	public BufferedReader read(BufferedReader reader) {//throws IOException {
		for (T x : this) {
			//String s; Integer i; i.
			//reader.read(x);
			((IRead) x).read(reader);
		}
		return reader;
	}
	public TVector<T>  reverseOn() { // this.reverse();
		int m = size() / 2;
		for (int i = 0; i < m; ++i) {
			int j = size() - i - 1;
			T x = get(i);
			set(i, get(j));
			set(j, x);
		}
		return this;
	}
	public TVector<T>  reverse() { // this.reverse();
		TVector<T> v = newInstance();
		v.ensureCapacity(size());
		for (int i =1; i<=size();++i)		
			v.add(get(size()-i));
		return v;
	}
	

	public VectorI getVI(String name){
		VectorI v = new VectorI(size());
		for (int i=0; i<size(); ++i){
			Integer s = ((IGetIntByString) get(i)).getInt(name);
			v.set(i,s);			
		}
		return v;		
	}
	public int sum(String name){
		int n=0;
		for (T x: this)
			n+= ((IGetIntByString)x).getInt(name);
		return n;		
	}
	public TVector<Object> getVO(String name){
		TVector<Object> v = new TVector<Object>(Object.class);
		v.ensureCapacity(size());
		for (int i=0; i<size(); ++i){
			Object x = ((IGetObjByString) get(i)).getObj(name);
			v.add(x);			
		}
		v.c = v.firstElement().getClass();
		return v;		
	}
	public TVector<Object> getVO(String name, int id){
		TVector<Object> v = new TVector<Object>(Object.class);
		v.ensureCapacity(size());
		for (int i=0; i<size(); ++i){
			Object x = ((IGetObjByStringInt) get(i)).getObj(name,id);
			v.add(x);			
		}
		v.c = v.firstElement().getClass();
		return v;		
	}
	public TVector<Object> getVO(String name, Collection<Integer> vi) {
		TVector<Object> v = new TVector<Object>(Object.class);
		v.ensureCapacity(vi.size());
		for (int i:vi) {
			Object x = ((IGetObjByString) get(i)).getObj(name);
			v.add(x);			
		}
		return v;
	}
	
	public VectorS getVS(String name, Collection<Integer> vi){
		VectorS v = new VectorS();//vi.size());
		v.ensureCapacity(vi.size());
		for (int i:vi) {
			String s = ((IGetStringByString) get(i)).getString(name);
			//v.set(i, s);
			v.add(s);
		}			
		return v;
	}
	public VectorI getVI(String name, Collection<Integer> vi){
		VectorI v = new VectorI();//vi.size());
		v.ensureCapacity(vi.size());
		for (int i:vi) 
			v.add(  ((IGetIntByString) get(i)).getInt(name));		
		return v;
	}

	public VectorS getVS(String name, int[] vi) {
		VectorS v = new VectorS();
		v.ensureCapacity(vi.length);
		for (int i = 0; i < vi.length; ++i) {
			if (vi[i]>=0)
			v.add(((IGetStringByString)  get(vi[i]))
					.getString(name));			
		}			
		return v;
	}
	public VectorS getVS() {
		VectorS v = new VectorS(size());
		for (int i=0; i<size(); ++i){
			v.set(i,get(i).toString());			
		}
		return v;
	}	
	

	public VectorS getVS(String name) {
		VectorS v = new VectorS(size());
		for (int i=0; i<size(); ++i){
			String s = ((IGetStringByString) get(i)).getString(name);
			v.set(i,s);			
		}
		return v;
	}	
	
	public SequenceS enuString(String name){//SequenceS
		return new SequenceS(this, new PipeXS<T>(name));
	}
	

	public TVector<T> reorder(VectorI  vi){		 
		TVector<T> v= newInstance();
		v.ensureCapacity(vi.size());
		for (int i: vi)
			v.add(get(vi.get(i)));		
		return v;
	}
	public VectorD getVD(String name){
		VectorD v = new VectorD(size());
		for (int i=0; i<size(); ++i){
			IGetDoubleByString o=(IGetDoubleByString) get(i);
			if (o!=null)
				v.set(i,o.getDouble(name));			
		}
		return v;		
	}
	public VectorD getVD(String name, Collection<Integer> vi) {
		VectorD v = new VectorD();
		v.ensureCapacity(vi.size());
		for (int i:vi) 
			v.add( ((IGetDoubleByString) get(i)).getDouble(name));			
		return v;
	}

	public TVector<T> setVD(String name, Vector<Double> vd){
		if (size()!= vd.size()){
			System.err.println("uneven vector length");
			return null;
		}
		for (int i=0; i<size(); ++i)
			((ISetDoubleByString) get(i)).setDouble(name, vd.get(i));		
		return this;		
	}
	public TVector<T> setVD(String name, Vector<Integer> vi, Vector<Double> vd){
		if (vi.size()!= vd.size()){
			System.err.println("uneven vector length "
					+vi.size() +" and "+ vd.size());
			return null;
		}
		for (int i=0; i<vi.size(); ++i)
			((ISetDoubleByString) get(vi.get(i))).setDouble(name, vd.get(i));			
		return this;
	}
	public TVector<T> setMD(String name, Map<Integer,Double> m){
		for ( Map.Entry<Integer,Double> e : m.entrySet() ) 
			((ISetDoubleByString) get(e.getKey()))
				.setDouble(name, e.getValue());			
		return this;
	}
	public VectorS getVS(int id) {
		VectorS v = new VectorS(size());
		for (int i=0; i<size(); ++i){
			String s = ((IGetStringByInt) get(i)).getString(id);
			v.set(i,s);			
		}
		return v;
	}




	public TVector<T> shrinkUnique()	{
		return toSet().toVector();
	}
	//remove all occurence of value x 
	public TVector<T> removeValue(T x)	{
		TVector<T> v = newInstance();
		for (int i = 0; i < size(); ++i) {
			if (((Comparable)get(i)).compareTo(x)==0)
				continue;			
			v.add(get(i));
		}
		return v;
	}	
	public TVector<T> shrinkRepeated()	{
		TVector<T> v = newInstance();
		for (int i = 0; i < size(); ++i) {
			if (i==0) {
				v.add(get(i));
				continue;
			}
			if (((Comparable)get(i)).compareTo(get(i-1))==0)
				continue;			
			v.add(get(i));
		}
		return v;
	}
	public TVector<T> shrinkRepeatedOn()	{
		clear();
		addAll(shrinkRepeated());
		return this;
	}	
	public TVector<T> sort()	{
		return this.toSet().toVector();
	}

	public TVector<T> sortOn()	{
		load(sort());		
		return this;
	}		
	/** 
	 * assuming v is sorted in ascending order
	 * @param x
	 * @return  i if x <= v[i], else v.size()
	 */
	public int findSorted(T x){
		if (size()==0)
			return 0;

		int l=0;	// v[l] is always < x
		int r=size()-1;	// v[r] is always > x
		if (((Comparable)x).compareTo(get(0)) <=0)	return 0;
		if (((Comparable)x).compareTo(get(r)) >0)	return size();

		int m = r/2;
	  for (;r-l>1;) {
	  	int c= ((Comparable)x).compareTo(get(m));
	  	if (c==0) return m;
	  	if (c>0) 	
	  		l=m; 
	  	else	
	  		r=m; 
			m=(l+r)/2;
	  }
		return r;
	}	
	public VectorI compareTo(T x){
		VectorI v = new VectorI(size());
		for (int i=0; i<size(); ++i){
			v.set(i,((Comparable)get(i)).compareTo(x));			
		}
		return v;		
	}
	public VectorI idxIn(Set<T> m){
		VectorI v = new VectorI();
		for (int i=0; i<size(); ++i){
			if (m.contains(this.get(i)))
				v.add(i);
		}
		return v;
	}
	public VectorI idxOut(Set<T> m){
		VectorI v = new VectorI();
		for (int i=0; i<size(); ++i){
			if (! m.contains(this.get(i)))
				v.add(i);
		}
		return v;
	}
	public VectorI maskIn(Set<T> m){
		VectorI v = new VectorI();//this.size());//compareTo(x);
		for (T x: this){
			if (m.contains(x))
				v.add(1);
			else
				v.add(0);
		}
		return v;
	}
	public VectorI idxEqualToInt(int x) {
		VectorI v=new  VectorI();
		v.ensureCapacity(size());
		for (int i=0; i<size(); ++i){
			int c=((Comparable)get(i)).compareTo(x);
			if (c==0) v.add(i);
		}
		return v;
	}

	
	public VectorI idxNEqualToInt(int x) {
		VectorI v=new  VectorI();
		v.ensureCapacity(size());
		for (int i=0; i<size(); ++i){
			int c=((Comparable)get(i)).compareTo(x);
			if (c!=0) v.add(i);
		}
		return v;
	}
	
	/**
	 * transfrom {z,x,x,y,y,y} to {z->{0}, x->{1,2}, y->{3,4,5}
	 * 
	 */
	public TMap<T, VectorI> idxEqualToX() {
		TMap<T, VectorI> mv = new TMap<T, VectorI>(c, VectorI.class);
		for (int i=0; i<size(); ++i)
			mv.getC(get(i)).add(i);			
		return mv;
	}		
	public VectorI idxEqualTo(T x) {
		return compareTo(x).idxEqualToInt(0);
	}	
//	public VectorI idxByGet(String name, String value){	
//		return getVString(name).idxEqualTo(value);	}
	
	public VectorI idxLargerThan(T x) {
		return compareTo(x).idxEqualToInt(1);
	}	
	public VectorI idxSmallerThan(T x) {
		return compareTo(x).idxEqualToInt(-1);
	}	
	public String joinIndexed(String cPair, String cInst){
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			if (i > 0) sb.append(cInst);
			sb.append(i).append(cPair)
				.append(FString.format(get(i)));
		}
		return (sb.toString());		
	}
	public String joinIndexed(){
		/*StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			sb.append(String.format(
					"(%d) %.2f ",i, get(i)));
		}*/
		return joinIndexed("="," ");		
	}
	
	
		
	public String join(String c, int ib, int ie) {
		ie= Math.min(ie, size());
		StringBuffer sb = new StringBuffer();
		for (int i = ib; i < ie; i++) {
			if (i > ib)
				sb.append(c);
			if (get(i)!=null)
				sb.append(FString.format(get(i)));
		}
		return (sb.toString());
	}
	public String join(String c, int ib) {
		return join(c, ib,size());
	}
	public String join(String c) {
		return join(c, 0);
	}
	public String joinIndented( String sIndent){//int nIndent) {
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < size(); i++) {
			sb.append(sIndent//FString.repeat(" ",nIndent)
					+FString.format(get(i))+"\n");
		}
		return (sb.toString());
	}
	public  String toString() {
    return join(" ");
  }


  public synchronized String toString(String c) {
    return join(c);
  }
  

	/**
	 * remove the elements e that 
	 * e.getString(key) is contained in ms 
	 * @param key
	 * @param ms
	 */
	public TVector<T> removeByString( String key, Set<String> ms ) {
		TVector<T> v = newInstance();
		v.ensureCapacity(size());
		for ( T x : this) {
			if ( ms.contains( ((IGetStringByString)x).getString(key)))
				continue;
			v.add(x);			
		}
		return v;
/*		TSet<V> m = new TSet<V>(c);
		for ( V x : this) {
			if ( ms.contains( ((IGetStringByString)x).getString(key))) {
				m.add(x);
			}
		}
		removeAll(m);*/
	}
	public TVector<T> remove( Set<T> m ) {
		return sub(idxOut(m));
	}
	public void removeByStringOn( String key, Set<String> ms ) {
		TVector<T> v =removeByString(key, ms);
		this.clear();
		this.addAll(v);
	}	
	
	public TVector<T> pushFrontOn( T x ) {
		add(null);		
		for (int i=size()-1; i>=1; --i){
			set(i,get(i-1));
		}
		set(0,x);	
		return this;
	}
	public TVector<T> pushBackOn( T x ) {
		add(x);		
		return this;
	}
	
	// every inefficient implementation
	public TVector<T> insertSortedOn(T x) {
		//this.find(x);
		add(x);
		int i=size()-2;
		for (; i>=0; --i){
			if (((Comparable)x).compareTo(get(i)) != -1) break;
			//if (get(i) <= x) break;
			set(i+1,get(i));
		}
		set(i+1,x);
		return this;
	}
	
	/**
	 * insert x to the j-th position
	 * keep at most n elements 
	 */
	public TVector<T> insertTruncate(T x, int j, int n) {
		if (j>=n) 
			return this;	//no need to insert
		
		if (size()<n)
			add(x);		
		
		if (size()>n)
			System.err.print("insertTruncate() exceed max length");
		
		for (int i=size()-2; i>=j; --i){//scooping
			set(i+1,get(i));
		}
		set(j,x);
		return this;
	}
	

	public T sum(){
		T x = this.newValue();
		for (T y: this){
			if (y!= null)
				((IPlusObjOn)x).plusObjOn(y);
		}
		return x;
	}
	public T mean(){
		//V x
		if (size()==0) return null;
		if (size()==1) return get(0);
		//clone(0);//TODO should clone?
		
		T x=sum();
		return (T) ((IMultiplyOn)x).multiplyOn(1.0/size());
	}



	/**
	 * 0 is the right most one
	 * * @param i
	 * @return
	 */
	public T getRight(int i){
		return get(size()-i-1);
	}
	public String joinS(Vector<String> v, String cpair, String c) {
		StringBuffer sb = new StringBuffer();
		int first=1;
		for (int i = 0; i < size(); ++i) {
			if (first==1)	first=0;			
			else	sb.append(c);				
			sb.append(FString.format(get(i))	+cpair+v.get(i));			
		}
		return (sb.toString());
	}	
	public String joinWith(Vector<T> v, String cpair, String c) {
		StringBuffer sb = new StringBuffer();
		int first=1;
		for (int i = 0; i < size(); ++i) {
			if (first==1)	first=0;			
			else	sb.append(c);				
			sb.append(FString.format(get(i))	
					+cpair+FString.format(v.get(i)));			
		}
		return (sb.toString());
	}	
	public static String toString(
			TVector<String> vk, TVector<String> vx){		
		return vk.joinS(vx, "=","\t");
	}	
	public static String joinTabbedStrings(String s1, String s2){
		return toString(FString.splitVS(s1, "\t")
				, FString.splitVS(s2,"\t"));
	}
	public T sample(){
		if (size()==0)return null;
		return get(FRand.drawInt(size()));
	}
	public T sample(int b, int e){
		if (size()==0)return null;
		return get(FRand.drawInt(b,e));
	}
	
	public TVector<T> randomize(){
		for (int i=0; i< size(); ++i)
			swap(i, FRand.rand.nextInt(size()));
		return this;
	}
	/**swap object i1 and i2*/
	public TVector<T> swap(int i, int j){
		if (i!=j){
			T x=get(i);
			set(i,get(j));
			set(j,x);
		}
		return this;
	}
	public boolean save(String fn){
		return	FFile.save(this,fn,"\n");
	}	
	public boolean save(String fn, String sep){
		return	FFile.save(this,fn,sep);
	}	
	public boolean saveT(String fn,String title){
		return	FFile.save(this,fn,"\n",title);
	}	
	
	public T parseLine(String line){
		return null;
	}	

	public boolean loadFile(String fn){
		BufferedReader br = FFile.bufferedReader(fn);
		if (br==null)
			return false;
		String line=null;
		while ( (line = FFile.readLine(br))!=null) 
			add(parseLine(line));
		FFile.close(br);
		return true;
	}
	public boolean loadFile(String fn, int iCol, String sep, boolean bSkipTitle){
		BufferedReader br = FFile.bufferedReader(fn);
		if (br==null)
			return false;
		String line=null;
		if (bSkipTitle) FFile.readLine(br);
		
		while ( (line = FFile.readLine(br))!=null) 
			add(parseLine(line.split(sep)[iCol]));
		FFile.close(br);
		return true;
	}
	public TVector<T> loadLine(String x, String sep) {
		this.clear();
		String vs[]= x.split(sep);		
		this.ensureCapacity(vs.length);
		for (String s: vs)
			this.add(parseLine(s));
		return this;
	}
  public synchronized T[] toArray() {
  	//Array.newInstance((c[]).class, size());
  	T v[]=(T[]) new Object[size()];
    return (T[])Arrays.copyOf(elementData, elementCount);
  }

  public static class VectorD extends TVector<Double> implements   ICloneable{
  	public VectorD(){
  		super(Double.class);
  	}
  	public VectorD(int k){
  		super(k, 0.0);
  	}
  	public VectorD(int k, Double v){
  		super(k,v);
  	}	
  	
  	public Double newValue(){//weakness of Java template
  		return 0.0;
  	}
  	public VectorD newInstance(){
  		return new VectorD();
  	}		

  	
  	public int countNonZero(){
  		int n=0;
  		for (double d:this)
  			if (d!=0.0) ++n;
  		return n;
  	}
  	
  	public double normL1(){
  		double s=0;
  		for (Double d: this)
  			s+= Math.abs(d);
  		return s;
  	}
  	public double[] toDoubleArray(){
  		double[] v = new double[size()];
  		for (int i=0; i< size();++i)
  			v[i]=get(i);
  		return v;
  	}
  	public VectorD setAll(double[] v)	{
  		this.setSize(v.length);
  		for (int i=0; i<v.length; ++i)
  			this.set(i,v[i]);
  		return this;
  	}
  }
  public static class VectorI extends TVector<Integer>	implements Comparable<VectorI>{
  	public VectorI(){
  		super(Integer.class);
  	}
  	public VectorI(int k){
  		super(k, 0);//, Integer.class);
  	}
  	public VectorI(int k, int v){
  		super(k,v);
  	}	
  	public int compareTo(VectorI c){
  		int len= Math.min(size(),c.size());
  		
  		for (int i=0; i<len; ++i ){
  			if (get(i)<c.get(i))return -1;
  			else if (get(i)>c.get(i))return 1;
  		}
  		if (size()<c.size())return -1;
  		else if (size()>c.size())return 1;		
  		return 0;
  	}

  	public Integer newValue(){//weakness of Java template
  		return 0;
  	}	
  	public VectorI newInstance(){
  		return new VectorI();
  	}

  	public MapIIb newMapValueId(){
  		return new MapIIb();
  	}
  	public VectorI plusOnE( int i, int x){
  		extend(i+1);
  		set(i, get(i)+x);
  		return this;
  	}
  	
  	public Integer sum() {
  		Integer s=0;
  		for (Integer d:this)		s += d;
  		return s;
  	}
  }
  public static class VectorS extends TVector<String>implements Cloneable{
  	public VectorS(){
  		super(String.class);
  	}
  	public VectorS(String[] v) {
  		super(v);
  	}
  	public VectorS(Collection<String> v){
  		super(v);
  	}
  	public VectorS(int n){
  		super(n,String.class);
  	}
  	public VectorS sub(Vector<Integer> vi) {
  		return (VectorS) super.sub(vi);
  	}	
  	public MapSI newMapValueId(){
  		return new MapSI();
  	}

  	public MapSI toMapValueId(){
  		return (MapSI) super.toMapValueId();
  	}
  }
  public static class VectorVectorS  extends TVector<VectorS>{
  	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
  	public VectorVectorS newInstance(){
  		return new VectorVectorS();
  	}	

  	public VectorVectorS(){
  		super(VectorS.class);
  		//super(Double.class);
  	}	


  }
  public static class TVectorVector<V> extends TVector<TVector<V>>{
  	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
  	public TVectorVector<V> newInstance(){
  		return new TVectorVector<V>(c);
  	}		
  	public TVector<V> newValue(){
  		return new TVector<V>(c);
  	}		
  	public TVectorVector(Class c){
  		super( (new TVector<V>(c)).getClass());
  	}
  	// removed nov 12 2010 (nobody calls)
//  	public void setRange(int i, int j, V x) {
//  		get(i).set(j, x);		
//  	}	
  	public V get(int i, int j) {
  		return get(i).get(j);		
  	}	
  	public void setSize(int m, int n){
  		this.setSize(m);
  		for (int i=0;i<m; ++i){
  			TVector<V> v=newValue();
  			v.setSize(n);
  			this.set(i,v);
  		}
  	}
  	public void setSizeLowerTriangle(int n){
  		this.setSize(n);
  		for (int i=1;i<=n; ++i){
  			TVector<V> v=newValue();
  			v.setSize(i);
  			this.set(i-1,v);
  		}
  	}
  }

  
  /**
   * @author nlao
   *	a matrix consists of column vectors
   */
  public static class VectorMapID extends TVector<MapID> { 
  	private static final long serialVersionUID = 2008042701L; // YYYYMMDD
  	public VectorMapID newInstance(){
  		return new VectorMapID();
  	}	

  	public VectorMapID(){
  		super(MapID.class);
  	}	
  	public VectorMapID(int n){
  		super(n, MapID.class);
  	}	
  	public MapID  merge(){		
  		MapID  v = new MapID();
  		for ( MapID m : this ) 
  			v.plusOn(m);		
  		return v;
  	}

  	/**
  	 * each MapID_i is treated as a distribution p(X_i=x)
  	 * @return p(sum_i(X_i)=x)
  	 */
  	public MapID  convolve(){		
  		MapID  v = new MapID();
  		v.put(0, 1.0);
  		for ( MapID m : this ) 
  			v= v.convolve(m);				
  		return v;
  	}
  	public Double get(int i, int j){
  		return getE(i).get(j);
  	}
  	public Double set(int i, int j, Double x){
  		return getE(i).put(j, x);
  	}	
  	public Double getC(int i, int j){
  		return get(i).getC(j);
  	}	
  	public void plusOn(int i, int j, Double x){
  		getE(i).plusOn(j,x);
  	}		
  	public VectorMapID plusOn(VectorMapID vm){
  		for (int i=0; i<vm.size(); ++i){
  			getE(i).plusOn(vm.get(i));
  		}
  		return this;
  	}	
  	public VectorMapID plusOn(int i, TMapXD<Integer> m){
  		getE(i).plusOn(m);
  		return this;
  	}	
  	public VectorMapID minusOn(int i, TMapXD<Integer> m){
  		getE(i).minusOn(m);
  		return this;
  	}		
  	public VectorMapID plusOn( TMapXD<Integer> m, int j){
  		if (m==null) return this;
  		for ( Map.Entry<Integer, Double> e : m.entrySet() ) {
  			Integer k = e.getKey();
  			Double x = e.getValue();
  			getE(k).plusOn(j,x);
  		}		
  		return this;
  	}			
  	public VectorMapID minusOn( TMapXD<Integer> m, int j){
  		if (m==null) return this;
  		for ( Map.Entry<Integer, Double> e : m.entrySet() ) {
  			Integer k = e.getKey();
  			Double x = e.getValue();
  			getE(k).minusOn(j,x);
  		}		
  		return this;
  	}			
//  	public void addOn(int i, int j, Double x){	extend(i).plusOn(j,x);	}
  	public static VectorMapID I(int p){
  		VectorMapID v= new VectorMapID();
  		v.init(p);//		v.ensureCapacity(p);
  		for (int i=0;i<p; ++i)
  			v.get(i).add(i);
  		return v;
  	}
  	
  	public VectorD multiply( MapID m){
  		//MapID
  		VectorD v = new VectorD();
  		v.ensureCapacity(this.size());
  		for ( MapID m1: this ) {
  			v.add(m1.inner(m));
  		}		
  		return v;
  	}	
  	public VectorD multiply( VectorD m){
  		//MapID
  		VectorD v = new VectorD();
  		v.ensureCapacity(this.size());
  		for ( MapID m1: this ) {
  			v.add(m1.inner(m));
  		}		
  		return v;
  	}		
  	public VectorD multiply(double[] m){
  		VectorD v = new VectorD();
  		v.ensureCapacity(this.size());
  		for ( MapID m1: this ) 
  			v.add(m1.inner(m));
  		
  		return v;
  	}		
  	
  	public MapID weightedSum( VectorD vWeights){
  		if (vWeights.size()!= size())
  			System.err.println("unmatched matrix dimension");
  		MapID v = new MapID();
  		for (int i=0; i<size();++i) 
  			v.plusOn(get(i),vWeights.get(i));		
  		return v;
  	}	
  	
  	public MapID weightedSum( double[] vWeights){
  		if (vWeights.length!= size())
  			System.err.println("unmatched matrix dimension");
  		MapID v = new MapID();
  		for (int i=0; i<size();++i) 
  			v.plusOn(get(i),vWeights[i]);		
  		return v;
  	}	
  	
  	public VectorD multiplySum( VectorMapID vm){
  		VectorD v = new VectorD();
  		v.ensureCapacity(vm.size());
  		for ( MapID m: vm ) {
  			v.add( multiply(m).sum());
  		}			
  		return v;
  	}
  	
  	
  	public VectorMapID sub(Set<Integer> vi) {
  		return (VectorMapID) super.sub(vi);
  	}
  	public VectorMapID sub(Vector<Integer> vi) {
  		return (VectorMapID) super.sub(vi);
  	}
  	public VectorMapID subRows(Set<Integer> vi) {
  		VectorMapID vm = new VectorMapID();
  		vm.ensureCapacity(size());
  		for ( MapID m: this ) {
  			vm.add((MapID) m.sub(vi));
  		}			
  		return vm;
  	}
  	public VectorMapID subRows(Vector<Integer> vi) {
  		VectorMapID vm = new VectorMapID();
  		vm.ensureCapacity(size());
  		for ( MapID m: this ) {
  			vm.add((MapID) m.sub(vi));
  		}			
  		return vm;
  	}
  	/** Sherman-Morrison updating formula
  	 * replace the iTH column of C matrix with new vector vNew
  	 * A'=A-a_i * dc'A /(1+dc'a_i)
  	 * */
  	public VectorMapID SMUpdate(int i, MapID vOld, MapID vNew){
  		MapID dc= vNew.minus(vOld);
  		MapID ai=(MapID) get(i).clone();
  		double under = 1+dc.inner(ai);
  		this.minusOuterOn(multiply(dc),ai.devide(under));
  		return this;
  	}
  	
  	/**swap column i1 and i2*/
  	public VectorMapID swap(int i1, int i2){
  		return (VectorMapID) super.swap(i1, i2);
  	}
  	
  	public VectorMapID plusOuterOn(Vector< Double> v, MapID m) {
  		for (int i=0; i<v.size();++i){
  			Double x = v.get(i);
  			if (x!=0.0) 
  				this.get(i).plusOn(m,x);
  		}
  		return this;
  	}	
  	public VectorMapID minusOuterOn(Vector< Double> v,MapID m) {
  		for (int i=0; i<v.size();++i){
  			Double x = v.get(i);
  			if (x!=0.0) 
  				this.get(i).plusOn(m,-x);
  		}
  		return this;
  	}	
  	
  	public VectorMapID plusOuterOn(MapID m1, MapID m2) {
  		for (Map.Entry<Integer, Double> e: m1.entrySet())
  			getE(e.getKey()).plusOn(m2,e.getValue());		
  		return this;
  	}	
  	public VectorMapID plusOuterOn(MapID m1,VectorI vidx1
  			, MapID m2, VectorI vidx2) {
  		if (m1.size()==0 || m2.size()==0)
  			return this;
  		for (Map.Entry<Integer, Double> e: m1.entrySet()){
  			int k=vidx1.get(e.getKey());
  			getE(k).plusOn(m2,vidx2,e.getValue());		
  		}
  		return this;
  	}	


  	public VectorMapID catOn(VectorMapID v)	{
  		return (VectorMapID) super.catOn(v);
  	}
  	public VectorMapID upperTrinagleOn(){
  		VectorI vd= new VectorI();
  		for (int i=0; i<this.size(); ++i){
  			MapID m = get(i);
  			if (m.size()==0) continue;
  			vd.clear();
  			for (int id: m.keySet()){
  				if (id>=i) break;
  				getE(id).plusOn(i, m.get(id));		
  				vd.add(id);
  			}
  			for (int id: vd)
  				m.remove(id);
  			/*vd.addAll(m.keySet());
  			for (int id: vd)
  				if (id<i){
  					getE(id).plusOn(i, m.get(id));
  					m.remove(id);
  				}*/
  		}
  		return this;
  	}
  	public int totalSize(){
  		return getVI(CTag.size).sum();
  	}
  	public String toString(){
  		VectorI vi = getVI(CTag.size);		
  		return "#elements="+vi.sum()+"\n"
  			+vi.join(",");
  	}
  	public void clearElements(){
  		for (MapID m: this)
  			m.clear();
  	}
  	public VectorD getRowV(int i){
  		VectorD v= new VectorD();
  		v.ensureCapacity(size());
  		for (MapID m: this){
  			Double d=m.get(i);
  			if (d!=null) v.add(d);
  			else v.add(0.0);
  		}			
  		return v;
  	}
  	
  	public VectorD getRowVNega(int i){
  		VectorD v= new VectorD();
  		v.ensureCapacity(size());
  		for (MapID m: this){
  			Double d=m.get(i);
  			if (d!=null) v.add(-d);
  			else v.add(0.0);
  		}			
  		return v;
  	}
  	public MapID getRowM(int i){
  		MapID m= new MapID();
  		for (int j=0; j<size(); ++j){
  			Double d=get(j).get(i);
  			if (d!=null) m.put(j,d);
  		}			
  		return m;
  	}
  	public MapID getRowMNega(int i){
  		MapID m= new MapID();
  		for (int j=0; j<size(); ++j){
  			Double d=get(j).get(i);
  			if (d!=null) m.put(j,-d);
  		}			
  		return m;
  	}
  	public VectorMapID getRowsVM(VectorI vi){
  		VectorMapID vv= new VectorMapID();
  		vv.ensureCapacity(vi.size());
  		for (int i: vi)
  			vv.add(getRowM(i));
  		return vv;
  	}
  	public VectorMapID getRowsVMNega(VectorI vi){
  		VectorMapID vv= new VectorMapID();
  		vv.ensureCapacity(vi.size());
  		for (int i: vi)
  			vv.add(getRowMNega(i));
  		return vv;
  	}
  	public boolean containsKey(int key){
  		for (MapID m: this)
  			if (m.containsKey(key))
  				return true;
  		return false;
  	}
  }
}
