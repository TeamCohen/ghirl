package ghirl.graph;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

import ghirl.util.CompactImmutableArrayDistribution;
import ghirl.util.CompactImmutableDistribution;
import ghirl.util.Distribution;

public class SparseCompactGraph extends CompactGraph {
	private static final Logger logger = Logger.getLogger(SparseCompactGraph.class);
	/** One row for each graphId */
	private SparseRow[] sparseWalkInfo;
	/** Buffer for load-time only allows for the maximum number of link labels. */
	private SparseRow sparseWalkBuffer;
	/** Carries load-time state */
	private int lastSrcId=-1, nLinks=-1;
	
	@Override
    protected void initWalkInfo() {
        sparseWalkInfo = new SparseRow[ graphIds.length ];
        sparseWalkBuffer = new SparseRow(linkLabels.length);
    }
	@Override
    protected void initWalkInfoCell(int srcId, int linkId, int[]destId, float[] totalWeightSoFar) {
		if (srcId != lastSrcId) { 
			if (lastSrcId != -1) finishWalkInfo();
			lastSrcId = srcId; nLinks = 0;
		}
		sparseWalkBuffer.put(nLinks, linkId, new CompactImmutableArrayDistribution(destId, totalWeightSoFar, graphIds));
		nLinks++;
	}
	
	// The arraycopy here is kindof gross, but without knowing ahead of time how 
	// many links are on each edge we'd end up thrashing in memory anyhow.
	// If we want, we can add a field to the nodes file which says
	// how many outgoing links it has.  This would be fairly easy to compute,
	// but we'd have to thread it through the rest of the CompactGraph load()
	// pipeline (MxN CompactGraph doesn't need to know on a per-node basis)
	// and I wasn't ready to do that just yet.
	@Override
	protected void finishWalkInfo() { 
		int size=0;
		String memstring="";
		if (logger.isEnabledFor(Priority.INFO)) {
			size = sparseWalkBuffer.sizeInBytes(nLinks);
			Runtime r = Runtime.getRuntime();
			long free=r.freeMemory(), total = r.totalMemory();
			memstring = free+" bytes of "+total+" available ("+Math.round((double)free/total*100)+"%)";
		}
		logger.info("Writing row "+lastSrcId+" with "+nLinks+" links: "+size+" in data; "+memstring);
		sparseWalkInfo[lastSrcId] = new SparseRow(nLinks);
		System.arraycopy(sparseWalkBuffer.sortedLabelIds, 0, 
				sparseWalkInfo[lastSrcId].sortedLabelIds, 0, nLinks);
		System.arraycopy(sparseWalkBuffer.destinations, 0,
				sparseWalkInfo[lastSrcId].destinations, 0, nLinks);
	}
	
	@Override
    protected Distribution getStoredDist(int fromNodeIndex,int linkIndex)
    {
        Distribution result = sparseWalkInfo[fromNodeIndex].get(linkIndex);
        if (result == null) {
            return EMPTY_DIST;
        } else {
            return result;
        }
    }

	protected class SparseRow {
		public int[] sortedLabelIds;
		public CompactImmutableDistribution[] destinations;
		public SparseRow(int nlabels) {
			sortedLabelIds = new int[nlabels];
			destinations = new CompactImmutableDistribution[nlabels];
		}
		public CompactImmutableDistribution get(int labelIndex) {
			int i = Arrays.binarySearch(sortedLabelIds, labelIndex);
			if (i<0) return null;
			return destinations[i];
		}
		public void put(int index, int linkLabelId, CompactImmutableDistribution cid) {
			if (index > destinations.length) 
				logger.error("Attempt to write past the end of this sparse row (index "+index+", length "+destinations.length);
			sortedLabelIds[index] = linkLabelId;
			destinations[index] = cid;
		}
		public int sizeInBytes() { return sizeInBytes(sortedLabelIds.length); }
		public int sizeInBytes(int n) {
			int size =0;
			for (int i=0; i<n; i++) size += sparseWalkBuffer.destinations[i].sizeInBytes();
			return Integer.SIZE*n/8 + 4*n+ size;
		}
	}
}
