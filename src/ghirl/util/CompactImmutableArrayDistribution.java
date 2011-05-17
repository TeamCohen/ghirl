package ghirl.util;

import edu.cmu.lti.algorithm.container.MapID;
import edu.cmu.minorthird.util.StringUtil;
import ghirl.graph.GraphId;
import ghirl.graph.ICompact;

import java.util.Arrays;
import java.util.Iterator;

public class CompactImmutableArrayDistribution extends
		CompactImmutableDistribution {

	/** array of GraphId elements not in sorted order */
	protected Object[] vObject;
	
	/**
	 * Create a new distribution from a paired list of object weights and indices 
	 * into a master sorted list of objects in the graph.
	 * @param objectIndex - Array of objects in the distribution, named by
	 * their positions in the sortedObjectArray.
	 * @param totalWeightSoFar - parallel array of the cumulative weight of all objects 
	 * in the distribution up to the corresponding point in the objectIndex array.
	 * @param vObject - not sorted array that contains at least all objects in the distribution (and
	 * may contain all objects in the graph)
	 * 
	 * No local copies are made of the arrays, but the arrays are never modified.
	 */
	public CompactImmutableArrayDistribution(int[] objectIndex, float[] totalWeightSoFar, Object[] vObject)
	{
		super(objectIndex, totalWeightSoFar);
		this.vObject = vObject;
	}
	
	ICompact g=null;
	public CompactImmutableArrayDistribution(MapID mDist, ICompact g){
		this(mDist.toVectorKey().toIntArray() 
				,mDist.toVectorValue().cumulateOn().toFloatArray()
				,null);//g.getGraphIds());
		this.g=g;
	}

	/**
	 * @param dist - is a distribution of objects
	 * @param vObj - not sorted array that contains all objects in the distribution
	 */
	public CompactImmutableArrayDistribution(Distribution dist, Object[] vObj)
	{
		super( new int[dist.size()], new float[dist.size()]);
		this.vObject = vObj;
		totalWeight = 0;
		int k = 0;
		for (Iterator i=dist.iterator(); i.hasNext(); ) {
			Object o = i.next();
			totalWeight += dist.getLastWeight();
			viObj[ k ] = Arrays.binarySearch(vObj, o);
			if (viObj[ k ]<0) throw new IllegalStateException("Can't have a nonexistant object "+o+" in objectindex"+
					" k="+k+", "+viObj[k]+" in "+StringUtil.toString(vObj));
			tot[ k ] = totalWeight;
			k++;
		}
	}
	
	protected int getIndex(Object obj) {
		if (g!=null)
			return g.getNodeIdx((GraphId)obj);
		return Arrays.binarySearch(vObject,obj);
	}
	protected Object getObject(int index) {
		if (g!=null)
			return g.getGraphIds()[index];
		return vObject[index];
	}
}
