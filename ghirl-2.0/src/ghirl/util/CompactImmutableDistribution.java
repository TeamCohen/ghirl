package ghirl.util;

import java.util.*;
import edu.cmu.minorthird.util.StringUtil;

/**
 * A distribution that takes up minimal space, but cannot be added to or changed.
 */
public abstract class CompactImmutableDistribution extends Distribution
{

	// parallel arrays
	/** objectIndex[k] is index of object in sortedObjectArray */
	protected int[] objectIndex; 
	/** totalWeightSoFar[k] is cumulative weight of all objects pointed to be objectIndex[0]...objectIndex[k] */
	protected float[] totalWeightSoFar; 
	/** same as totalWeightSoFar[ totalWeightSoFar.length-1 ]; */
	protected float totalWeight;

	/** Only for clever extensions. */
	protected CompactImmutableDistribution() {}
	public CompactImmutableDistribution(int[] objectIndex, float[] totalWeightSoFar) {

		this.totalWeightSoFar = totalWeightSoFar;
		this.objectIndex = objectIndex;
		if (totalWeightSoFar.length>0) 
			this.totalWeight = totalWeightSoFar[totalWeightSoFar.length - 1];
	}
	
	abstract protected int getIndex(Object obj);
	abstract protected Object getObject(int index);

	public int sizeInBytes()
	{
		return (Integer.SIZE*objectIndex.length + Float.SIZE*totalWeightSoFar.length + Float.SIZE)/8;
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
            if (objectIndex[0] == index) {
            	theLastWeight = totalWeightSoFar[0];
            	return theLastWeight;
            }
            for (int k=1; k<objectIndex.length; k++) {
            	if (objectIndex[k] == index) {
                	theLastWeight = totalWeightSoFar[k] - totalWeightSoFar[k-1];
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
		public boolean hasNext() { return index < objectIndex.length; }
		public void remove() { throw new UnsupportedOperationException("can't remove"); }
		public Object next() { 
                    Object result = getObject(objectIndex[index]); 
                    if (index==0) theLastWeight = totalWeightSoFar[0];
                    else theLastWeight = totalWeightSoFar[index]-totalWeightSoFar[index-1];
                    index++;
                    return result;
		}
	}

	public int size()
	{
		return objectIndex.length;
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
		int find = Arrays.binarySearch( totalWeightSoFar, r );
		if (find < 0) {
			// find = -insertionPoint-1 
			int insertionPoint = -(find+1);
			return getObject( objectIndex[insertionPoint] );
		} else {
			return getObject( objectIndex[find] );
		}
	}

	public String toString()
	{
		double[] wts = new double[totalWeightSoFar.length];
		if (totalWeightSoFar.length>=1) {
			wts[0] = totalWeightSoFar[0];
			for (int i=1; i<totalWeightSoFar.length; i++) {
				wts[i] = totalWeightSoFar[i]-totalWeightSoFar[i-1];
			}
		}
		return 
		"[cid tot "+getTotalWeight()+" wts: "+StringUtil.toString(wts)+" idx: "+StringUtil.toString(objectIndex)
		//+" obj: "+StringUtil.toString(sortedObjectArray)+"]";
		;
	}

	// it's ok to share all structure since the distribution is immutable
	final public Distribution copy()
	{
		return this;
	}
}
