package ghirl.util;

import java.util.*;
import edu.cmu.minorthird.util.StringUtil;

/**
 * A distribution that takes up minimal space, but cannot be added to or changed.
 */
public class CompactImmutableDistribution extends Distribution
{
	private Object[] sortedObjectArray;

	// parallel arrays
	// objectIndex[k] is index of object in sortedObjectArray */
	private int[] objectIndex; 
	// totalWeightSoFar[k] is cumulative weight of all objects pointed to be objectIndex[0]...objectIndex[k]
	private float[] totalWeightSoFar; 
	// same as totalWeightSoFar[ totalWeightSoFar.length-1 ];
	private float totalWeight;

	/**
	 * @param objectIndex - array objects in the distribution, named by
	 * their positions in the sortedObjectArray.
	 * @param totalWeightSoFar - parallel array of the cumulative weight of all objects 
	 * in the distribution up to the corresponding point in the objectIndices array.
	 * @param sortedObjectArray - sorted array that contains all objects in the distribution
	 * 
	 * The arrays passed in are not copied.
	 */
	public CompactImmutableDistribution(int[] objectIndex, float[] totalWeightSoFar, Object[] sortedObjectArray)
	{
		this.sortedObjectArray = sortedObjectArray;
		this.totalWeightSoFar = totalWeightSoFar;
		this.objectIndex = objectIndex;
		this.totalWeight = totalWeightSoFar[totalWeightSoFar.length - 1];
	}

	/**
	 * @param dist - is a distribution of objects
	 * @param sortedObjectArray - sorted array that contains all objects in the distribution
	 */
	public CompactImmutableDistribution(Distribution dist, Object[] sortedObjectArray)
	{
		this.sortedObjectArray = sortedObjectArray;
		objectIndex = new int[dist.size()];
		totalWeightSoFar = new float[dist.size()];
		totalWeight = 0;
		int k = 0;
		for (Iterator i=dist.iterator(); i.hasNext(); ) {
			Object o = i.next();
			totalWeight += dist.getLastWeight();
			objectIndex[ k ] = Arrays.binarySearch(sortedObjectArray, o);
			if (objectIndex[ k ]<0) throw new IllegalStateException("Can't have a nonexistant object "+o+" in objectindex"+
					" k="+k+", "+objectIndex[k]+" in "+StringUtil.toString(sortedObjectArray));
			totalWeightSoFar[ k ] = totalWeight;
			k++;
		}
	}

	public int sizeInBytes()
	{
		return 4*objectIndex.length + 4*totalWeightSoFar.length + 8;
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
            int index = Arrays.binarySearch(sortedObjectArray,obj);
            if (index<0) return 0; 
            else if (index==0) {
                theLastWeight = totalWeightSoFar[0];
                return theLastWeight;
            } else {
                for (int k=1; k<objectIndex.length; k++) {
                    if (objectIndex[k]==index) {
                        theLastWeight = totalWeightSoFar[k] - totalWeightSoFar[k-1];
                        System.out.println("weight of "+obj+"="+theLastWeight);
                        return theLastWeight;
                    }
                }
                return 0;
            }
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
                    Object result = sortedObjectArray[objectIndex[index]]; 
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
			return sortedObjectArray[ insertionPoint ];
		} else {
			return sortedObjectArray[ find ];
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
		+" obj: "+StringUtil.toString(sortedObjectArray)+"]";
	}

	// it's ok to share all structure since the distribution is immutable
	final public Distribution copy()
	{
		return this;
	}
}
