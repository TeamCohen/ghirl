package ghirl.graph;

import edu.cmu.lti.algorithm.container.SetI;
import ghirl.util.CompactImmutableArrayDistribution;
import ghirl.util.CompactImmutableDistribution;
import ghirl.util.Distribution;

import java.util.Arrays;

import org.apache.log4j.Logger;
import org.apache.log4j.Priority;

public class SparseCompactGraph extends CompactGraph {
	private static final Logger logger = Logger.getLogger(SparseCompactGraph.class);
	/** One row for each graphId */
	private SparseRow[] sparseWalkInfo;
	/** Buffer (for load-time only) allows for the maximum number of link labels. */
	private SparseRow sparseWalkBuffer;
	/** Carries load-time state */
	private int lastSrcId=-1, nLinks=-1;
	
	@Override
    protected void initWalkInfo() {
        sparseWalkInfo = new SparseRow[ graphIds.length ];
        sparseWalkBuffer = new SparseRow(linkLabels.length);
    }
	@Override
    protected void addWalkInfoDistribution(int srcId, int linkId, int[]destId, float[] totalWeightSoFar) {
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
		if (logger.isDebugEnabled()) {
			StringBuilder sb=new StringBuilder();
			for (int i=0;i<nLinks;i++) {
				sb.append(sparseWalkBuffer.sortedLabelIds[i]).append(":").append(sparseWalkBuffer.destinations[i].size()).append(" ");
			}
			Runtime r = Runtime.getRuntime();
			long free=r.freeMemory(), total = r.totalMemory();
			String memstring = free+" bytes free of "+total+" available ("+Math.round((double)free/total*100)+"%)";
			logger.debug("Writing row "+lastSrcId+" with "+nLinks+" links: ("+sb.toString()+"). "+memstring);
		}
		//too much log info in the log file, removed by Ni
		//...set log4j.logger.ghirl.graph.SparseCompactGraph=INFO or greater. That's what a logging system is for.
		sparseWalkInfo[lastSrcId] = new SparseRow(nLinks);
		System.arraycopy(sparseWalkBuffer.sortedLabelIds, 0, 
				sparseWalkInfo[lastSrcId].sortedLabelIds, 0, nLinks);
		System.arraycopy(sparseWalkBuffer.destinations, 0,
				sparseWalkInfo[lastSrcId].destinations, 0, nLinks);
	}
	
	@Override
    protected Distribution getStoredDist(int fromNodeIndex,int linkIndex)
    {
        if (logger.isDebugEnabled()) logger.debug("Getting distribution for node "+fromNodeIndex+" link "+linkIndex);
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
			if (logger.isDebugEnabled()) {
				logger.debug("Link "+labelIndex+" is at sparse index "+i);
			}
			if (i<0) return null;
			return destinations[i];
		}
		public void put(int index, int linkLabelId, CompactImmutableDistribution cid) {
			if (index > destinations.length) 
				logger.error("Attempt to write past the end of this sparse row (index "+index+", length "+destinations.length);
			sortedLabelIds[index] = linkLabelId;
			if (index > 0) assert (sortedLabelIds[index] > sortedLabelIds[index-1]) : "Link labels must be sorted! Tried to add label id "+sortedLabelIds[index]+" after id "+sortedLabelIds[index-1]+" at index "+index;
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
