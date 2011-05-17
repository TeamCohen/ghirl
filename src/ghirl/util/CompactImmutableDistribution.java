package ghirl.util;

import java.util.*;

import edu.cmu.lti.algorithm.container.MapID;
import edu.cmu.minorthird.util.StringUtil;

/**
 * A distribution that takes up minimal space, 
 * but cannot be added to or changed.
 */
public abstract class CompactImmutableDistribution extends Distribution
{

	// parallel arrays
	/** viObj[k] is index of object in sortedObjectArray */
	protected int[] viObj; 
	/** tot[k] is cumulative weight of all objects pointed to be objectIndex[0]...objectIndex[k] */
	protected float[] tot; 
	/** same as tot[ tot.length-1 ]; */
	protected float totalWeight;

	/** Only for clever extensions. */
	protected CompactImmutableDistribution() {}
	public CompactImmutableDistribution(int[] objectIndex, float[] totalWeightSoFar) {

		this.tot = totalWeightSoFar;
		this.viObj = objectIndex;
		if (totalWeightSoFar.length>0) 
			this.totalWeight = totalWeightSoFar[totalWeightSoFar.length - 1];
	}
	
	abstract protected int getIndex(Object obj);
	abstract protected Object getObject(int index);

	public int sizeInBytes()
	{
		return (Integer.SIZE*viObj.length + Float.SIZE*tot.length + Float.SIZE)/8;
	}

	/** Not supported in this implementation.
	 */
	public void add(double weight,Object obj)
	{
		throw new UnsupportedOperationException("cannot add to a CompactImmutableDistribution");
	}

	/** Not supported in this implementation.
	 */
	public Object remove(Object obj)
	{
		throw new UnsupportedOperationException("cannot add to a CompactImmutableDistribution");	
	}

	/** This is supported, but takes time linear in the size of the distribution. 
	 */
	public double getWeight(Object obj)
	{
    int index = getIndex(obj); //Arrays.binarySearch(sortedObjectArray,obj);
    if (index<0) return 0;
    if (viObj[0] == index) {
    	theLastWeight = tot[0];
    	return theLastWeight;
    }
    for (int k=1; k<viObj.length; k++) {
    	if (viObj[k] == index) {
        	theLastWeight = tot[k] - tot[k-1];
        	return theLastWeight;
    	}
    }
    return 0;
	}

	/** Return an iterator over all objects.
	 */
	public Iterator iterator()
	{
		return new MyIterator();
	}

	private class MyIterator implements Iterator
	{
		int index = 0;
		public boolean hasNext() { 
			return index < viObj.length; 
		}
		
		public void remove() { 
			throw new UnsupportedOperationException("can't remove"); 
		}
		
		public Object next() { 
      Object result = getObject(viObj[index]); 
      if (index==0) theLastWeight = tot[0];
      else theLastWeight = tot[index]-tot[index-1];
      index++;
      return result;
		}
	}

	public int size()
	{
		return viObj.length;
	}

	public double getLastWeight()
	{
		return theLastWeight;
	}

	public double getTotalWeight()
	{
		return totalWeight;
	}

	public Object sample(Random rand)
	{
		float r = (float)rand.nextDouble()*totalWeight;
		int find = Arrays.binarySearch( tot, r );
		if (find < 0) {
			// find = -insertionPoint-1 
			int insertionPoint = -(find+1);
			return getObject( viObj[insertionPoint] );
		} else {
			return getObject( viObj[find] );
		}
	}

	public String toString()
	{
		double[] wts = new double[tot.length];
		if (tot.length>=1) {
			wts[0] = tot[0];
			for (int i=1; i<tot.length; i++) {
				wts[i] = tot[i]-tot[i-1];
			}
		}
		return 
		"[cid tot "+getTotalWeight()+" wts: "+StringUtil.toString(wts)+" idx: "+StringUtil.toString(viObj)
		//+" obj: "+StringUtil.toString(sortedObjectArray)+"]";
		;
	}
	
  public MapID toMapID(){
  	MapID m= new MapID();
		if (tot.length>=1) {
			for (int i=0; i<tot.length; i++) {
				double w=(i==0)? tot[0]:
					tot[i]-tot[i-1];
				m.put(viObj[i], w);
			}
		}
		return m;
  }
	public String print(){
		return toMapID().toString();

	}

	// it's ok to share all structure since the distribution is immutable
	final public Distribution copy()
	{
		return this;
	}
}
