package ghirl.util;

import ghirl.graph.GraphId;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import tokyocabinet.BDB;
import tokyocabinet.Util;

public class CompactTCDistribution extends CompactImmutableDistribution implements Serializable {
	/** Keys are node ids (int), values are flavor$shortName. */
	protected transient BDB graphIds;
	protected boolean ordered;
	protected Integer[] orderedIndices;
	
	public void setDb(BDB graphIds) { this.graphIds = graphIds; }
	public byte[] serialize() throws IOException {
		if (Integer.SIZE != Float.SIZE)
			throw new RuntimeException("System not supported.  Float and Int must have same bit length.");
		// Serialization format:
		// int (Byte.SIZE) bytes-per-int
		// int (Integer.SIZE) length
		// float[] (Float.SIZE) totalWeight
		// byte (Byte.SIZE) ordered
		// int[] (Integer.SIZE x length) objectIndex
		// float[] (Float.SIZE x length) totalWeightSoFar
		// [int[] (Integer.SIZE x length) orderedIndices]
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int bytesPerInt = Integer.SIZE / Byte.SIZE;
		baos.write(bytesPerInt);                                                // bytes per int
		SerializationUtil.serializeInt(viObj.length, baos, bytesPerInt);                    // length of distribution
		SerializationUtil.serializeInt(Float.floatToRawIntBits(totalWeight), baos, bytesPerInt);  // total weight of distribution
		baos.write(ordered ? 1 : 0);                                            // whether ordered list is also stored
		for (int i=0; i<viObj.length; i++)                                // objectIndex[]
			SerializationUtil.serializeInt(viObj[i], baos, bytesPerInt);
		for (int i=0; i<viObj.length; i++)                                // totalWeightSoFar[]
			SerializationUtil.serializeInt(Float.floatToRawIntBits(tot[i]), baos, bytesPerInt);
		if (ordered) {
			for (int i=0; i<viObj.length; i++)                            // orderedIndices[]
				SerializationUtil.serializeInt(orderedIndices[i].intValue(), baos, bytesPerInt);
		}
		baos.close();
		return baos.toByteArray();
	}
	public CompactTCDistribution(byte[] bytes, BDB graphIds) throws IOException {
		ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
		int bytesPerInt, length;
		bytesPerInt = bais.read();
		length = SerializationUtil.deserializeInt(bais, bytesPerInt);
		int totalWeightBits = SerializationUtil.deserializeInt(bais, bytesPerInt);
		this.totalWeight = Float.intBitsToFloat(totalWeightBits);
		this.ordered = bais.read() == 1;
		this.viObj = new int[length];
		this.tot = new float[length];
		for(int i=0; i<length; i++) {
			viObj[i] = SerializationUtil.deserializeInt(bais, bytesPerInt);
		}
		for(int i=0; i<length; i++) {
			tot[i] = 
				Float.intBitsToFloat(SerializationUtil.deserializeInt(bais,bytesPerInt));
		}
		if (ordered) {
			this.orderedIndices = new Integer[length];
			for(int i=0; i<length; i++) {
				orderedIndices[i] = SerializationUtil.deserializeInt(bais,bytesPerInt);
			}
		}
		bais.close();
		this.graphIds = graphIds;
	}
	
	public CompactTCDistribution(int[] objectIndex, float[] totalWeightSoFar, BDB graphIds) {
		this(objectIndex,totalWeightSoFar,graphIds,false);
	}
	public CompactTCDistribution(int[] objectIndex, float[] totalWeightSoFar, BDB graphIds, boolean ordered) {
		super(objectIndex,totalWeightSoFar);
		this.graphIds = graphIds;
		this.ordered = ordered;
		if (ordered) sortObjectIndex();
	}
	
	public Iterator orderedIterator(boolean descending) {
		if (!ordered) throw new UnsupportedOperationException("Ordered representation is n^3 without pre-indexing; you don't want to do that");
		return new MyOrderedIterator();
	}

	@Override
	protected int getIndex(Object obj) {
		throw new UnsupportedOperationException("No mapping of object->index is stored for CompactTCDistribution");
	}

	@Override
	protected Object getObject(int index) {
		byte[] result = graphIds.get(Util.packint(index));
		if (result == null) return null;
		String name = new String(result);
		return GraphId.fromString(name);
	}
	private void sortObjectIndex() {
		// first create an array with the index of the matched pair objectIndex[i], totalWeight[i]
		orderedIndices = new Integer[viObj.length];
		for (int i=0;i<orderedIndices.length;i++) orderedIndices[i]=i;
		Arrays.sort(orderedIndices,new Comparator<Integer>() {
			public int compare(Integer index1, Integer index2) {
				float w1, w2;
				if (index1 > 0) w1 = tot[index1] - tot[index1-1];
				else w1 = tot[0];
				if (index2 > 0) w2 = tot[index2] - tot[index2-1];
				else w2 = tot[0];
				return ((Float)w1).compareTo(w2);
			}
		});
	}
	private class MyOrderedIterator implements Iterator {
        private int i;
        public MyOrderedIterator() { this.i = 0; }
        public void remove() { throw new UnsupportedOperationException("can't remove!"); }
        public boolean hasNext() { return i<viObj.length; }
        public Object next() {
        	int ind = orderedIndices[ i ];
            Object o = getObject( viObj[ind ]);
            //System.out.println("getting weight of "+o+" in MyOtherIterator");
            theLastWeight = ind > 0 ? tot[ind] - tot[ind-1] : tot[0];
            i++;
            return o;
        }
	}

}
