package ghirl.util;

import java.util.Arrays;
import java.util.Iterator;

import edu.cmu.minorthird.util.StringUtil;

public class CompactImmutableArrayDistribution extends
		CompactImmutableDistribution {

	/** array of GraphId elements in sorted order */
	protected Object[] sortedObjectArray;
	
	/**
	 * Create a new distribution from a paired list of object weights and indices 
	 * into a master sorted list of objects in the graph.
	 * @param objectIndex - Array of objects in the distribution, named by
	 * their positions in the sortedObjectArray.
	 * @param totalWeightSoFar - parallel array of the cumulative weight of all objects 
	 * in the distribution up to the corresponding point in the objectIndex array.
	 * @param sortedObjectArray - sorted array that contains at least all objects in the distribution (and
	 * may contain all objects in the graph)
	 * 
	 * No local copies are made of the arrays, but the arrays are never modified.
	 */
	public CompactImmutableArrayDistribution(int[] objectIndex, float[] totalWeightSoFar, Object[] sortedObjectArray)
	{
		super(objectIndex, totalWeightSoFar);
		this.sortedObjectArray = sortedObjectArray;
	}

	/**
	 * @param dist - is a distribution of objects
	 * @param sortedObjectArray - sorted array that contains all objects in the distribution
	 */
	public CompactImmutableArrayDistribution(Distribution dist, Object[] sortedObjectArray)
	{
		super( new int[dist.size()], new float[dist.size()]);
		this.sortedObjectArray = sortedObjectArray;
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
	
	protected int getIndex(Object obj) {
		return Arrays.binarySearch(sortedObjectArray,obj);
	}
	protected Object getObject(int index) {
		return sortedObjectArray[index];
	}
}
